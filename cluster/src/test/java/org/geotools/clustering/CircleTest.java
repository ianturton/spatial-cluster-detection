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

import junit.framework.TestCase;

/**
 * @author ijt1
 *
 */
public class CircleTest extends TestCase {

    /**
     * Test method for {@link org.geotools.clustering.Circle#getBounds()}.
     */
    public void testGetBounds() {
        Circle c = new Circle(10, 20, 5);
        System.out.println(c.getBounds());
        c = new Circle(442500, 575000, 10000.0);
        System.out.println(c.getBounds());
    }

}
