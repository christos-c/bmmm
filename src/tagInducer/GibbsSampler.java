package tagInducer;

import tagInducer.corpus.Corpus;
import utils.FileUtils;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public abstract class GibbsSampler {

	/** The corpus data (bag of words) */
	protected int[] words;
	/** A map of features counts (MxF) for each feature type */
	protected Map<String, int[][]> features;
	/** The gold-standard tags (if they exist) */
	protected int[] goldTags;
	/** Vocabulary size (number of word types) */
	protected int numTypes;
	protected int numClasses;
	/** Dirichlet parameters controlling phi (feature parameters) */
	protected Map<String, Double> hyperFeats;
	/** Allows TWOWAY or FULL beta untying */
	private String untiedHyperType;
	protected Map<String, double[]> hyperFeatsUntied;
	/** Dirichlet parameter controlling theta (class parameters) */
	protected double hyperClass;
	/** Cluster assignments for each word type */
	protected int z[];
	/** Best cluster assignments according to logProb */
	protected int bestZ[];
	/** Total number of words assigned to cluster j */
	protected int[] typesPerClass;
	protected int ITERATIONS;
	protected boolean HAS_TAGS = false;
	protected Evaluator eval;
	/** Counts of feature tokens per class (NxF) a specific feature type */
	protected Map<String, int[][]> featuresPerClass;
	/** Counts total number of feature tokens in class (sum over all context words) */
	protected Map<String, int[]> sumClassFeatures;
	protected boolean printLog;
	/** Annealing temperature schedule */
	protected double[] temperatures;
	protected static int tempIncrements = 20;
	protected int iterIncrements;
	/** Number of hyperparameter sampling iterations */
	protected static int HYPERSAMPLE_ITERATIONS = 5;
	/** The standard deviation for generating new hypersamples */
	protected static double HYPERSAMPLING_RATIO = 0.1;
	protected double bestClassLogP = Double.NEGATIVE_INFINITY;
	protected int step;
	protected BufferedWriter logOut;

	protected FileUtils f = new FileUtils();
	protected Corpus corpus;
	
	protected boolean outToSingleFile;
	
	/**
	 * Initialise the Gibbs sampler and configure the sampling options.
	 */
	public GibbsSampler(Corpus corpus, Options o) {
		this.corpus = corpus;
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
		this.untiedHyperType = o.getTiedHyperType();
		this.outToSingleFile = o.isOutputToSingleFile();
	}
	
	public void setRunParameters(int iters){
		//Initialise betas
		hyperFeats = new HashMap<String, Double>();
		for (String feat:features.keySet()) hyperFeats.put(feat, 0.1);
		//Initialise alpha
		this.hyperClass = 0.1;
		//If we are using untied betas
		//Should get rid of hyperFeats at this point...
		if (untiedHyperType != null){
			hyperFeatsUntied = new HashMap<String, double[]>();
			for (String featType:hyperFeats.keySet()){
				double[] hypers;
				if (untiedHyperType.equals("twoway")) {
					hypers = new double[2];
				}
				else {
					int numFeatures = features.get(featType)[0].length;
					hypers = new double[numFeatures];
				}
				Arrays.fill(hypers, 0.1);
				hyperFeatsUntied.put(featType, hypers);
			}
		}
		this.ITERATIONS = iters;
		step = ITERATIONS / 50;
		//Create the temperature schedule
		createTempSchedule();
	}
	
	protected void createTempSchedule() {
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
		iterIncrements = ITERATIONS/tempIncrements;
		int start = ITERATIONS-(4*iterIncrements);
		for (int i = start; i < ITERATIONS; i++){
			if (i % iterIncrements == 0) temperatures[i] = annealTemps[tempIndex++];
			else temperatures[i] = temperatures[i-1];
		}
	}

	public abstract void gibbs(String outDir) throws IOException;
	
	/**
	 * Initialisation
	 * 
	 * @param numClusters number of clusters
	 */
	public void initialise(int numClusters) {
		typesPerClass = new int[numClusters];
		featuresPerClass = new HashMap<String, int[][]>();
		sumClassFeatures = new HashMap<String, int[]>();
		
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
			double newHyper = randNormal(hyperClass, hyperClass*HYPERSAMPLING_RATIO);
			if (untiedHyperType == null){
				if (acceptHyper(newHyper, hyperFeats, hyperClass, newHyper, oldPosterior, temperature))
				 	hyperClass = newHyper;
			}	
			else {
				if (acceptHyper(newHyper, hyperFeatsUntied, hyperClass, newHyper, oldPosterior, temperature))
					hyperClass = newHyper;
			}
			
			//SAMPLE BETAS (hyperFeats)
			//Tied betas
			if (untiedHyperType == null){
				for (String feat:hyperFeats.keySet()){
					Map<String, Double> newHyperFeats = new HashMap<String, Double>(hyperFeats);
					double hyperFeat = hyperFeats.get(feat);
					double newHyperFeat = randNormal(hyperFeat, hyperFeat*HYPERSAMPLING_RATIO);
					newHyperFeats.put(feat, newHyperFeat);
					if (acceptHyper(hyperClass, newHyperFeats, hyperFeat, newHyperFeat, oldPosterior, temperature)) 
						hyperFeats.put(feat, newHyperFeat);
				}
			}
			//Untied betas (whichever type)
			else {
				for (String feat:hyperFeatsUntied.keySet()){
					Map<String, double[]> newHyperFeats = new HashMap<String, double[]>(hyperFeatsUntied);
					double[] hyperFeatArr = hyperFeatsUntied.get(feat);
					//For each beta
					for (int hf = 0; hf < hyperFeatArr.length; hf++){
						double hyperFeat = hyperFeatArr[hf];
						double newHyperFeat = randNormal(hyperFeatArr[hf], hyperFeatArr[hf]*HYPERSAMPLING_RATIO);
						double[] newHyperFeatArr = new double[hyperFeatArr.length];
						System.arraycopy(hyperFeatArr, 0, newHyperFeatArr, 0, hyperFeatArr.length);
						newHyperFeatArr[hf] = newHyperFeat;
						newHyperFeats.put(feat, newHyperFeatArr);
						if (acceptHyper(hyperClass, newHyperFeats, hyperFeat, newHyperFeat, oldPosterior, temperature)) 
							hyperFeatsUntied.put(feat, newHyperFeatArr);
					}
				}
			}
		}
	}	
	private boolean acceptHyper(double hyperCl, Map<String, ?> hyperFeats, 
			double oldHyper, double newHyper, double oldP, double temp){
		double newP = logPrior(hyperCl)+logLikelihood(hyperFeats);
		double r = Math.exp(newP-oldP)*
		densityNorm(oldHyper, newHyper, newHyper*HYPERSAMPLING_RATIO)/
		densityNorm(newHyper, oldHyper, oldHyper*HYPERSAMPLING_RATIO);
		r = Math.pow(r, temp);
		if ((r >= 1) || (r >= Math.random())) return true;
		return false;
	}

	protected double randNormal(double mean, double stdDev){
		double r1 = Math.random();
		double r2 = Math.random();
		return stdDev*Math.sqrt(-2*Math.log(r1))*Math.cos(2*Math.PI*r2)+mean;
	}
	protected double densityNorm(double val, double mean, double stdDev){
		return 1.0/(stdDev*Math.sqrt(2*Math.PI))*Math.exp(-1*Math.pow(val-mean, 2)/(2*stdDev*stdDev));
	}
	
	protected int multSample(double[] p){
		int totObs = p.length;
		double[] dist = new double[totObs];
		System.arraycopy(p, 0, dist, 0, totObs);
		int sample;
		//Cumulate multinomial parameters
		for (sample = 1; sample < totObs; sample++) {
			dist[sample] += dist[sample-1];
		}
		//Sample from the cumulative distribution
		//Scaled sample because of unnormalised p[]
		double u = Math.random()*dist[totObs-1];
		for (sample = 0; sample < totObs; sample++) {
			if (dist[sample] > u) break;
		}
		return sample;
	}
	
	/** 
	 * Multinomial sample in log-space 
	 */
	protected int multSampleLog(double[] p){
		int totObs = p.length;
		double[] dist = new double[totObs];
		System.arraycopy(p, 0, dist, 0, totObs);
		double min = 0; double max = Double.NEGATIVE_INFINITY;
		for (int sample = 0; sample < totObs; sample++) {
			if (dist[sample] < min) min = dist[sample];
			if (dist[sample] > max) max = dist[sample];
		}
		boolean mult = false;
		if (max < -100) mult = true;
		for (int sample = 0; sample < totObs; sample++) {
			if (mult) {
				double div = min/(-100);
				dist[sample] = Math.exp(dist[sample]/div);
			}
			else dist[sample] = Math.exp(dist[sample]);
		}
		return multSample(dist);
	}
	
	/**
	 * Returns the log of the prior probability given the new assignments.<br>
	 * P(z_i)=P(z_1)*P(z_2|z_1)*P(z_3|z_2,z_1)...*P(z_i|z_1,...,z_{i-1})
	 * @param hyperClass The new value for the class hyperparameter
	 * @return The log prior probability
	 */
	protected double logPrior(double hyperClass){
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
	 * Modified to allow for untied betas with either two different values
	 * (one for every normal feature and one for the NULL) or <code>numFeatures</code>
	 * different values.
	 * @param genericHyperFeats The new value for the features hyperparameter
	 * @return The log likelihood
	 */
	@SuppressWarnings("unchecked")
	protected double logLikelihood(Map<String, ?> genericHyperFeats){
		double likelihood = 0;
		//Check whether we use tied or untied betas
		Map<String, Double> hyperFeatsTied = null;
		Map<String, double[]> hyperFeatsUntied = null;
		if (genericHyperFeats.values().iterator().next().getClass().equals(Double.class))
			hyperFeatsTied = (HashMap<String, Double>) genericHyperFeats;
		else hyperFeatsUntied = (HashMap<String, double[]>) genericHyperFeats;
		Map<String, int[][]> cumClassTokens = new HashMap<String, int[][]>();
		Map<String, int[]> cumTotClassTokens =  new HashMap<String, int[]>();
		//Initialise the cumulative count maps
		for (String featType:features.keySet()){
			int classTokens[][] = new int[numClasses][features.get(featType)[0].length];
			for (int[] i:classTokens) Arrays.fill(i,0);
			cumClassTokens.put(featType, classTokens);
			int[] temp = new int[numClasses];
			Arrays.fill(temp, 0);
			cumTotClassTokens.put(featType, temp);
		}
		
		//Calculate the likelihood
		//CASE 1: Using tied betas for all features
		if (hyperFeatsTied != null){
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
							featProb = (cumClassTokens.get(featType)[cluster][feature] + featureToken + hyperFeatsTied.get(featType))/
							(cumTotClassTokens.get(featType)[cluster] + totFeatTokens++ + numFeatures*hyperFeatsTied.get(featType));
							likelihood += Math.log(featProb);
						}
					}
				}
			}
		}
		//CASE 2: Using tied betas for all but the last feature
		else if (hyperFeatsUntied.values().iterator().next().length == 2){
			for (int wordType = 0; wordType < numTypes; wordType++){
				int cluster = z[wordType];
				for (String featType:features.keySet()){
					for (int feat:features.get(featType)[wordType]) {
						cumTotClassTokens.get(featType)[cluster]+=feat;
					}
					int numFeatures = features.get(featType)[0].length;
					int totFeatTokens = 0;
					//Tie the betas up to this point
					for (int feature = 0; feature < numFeatures-1; feature++){
						int countWordTypeTokens = features.get(featType)[wordType][feature];
						if (countWordTypeTokens==0) continue;
						cumClassTokens.get(featType)[cluster][feature]+=countWordTypeTokens;
						double featProb;
						for (int featureToken = 0; featureToken < countWordTypeTokens; featureToken++){
							featProb = (cumClassTokens.get(featType)[cluster][feature] + featureToken + hyperFeatsUntied.get(featType)[0])/
							(cumTotClassTokens.get(featType)[cluster] + totFeatTokens++ + numFeatures*hyperFeatsUntied.get(featType)[0]);
							likelihood += Math.log(featProb);
						}
					}
					//Different beta for the last (NULL) feature
					int feature = numFeatures-1;
					int countWordTypeTokens = features.get(featType)[wordType][feature];
					if (countWordTypeTokens==0) continue;
					cumClassTokens.get(featType)[cluster][feature]+=countWordTypeTokens;
					double featProb;
					for (int featureToken = 0; featureToken < countWordTypeTokens; featureToken++){
						featProb = (cumClassTokens.get(featType)[cluster][feature] + featureToken + hyperFeatsUntied.get(featType)[1])/
						(cumTotClassTokens.get(featType)[cluster] + totFeatTokens++ + numFeatures*hyperFeatsUntied.get(featType)[1]);
						likelihood += Math.log(featProb);
					}
				}
			}
		}
		//CASE 3: Using completely untied betas
		else {
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
							featProb = (cumClassTokens.get(featType)[cluster][feature] + featureToken + hyperFeatsUntied.get(featType)[feature])/
							(cumTotClassTokens.get(featType)[cluster] + totFeatTokens++ + numFeatures*hyperFeatsUntied.get(featType)[feature]);
							likelihood += Math.log(featProb);
						}
					}
				}
			}
		}
		return likelihood;
	}
	
	protected void writeLog(String outDir, int i, double classPosterior, double temp) throws IOException{
		logOut = f.createOut(outDir+"/log", true);
		if (HAS_TAGS){
			eval.setData(getTokenAssignments(false), goldTags);
			double accuracy = eval.manyToOne();
			double vm = eval.VMeasure();
			if (printLog) System.out.println(i+" - class_post: "+classPosterior+"\tM-1: "+accuracy+"\tVM: "+vm);
			logOut.write(i+"\t"+classPosterior+"\t"+accuracy+"\t"+vm+"\t"+temp);
		}
		else {
			if (printLog) System.out.println(i+" - class_post: "+classPosterior);
			logOut.write(i+"\t"+classPosterior+"\t"+temp);
		}
		if (printLog) {
			if (hyperFeatsUntied != null)
				System.out.println("\thyperClass:"+hyperClass+"\thyperFeats"+printTied(hyperFeatsUntied));
			else System.out.println("\thyperClass:"+hyperClass+"\thyperFeats"+hyperFeats);
		}

		if (hyperFeatsUntied != null) logOut.write("\t"+hyperClass+"\t"+printTied(hyperFeatsUntied));
		else logOut.write("\t"+hyperClass+"\t"+hyperFeats);
		logOut.write("\n");
		logOut.close();
	}
	
	private String printTied(Map<String, double[]> map) {
		String res = "";
		for (String feat:map.keySet()){
			res += feat+"=[";
			for (double f:map.get(feat)) res += f+"|";
			res = res.substring(0,res.length()-1);
			res += "] ";
		}
		return res;
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
	
	public int[] getClasses(boolean best){
		if (best) return bestZ;
		return z;
	}
}
