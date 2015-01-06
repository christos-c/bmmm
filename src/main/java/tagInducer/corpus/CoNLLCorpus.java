package tagInducer.corpus;

import tagInducer.Options;
import tagInducer.features.DepFeatures;
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
    private static final int coarsePosIndex = 4;
    // XXX Assume that we have a corpus with UPOS
    private static final int uPosIndex = 5;
    private static final int featIndex = 6;
    private static final int depIndex = 7;
    private static final int depTypeIndex = 8;

    /** One dimensional array of all word dependencies */
    private int[] deps;

    private int[] uPos;
    private StringCoder uPosCoder;

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
        String file = o.getCorpusFileName();
        String line;
        int totalWords = 0;

        List<List<Integer>> corpusSents = new ArrayList<>();
        List<List<Integer>> corpusTags = new ArrayList<>();
        List<List<Integer>> corpusDeps = new ArrayList<>();
        List<List<Integer>> corpusUPos = new ArrayList<>();

        try {
            BufferedReader in = FileUtils.createIn(file);
            //A list to store the sentence words
            List<Integer> sentWords = new ArrayList<>();
            //A list to store the sentence head dependencies
            List<Integer> sentDeps = new ArrayList<>();
            //A list to store the sentence fine-grained PoS tags
            List<Integer> sentTags = new ArrayList<>();
            List<Integer> sentUPosTags = new ArrayList<>();
            while ((line = in.readLine())!=null) {
                //Read through each sentence
                if (!line.isEmpty()) {
                    String[] splits = line.split("\\s+");
                    //XXX Assume CoNLL style file (check if we need the '/' correction)
                    sentWords.add(wordsCoder.encode(splits[wordIndex]));
                    totalWords++;
                    sentTags.add(tagsCoder.encode(splits[finePosIndex]));
                    sentDeps.add(Integer.parseInt(splits[depIndex]));
                    sentUPosTags.add(uPosCoder.encode(splits[uPosIndex]));
                    continue;
                }
                //At this point I have the list with each word and its head
                corpusSents.add(sentWords);
                corpusTags.add(sentTags);
                corpusDeps.add(sentDeps);
                corpusUPos.add(sentUPosTags);
                sentTags.clear();
                sentWords.clear();
                sentDeps.clear();
                sentUPosTags.clear();
            }
        }
        catch (IOException e) {
            System.err.println("Error reading the corpus file.\n" + e.getMessage());
            System.exit(-1);
        }

        int sentNum = corpusSents.size();
        words = new int[totalWords];
        goldTags = new int[totalWords];
        deps = new int[totalWords];
        uPos = new int[totalWords];

        corpus = new int[sentNum][];
        corpusGoldTags = new int[sentNum][];

        int totalWordInd = 0;
        for (int sentInd = 0; sentInd < corpusSents.size(); sentInd++) {
            int sentSize = corpusSents.get(sentInd).size();
            int[] sentWords = new int[sentSize];
            int[] sentTags = new int[sentSize];
            for (int i = 0; i < sentSize; i++) {
                int word = corpusSents.get(sentInd).get(i);
                int tag = corpusTags.get(sentInd).get(i);
                words[totalWordInd] = word;
                goldTags[totalWordInd] = tag;
                deps[totalWordInd] = corpusDeps.get(sentInd).get(i);
                uPos[totalWordInd] = corpusUPos.get(sentInd).get(i);
                sentWords[i] = word;
                sentTags[i] = tag;
                totalWordInd++;
            }
            corpus[sentInd] = sentWords;
            corpusGoldTags[sentInd] = sentTags;
        }
    }

    @Override
    public void addDepFeats() throws IOException {
        new DepFeatures(featureVectors, this);
    }

    @Override
    public void writeTagged(int[] classes, String outFile) throws IOException {
        // Original output format:
        //   0      1      2      3          4         5      6      7       8
        // index  word  lemma  fineTag  [coarseTag]  uPOS  [feat]  [dep]  [depType]
        // BMMM output format:
        // index  word    _    fineTag   clusterID   uPOS    _      dep      _
        // XXX Maybe we should reconstruct the original file
        int wordInd = 0;
        BufferedWriter out = FileUtils.createOut(outFile);
        for (int[] sentence : corpus) {
            String outLine = "";
            for (int i = 0; i < sentence.length; i++) {
                outLine += (i+1) + "\t";
                outLine += wordsCoder.decode(words[wordInd]) + "\t";
                outLine += "_\t";
                outLine += tagsCoder.decode(goldTags[wordInd]) + "\t";
                outLine += classes[wordInd] + "\t";
                outLine += uPosCoder.decode(uPos[wordInd]) + "\t";
                outLine += "_\t";
                outLine += deps[wordInd] + "\t";
                outLine += "_\t";
                outLine += "\n";
                wordInd++;
            }
            out.write(outLine.trim() + "\n");
        }
        out.close();
    }

    public int[] getDeps() {
        return deps;
    }
}
