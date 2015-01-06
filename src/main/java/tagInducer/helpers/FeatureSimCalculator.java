package tagInducer.helpers;

import java.util.HashMap;
import java.util.Map;

/**
 * Auxiliary class that calculates the amount of intra-cluster similarity
 * and inter-cluster diversity a feature provides.<br>
 * Used with input features combined by gold PoS tags.
 * @author Christos Christodoulopoulos
 */
public class FeatureSimCalculator {

	/**
	 * Assume a map of gold tags to a single feature array as input.
	 * All the arrays must have the same size. 
	 */
	public FeatureSimCalculator(Map<String, int[][]> featuresMap){
		//Intra-cluster similarity (lower scores better):
		//1.For each feature type (column) calculate the mean
		//2.For each word type (row) calculate the absolute distance from the mean
		//3.Sum over all word and feature types
		Map<String, double[]> means = new HashMap<>();
		Map<String, Double> scores = new HashMap<>();
		for (String pos:featuresMap.keySet()){
			if (featuresMap.get(pos).length==0) continue;
			//[word types] x [features]
			int[][] feat = featuresMap.get(pos);
			double[] mean = new double[feat[0].length];
			for (int[] aFeat : feat) {
				for (int j = 0; j < aFeat.length; j++) {
					mean[j] += aFeat[j];
				}
			}
			for (int i = 0; i < mean.length; i++) {
				mean[i] = mean[i]/feat.length;
			}
			means.put(pos, mean);
			double[][] dist = new double[feat.length][feat[0].length];
			double totSimScore = 0;
			for (int i = 0; i < feat.length; i++) {
				for (int j = 0; j < feat[i].length; j++) {
					dist[i][j] = Math.abs(feat[i][j]-mean[j]);
					totSimScore += dist[i][j];
				}
			}
			scores.put(pos, totSimScore);
		}
		//Inter-cluster diversity (lower scores better):
		//1.For each pairwise combinations of gold PoS tags 
		//2.Calculate the total (absolute) distance of the means of each feature type
		//3.Invert the score so that the highest score is 0
		//4.Sum over all combinations (higher score better)
		for (String pos1:featuresMap.keySet()){
			if (featuresMap.get(pos1).length==0) continue;
			double totDistScore = 0;
			double maxDist = 0;
			for (String pos2:featuresMap.keySet()){
				if (featuresMap.get(pos2).length==0) continue;
				if (pos1.equals(pos2)) continue;
				double dist = 0;
				for (int i = 0; i < means.get(pos1).length; i++) {
					dist += Math.abs(means.get(pos1)[i]-means.get(pos2)[i]);
				}
				
				if (dist > maxDist) maxDist = dist;
			}
			for (String pos2:featuresMap.keySet()){
				if (featuresMap.get(pos2).length==0) continue;
				if (pos1.equals(pos2)) continue;
				double dist = 0;
				for (int i = 0; i < means.get(pos1).length; i++) {
					dist += Math.abs(means.get(pos1)[i]-means.get(pos2)[i]);
				}
				totDistScore += maxDist-dist;
			}
			scores.put(pos1, (scores.get(pos1)+totDistScore)/featuresMap.size());
		}
		for (String pos:featuresMap.keySet()){
			if (featuresMap.get(pos).length==0) continue;
			System.out.println(pos+": "+scores.get(pos));
		}
	}
	
	public static void main(String[] args){
		int [][] featsNN = {{100, 34, 0}, {2, 45, 126}, {34, 568, 8}};
		int [][] featsVB = {{0, 250, 45}, {50, 0, 1}, {3, 88, 74}};
		int [][] featsJJ = {{87, 5, 86}, {96, 8, 456}, {8, 8, 0}};
		Map<String, int[][]> map = new HashMap<>();
		map.put("NN", featsNN);
		map.put("VB", featsVB);
		map.put("JJ", featsJJ);
		new FeatureSimCalculator(map);
		/* Sample code:
		Map<String, int[][]> featMap = new HashMap<String, int[][]>();
		for (String targetPOS:tagsCoder.stringSet()){
			StringCoder posWordCoder = new StringCoder();
			List<int[]> posFeats = new ArrayList<int[]>();
			List<String> seenWords = new ArrayList<String>();
			for (int wordInd = 0; wordInd < words.length; wordInd++){
				String word = wordsCoder.decode(words[wordInd]);
				if (seenWords.contains(word)) continue;
				seenWords.add(word);
				String tag = tagsCoder.decode(goldTags[wordInd]);
				if (!tag.equals(targetPOS)) continue;
				posWordCoder.encode(word);
				posFeats.add(features[wordsCoder.encode(word)]);
			}
			//Turn the list into an array
			int[][] t = new int[posFeats.size()][features[0].length];
			for (int f = 0; f < posFeats.size(); f++){
				t[f] = posFeats.get(f);
			}
			featMap.put(targetPOS, t);
		}
		new FeatureSimCalculator(featMap);
		*/
	}
}
