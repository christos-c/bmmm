package tagInducer.corpus;

import tagInducer.Options;
import tagInducer.corpus.json.SentenceObj;
import tagInducer.utils.CollectionUtils;
import tagInducer.utils.FileUtils;
import tagInducer.utils.StringCoder;
import tagInducer.utils.StringUtils;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Corpus {
	protected int numTokens, numTypes, numClusters;

	/** The full corpus (arrays of sentences) */
	protected String[][] corpusOriginalSents;

	/** The full corpus but with pre-processed sentences (passed through a StringCoder) **/
	protected int[][] corpusProcessedSents;

	/** The full corpus tags (arrays of sentences) */
	protected String[][] corpusGoldTags;

	/** The full corpus dependencies */
	protected int[][] corpusDeps;

	protected String[][] corpusUPos;

	protected String[][] corpusCCGCats;

	/**
	 * Can be either the clusters from a previous run (used during dep. feature extraction)
	 * or the by-sentence array of induced clusters from this run (used when writing the corpus)
	 */
	protected int[][] corpusClusters;

	/** Map from integers to word types and reverse*/
	private StringCoder wordTypeCoder = new StringCoder();

	/** Whether the corpus has gold-standard tag annotation */
	protected boolean HAS_TAGS;

	protected Options o;

	private static final int wordIndex = 1;
	private static final int lemmaIndex = 2;
	private static final int finePosIndex = 3;
	// This was originally coarse-grained tags, now used to load the cluster IDs from a previous BMMM run
	private static final int clusterIndex = 4;
	// XXX Assume that we have a corpus with UPOS
	private static final int uPosIndex = 5;
	// This is overloaded to included CCG categories
	private static final int featIndex = 6;
	private static final int depIndex = 7;
	private static final int depTypeIndex = 8;

    private final List<Integer> frequentWordList;

    /**
	 * Extracts a corpus from input
	 * @param o The configuration object
	 */
	public Corpus(Options o){
		this.o = o;

		//Read the corpus
		readCorpus();

		// Create the word types to be used for clustering
		corpusProcessedSents = new int[corpusOriginalSents.length][];
		for (int sentInd = 0; sentInd < corpusOriginalSents.length; sentInd++) {
			corpusProcessedSents[sentInd] = new int[corpusOriginalSents[sentInd].length];
			for (int wordInd = 0; wordInd < corpusOriginalSents[sentInd].length; wordInd++) {
				numTokens++;
				String word = corpusOriginalSents[sentInd][wordInd];
				int wordTypeIndex = wordTypeCoder.encode(preProcessWord(word));
				corpusProcessedSents[sentInd][wordInd] = wordTypeIndex;
			}
		}
		numTypes = wordTypeCoder.size();
		numClusters = CollectionUtils.countUnique(corpusClusters);

        //Create a list of feature words (N most frequent original words)
        Map<Integer, Integer> wordFreq = new HashMap<>();
        //Get the word counts
        for (int[] sent : getCorpusProcessedSents()) {
            for (int word : sent) {
                if (wordFreq.containsKey(word)) wordFreq.put(word, wordFreq.get(word) + 1);
                else wordFreq.put(word, 1);
            }
        }
        //Resort wrt frequency and prune
        // Check in case we have less than numContextFeatWords in the corpus
        int numContextWords = Math.min(o.getNumContextFeats(), wordFreq.size());
        frequentWordList = CollectionUtils.sortByValueList(wordFreq).subList(0, numContextWords);
	}

	protected void readCorpus() {
		List<List<String>> corpusSentsList = new ArrayList<>();
		List<List<String>> corpusTagsList = new ArrayList<>();
		List<List<Integer>> corpusDepsList = new ArrayList<>();
		List<List<String>> corpusUPosList = new ArrayList<>();
		List<List<String>> corpusCCGCatList = new ArrayList<>();
		List<List<Integer>> corpusClustersList = new ArrayList<>();

		try {
			BufferedReader in = FileUtils.createIn(o.getCorpusFileName());
			//A list to store the sentence words
			List<String> sentWords = new ArrayList<>();
			//A list to store the sentence head dependencies
			List<Integer> sentDeps = new ArrayList<>();
			//A list to store the sentence fine-grained PoS tags
			List<String> sentTags = new ArrayList<>();
			List<String> sentUPosTags = new ArrayList<>();
			// A list to store the CCG cats (hijacked from morph-feats column)
			List<String> sentCCGCats = new ArrayList<>();
			// A list to store the sentence clusters from a previous run (or coarse tags if 1st run)
			List<Integer> sentClusters = new ArrayList<>();
			String line;
			while ((line = in.readLine()) != null) {
				//Read through each sentence
				if (!line.isEmpty()) {
					String[] splits = line.split("\\s+");
					sentWords.add(splits[wordIndex]);
					sentTags.add(splits[finePosIndex]);
					String depStr = splits[depIndex];
					if (depStr.equals("_")) sentDeps.add(-1);
					else sentDeps.add(Integer.parseInt(depStr));
					sentUPosTags.add(splits[uPosIndex]);
					sentCCGCats.add(splits[featIndex]);
					String clusterStr = splits[clusterIndex];
					if (!clusterStr.matches("[0-9]+")) sentClusters.add(-1);
					else sentClusters.add(Integer.parseInt(splits[clusterIndex]));
					continue;
				}
				//At this point I have the list with each word and its head
				corpusSentsList.add(new ArrayList<>(sentWords));
				corpusTagsList.add(new ArrayList<>(sentTags));
				corpusDepsList.add(new ArrayList<>(sentDeps));
				corpusUPosList.add(new ArrayList<>(sentUPosTags));
				corpusCCGCatList.add(new ArrayList<>(sentCCGCats));
				corpusClustersList.add(new ArrayList<>(sentClusters));
				sentTags.clear();
				sentWords.clear();
				sentDeps.clear();
				sentUPosTags.clear();
				sentCCGCats.clear();
				sentClusters.clear();
			}
		}
		catch (IOException e) {
			System.err.println("Error reading the corpus file.\n" + e.getMessage());
			System.exit(-1);
		}

		HAS_TAGS = !corpusTagsList.get(0).get(0).equals("_");
		corpusDeps = CollectionUtils.toArray2D(corpusDepsList);
		corpusUPos = CollectionUtils.toStringArray2D(corpusUPosList);
		corpusCCGCats = CollectionUtils.toStringArray2D(corpusCCGCatList);
		corpusClusters = CollectionUtils.toArray2D(corpusClustersList);
		corpusOriginalSents = CollectionUtils.toStringArray2D(corpusSentsList);
		corpusGoldTags = CollectionUtils.toStringArray2D(corpusTagsList);
	}

	/**
	 * Process a word to convert it to one of the word types used in the clustering process.
	 * Right now it's just lowercasing and punctuation, but other types of cleaning-up can be added.
	 * @param rawWord The orthographic string of the word token
	 * @return The processed version of the token
	 */
	public String preProcessWord(String rawWord) {
		// If the option is active, put the punctuation marks in a separate cluster
		if (o.isIgnorePunct() && StringUtils.isPunct(rawWord))
			return ".";
		if (o.isLowercase())
			return rawWord.toLowerCase();
		return rawWord;
	}

	/**
	 * Sets the per-sentence and per-token cluster given the induced assignments
	 * @param currentZ The 1D array containing the cluster assignments for each word type
	 */
	public void setCorpusClusters(int[] currentZ) {
		for (int i = 0; i < corpusProcessedSents.length; i++) {
			for (int j = 0; j < corpusProcessedSents[i].length; j++) {
				String wordStr = corpusOriginalSents[i][j];
				if (o.isIgnorePunct() && StringUtils.isPunct(wordStr))
					corpusClusters[i][j] = -1;
				else corpusClusters[i][j] = currentZ[corpusProcessedSents[i][j]];
			}
		}
		numClusters = CollectionUtils.countUnique(corpusClusters);
	}

	public void setCorpusClusters(int[][] sentClusters) {
		corpusClusters = sentClusters;
		numClusters = CollectionUtils.countUnique(corpusClusters);
	}

	/**
	 * Outputs the tagged corpus
	 * @param outFile The name of the output file
	 */
	public void writeTagged(String outFile) throws IOException {
		BufferedWriter out = FileUtils.createOut(outFile);
		for (int sentInd = 0; sentInd < corpusOriginalSents.length; sentInd++) {
			for (int wordInd = 0; wordInd < corpusOriginalSents[sentInd].length; wordInd++) {
				out.write((wordInd+1) + "\t" + corpusOriginalSents[sentInd][wordInd] + "\t_\t" +
						corpusGoldTags[sentInd][wordInd] + "\t" + corpusClusters[sentInd][wordInd] +"\t" +
						corpusUPos[sentInd][wordInd] + "\t_\t" + corpusDeps[sentInd][wordInd] + "\t_\n");
			}
			out.write("\n");
		}
		out.close();
	}

	public int getWordType(String rawWord) {
		String processWord = preProcessWord(rawWord);
		if (wordTypeCoder.exists(processWord))
			return wordTypeCoder.encode(processWord);
		return -1;
	}

	public int getNumTypes(){
		return numTypes;
	}
	public int getNumTokens(){
		return numTokens;
	}
	public boolean hasTags() {
		return HAS_TAGS;
	}
	public String[][] getCorpusOriginalSents() {
		return corpusOriginalSents;
	}
	public int[][] getCorpusProcessedSents() {
		return corpusProcessedSents;
	}
 	public int[][] getCorpusDeps() {
		return corpusDeps;
	}
	public int[][] getCorpusClusters() {
		return corpusClusters;
	}
	public String[][] getCorpusGoldTags() {
		return corpusGoldTags;
	}
	public String[][] getCorpusUPOS() {
		return corpusUPos;
	}
	public String[][] getCorpusCCGCats() { return corpusCCGCats; }
	public int getNumClusters() {
		return numClusters;
	}

	public int getNumSentences() {
		return corpusOriginalSents.length;
	}

	public String getWordString(int i) {
		return wordTypeCoder.decode(i);
	}

    public List<Integer> getFrequentWordList() {
        return frequentWordList;
    }
}
