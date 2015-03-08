package tagInducer.utils;

import tagInducer.Evaluator;
import tagInducer.OptionsCmdLine;
import tagInducer.corpus.CCGJSONCorpus;
import tagInducer.corpus.Corpus;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

/**
 * A wrapper for the binary HMM tool (ohmm_train/ohmm_test)
 */
public class HMMWrapper {

    public static void hmmTag(Corpus corpus, String modelName, int numStates, String fileOut)
            throws IOException, InterruptedException {

        String tempFileIn = "temp.txt";
        writeSingleLineFile(corpus, tempFileIn);

        System.out.println("Training HMM");
        String hmmStates = "H=" + numStates + " ";
        String iterations = "I=" + 500 + " ";
        String miscSettings = "M=0 C=1e-4 V=1 p=1 e=0.1 t=0.0001";
        String args = tempFileIn + " " + modelName + " " + hmmStates + iterations + miscSettings;
        Process p = Runtime.getRuntime().exec("bin/ohmm_train " + args);
        BufferedReader stdError = new BufferedReader(new InputStreamReader(p.getErrorStream()));

        // Print a progress
        int count = 0;
        while (stdError.readLine() != null) {
            if (count % 50 == 0) System.out.print('.');
            count++;
        }
        stdError.close();
        System.out.println();

        int exitStatus = p.waitFor();
        if (exitStatus != 0) {
            System.err.println("Training HMM failed");
            String s;
            while ((s = stdError.readLine()) != null) {
                System.err.println(s);
            }
            stdError.close();
            System.exit(-1);
        }

        System.out.println("Testing HMM");
        // ARGS: testfile modelfile outfile -s SMOOTHING
        // outfile will be tempFileIn.{marginal,viterbi} (we will use the marginal)
        args = tempFileIn + " " + modelName + " " + tempFileIn + " -t 1.0 -s 1.0";
        p = Runtime.getRuntime().exec("bin/ohmm_test " + args);

        stdError = new BufferedReader(new InputStreamReader(p.getErrorStream()));

        exitStatus = p.waitFor();
        if (exitStatus != 0) {
            System.err.println("Tagging with HMM failed");
            String s;
            while ((s = stdError.readLine()) != null) {
                System.err.println(s);
            }
            stdError.close();
            System.exit(-1);
        }

        int[][] clusters = readClusters(tempFileIn + ".marginal");


        corpus.setCorpusClusters(clusters);

        String evalScores;
        // Run an evaluation at the end
        if (corpus.hasTags()) {
            System.out.println();
            Evaluator eval = new Evaluator(corpus);
            evalScores = eval.scoresSummary();
            System.out.println(evalScores);
        }

        //Output the tagged file
        corpus.writeTagged(fileOut);
    }

    private static void writeSingleLineFile(Corpus corpus, String file) throws IOException {
        List<String> outLines = new ArrayList<>(corpus.getNumSentences());

        for (String[] s : corpus.getCorpusOriginalSents()) {
            String sent = "";
            for (String w : s) {
                sent += w + " ";
            }
            outLines.add(sent.trim());
        }

        BufferedWriter writer = new BufferedWriter(new FileWriter(file));

        for (String line : outLines) {
            writer.write(line);
            writer.newLine();
        }

        writer.close();
    }

    private static int[][] readClusters(String file) throws FileNotFoundException {
        List<String> hmmTaggedSents = new ArrayList<>();
        Scanner scanner = new Scanner(new FileInputStream(file));

        while (scanner.hasNextLine()) {
            hmmTaggedSents.add(scanner.nextLine());
        }
        scanner.close();

        int[][] clustersArr = new int[hmmTaggedSents.size()][];

        for (int sent = 0; sent < hmmTaggedSents.size(); sent++) {
            String taggedSent = hmmTaggedSents.get(sent);
            String[] split = taggedSent.split("\\s+");
            clustersArr[sent] = new int[split.length];
            for (int word = 0; word < split.length; word++) {
                String wordTag = split[word];
                String tag = wordTag.substring(wordTag.lastIndexOf('/') + 1);
                clustersArr[sent][word] = Integer.parseInt(tag);
            }
        }

        return clustersArr;
    }

    public static void main(String[] args) throws IOException, InterruptedException {
        Corpus corpus = new CCGJSONCorpus(new OptionsCmdLine(new String[]{"-in", args[0]}));
        HMMWrapper.hmmTag(corpus, "temp.hmmModel", Integer.parseInt(args[1]), args[2]);
    }
}
