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
import java.util.Arrays;

import junit.framework.TestCase;

import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.coverage.grid.GridGeometry2D;
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
import org.geotools.coverage.grid.GridCoordinates2D;
import org.geotools.coverage.grid.ViewType;
import org.opengis.referencing.operation.TransformException;

/**
 * @author ijt1 
 *
 */
public class QuantizeCircleTest extends TestCase {

    static boolean setup = false;
    static ArrayList<Circle> results = new ArrayList<Circle>();

    @Override
    protected void setUp() throws Exception {
        // TODO Auto-generated method stub
        if (!setup) {
            super.setUp();
            Circle[] test = {
                new Circle(420000, 560000, 5000.0, 15.0)/*,
            new Circle(427500,565000,5000.0,15.542833634382614),
            new Circle(432500,570000,5000.0,11.402839291109292),
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
            results.addAll(Arrays.asList(test));

        }
    }

    /**
     * Test method for {@link org.geotools.clustering.QuantizeCircle#quantize(org.geotools.clustering.Circle)}.
     * @throws FactoryException 
     * @throws NoSuchAuthorityCodeException 
     * @throws MismatchedDimensionException 
     */
    public void testQuantize() throws MismatchedDimensionException, NoSuchAuthorityCodeException, FactoryException, TransformException {
        ReferencedEnvelope resBounds = new ReferencedEnvelope(CRS.decode("EPSG:27700"));
        /*for (Circle c : results) {
        resBounds.expandToInclude(c.getBounds());
        }*/
        final Circle circle = results.get(0);
        Point ll = circle.centre;
        Coordinate coord1 = (Coordinate) ll.getCoordinate().clone();
        coord1.x = coord1.x + 10000;
        coord1.y = coord1.y + 10000;
        Coordinate coord2 = (Coordinate) ll.getCoordinate().clone();
        coord2.x = coord2.x - 10000;
        coord2.y = coord2.y - 10000;
        resBounds.expandToInclude(coord1);
        resBounds.expandToInclude(coord2);
        System.out.println(resBounds);

        final double scale = 100.0;

        QuantizeCircle qc = new QuantizeCircle(resBounds, scale);


        GridCoverage2D grid = qc.processCircles(results);
        final Envelope envelope = grid.getEnvelope();
        System.out.println(envelope);
        double delta = 1e-4;
        assertEquals(410000.0, envelope.getMinimum(0), delta);
        assertEquals(550000.0, envelope.getMinimum(1), delta);
        assertEquals(430000.0, envelope.getMaximum(0), delta);
        assertEquals(570000.0, envelope.getMaximum(1), delta);

        
        double[] res = new double[1];
        
        GridGeometry2D gg = grid.getGridGeometry();
        GridEnvelope r = gg.getGridRange();
        int startX = r.getLow(0);
        int endX = r.getHigh(0);
        int startY = r.getLow(1);
        int endY = r.getHigh(1);
       
        DirectPosition c;
        grid.view(ViewType.NATIVE);
        double total = 0.0;
        for (int i = startX; i < endX; i++) {
            for (int j = startY; j < endY; j++) {
                c = gg.gridToWorld(new GridCoordinates2D(i, j));
                grid.evaluate(c, res);
                if(!Double.isNaN(res[0])&&!Double.isInfinite(res[0]))total += res[0];
                //if(i==100)System.out.println(j + " " + res[0]);
            }
        }
        /**/
        System.out.println("Total under the kernel is "+total);
        assertEquals("Wrong ammount under the kernal", 15.0, total, delta);
    }
}
