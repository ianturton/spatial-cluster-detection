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
import java.util.Map;
import java.util.NoSuchElementException;

import org.geotools.clustering.significance.BootstrapTest;
import org.geotools.clustering.significance.MonteCarloTest;
import org.geotools.clustering.significance.SignificanceTestException;

import org.geotools.clustering.significance.PoissonTest;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.filter.text.cql2.CQLException;
import org.geotools.filter.text.ecql.ECQL;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.text.Text;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.filter.Filter;
import org.opengis.filter.expression.PropertyName;
import org.opengis.geometry.BoundingBox;
import org.opengis.geometry.MismatchedDimensionException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

/**
 * @author ijt1
 * 
 */
public class GamProcess extends AbstractClusterProcess {

	private boolean sharedData = false;
	private double minRadius;
	private double maxRadius;
	private double radiusStep;
	private double overlap;
	private int totalCircles;

	public GamProcess(ClusterMethodFactory factory) {
		super(factory);

	}

	void processParameters(Map<String, Object> input)
			throws IllegalArgumentException, ClusterException {
		pop = (SimpleFeatureCollection) input
				.get(ClusterMethodFactory.POPULATION.key);
		String attributeStr = (String) input
				.get(ClusterMethodFactory.POPATTRIBUTE.key);
		popattribute = null;
		try {
			popattribute = ECQL.toExpression(attributeStr);
		} catch (CQLException e) {
			throw new ClusterException(e);
		}
		can = (SimpleFeatureCollection) input
				.get(ClusterMethodFactory.CANCER.key);
		attributeStr = (String) input
				.get(ClusterMethodFactory.CANATTRIBUTE.key);
		canattribute = null;
		try {
			canattribute = ECQL.toExpression(attributeStr);
		} catch (CQLException e) {
			throw new ClusterException(e);
		}
		if (pop == can) {
			System.out.println("identical inputs!");
			sharedData = true;
		}
		minRadius = ((Double) input.get(ClusterMethodFactory.MINRAD.key))
				.doubleValue();
		maxRadius = ((Double) input.get(ClusterMethodFactory.MAXRAD.key))
				.doubleValue();
		radiusStep = ((Double) input.get(ClusterMethodFactory.STEP.key))
				.doubleValue();
		overlap = ((Double) input.get(ClusterMethodFactory.OVERLAP.key))
				.doubleValue();
		String testName = input.get(ClusterMethodFactory.TESTNAME.key)
				.toString();
		// switch the statistic name (when we have more tests)
		if ("Poisson".equalsIgnoreCase(testName)) {
			test = new PoissonTest(input);
		}else if("MonteCarlo".equalsIgnoreCase(testName)){
        	try {
				test = new MonteCarloTest(input);
			} catch (SignificanceTestException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				throw new ClusterException(e);
			}
        } else if("Bootstrap".equalsIgnoreCase(testName)){
        	try {
				test = new BootstrapTest(input);
			} catch (SignificanceTestException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				throw new ClusterException(e);
			}
        }else {
			if (testName.length() > 0) {
				throw new IllegalArgumentException("Unknown statistical test "
						+ testName);
			} else {
				throw new IllegalArgumentException(
						"No Statistical test specified");
			}
		}
	}

	ArrayList<Circle> process() throws MismatchedDimensionException,
			NoSuchElementException, SignificanceTestException {
		ArrayList<Circle> results = new ArrayList<Circle>();
		ReferencedEnvelope bounds = pop.getBounds();
		// extend bounds by 1/2 max rad in all directions
		final double halfMaxRadius = maxRadius / 2.0;
		double minX = bounds.getMinX() - halfMaxRadius;
		double minY = bounds.getMinY() - halfMaxRadius;
		double maxX = bounds.getMaxX() + halfMaxRadius;
		double maxY = bounds.getMaxY() + halfMaxRadius;
		bounds = new ReferencedEnvelope(minX, maxX, minY, maxY, bounds
				.getCoordinateReferenceSystem());
		int loopCount = 0;
		for (double radius = minRadius; radius <= maxRadius; radius += radiusStep) {
			for (double x = minX; x <= maxX; x += radius * overlap) {
				for (double y = minY; y <= maxY; y += radius * overlap) {
					loopCount++;
				}
			}
		}
		totalCircles = loopCount;
		System.out.println("About to do " + totalCircles + " circles");

		CoordinateReferenceSystem popCrs = pop.getBounds()
				.getCoordinateReferenceSystem();
		CoordinateReferenceSystem canCrs = can.getBounds()
				.getCoordinateReferenceSystem();
		PropertyName popgeom = ff.property(pop.getSchema()
				.getGeometryDescriptor().getName());
		PropertyName cangeom = ff.property(can.getSchema()
				.getGeometryDescriptor().getName());
		SimpleFeatureIterator popIt = null;
		SimpleFeatureIterator canIt = null;
		loopCount = 0;
		for (double radius = minRadius; radius <= maxRadius; radius += radiusStep) {

			System.out.println("radius = " + radius + "\n min/max R"
					+ minRadius + "," + maxRadius);
			monitor.setTask(Text.text("Radius " + radius));
			float i = (float) loopCount / (float) totalCircles;
			monitor.progress(10.0f + (80.0f * i));
			for (double x = minX; x <= maxX; x += radius * overlap) {

				SimpleFeatureCollection popsubset=null;
				SimpleFeatureCollection cansubset = null;
				try {
					boolean fast = false; // oddly this slows things down
					if (fast) {
						Filter bbox = ff.bbox("the_geom", x - radius, minY, x
								+ radius, maxY, "EPSG:27700");
						// System.out.println("applying " + bbox);
						popsubset = pop.subCollection(bbox);
						if (!sharedData) {
							cansubset = can.subCollection(bbox);
						}
					} else {
						popsubset = pop;
						cansubset = null;
						if (!sharedData) {
							cansubset = can;
						}
					}
					// System.out.println("got " + subset.size());
					if (popsubset.size() > 0) {
						for (double y = minY; y <= maxY; y += radius * overlap) {
							loopCount++;
							i = (float) loopCount / (float) totalCircles;
							if (loopCount % 1000 == 0) {
								monitor.progress(10.0f + (80.0f * i));
							}
							Circle circle = new Circle(x, y, radius);
							double popCount = 0;
							double canCount = 0;
							boolean fast2 = true;
							if (fast2) {
								Filter filter = ff.within(popgeom, ff
										.literal(circle.toPolygon()));
								// System.out.println(filter);
								// get pop points in circle
								SimpleFeatureCollection popPoints = popsubset
										.subCollection(filter);
								if (popPoints.size() > 0) {
									// System.out.println(circle + " got " +
									// popPoints.size()
									// + " pop points");
									popIt = popPoints.features();
									while (popIt.hasNext()) {
										SimpleFeature feature = popIt.next();
										Object evaluate = popattribute
												.evaluate(feature);
										// System.out.println(evaluate);
										Number count = (Number) evaluate;
										popCount += (count.doubleValue() * overrat);
										if (sharedData) {
											evaluate = canattribute
													.evaluate(feature);
											// System.out.println(evaluate);
											count = (Number) evaluate;
											canCount += count.doubleValue();
										}
									}
									popIt.close();
									// System.out.println("\tContaining " +
									// popCount + " people");
									// System.out.println("\tContaining " +
									// popCount + " people");
								}
								filter = ff.within(cangeom, ff.literal(circle
										.toPolygon()));
								// System.out.println(filter);
								// get pop points in circle
								if (!sharedData) {
									SimpleFeatureCollection canPoints = cansubset
											.subCollection(filter);
									if (canPoints.size() > 0) {
										// System.out.println(circle + " got " +
										// canPoints.size()
										// + " case points");
										try {
											canIt = canPoints.features();
											while (canIt.hasNext()) {
												SimpleFeature feature = canIt
														.next();
												final Object evaluate = canattribute
														.evaluate(feature);
												// System.out.println(evaluate);
												Number count = (Number) evaluate;
												canCount += count.doubleValue();
											}
										} finally {
											canIt.close();
										}
										// System.out.println("\tContaining " +
										// canCount +
										// " cases");
										// System.out.println("\tContaining " +
										// canCount +
										// " cases");
									} // canPoints > 0
								} // sharedData
							} else {
								// fast2
								BoundingBox bbox = new ReferencedEnvelope(
										circle.getBounds(), popCrs);
								Filter filter = ff.bbox(popgeom, bbox);
								// System.out.println(filter);
								// get pop points in circle
								SimpleFeatureCollection popPoints = popsubset
										.subCollection(filter);
								if (popPoints.size() > 0) {
									// System.out.println(circle + " got " +
									// popPoints.size() + " pop points");
									try {
										popIt = popPoints.features();
										while (popIt.hasNext()) {
											SimpleFeature feature = popIt
													.next();
											Filter filter2 = ff.within(popgeom,
													ff.literal(circle
															.toPolygon()));
											if (filter2.evaluate(feature)) {
												Object evaluate = popattribute
														.evaluate(feature);
												// System.out.println(evaluate);
												Number count = (Number) evaluate;
												popCount += (count
														.doubleValue() * overrat);
												if (sharedData) {
													evaluate = canattribute
															.evaluate(feature);
													// System.out.println(evaluate);
													count = (Number) evaluate;
													canCount += count
															.doubleValue();
												}
											}
										}
									} finally {
										popIt.close();
									}
									// System.out.println("\tContaining " +
									// popCount + " people");
									// System.out.println("\tContaining " +
									// popCount + " people");
								} // get pop points in circle
								if (!sharedData) {
									bbox = new ReferencedEnvelope(circle
											.getBounds(), canCrs);
									filter = ff.bbox(cangeom, bbox);
									SimpleFeatureCollection canPoints = cansubset
											.subCollection(filter);
									if (canPoints.size() > 0) {
										// System.out.println(circle + " got " +
										// canPoints.size()
										// + " case points");
										canIt = canPoints.features();
										while (canIt.hasNext()) {
											SimpleFeature feature = canIt
													.next();
											Filter filter2 = ff.within(cangeom,
													ff.literal(circle
															.toPolygon()));
											if (filter2.evaluate(feature)) {
												final Object evaluate = canattribute
														.evaluate(feature);
												// System.out.println(evaluate);
												Number count = (Number) evaluate;
												canCount += count.doubleValue();
											}
										}
										canIt.close();
										// System.out.println("\tContaining " +
										// canCount +
										// " cases");
										// System.out.println("\tContaining " +
										// canCount +
										// " cases");
									} // canPoints > 0
								} // sharedData
							}
							if (test.isWorthTesting(popCount, canCount)) {
								if (test.isSignificant(popCount, canCount)) {
									double stat = test.getStatistic();
									circle.setStatistic(stat);
									results.add(circle);
									// System.out.println(circle + " " + stat);
								} else {
									// System.out.println("not significant with "
									// + popCount
									// + canCount + " cases");
								}
							} else {
								// System.out.println("not worth testing " +
								// popCount + " with "
								// + canCount + " cases");
							}
						} // Y loop
						// Y loop
					}
				} finally {
					if(popIt!=null)popIt.close();
					if(canIt!=null)canIt.close();
				}
			} // X loop
			// X loop
		}
		return results;
	}
}
