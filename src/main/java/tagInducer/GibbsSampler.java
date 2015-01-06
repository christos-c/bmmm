package tagInducer;

import tagInducer.corpus.Corpus;
import utils.FileUtils;
import utils.MathsUtils;
import utils.StringUtils;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class GibbsSampler {

	/** The corpus data (bag of words) */
	private int[] words;
	/** A map of features counts (MxF) for each feature type */
	private Map<String, int[][]> features;
	/** The gold-standard tags (if they exist) */
	private int[] goldTags;
	/** Vocabulary size (number of word types) */
	private int numTypes;
	private int numClasses;
	/** Dirichlet parameters controlling phi (feature parameters) */
	private Map<String, Double> hyperFeats;
	/** Dirichlet parameter controlling theta (class parameters) */
	private double hyperClass;
	/** Cluster assignments for each word type */
	private int z[];
	/** Best cluster assignments according to logProb */
	private int bestZ[];
	/** Total number of words assigned to cluster j */
	private int[] typesPerClass;
	private int ITERATIONS;
	private boolean HAS_TAGS = false;
	private Evaluator eval;
	/** Counts of feature tokens per class (NxF) a specific feature type */
	private Map<String, int[][]> featuresPerClass;
	/** Counts total number of feature tokens in class (sum over all context words) */
	private Map<String, int[]> sumClassFeatures;
	private boolean printLog;
	/** Annealing temperature schedule */
	private double[] temperatures;
	private static final int tempIncrements = 20;
	/** Number of hyperparameter sampling iterations */
	private static final int HYPERSAMPLE_ITERATIONS = 5;
	/** The standard deviation for generating new hypersamples */
	private static double HYPERSAMPLING_RATIO = 0.1;
	private double bestClassLogP = Double.NEGATIVE_INFINITY;

	private StringUtils s = new StringUtils();
	private MathsUtils m = new MathsUtils();

	private boolean outToSingleFile;

	/**
	 * Initialise the Gibbs sampler and configure the sampling options.
	 */
	public GibbsSampler(Corpus corpus, Options o) {
		words = corpus.getWords();
		features = corpus.getFeatures();
		numTypes = corpus.getNumTypes();
		if (corpus.hasTags()){
			goldTags = corpus.getGoldTags();
			HAS_TAGS = true;
			eval = new Evaluator();
		}
		this.numClasses = o.getNumClasses();
		this.printLog = o.isPrintLog();
		this.outToSingleFile = o.isOutputToSingleFile();
	}

	public void setRunParameters(int iters){
		//Initialise betas
		hyperFeats = new HashMap<>();
		for (String feat:features.keySet()) hyperFeats.put(feat, 0.1);
		//Initialise alpha
		this.hyperClass = 0.1;
		this.ITERATIONS = iters;
		//Create the temperature schedule
		createTempSchedule();
	}

	private void createTempSchedule() {
		double annealIters = ITERATIONS-(Math.round(ITERATIONS*0.5));
		double annealStartTemp = 2;
		double annealStopTemp = 1;
		double annealA = 10;
		double annealB = 0.2;
		temperatures = new double[ITERATIONS];
		Arrays.fill(temperatures, 1);
		for (int i = 0; i < annealIters; i++){
			double x = i/annealIters;
			double s = 1/(1+Math.exp(annealA*(x-annealB)));
			double s0 = 1/(1+Math.exp(annealA*(0-annealB)));
			double s1 = 1/(1+Math.exp(annealA*(1-annealB)));
			double temp = (annealStartTemp-annealStopTemp)*(s-s1)/(s0-s1)+annealStopTemp;
			temperatures[i] = 1/temp;
		}
		double[] annealTemps = new double[5];
		double temp = 1;
		for (int i = 0; i < 5; i++){
			temp *= 1.11;
			annealTemps[i] = temp;
		}
		int tempIndex = 0;
		int iterIncrements = ITERATIONS / tempIncrements;
		int start = ITERATIONS-(4* iterIncrements);
		for (int i = start; i < ITERATIONS; i++){
			if (i % iterIncrements == 0) temperatures[i] = annealTemps[tempIndex++];
			else temperatures[i] = temperatures[i-1];
		}
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
		if (!printLog) System.out.print("Iter: " + i + "/" + ITERATIONS);
		for (i = 1; i <= ITERATIONS; i++) {
			temperature = temperatures[tempIndex++];

			//**Main Loop**
			for (int type = 0; type < z.length; type++) {
				//Sample a new cluster
				z[type] = sampleCluster(type, temperature);
				//Add newly estimated z_i to count variables
				typesPerClass[z[type]]++;
			}
			//END:**Main Loop**

			// Do the hyperparameter sampling every 5 iterations
			if (i % 5 == 0) {
				//Calculate the log posterior
				double prior = totalLogPrior(hyperClass);
				double classLikelihood = totalLogLikelihood(hyperFeats);
				double classPosterior = prior + classLikelihood;
				//Check if we increased the logProb and if so, store this model
				if (classPosterior > bestClassLogP) {
					bestClassLogP = classPosterior;
					bestZ = new int[z.length];
					System.arraycopy(z, 0, bestZ, 0, z.length);
				}
				//Sample for class/feature hyperparameters
				sampleHyper(temperature, classPosterior);

				//If we are *not* outputting only the tagged file
				if (!outToSingleFile) {
					//Enable this for logging
					writeLog(outDir, i, classPosterior, temperature);
				}
			}

			if (!printLog) System.out.print(s.del(i + "/" + ITERATIONS)+i+"/"+ITERATIONS);
		}
		// Run an evaluation at the end
		if (HAS_TAGS) {
			System.out.println();
			eval.setData(getTokenAssignments(true), goldTags);
			double accuracy = eval.manyToOne();
			double vm = eval.VMeasure();
			System.out.println("M-1: "+accuracy+"\tVM: "+vm);
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
			p[cluster] += logLikelihood(cluster, type, hyperFeats);
		}

		//Perform annealing (since we're in log space we multiply)
		for (cluster = 0; cluster < numClasses; cluster++)
			p[cluster] *= temperature;


		//Draw a cluster from the multinomial p
		cluster = m.multSampleLog(p);

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

	/**
	 * Initialisation
	 *
	 * @param numClusters number of clusters
	 */
	public void initialise(int numClusters) {
		typesPerClass = new int[numClusters];
		featuresPerClass = new HashMap<>();
		sumClassFeatures = new HashMap<>();

		for (String featType:features.keySet()){
			int[][] temp = new int[numClusters][features.get(featType)[0].length];
			for (int[] feats:temp) Arrays.fill(feats, 0);
			featuresPerClass.put(featType, temp);
			int[] temp2 = new int[numClusters];
			Arrays.fill(temp2, 0);
			sumClassFeatures.put(featType, temp2);
		}

		z = new int[numTypes];
		//For every word type
		int cluster;
		for (int type = 0; type < numTypes; type++) {
			cluster = (int) (Math.random() * numClusters);
			z[type] = cluster;

			//Total number of words assigned to cluster j.
			typesPerClass[cluster]++;

			for (String featType:features.keySet()){
				int clustSum = 0;
				for (int feature = 0; feature < features.get(featType)[0].length; feature++){
					int sum = features.get(featType)[type][feature];
					featuresPerClass.get(featType)[cluster][feature] += sum;
					clustSum += sum;
				}
				sumClassFeatures.get(featType)[cluster] += clustSum;
			}
		}
		//Initialise bestZ
		bestZ = new int[z.length];
		System.arraycopy(z, 0, bestZ, 0, z.length );
	}

	/**
	 * Hyperparameter sampling.<br>
	 * Based on the Metropolis-Hastings sampler of Goldwater & Griffiths (2007)<br>
	 * Sample more than once for better results.
	 * @param temperature The annealing temperature
	 */
	protected void sampleHyper(double temperature, double oldPosterior) {
		for (int i = 0; i < HYPERSAMPLE_ITERATIONS; i++){
			//SAMPLE ALPHAS (hyperClass)
			double newHyper = m.randNormal(hyperClass, hyperClass * HYPERSAMPLING_RATIO);
			if (acceptHyper(newHyper, hyperFeats, hyperClass, newHyper, oldPosterior, temperature))
				hyperClass = newHyper;

			//SAMPLE BETAS (hyperFeats)
			for (String feat:hyperFeats.keySet()){
				Map<String, Double> newHyperFeats = new HashMap<>(hyperFeats);
				double hyperFeat = hyperFeats.get(feat);
				double newHyperFeat = m.randNormal(hyperFeat, hyperFeat * HYPERSAMPLING_RATIO);
				newHyperFeats.put(feat, newHyperFeat);
				if (acceptHyper(hyperClass, newHyperFeats, hyperFeat, newHyperFeat, oldPosterior, temperature))
					hyperFeats.put(feat, newHyperFeat);
			}
		}
	}
	private boolean acceptHyper(double hyperCl, Map<String, Double> hyperFeats,
								double oldHyper, double newHyper, double oldP, double temp){
		double newP = totalLogPrior(hyperCl)+ totalLogLikelihood(hyperFeats);
		double r = Math.exp(newP-oldP)*
				m.densityNorm(oldHyper, newHyper, newHyper * HYPERSAMPLING_RATIO) /
				m.densityNorm(newHyper, oldHyper, oldHyper * HYPERSAMPLING_RATIO);
		r = Math.pow(r, temp);
		return (r >= 1) || (r >= Math.random());
	}

	/**
	 * Returns the log of the prior probability given the new assignments.<br>
	 * P(z_i)=P(z_1)*P(z_2|z_1)*P(z_3|z_2,z_1)...*P(z_i|z_1,...,z_{i-1})
	 * @param hyperClass The new value for the class hyperparameter
	 * @return The log prior probability
	 */
	protected double totalLogPrior(double hyperClass){
		double prior = 0;
		int cumTotTypes = 0;
		int[] cumTypesPerClass = new int[numClasses];
		Arrays.fill(cumTypesPerClass, 0);
		for (int wordType = 0; wordType < numTypes; wordType++) {
			int cluster = z[wordType];
			cumTypesPerClass[cluster]++;
			cumTotTypes++;
			prior += Math.log((cumTypesPerClass[cluster] + hyperClass) / ((cumTotTypes) + numClasses*hyperClass));
		}
		return prior;
	}

	/**
	 * Returns the log of the likelihood given the assignments so far.<br>
	 * P(f|z_i)=P(f|z_1)*P(f|z_1,z_2)*...*P(f|z_1,...,z_{i-1}).<br>
	 * @param hyperFeats The new value for the features hyperparameter
	 * @return The log likelihood
	 */
	protected double totalLogLikelihood(Map<String, Double> hyperFeats){
		double likelihood = 0;
		Map<String, int[][]> cumClassTokens = new HashMap<>();
		Map<String, int[]> cumTotClassTokens =  new HashMap<>();
		//Initialise the cumulative count maps
		for (String featType:features.keySet()){
			int classTokens[][] = new int[numClasses][features.get(featType)[0].length];
			cumClassTokens.put(featType, classTokens);
			int[] temp = new int[numClasses];
			cumTotClassTokens.put(featType, temp);
		}

		//Calculate the likelihood
		//XXX Using tied betas for all features
		for (int wordType = 0; wordType < numTypes; wordType++){
			int cluster = z[wordType];
			for (String featType:features.keySet()){
				for (int feat:features.get(featType)[wordType]) {
					cumTotClassTokens.get(featType)[cluster]+=feat;
				}
				int numFeatures = features.get(featType)[0].length;
				int totFeatTokens = 0;
				for (int feature = 0; feature < numFeatures; feature++){
					int countWordTypeTokens = features.get(featType)[wordType][feature];
					if (countWordTypeTokens==0) continue;
					cumClassTokens.get(featType)[cluster][feature]+=countWordTypeTokens;
					double featProb;
					for (int featureToken = 0; featureToken < countWordTypeTokens; featureToken++){
						featProb = (cumClassTokens.get(featType)[cluster][feature] + featureToken + hyperFeats.get(featType))/
								(cumTotClassTokens.get(featType)[cluster] + totFeatTokens++ + numFeatures* hyperFeats.get(featType));
						likelihood += Math.log(featProb);
					}
				}
			}
		}

		return likelihood;
	}

	private double logLikelihood(int cluster, int type, Map<String, Double> hyperFeats) {
		double likelihood = 0;
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
					featProb = (countClassTokens + featureToken + hyperFeats.get(featType))/
							(countTotClassTokens + totFeatTokens++ + numFeatures* hyperFeats.get(featType));
					likelihood += Math.log(featProb);
				}
			}
		}
		return likelihood;
	}

	private void writeLog(String outDir, int i, double classPosterior, double temp) throws IOException{
		BufferedWriter logOut = FileUtils.createOut(outDir + "/log", true);
		if (HAS_TAGS){
			eval.setData(getTokenAssignments(false), goldTags);
			double accuracy = eval.manyToOne();
			double vm = eval.VMeasure();
			if (printLog) System.out.println(i+" - class_post: "+classPosterior+"\tM-1: "+accuracy+"\tVM: "+vm);
			logOut.write(i + "\t" + classPosterior + "\t" + accuracy + "\t" + vm + "\t" + temp);
		}
		else {
			if (printLog) System.out.println(i+" - class_post: "+classPosterior);
			logOut.write(i + "\t" + classPosterior + "\t" + temp);
		}
		if (printLog) {
			System.out.println("\thyperClass:"+hyperClass+"\thyperFeats"+hyperFeats);
		}

		logOut.write("\t" + hyperClass + "\t" + hyperFeats);
		logOut.write("\n");
		logOut.close();
	}

	/**
	 * Use <code>z</code> and <code>words</code>
	 * to assign a cluster to every token of a word type
	 * @return The token-level assignments
	 */
	public int[] getTokenAssignments(boolean best) {
		int[] tokenClusters = new int[words.length];
		if (best)
			for (int word=0; word<words.length; word++)
				tokenClusters[word] = bestZ[words[word]];
		else
			for (int word=0; word<words.length; word++)
				tokenClusters[word] = z[words[word]];
		return tokenClusters;
	}
}
