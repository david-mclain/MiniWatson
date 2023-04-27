import opennlp.tools.langdetect.*;
import opennlp.tools.lemmatizer.DictionaryLemmatizer;
import opennlp.tools.postag.POSModel;
import opennlp.tools.postag.POSTaggerME;
import java.io.IOException;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.en.EnglishAnalyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.analysis.core.WhitespaceAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.Term;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.similarities.BM25Similarity;
import org.apache.lucene.search.similarities.ClassicSimilarity;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.ByteBuffersDirectory;
import org.apache.lucene.store.FSDirectory;
import java.io.*;
import java.util.Scanner;
import java.util.Hashtable;

public class QueryEngine {
    // File path for text file containing jeopardy questions
    private static final String ANSWERS = "src/main/resources/questions.txt";
    private Analyzer analyzer;    
    private String directoryPath;
    private Directory index;
    private Hashtable<Integer, Integer> hitsAtPositions;
    private boolean lemmatize = false;
    private InputStream posModelIn;
    private POSModel posModel;
    private POSTaggerME posTagger;
    private InputStream dictLemmatizer;
    private DictionaryLemmatizer lemmatizer;

    public static void main(String[] args) throws IOException, ParseException {
        try {
            QueryEngine queryEngine = new QueryEngine("custom", "bm25");
        }
        catch(Exception e) {
            System.out.println("Caught exception");
        }
    }

    public QueryEngine(String searchType, String scoringMethod) throws IOException, ParseException {
        directoryPath = "src/main/resources/";
        if (searchType.equals("lemma")) {
            directoryPath = directoryPath + "lemmatized-indexed-documents";
            analyzer = new StandardAnalyzer();
            lemmatize = true;
            posModelIn = new FileInputStream("src/main/resources/dictionary/en-pos-maxent.bin");
            dictLemmatizer = new FileInputStream("src/main/resources/dictionary/en-lemmatizer.dict");
            posModel = new POSModel(posModelIn);
            posTagger = new POSTaggerME(posModel);
            lemmatizer = new DictionaryLemmatizer(dictLemmatizer);
        }
        else if (searchType.equals("standard")) {
            directoryPath = directoryPath + "standard-indexed-documents";
            analyzer = new StandardAnalyzer();
        }
        else if (searchType.equals("custom")) {
            directoryPath = directoryPath + "custom-indexed-documents";
            analyzer = new CustomAnalyzer();
            System.out.println("Indexing using custom analyzer");
        }
        else if (searchType.equals("porter")) {
            directoryPath = directoryPath + "stemmed-indexed-documents";
            analyzer = new EnglishAnalyzer();
        }
        else {
            System.err.println("Error! Must specify type of search desired");
            throw new RuntimeException();
        }
        index = null;
        hitsAtPositions = new Hashtable<>();
        try {
            index = FSDirectory.open(new File(directoryPath).toPath());
        }
        catch(IOException e) {
            throw new RuntimeException(e);
        }
        Scanner input = null;
        IndexReader reader = DirectoryReader.open(index);
        IndexSearcher searcher = new IndexSearcher(reader);
        if (scoringMethod.equals("bm25")) {
            searcher.setSimilarity((new BM25Similarity((float)1.4, (float)0.15)));
        }
        else if (scoringMethod.equals("tfidf")) {
            searcher.setSimilarity(new TFIDFSimilarity());
        }
        try {
            input = new Scanner(new File(ANSWERS));
        }
        catch(Exception e) {}
        performQueries(input, reader, searcher);
        reader.close();
    }

    private void performQueries(Scanner input, IndexReader reader, IndexSearcher searcher) throws IOException, ParseException {
        int j = 1;
        int matches = 0;
        int hitsAtOne = 0;
        // Loop through every clue and answer in file and print the hits where answer is in top 10 docs
        while (input.hasNextLine()) {
            String category = input.nextLine();
            String clue = input.nextLine();
            String answer = input.nextLine();
            input.nextLine();
            
            String queryString = clue.replaceAll("\\p{Cntrl}", "") + 
                " " + category.replaceAll("\\p{Cntrl}", "");
            QueryParser queryParser = new QueryParser("body", analyzer);
            queryString = QueryParser.escape(queryString);

            if (lemmatize) {
                queryString = lemmatizeString(queryString);
            }
            Query query = queryParser.parse(queryString);
            int hitsPerPage = 10;
            TopDocs docs = searcher.search(query, hitsPerPage);
            ScoreDoc[] hits = docs.scoreDocs;
            System.out.println("Question " + j + ": " + answer.toLowerCase());
            for(int i = 0; i < hits.length; ++i) {
                int docId = hits[i].doc;
                Document d = searcher.doc(docId);
                if ((d.get("title").replaceAll("\\[|\\]", "").toLowerCase().matches(answer.toLowerCase()))) {
                    System.out.println("Document hit for " + answer + " at position: " + (i + 1));
                    matches++;
                    int temp = hitsAtPositions.getOrDefault(i + 1, 0);
                    temp++;
                    hitsAtPositions.put(i + 1, temp);
                    hitsAtOne = i == 0 ? hitsAtOne + 1 : hitsAtOne;
                }
            }
            j++;
        }
        printStats(matches, hitsAtOne);
    }

    private String lemmatizeString(String str) {
        String[] tokens = str.split("[\\s@&.?$+-/=]+");
        String tags[] = posTagger.tag(tokens);
        String[] lemmas = lemmatizer.lemmatize(tokens, tags);
        String ret = "";
        for (int i = 0; i < lemmas.length; i++) {
            if (lemmas[i].equals("O")) {
                ret = ret + tokens[i];
            }
            else {
                ret = ret + lemmas[i];
            }
            if (i != lemmas.length - 1) {
                ret = ret + " ";
            }
        }
        return ret;
    }

    private void printStats(int matches, int hitsAtOne) {
        System.out.println("Total hits in top 10 docs: " + matches);
        System.out.printf("P@1: %.2f\n", (hitsAtOne / 100.0));
        for (int i = 1; i <= 10; i++) {
            // UNCOMMENT BELOW LINE TO SEE WHAT POSITION RESULT IS IN
            System.out.println("Docs in position " + i + ": " + hitsAtPositions.getOrDefault(i, 0));
        }
    }

}
