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

import java.io.File;
import java.io.Serializable;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import junit.framework.TestCase;
import org.geotools.clustering.utils.Utilities;

import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.data.DataStoreFactorySpi;
import org.geotools.data.DataUtilities;
import org.geotools.data.FeatureSource;
import org.geotools.data.shapefile.ShapefileDataStore;
import org.geotools.data.shapefile.ShapefileDataStoreFactory;
import org.geotools.feature.FeatureCollection;
import org.geotools.process.Process;
import org.geotools.test.TestData;

/**
 * @author ijt1
 * 
 */
public class BessagAndNewellProcessTest extends TestCase {

    public void testProcess() throws Exception {
        File f = TestData.file(this, "all_data.shp");
        //System.out.println(f + " " + f.exists());
        URL url = DataUtilities.fileToURL(f);
        DataStoreFactorySpi dataStoreFactory = new ShapefileDataStoreFactory();

        Map<String, Serializable> params2 = new HashMap<String, Serializable>();
        params2.put("url", url);
        params2.put("create spatial index", Boolean.TRUE);
        ShapefileDataStore store = (ShapefileDataStore) dataStoreFactory
                .createNewDataStore(params2);
        //ShapefileDataStore store = new ShapefileDataStore(url);
        assertNotNull(store);
        FeatureSource featureSource = store.getFeatureSource();
        final FeatureCollection features = featureSource.getFeatures();
        
      
       
        Map<String, Object> params = new HashMap<String, Object>();

        params.put(ClusterMethodFactory.NAME.key, "b&n");
        params.put(ClusterMethodFactory.POPULATION.key, features);
        params.put(ClusterMethodFactory.POPATTRIBUTE.key, "pop");
        params.put(ClusterMethodFactory.CANCER.key, features);
        params.put(ClusterMethodFactory.CANATTRIBUTE.key, "cases");
        params.put(ClusterMethodFactory.NONEIGHBOURS.key, 5);
        
        params.put(ClusterMethodFactory.TESTNAME.key, "poisson");
        ClusterMethodFactory factory = new ClusterMethodFactory();
        Process process = factory.create(params);
        assertNotNull(process);
        long start = System.currentTimeMillis();
        Map<String, Object> results = process.execute(params, new ClusterMonitor());
        long end = System.currentTimeMillis();
        System.out.println("process took "+((end-start)/1000)+ " seconds");
        GridCoverage2D grid = (GridCoverage2D) results.get(ClusterMethodFactory.RESULT.key);
        String basename = f.toString();
        basename=basename.substring(0, basename.length() - 4);
        String filename = basename + "_BN.tiff";
        Utilities.writeGrid(filename, grid);

        FeatureCollection outfeatures = (FeatureCollection)results.get(ClusterMethodFactory.CIRCLES.key);
        Utilities.writeCircles(basename+"_BN.shp", outfeatures);
        

    }


}
