package tagInducer.features;

import tagInducer.corpus.CCGJSONCorpus;
import tagInducer.corpus.Corpus;
import tagInducer.corpus.json.PARGDep;
import tagInducer.corpus.json.SentenceObj;
import tagInducer.utils.FileUtils;
import tagInducer.utils.CollectionUtils;
import tagInducer.utils.StringCoder;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A set of features derived from the predicate-argument output (.parg files) of the CCG parser.
 */
public class PargFeatures implements Features {

    private final Map<Integer, Map<Integer, Integer>> pargFeatCounts;

    private final Corpus corpus;

    public PargFeatures(Corpus corpus, String pargFile) throws IOException {
        this.corpus = corpus;
        // Read the features
        // A map from parg feature type to word-type to count
        if (corpus instanceof CCGJSONCorpus)
            pargFeatCounts = readPargFeats(((CCGJSONCorpus)corpus).sentences());
        else
            pargFeatCounts = readPargFeats(pargFile);

        // TODO Apply a threshold
        for (int feat : pargFeatCounts.keySet()) {
            int sum = CollectionUtils.sumMap(pargFeatCounts.get(feat));
        }

    }

    public int[][] getFeatures() {
        // TODO Add the NULL feature at the end
        int[][] features = new int[corpus.getNumTypes()][pargFeatCounts.size()+1];

        for (int pargFeat : pargFeatCounts.keySet()){
            Map<Integer, Integer> wordFeatCounts = pargFeatCounts.get(pargFeat);
            for (int word : wordFeatCounts.keySet()){
                features[word][pargFeat] += wordFeatCounts.get(word);
            }
        }
        return features;
    }

    /**
     * Takes in a JSON corpus and computes features of the form  dep: cat_slot_headCluster
     */
    private Map<Integer, Map<Integer,Integer>> readPargFeats(List<SentenceObj> sentences){
        Map<Integer, Map<Integer, Integer>> featMap = new HashMap<>();
        StringCoder pargFeatCoder = new StringCoder();
        for (SentenceObj sentence : sentences) {
            if (sentence.synPars != null && sentence.synPars[0].depParse != null) {
                for (PARGDep dep : sentence.synPars[0].depParse) {
                    String featStr = dep.category + "_" + dep.slot + "_" + sentence.words[dep.head].cluster;
                    String wordStr = sentence.words[dep.dependent].word;
                    addFeatureToWord(wordStr, featStr, featMap, pargFeatCoder);
                }
            }
        }
        return featMap;
    }

    private Map<Integer, Map<Integer, Integer>> readPargFeats(String pargFile) throws IOException {
        // The index of the dependent word
        int wordInd = 4;
        int featCatInd = 2;
        int featSlotInd = 3;

        Map<Integer, Map<Integer, Integer>> featMap = new HashMap<>();
        StringCoder pargFeatCoder = new StringCoder();
        String line;
        BufferedReader in = FileUtils.createIn(pargFile);
        while ((line = in.readLine())!=null) {
            //Read through each sentence
            if (line.startsWith("<"))  continue;
            String[] splits = line.split("\\s+");
            String wordStr = splits[wordInd];
            // The feature is the category_slot string
            String featStr = splits[featCatInd] + "_" + splits[featSlotInd];

            addFeatureToWord(wordStr, featStr, featMap, pargFeatCoder);
        }
        return featMap;
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
