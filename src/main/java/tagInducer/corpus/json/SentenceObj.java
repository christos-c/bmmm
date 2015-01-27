package tagInducer.corpus.json;

import com.google.gson.GsonBuilder;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Stores a series of entities, words and syntactic parses
 * @author ybisk
 * @author christod
 */
public class SentenceObj {
    private static final GsonBuilder gsonBuilder = new GsonBuilder();

    public WordObj[] words;
    public SynParObj[] synPars;
    public String sentence;

    public boolean equals(Object o) {
        if (!SentenceObj.class.isInstance(o))
            return false;
        SentenceObj other = (SentenceObj) o;
        return Arrays.deepEquals(words, other.words)
                && Arrays.deepEquals(synPars, other.synPars)
                && (sentence == null ? other.sentence == null : sentence.equals(other.sentence));
        // Does not require variable accounts in equality
    }

    @Override
    public String toString() {
        return gsonBuilder.disableHtmlEscaping().create().toJson(this);
    }

    public List<String> getWords() {
        List<String> list = new ArrayList<>();
        for (WordObj word : words) {
            list.add(word.word);
        }
        return list;
    }

    public List<String> getUPOSs() {
        List<String> list = new ArrayList<>();
        for (WordObj word : words) {
            list.add(word.upos);
        }
        return list;
    }

    public List<String> getGoldTags() {
        List<String> list = new ArrayList<>();
        for (WordObj word : words) {
            list.add(word.pos);
        }
        return list;
    }

    public List<Integer> getClusters() {
        List<Integer> list = new ArrayList<>();
        for (WordObj word : words) {
            list.add(Integer.parseInt(word.cluster));
        }
        return list;
    }

    public List<String> getCCGCats() {
        List<String> list = new ArrayList<>();
        for (WordObj word : words) {
            list.add(word.cat);
        }
        return list;
    }

    public List<Integer> getCoNLLHeads() {
        List<Integer> list = new ArrayList<>();
        for (SynParObj synPar : synPars) {
            //TODO something like:
            //list.add(Integer.parseInt(synPar.conllParse.head));
        }
        return list;
    }
}
