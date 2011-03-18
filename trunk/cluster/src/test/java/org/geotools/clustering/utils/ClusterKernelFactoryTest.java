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

package org.geotools.clustering.utils;

import javax.media.jai.KernelJAI;

import junit.framework.TestCase;

/**
 * @author ijt1
 *
 */
public class ClusterKernelFactoryTest extends TestCase {

    /**
     * Test method for {@link org.geotools.clustering.utils.ClusterKernelFactory#createCircle(int, org.geotools.clustering.utils.ClusterKernelFactory.ValueType, float)}.
     */
    public void testCreateCircleIntValueTypeFloat() {
        ClusterKernelFactory.ValueType type = ClusterKernelFactory.ValueType.EPANECHNIKOV; 
        final int radius = 10;
        KernelJAI kernel = ClusterKernelFactory.createCircle(radius, type, 1.0f);
        float[] data = kernel.getKernelData();
        int k = 0;
        for (int i = -radius; i <= radius; i++) {
            for (int j = -radius; j <= radius; j++, k++) {
                System.out.print(data[k]+" ");
                
            }
            System.out.println();
        }
    }

}
