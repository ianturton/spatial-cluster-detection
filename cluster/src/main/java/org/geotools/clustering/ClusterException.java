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
import org.geotools.process.ProcessException;
/**
 * @author ijt1
 *
 */
public class ClusterException extends ProcessException {
    /**
     * 
     */
    private static final long serialVersionUID = 1L;

    public ClusterException(String message) {
        super(message);
    }

    public ClusterException(Exception ex) {
        super(ex);
    }
}
