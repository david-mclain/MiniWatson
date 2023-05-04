import org.apache.lucene.search.similarities.BasicStats;
import org.apache.lucene.search.similarities.SimilarityBase;

public class TFIDFSimilarity extends SimilarityBase {

    /**
     * A custom implementation of the tf-idf weighting method to create
     * a score.
     * 
     * @param stats - BasicStats, contains statistics of the entire index.
     * @param termFreq - float, the term frequency of a given term
     * @param docLength - float, the document length.
     * @return float - the score
     */
    @Override
    protected float score(BasicStats stats, float termFreq, float docLength) {
        double tf = 1 + (Math.log(termFreq) / Math.log(2));
        double idf = Math.log((stats.getNumberOfDocuments() + 1) / stats.getDocFreq()) / Math.log(2);
        float similarity = (float) (tf * idf);
        return similarity;
    }

    @Override
    public String toString() {
        return "";
    }
}
