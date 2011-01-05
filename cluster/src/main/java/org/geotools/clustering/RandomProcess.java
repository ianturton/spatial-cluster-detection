/*
 *    GeoTools - The Open Source Java GIS Toolkit
 *    http://geotools.org
 *
 *    (C) 2004-2010, Open Source Geospatial Foundation (OSGeo)
 *
 *    This library is free software; you can redistribute it and/or
 *    modify it under the terms of the GNU Lesser General Public
 *    License as published by the Free Software Foundation;
 *    version 2.1 of the License.
 *
 *    This library is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *    Lesser General Public License for more details.
 */
package org.geotools.clustering;

import java.util.ArrayList;
import java.util.Map;
import java.util.NoSuchElementException;
import org.geotools.clustering.significance.PoissonTest;
import org.geotools.clustering.significance.SignificanceTestException;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.opengis.filter.Filter;
import org.geotools.filter.text.cql2.CQLException;
import org.geotools.filter.text.ecql.ECQL;
import org.geotools.geometry.jts.ReferencedEnvelope;

import org.geotools.process.ProcessException;
import org.geotools.process.ProcessFactory;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.filter.expression.PropertyName;
import org.opengis.geometry.MismatchedDimensionException;
import org.opengis.util.ProgressListener;

/**
 * @author ijt1
 *
 */
public class RandomProcess extends AbstractClusterProcess {

    private boolean sharedData = false;
    private double minRadius;
    private double maxRadius;
    private double numberOfCircles;

    /**
     * @param factory
     */
    protected RandomProcess(ProcessFactory factory) {
        super(factory);
        // TODO Auto-generated constructor stub
    }

    @Override
    ArrayList<Circle> process() throws MismatchedDimensionException, NoSuchElementException, SignificanceTestException {
        ArrayList<Circle> results = new ArrayList<Circle>();
        double radRange = maxRadius - minRadius;
        ReferencedEnvelope env = can.getBounds();
        double width = env.getWidth() - maxRadius;
        double height = env.getHeight() - maxRadius;
        double minX = env.getMinX();
        double minY = env.getMinY();
        PropertyName popgeom = ff.property(pop.getSchema().getGeometryDescriptor().getName());
        PropertyName cangeom = ff.property(can.getSchema().getGeometryDescriptor().getName());
        SimpleFeatureIterator popIt;
        SimpleFeatureIterator canIt;
        for (int i = 0; i < numberOfCircles; i++) {
            double rad = minRadius + Math.random() * radRange;
            double x = minX + Math.random() * width;
            double y = minY + Math.random() * height;
            Circle circle = new Circle(x, y, rad);
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
                    SimpleFeature feature = popIt.next();
                    Object evaluate = popattribute.evaluate(feature);
                    // System.out.println(evaluate);
                    Number count = (Number) evaluate;
                    popCount += (count.doubleValue() * overrat);
                    if (sharedData) {
                        evaluate = canattribute.evaluate(feature);
                        // System.out.println(evaluate);
                        count = (Number) evaluate;
                        canCount += count.doubleValue();
                    }
                }
                // System.out.println("\tContaining " + popCount + " people");
                
            }
            filter = ff.within(cangeom, ff.literal(circle.toPolygon()));
            // System.out.println(filter);
            // get pop points in circle
            if (!sharedData) {
                SimpleFeatureCollection canPoints = can.subCollection(filter);
                if (canPoints.size() > 0) {
                    // System.out.println(circle + " got " + canPoints.size()
                    // + " case points");
                    canIt = canPoints.features();
                    while (canIt.hasNext()) {
                        SimpleFeature feature = canIt.next();
                        final Object evaluate = canattribute.evaluate(feature);
                        // System.out.println(evaluate);
                        Number count = (Number) evaluate;
                        canCount += count.doubleValue();
                    }
                    
                } // canPoints > 0
            } // sharedData
            if (test.isWorthTesting(popCount, canCount)
                    && test.isSignificant(popCount, canCount)) {
                double stat = test.getStatistic();
                circle.setStatistic(stat);
                results.add(circle);
            }
        }
        return results;

    }

    void processParameters(Map<String, Object> input) throws IllegalArgumentException,
            ClusterException {
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
            //System.out.println("identical inputs!");
            sharedData = true;
        }
        minRadius = ((Double) input.get(ClusterMethodFactory.MINRAD.key)).doubleValue();
        maxRadius = ((Double) input.get(ClusterMethodFactory.MAXRAD.key)).doubleValue();
        numberOfCircles = ((Double) input.get(ClusterMethodFactory.NCIRCLES.key)).doubleValue();
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
