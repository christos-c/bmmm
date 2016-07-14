package tagInducer;

import tagInducer.corpus.CCGJSONCorpus;
import tagInducer.corpus.Corpus;
import tagInducer.utils.CollectionUtils;
import tagInducer.utils.StringCoder;
import tagInducer.utils.StringUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Implements all the evaluation measures used in the 
 * 2010 EMNLP paper (except substitutable f-score)
 * 
 * @author Christos Christodoulopoulos
 */
public class Evaluator {
	
	private int[] clusters;
	private int numClusters;
	
	private int[] goldTags;
	private int numGoldTags;
	
	private int[][] coocCounts;
	private int[] clusterCounts;
	private int[] goldTagCounts;

	private int totalWords;
	private double clusterEntropy;
	private double tagEntropy;
	private double mutualInformation;

	public Evaluator(Corpus corpus) {
		this(corpus, "fine");
	}

	private Evaluator(Corpus corpus, String goldType) {
		List<Integer> c = new ArrayList<>();
		List<Integer> g = new ArrayList<>();

		String[][] tags;
		switch (goldType) {
			case "upos":
				tags = corpus.getCorpusUPOS();
				break;
			case "ccg":
				tags = corpus.getCorpusCCGCats();
				break;
			default:
				tags = corpus.getCorpusGoldTags();
				break;
		}
		int[][] clusters = corpus.getCorpusClusters();

		StringCoder goldTagCoder = new StringCoder();
		StringCoder clusterCoder = new StringCoder();
		String[][] sents = corpus.getCorpusOriginalSents();
		for (int sentId = 0; sentId < sents.length; sentId++) {
			for (int wordId = 0; wordId < sents[sentId].length; wordId++) {
				// Code the clusters as well since there is the case of -1 for punct
				c.add(clusterCoder.encode(clusters[sentId][wordId]+""));
				g.add(goldTagCoder.encode(tags[sentId][wordId]));
			}
		}
		setData(CollectionUtils.toArray(c), CollectionUtils.toArray(g));
	}

	private Evaluator(Corpus corpus, String goldType, String fromType) {
		List<Integer> c = new ArrayList<>();
		List<Integer> g = new ArrayList<>();

		String[][] tags, clusters;
		switch (goldType) {
			case "upos":
				tags = corpus.getCorpusUPOS();
				break;
			case "ccg":
				tags = corpus.getCorpusCCGCats();
				break;
			default:
				tags = corpus.getCorpusGoldTags();
				break;
		}
		switch (fromType) {
			case "fromUPOS":
				clusters = corpus.getCorpusUPOS();
				break;
			case "fromFine":
				clusters = corpus.getCorpusGoldTags();
				break;
			default:
				clusters = null;
				break;
		}
		assert clusters != null;

		StringCoder goldTagCoder = new StringCoder();
		StringCoder clusterCoder = new StringCoder();
		String[][] sents = corpus.getCorpusOriginalSents();
		for (int sentId = 0; sentId < sents.length; sentId++) {
			for (int wordId = 0; wordId < sents[sentId].length; wordId++) {
				// Code the clusters as well since there is the case of -1 for punct
				c.add(clusterCoder.encode(clusters[sentId][wordId]));
				g.add(goldTagCoder.encode(tags[sentId][wordId]));
			}
		}
		setData(CollectionUtils.toArray(c), CollectionUtils.toArray(g));
	}
	
	private void setData(int[] clusters, int[] goldTags){
		this.clusters = clusters;
		this.goldTags = goldTags;

		//Make sure that both array have the same number of words
		assert clusters.length == goldTags.length;
		this.totalWords = clusters.length;

		//Induce the number of gold tags
		numGoldTags = 0;
		List<Integer> seenTags = new ArrayList<>();
		for (int goldTag:goldTags){
			if (!seenTags.contains(goldTag)){
				numGoldTags++;
				seenTags.add(goldTag);
			}
		}
		
		//Use the max cluster index instead
		numClusters = -1;
		for (int cluster:clusters){
			if (cluster > numClusters) numClusters = cluster;
		}
		
		numClusters++;
		
		clusterCounts = new int[numClusters];
		goldTagCounts = new int[numGoldTags];
		coocCounts = new int[numClusters][numGoldTags];
		
		//Initialise arrays
		Arrays.fill(clusterCounts, 0);
		Arrays.fill(goldTagCounts, 0);
		for (int[] cluster:coocCounts) {
			Arrays.fill(cluster, 0);
		}
		
		//Count the co-occurrences of cluster i with goldTag j
		for (int word = 0; word < clusters.length; word++){
			clusterCounts[clusters[word]]++;
			goldTagCounts[goldTags[word]]++;
			coocCounts[clusters[word]][goldTags[word]]++;
		}
		
	}
	
	double manyToOne(){
		Map<Integer, Integer> manyToOneMap = new HashMap<>();
		
		//Many-to-one mapping
		for (int cluster = 0; cluster < coocCounts.length; cluster++){
			int mostFreqTag = 0; int mostFreqTagCount = 0;
			//Find the most frequent tag
			for (int goldTag = 0; goldTag < coocCounts[cluster].length; goldTag++){
				if (coocCounts[cluster][goldTag] > mostFreqTagCount){
					mostFreqTag = goldTag;
					mostFreqTagCount = coocCounts[cluster][goldTag];
				}
			}
			manyToOneMap.put(cluster, mostFreqTag);
		}
		
		int correctTags = 0;
		for (int word = 0; word < goldTags.length; word++) {
			if (goldTags[word] == manyToOneMap.get(clusters[word])) correctTags++;
		}
		
		return ((double)correctTags/(double)goldTags.length)*100;
	}

	/**
	 * Calculates V-Measure as defined in Rosenberg & Hirschberg (2007).
	 * @return The array of V-Measure [0], homogeneity [1], and completeness [2]
	 */
	double[] VMeasure(){
		//H(CL): Cluster entropy
		if (clusterEntropy == 0)
			clusterEntropy = entropy(clusterCounts, numClusters);

		//H(T): Tag entropy
		if (tagEntropy == 0)
			tagEntropy = entropy(goldTagCounts, numGoldTags);

		//I(CL,T): Mutual information
		if (mutualInformation == 0)
			mutualInformation = mutualInformation();
		
		//H(CL|T): Conditional cluster entropy
		double H_CL_T = clusterEntropy - mutualInformation;
		//H(T|CL): Conditional tag entropy
		double H_T_CL = tagEntropy - mutualInformation;
		//Completeness: 1-H(T|CL)/H(T)
		// In order to satisfy the completeness criterion, a clustering
		// must assign *all* of those datapoints that are members of a single class to a single cluster.
		double c=1-(H_CL_T/clusterEntropy);
		//Homogeneity: 1-H(CL|T)/H(CL)
		// In order to satisfy the homogeneity criterion, a clustering
		// must assign *only* those datapoints that are members of a single class to a single cluster.
		double h=1-(H_T_CL/tagEntropy);
		//V-Measure = (2*h*c)/(h+c)
		double VM=(2*h*c)/(h+c);

		double[] vmResults = new double[3];
		vmResults[0] = VM*100;
		vmResults[1] = h*100;
		vmResults[2] = c*100;
		return vmResults;
	}
	
	double VI() {
		//H(CL): Cluster entropy
		if (clusterEntropy == 0)
			clusterEntropy = entropy(clusterCounts, numClusters);

		//H(T): Tag entropy
		if (tagEntropy == 0)
			tagEntropy = entropy(goldTagCounts, numGoldTags);

		//I(CL,T): Mutual information
		if (mutualInformation == 0)
			mutualInformation = mutualInformation();
		
		//H(CL|T): Conditional cluster entropy
		double H_CL_T= clusterEntropy - mutualInformation;
		//H(T|CL): Conditional tag entropy
		double H_T_CL= tagEntropy - mutualInformation;
		//VI(CL,T) = H(CL|T)+H(T|CL)
		return H_CL_T+H_T_CL;
	}

	private double entropy(int[] clusterCounts, int numClusters) {
		double entropy = 0;
		for (int cluster = 0; cluster < numClusters; cluster++) {
			double prob = (double)clusterCounts[cluster] / totalWords;
			if(prob != 0){
				entropy -= prob*Math.log(prob)/Math.log(2);
			}
		}
		return entropy;
	}

	private double mutualInformation() {
		double MI = 0;
		for (int cluster = 0; cluster < numClusters; cluster++) {
			double clProb = (double)clusterCounts[cluster] / totalWords;
			for (int goldTag = 0; goldTag < numGoldTags; goldTag++) {
				double tagProb = (double)goldTagCounts[goldTag] / totalWords;
				double prob;
				prob = (double)coocCounts[cluster][goldTag] / totalWords;
				if(prob != 0) {
					MI += prob*Math.log(prob/(tagProb*clProb)) / Math.log(2);
				}
			}
		}
		return MI;
	}

	public String scoresSummary() {
		return "M-1:\t" + StringUtils.twoDecimal(manyToOne()) + "\n" +
				"VI:\t" + StringUtils.twoDecimal(VI()) + "\n" +
				"VM:\t" + StringUtils.twoDecimal(VMeasure()[0]) + "\n" +
				"VM-Heterogeneity:\t" + StringUtils.twoDecimal(VMeasure()[1]) + "\n" +
				"VM-Completeness:\t" + StringUtils.twoDecimal(VMeasure()[2]);
	}

	public static void main(String[] args) {
		if (args.length < 1 || (args.length > 1 &&
				!(args[1].equals("-upos") || args[1].equals("-ccg") || args[1].equals("-fine")))) {
			String usage = "Usage:\n$>tagInducer.Evaluator <corpus> [-upos|-ccg|-fine] [-fromUPOS|-fromFine]\n" +
					"\t-upos: Compare against UPOS instead of fine-grained tags (6th column).\n" +
					"\t-ccg: Compare against CCG-cats instead of fine-grained tags (7th column).\n" +
					"\t-fine: Compare against fine-grained tags (4th column; only use when using -fromX).\n\n" +
					"\t-fromUPOS: Compare UPOS against the first category (either -fine or -ccg).\n" +
					"\t-fromFine: Compare fine-grained against the first category (either -upos or -ccg).";
			System.err.println(usage);
			System.exit(1);
		}
		else {
			OptionsCmdLine options = new OptionsCmdLine(new String[]{"-in", args[0]});
			Corpus corpus;
			if (options.getCorpusFileName().contains("json")) {
				corpus = new CCGJSONCorpus(options);
			}
			else corpus = new Corpus(options);
			Evaluator eval;
			if (args.length == 2) {
				eval = new Evaluator(corpus, args[1].substring(1));
			}
			else if (args.length > 2) {
				eval = new Evaluator(corpus, args[1].substring(1), args[2].substring(1));
			}
			else eval = new Evaluator(corpus);
			System.out.println(eval.scoresSummary());
		}
	}
}
