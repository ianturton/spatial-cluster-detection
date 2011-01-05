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
import java.util.HashMap;
import java.util.Map;

import org.geotools.clustering.significance.PoissonTest;
import org.geotools.clustering.significance.SignificanceTest;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.filter.text.cql2.CQLException;
import org.geotools.filter.text.ecql.ECQL;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.process.ProcessException;
import org.geotools.process.ProcessFactory;
import org.geotools.process.impl.AbstractProcess;
import org.geotools.text.Text;
import org.geotools.util.NullProgressListener;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.filter.Filter;
import org.opengis.filter.FilterFactory2;
import org.opengis.filter.expression.Expression;
import org.opengis.filter.expression.PropertyName;
import org.opengis.geometry.BoundingBox;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.util.ProgressListener;

/**
 * @author ijt1
 * 
 */
public class GamProcess extends AbstractProcess {
    boolean started = false;

    private FilterFactory2 ff;

    private double overrat;

    private ReferencedEnvelope bounds;

    private boolean sharedData = false;

    public GamProcess(ClusterMethodFactory factory) {
        super(factory);
        ff = CommonFactoryFinder.getFilterFactory2(null);

    }

    public ProcessFactory getFactory() {
        return factory;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.geotools.process.Process#execute(java.util.Map, org.opengis.util.ProgressListener)
     */
    public Map<String, Object> execute(Map<String, Object> input, ProgressListener monitor)
            throws ProcessException {
        if (started)
            throw new IllegalStateException("Process can only be run once");
        started = true;
        ArrayList<Circle> results = new ArrayList<Circle>();
        if (monitor == null)
            monitor = new NullProgressListener();

        try {
            monitor.started();
            monitor.setTask(Text.text("Grabbing arguments"));
            monitor.progress(5.0f);
            SimpleFeatureCollection pop = (SimpleFeatureCollection) input
                    .get(ClusterMethodFactory.POPULATION.key);
            CoordinateReferenceSystem popCrs = pop.getBounds().getCoordinateReferenceSystem();
            String attributeStr = (String) input.get(ClusterMethodFactory.POPATTRIBUTE.key);
            Expression popattribute = null;
            try {
                popattribute = ECQL.toExpression(attributeStr);
            } catch (CQLException e) {
                throw new ClusterException(e);
            }
            SimpleFeatureCollection can = (SimpleFeatureCollection) input
                    .get(ClusterMethodFactory.CANCER.key);
            CoordinateReferenceSystem canCrs = can.getBounds().getCoordinateReferenceSystem();
            attributeStr = (String) input.get(ClusterMethodFactory.CANATTRIBUTE.key);
            Expression canattribute = null;
            try {
                canattribute = ECQL.toExpression(attributeStr);
            } catch (CQLException e) {
                throw new ClusterException(e);
            }
            if (pop == can) {
                System.out.println("identical inputs!");
                sharedData = true;
            }
            double minRadius = ((Double) input.get(ClusterMethodFactory.MINRAD.key)).doubleValue();
            double maxRadius = ((Double) input.get(ClusterMethodFactory.MAXRAD.key)).doubleValue();
            double radiusStep = ((Double) input.get(ClusterMethodFactory.STEP.key)).doubleValue();
            double overlap = ((Double) input.get(ClusterMethodFactory.OVERLAP.key)).doubleValue();
            String testName = input.get(ClusterMethodFactory.TESTNAME.key).toString();
            // switch the statistic name (when we have more tests)
            SignificanceTest test;
            if ("Poisson".equalsIgnoreCase(testName)) {
                test = new PoissonTest(input);
            } else {
                if (testName.length() > 0) {
                    throw new IllegalArgumentException("Unknown statistical test " + testName);
                } else {
                    throw new IllegalArgumentException("No Statistical test specified");
                }
            }
            monitor.setTask(Text.text("Pre-Processing Data"));
            monitor.progress(5.0f);
            SimpleFeatureIterator popIt = pop.features();
            double totalPop = 0.0;
            while (popIt.hasNext()) {
                SimpleFeature feature = popIt.next();
                final Object evaluate = popattribute.evaluate(feature);
                // System.out.println(evaluate);
                Number count = (Number) evaluate;
                totalPop += count.doubleValue();
            }
            SimpleFeatureIterator canIt = can.features();
            double totalCan = 0.0;
            while (canIt.hasNext()) {
                SimpleFeature feature = canIt.next();
                final Object evaluate = canattribute.evaluate(feature);
                // System.out.println(evaluate);
                Number count = (Number) evaluate;
                totalCan += count.doubleValue();
            }
            overrat = (double) totalCan / (double) totalPop;
            monitor.setTask(Text.text("Processing Data"));
            monitor.progress(10.0f);
            bounds = pop.getBounds();

            // extend bounds by 1/2 max rad in all directions
            final double halfMaxRadius = maxRadius / 2.0;
            double minX = bounds.getMinX() - halfMaxRadius;
            double minY = bounds.getMinY() - halfMaxRadius;
            double maxX = bounds.getMaxX() + halfMaxRadius;
            double maxY = bounds.getMaxY() + halfMaxRadius;
            int loopCount = 0;
            for (double radius = minRadius; radius <= maxRadius; radius += radiusStep) {
                for (double x = minX; x <= maxX; x += radius * overlap) {
                    for (double y = minY; y <= maxY; y += radius * overlap) {
                        loopCount++;
                    }
                }
            }
            int totalLoops = loopCount;
            System.out.println("About to do " + totalLoops + " circles");
            loopCount = 0;
            PropertyName popgeom = ff.property(pop.getSchema().getGeometryDescriptor().getName());
            PropertyName cangeom = ff.property(can.getSchema().getGeometryDescriptor().getName());
            bounds = new ReferencedEnvelope(minX, maxX, minY, maxY,
                    bounds.getCoordinateReferenceSystem());
            for (double radius = minRadius; radius <= maxRadius; radius += radiusStep) {
                System.out.println("radius = " + radius + "\n min/max R" + minRadius + ","
                        + maxRadius);
                monitor.setTask(Text.text("Radius " + radius));
                float i = (float) loopCount / (float) totalLoops;
                monitor.progress(10.0f + (80.0f * i));
                for (double x = minX; x <= maxX; x += radius * overlap) {

                    SimpleFeatureCollection popsubset;
                    SimpleFeatureCollection cansubset = null;
                    boolean fast = false; // oddly this slows things down
                    if (fast) {
                        Filter bbox = ff.bbox("the_geom", x - radius, minY, x + radius, maxY,
                                "EPSG:27700");
                        // System.out.println("applying " + bbox);
                        popsubset = pop.subCollection(bbox);

                        if (!sharedData) {
                            cansubset = can.subCollection(bbox);
                        }
                    } else {
                        popsubset = pop;
                        cansubset = null;
                        if (!sharedData) {
                            cansubset = can;
                        }
                    }
                    // System.out.println("got " + subset.size());
                    if (popsubset.size() > 0) {

                        for (double y = minY; y <= maxY; y += radius * overlap) {
                            loopCount++;
                            i = (float) loopCount / (float) totalLoops;
                            if (loopCount % 1000 == 0)
                                monitor.progress(10.0f + (80.0f * i));
                            Circle circle = new Circle(x, y, radius);
                            double popCount = 0;
                            double canCount = 0;
                            boolean fast2 = true;
                            if (fast2) {
                                Filter filter = ff.within(popgeom, ff.literal(circle.toPolygon()));
                                // System.out.println(filter);
                                // get pop points in circle
                                SimpleFeatureCollection popPoints = popsubset.subCollection(filter);

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
                                    SimpleFeatureCollection canPoints = cansubset
                                            .subCollection(filter);

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
                                        // System.out.println("\tContaining " + canCount +
                                        // " cases");
                                    }// canPoints > 0
                                }// sharedData
                            } else {// fast2
                                BoundingBox bbox = new ReferencedEnvelope(circle.getBounds(),
                                        popCrs);
                                Filter filter = ff.bbox(popgeom, bbox);
                                // System.out.println(filter);
                                // get pop points in circle
                                SimpleFeatureCollection popPoints = popsubset.subCollection(filter);

                                if (popPoints.size() > 0) {
                                  //System.out.println(circle + " got " + popPoints.size() + " pop points");
                                    popIt = popPoints.features();

                                    while (popIt.hasNext()) {
                                        SimpleFeature feature = popIt.next();
                                        Filter filter2 = ff.within(popgeom,
                                                ff.literal(circle.toPolygon()));
                                        if (filter2.evaluate(feature)) {
                                            Object evaluate = popattribute.evaluate(feature);
                                            //System.out.println(evaluate);
                                            Number count = (Number) evaluate;
                                            popCount += (count.doubleValue() * overrat);
                                            if (sharedData) {
                                                evaluate = canattribute.evaluate(feature);
                                                // System.out.println(evaluate);
                                                count = (Number) evaluate;
                                                canCount += count.doubleValue();
                                            }
                                        }
                                    }
                                    // System.out.println("\tContaining " + popCount + " people");
                                }

                                // System.out.println(filter);
                                // get pop points in circle
                                if (!sharedData) {
                                    bbox = new ReferencedEnvelope(circle.getBounds(), canCrs);
                                    filter = ff.bbox(cangeom, bbox);
                                    SimpleFeatureCollection canPoints = cansubset
                                            .subCollection(filter);

                                    if (canPoints.size() > 0) {
                                        // System.out.println(circle + " got " + canPoints.size()
                                        // + " case points");
                                        canIt = canPoints.features();

                                        while (canIt.hasNext()) {
                                            SimpleFeature feature = canIt.next();
                                            Filter filter2 = ff.within(cangeom,
                                                    ff.literal(circle.toPolygon()));
                                            if (filter2.evaluate(feature)) {
                                                final Object evaluate = canattribute
                                                        .evaluate(feature);
                                                // System.out.println(evaluate);
                                                Number count = (Number) evaluate;
                                                canCount += count.doubleValue();
                                            }
                                        }
                                        // System.out.println("\tContaining " + canCount +
                                        // " cases");
                                    }// canPoints > 0
                                }// sharedData
                            }
                            if (test.isWorthTesting(popCount, canCount)) {
                                if (test.isSignificant(popCount, canCount)) {
                                    double stat = test.getStatistic();
                                    circle.setStatistic(stat);
                                    results.add(circle);
                                    // System.out.println(circle + " " + stat);
                                } else {
                                    // System.out.println("not significant with " + popCount
                                    // + " with " + canCount + " cases");
                                }
                            } else {
                                // System.out.println("not worth testing " + popCount + " with "
                                // + canCount + " cases");
                            }
                        }// Y loop
                    }
                }// X loop
            }
            if (monitor.isCanceled()) {
                System.err.println("user cancel");
                return null; // user has canceled this operation
            }

            // Geometry resultGeom = geom1.buffer(buffer);

            monitor.setTask(Text.text("Encoding result"));
            monitor.progress(90.0f);
            GridCoverage2D cov = convert(results);
            Map<String, Object> result = new HashMap<String, Object>();
            result.put(ClusterMethodFactory.RESULT.key, cov);
            monitor.complete(); // same as 100.0f

            return result;
        } catch (Exception eek) {
            System.err.println(eek);
            monitor.exceptionOccurred(eek);
            return null;
        } finally {
            monitor.dispose();
        }
    }

    /**
     * @param results2
     * @return
     */
    private GridCoverage2D convert(ArrayList<Circle> results) {
        ReferencedEnvelope resBounds = new ReferencedEnvelope(bounds.getCoordinateReferenceSystem());
        for (Circle c : results) {
            resBounds.expandToInclude(c.getBounds());
        }
        System.out.println(resBounds);

        final double scale = 100.0;

        QuantizeCircle qc = new QuantizeCircle(resBounds, scale);

        return qc.processCircles(results);
    }

}