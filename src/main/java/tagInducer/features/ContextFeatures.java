package tagInducer.features;

import tagInducer.corpus.Corpus;
import tagInducer.utils.CollectionUtils;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ContextFeatures implements Features {
	private final Corpus corpus;
	private final List<String> frequentWordList;

	public ContextFeatures(Corpus corpus, int numContextFeatWords) throws IOException{
		this.corpus = corpus;

		//Create a list of feature words (N most frequent original words)
		Map<String, Integer> wordFreq = new HashMap<>();
		//Get the word counts
		for (String[] sent : corpus.getCorpusOriginalSents()) {
			for (String word : sent) {
				if (wordFreq.containsKey(word)) wordFreq.put(word, wordFreq.get(word) + 1);
				else wordFreq.put(word, 1);
			}
		}
		//Resort wrt frequency and prune
		// Check in case we have less than numContextFeatWords in the corpus
		numContextFeatWords = Math.min(numContextFeatWords, wordFreq.size());
		frequentWordList = CollectionUtils.sortByValueList(wordFreq).subList(0, numContextFeatWords);
	}

	@Override
	public int[][] getFeatures() {
		int numContextFeats = frequentWordList.size();
		int numTypes = corpus.getNumTypes();
		// Context features: +/- 1 word and two NULL features (no freq. context words on either side)
		// (add two extra features to the array)
		int totalFeatNum = (numContextFeats * 2) + 2;
		int[][] features = new int[numTypes][totalFeatNum];
		int[][] leftFeatures = new int[numTypes][numContextFeats + 1];
		int[][] rightFeatures = new int[numTypes][numContextFeats + 1];
		for (String[] sentence: corpus.getCorpusOriginalSents()) {
			//For every target word in the sentence
			for (int pos = 0; pos < sentence.length; pos++) {
				int wordType = corpus.getWordType(sentence[pos]);

				int prevPosition = pos - 1;
				//Add the previous context word
				if (prevPosition >= 0) {
					String prevWord = sentence[prevPosition];
					int prevContextWordIndex = frequentWordList.indexOf(prevWord);
					if (prevContextWordIndex > 0) leftFeatures[wordType][prevContextWordIndex]++;
					// If no freq. context word to the right add NULL feature
					// (first extra feature)
					else leftFeatures[wordType][numContextFeats]++;
				}

				int nextPosition = pos + 1;
				//Add the next context word
				if (nextPosition < sentence.length) {
					String nextWord = sentence[nextPosition];
					int nextContextWordIndex = frequentWordList.indexOf(nextWord);
					if (nextContextWordIndex > 0) rightFeatures[wordType][nextContextWordIndex]++;
					// If no freq. context word to the right add NULL feature
					// (second extra feature)
					else rightFeatures[wordType][numContextFeats]++;
				}
			}
		}
		for (int i = 0; i < leftFeatures.length; i++)
			System.arraycopy(leftFeatures[i], 0, features[i], 0, leftFeatures[i].length);
		for (int i = 0; i < rightFeatures.length; i++)
			System.arraycopy(rightFeatures[i], 0, features[i], leftFeatures[i].length, rightFeatures[i].length);
		return features;
	}
}
