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
import java.util.HashMap;
import java.util.Map;

import org.geoserver.wps.gs.GeoServerProcess;
import org.geoserver.wps.jts.DescribeParameter;
import org.geoserver.wps.jts.DescribeProcess;
import org.geoserver.wps.jts.DescribeResult;
import org.geotools.clustering.ClusterMethodFactory;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.feature.FeatureCollection;
import org.geotools.process.Process;
import org.geotools.process.ProcessException;
import org.opengis.util.ProgressListener;

/**
 * @author ijt1
 * 
 */
public class ClusterProcess implements GeoServerProcess {
    @DescribeProcess(title = "spatialCluster", description = "Tests a set of cases and background population to find local clusters of excess, returns a raster surface of hotspots.")
    @DescribeResult(name = "result", description = "The Raster surface showing local clusters")
    public GridCoverage2D execute(
            // @DescribeParameter(name = "features", description =
            // "The feature collection whose geometries will be collected") FeatureCollection
            // features,
            @DescribeParameter(name = "type", description = "The name of the cluster method") String name,
            @DescribeParameter(name = "population", description = "The background population") FeatureCollection pop,
            @DescribeParameter(name = "popattribute", description = "The attribute that contains the population count") String popattribute,
            @DescribeParameter(name = "cancer", description = "The case data") FeatureCollection cancer,
            @DescribeParameter(name = "canattribute", description = "The attribute with the cases") String canattribute,
            @DescribeParameter(name = "MINRAD", description = "The smallest circle to consider") double minrad,
            @DescribeParameter(name = "MAXRAD", description = "The largest circle to consider") double maxrad,
            @DescribeParameter(name = "step", description = "How much to change the size of the circles by") double step,
            @DescribeParameter(name = "overlap", description = "How much should the circles overlap by (0-1)") double overlap,
            @DescribeParameter(name = "significance", description = "name of the significance test", min = 0) String testName,
            ProgressListener progressListener) throws IOException, ProcessException {
        Map<String, Object> params = new HashMap<String, Object>();
        params.put(ClusterMethodFactory.NAME.key, name);
        params.put(ClusterMethodFactory.POPULATION.key, pop);
        params.put(ClusterMethodFactory.POPATTRIBUTE.key, popattribute);
        params.put(ClusterMethodFactory.CANCER.key, cancer);
        params.put(ClusterMethodFactory.CANATTRIBUTE.key, canattribute);
        params.put(ClusterMethodFactory.MINRAD.key, minrad);
        params.put(ClusterMethodFactory.MAXRAD.key, maxrad);
        params.put(ClusterMethodFactory.STEP.key, step);
        params.put(ClusterMethodFactory.OVERLAP.key, overlap);
        if(testName!=null && !testName.isEmpty())params.put(ClusterMethodFactory.TESTNAME.key, testName);
        ClusterMethodFactory factory = new ClusterMethodFactory();
        Process process = factory.create(params);
        Map<String, Object> results = process.execute(params, progressListener);
        return (GridCoverage2D) results.get(ClusterMethodFactory.RESULT.key);

    }

}
