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

import java.awt.image.DataBuffer;
import java.awt.image.WritableRaster;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.media.jai.RasterFactory;

import org.geotools.coverage.CoverageFactoryFinder;
import org.geotools.coverage.grid.GridCoordinates2D;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.coverage.grid.GridCoverageFactory;
import org.geotools.coverage.grid.GridEnvelope2D;
import org.geotools.coverage.grid.GridGeometry2D;
import org.geotools.coverage.grid.InvalidGridGeometryException;
import org.geotools.geometry.DirectPosition2D;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.opengis.coverage.grid.GridEnvelope;
import org.opengis.geometry.DirectPosition;
import org.opengis.referencing.operation.TransformException;

/**
 * @author ijt1
 * 
 */
public class QuantizeCircle {

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
    private ReferencedEnvelope env;
    private GridGeometry2D gg;

    /**
     *
     * produce a kernel density surface at x,y with radius r and
     * height value after Epanechnikov
     * (1969) and Brunsdon (1990)
     * 
     * K(u) = 3/4(1-u^2)1_{|u|<=1}
     *
     * @param env - the bounds of the surface
     * @param cellSize - what size to make the pixels
     */
    public QuantizeCircle(ReferencedEnvelope env, double cellSize) {
        this.env = env;
        this.cellsize = cellSize;
        System.out.println("cellsize " + cellsize);
        height = (int) Math.ceil((env.getHeight() / cellsize));
        width = (int) Math.ceil((env.getWidth() / cellsize));

        data = new double[height][width];
        GridEnvelope2D gridEnv = new GridEnvelope2D(0, 0, width, height);

        gg = new GridGeometry2D(gridEnv, (org.opengis.geometry.Envelope) env);
    }

    public double getCellsize() {
        return cellsize;
    }

    /**
     * will circles be multiplied by the kernel
     * @return
     */
    public boolean isQuantizeOn() {
        return quantizeOn;
    }

    public void setQuantizeOn(boolean quantizeOn) {
        this.quantizeOn = quantizeOn;
    }

    public GridCoverage2D processCircles(List<Circle> results) {
        for (Circle c : results) {
            quantize(c);
        }
        WritableRaster raster =
                RasterFactory.createBandedRaster(DataBuffer.TYPE_DOUBLE, width,
                height, 1, null);

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {

                raster.setSample(x, y, 0, data[y][x]);
            }
        }

        GridCoverageFactory gcf =
                CoverageFactoryFinder.getGridCoverageFactory(null);

        CharSequence name = "Process Results";

        GridCoverage2D grid = gcf.create(name, raster, env);

        return grid;
    }

    protected final void quantize(Circle c) {
        final double radius = c.getRadius();

        double x = c.getCentre().getX();
        x = Math.ceil((x + cellsize / 2.0) / cellsize) * cellsize;
        double y = c.getCentre().getY();
        y = Math.floor((y + cellsize / 2.0) / cellsize) * cellsize;
        final double value = c.getStatistic();
        //System.out.println("r " + radius);

        if (radius < (cellsize)) {
            addToCell(x, y, value);
            return;
        }

        if (radius != lastRadius) {

            lastRadius = radius;
            double rsq = radius * radius;
            final double w = Math.ceil((radius + cellsize / 2.0) / cellsize) * cellsize;
            double min = -w;//-radius - cellsize / 2.0;
            double max = w;//radius + cellsize / 2.0;
            //System.out.println(min + " " + max);
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

            if (sum > 0.0) {
                sum = 1.0 / sum;
            }
            for (int j = 0; j < m; j++) {
                xker[j] *= sum;
            }
        }
        // now apply it to the cells
        for (int j = 0; j < m; j++) {
            //System.out.println(j+" "+xadr[j]+","+yadr[j]+" -> "+xker[j]);
            if (quantizeOn) {
                addToCell(x + xadr[j], y + yadr[j], (value * xker[j]));
            } else {
                addToCell(x + xadr[j], y + yadr[j], value);
            }
        }
    }

    private void addToCell(int ix, int iy, double value) {
        data[iy][ix] += value;
    }

    /**
     * increment the raster at (x,y) by value;
     * @param x
     * @param y
     * @param value
     */
    protected GridCoordinates2D addToCell(double x, double y, double value) {
        GridCoordinates2D gridCoords;
        int ix;
        int iy;
        DirectPosition2D pos = new DirectPosition2D(x, y);
        try {
            gridCoords = gg.worldToGrid(pos);
        } catch (InvalidGridGeometryException e) {
            System.out.println(e);
            return null;
        } catch (TransformException e) {
            System.out.println(e);
            return null;
        }
        ix = gridCoords.x;
        iy = gridCoords.y;
        //System.out.println(ix+","+iy+" -> "+value);
        if (ix < 0 || ix >= data[0].length) {
            System.out.println("converting " + x + " gave " + ix);
        } else if (iy < 0 || iy >= data.length) {
            System.out.println("converting " + y + " gave " + iy);
        } else {
            data[iy][ix] += value;
        }
        return gridCoords;
    }
}
