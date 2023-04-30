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
import org.apache.lucene.search.*;
import org.apache.lucene.search.similarities.BM25Similarity;
import org.apache.lucene.search.similarities.ClassicSimilarity;
import org.apache.lucene.search.spans.SpanNearQuery;
import org.apache.lucene.search.spans.SpanOrQuery;
import org.apache.lucene.search.spans.SpanQuery;
import org.apache.lucene.search.spans.SpanTermQuery;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.ByteBuffersDirectory;
import org.apache.lucene.store.FSDirectory;
import java.io.*;
import java.util.ArrayList;
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
    private boolean positional = false;
    private InputStream posModelIn;
    private POSModel posModel;
    private POSTaggerME posTagger;
    private InputStream dictLemmatizer;
    private DictionaryLemmatizer lemmatizer;
    private Scanner input;
    private IndexReader reader;
    private IndexSearcher searcher;

    public static void main(String[] args) throws IOException, ParseException {
        try {
            QueryEngine queryEngine = new QueryEngine("positional", "bm25");
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
        else if (searchType.equals("positional")) {
            directoryPath = directoryPath + "positional-indexed-documents";
            analyzer = new EnglishAnalyzer();
            positional = true;
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
        input = null;
        reader = DirectoryReader.open(index);
        searcher = new IndexSearcher(reader);
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
    }

    public void performQueries() throws IOException, ParseException {
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
            String queryStringNoCat = clue.replaceAll("\\p{Cntrl}", "");

            if (lemmatize) {
                queryString = lemmatizeString(queryString);
            }

            // if clue contains at least one quote, do phrase searches on quotes
            TopDocs docs;
            if (queryStringNoCat.contains("\"") && positional == true) {
                // some of the categories have quotation marks in them, so just splitting on clue
                String[] splitQueryString = queryStringNoCat.split("\"");
                QueryParser queryParser = new QueryParser("body", analyzer);

                // breaking on quotes will cause content within quotes to be in every odd index
                ArrayList<Query> phraseQueries = new ArrayList<Query>();
                for (int i = 1; i < splitQueryString.length; i += 2) {
                    String tempString = QueryParser.escape(splitQueryString[i]);
                    tempString = "\""+tempString+"\"2.5";
                    phraseQueries.add(queryParser.parse(tempString));
                }

                // build a Boolean query with all the quotes as phrase queries and the entire question
                // as a normal query
                BooleanQuery.Builder boolBuilder = new BooleanQuery.Builder();
                for (int i = 0; i < phraseQueries.size(); i++) {
                    boolBuilder.add(phraseQueries.get(i), BooleanClause.Occur.SHOULD);
                }

                // building the complete query (all phrase queries and the full question query)
                queryString = QueryParser.escape(queryString);
                Query fullQuery = queryParser.parse(queryString);
                boolBuilder.add(fullQuery, BooleanClause.Occur.SHOULD);
                BooleanQuery completeQuery = boolBuilder.build();

                int hitsPerPage = 10;
                docs = searcher.search(completeQuery, hitsPerPage);

                // normal query
            } else {
                QueryParser queryParser = new QueryParser("body", analyzer);
                queryString = QueryParser.escape(queryString);
                Query query = queryParser.parse(queryString);
                int hitsPerPage = 10;
                docs = searcher.search(query, hitsPerPage);
            }

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
