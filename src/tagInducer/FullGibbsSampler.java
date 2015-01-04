package tagInducer;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import tagInducer.corpus.Corpus;

/**
 * A Gibbs sampler based on the LDA sampler
 * @author Christos Christodoulopoulos
 */
public class FullGibbsSampler extends GibbsSampler {

	public FullGibbsSampler(Corpus corpus, Options o) {
		super(corpus, o);
	}
	
	/** Helper function for pretty-printing progress (prints backspace characters) */
	private String del(String toDel){
		char bs = '\b';
		String d = "";
		for (int i = 0; i < toDel.toCharArray().length; i++) {
			d += bs;
		}
		return d;
	}


	/**
	 * Main method: Select initial state <br>
	 * Repeat a large number of times: <br>
	 * 1. Select an element <br>
	 * 2. Update conditional on other elements. <br>
	 * If appropriate, output summary for each run.
	 * @throws IOException 
	 */
	public void gibbs(String outDir) throws IOException {
		double temperature;
		int tempIndex = 0;
		
		int i=0;
		if (!printLog) System.out.print("Iter: ["+i+"/"+ITERATIONS+"]");
		for (i = 1; i <= ITERATIONS; i++) {
			temperature = temperatures[tempIndex++];
			
			double classPosterior = 0;
			//**Main Loop**
			for (int type = 0; type < z.length; type++) {
				//Sample a new cluster
				z[type] = sampleCluster(type, temperature);
				//Add newly estimated z_i to count variables
				typesPerClass[z[type]]++;		
			}
			//END:**Main Loop**
			
			//Calculate the log posterior
			double prior = logPrior(hyperClass);
			double classLikelihood;
			if (hyperFeatsUntied != null)
				classLikelihood = logLikelihood(hyperFeatsUntied);
			else classLikelihood = logLikelihood(hyperFeats);
			classPosterior += prior+classLikelihood;
			//Check if we increased the logProb and if so, store this model
			if (classPosterior > bestClassLogP){
				bestClassLogP = classPosterior;
				bestZ = new int[z.length];
				System.arraycopy(z, 0, bestZ, 0, z.length );
			}
			//Sample for class/feature hyperparameters
			sampleHyper(temperature, classPosterior);
			
			//If we are *not* outputting only the tagged file
			if (!outToSingleFile){
				//Enable this for logging
				writeLog(outDir, i, classPosterior, temperature);
			}
			if (!printLog) System.out.print(del(i+"/"+ITERATIONS+"]")+i+"/"+ITERATIONS+"]");
		}
	}

	/**
	 * Sample a cluster assignment z_i conditioned on the word type
	 * Modified to allow for untied betas (<code>hyperFeats</code>) with either two different values
	 * (one for every normal feature and one for the NULL) or <code>numFeatures</code>
	 * different values.
	 * @param type Word type index
	 * @param temperature The current annealing temperature
	 * @return Cluster number
	 */
	private int sampleCluster(int type, double temperature) {
		int cluster = z[type];
		//Discount type counts
		typesPerClass[cluster]--;

		//Discount feature token counts
		for (String featType:features.keySet()){
			int clustSum = 0;
			for (int feature = 0; feature < features.get(featType)[0].length; feature++){
				int sum = features.get(featType)[type][feature];
				featuresPerClass.get(featType)[cluster][feature] -= sum;
				clustSum += sum;
			}
			sumClassFeatures.get(featType)[cluster] -= clustSum;
		}

		//Do multinomial sampling via cumulative method:
		double[] p = new double[numClasses];
		//For every cluster
		for (cluster = 0; cluster < numClasses; cluster++) {
			//Calculate the cluster selection probability (the prior)
			double clusterProb = (typesPerClass[cluster] + hyperClass) / ((numTypes-1) + numClasses*hyperClass);
			p[cluster] = Math.log(clusterProb);
			
			//Calculate the feature emission probability (the features likelihood)
			//Check for the type of untied hyperparameters
			if (hyperFeatsUntied != null)
				p[cluster] += likelihood(cluster, type, hyperFeatsUntied);
			else p[cluster] += likelihood(cluster, type, hyperFeats);
		}
		
		//Perform annealing (since we're in log space we multiply)
		for (cluster = 0; cluster < numClasses; cluster++) 
			p[cluster] *= temperature;
		
		
		//Draw a cluster from the multinomial p
		cluster = multSampleLog(p);
		
		for (String featType:features.keySet()){
			int clustSum = 0;
			for (int feature = 0; feature < features.get(featType)[0].length; feature++){
				double sum = features.get(featType)[type][feature];
				featuresPerClass.get(featType)[cluster][feature] += sum;
				clustSum += sum;
			}
			sumClassFeatures.get(featType)[cluster] += clustSum;
		}
		return cluster;
	}


	@SuppressWarnings("unchecked")
	private double likelihood(int cluster, int type, Map<String, ?> genericHyperFeats) {
		double likelihood = 0;
		//Check whether we use tied or untied betas
		Map<String, Double> hyperFeatsTied = null;
		Map<String, double[]> hyperFeatsUntied = null;
		if (genericHyperFeats.values().iterator().next().getClass().equals(Double.class))
			hyperFeatsTied = (HashMap<String, Double>) genericHyperFeats;
		else hyperFeatsUntied = (HashMap<String, double[]>) genericHyperFeats;
		
		//Calculate the likelihood
		//CASE 1: Using tied betas for all features
		if (hyperFeatsTied != null){
			for (String featType:features.keySet()){
				int numFeatures = features.get(featType)[0].length;
				int countTotClassTokens = sumClassFeatures.get(featType)[cluster];
				int totFeatTokens = 0;
				//For every feature token of every non-zero feature type
				for (int feature = 0; feature < numFeatures; feature++){
					int featCounts = features.get(featType)[type][feature];
					if (featCounts==0) continue;
					double featProb;
					for (int featureToken = 0; featureToken < featCounts; featureToken++){
						//Count tokens of context word given class (context matrix column)
						int countClassTokens = featuresPerClass.get(featType)[cluster][feature];
						//Calculate the feature selection probability
						featProb = (countClassTokens + featureToken + hyperFeatsTied.get(featType))/
						(countTotClassTokens + totFeatTokens++ + numFeatures*hyperFeatsTied.get(featType));
						likelihood += Math.log(featProb);
					}
				}
			}
		}
		//CASE 2: Using tied betas for all but the last feature
		else if (hyperFeatsUntied.values().iterator().next().length == 2){
			for (String featType:features.keySet()){
				int numFeatures = features.get(featType)[0].length;
				int countTotClassTokens = sumClassFeatures.get(featType)[cluster];
				int totFeatTokens = 0;
				//Tie the betas up to this point
				for (int feature = 0; feature < numFeatures-1; feature++){
					int featCounts = features.get(featType)[type][feature];
					if (featCounts==0) continue;
					double featProb;
					for (int featureToken = 0; featureToken < featCounts; featureToken++){
						//Count tokens of context word given class (context matrix column)
						int countClassTokens = featuresPerClass.get(featType)[cluster][feature];
						//Calculate the feature selection probability
						featProb = (countClassTokens + featureToken + hyperFeatsUntied.get(featType)[0])/
						(countTotClassTokens + totFeatTokens++ + numFeatures*hyperFeatsUntied.get(featType)[0]);
						likelihood += Math.log(featProb);
					}
				}
				//Different beta for the last (NULL) feature
				int feature = numFeatures-1;
				int featCounts = features.get(featType)[type][feature];
				if (featCounts==0) continue;
				double featProb;
				for (int featureToken = 0; featureToken < featCounts; featureToken++){
					//Count tokens of context word given class (context matrix column)
					int countClassTokens = featuresPerClass.get(featType)[cluster][feature];
					//Calculate the feature selection probability
					featProb = (countClassTokens + featureToken + hyperFeatsUntied.get(featType)[1])/
					(countTotClassTokens + totFeatTokens++ + numFeatures*hyperFeatsUntied.get(featType)[1]);
					likelihood += Math.log(featProb);
				}
			}
		}
		//CASE 3: Using completely untied betas
		else {
			for (String featType:features.keySet()){
				int numFeatures = features.get(featType)[0].length;
				int countTotClassTokens = sumClassFeatures.get(featType)[cluster];
				int totFeatTokens = 0;
				//For every feature token of every non-zero feature type
				for (int feature = 0; feature < numFeatures; feature++){
					int featCounts = features.get(featType)[type][feature];
					if (featCounts==0) continue;
					double featProb;
					for (int featureToken = 0; featureToken < featCounts; featureToken++){
						//Count tokens of context word given class (context matrix column)
						int countClassTokens = featuresPerClass.get(featType)[cluster][feature];
						//Calculate the feature selection probability
						featProb = (countClassTokens + featureToken + hyperFeatsUntied.get(featType)[feature])/
						(countTotClassTokens + totFeatTokens++ + numFeatures*hyperFeatsUntied.get(featType)[feature]);
						likelihood += Math.log(featProb);
					}
				}
			}
		}
		return likelihood;
	}
}

