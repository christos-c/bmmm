package tagInducer.corpus;

import tagInducer.Options;
import tagInducer.features.DepFeatures;
import utils.FileUtils;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;

/**
 * A corpus where each sentence is a single line. Each word can be either
 * tagged (using '/' as a delimiter) or not.
 */
public class LineCorpus extends Corpus {

    /**
     * Extracts a corpus from input
     *
     * @param o The configuration object
     */
    public LineCorpus(Options o) {
        super(o);
    }

    @Override
    protected void readCorpus() {
        String file = o.getCorpusFileName();
        String line;
        int sentenceInd = 0, totalWords = 0;

        //Get the corpus statistics (and also check for tags)
        try {
            BufferedReader in = FileUtils.createIn(file);
            line = in.readLine();
            sentenceInd++;
            totalWords+=line.split("\\s+").length;
            //Check the first line for tags
            HAS_TAGS = checkForTags(line);

            while ((line = in.readLine())!=null){
                sentenceInd++;
                totalWords+=line.split("\\s+").length;
            }
            in.close();
        }
        catch (IOException e) {
            System.err.println("Error reading the corpus file.\n" + e.getMessage());
            System.exit(-1);
        }

        corpus = new int[sentenceInd][];
        words = new int[totalWords];
        if (HAS_TAGS) {
            corpusGoldTags = new int[sentenceInd][];
            goldTags = new int[totalWords];
        }
        sentenceInd = 0;

        //Read the corpus
        try {
            BufferedReader in = FileUtils.createIn(file);
            while ((line = in.readLine())!=null){
                //Map word types to integers
                int[] lineWords = new int[line.split("\\s+").length];
                //Map tags to integers
                int[] lineTags = new int[line.split("\\s+").length];
                int wordInd = 0;
                for (String word:line.split("\\s+")){
                    String tag;
                    //If tags exist, first extract them...
                    if (HAS_TAGS) {
                        tag = s.extractTag(word);
                        word = s.extractWord(word);

                        //... and then get the tag index
                        lineTags[wordInd] = tagsCoder.encode(tag);
                    }

                    //Get the word index
                    lineWords[wordInd] = wordsCoder.encode(word);
                    wordInd++;
                    totalWords++;
                }
                corpus[sentenceInd] = lineWords;
                if (HAS_TAGS) corpusGoldTags[sentenceInd] = lineTags;
                sentenceInd++;
            }
            in.close();
        }
        catch (IOException e) {
            System.err.println("Error reading the corpus file.");
            System.exit(-1);
        }
    }

    private boolean checkForTags(String line) {
        boolean hasTags = true;
        for (String word:line.split("\\s+")){
            hasTags = word.contains("/") && hasTags;
        }
        return hasTags;
    }

    @Override
    public void addDepFeats() throws IOException {
        new DepFeatures(wordsCoder, featureVectors, o.getDepsFile());
    }

    @Override
    public void writeTagged(int[] classes, String outFile) throws IOException {
        String outLine = "";
        int wordInd = 0;
        BufferedWriter out = FileUtils.createOut(outFile);
        for (int[] sentence : corpus) {
            for (int ignored : sentence) {
                outLine += int2Word(words[wordInd]) + "/" + classes[wordInd] + " ";
                wordInd++;
            }
            out.write(outLine.trim() + "\n");
            outLine = "";
        }
        out.close();
    }
}
