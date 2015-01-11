package tagInducer.features;

import tagInducer.corpus.Corpus;
import tagInducer.utils.StringCoder;

import java.util.HashMap;
import java.util.Map;

public class DepFeatures implements Features {
	
	/** A map from word types to dependency heads (with frequency) */
	private Map<Integer, Map<Integer, Integer>> headDepMap;

	private final Corpus corpus;

	/**
	 * Adds (gold-standard) dependencies as features.
	 * For each word token use the dependency head as feature and sum over types.
	 * All the dependency features are over (gold-standard) head PoS tags.
	 */
	public DepFeatures(Corpus corpus) {
		this.corpus = corpus;
		//Read the features
		readDepFeats();
	}

	@Override
	public int[][] getFeatures() {
		//Generate the features
		//For the case of PoS tags as features
		//Number of features = #unsupervised clusters +1 for ROOT
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
	 * Reads dependency data directly from a CoNLL-style corpus
	 */
	public void readDepFeats() {
		headDepMap = new HashMap<>();
		int[][] corpusSents = corpus.getCorpusProcessedSents();
		int[][] corpusDeps = corpus.getCorpusDeps();
		int[][] corpusClusters = corpus.getCorpusClusters();
		for (int sentIndex = 0; sentIndex < corpus.getNumTypes(); sentIndex++) {
			for (int wordIndex = 0; wordIndex < corpus.getNumTypes(); wordIndex++) {
				int headIndex = corpusDeps[sentIndex][wordIndex];
				if (headIndex == 0) {
					addDep(corpusSents[sentIndex][wordIndex], -1);
				} else {
					int head = corpusClusters[sentIndex][headIndex - 1];
					addDep(corpusSents[sentIndex][wordIndex], head);
					//Now for the reverse dependency (comment out for gold deps)
					head = corpusClusters[sentIndex][wordIndex];
					addDep(corpusSents[sentIndex][headIndex - 1], head);
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
