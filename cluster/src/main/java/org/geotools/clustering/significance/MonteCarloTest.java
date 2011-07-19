package org.geotools.clustering.significance;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;

import org.geotools.clustering.ClusterException;
import org.geotools.clustering.ClusterMethodFactory;
import org.geotools.data.DataUtilities;
import org.geotools.data.Parameter;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.feature.SchemaException;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.filter.text.cql2.CQLException;
import org.geotools.filter.text.ecql.ECQL;
import org.geotools.text.Text;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.filter.expression.Expression;

public class MonteCarloTest extends SignificanceTest {
	// Monte Carlo test class for cluster methods
	double stat = 0.0;
	private SimpleFeatureCollection pop;
	private Expression popattribute;
	private SimpleFeatureCollection can;
	private Expression canattribute;
	
	private boolean sharedData;
	private int hH, maxMC;
	private ArrayList<SimpleFeature> features;
	public static final Parameter<Integer> MONTECARLOREJECTIONCOUNT = new Parameter<Integer>(
			"MONTECARLOREJECTIONCOUNT", Integer.class, Text
					.text("Monte Carlo Test rejection count"), Text
					.text("When to reject the Monte Carlo test"), false, 1, 50,
			20, null);
	public static final Parameter<Integer> MONTECARLOSAMPLESIZE = new Parameter<Integer>(
			"MONTECARLOSAMPLESIZE", Integer.class, Text
					.text("Monte Carlo Test sample size"),
			Text.text("The size of the sampe to use in the Monte Carlo test"),
			false, 50, 500, 200, null);

	public MonteCarloTest(Map<String, Object> params) throws ClusterException, SignificanceTestException {
		super(params);
		name = "Monte Carlo";
		if (canProcess(params)) {
			pop = (SimpleFeatureCollection) params
					.get(ClusterMethodFactory.POPULATION.key);
			String attributeStr = (String) params
					.get(ClusterMethodFactory.POPATTRIBUTE.key);
			popattribute = null;
			try {
				popattribute = ECQL.toExpression(attributeStr);
			} catch (CQLException e) {
				throw new ClusterException(e);
			}
			can = (SimpleFeatureCollection) params
					.get(ClusterMethodFactory.CANCER.key);
			attributeStr = (String) params
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
			hH = (Integer) params
					.get(MONTECARLOREJECTIONCOUNT.key);
			maxMC = (Integer) params
					.get(MONTECARLOSAMPLESIZE.key);// MonteCarloSampleSize();
			SimpleFeatureType TYPE;
			try {
				TYPE = DataUtilities.createType("Location", "location:Point,"
						+ "pop:Double," + "cases:Double");
			} catch (SchemaException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				throw new SignificanceTestException(e.getMessage());
			}
			

			SimpleFeatureBuilder featureBuilder = new SimpleFeatureBuilder(TYPE);
			features = new ArrayList<SimpleFeature>();
			if (!sharedData) {
				SimpleFeatureIterator popIt = null;
				int popcount = 0;
				try {
					popIt = pop.features();
					while (popIt.hasNext()) {
						SimpleFeature feature = popIt.next();
						final Object evaluate = popattribute.evaluate(feature);
						// System.out.println(evaluate);
						Number count1 = (Number) evaluate;
						popcount += count1.doubleValue();
						featureBuilder.add(feature.getDefaultGeometry());
						featureBuilder.add(count1);
						featureBuilder.add(0.0);
						SimpleFeature feature2 = featureBuilder.buildFeature(null);
						features.add(feature2);
					}
				} finally {
					popIt.close();
				}
				SimpleFeatureIterator canIt = null;

				try {
					canIt = can.features();
					while (canIt.hasNext()) {
						SimpleFeature feature = canIt.next();
						final Object evaluate = canattribute.evaluate(feature);
						// System.out.println(evaluate);
						Number count1 = (Number) evaluate;
						//System.out.println("cases sample "+feature.getID()+" "+count1);
						featureBuilder.add(feature.getDefaultGeometry());
						featureBuilder.add(0.0);
						featureBuilder.add(count1);
						SimpleFeature feature2 = featureBuilder.buildFeature(null);
						features.add(feature2);
					}
				} finally {
					canIt.close();
				}
			} else {
				SimpleFeatureIterator popIt = null;

				try {
					popIt = pop.features();
					while (popIt.hasNext()) {
						SimpleFeature feature = popIt.next();
						final Object evaluate = popattribute.evaluate(feature);
						// System.out.println(evaluate);
						Number count1 = (Number) evaluate;
						
						final Object evaluate2 = canattribute.evaluate(feature);
						// System.out.println(evaluate);
						Number count2 = (Number) evaluate2;
						
						featureBuilder.add(feature.getDefaultGeometry());
						featureBuilder.add(count1);
						//System.out.println("cases sample "+feature.getID()+" "+count2);
						featureBuilder.add(count2);
						SimpleFeature feature2 = featureBuilder.buildFeature(null);
						features.add(feature2);
					}
				} finally {
					popIt.close();
				}

			}

		}
	}



	@Override
	public double getStatistic() {
		// TODO Auto-generated method stub
		return stat;
	}

	@Override
	public boolean isSignificant(double obsP, double obsC)
			throws SignificanceTestException {
		double cases = obsC;
		
		double samp_cases = (double) 0.0;
		double popcount = obsP;
		double Z, prob;
		int psize, k, r, count;

		if (!isWorthTesting(obsP, obsC))
			return false;
		// generate a list of features to do selection against
		
		r = 0;
		count = 0;

		// psize = points.size();
		for (int i = 0; i < maxMC; i++)
		// for (int i=0; i<200; i++)
		{
			samp_cases = 0.0;
			// i think I need to be sampling from all of the points...!
			for (int j = 0; j < numberOfPoints; j++) {
				/*
				 * p2 = database.getRandomPoint(); samp_cases += p2.getCases();
				 */
				int rand = (int) (Math.random() * features.size());
				samp_cases += (Double) features.get(rand).getAttribute("cases");
			}
			//System.out.println("comparing "+cases+" with sample "+samp_cases);
			if (samp_cases > cases)
				r += 1;
			count += 1;
			if (r > hH)
				break; // stop early if its not going to be significant
		}
		Z = (double) (r + 1) / (count + 1);
		// calculate probability
		// System.out.println("Z:" +"  "+ Z +" "+ r +" "+ count);
		prob = Z;
		// if (Z < 0.05)
		if (Z < ((Double) parameters.get(THRESHOLD.key)).doubleValue()) {
			switch (((Integer) parameters.get(STATTYPE.key)).intValue()) {
			case 1:
				stat = cases - popcount;
				break;
			case 2:
				stat = cases / popcount;
				break;
			case 3:
				stat = (double) 1.0 - prob;
				break;
			}
			return true;
		} else
			return false;
	}

	boolean canProcess(Map<String, Object> params) {
		if (params == null) {
			return false;
		}
		if (super.canProcess(params)) {
			Parameter<?>[] arrayParameters = getParametersInfo();
			
			for (int i = 0; i < arrayParameters.length; i++) {
				Parameter<?> param = arrayParameters[i];

				if (!params.containsKey(param.key)) {
					if (param.required) {
						return false; // missing required key!
					} else {
						// insert default
						System.out.println("adding " + param.key
								+ " with a value of " + param.sample);
						parameters.put(param.key, param.sample);
					}
				}

			}
			return true;
		}
		return false;
	}
	
	public final Parameter<?>[] getParametersInfo() {
        LinkedHashMap<String, Parameter<?>> map = new LinkedHashMap<String, Parameter<?>>();
        setupParameters(map);

        return map.values().toArray(new Parameter<?>[map.size()]);
    }
	void setupParameters(LinkedHashMap<String, Parameter<?>> map) {
		super.setupParameters(map);
		map.put(MONTECARLOREJECTIONCOUNT.key, MONTECARLOREJECTIONCOUNT);
		map.put(MONTECARLOSAMPLESIZE.key, MONTECARLOSAMPLESIZE);
	}
}
