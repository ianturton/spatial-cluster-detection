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

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.geotools.clustering.significance.PoissonTest;
import org.geotools.clustering.significance.SignificanceTest;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.coverage.grid.GridCoverageBuilder;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.filter.LiteralExpression;
import org.geotools.filter.text.cql2.CQLException;
import org.geotools.filter.text.ecql.ECQL;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.process.ProcessException;
import org.geotools.process.ProcessFactory;
import org.geotools.process.feature.AbstractFeatureCollectionProcessFactory;
import org.geotools.process.impl.AbstractProcess;
import org.geotools.process.raster.VectorToRasterException;
import org.geotools.process.raster.VectorToRasterFactory;
import org.geotools.text.Text;
import org.geotools.util.NullProgressListener;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.type.Name;
import org.opengis.filter.Filter;
import org.opengis.filter.FilterFactory;
import org.opengis.filter.FilterFactory2;
import org.opengis.filter.expression.Expression;
import org.opengis.filter.expression.Literal;
import org.opengis.filter.expression.PropertyName;
import org.opengis.util.ProgressListener;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Point;

/**
 * @author ijt1
 * 
 */
public class GamProcess extends AbstractProcess {
    boolean started = false;

    static final private GeometryFactory gf = new GeometryFactory();

    

    private FilterFactory2 ff;

    private double overrat;

    private ReferencedEnvelope bounds;

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
            String attributeStr = (String) input.get(ClusterMethodFactory.POPATTRIBUTE.key);
            Expression popattribute = null;
            try {
                popattribute = ECQL.toExpression(attributeStr);
            } catch (CQLException e) {
                throw new ClusterException(e);
            }
            SimpleFeatureCollection can = (SimpleFeatureCollection) input
                    .get(ClusterMethodFactory.CANCER.key);
            attributeStr = (String) input.get(ClusterMethodFactory.CANATTRIBUTE.key);
            Expression canattribute = null;
            try {
                canattribute = ECQL.toExpression(attributeStr);
            } catch (CQLException e) {
                throw new ClusterException(e);
            }

            double minRadius = ((Double) input.get(ClusterMethodFactory.MINRAD.key)).doubleValue();
            double maxRadius = ((Double) input.get(ClusterMethodFactory.MAXRAD.key)).doubleValue();
            double radiusStep = ((Double) input.get(ClusterMethodFactory.STEP.key)).doubleValue();

            String testName = input.get(ClusterMethodFactory.TESTNAME.key).toString();
            // switch the statistic name (when we have more tests)
            SignificanceTest test = new PoissonTest(input);
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
            PropertyName popgeom = ff.property(pop.getSchema().getGeometryDescriptor().getName());
            PropertyName cangeom = ff.property(can.getSchema().getGeometryDescriptor().getName());
            bounds = new ReferencedEnvelope(minX, maxX, minY, maxY,
                    bounds.getCoordinateReferenceSystem());
            for (double radius = minRadius; radius <= maxRadius; radius += radiusStep) {
                System.out.println("radius = " + radius + "\n min/max R" + minRadius + ","
                        + maxRadius);
                for (double x = minX; x <= maxX; x += radius) {
                    Filter bbox = ff.bbox("the_geom", x - radius, minY, x + radius, maxY,
                            "EPSG:27700");
                    // System.out.println("applying " + bbox);
                    SimpleFeatureCollection popsubset = pop.subCollection(bbox);
                    SimpleFeatureCollection cansubset = can.subCollection(bbox);
                    // System.out.println("got " + subset.size());
                    if (popsubset.size() > 0) {

                        for (double y = minY; y <= maxY; y += radius) {
                            Circle circle = new Circle(x, y, radius);
                            double popCount = 0;
                            double canCount = 0;
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
                                    final Object evaluate = popattribute.evaluate(feature);
                                    // System.out.println(evaluate);
                                    Number count = (Number) evaluate;
                                    popCount += (count.doubleValue() * overrat);
                                }
                                // System.out.println("\tContaining " + popCount + " people");
                            }
                            filter = ff.within(cangeom, ff.literal(circle.toPolygon()));
                            // System.out.println(filter);
                            // get pop points in circle
                            SimpleFeatureCollection canPoints = cansubset.subCollection(filter);

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
                                // System.out.println("\tContaining " + canCount + " cases");
                            }// canPoints > 0
                            if (test.isWorthTesting(popCount, canCount)) {
                                if (test.isSignificant(popCount, canCount)) {
                                    double stat = test.getStatistic();
                                    circle.setStatistic(stat);
                                    results.add(circle);
                                } else {
                                    //System.out.println("not significant with " + popCount
                                      //      + " with " + canCount + " cases");
                                }
                            } else {
                                //System.out.println("not worth testing " + popCount + " with "
                                  //      + canCount + " cases");
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
            GridCoverage2D cov = convert(results);
            monitor.setTask(Text.text("Encoding result"));
            monitor.progress(90.0f);

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
        for(Circle c:results) {
            resBounds.expandToInclude(c.getBounds());
        }
        System.out.println(resBounds);
        GridCoverageBuilder builder = new GridCoverageBuilder();
        builder.setEnvelope(bounds);
        builder.setCoordinateReferenceSystem(bounds.getCoordinateReferenceSystem());
        final double scale = 1000.0;
        final int width = (int)(resBounds.getWidth()/scale);
        final int height = (int)(resBounds.getHeight()/scale);
        builder.setImageSize(width, height);
        BufferedImage img = builder.getBufferedImage();
        QuantizeCircle qc = new QuantizeCircle(img);
        qc.setCellsize(scale);
        for(Circle c:results) {
            qc.quantize(c);
        }
        return builder.getGridCoverage2D();
    }

    

}
