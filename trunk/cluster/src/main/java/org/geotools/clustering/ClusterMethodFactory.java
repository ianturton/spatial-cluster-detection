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

import java.util.Collections;
import java.util.Map;
import java.util.TreeMap;

import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.data.Parameter;
import org.geotools.feature.FeatureCollection;
import org.geotools.process.Process;
import org.geotools.process.feature.AbstractFeatureCollectionProcessFactory;
import org.geotools.text.Text;
import org.opengis.util.InternationalString;

/**
 * @author ijt1
 * 
 */
public class ClusterMethodFactory extends
		AbstractFeatureCollectionProcessFactory {

	final String VERSION = "1.0";

	public static final Parameter<FeatureCollection> POPULATION = new Parameter<FeatureCollection>(
			"population", FeatureCollection.class, Text
					.text("Features containing population"), Text
					.text("Features containing the population at risk"));
	/**
	 * The source of values for population at risk. This is either the name (
	 * {@code String}) of a numeric feature property or a CQL expression that
	 * can be evaluated to a numeric value.
	 * <p>
	 * This parameter is mandatory.
	 */
	public static final Parameter<String> POPATTRIBUTE = new Parameter<String>(
			"popattribute", String.class, Text.text("Population Attribute"),
			Text.text("The feature attribute with the population at risk"),
			true, // this parameter is mandatory
			1, 1, null, null);
	public static final Parameter<FeatureCollection> CANCER = new Parameter<FeatureCollection>(
			"cancer", FeatureCollection.class, Text
					.text("Features containing incidents"), Text
					.text("Features containing the incidents"));
	public static final Parameter<String> CANATTRIBUTE = new Parameter<String>(
			"canattribute", String.class, Text.text("Incident Attribute"), Text
					.text("The feature attribute with the incidents"), true, // this
																				// parameter
																				// is
																				// mandatory
			1, 1, null, null);

	public static final Parameter<String> NAME = new Parameter<String>("type",
			String.class, Text.text("Type of Method required"), Text
					.text("What type of method (GAM)"), true, 1, 1, "GAM", null);
	public static final Parameter<Double> MINRAD = new Parameter<Double>(
			"MINRAD", Double.class, Text.text("Minimum Radius"), Text
					.text("Radius of the smallest circle"), true, 1, 1, 5.0,
			null);
	public static final Parameter<Double> MAXRAD = new Parameter<Double>(
			"MaxRAD", Double.class, Text.text("Maximum Radius"), Text
					.text("Radius of the largest circle"), true, 1, 1, 5.0,
			null);
	public static final Parameter<Double> OVERLAP = new Parameter<Double>(
			"overlap", Double.class, Text.text("Circle Overlap"), Text
					.text("% of radius to overlap circles"), false, 1, 1, 0.5,
			null);
	public static final Parameter<Double> STEP = new Parameter<Double>("step",
			Double.class, Text.text("Radius Step"), Text
					.text("Step Size of radius"), false, 1, 1, 5.0, null);
	public static final Parameter<String> TESTNAME = new Parameter<String>(
			"test", String.class, Text.text("Name of Statistic"), Text
					.text("Type of Statistical test to use"), false, 1, 1,
			"Poisson", null);

	public static final Parameter<Double> THRESHOLD = new Parameter<Double>(
			"threshold", Double.class, Text.text("Threshold"), Text
					.text("minimum number of points to be considered"), false,
			1, 1, 1.0, null);

	public static final Parameter<Integer> MINPOP = new Parameter<Integer>(
			"minpop", Integer.class, Text.text("Minium Population"), Text
					.text("minimum population to be considered"), false, 1, 1,
			1, null);
	public static final Parameter<Integer> MINCAN = new Parameter<Integer>(
			"mincan", Integer.class, Text.text("Minium Incidence"), Text
					.text("minimum number of cases to be considered"), false,
			1, 1, 1, null);
	public static final Parameter<Integer> NCIRCLES = new Parameter<Integer>(
			"NCIRCLES", Integer.class, Text.text("Number of Circles"), Text
					.text("Number of circles to be generated"), false, 1, 1, 1,
			null);
	
	public static final Parameter<Integer> NONEIGHBOURS = new Parameter<Integer>(
			"NONEIGHBOURS", Integer.class, Text.text("Number of Neighbours"),
			Text.text("Number of neighbours to be considered"), false, 1, 1, 1,
			null);
	public static final Parameter<Double> SIGNIFICANCE = new Parameter<Double>(
			"significance", Double.class, Text.text("Significance threshold"),
			Text.text("Threshold to consider a circle significant"), false, 1,
			1, 0.001, null);

	public static final Parameter<GridCoverage2D> RESULT = new Parameter<GridCoverage2D>(
			"result", GridCoverage2D.class, Text.text("Results Coverage"), Text
					.text("The results surface"));
	public static final Parameter<FeatureCollection> CIRCLES = new Parameter<FeatureCollection>(
			"circles", FeatureCollection.class, Text
					.text("Significant Circles"), Text
					.text("The Significant Circles"));

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.geotools.process.feature.AbstractFeatureCollectionProcessFactory#
	 * addParameters(java.util .Map)
	 */
	@Override
	protected void addParameters(Map<String, Parameter<?>> parameters) {
		parameters.put(NAME.key, NAME);
		parameters.put(POPULATION.key, POPULATION);
		parameters.put(POPATTRIBUTE.key, POPATTRIBUTE);
		parameters.put(CANCER.key, CANCER);
		parameters.put(CANATTRIBUTE.key, CANATTRIBUTE);
		parameters.put(MINRAD.key, MINRAD);
		parameters.put(MAXRAD.key, MAXRAD);
		parameters.put(STEP.key, STEP);
		parameters.put(NCIRCLES.key, NCIRCLES);
		parameters.put(MINCAN.key, MINCAN);
		parameters.put(MINPOP.key, MINPOP);
		parameters.put(TESTNAME.key, TESTNAME);
		parameters.put(THRESHOLD.key, THRESHOLD);
		parameters.put(SIGNIFICANCE.key, SIGNIFICANCE);
		parameters.put(TESTNAME.key, TESTNAME);
		parameters.put(NONEIGHBOURS.key, NONEIGHBOURS);
		
	}

	/**
	 * Map used to describe operation results.
	 */
	static final Map<String, Parameter<?>> resultInfo = new TreeMap<String, Parameter<?>>();

	static {
		resultInfo.put(RESULT.key, RESULT);
		resultInfo.put(CIRCLES.key, CIRCLES);
	}

	public Process create(Map<String, Object> parameters)
			throws IllegalArgumentException {
		// TODO Auto-generated method stub

		String method = (String) parameters.get("type");
		if (method.equalsIgnoreCase("gam")) {
			return new GamProcess(this);
		} else if (method.equalsIgnoreCase("random")) {
			return new RandomProcess(this);
		} else if (method.equalsIgnoreCase("b&n")) {
			return new BesagAndNewellProcess(this);
		} else {
			throw new IllegalArgumentException("Unknown method " + method);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.geotools.process.impl.SingleProcessFactory#getDescription()
	 */
	@Override
	protected InternationalString getDescription() {
		// TODO Auto-generated method stub
		return Text.text("A Cluster Process");
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.geotools.process.impl.SingleProcessFactory#getResultInfo(java.util
	 * .Map)
	 */
	@Override
	protected Map<String, Parameter<?>> getResultInfo(Map<String, Object> arg0)
			throws IllegalArgumentException {
		// TODO Auto-generated method stub
		return Collections.unmodifiableMap(resultInfo);

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.geotools.process.impl.SingleProcessFactory#getVersion()
	 */
	@Override
	protected String getVersion() {
		// TODO Auto-generated method stub

		return VERSION;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.geotools.process.impl.SingleProcessFactory#supportsProgress()
	 */
	@Override
	protected boolean supportsProgress() {
		// TODO Auto-generated method stub
		return true;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.geotools.process.impl.SingleProcessFactory#create()
	 */
	@Override
	protected Process create() {
		// TODO Auto-generated method stub
		return new GamProcess(this);
	}

}
