package tagInducer.corpus;

import junit.framework.TestCase;
import tagInducer.Options;
import tagInducer.OptionsCmdLine;

public class CoNLLCorpusTest extends TestCase {
    Options options;

    public void setUp() throws Exception {
        super.setUp();
        options = new OptionsCmdLine(new String[]{"-in", "src/test/resources/test.conll"});
    }

    public void testReadCorpus() throws Exception {
        Corpus corpus = new CoNLLCorpus(options);
        // We should have 9 words, 8 unique word types
        assertEquals(9, corpus.getWords().length);
        assertEquals(8, corpus.getNumTypes());
    }
}