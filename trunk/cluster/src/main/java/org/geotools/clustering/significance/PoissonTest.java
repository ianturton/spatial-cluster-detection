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

import java.util.Map;
import java.util.Set;

/**
 * @author ijt1
 * 
 */
public class PoissonTest extends SignificanceTest {
    public static final int MAXCAN = 300;

    private double[] cons;

    private double statistic;

    /**
     * @param params
     */
    public PoissonTest(Map<String, Object> params) {
        super(params);
        name = "Poisson";
        if (canProcess(params)) {
            cons = new double[MAXCAN];

            System.out.println(" *Poisson Test used with MAXIMUM case count of " + MAXCAN);
            for (int i = 1; i < MAXCAN; i++) {
                cons[i] = ((double) 1.0) / i;
            }
        }
        Set<String> keys = parameters.keySet();
        for (String key : keys) {
            System.out.println(key + ":" + parameters.get(key));
        }

    }

    public boolean canProcess(Map<String, Object> params) {
        // test global parameters
        boolean ret = super.canProcess(params);
        if (ret) {// test local params

        }
        return ret;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.geotools.clustering.significance.SignificanceTest#isSignificant(double, double)
     */
    public boolean isSignificant(double sumP, double sumC) throws PoissonException {

        double cumPrb[];
        cumPrb = new double[MAXCAN];
        // int jA = (int) sumC;
        // double aMean = (double) sumP;
        double prob;

        int jA = (int) sumC;
        double aMean = (double) sumP;

        if (!isWorthTesting(sumP, sumC)) {
            return false;
        }

        if (jA > MAXCAN) {
            throw new PoissonException("Too many cases for a Poisson Test");
        }
        if (jA > 1) {
            cumPrb[0] = Math.exp(-aMean);
            prob = cumPrb[0];
            for (int j = 1; j < jA; j++) {
                cumPrb[j] = aMean * cons[j] * cumPrb[j - 1];
                prob += cumPrb[j];
            }
            prob = 1.0 - prob;
        } else
            prob = 1.0 - Math.exp(-aMean);
        // System.out.println(prob +" "+ parameters.getSignificanceThreshold() +" "+ totcases +" "+
        // totpop);
        statistic = sumC - sumP;
        if (prob <= ((Double) parameters.get(THRESHOLD.key)).doubleValue()) {
            switch (((Integer) parameters.get(STATTYPE.key)).intValue()) {
            // case 1: stat = sumC - sumP; break;
            // case 2: stat = sumC/sumP; break;
            case 1:
                statistic = sumC - sumP;
                break;
            case 2:
                statistic = sumC / sumP;
                break;
            case 3:
                statistic = (double) 1.0 - prob;
                break;
            }
            // System.out.println(totpop +"  "+ totcases +"  "+ jA +"  "+ prob +"  "+ stat);
            return true;
        } else
            return false;

    }

    /*
     * (non-Javadoc)
     * 
     * @see org.geotools.clustering.significance.SignificanceTest#getStatistic()
     */
    public double getStatistic() {
        // TODO Auto-generated method stub
        return statistic;
    }

}
