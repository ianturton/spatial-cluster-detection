/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.geotools.clustering;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import org.geotools.clustering.significance.SignificanceTestException;
import org.geotools.clustering.significance.SignificanceTest;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.process.ProcessException;
import org.geotools.process.ProcessFactory;
import org.geotools.process.impl.AbstractProcess;
import org.geotools.text.Text;
import org.geotools.util.NullProgressListener;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.filter.expression.Expression;
import org.opengis.geometry.MismatchedDimensionException;
import org.opengis.util.ProgressListener;

/**
 *
 * @author ijt1
 */
public abstract class AbstractClusterProcess extends AbstractProcess {
    SimpleFeatureCollection can;
    Expression canattribute;
    double overrat;
    SimpleFeatureCollection pop;
    Expression popattribute;
    boolean started = false;
    SignificanceTest test;
    ProgressListener monitor;
    protected AbstractClusterProcess(ProcessFactory factory) {
        super(factory);
    }

    GridCoverage2D convert(List<Circle> results) {
        ReferencedEnvelope resBounds = new ReferencedEnvelope(pop.getBounds().getCoordinateReferenceSystem());
        for (Circle c : results) {
            resBounds.expandToInclude(c.getBounds());
        }
        System.out.println(resBounds);
        final double scale = 100.0;
        QuantizeCircle qc = new QuantizeCircle(resBounds, scale);
        return qc.processCircles(results);
    }

    public Map<String, Object> execute(Map<String, Object> input, ProgressListener mon) throws ProcessException {
        if (started) {
            throw new IllegalStateException("Process can only be run once");
        }
        started = true;
        ArrayList<Circle> results = new ArrayList<Circle>();
        if (mon == null) {
            monitor = new NullProgressListener();
        } else {
            monitor = mon;
        }
        try {
            mon.started();
            mon.setTask(Text.text("Grabbing arguments"));
            mon.progress(5.0F);
            processParameters(input);
            mon.setTask(Text.text("Pre-Processing Data"));
            mon.progress(7.0F);
            SimpleFeatureIterator popIt = pop.features();
            double totalPop = 0.0;
            while (popIt.hasNext()) {
                SimpleFeature feature = popIt.next();
                final Object evaluate = popattribute.evaluate(feature);
                // System.out.println(evaluate);
                Number count = (Number) evaluate;
                totalPop += count.doubleValue();
            }
            SimpleFeatureIterator canIt = can.features();
            double totalCan = 0.0;
            while (canIt.hasNext()) {
                SimpleFeature feature = canIt.next();
                final Object evaluate = canattribute.evaluate(feature);
                // System.out.println(evaluate);
                Number count = (Number) evaluate;
                totalCan += count.doubleValue();
            }
            overrat = (double) totalCan / (double) totalPop;
            mon.setTask(Text.text("Processing Data"));
            mon.progress(10.0F);
            
            results = process();
            if (mon.isCanceled()) {
                System.err.println("user cancel");
                return null; // user has canceled this operation
            }
            mon.setTask(Text.text("Encoding result"));
            mon.progress(90.0F);
            GridCoverage2D cov = convert(results);
            Map<String, Object> result = new HashMap<String, Object>();
            result.put(ClusterMethodFactory.RESULT.key, cov);
            mon.complete(); // same as 100.0f
            return result;
        } catch (Exception eek) {
            System.err.println(eek);
            mon.exceptionOccurred(eek);
            return null;
        } finally {
            mon.dispose();
        }
    }

    public ProcessFactory getFactory() {
        return factory;
    }

    abstract ArrayList<Circle> process() throws MismatchedDimensionException, NoSuchElementException, SignificanceTestException;

    /**
     * Take the input parameters and convert them into useful values to be used in process();
     * @param input - the map of parameters
     * @throws IllegalArgumentException
     * @throws ClusterException
     */
    abstract void processParameters(Map<String, Object> input) throws IllegalArgumentException, ClusterException;

}
