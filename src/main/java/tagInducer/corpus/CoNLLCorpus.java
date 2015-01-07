package tagInducer.corpus;

import tagInducer.Options;
import tagInducer.features.DepFeatures;
import tagInducer.features.PargDepFeatures;
import utils.CollectionUtils;
import utils.FileUtils;
import utils.StringCoder;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * A corpus that uses the CoNLL (column) format.
 */
public class CoNLLCorpus extends Corpus {

    private static final int wordIndex = 1;
    private static final int lemmaIndex = 2;
    private static final int finePosIndex = 3;
    // This was originally coarse-grained tags, now used to load the cluster IDs from a previous BMMM run
    private static final int clusterIndex = 4;
    // XXX Assume that we have a corpus with UPOS
    private static final int uPosIndex = 5;
    private static final int featIndex = 6;
    private static final int depIndex = 7;
    private static final int depTypeIndex = 8;

    /** One dimensional array of all word dependencies */
    private int[][] corpusDeps;

    private int[][] corpusUPos;

    private int[][] corpusClusters;

    private StringCoder uPosCoder, clustersCoder;

    /**
     * Extracts a corpus from input
     *
     * @param o The configuration object
     */
    public CoNLLCorpus(Options o) {
        super(o);
        // We assume this file format has gold tags
        HAS_TAGS = true;
    }

    @Override
    protected void readCorpus() {
        uPosCoder = new StringCoder();
        clustersCoder = new StringCoder();
        String file = o.getCorpusFileName();
        String line;
        int totalWords = 0;

        List<List<Integer>> corpusSentsList = new ArrayList<>();
        List<List<Integer>> corpusTagsList = new ArrayList<>();
        List<List<Integer>> corpusDepsList = new ArrayList<>();
        List<List<Integer>> corpusUPosList = new ArrayList<>();
        List<List<Integer>> corpusClustersList = new ArrayList<>();

        try {
            BufferedReader in = FileUtils.createIn(file);
            //A list to store the sentence words
            List<Integer> sentWords = new ArrayList<>();
            //A list to store the sentence head dependencies
            List<Integer> sentDeps = new ArrayList<>();
            //A list to store the sentence fine-grained PoS tags
            List<Integer> sentTags = new ArrayList<>();
            List<Integer> sentUPosTags = new ArrayList<>();
            List<Integer> sentClusters = new ArrayList<>();
            while ((line = in.readLine())!=null) {
                //Read through each sentence
                if (!line.isEmpty()) {
                    String[] splits = line.split("\\s+");
                    //XXX Assume CoNLL style file (check if we need the '/' correction)
                    sentWords.add(wordsCoder.encode(splits[wordIndex]));
                    totalWords++;
                    sentTags.add(tagsCoder.encode(splits[finePosIndex]));
                    String depStr = splits[depIndex];
                    if (depStr.equals("_")) sentDeps.add(-1);
                    else sentDeps.add(Integer.parseInt(depStr));
                    sentUPosTags.add(uPosCoder.encode(splits[uPosIndex]));
                    sentClusters.add(clustersCoder.encode((splits[clusterIndex])));
                    continue;
                }
                //At this point I have the list with each word and its head
                corpusSentsList.add(new ArrayList<>(sentWords));
                corpusTagsList.add(new ArrayList<>(sentTags));
                corpusDepsList.add(new ArrayList<>(sentDeps));
                corpusUPosList.add(new ArrayList<>(sentUPosTags));
                corpusClustersList.add(new ArrayList<>(sentClusters));
                sentTags.clear();
                sentWords.clear();
                sentDeps.clear();
                sentUPosTags.clear();
                sentClusters.clear();
            }
        }
        catch (IOException e) {
            System.err.println("Error reading the corpus file.\n" + e.getMessage());
            System.exit(-1);
        }

        // These are going to be populated by Corpus (line 67)
        words = new int[totalWords];
        goldTags = new int[totalWords];

        corpusDeps = CollectionUtils.toArray2D(corpusDepsList);
        corpusUPos = CollectionUtils.toArray2D(corpusUPosList);
        corpusClusters = CollectionUtils.toArray2D(corpusClustersList);
        corpusSents = CollectionUtils.toArray2D(corpusSentsList);
        corpusGoldTags = CollectionUtils.toArray2D(corpusTagsList);
    }

    @Override
    public void addDepFeats() throws IOException {
        new DepFeatures(featureVectors, this);
    }

    @Override
    public void addPargDepFeats() throws IOException {
        new PargDepFeatures(this, o.getPargFile(), featureVectors);
    }

    @Override
    public void writeTagged(int[] classes, String outFile) throws IOException {
        // Original output format:
        //   0      1      2      3          4         5      6      7       8
        // index  word  lemma  fineTag  [coarseTag]  uPOS  [feat]  [dep]  [depType]
        // BMMM output format:
        // index  word    _    fineTag   clusterID   uPOS    _      dep      _
        // XXX Maybe we should reconstruct the original file
        int totalWordInd = 0;
        BufferedWriter out = FileUtils.createOut(outFile);
        for (int sentInd = 0; sentInd < corpusSents.length; sentInd++) {
            String outLine = "";
            for (int wordInd = 0; wordInd < corpusSents[sentInd].length; wordInd++) {
                String wordStr = wordsCoder.decode(corpusSents[sentInd][wordInd]);
                int clusterId;
                if (o.isIgnorePunct() && wordStr.matches("\\p{Punct}"))
                    clusterId = -1;
                else clusterId = classes[totalWordInd];
                outLine += (wordInd + 1) + "\t";
                outLine += wordStr + "\t";
                outLine += "_\t";
                outLine += tagsCoder.decode(corpusGoldTags[sentInd][wordInd]) + "\t";
                outLine += clusterId + "\t";
                outLine += uPosCoder.decode(corpusUPos[sentInd][wordInd]) + "\t";
                outLine += "_\t";
                outLine += corpusDeps[sentInd][wordInd]+"\t";
                outLine += "_\t";
                outLine += "\n";
                totalWordInd++;
            }
            out.write(outLine.trim() + "\n");
        }
        out.close();
    }

    public int[][] getCorpusDeps() {
        return corpusDeps;
    }
    public int[][] getCorpusClusters() {
        return corpusClusters;
    }
    public int getNumClusters() {
        return clustersCoder.size();
    }
}
