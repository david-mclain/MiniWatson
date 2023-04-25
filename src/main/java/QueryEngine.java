import java.io.IOException;
import org.apache.lucene.analysis.en.EnglishAnalyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.queryparser.classic.MultiFieldQueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.ByteBuffersDirectory;
import org.apache.lucene.store.FSDirectory;
import java.io.File;
import java.util.Scanner;
import java.util.Hashtable;

public class QueryEngine {

    private static final String ANSWERS = "src/main/resources/questions.txt"; // File path for clues and answers
    
    public static void main(String[] args) throws IOException, ParseException {
        //StandardAnalyzer analyzer = new StandardAnalyzer();
        CustomAnalyzer analyzer = new CustomAnalyzer();
        Directory index = null;
        Hashtable<Integer, Integer> hitsAtPositions = new Hashtable<>();
        // Open directory containing indexed files
        try {
            index = FSDirectory.open(new File("src/main/resources/custom-indexed-documents").toPath());
        }
        catch(IOException e) {
            throw new RuntimeException(e);
        }
        Scanner input = null;
        IndexReader reader = DirectoryReader.open(index);
        IndexSearcher searcher = new IndexSearcher(reader);
        // COMMENT BELOW LINE TO USE DEFAULT SCORING METHOD
        //searcher.setSimilarity(new TFIDFSimilarity());
        // Open file containing query clues and answers
        try {
            input = new Scanner(new File(ANSWERS));
        }
        catch(Exception e) {}
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
            int hitsPerPage = 10;
            TopDocs docs = searcher.search(queryParser.parse(queryString), hitsPerPage);
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
                // UNCOMMENT BELOW CODE TO SEE WHAT TITLES ARE FOUND AT WHAT POSITION
                //System.out.println("Title " + d.get("title") + " found at position: " + (i + 1));
            }
            j++;
        }
        System.out.println("Total hits in top 10 docs: " + matches);
        System.out.println("P@1: " + (hitsAtOne / 100.0));
        for (int i = 1; i <= 10; i++) {
            // UNCOMMENT BELOW LINE TO SEE WHAT POSITION RESULT IS IN
            System.out.println("Docs in position " + i + ": " + hitsAtPositions.getOrDefault(i, 0));
        }
        reader.close();
    }

}
