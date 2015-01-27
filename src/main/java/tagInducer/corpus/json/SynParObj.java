package tagInducer.corpus.json;

import com.google.gson.GsonBuilder;

/**
 * A syntactic parse in AUTO, PARG and CoNLL formats with a score or probability
 */
public class SynParObj {
    private static final GsonBuilder gsonBuilder = new GsonBuilder();

    public final String synPar;
    public final String depParse;
    public final String conllParse;
    final double score;

    /**
     * Simple parse constructor
     *
     * @param AUTOparse   AUTO Parse
     * @param PARGdeps    PARG dependencies
     * @param CONLLdeps   CoNLL dependencies
     * @param probability Probability of the parse
     */
    public SynParObj(String AUTOparse, String PARGdeps, String CONLLdeps, double probability) {
        synPar = AUTOparse;
        depParse = PARGdeps;
        conllParse = CONLLdeps;
        score = probability;
    }

    @Override
    public String toString() {
        return gsonBuilder.disableHtmlEscaping().create().toJson(this);
    }

    @Override
    public boolean equals(Object o) {
        if (!SynParObj.class.isInstance(o))
            return false;
        SynParObj other = (SynParObj) o;
        return (synPar == null ? other.synPar == null : synPar.equals(other.synPar))
                && (depParse == null ? other.depParse == null : depParse.equals(other.depParse))
                && score == score;
    }
}