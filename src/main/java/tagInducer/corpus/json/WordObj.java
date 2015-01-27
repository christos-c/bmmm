package tagInducer.corpus.json;

import com.google.gson.GsonBuilder;

/**
 * Words are the word form, part of speech tag and NER label if applicable
 */
public class WordObj {
    private static final GsonBuilder gsonBuilder = new GsonBuilder();

    /* Word form  */
    public String word;
    /* Word form  */
    public String lemma;
    /* Part of speech tag  */
    public String pos;
    /* Coarse Part of speech tag  */
    public String cpos;
    /* Universal Part of speech tag  */
    public String upos;
    /* CCG Category (Supertag) */
    public String cat;
    /* BMMM Cluster */
    public String cluster;

    @Override
    public String toString() {
        return gsonBuilder.disableHtmlEscaping().create().toJson(this);
    }

    @Override
    public boolean equals(Object o) {
        if (!WordObj.class.isInstance(o))
            return false;
        WordObj other = (WordObj) o;
        return ((other.word == null ? word == null : other.word.equals(word)))
                && (other.lemma == null ? lemma == null : other.lemma.equals(lemma))
                && (other.cluster == null ? cluster == null : other.cluster.equals(cluster))
                && (other.pos == null ? pos == null : other.pos.equals(pos))
                && (other.cpos == null ? cpos == null : other.cpos.equals(cpos))
                && (other.upos == null ? upos == null : other.upos.equals(upos))
                && (other.cat == null ? cat == null : other.cat.equals(cat));
    }
}