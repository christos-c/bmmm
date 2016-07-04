package tagInducer.corpus;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import tagInducer.Options;
import tagInducer.corpus.json.SentenceObj;
import tagInducer.utils.CollectionUtils;
import tagInducer.utils.FileUtils;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Class for reading JSON files as defined by sivareddy.
 * Stores a series of words and syntactic parses
 * Expanded/modified for use with {@link Corpus}
 *
 * @author ybisk
 * @author christod
 */
public class CCGJSONCorpus extends Corpus {
    public static final GsonBuilder gsonBuilder = new GsonBuilder();
    private static final Gson gsonReader = new Gson();

    private static List<SentenceObj> sentences;

    /**
     * Extracts a corpus from input
     *
     * @param o The configuration object
     */
    public CCGJSONCorpus(Options o) {
        super(o);
        //TODO figure out where to read this from
        HAS_TAGS = true;
    }

    @Override
    public void readCorpus() {
        List<List<String>> corpusSentsList = new ArrayList<>();
        List<List<String>> corpusTagsList = new ArrayList<>();
        List<List<Integer>> corpusDepsList = new ArrayList<>();
        List<List<String>> corpusUPosList = new ArrayList<>();
        List<List<String>> corpusCCGCatList = new ArrayList<>();
        List<List<Integer>> corpusClustersList = new ArrayList<>();
        try {
            sentences = readJSON(o.getCorpusFileName());
            for (SentenceObj sent : sentences) {
                if (sent.lengthNoPunctuation() <= o.getMaxLength()) {
                    corpusSentsList.add(new ArrayList<>(sent.getWords()));
                    corpusTagsList.add(new ArrayList<>(sent.getGoldTags()));
                    corpusUPosList.add(new ArrayList<>(sent.getUPOSs()));
                    corpusCCGCatList.add(new ArrayList<>(sent.getCCGCats()));
                    corpusDepsList.add(new ArrayList<>(sent.getCoNLLHeads()));
                    corpusClustersList.add(new ArrayList<>(sent.getClusters()));
                }
            }
            corpusOriginalSents = CollectionUtils.toStringArray2D(corpusSentsList);
            corpusGoldTags = CollectionUtils.toStringArray2D(corpusTagsList);
            corpusUPos = CollectionUtils.toStringArray2D(corpusUPosList);
            corpusCCGCats = CollectionUtils.toStringArray2D(corpusCCGCatList);
            corpusDeps = CollectionUtils.toArray2D(corpusDepsList);
            corpusClusters = CollectionUtils.toArray2D(corpusClustersList);
        } catch (IOException e) {
            System.err.println("Error reading the corpus file.\n" + e.getMessage());
            System.exit(-1);
        }
    }

    public List<SentenceObj> getSentences() {
        return sentences;
    }

    /**
     * Outputs the tagged corpus
     * @param outFile The name of the output file
     */
    public void writeTagged(String outFile) throws IOException {
        BufferedWriter out = FileUtils.createOut(outFile);
        for (int sentInd = 0; sentInd < corpusOriginalSents.length; sentInd++) {
            SentenceObj sentence = new SentenceObj();
            for (int wordInd = 0; wordInd < corpusOriginalSents[sentInd].length; wordInd++) {
                sentence.addWord(
                        corpusOriginalSents[sentInd][wordInd],    // Word
                        "",                                       // Lemma
                        corpusGoldTags[sentInd][wordInd],         // Gold POS Tag
                        corpusUPos[sentInd][wordInd],             // Gold UPOS Tag
                        corpusClusters[sentInd][wordInd] + "",    // Induced Cluster
                        corpusCCGCats[sentInd][wordInd]);         // CCG Category
            }
            out.write(sentence + "\n");
        }
        out.close();
    }

    @Override
    public String toString() {
        return gsonBuilder.disableHtmlEscaping().create().toJson(this);
    }

    /**
     * Read in a JSON file.  One JSON per line
     *
     * @param filename Source file
     * @return ArrayList of JSON objects
     * @throws java.io.IOException
     */
    public static List<SentenceObj> readJSON(String filename) throws IOException {
        List<SentenceObj> parses = new ArrayList<>();
        BufferedReader reader = FileUtils.createIn(filename);
        String line;
        while ((line = reader.readLine()) != null) {
            parses.add(gsonReader.fromJson(line, SentenceObj.class));
        }
        return parses;
    }
}
