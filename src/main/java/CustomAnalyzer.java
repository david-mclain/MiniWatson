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

public class CustomAnalyzer extends Analyzer {

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
