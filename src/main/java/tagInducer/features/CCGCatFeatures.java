package tagInducer.features;

import tagInducer.corpus.Corpus;
import tagInducer.utils.StringCoder;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * A set of features derived from the CCG categories for each word stored in the corpus file (6th column)
 */
public class CCGCatFeatures implements Features {

    private final Map<Integer, Map<Integer, Integer>> ccgCatFeatCounts;

    private final Corpus corpus;

    public CCGCatFeatures(Corpus corpus) throws IOException {
        this.corpus = corpus;
        // Read the features
        // A map from parg feature type to word-type to count
        ccgCatFeatCounts = readPargFeats();
    }

    public int[][] getFeatures() {
        // TODO Add the NULL feature at the end
        int[][] features = new int[corpus.getNumTypes()][ccgCatFeatCounts.size()+1];

        for (int pargFeat : ccgCatFeatCounts.keySet()){
            Map<Integer, Integer> wordFeatCounts = ccgCatFeatCounts.get(pargFeat);
            for (int word : wordFeatCounts.keySet()){
                features[word][pargFeat] += wordFeatCounts.get(word);
            }
        }
        return features;
    }

    private Map<Integer, Map<Integer, Integer>> readPargFeats() throws IOException {
        Map<Integer, Map<Integer, Integer>> featMap = new HashMap<>();
        StringCoder ccgCatFeatCoder = new StringCoder();
        // The CCG categories for each sentence/word
        String[][] ccgCats = corpus.getCorpusCCGCats();
        int[][] corpusSents = corpus.getCorpusProcessedSents();
        for (int sentInd = 0; sentInd < ccgCats.length; sentInd++) {
            for (int wordInd = 0; wordInd < ccgCats[sentInd].length; wordInd++) {
                int wordType = corpusSents[sentInd][wordInd];
                int feat = ccgCatFeatCoder.encode(ccgCats[sentInd][wordInd]);
                Map<Integer, Integer> featCount;
                if (featMap.containsKey(feat)) {
                    featCount = featMap.get(feat);
                    if (featCount.containsKey(wordType))
                        featCount.put(wordType, featCount.get(wordType) + 1);
                    else
                        featCount.put(wordType, 1);
                }
                else {
                    featCount = new HashMap<>();
                    featCount.put(wordType, 1);
                }
                featMap.put(feat, featCount);
            }
        }
        return featMap;
    }
}
