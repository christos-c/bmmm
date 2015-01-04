package tagInducer;

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
	
	public void setData(int[] clusters, int[] goldTags){
		this.clusters = clusters;
		this.goldTags = goldTags;
		
		//Induce the number of gold tags
		//TODO: put this in the constructor
		numGoldTags = 0;
		List<Integer> seenTags = new ArrayList<Integer>();
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
		
		//Make sure that both array have the same number of words
		assert(clusters.length == goldTags.length);
		
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
		Map<Integer, Integer> manyToOneMap = new HashMap<Integer, Integer>();
		
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
	
	public double oneToOne(){
		return 0;
	}
	
	public double VMeasure(){
		int totalWords = clusters.length;
		double H_T=0, H_CL=0, I=0;
		
		//H(CL): Cluster entropy
		for (int cluster = 0; cluster < numClusters; cluster++){
			double prob = (double)clusterCounts[cluster]/totalWords;
			if(prob!=0){
				H_CL -= prob*Math.log(prob)/Math.log(2);
			}
		}
		//H(T): Tag entropy
		for (int goldTag=0; goldTag < numGoldTags; goldTag++){
			double prob = (double)goldTagCounts[goldTag]/totalWords;
			if(prob!=0){
				H_T -= prob*Math.log(prob)/Math.log(2);
			}
		}
		//I(CL,T): Mutual information
		for (int cluster = 0; cluster < numClusters; cluster++){
			double clProb = (double)clusterCounts[cluster]/totalWords;			
			for (int goldTag=0; goldTag < numGoldTags; goldTag++){
				double tagProb = (double)goldTagCounts[goldTag]/totalWords;
				double prob;
				prob = (double)coocCounts[cluster][goldTag]/totalWords;
				if(prob!=0){
					I += prob*Math.log(prob/(tagProb*clProb))/Math.log(2);			
				}
			}
		}
		
		//H(CL|T): Conditional cluster entropy
		double H_CL_T=H_CL-I;
		//H(T|CL): Conditional tag entropy
		double H_T_CL=H_T-I;
		//h=1-H(CL|T)/H(CL)
		double c=1-(H_CL_T/H_CL);
		//c=1-H(T|CL)/H(T)
		double h=1-(H_T_CL/H_T);
		//V-Measure = (2*h*c)/(h+c)
		double VM=(2*h*c)/(h+c);
		
		return VM*100;
	}
	
	public double VI(){
		int totalWords = clusters.length;
		double H_T=0, H_CL=0, I=0;
		
		//H(CL): Cluster entropy
		for (int cluster:clusters){
			double prob = (double)clusterCounts[cluster]/totalWords;
			if(prob!=0){
				H_CL -= prob*Math.log(prob)/Math.log(2);
			}
		}
		//H(T): Tag entropy
		for (int goldTag:goldTags){
			double prob = (double)goldTagCounts[goldTag]/totalWords;
			if(prob!=0){
				H_T -= prob*Math.log(prob)/Math.log(2);
			}
		}
		//I(CL,T): Mutual information
		for (int cluster:clusters){
			double clProb = (double)clusterCounts[cluster]/totalWords;			
			for (int goldTag:goldTags){
				double tagProb = (double)goldTagCounts[goldTag]/totalWords;
				double prob;
				prob = (double)coocCounts[cluster][goldTag]/totalWords;
				if(prob!=0){
					I += prob*Math.log(prob/(tagProb*clProb))/Math.log(2);			
				}
			}
		}
		
		//H(CL|T): Conditional cluster entropy
		double H_CL_T=H_CL-I;
		//H(T|CL): Conditional tag entropy
		double H_T_CL=H_T-I;
		//VI(CL,T) = H(CL|T)+H(T|CL)
		double VI=H_CL_T+H_T_CL;
		
		return VI;
	}
}
