package tagInducer.features;

import utils.FileUtils;
import utils.StringCoder;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * A set of features derived from the predicate-argument output (.parg files) of the CCG parser.
 */
public class PargFeatures {

    public PargFeatures(StringCoder wordsCoder, Map<String, int[][]> featureVectors, String pargFile)
            throws IOException {
        // Read the features
        // A map from parg feature type to word-type to count
        Map<Integer, Map<Integer, Integer>> pargFeatCounts = readPargFeats(wordsCoder, pargFile);

        int numWordTypes = wordsCoder.size();

        // TODO Add the NULL feature at the end
        int[][] features = new int[numWordTypes][pargFeatCounts.size()+1];

        for (int pargFeat : pargFeatCounts.keySet()){
            Map<Integer, Integer> wordFeatCounts = pargFeatCounts.get(pargFeat);
            for (int word : wordFeatCounts.keySet()){
                features[word][pargFeat] += wordFeatCounts.get(word);
            }
        }
        featureVectors.put("PARG", features);
    }

    private Map<Integer, Map<Integer, Integer>> readPargFeats(StringCoder wordsCoder, String pargFile)
            throws IOException {
        // The index of the dependent word
        int wordInd = 4;
        int featCatInd = 2;
        int featSlotInd = 3;

        Map<Integer, Map<Integer, Integer>> featMap = new HashMap<Integer, Map<Integer, Integer>>();
        StringCoder pargFeatCoder = new StringCoder();
        String line;
        BufferedReader in = FileUtils.createIn(pargFile);
        while ((line = in.readLine())!=null) {
            //Read through each sentence
            if (line.startsWith("<"))  continue;
            String[] splits = line.split("\t");
            String wordStr = splits[wordInd];
            // Do not add features for words that don't exist
            if (!wordsCoder.exists(wordStr)) continue;
            int word = wordsCoder.encode(wordStr);
            // The feature is the category_slot string
            String featStr = splits[featCatInd] + "_" + splits[featSlotInd];
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
                featCount = new HashMap<Integer, Integer>();
                featCount.put(word, 1);
            }
            featMap.put(feat, featCount);

        }
        return featMap;
    }


}
