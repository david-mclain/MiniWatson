import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.store.ByteBuffersDirectory;
import org.apache.lucene.store.Directory;

import java.io.File;
import java.io.IOException;
import java.util.Scanner;

public class IndexBuilder {
    //private boolean indexExists = false;
    private static final String WIKI_DIRECTORY_PATH  = "wiki-data";
    private StandardAnalyzer analyzer;
    private static Directory index;
    private static IndexWriterConfig config;
    private static IndexWriter writer;
    private int docId;  // For the current document

    public IndexBuilder() {
        analyzer = new StandardAnalyzer();
        index = new ByteBuffersDirectory();
        config = new IndexWriterConfig(analyzer);

        // for indexing the wiki-example file
        try {
            writer = new IndexWriter(index, config);
            addToIndex(new File("src/main/resources/wiki-example.txt"));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        // Uncomment below code to index all files in the wiki data
        /*
        File wikiFolder = new File(WIKI_DIRECTORY_PATH);
        File[] wikiFiles = wikiFolder.listFiles();


        try {
            writer = new IndexWriter(index, config);
            for (File f : wikiFiles) {
                System.out.println("***********INDEXING FILE: " + f.getName() + "***********");
                addToIndex(f);
            }
            writer.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
         */
    }

    /**
     * For testing purposes only.
     * @param args
     */
    public static void main(String args[]) {
        IndexBuilder builder = new IndexBuilder();

        // Uncomment below code to print the index
        /*
        try {
            printIndex();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
         */
    }
    void addToIndex(File wikiFile) throws IOException {
        // template for each document
        Document doc = new Document();

        String title = "";
        String body = "";
        String categories = "";

        //IndexWriterConfig config = new IndexWriterConfig(analyzer);
        //IndexWriter writer = new IndexWriter(index, config);

        boolean parsingFirstDoc = true;

        Scanner sc = new Scanner(wikiFile);
        while (sc.hasNextLine()) {
            String line = sc.nextLine().strip();
            if (line.isBlank())
                continue;

            if (isTitle(line)) {
                // Write previous document to index
                if (!parsingFirstDoc) {
                    addBodyAndWrite(doc, body, writer);
                }

                // Create new document
                doc = new Document();
                title = line.substring(2, line.length() - 2);
                body = "";

                addDocIdAndTitle(doc, docId, title);

                docId++;
                parsingFirstDoc = false;

                continue;
            }

            if (isCategory(line)) {
                line = line.substring("CATEGORIES: ".length(), line.length());
                categories = line;
                doc.add(new TextField("categories", categories, Field.Store.YES));
            }

            // remove markers from subsection headings
            if (isSubsectionHeader(line))
                line = line.replace("=", "");

            // to avoid newline character in the beginning of the body
            body = body.equals("")? line : body + "\n" + line;
        }

        // Add the last document
        doc.add(new TextField("body", body, Field.Store.YES));
        writer.addDocument(doc);

        writer.commit();
        sc.close();
    }

    boolean isTitle(String line) {
        return line.startsWith("[[") && !line.startsWith("[[File:") && line.endsWith("]]");
    }

    boolean isCategory(String line) {
        return line.startsWith("CATEGORIES:");
    }

    boolean isSubsectionHeader(String line) {
        return line.startsWith("=") && line.endsWith("=");
    }
    void addDocIdAndTitle(Document doc, int docId, String title) {
        doc.add(new StringField("docId", Integer.toString(docId), Field.Store.YES));
        doc.add(new TextField("title", title, Field.Store.YES));
    }

    void addBodyAndWrite(Document doc, String body, IndexWriter writer) throws IOException {
        doc.add(new TextField("body", body, Field.Store.YES));
        writer.addDocument(doc);
    }

    static void printIndex() throws IOException {
        IndexReader reader = DirectoryReader.open(index);

        for (int i = 0; i < reader.maxDoc(); i++) {
            Document doc = reader.document(i);
            String docId = doc.get("docId");
            String title = doc.get("title");
            String body = doc.get("body");
            String categories = doc.get("categories");

            System.out.println("DOC_ID: " + docId);
            System.out.println("TITLE: " + title);
            System.out.println("BODY:\n" + body);
            System.out.println("CATEGORIES: " + categories);
            System.out.println("============================================================");
        }
    }
}
