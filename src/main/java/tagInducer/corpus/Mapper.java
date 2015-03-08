package tagInducer.corpus;

import tagInducer.OptionsCmdLine;
import tagInducer.utils.CollectionUtils;

import java.util.HashMap;
import java.util.Map;

public class Mapper {

    /**
     * Prints a mapping (a la M-1) between the clusters of this corpus and the clusters of another corpus.
     * This can be parametrized to print a many-to-k mapping.
     * @param otherCorpus The corpus whose clusters we are mapping to
     * @param topK Print top k otherClusters for each cluster
     */
    public void mapClustersTo(Corpus corpus, Corpus otherCorpus, int topK) {
        Map<Integer, Map<Integer, Integer>> tagMap = new HashMap<>();
        for (int sentInd = 0; sentInd < corpus.getCorpusClusters().length; sentInd++) {
            int[] sentClusters = corpus.getCorpusClusters()[sentInd];
            int[] otherSentClusters = otherCorpus.getCorpusClusters()[sentInd];
            for (int wordInd = 0; wordInd < sentClusters.length; wordInd++) {
                int cluster = sentClusters[wordInd];
                int otherCluster = otherSentClusters[wordInd];
                Map<Integer, Integer> map;
                if (tagMap.containsKey(cluster))
                    map = tagMap.get(cluster);
                else map = new HashMap<>();
                if (map.containsKey(otherCluster))
                    map.put(otherCluster, map.get(otherCluster) + 1);
                else map.put(otherCluster, 1);
                tagMap.put(cluster, map);
            }
        }
        // Get a sorted list of the most frequent otherClusters per cluster
        for (int cluster : tagMap.keySet()) {
            Map<Integer, Integer> sortedOtherClusters = CollectionUtils.sortByValueMap(tagMap.get(cluster));
            String clusterSet = CollectionUtils.sumMap(sortedOtherClusters) + "\t";
            int count = 0;
            for (int otherCluster : sortedOtherClusters.keySet()) {
                if (count == topK) break;
                clusterSet += otherCluster + "\t" + sortedOtherClusters.get(otherCluster) + "\t";
                count++;
            }
            System.out.println(cluster + "\t" + clusterSet.trim());
        }
    }

    public static void main(String[] args) {
        if (args.length < 2) {
            String usage = "Usage:\n$>tagInducer.corpus.Mapper <corpus> <other-corpus> [topK]";
            System.err.println(usage);
            System.exit(1);
        }
        else {
            OptionsCmdLine options = new OptionsCmdLine(new String[]{"-in", args[0]});
            Corpus corpus = new CCGJSONCorpus(options);
            options = new OptionsCmdLine(new String[]{"-in", args[1]});
            Corpus otherCorpus = new CCGJSONCorpus(options);
            int topK = (args.length > 2) ? Integer.parseInt(args[2]) : 1;
            new Mapper().mapClustersTo(corpus, otherCorpus, topK);
        }
    }
}
