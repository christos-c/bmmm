package tagInducer.features;

import tagInducer.corpus.CCGJSONCorpus;
import tagInducer.corpus.Corpus;
import tagInducer.corpus.json.PARGDep;
import tagInducer.corpus.json.SentenceObj;
import tagInducer.utils.StringCoder;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A set of features derived from the predicate-argument output (.parg files) of the CCG parser.
 * NB: for historical reasons CCG predicates are called "heads" and arguments are called "dependents"
 */
public class PargFeatures implements Features {

    private Map<Integer, Map<Integer, Integer>> headCatFeatCounts;
    private Map<Integer, Map<Integer, Integer>> catFeatCounts;
    private Map<Integer, Map<Integer, Integer>> contextFeatCounts;

    private final Corpus corpus;

    public PargFeatures(Corpus corpus) throws IOException {
        this.corpus = corpus;
        // Read the features
        // A map from parg feature type to word-type to count
        if (!(corpus instanceof CCGJSONCorpus)){
            System.err.println("Corpus needs to be in JSON format");
            System.exit(-1);
        }
        List<SentenceObj> sentences = ((CCGJSONCorpus)corpus).getSentences();
        List<Integer> freqWordsList = corpus.getFrequentWordList();

        headCatFeatCounts = new HashMap<>();
        catFeatCounts = new HashMap<>();
        contextFeatCounts = new HashMap<>();

        readFeatures(sentences, freqWordsList);
    }

    @Override
    public int[][] getFeatures() {
        return null;
    }

    public int[][] getCatFeatures() {
        return populateFeatureMatrix(catFeatCounts);
    }

    public int[][] getHeadCatFeatures() {
        return populateFeatureMatrix(headCatFeatCounts);
    }

    public int[][] getContextFeatures() {
        return populateFeatureMatrix(contextFeatCounts);
    }

    private int[][] populateFeatureMatrix(Map<Integer, Map<Integer, Integer>> featureMap) {
        int[][] features = new int[corpus.getNumTypes()][featureMap.size()];
        for (int pargFeat : featureMap.keySet()){
            Map<Integer, Integer> wordFeatCounts = featureMap.get(pargFeat);
            for (int word : wordFeatCounts.keySet()){
                features[word][pargFeat] += wordFeatCounts.get(word);
            }
        }
        return features;
    }

    /**
     * Takes in a JSON corpus and computes features of the form  dep: headCat
     */
    private void readFeatures(List<SentenceObj> sentences, List<Integer> freqWordList){
        StringCoder headCatCoder = new StringCoder();
        StringCoder catCoder = new StringCoder();
        StringCoder contextCoder = new StringCoder();
        for (SentenceObj sentence : sentences) {
            if (sentence.synPars == null || sentence.synPars[0].depParse == null) continue;
            for (PARGDep dep : sentence.synPars[0].depParse) {
                String headCatFeat = dep.category;
                String depCatFeat = sentence.words[dep.dependent].cat;

                // Create two separate syntactic context features from each token's predicates, args
                // (~= left, right) w.r.t to their frequency
                String headWordStr = corpus.preProcessWord(sentence.words[dep.head].word);
                String depWordStr = corpus.preProcessWord(sentence.words[dep.dependent].word);
                int headInt = corpus.getWordType(headWordStr);
                int depInt = corpus.getWordType(depWordStr);
                // Add the dependent (argument) as a feature for the predicate
                String featStr = "IsPredOf:";
                if (freqWordList.contains(depInt))
                    featStr += depWordStr;
                else featStr += "NULL";
                addFeatureToWord(headWordStr, featStr, contextFeatCounts, contextCoder);
                // Add the head (predicate) as a feature for the argument
                featStr = "IsArgOf:";
                if (freqWordList.contains(headInt))
                    featStr += headWordStr;
                else featStr += "NULL";
                addFeatureToWord(depWordStr, featStr, contextFeatCounts, contextCoder);

                addFeatureToWord(depWordStr, headCatFeat, headCatFeatCounts, headCatCoder);
                addFeatureToWord(depWordStr, depCatFeat, catFeatCounts, catCoder);
            }
        }
    }

    private void addFeatureToWord(String wordStr, String featStr, Map<Integer,Map<Integer,Integer>> featMap,
                                  StringCoder pargFeatCoder) {
        // Do not add features for words that don't exist
        int word = corpus.getWordType(wordStr);
        if (word == -1) return;

        int feat = pargFeatCoder.encode(featStr);
        Map<Integer, Integer> featCount;
        if (featMap.containsKey(feat)) {
            featCount = featMap.get(feat);
            if (featCount.containsKey(word))
                featCount.put(word, featCount.get(word) + 1);
            else
                featCount.put(word, 1);
        }
        else {
            featCount = new HashMap<>();
            featCount.put(word, 1);
        }
        featMap.put(feat, featCount);
    }
}
