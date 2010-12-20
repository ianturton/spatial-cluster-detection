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


/**
 * @author ijt1
 * 
 */
public class QuantizeCircle {
    
    BufferedImage data;
    /**
     * produce a kernel density surface at x,y with radius r and height value after Epanechnikov
     * (1969) and Brunsdon (1990)
     * 
     * K(u) = 3/4(1-u^2)1_{|u|<=1}
     */
    
    /**
     * @param img 
     * 
     */
    public QuantizeCircle(BufferedImage img) {
        this.data = img;
    }
    double lastRadius = -1;

    int m;

    int numb;

    double xker[];

    double xadr[];

    double yadr[];

    private double cellsize=1.0;

    private boolean quantizeOn = true;

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
       System.out.println(data.getWidth()+" "+data.getHeight());
       System.out.println(x+","+y+" "+((int)(x/cellsize))+" "+((int)(y/cellsize)));
        data.setRGB((int)(x/cellsize), (int)(y/cellsize), (int)(value/255));
    }
}
