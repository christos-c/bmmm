package tagInducer.corpus;

import tagInducer.Options;
import tagInducer.features.AlignFeatures;
import tagInducer.features.ContextFeatures;
import tagInducer.features.MorfFeatures;
import tagInducer.features.PargFeatures;
import utils.MapUtils;
import utils.StringCoder;
import utils.StringUtils;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public abstract class Corpus {

	/** The full corpus (arrays of sentences) */
	protected int[][] corpus;

	/** The full corpus tags (arrays of sentences) */
	protected int[][] corpusGoldTags;

	/** Map from integers to word types and reverse*/
	protected StringCoder wordsCoder = new StringCoder();
	
	/** Map from integers to gold-standard tags and reverse  */
	protected StringCoder tagsCoder = new StringCoder();

	/** 1D array of the corpus (bag-of-words) */
	protected int[] words;

	/** 1D array of the tags of the corpus (for evaluation) */
	protected int[] goldTags;

	/** A map of feature vectors per word type.<br>
	 * <code>String</code> will contain the feature type (left, right, ...)
	 * and <code>int[][]</code> is a MxF array of features per type.<br> 
	 */
	protected Map<String, int[][]> featureVectors = new HashMap<>();

	/** Number of feature (most frequent) words */
	private int numContextFeatWords;

	/** Whether the corpus has gold-standard tag annotation */
	protected boolean HAS_TAGS = true;

	private Map<Integer, Integer> frequentWordMap = new HashMap<>();

	protected StringUtils s = new StringUtils();
	
	protected Options o;

	/**
	 * Extracts a corpus from input
	 * @param o The configuration object
	 */
	public Corpus(Options o){
		this.o = o;
		numContextFeatWords = o.getNumContextFeats();

		//Read the corpus (type-specific)
		readCorpus();

		//Populate words
		int wordInd = 0;
		for (int sentence = 0; sentence < corpus.length; sentence++){
			for (int word = 0; word < corpus[sentence].length; word++){
				words[wordInd] = corpus[sentence][word];
				if (HAS_TAGS) goldTags[wordInd] = corpusGoldTags[sentence][word];
				wordInd++;
			}
		}

		//Create a list of feature words (N most frequent words)
		Map<Integer, Integer> wordFreq = new HashMap<>();
		//Get the word counts
		for (int word : words){
			if (wordFreq.containsKey(word)) wordFreq.put(word, wordFreq.get(word) + 1);
			else wordFreq.put(word, 1);
		}
		//Resort wrt frequency and prune
		// Check in case we have less than numContextFeatWords in the corpus
		numContextFeatWords = Math.min(numContextFeatWords, wordFreq.size());
		List<Integer> frequentWordList = MapUtils.sortByValueList(wordFreq).subList(0, numContextFeatWords);
		
		wordInd = 0;
		for (int word: frequentWordList){
			frequentWordMap.put(word, wordInd);
			wordInd++;
		}
	}
	
	public void addAllContextFeats() throws IOException {
		new ContextFeatures(wordsCoder, numContextFeatWords, featureVectors, corpus, frequentWordMap);
	}

	// This depends on the corpus type
	public abstract void addDepFeats() throws IOException;
	
	public void addMorphFeats() throws IOException {
		new MorfFeatures(wordsCoder, featureVectors, words, o);
	}
	
	public void addAlignsFeats() throws IOException {
		new AlignFeatures(wordsCoder.size(), corpus, featureVectors, words, o);
	}

	public void addPargFeats() throws IOException {
		new PargFeatures(wordsCoder, featureVectors, o.getPargFile());
	}

	protected abstract void readCorpus();

	/**
	 * Outputs the tagged corpus
	 * @param classes The list of induced tagged
	 * @param outFile The name of the output file
	 */
	public abstract void writeTagged(int[] classes, String outFile) throws IOException;
	
	/**
	 * Returns the gold-standard tags (if they exist)
	 */
	public int[] getGoldTags(){
		int wordInd = 0;
		for (int[] goldTagSent : corpusGoldTags) {
			for (int goldTagWord : goldTagSent) {
				goldTags[wordInd] = goldTagWord;
				wordInd++;
			}
		}
		return goldTags;
	}

	/** Returns the bag-of-words */
	public int[] getWords(){return words;}
	public int getNumTypes(){return wordsCoder.size();}
	/** Returns the features of the words in bag-of-words form */
	public Map<String, int[][]> getFeatures(){return featureVectors;}
	public String int2Word(int index){return wordsCoder.decode(index);}
	public boolean hasTags() {return HAS_TAGS;}
}
