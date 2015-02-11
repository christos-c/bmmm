package tagInducer.features;

import tagInducer.corpus.CCGJSONCorpus;
import tagInducer.corpus.Corpus;
import tagInducer.corpus.json.PARGDep;
import tagInducer.corpus.json.SentenceObj;
import tagInducer.utils.StringCoder;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A set of features derived from the predicate-argument output (.parg files) of the CCG parser.
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
            Map<String, List<String>> headChildrenMap = new HashMap<>();
            for (PARGDep dep : sentence.synPars[0].depParse) {
                String headCatFeat = dep.category;
                String headStr = sentence.words[dep.head].word;
                String depCatFeat = sentence.words[dep.dependent].cat;
                String depStr = sentence.words[dep.dependent].word;

                // For each head (word), collect all its children
                List<String> children;
                if (headChildrenMap.containsKey(headStr))
                    children = headChildrenMap.get(headStr);
                else children = new ArrayList<>();
                children.add(depStr);
                headChildrenMap.put(headStr, children);

                addFeatureToWord(depStr, headCatFeat, headCatFeatCounts, headCatCoder);
                addFeatureToWord(depStr, depCatFeat, catFeatCounts, catCoder);
            }
            // Go through the list of siblings (children of the same parent)
            // and create (syntactic) context features from those that are frequent words
            for (List<String> siblings : headChildrenMap.values()) {
                for (String word : siblings) {
                    for (String sibling : siblings) {
                        if (word.equals(sibling)) continue;
                        // Similarly to the distributional context features we need to add an extra NULL feature
                        if (freqWordList.contains(corpus.getWordType(sibling))) {
                            addFeatureToWord(word, sibling, contextFeatCounts, contextCoder);
                        }
                        else {
                            addFeatureToWord(word, "NULL", contextFeatCounts, contextCoder);
                        }
                    }
                }
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
