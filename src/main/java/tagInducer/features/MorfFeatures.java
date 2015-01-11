package tagInducer.features;

import tagInducer.corpus.Corpus;
import tagInducer.utils.FileUtils;
import tagInducer.utils.StringUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class MorfFeatures implements Features {

	private StringUtils s = new StringUtils();
	private final Map<String, Map<String, Integer>> morphFeatCounts;
	private final boolean extendedMorphFeats;
	private final Corpus corpus;

	public MorfFeatures(Corpus corpus, String morphFile, boolean useExtendedMorphFeats) throws IOException {
		this.corpus = corpus;
		extendedMorphFeats = useExtendedMorphFeats;
		morphFeatCounts = readMorph(morphFile);
	}

	@Override
	public int[][] getFeatures() {
		//Add the NULL morph feature
		int numMorphFeats = morphFeatCounts.size() + 1;
		int numWordTypes = corpus.getNumTypes();
		int[][] morphFeats = new int[numWordTypes][numMorphFeats];

		//Now fill the last part of the feature vectors with morphological features
		//Get the word index
		int featIndex = 0;
		for (String morphFeat : morphFeatCounts.keySet()){
			for (String word : morphFeatCounts.get(morphFeat).keySet()){
				int wordIndex = corpus.getWordType(word);
				if (wordIndex == -1) continue;
				morphFeats[wordIndex][featIndex]++;
			}
			featIndex++;
		}
		//Add the NULL feature counts
		for (int i = 0; i < morphFeats.length; i++){
			if (isNull(morphFeats[i])) morphFeats[i][numMorphFeats-1]++;
		}

		if (extendedMorphFeats) {
			//Indices:
			//0: hasInitialCapital
			//1: hasDigit
			//2: hasHyphen
			//3: hasPunctuation
			int[][] extendedFeats = new int[numWordTypes][4];

			for (String[] sent : corpus.getCorpusOriginalSents()) {
				for (String word : sent) {
					int wordIndex = corpus.getWordType(word);
					if (wordIndex == -1) continue;

					if (word.matches("^[A-Z].*")) extendedFeats[wordIndex][0]++;
					if (word.matches(".*[0-9].*")) extendedFeats[wordIndex][1]++;
					if (word.contains("-")) extendedFeats[wordIndex][2]++;
					if (word.matches(".*\\W.*")) extendedFeats[wordIndex][3]++;
				}
			}
			int[][] newFeats = new int[numWordTypes][numMorphFeats + 4];
			for (int i = 0; i < numWordTypes; i++){
				System.arraycopy(morphFeats[i], 0, newFeats[i], 0, morphFeats[i].length);
				System.arraycopy(extendedFeats[i], 0, newFeats[i], morphFeats[i].length, extendedFeats[i].length);
			}
			return newFeats;
		}
		return morphFeats;
	}

	private boolean isNull(int[] vector) {
		for (int val:vector) if (val != 0) return false;
		return true;
	}

	private Map<String, Map<String, Integer>> readMorph(String morphFile) throws IOException{
		Map<String, Map<String, Integer>> morphFeatCounts = new HashMap<>();
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
		return morphFeatCounts;
	}
}
