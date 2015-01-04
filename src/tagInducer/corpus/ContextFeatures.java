package tagInducer.corpus;

import java.io.IOException;
import java.util.Map;

import utils.StringCoder;

public class ContextFeatures {

	public ContextFeatures(StringCoder wordsCoder, int numContextFeats, Map<String, int[][]> featureVectors,
			int[][] corpus, Map<Integer, Integer> freqMap) throws IOException{
		//Add the left and right NULL features (+2 in total)
		int[][] features = new int[wordsCoder.size()][(numContextFeats*2)+2];
		//This fills the feature vectors up to 2*numContextWords
		int[][] leftFeatures = new int[wordsCoder.size()][numContextFeats+1];
		int[][] rightFeatures = new int[wordsCoder.size()][numContextFeats+1];
		for (int[] sentence: corpus) {
			//For every target word in the sentence
			for (int pos=0; pos < sentence.length; pos++) {
				int wordIndex = sentence[pos];
				int prevContextPosition = pos-1;
				//Add the previous context word
				if (prevContextPosition >= 0) {
					int contextIndex =  sentence[prevContextPosition];
					if (freqMap.containsKey(contextIndex))
						leftFeatures[wordIndex][freqMap.get(contextIndex)]++;
					//Null left context
					else leftFeatures[wordIndex][numContextFeats]++;
				}
				int nextContextPosition = pos+1;
				//Add the next context word
				if (nextContextPosition < sentence.length) {
					int contextIndex =  sentence[nextContextPosition];
					if (freqMap.containsKey(contextIndex))
						rightFeatures[wordIndex][freqMap.get(contextIndex)]++;
					//Null right context
					else rightFeatures[wordIndex][numContextFeats]++;
				}
			}
		}
		for (int i=0; i<leftFeatures.length; i++)
			System.arraycopy(leftFeatures[i], 0, features[i], 0, leftFeatures[i].length);
		for (int i=0; i<rightFeatures.length; i++)
			System.arraycopy(rightFeatures[i], 0, features[i], leftFeatures[i].length, rightFeatures[i].length);
		featureVectors.put("ALL_CONTEXT", features);
	}
}
