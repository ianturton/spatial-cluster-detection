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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.filter.text.cql2.CQLException;
import org.geotools.filter.text.ecql.ECQL;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.process.ProcessException;
import org.geotools.process.ProcessFactory;
import org.geotools.process.feature.AbstractFeatureCollectionProcessFactory;
import org.geotools.process.impl.AbstractProcess;
import org.geotools.process.raster.VectorToRasterException;
import org.geotools.process.raster.VectorToRasterFactory;
import org.geotools.text.Text;
import org.geotools.util.NullProgressListener;
import org.opengis.filter.expression.Expression;
import org.opengis.util.ProgressListener;

/**
 * @author ijt1
 * 
 */
public class GamProcess extends AbstractProcess {
    boolean started = false;

    private ArrayList<Circle> results = new ArrayList<Circle>();

    public GamProcess(ClusterMethodFactory factory) {
        super(factory);
    }

    public ProcessFactory getFactory() {
        return factory;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.geotools.process.Process#execute(java.util.Map, org.opengis.util.ProgressListener)
     */
    public Map<String, Object> execute(Map<String, Object> input, ProgressListener monitor)
            throws ProcessException {
        if (started)
            throw new IllegalStateException("Process can only be run once");
        started = true;

        if (monitor == null)
            monitor = new NullProgressListener();

        try {
            monitor.started();
            monitor.setTask(Text.text("Grabbing arguments"));
            monitor.progress(5.0f);
            SimpleFeatureCollection pop = (SimpleFeatureCollection) input
                    .get(ClusterMethodFactory.POPULATION.key);
            String attributeStr = (String) input.get(ClusterMethodFactory.POPATTRIBUTE.key);
            Expression popattribute = null;
            try {
                popattribute = ECQL.toExpression(attributeStr);
            } catch (CQLException e) {
                throw new ClusterException(e);
            }
            SimpleFeatureCollection can = (SimpleFeatureCollection) input
                    .get(ClusterMethodFactory.CANCER.key);
            attributeStr = (String) input.get(ClusterMethodFactory.CANATTRIBUTE.key);
            Expression canattribute = null;
            try {
                canattribute = ECQL.toExpression(attributeStr);
            } catch (CQLException e) {
                throw new ClusterException(e);
            }
            monitor.setTask(Text.text("Processing Data"));
            monitor.progress(10.0f);
            ReferencedEnvelope bounds = pop.getBounds();
            
            if (monitor.isCanceled()) {
                return null; // user has canceled this operation
            }

            // Geometry resultGeom = geom1.buffer(buffer);
            GridCoverage2D cov = convert(results);
            monitor.setTask(Text.text("Encoding result"));
            monitor.progress(90.0f);

            Map<String, Object> result = new HashMap<String, Object>();
            result.put(ClusterMethodFactory.RESULT.key, cov);
            monitor.complete(); // same as 100.0f

            return result;
        } catch (Exception eek) {
            monitor.exceptionOccurred(eek);
            return null;
        } finally {
            monitor.dispose();
        }
    }

    /**
     * @param results2
     * @return
     */
    private GridCoverage2D convert(ArrayList<Circle> results2) {
        // TODO Auto-generated method stub
        return null;
    }

}
