package tagInducer.features;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import tagInducer.Options;
import utils.FileUtils;
import utils.StringCoder;
import utils.StringUtils;

public class MorfFeatures {
	
	private String morphMethod, morphFile;
	private StringUtils s = new StringUtils();

	public MorfFeatures(StringCoder wordsCoder, Map<String, int[][]> featureVectors, int[] words, Options o)
			throws IOException {
		boolean extendedMorphFeats = o.isExtendedMorph();
		morphMethod = o.getMorphMethod();
		morphFile = o.getMorphFile();		
		
		Map<String, Map<String, Integer>> morphFeatCounts = readMorph(wordsCoder, words);

		//Add the NULL morph feature
		int numMorphFeats = morphFeatCounts.size()+1;
		int[][] morphFeats = new int[wordsCoder.size()][numMorphFeats];
		for (int[] feats:morphFeats) Arrays.fill(feats, 0);

		//Now fill the last part of the feature vectors with morphological features
		//Get the word index
		int wordIndex; int featIndex = 0;
		for (String morphFeat:morphFeatCounts.keySet()){
			for (String word:morphFeatCounts.get(morphFeat).keySet()){
				if (!wordsCoder.exists(word)) continue;
				wordIndex = wordsCoder.encode(word);
				morphFeats[wordIndex][featIndex]++;
			}
			featIndex++;
		}
		//Add the NULL feature counts
		for (int i = 0; i < morphFeats.length; i++){
			if (isNull(morphFeats[i])) morphFeats[i][numMorphFeats-1]++;
		}
		
		if (extendedMorphFeats){
			//Indices:
			//0: hasInitialCapital
			//1: hasDigit
			//2: hasHyphen
			//3: hasPunctuation
			int[][] extendedFeats = new int[wordsCoder.size()][4];
			for (int[] feats:extendedFeats) Arrays.fill(feats, 0);
			
			for (int wordInd:wordsCoder.intSet()){
				String word = wordsCoder.decode(wordInd);
				if (word.matches("^[A-Z].*"))extendedFeats[wordInd][0]++;
				if (word.matches(".*[0-9].*")) extendedFeats[wordInd][1]++;
				if (word.contains("-")) extendedFeats[wordInd][2]++;
				if (word.matches(".*\\W.*"))extendedFeats[wordInd][3]++;
			}
			int[][] newFeats = new int[wordsCoder.size()][numMorphFeats+4];
			for (int i=0; i<wordsCoder.size(); i++){
				System.arraycopy(morphFeats[i], 0, newFeats[i], 0, morphFeats[i].length);
				
				System.arraycopy(extendedFeats[i], 0, newFeats[i], 
						morphFeats[i].length, extendedFeats[i].length);
			}
			featureVectors.put("MORPH", newFeats);
		}
		else featureVectors.put("MORPH", morphFeats);
	}

	private boolean isNull(int[] vector) {
		for (int val:vector) if (val != 0) return false;
		return true;
	}

	private Map<String, Map<String, Integer>> readMorph(StringCoder wordsCoder, int[] words) throws IOException{
		Map<String, Map<String, Integer>> morphFeatCounts = new HashMap<>();
		if (morphMethod.equalsIgnoreCase("morfessor")){
			BufferedReader in = FileUtils.createIn(morphFile);
			String line, word, suffix;
			String[] splits;			
			while ((line = in.readLine())!=null){
				if (line.startsWith("#")) continue;
				splits = line.split("\\s+");
				word = "";
				if (splits.length < 2) continue;
				//Ignore the number for now
				for (int i = 1; i < splits.length; i++){
					if (splits[i].startsWith("+")) continue;
					word += splits[i].substring(0, splits[i].indexOf("/"));
				}
				if (splits[splits.length-1].contains("SUF")){
					suffix = s.joinSuffixes(splits);
					if (morphFeatCounts.containsKey(suffix)){
						morphFeatCounts.get(suffix).put(word, 1);
					}
					else {
						Map<String, Integer> temp = new HashMap<>();
						temp.put(word, 1);
						morphFeatCounts.put(suffix, temp);
					}
				}
			}
		}
		//Use the final 2 letters of each word as features
		else {
			for (int wordInd:words){
				String wordType = wordsCoder.decode(wordInd);
				if (!s.checkString(wordType, 4)) continue;
				String suffix = wordType.substring(wordType.length()-2);
				if (morphFeatCounts.containsKey(suffix)) {
					if (!morphFeatCounts.get(suffix).containsKey(wordType))
						morphFeatCounts.get(suffix).put(wordType, 1);
				}
				else {
					Map<String, Integer> temp = new HashMap<>();
					temp.put(wordType, 1);
					morphFeatCounts.put(suffix, temp);
				}
			}
		}
		return morphFeatCounts;
	}
}
