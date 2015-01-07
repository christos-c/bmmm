package tagInducer.features;

import tagInducer.corpus.CoNLLCorpus;
import utils.FileUtils;
import utils.StringCoder;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DepFeatures {

	private String depsFile;
	/** 
	 * Map from integers to unsupervised tags (of previous iter) and reverse.
	 * Used for dependencies.  
	 */
	private StringCoder depsTagsCoder = new StringCoder();
	
	/** A map from word types to dependency heads (with frequency) */
	private Map<Integer, Map<Integer, Integer>> headDepMap;

	/**
	 * Adds (gold-standard) dependencies as features.
	 * For each word token use the dependency head as feature and sum over types.
	 * All the dependency features are over (gold-standard) head PoS tags.
	 */
	public DepFeatures(StringCoder wordsCoder, Map<String, int[][]> featureVectors, String depsFile) throws IOException{
		this.depsFile = depsFile;
		//Read the features
		if (headDepMap == null) readDepFeats(wordsCoder);
		//Generate the features
		generateFeatures(wordsCoder.size(), featureVectors);
	}

	public DepFeatures(Map<String, int[][]> featureVectors, CoNLLCorpus corpus) {
		//Read the features
		if (headDepMap == null) readDepFeats(corpus);
		//Generate the features
		generateFeatures(corpus.getNumTypes(), featureVectors);
	}

	private void generateFeatures(int numWordTypes, Map<String, int[][]> featureVectors) {
		//For the case of PoS tags as features
		//Number of features = #unsupervised tags +1 for ROOT
		int numTags = depsTagsCoder.size();
		int[][] features = new int[numWordTypes][(numTags)+1];
		StringCoder headFreqMap = new StringCoder();

		for (int wordType:headDepMap.keySet()){
			Map<Integer, Integer> heads = headDepMap.get(wordType);
			for (int headType:heads.keySet()){
				features[wordType][headFreqMap.encode(Integer.toString(headType))]+=heads.get(headType);
			}
		}
		featureVectors.put("DEPS", features);
	}

	/**
	 * Reads dependency data directly from a CoNLL-style corpus
	 * @param corpus The object containing the words, tags and dependencies
	 */
	public void readDepFeats(CoNLLCorpus corpus) {
		headDepMap = new HashMap<>();
		int[][] corpusSents = corpus.getCorpusSents();
		int[][] corpusDeps = corpus.getCorpusDeps();
		int[][] corpusTags = corpus.getCorpusClusters();
		for (int sentIndex = 0; sentIndex < corpus.getNumTypes(); sentIndex++) {
			for (int wordIndex = 0; wordIndex < corpus.getNumTypes(); wordIndex++) {
				int headIndex = corpusDeps[sentIndex][wordIndex];
				if (headIndex == 0) {
					addDep(corpusSents[sentIndex][wordIndex], -1);
				} else {
					int head = corpusTags[sentIndex][headIndex - 1];
					addDep(corpusSents[sentIndex][wordIndex], head);
					//Now for the reverse dependency (comment out for gold deps)
					head = corpusTags[sentIndex][wordIndex];
					addDep(corpusSents[sentIndex][headIndex - 1], head);
				}
			}
		}
	}

	/**
	 * Reads dependency data from a CoNLL-style file
	 * @param wordsCoder The coder from word strings to integers
	 * @throws IOException
	 */
	public void readDepFeats(StringCoder wordsCoder) throws IOException {
		headDepMap = new HashMap<>();
		BufferedReader in = FileUtils.createIn(depsFile);
		String line;
		//A list to store the sentence words
		List<Integer> sentWords = new ArrayList<>();
		//A list to store the sentence head dependencies
		List<Integer> sentDeps = new ArrayList<>();
		//A list to store the sentence PoS tags (these should be the clusters from a previous BMMM run)
		List<Integer> sentPOS = new ArrayList<>();
		while ((line = in.readLine())!=null) {
			//Read through each sentence
			if (!line.isEmpty()) {
				String[] splits = line.split("\t");
				//XXX Assume CoNLL style file (check if we need the '/' correction)
				sentWords.add(wordsCoder.encode(splits[1]));
				sentPOS.add(depsTagsCoder.encode(splits[3]));
				//Check either the 7th (no UPOS) or 8th column for dependencies
				if (splits[7].matches("[0-9]+")) sentDeps.add(Integer.parseInt(splits[7]));
				else sentDeps.add(Integer.parseInt(splits[6]));
				continue;
			}
			//At this point I have the list with each word and its head
			//Now I need to get the PoS of the head
			//For Head-Dependency go wordType->Head
			//Modified for undirected dependencies
			//After each sentence: sentWords.size()==sentDeps.size()==sentPos.size()
			for (int i = 0; i < sentWords.size(); i++){
				int wordType = sentWords.get(i);
				int headIndex = sentDeps.get(i);
				int head = (headIndex==0) ? -1 : sentPOS.get(headIndex-1);
				addDep(wordType, head);
				//Now for the reverse dependency (comment out for gold deps)
				if (headIndex != 0){
					wordType = sentWords.get(headIndex-1);
					head = sentPOS.get(i);
					addDep(wordType, head);
				}
			}
			sentPOS.clear();
			sentWords.clear();
			sentDeps.clear();
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
