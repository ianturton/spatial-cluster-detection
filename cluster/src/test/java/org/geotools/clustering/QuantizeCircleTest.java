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

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;

import junit.framework.TestCase;

import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.coverage.grid.GridGeometry2D;
import org.geotools.feature.FeatureCollection;
import org.geotools.geometry.DirectPosition2D;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.referencing.CRS;
import org.opengis.coverage.grid.GridEnvelope;
import org.opengis.geometry.DirectPosition;
import org.opengis.geometry.Envelope;
import org.opengis.geometry.MismatchedDimensionException;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.NoSuchAuthorityCodeException;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Point;
import java.io.File;
import org.geotools.clustering.utils.Utilities;
import org.geotools.coverage.grid.GridCoordinates2D;
import org.geotools.coverage.grid.ViewType;
import org.geotools.gce.geotiff.GeoTiffWriter;
import org.geotools.test.TestData;
import org.opengis.filter.expression.Add;
import org.opengis.referencing.operation.TransformException;

/**
 * @author ijt1 
 *
 */
public class QuantizeCircleTest extends TestCase {

    static boolean setup = false;
    static ArrayList<Circle> results = new ArrayList<Circle>();
    static double expectedTotal = 0;
    static QuantizeCircle qc;
    static ReferencedEnvelope resBounds;

    @Override
    protected void setUp() throws Exception {
        // TODO Auto-generated method stub
        if (!setup) {
            super.setUp();
            setup = true;
            Circle[] test = {
                new Circle(426625.0, 562805.0, 4511.0, 15.0),
                new Circle(434538.608998373, 574268.198719409, 3494.94915845955, 7.5)/*,
            new Circle(426624.88935463, 562804.920944528, 4510.91327941836, 15.0)
            new Circle(426500,565000,7000.0,15.0),
            new Circle(432500,570000,5000.0,10)/*,
            new Circle(432500,575000,5000.0,7.536707656508128),
            new Circle(437500,575000,5000.0,7.155322144156536),
            new Circle(422500,561000,6000.0,17.47980612854911),
            new Circle(428500,561000,6000.0,20.530726310849985),
            new Circle(440500,573000,6000.0,6.144735065920152),
            new Circle(427500,560000,7000.0,19.851710195607026),
            new Circle(441500,574000,7000.0,5.624576549011038),
            new Circle(424500,565000,8000.0,23.40651937750293),
            new Circle(432500,565000,8000.0,21.44171643086265),
            new Circle(419500,561000,9000.0,20.47783913040685),
            new Circle(428500,561000,9000.0,22.956439990486373),
            new Circle(422500,565000,10000.0,27.091876811920287),
            new Circle(442500,575000,10000.0,10.150218233944216)*/

            };
            System.out.println("testing with " + test.length + " circles");
            results.addAll(Arrays.asList(test));
            resBounds = new ReferencedEnvelope(CRS.decode("EPSG:27700"));

            for (Circle c : results) {
                expectedTotal += c.getStatistic();
                resBounds.expandToInclude(c.getBounds());
            }


            final double scale = 100.0;

            qc = new QuantizeCircle(resBounds, scale);
        }
    }

    /**
     * Test method for {@link org.geotools.clustering.QuantizeCircle#quantize(org.geotools.clustering.Circle)}.
     * @throws FactoryException 
     * @throws NoSuchAuthorityCodeException 
     * @throws MismatchedDimensionException 
     */
    public void testQuantize() throws MismatchedDimensionException, NoSuchAuthorityCodeException, FactoryException, TransformException {



        GridCoverage2D grid = qc.processCircles(results);
        File out;
        try {
            File outDir = TestData.file(this, null);
            out = new File(outDir, "quantizeCircleOut.tiff");
            Utilities.writeGrid(out, grid);
            out = new File(outDir, "quantizeCircleOut.shp");
            FeatureCollection circles = Utilities.circles2FeatureCollection(results, resBounds.getCoordinateReferenceSystem());
            Utilities.writeCircles(out, circles);
        } catch (FileNotFoundException ex) {
            Logger.getLogger(QuantizeCircleTest.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(QuantizeCircleTest.class.getName()).log(Level.SEVERE, null, ex);
        }

        final Envelope envelope = grid.getEnvelope();
        System.out.println(envelope);
        double delta = 1e-2;


        double[] res = new double[1];

        GridGeometry2D gg = grid.getGridGeometry();
        GridEnvelope r = gg.getGridRange();
        int startX = r.getLow(0);
        int endX = r.getHigh(0);
        int startY = r.getLow(1);
        int endY = r.getHigh(1);

        DirectPosition c;

        double total = 0.0;
        for (int i = startX; i < endX; i++) {
            for (int j = startY; j < endY; j++) {
                c = gg.gridToWorld(new GridCoordinates2D(i, j));
                grid.evaluate(c, res);
                if (!Double.isNaN(res[0]) && !Double.isInfinite(res[0])) {
                    total += res[0];
                }
                if (i == 100) {
                    System.out.println(j + " " + res[0]);
                }
            }
        }
        /**/
        System.out.println("Total under the kernel is " + total);
        assertEquals("Wrong ammount under the kernal", expectedTotal, total, delta);
    }

    public void testAddToCell() {
        double cellsize = qc.getCellsize();
        GridCoordinates2D g;
        for (Circle c : results) {
            double radius = c.getRadius();

            double rsq = radius * radius;
            final double w = Math.ceil((radius + cellsize / 2.0) / cellsize) * cellsize;
            double min = -w;//-radius - cellsize / 2.0;
            double max = w;//radius + cellsize / 2.0;
            //System.out.println(min + " " + max);
            int numb = (int) (Math.ceil((2.0 * (radius + cellsize))) / cellsize);

            double[] xker = new double[numb * numb];
            double[] xadr = new double[numb * numb];
            double[] yadr = new double[numb * numb];
            double lastX = 0;
            double yy, xx, xxsq, dis, sum;
            xx = min;
            int m = 0;
            sum = 0;
            int lastI = 0;
            int lastJ = 0;
            double x = c.getCentre().getX();
            x = Math.ceil((x + cellsize / 2.0) / cellsize) * cellsize;
            double y = c.getCentre().getY();
            y = Math.floor((y + cellsize / 2.0) / cellsize) * cellsize;
            for (int i = 0; i <= numb; i++) {
                yy = max;
                xxsq = xx * xx;
                for (int j = 0; j <= numb; j++) {
                    dis = xxsq + yy * yy;
                    if (dis <= rsq) { // 1_{|u|<=1} scaled by Radius of circle

                        g = qc.addToCell(x + xx, y + yy, sum);
                        if (g.x > lastI && Math.abs(lastI - g.x) > 1) {
                            System.out.println("skipped col " + lastI + " to " + g.x);
                            System.out.println("xx " + xx + " x " + x + " lastX " + lastX);
                        }
                        if (g.y > lastJ && Math.abs(lastJ - g.y) > 1) {
                            System.out.println("skipped row " + lastJ + " to " + g.y);
                        }
                        lastI = g.x;
                        lastJ = g.y;
                    }
                    yy -= cellsize;

                }
                lastX = xx;
                xx += cellsize;
                //lastI=0;
            }
        }
    }
}
