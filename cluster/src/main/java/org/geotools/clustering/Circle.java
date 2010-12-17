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

import com.vividsolutions.jts.geom.Point;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.Polygon;
import com.vividsolutions.jts.util.GeometricShapeFactory;

public class Circle {
    Point centre;

    double radius;

    static final private GeometricShapeFactory fac = new GeometricShapeFactory();

    static final private GeometryFactory gf = new GeometryFactory();

    public Circle(Point centre, double radius) {
        super();
        this.centre = centre;
        this.radius = radius;
    }

    public Circle() {
        this.radius = 0.0;
        this.centre = null;
    }

    public Circle(double x, double y, double r) {
        this.centre = gf.createPoint(new Coordinate(x, y));
        this.radius = r;
    }

    public Point getCentre() {
        return centre;
    }

    public void setCentre(Point centre) {
        this.centre = centre;
    }

    public double getRadius() {
        return radius;
    }

    public void setRadius(double radius) {
        this.radius = radius;
    }

    public boolean contains(Point p) {
        if (centre == null)
            return false;
        // System.out.println(centre.distance(p)+" <= "+radius);
        return centre.distance(p) <= radius;
    }

    @Override
    public String toString() {
        // TODO Auto-generated method stub
        return centre + " " + radius;
    }

    public Polygon toPolygon() {
        return toPolygon(360);
    }

    public Polygon toPolygon(int i) {
        fac.setSize(radius * 2.0);
        fac.setNumPoints(i);
        fac.setCentre(centre.getCoordinate());
        Polygon circle = fac.createCircle();
        return circle;
    }
}
