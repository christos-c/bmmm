package tagInducer.corpus;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import tagInducer.Options;
import utils.FileUtils;
import utils.MapUtils;
import utils.StringCoder;
import utils.StringUtils;

public class Corpus {

	/** The full corpus (arrays of sentences) */
	private int[][] corpus;

	/** The full corpus tags (arrays of sentences) */
	private int[][] corpusGoldTags;

	/** Map from integers to word types and reverse*/
	private StringCoder wordsCoder = new StringCoder();
	
	/** Map from integers to gold-standard tags and reverse  */
	private StringCoder tagsCoder = new StringCoder();

	/** 1D array of the corpus (bag-of-words) */
	private int[] words;

	/** 1D array of the tags of the corpus (for evaluation) */
	private int[] goldTags;

	/** A map of feature vectors per word type.<br>
	 * <code>String</code> will contain the feature type (left, right, ...)
	 * and <code>int[][]</code> is a MxF array of features per type.<br> 
	 */
	private Map<String, int[][]> featureVectors = new HashMap<String, int[][]>();

	private FileUtils f = new FileUtils();

	/** Number of feature (most frequent) words */
	private int NUM_CONTEXT_FEAT_WORDS = 100;

	/** Map for word frequencies (wordType -> count) */
	private Map<Integer, Integer> wordFreq;

	/** Whether the corpus has gold-standard tag annotation */
	private boolean HAS_TAGS = true;

	private Map<Integer, Integer> frequentWordMap = new HashMap<Integer, Integer>();
	
	private List<Integer> frequentWordList;

	private StringUtils s = new StringUtils();
	
	private Options o;

	/**
	 * Extracts a corpus from input
	 * @param o The configuration object
	 */
	public Corpus(Options o){
		this.o = o;
		int numContextFeats = o.getNumContextFeats();
		if (numContextFeats != 0) NUM_CONTEXT_FEAT_WORDS = numContextFeats;

		//Read the corpus
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
		wordFreq = new HashMap<Integer, Integer>();
		//Get the word counts
		for (int word:words){
			if (wordFreq.containsKey(word)) wordFreq.put(word, wordFreq.get(word)+1);
			else wordFreq.put(word, 1);
		}
		//Resort wrt frequency and prune
		frequentWordList = MapUtils.sortByValueList(wordFreq).subList(0, NUM_CONTEXT_FEAT_WORDS);
		
		wordInd = 0;
		for (int word:frequentWordList){
			frequentWordMap.put(word, wordInd);
			wordInd++;
		}
	}
	
	public void addAllContextFeats() throws IOException {
		new ContextFeatures(wordsCoder, NUM_CONTEXT_FEAT_WORDS, 
				featureVectors, corpus, frequentWordMap);
	}
	
	public void addDepFeats() throws IOException {
		new DepFeatures(wordsCoder, featureVectors, o);
	}
	
	public void addMorphFeats() throws IOException {
		new MorfFeatures(wordsCoder, featureVectors, words, o);
	}
	
	public void addAlignsFeats() throws IOException {
		new AlignFeatures(wordsCoder.size(), corpus, featureVectors, words, o);
	}

	private void readCorpus(){
		String file = o.getCorpusFileName();
		String line;
		int sentenceInd = 0, totalWords = 0;

		//Get the corpus statistics (and also check for tags)
		try {
			BufferedReader in = f.createIn(file);
			while ((line = in.readLine())!=null){
				sentenceInd++;
				totalWords+=line.split("\\s+").length;
				HAS_TAGS = s.checkForTags(line) && HAS_TAGS;
			}
			in.close();
		} 
		catch (IOException e) {
			System.err.println("Error reading the corpus file.");
			System.exit(-1);
		}

		corpus = new int[sentenceInd][];
		words = new int[totalWords];
		if (HAS_TAGS) {
			corpusGoldTags = new int[sentenceInd][];
			goldTags = new int[totalWords];
		}
		sentenceInd = 0;

		//Read the corpus
		try {
			BufferedReader in = f.createIn(file);
			while ((line = in.readLine())!=null){
				//Map word types to integers
				int[] lineWords = new int[line.split("\\s+").length];
				//Map tags to integers
				int[] lineTags = new int[line.split("\\s+").length];
				int wordInd = 0;
				for (String word:line.split("\\s+")){
					String tag;
					//If tags exist, first extract them...
					if (HAS_TAGS) {
						tag = s.extractTag(word);
						word = s.extractWord(word);

						//... and then get the tag index
						lineTags[wordInd] = tagsCoder.encode(tag);
					}

					//Get the word index
					lineWords[wordInd] = wordsCoder.encode(word);
					wordInd++;
					totalWords++;
				}
				corpus[sentenceInd] = lineWords;
				if (HAS_TAGS) corpusGoldTags[sentenceInd] = lineTags;
				sentenceInd++;
			}
			in.close();
		}
		catch (IOException e) {
			System.err.println("Error reading the corpus file.");
			System.exit(-1);
		}
	}

	/**
	 * Outputs the tagged corpus
	 * @param classes The list of induced tagged
	 * @param outFile The name of the output file
	 */
	public void writeTagged(int[] classes, String outFile) throws IOException{
		String outLine = "";
		int wordInd = 0;
		BufferedWriter out = f.createOut(outFile);
		for (int sentence = 0; sentence < corpus.length; sentence++){
			for (int word = 0; word < corpus[sentence].length; word++){
				outLine += int2Word(words[wordInd]) + "/" +
				classes[wordInd] + " ";
				wordInd++;
			}
			out.write(outLine.trim() + "\n");
			outLine = "";
		}
		out.close();
	}
	
	/**
	 * Returns the gold-standard tags (if they exist)
	 */
	public int[] getGoldTags(){
		int wordInd = 0;
		for (int sentence = 0; sentence < corpusGoldTags.length; sentence++){
			for (int word = 0; word < corpusGoldTags[sentence].length; word++){
				goldTags[wordInd] = corpusGoldTags[sentence][word];
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
	public int word2Int(String word){return wordsCoder.encode(word);}
	public int getNUM_CONTEXT_FEAT_WORDS() {return NUM_CONTEXT_FEAT_WORDS;}
	public boolean hasTags() {return HAS_TAGS;}
}
