/*
 * Copyright 2009 Michael Bedward
 *
 * This file is part of jai-tools.

 * jai-tools is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.

 * jai-tools is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.

 * You should have received a copy of the GNU Lesser General Public
 * License along with jai-tools.  If not, see <http://www.gnu.org/licenses/>.
 *
 */
package org.geotools.clustering.utils;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.MultiPoint;
import jaitools.CollectionFactory; 
import jaitools.media.jai.kernel.KernelFactory;

import java.awt.Point;
import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.PathIterator;
import java.awt.geom.Point2D;
import java.util.List;
import javax.media.jai.KernelJAI;

/**
 * A factory class with static methods to create a variety of KernelJAI objects with specified
 * geometries
 * 
 * @author Michael Bedward
 * @since 1.0
 * @version $Id: KernelFactory.java 1383 2011-02-10 11:22:29Z michael.bedward $
 */
public class ClusterKernelFactory extends KernelFactory{

    /**
     * Meaning of the kernel values
     */
    public static enum ValueType {

        /**
         * Simple binary kernel with values of 1 or 0
         */
        BINARY,
        /**
         * The value of each kernel element is its distance to the kernel's key element (the element
         * that is placed over image pixels during kernel operations).
         */
        DISTANCE,
        /**
         * The value of each kernel element is the inverse distance to the kernel's key element (the
         * element that is placed over image pixels during kernel operations).
         */
        INVERSE_DISTANCE,
        /**
         * K(u) = 3/4(1-u^2)1_{|u|<=1}
         */
        EPANECHNIKOV; 
    };
    private static final float FTOL = 1.0e-8f;
    /**
     * Compare to float values allowing for a fixed round-off tolerance
     * @param f1 first value
     * @param f2 second value
     * @return a value < 0 if f1 < f2; 0 if f1 == f2; a value > 0 if f1 > f2
     */
    static int fcomp(float f1, float f2) {
        if (Math.abs(f1 - f2) < FTOL) {
            return 0;
        } else {
            return Float.compare(f1, f2);
        }
    }

    /**
     * Creates a new KernelJAI object with a circular configuration. The kernel width is 2*radius +
     * 1. The kernel's key element is at position x=radius, y=radius
     * 
     * @param radius
     *            the radius of the circle expressed in pixels
     * 
     * @param type
     *            one of {@linkplain ValueType#BINARY}, {@linkplain ValueType#DISTANCE},
     *            {@linkplain ValueType#INVERSE_DISTANCE} or {@linkplain ValueType#EPANECHNIKOV}
     * 
     * @param centreValue
     *            the value to assign to the kernel centre (key element)
     * 
     * @return a new instance of KernelJAI
     */
    public static KernelJAI createCircle(int radius, ValueType type, float centreValue) {

        if (radius <= 0) {
            throw new IllegalArgumentException("Invalid radius (" + radius + "); must be > 0");
        }

        //KernelFactoryHelper kh = new KernelFactoryHelper();

        int width = 2 * radius + 1;
        float[] weights = new float[width * width];

        float r2 = radius * radius;
        int k0 = 0;
        int k1 = weights.length - 1;
        float sum = 0.0f;
        for (int y = radius; y > 0; y--) {
            int y2 = y * y;
            for (int x = -radius; x <= radius; x++, k0++, k1--) {
                float dist2 = x * x + y2;
                float value = 0f;

                if (fcomp(r2, dist2) >= 0) {
                    if (type == ValueType.EPANECHNIKOV) {
                        value = 1.0f - dist2 / r2; // 1- u^2
                        sum += value;
                    } else if (type == ValueType.DISTANCE) {
                        value = (float) Math.sqrt(dist2);
                    } else if (type == ValueType.INVERSE_DISTANCE) {
                        value = 1.0f / (float) Math.sqrt(dist2);
                    } else {
                        value = 1.0f;
                    }

                    weights[k0] = weights[k1] = value;
                }
            }
        }

        for (int x = -radius; x <= radius; x++, k0++) {
            float value;
            if (x == 0) {
                value = centreValue;
            } else {
                if (type == ValueType.EPANECHNIKOV) {
                    value = 1.0f - (x * x) / r2; // 1- u^2
                    sum += value;
                } else if (type == ValueType.DISTANCE) {
                    value = (float) Math.sqrt(x * x);
                } else if (type == ValueType.INVERSE_DISTANCE) {
                    value = 1.0f / (float) Math.sqrt(x * x);
                } else {
                    value = 1.0f;
                }
            }
            weights[k0] = value;
        }
        if (type == ValueType.EPANECHNIKOV) {
            if (sum > 0.0) {
                sum = 1.0f / sum;
            }
            for (int j = 0; j < weights.length; j++) {
                weights[j] *= sum;
            }
        }
        return new KernelJAI(width, width, weights);
    }

    
}
