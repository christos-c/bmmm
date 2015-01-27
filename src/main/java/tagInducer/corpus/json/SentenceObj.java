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
            list.add(word.cluster == null ? -1 : Integer.parseInt(word.cluster));
        }
        return list;
    }

    public List<String> getCCGCats() {
        List<String> list = new ArrayList<>();
        for (WordObj word : words) {
            list.add(word.cat == null ? "" : word.cat);
        }
        return list;
    }

    public List<Integer> getCoNLLHeads() {
        if (synPars == null || synPars[0].conllParse == null) // Return list of -1
            return new ArrayList<Integer>() {{  for(int i = 0; i < words.length; ++i) add(-1);  }};

        ArrayList<Integer> list = new ArrayList<>();
        for (CoNLLDep dep : synPars[0].conllParse) {
            list.set(dep.index, dep.head);
        }
        return list;
    }

    /**
     * Creates a new WordObj and populates the values
     */
    public void addWord(String word, String lemma, String pos, String upos, String cluster) {
        if (words == null)
            words = new WordObj[1];
        else
            words = Arrays.copyOf(words, words.length + 1);
        words[words.length-1] = new WordObj();
        words[words.length-1].word  = word;
        words[words.length-1].lemma = lemma;
        words[words.length-1].pos   = pos;
        words[words.length-1].upos  = upos;
        words[words.length-1].cluster   = cluster;
    }

}
