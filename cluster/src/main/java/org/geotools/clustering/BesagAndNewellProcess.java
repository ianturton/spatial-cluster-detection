/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.geotools.clustering;

import java.util.ArrayList;
import java.util.Map;
import java.util.NoSuchElementException;
import org.geotools.clustering.significance.PoissonTest;
import org.geotools.clustering.significance.SignificanceTestException;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.filter.text.cql2.CQLException;
import org.geotools.filter.text.ecql.ECQL;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.filter.Filter;
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
        SimpleFeatureCollection justCan = can;
        Filter filter = ff.greater(canattribute, ff.literal(0.0));
        if (sharedData) {
            justCan = can.subCollection(filter);
        }
        System.out.println("considering " + justCan.size() + " of " + can.size() + " case points");
        //find a case point
        SimpleFeatureIterator it = null;
        try {
            it = justCan.features();
            while (it.hasNext()) {
                SimpleFeature feature = it.next();

                //find K nearest neighbours
                //construct circle that includes those neighbours
                //process circle
                //find K nearest neighbours
                //construct circle that includes those neighbours
                //process circle
            }
        } finally {
            it.close();
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
