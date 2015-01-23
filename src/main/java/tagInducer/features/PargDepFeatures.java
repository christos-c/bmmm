package tagInducer.features;

import tagInducer.corpus.Corpus;
import tagInducer.utils.CollectionUtils;
import tagInducer.utils.FileUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PargDepFeatures implements Features {

	/** A map from word types to dependency heads (with frequency) */
	private Map<Integer, Map<Integer, Integer>> headDepMap;

	private final Corpus corpus;

	private final int numContextWords;

	private final boolean undirDeps;

	public PargDepFeatures(Corpus corpus, String pargFile, int numContextWords, boolean undirDeps) throws IOException {
		this.corpus = corpus;
		this.numContextWords = numContextWords;
		this.undirDeps = undirDeps;
		this.headDepMap = new HashMap<>();

		//Create a list of feature words (N most frequent original words)
		Map<Integer, Integer> wordFreq = new HashMap<>();
		//Get the word counts
		for (int[] sent : corpus.getCorpusProcessedSents()) {
			for (int word : sent) {
				if (wordFreq.containsKey(word)) wordFreq.put(word, wordFreq.get(word) + 1);
				else wordFreq.put(word, 1);
			}
		}
		//Resort wrt frequency and prune
		// Check in case we have less than numContextFeatWords in the corpus
		numContextWords = Math.min(numContextWords, wordFreq.size());
		List<Integer> frequentWordList = CollectionUtils.sortByValueList(wordFreq).subList(0, numContextWords);

		//Read the features
		for (File file : FileUtils.listFilesMatching(pargFile)) {
			readDepFeats(file.getAbsolutePath(), frequentWordList);
		}
	}

	@Override
	public int[][] getFeatures() {
		//Generate the features
		//For the case of PoS tags as features
		//Number of features = #most frequent words +1 for NULL
		int[][] features = new int[corpus.getNumTypes()][numContextWords+1];

		for (int wordType : headDepMap.keySet()){
			Map<Integer, Integer> heads = headDepMap.get(wordType);
			for (int headType : heads.keySet()){
				features[wordType][headType] += heads.get(headType);
			}
		}
		return features;
	}

	/**
	 * Reads dependency data from a PARG file and uses the corpus to get the words and clusters
	 * @param pargFile The coder from word strings to integers
	 * @throws java.io.IOException
	 */
	public void readDepFeats(String pargFile, List<Integer> freqWordList) throws IOException {
		BufferedReader in = FileUtils.createIn(pargFile);
		String line;
		int[][] corpusSents = corpus.getCorpusProcessedSents();
		// The clusters from a previous run of the BMMM (otherwise the coarse tags)
		int[][] corpusTags = corpus.getCorpusClusters();
		int sentInd = -1;
		while ((line = in.readLine())!=null) {
			//Read through each sentence
			if (line.startsWith("<s>")) sentInd++;
			else if (!line.startsWith("<")) {
				String[] splits = line.split("\\s+");
				// *** IMPORTANT ***
				// We assume that the sentences/tokens mach exactly to the ones in the CoNLL corpus
				int headIndex = Integer.parseInt(splits[1]);
				int wordIndex = Integer.parseInt(splits[0]);
				int wordType = corpusSents[sentInd][wordIndex];
				int headType = corpusSents[sentInd][headIndex];
				if (freqWordList.contains(headType))
					addDep(wordType, freqWordList.indexOf(headType));
				else addDep(wordType, freqWordList.size());
				if (undirDeps) {
					//Now for the reverse dependency
					wordType = corpusSents[sentInd][headIndex];
					headType = corpusTags[sentInd][wordIndex];
					if (freqWordList.contains(headType))
						addDep(wordType, freqWordList.indexOf(headType));
					else addDep(wordType, freqWordList.size());
				}
			}
		}
	}

	private void addDep(int wordType, int head){
		if (headDepMap.containsKey(wordType)){
			Map<Integer, Integer> temp = headDepMap.get(wordType);
			if (temp.containsKey(head)) temp.put(head, temp.get(head)+1);
			else temp.put(head, 1);
			headDepMap.put(wordType, temp);
		}
		else {
			Map<Integer, Integer> temp = new HashMap<>();
			temp.put(head, 1);
			headDepMap.put(wordType, temp);
		}
	}
}
