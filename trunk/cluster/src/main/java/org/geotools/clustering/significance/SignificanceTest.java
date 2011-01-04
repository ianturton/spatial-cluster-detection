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

package org.geotools.clustering.significance;

import java.util.LinkedHashMap;
import java.util.Map;

import org.geotools.data.Parameter;
import org.geotools.text.Text;

/**
 * @author ijt1
 * 
 */
public abstract class SignificanceTest {
    static String name;

    static final Parameter<Boolean> EXCESS = new Parameter<Boolean>("excess", Boolean.class,
            Text.text("Test Excess"),
            Text.text("Should the test be to find an excess (true) or a deficiency (false)"),
            false, // this parameter is not mandatory
            1, 1, true, null);
    static final Parameter<Double> THRESHOLD = new Parameter<Double>("threshold", Double.class,
            Text.text("significance threshold"),
            Text.text("The significance threshold at which to accept the test"), false,
            1, 1, 0.01, null);
    static final Parameter<Integer> STATTYPE = new Parameter<Integer>("stattype", Integer.class,
            Text.text("Type of Statistic to be used"),
            Text.text("The Type of statistic test to be used (1-3)"), false,
            1, 1, 1, null);
    static final Parameter<Double> MINPOPSIZE = new Parameter<Double>("minpopsize", Double.class,
            Text.text("The Minimum population threshold"),
            Text.text("The Minimum population to be tested"), false,
            1, 1, 1.0, null);

    static final Parameter<Double> MINCANSIZE = new Parameter<Double>("mincansize", Double.class,
            Text.text("The Minimum cancer threshold"),
            Text.text("The Minimum number of cases to be tested"), false,
            1, 1, 1.0, null);
    
    protected Map<String, Object> parameters;

    public SignificanceTest(Map<String, Object> params) {
        this.parameters = params;
    }

    public abstract boolean isSignificant(double obsP, double obsC) throws PoissonException;

    public abstract double getStatistic();

    public final Parameter<?>[] getParametersInfo() {
        LinkedHashMap<String, Parameter<?>> map = new LinkedHashMap<String, Parameter<?>>();
        setupParameters(map);

        return map.values().toArray(new Parameter<?>[map.size()]);
    }

    void setupParameters(LinkedHashMap<String, Parameter<?>> map) {
        map.put(EXCESS.key, EXCESS);
        map.put(MINPOPSIZE.key, MINPOPSIZE);
        map.put(MINCANSIZE.key, MINCANSIZE);
        map.put(THRESHOLD.key, THRESHOLD);
        map.put(STATTYPE.key, STATTYPE);
    }

    boolean canProcess(Map<String, Object> params) {
        if (params == null) {
            return false;
        }
        Parameter<?>[] arrayParameters = getParametersInfo();
        for (int i = 0; i < arrayParameters.length; i++) {
            Parameter<?> param = arrayParameters[i];

            if (!params.containsKey(param.key)) {
                if (param.required) {
                    return false; // missing required key!
                } else {
                    // insert default
                    System.out.println("adding "+param.key+" with a value of "+param.sample);
                    parameters.put(param.key, param.sample);
                }
            }

        }
        return true;
    }

    public boolean isWorthTesting(double sumP, double sumC) {

        boolean excess = ((Boolean) parameters.get(EXCESS.key)).booleanValue();
        double minCanSize = ((Double) parameters.get(MINCANSIZE.key)).doubleValue();
        double minPopSize = ((Double) parameters.get(MINPOPSIZE.key)).doubleValue();
        if (excess) {
            return ((sumP <= sumC) && (sumP >= minPopSize) && (sumC >= minCanSize));
        } else {
            return ((sumP >= sumC) && (sumP >= minPopSize) && (sumC >= minCanSize));
        }
    }
    
    public static String getName() {
        return name;
    }
}
