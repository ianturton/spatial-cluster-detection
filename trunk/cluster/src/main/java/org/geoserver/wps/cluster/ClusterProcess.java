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

package org.geoserver.wps.cluster;

import java.io.IOException;

import org.geoserver.wps.gs.GeoServerProcess;
import org.geoserver.wps.jts.DescribeParameter;
import org.geoserver.wps.jts.DescribeProcess;
import org.geoserver.wps.jts.DescribeResult;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.FeatureIterator;
import org.geotools.geometry.jts.GeometryCollector;
import org.opengis.util.ProgressListener;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryCollection;

/**
 * @author ijt1
 *
 */
public class ClusterProcess implements GeoServerProcess{
    @DescribeProcess(title = "collectGeometries", description = "Collects all the default geometries in the feature collection and returns them as a single geometry collection")
    public class CollectGeometries implements GeoServerProcess {

        @DescribeResult(name = "result", description = "The reprojected features")
        public GeometryCollection execute(
                @DescribeParameter(name = "features", description = "The feature collection whose geometries will be collected") FeatureCollection features,
                ProgressListener progressListener) throws IOException {
            int count = features.size();
            float done = 0;

            FeatureIterator fi = null;
            GeometryCollector collector = new GeometryCollector();
            try {
                fi = features.features();
                while(fi.hasNext()) {
                    Geometry g = (Geometry) fi.next().getDefaultGeometryProperty().getValue();
                    collector.add(g);
        
                    // progress notification
                    done++;
                    if (progressListener != null && done % 100 == 0) {
                        progressListener.progress(done / count);
                    }
                }
            } finally {
                if (fi != null) {
                    fi.close();
                }
            }
            
            return collector.collect();
        }

    }
}
