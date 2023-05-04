import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.Analyzer.TokenStreamComponents;
import org.apache.lucene.analysis.StopFilter;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.core.LowerCaseFilter;
import org.apache.lucene.analysis.core.StopAnalyzer;
import org.apache.lucene.analysis.en.KStemFilter;
import org.apache.lucene.analysis.en.PorterStemFilter;
import org.apache.lucene.analysis.miscellaneous.CapitalizationFilter;
import org.apache.lucene.analysis.ngram.NGramTokenFilter;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.analysis.standard.StandardFilter;
import org.apache.lucene.analysis.standard.StandardTokenizer;

/**
 * A custom tokenizer created with the intent of improving accuracy
 * of queries.
 * 
 * The following method is overriden:
 * - createComponents - creates a tokenizer stream output
 */
public class CustomAnalyzer extends Analyzer {

    /**
     * Creates a tokenizer stream to be used in the construction
     * of the index. It first delimits words by whitespace
     * and punctuation. Then it converts all words to lower case.
     * It then removes stop words and applies another filter
     * to convert to lowercase form. It utilizes the KSTEM 
     * algorithm for stemming to convert the tokens to their root 
     * form.
     *              
     * @param fieldName - String, input stream to parse and tokenize.
     * @return  a TokenStreamComponents instance, which contains the final 
     *          tokenized stream once processed.
     */
    @Override
    protected TokenStreamComponents createComponents(String fieldName) {
        StandardTokenizer tokenizer = new StandardTokenizer();
        TokenStream tokenStream = new StandardFilter(tokenizer);
        tokenStream = new LowerCaseFilter(tokenStream);
        tokenStream = new StopFilter(tokenStream, StandardAnalyzer.STOP_WORDS_SET);
        tokenStream = new KStemFilter(tokenStream);
        tokenStream = new LowerCaseFilter(tokenStream);
        return new TokenStreamComponents(tokenizer, tokenStream);
    }

}
