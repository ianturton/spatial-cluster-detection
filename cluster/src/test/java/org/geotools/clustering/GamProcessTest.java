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
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import junit.framework.TestCase;

import org.geotools.data.DataUtilities;
import org.geotools.data.FeatureSource;
import org.geotools.data.FileDataStore;
import org.geotools.data.FileDataStoreFinder;
import org.geotools.data.shapefile.ShapefileDataStore;
import org.geotools.feature.FeatureCollection;
import org.geotools.process.Process;
import org.geotools.test.TestData;

/**
 * @author ijt1
 *
 */
public class GamProcessTest extends TestCase {
    
        
        public void testGamProcess() throws Exception {
            File f = TestData.file(this,"all_data.shp");
            System.out.println(f+" "+f.exists());
            URL url = DataUtilities.fileToURL(f);

            ShapefileDataStore store = new ShapefileDataStore(url);
            assertNotNull(store); 
            FeatureSource featureSource = store.getFeatureSource();
            Map<String, Object> params = new HashMap<String, Object>();
            final FeatureCollection features = featureSource.getFeatures();
            params.put(ClusterMethodFactory.NAME.key, "gam");
            params.put(ClusterMethodFactory.POPULATION.key, features);
            params.put(ClusterMethodFactory.POPATTRIBUTE.key, "pop");
            params.put(ClusterMethodFactory.CANCER.key, features);
            params.put(ClusterMethodFactory.CANATTRIBUTE.key, "cases");
            params.put(ClusterMethodFactory.MINRAD.key, 5000.0);
            params.put(ClusterMethodFactory.MAXRAD.key, 10000.0);
            params.put(ClusterMethodFactory.STEP.key, 1000.0);
            params.put(ClusterMethodFactory.TESTNAME.key, "poisson");
            ClusterMethodFactory factory = new ClusterMethodFactory();
            Process process = factory.create(params);
            assertNotNull(process);
            process.execute(params, new ClusterMonitor());
        }


}
