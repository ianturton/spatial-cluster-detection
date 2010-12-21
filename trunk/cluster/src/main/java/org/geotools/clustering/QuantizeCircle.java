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
import java.awt.image.DataBuffer;
import java.awt.image.WritableRaster;
import java.util.ArrayList;

import javax.media.jai.RasterFactory;

import jj2000.j2k.entropy.encoder.EBCOTRateAllocator;

import org.geotools.coverage.CoverageFactoryFinder;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.coverage.grid.GridCoverageFactory;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import com.vividsolutions.jts.geom.Envelope;

/**
 * @author ijt1
 * 
 */
public class QuantizeCircle {

    private Envelope env;

    /**
     * produce a kernel density surface at x,y with radius r and height value after Epanechnikov
     * (1969) and Brunsdon (1990)
     * 
     * K(u) = 3/4(1-u^2)1_{|u|<=1}
     */

    /**
     * @param scale
     * @param
     * 
     */
    public QuantizeCircle(Envelope env, double cellSize) {
        this.env = env;
        cellsize = cellSize;
        System.out.println("quantizing an envelope of " + env + " with a cell size of " + cellsize);
        height = (int) Math.ceil((env.getHeight() / cellsize));
        width = (int) Math.ceil((env.getWidth() / cellsize));
        System.out.println("map width " + env.getWidth() + " height " + env.getHeight());
        System.out.println("this gives a width of " + width + " and height of " + height);
        data = new double[height][width];
    }

    double lastRadius = -1;

    int m;

    int numb;

    double xker[];

    double xadr[];

    double yadr[];

    private double cellsize = 1.0;

    private boolean quantizeOn = true;

    private double[][] data;

    private int width;

    private int height;

    public double getCellsize() {
        return cellsize;
    }

    public void setCellsize(double cellsize) {
        this.cellsize = cellsize;
    }

    public boolean isQuantizeOn() {
        return quantizeOn;
    }

    public void setQuantizeOn(boolean quantizeOn) {
        this.quantizeOn = quantizeOn;
    }

    public GridCoverage2D processCircles(ArrayList<Circle> results) {
        for (Circle c : results) {
            quantize(c);
        }
        WritableRaster raster = RasterFactory.createBandedRaster(DataBuffer.TYPE_DOUBLE, width,
                height, 1, null);

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                raster.setSample(x, y, 0, data[y][x]);
            }
        }

        GridCoverageFactory gcf = CoverageFactoryFinder.getGridCoverageFactory(null);

        CharSequence name = "Process Results";
        GridCoverage2D grid = gcf.create(name, raster, (org.opengis.geometry.Envelope) env);

        return grid;
    }

    protected final void quantize(Circle c) {
        double radius = c.getRadius();
        double x = c.getCentre().getX();
        double y = c.getCentre().getY();
        double value = c.getStatistic();

        if (radius < (cellsize)) {
            addToCell(x, y, value);
            return;
        }
        if (radius != lastRadius) {
            lastRadius = radius;
            double rsq = radius * radius;
            double min = -radius - cellsize / 2.0;
            double max = radius + cellsize / 2.0;

            numb = (int) (Math.ceil((2.0 * (radius + cellsize))) / cellsize);
            xker = new double[numb * numb];
            xadr = new double[numb * numb];
            yadr = new double[numb * numb];
            double yy, xx, xxsq, dis, sum;
            xx = min;
            m = 0;
            sum = 0;
            for (int i = 0; i <= numb; i++) {
                yy = max;
                xxsq = xx * xx;
                for (int j = 0; j <= numb; j++) {
                    dis = xxsq + yy * yy;
                    if (dis <= rsq) { // 1_{|u|<=1} scaled by Radius of circle
                        // System.out.println("CRa->x "+xx+" y "+yy+" "+dis+" "+rsq);
                        xadr[m] = xx;
                        yadr[m] = yy;
                        xker[m] = 1.0d - dis / rsq; // 1- u^2
                        sum += xker[m];
                        m++;
                    }
                    yy -= cellsize;
                }
                xx += cellsize;
            }
            if (sum > 0.0d) {
                sum = (1.0d / sum);
            }
            for (int j = 0; j < m; j++) {
                xker[j] *= sum;
            }
        }
        // now apply it to the cells
        for (int j = 0; j < m; j++) {
            if (quantizeOn)
                addToCell(x + xadr[j], y + yadr[j], (value * xker[j]));
            else
                addToCell(x + xadr[j], y + yadr[j], value);
        }

    }

    /**
     * @param x
     * @param y
     * @param value
     */
    private void addToCell(double x, double y, double value) {
        int ix = (int) Math.floor(((x - env.getMinX()) / cellsize));
        int iy = (int) Math.floor(((y - env.getMinY()) / cellsize));
        if (ix < 0 || ix >= data[0].length) {
            System.out.println("converting " + x + " gave " + ix);
        } else if (iy < 0 || iy >= data.length) {
            System.out.println("converting " + y + " gave " + iy);
        } else {
            data[iy][ix] += value;
        }
    }
}
