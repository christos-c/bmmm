package tagInducer.features;

import tagInducer.corpus.Corpus;
import tagInducer.utils.FileUtils;
import tagInducer.utils.StringCoder;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class PargDepFeatures implements Features {

	/** A map from word types to dependency heads (with frequency) */
	private Map<Integer, Map<Integer, Integer>> headDepMap;

	private final Corpus corpus;

	public PargDepFeatures(Corpus corpus, String pargFile) throws IOException {
		this.corpus = corpus;
		//Read the features
		readDepFeats(pargFile);

	}

	@Override
	public int[][] getFeatures() {
		//Generate the features
		//For the case of PoS tags as features
		//Number of features = #unsupervised tags +1 for ROOT
		int[][] features = new int[corpus.getNumTypes()][(corpus.getNumClusters())+1];
		StringCoder headFreqMap = new StringCoder();

		for (int wordType : headDepMap.keySet()){
			Map<Integer, Integer> heads = headDepMap.get(wordType);
			for (int headType : heads.keySet()){
				features[wordType][headFreqMap.encode(Integer.toString(headType))] += heads.get(headType);
			}
		}
		return features;
	}

	/**
	 * Reads dependency data from a PARG file and uses the corpus to get the words and clusters
	 * @param pargFile The coder from word strings to integers
	 * @throws java.io.IOException
	 */
	public void readDepFeats(String pargFile) throws IOException {
		headDepMap = new HashMap<>();
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
				int headTag = corpusTags[sentInd][headIndex];
				addDep(wordType, headTag);
				//Now for the reverse dependency (comment out for gold deps)
				wordType = corpusSents[sentInd][headIndex];
				headTag = corpusTags[sentInd][wordIndex];
				addDep(wordType, headTag);
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
