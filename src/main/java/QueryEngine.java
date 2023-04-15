import java.io.IOException;
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
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.ByteBuffersDirectory;
import org.apache.lucene.store.FSDirectory;
import java.io.File;
import java.util.Scanner;

public class QueryEngine {

    private static final String ANSWERS = "src/main/resources/questions.txt"; // File path for clues and answers
    
    public static void main(String[] args) throws IOException, ParseException {
        StandardAnalyzer analyzer = new StandardAnalyzer();
        Directory index = null;
        // Open directory containing indexed files
        try {
            index = FSDirectory.open(new File("src/main/resources/standard-indexed-documents").toPath());
        }
        catch(IOException e) {
            throw new RuntimeException(e);
        }
        Scanner input = null;
        IndexReader reader = DirectoryReader.open(index);
        IndexSearcher searcher = new IndexSearcher(reader);
        // Open file containing query clues and answers
        try {
            input = new Scanner(new File(ANSWERS));
        }
        catch(Exception e) {}
        int j = 1;
        // Loop through every clue and answer in file and print the results of each query with top 10 docs
        while (input.hasNextLine()) {
            String category = input.nextLine();
            String clue = input.nextLine();
            String answer = input.nextLine();
            input.nextLine();
            Query q = new QueryParser("body", analyzer).parse(clue.replaceAll("[^a-zA-Z0-9 ]", ""));
            int hitsPerPage = 10;
            TopDocs docs = searcher.search(q, hitsPerPage);
            ScoreDoc[] hits = docs.scoreDocs;
            System.out.println("Question " + j + ": " + answer.toLowerCase());
            // 4. display results
            for(int i = 0; i < hits.length; ++i) {
                int docId = hits[i].doc;
                Document d = searcher.doc(docId);
                if (answer.toLowerCase().equals(d.get("title").replaceAll("\\[|\\]", "").toLowerCase())) {
                    System.out.println("Document hit for " + answer + "at position: " + (i + 1));
                }
                System.out.println("Title " + d.get("title") + " found at position: " + (i + 1));
                //System.out.println((i + 1) + ". " + d.get("title") + "\t" + d.get("categories"));
            }
            j++;
        }
        reader.close();
    }
}
