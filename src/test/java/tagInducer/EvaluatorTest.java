package tagInducer;

import junit.framework.TestCase;
import tagInducer.corpus.Corpus;

public class EvaluatorTest extends TestCase {
    private Evaluator evaluator;
    private Corpus corpus;
    private Options options;

    public void setUp() throws Exception {
        super.setUp();
        options = new OptionsCmdLine(new String[]{"-in", "src/test/resources/test.conll"});
        corpus = new Corpus(options);
    }

    public void testAllMetrics() {
        // The 9 types should be You, xxx, more, cookies, ?, i, gave, you, .
        // Insert a deliberate error by assigning 'more' and 'cookies' to the same category
        int[] clusterAssignment = {1, 2, 3, 3, 4, 1, 2, 1, 4};
        corpus.setCorpusClusters(clusterAssignment);
        evaluator = new Evaluator(corpus);
        assertEquals(80.0, evaluator.manyToOne(), 1.0);
        assertEquals(90.0, evaluator.VMeasure(), 1.0);
        assertEquals(0, evaluator.VI(), 1.0);
    }

    public void testAllMetricsLowercase() {
        options.setLowercase(true);
        corpus = new Corpus(options);
        // The 8 types should be you, xxx, more, cookies, ?, i, gave, .
        // Insert a deliberate error by assigning 'more' and 'cookies' to the same category
        int[] clusterAssignment = {1, 2, 3, 3, 4, 1, 2, 4};
        corpus.setCorpusClusters(clusterAssignment);
        evaluator = new Evaluator(corpus);
        assertEquals(80.0, evaluator.manyToOne(), 1.0);
        assertEquals(90.0, evaluator.VMeasure(), 1.0);
        assertEquals(0, evaluator.VI(), 1.0);
    }
}