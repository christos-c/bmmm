package tagInducer.utils;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;

/**
 * Utility class that converts a raw tokenised file (one sentence per line) to a CoNLL format.
 */
public class RawToCoNLL {
    public static void main(String[] args) throws IOException {
        BufferedReader in = FileUtils.createIn(args[0]);
        BufferedWriter out = FileUtils.createOut(args[0] + ".conll");
        String line;
        while ((line = in.readLine()) != null) {
            String[] words = line.split("\\s+");
            for (int i = 0; i < words.length; i++) {
                out.write((i+1) + "\t" + words[i] + "\t_\t_\t_\t_\t_\t_\t_\n");
            }
            out.write("\n");
        }
        out.close();
    }
}
