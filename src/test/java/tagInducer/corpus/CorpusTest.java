package tagInducer.corpus;

import junit.framework.TestCase;
import tagInducer.Options;
import tagInducer.OptionsCmdLine;

public class CorpusTest extends TestCase {
    private Options options;

    public void setUp() throws Exception {
        super.setUp();
        options = new OptionsCmdLine(new String[]{"-in", "src/test/resources/test.conll"});
    }

    public void testReadCorpus() throws Exception {
        Corpus corpus = new Corpus(options);
        // We should have 10 words, 9 unique word types ('You' and 'you' are different types)
        assertEquals(10, corpus.getNumTokens());
        assertEquals(9, corpus.getNumTypes());
    }

    public void testReadCorpusLowercase() throws Exception {
        options.setLowercase(true);
        Corpus corpus = new Corpus(options);
        // We should have 10 words, 8 unique word types ('You' and 'you' become the same type)
        assertEquals(10, corpus.getNumTokens());
        assertEquals(8, corpus.getNumTypes());
    }

    public void testReadCorpusIgnorePunct() throws Exception {
        options.setLowercase(true);
        options.setIgnorePunct(true);
        Corpus corpus = new Corpus(options);
        // We should have 10 words, 7 unique word types ('?' and '.' become the same type)
        assertEquals(10, corpus.getNumTokens());
        assertEquals(7, corpus.getNumTypes());
    }
}