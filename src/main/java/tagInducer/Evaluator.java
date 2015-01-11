package tagInducer;

import tagInducer.corpus.Corpus;
import tagInducer.utils.CollectionUtils;
import tagInducer.utils.StringCoder;

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
	
	int[][] coocCounts;
	int[] clusterCounts;
	int[] goldTagCounts;

	int totalWords;
	private double clusterEntropy;
	private double tagEntropy;
	private double mutualInformation;

	public Evaluator(Corpus corpus, boolean useUPOS) {
		List<Integer> c = new ArrayList<>();
		List<Integer> g = new ArrayList<>();

		String[][] tags;
		if (useUPOS) tags = corpus.getCorpusUPOS();
		else tags = corpus.getCorpusGoldTags();
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
	
	public void setData(int[] clusters, int[] goldTags){
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
	
	public double manyToOne(){
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

	public double VMeasure(){
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
		//h=1-H(CL|T)/H(CL)
		double c=1-(H_CL_T/clusterEntropy);
		//c=1-H(T|CL)/H(T)
		double h=1-(H_T_CL/tagEntropy);
		//V-Measure = (2*h*c)/(h+c)
		double VM=(2*h*c)/(h+c);
		
		return VM*100;
	}
	
	public double VI() {
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

	public static void main(String[] args) {
		if (args.length < 1 || (args.length > 1 && !args[1].equals("-upos"))) {
			String usage = "Usage:\n$>tagInducer.Evaluator <corpus> [-upos]\n" +
					"\t-upos: Whether to compare against UPOS instead of fine-grained tags.";
			System.err.println(usage);
			System.exit(1);
		}
		else {
			Corpus corpus = new Corpus(new OptionsCmdLine(new String[]{"-in", args[0]}));
			Evaluator eval;
			if (args.length > 1)
				eval = new Evaluator(corpus, true);
			else eval = new Evaluator(corpus, false);
			System.out.println("M-1:\t" + eval.manyToOne() + "\nVM:\t" + eval.VMeasure() + "\nVI:\t" + eval.VI());
		}

	}
}
