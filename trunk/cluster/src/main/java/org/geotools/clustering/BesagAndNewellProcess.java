/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.geotools.clustering;

import com.vividsolutions.jts.geom.Point;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.TreeMap;
import org.geotools.clustering.significance.PoissonTest;
import org.geotools.clustering.significance.SignificanceTestException;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.filter.text.cql2.CQLException;
import org.geotools.filter.text.ecql.ECQL;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.filter.Filter;
import org.opengis.filter.expression.PropertyName;
import org.opengis.geometry.MismatchedDimensionException;

/**
 * implements
 * <pre>
 * @article{citeulike:1463377,
author = {Besag, Julian and Newell, James},
citeulike-article-id = {1463377},
citeulike-linkout-0 = {http://dx.doi.org/10.2307/2982708},
citeulike-linkout-1 = {http://www.jstor.org/stable/2982708},
doi = {10.2307/2982708},
journal = {Journal of the Royal Statistical Society. Series A (Statistics in Society)},
keywords = {algorithm, automated, cancer, clustering, disease, spatial, spatial\_analysis},
number = {1},
pages = {143--155},
posted-at = {2007-07-17 18:38:19},
priority = {0},
title = {{The Detection of Clusters in Rare Diseases}},
url = {http://dx.doi.org/10.2307/2982708},
volume = {154},
year = {1991}
}
 * </pre>
 * @author ijt1
 */
public class BesagAndNewellProcess extends AbstractClusterProcess {

    private boolean sharedData = false;
    private int k = 0;

    public BesagAndNewellProcess(ClusterMethodFactory factory) {
        super(factory);
    }

    @Override
    ArrayList<Circle> process() throws MismatchedDimensionException, NoSuchElementException, SignificanceTestException {
        ArrayList<Circle> results = new ArrayList<Circle>();
        PropertyName popgeom = ff.property(pop.getSchema().getGeometryDescriptor().getName());
        PropertyName cangeom = ff.property(can.getSchema().getGeometryDescriptor().getName());
        SimpleFeatureCollection justCan = can;
        Filter canFilter = ff.greater(canattribute, ff.literal(0.0));
        if (sharedData) {
            justCan = can.subCollection(canFilter);
        }
        System.out.println("considering " + justCan.size() + " of " + can.size() + " case points");
        //find a case point
        SimpleFeatureIterator it = null;
        final int size = justCan.size();
        /*
         * TODO: replace this with a more efficient algorithm
         * for any realistic size dataset this will suck!
         */
        double[][] distances = new double[size][size];
        HashMap<Integer, TreeMap<Double, Integer>> lists = new HashMap<Integer, TreeMap<Double, Integer>>();
        SimpleFeature[] cancers = new SimpleFeature[size];
        int count = 0;
        try {
            it = justCan.features();
            while (it.hasNext()) {
                cancers[count++] = it.next();
            }
        } finally {
            it.close();
        }
        for (int i = 0; i < size; i++) {
            TreeMap<Double, Integer> index = new TreeMap<Double, Integer>();
            Point thisPoint = (Point) cancers[i].getDefaultGeometry();
            for (int j = 0; j < size; j++) {
                distances[i][j] = thisPoint.distance((Point) cancers[j].getDefaultGeometry());
                index.put(distances[i][j], j);
            }
            lists.put(i, index);
        }
        TreeMap<Double, Integer> dis;
        SimpleFeatureIterator popIt = null;
        SimpleFeatureIterator canIt = null;
        try {
            int counter = 0;
            it = justCan.features();
            while (it.hasNext()) {
                SimpleFeature feature = it.next();
                //find K nearest neighbours
                dis = lists.get(counter++);
                count = 0;
                double radius = 0.0;
                for (Double key : dis.keySet()) {
                    int index = dis.get(key);
                    radius = key.doubleValue();
                    System.out.println(index + " is " + key.toString() + " away");
                    count++;
                    if (count > k) {// don't count this point
                        break;
                    }
                }

                //construct circle that includes those neighbours
                Circle circle = new Circle((Point) feature.getDefaultGeometry(), radius);
                double popCount = 0;
                double canCount = 0;
                Filter filter = ff.within(popgeom, ff.literal(circle.toPolygon()));
                // System.out.println(filter);
                // get pop points in circle
                SimpleFeatureCollection popPoints = pop.subCollection(filter);
                if (popPoints.size() > 0) {
                    // System.out.println(circle + " got " + popPoints.size()
                    // + " pop points");
                    popIt = popPoints.features();
                    while (popIt.hasNext()) {
                        SimpleFeature feature2 = popIt.next();
                        Object evaluate = popattribute.evaluate(feature2);
                        // System.out.println(evaluate);
                        Number count2 = (Number) evaluate;
                        popCount += (count2.doubleValue() * overrat);
                        if (sharedData) {
                            evaluate = canattribute.evaluate(feature2);
                            // System.out.println(evaluate);
                            count2 = (Number) evaluate;
                            canCount += count2.doubleValue();
                        }
                    }
                    // System.out.println("\tContaining " + popCount + " people");
                    // System.out.println("\tContaining " + popCount + " people");
                }
                filter = ff.within(cangeom, ff.literal(circle.toPolygon()));
                // System.out.println(filter);
                // get pop points in circle
                if (!sharedData) {
                    SimpleFeatureCollection canPoints = justCan.subCollection(filter);
                    if (canPoints.size() > 0) {
                        // System.out.println(circle + " got " + canPoints.size()
                        // + " case points");
                        canIt = canPoints.features();
                        while (canIt.hasNext()) {
                            SimpleFeature feature2 = canIt.next();
                            final Object evaluate = canattribute.evaluate(feature);
                            // System.out.println(evaluate);
                            Number count2 = (Number) evaluate;
                            canCount += count2.doubleValue();
                        }
                        // System.out.println("\tContaining " + canCount +
                        // " cases");
                        // System.out.println("\tContaining " + canCount +
                        // " cases");
                    } // canPoints > 0
                } // sharedData
                //process circle
                if (test.isWorthTesting(popCount, canCount)) {
                    if (test.isSignificant(popCount, canCount)) {
                        double stat = test.getStatistic();
                        circle.setStatistic(stat);
                        results.add(circle);
                        // System.out.println(circle + " " + stat);
                    } else {
                        // System.out.println("not significant with " + popCount
                        // + canCount + " cases");
                    }
                } else {
                    // System.out.println("not worth testing " + popCount + " with "
                    // + canCount + " cases");
                }

            }
        } finally {
            if(it!=null)it.close();
            if(canIt!=null)canIt.close();
            if(popIt!=null)popIt.close();
        }
        return results;
    }

    @Override
    void processParameters(Map<String, Object> input) throws IllegalArgumentException, ClusterException {
        pop = (SimpleFeatureCollection) input.get(ClusterMethodFactory.POPULATION.key);
        String attributeStr = (String) input.get(ClusterMethodFactory.POPATTRIBUTE.key);
        popattribute = null;
        try {
            popattribute = ECQL.toExpression(attributeStr);
        } catch (CQLException e) {
            throw new ClusterException(e);
        }
        can = (SimpleFeatureCollection) input.get(ClusterMethodFactory.CANCER.key);
        attributeStr = (String) input.get(ClusterMethodFactory.CANATTRIBUTE.key);
        canattribute = null;
        try {
            canattribute = ECQL.toExpression(attributeStr);
        } catch (CQLException e) {
            throw new ClusterException(e);
        }
        if (pop == can) {
            System.out.println("identical inputs!");
            sharedData = true;
        }
        k = ((Integer) input.get(ClusterMethodFactory.NONEIGHBOURS.key)).intValue();

        String testName = input.get(ClusterMethodFactory.TESTNAME.key).toString();
        // switch the statistic name (when we have more tests)
        if ("Poisson".equalsIgnoreCase(testName)) {
            test = new PoissonTest(input);
        } else {
            if (testName.length() > 0) {
                throw new IllegalArgumentException("Unknown statistical test " + testName);
            } else {
                throw new IllegalArgumentException("No Statistical test specified");
            }
        }
    }
}
