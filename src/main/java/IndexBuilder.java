import opennlp.tools.langdetect.*;
import opennlp.tools.lemmatizer.DictionaryLemmatizer;
import opennlp.tools.postag.POSModel;
import opennlp.tools.postag.POSTaggerME;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.core.WhitespaceAnalyzer;
import org.apache.lucene.analysis.en.EnglishAnalyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.document.FieldType;
import org.apache.lucene.index.*;
import org.apache.lucene.store.ByteBuffersDirectory;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.search.similarities.ClassicSimilarity;
import java.io.*;
import java.text.FieldPosition;
import java.util.Scanner;

public class IndexBuilder {
    private static final String WIKI_DIRECTORY_PATH  = "src/main/resources/wiki-data";
    private String directoryPath = "src/main/resources/";
    private Analyzer analyzer;
    private static Directory index;
    private static IndexWriterConfig config;
    private static IndexWriter writer;
    private int docId;
    private boolean lemmatize = false;
    private boolean positional = false;
    private InputStream posModelIn;
    private POSModel posModel;
    private POSTaggerME posTagger;
    private InputStream dictLemmatizer;
    private DictionaryLemmatizer lemmatizer;
    private IndexOptions indexOptions;

    public IndexBuilder(String indexType) throws IOException, FileNotFoundException {
        if (indexType.equals("lemma")) {
            directoryPath = directoryPath + "lemmatized-indexed-documents";
            analyzer = new StandardAnalyzer();
            lemmatize = true;
            posModelIn = new FileInputStream("src/main/resources/dictionary/en-pos-maxent.bin");
            dictLemmatizer = new FileInputStream("src/main/resources/dictionary/en-lemmatizer.dict");
            posModel = new POSModel(posModelIn);
            posTagger = new POSTaggerME(posModel);
            lemmatizer = new DictionaryLemmatizer(dictLemmatizer);
        }
        else if (indexType.equals("standard")) {
            directoryPath = directoryPath + "standard-indexed-documents";
            analyzer = new StandardAnalyzer();
        }
        else if (indexType.equals("custom")) {
            directoryPath = directoryPath + "custom-indexed-documents";
            analyzer = new CustomAnalyzer();
        }
        else if (indexType.equals("porter")) {
            directoryPath = directoryPath + "stemmed-indexed-documents";
            analyzer = new EnglishAnalyzer();
        }
        else if (indexType.equals("positional")) {
            directoryPath = directoryPath + "positional-indexed-documents";
            analyzer = new EnglishAnalyzer();
            positional = true;
        }
        else {
            System.err.println("Error! Must specify type of search desired");
            throw new RuntimeException();
        }

        try {
            index = FSDirectory.open(new File(directoryPath).toPath());
            config = new IndexWriterConfig(analyzer);
        }
        catch(IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void indexWiki() {
        File wikiFolder = new File(WIKI_DIRECTORY_PATH);
        File[] wikiFiles = wikiFolder.listFiles();
        try {
            writer = new IndexWriter(index, config);
            for (File f : wikiFiles) {
                System.out.println("*****Indexing document: " + f.getName() + "*****");
                addToIndex(f);
            }
            writer.close();
        } catch (IOException e) {
            System.out.println("Exception");
            throw new RuntimeException(e);
        }
    }

    private void addToIndex(File wikiFile) throws IOException {
        // template for each document
        Document doc = new Document();

        String title = "";
        String body = "";
        String categories = "";

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
            }

            // remove markers from subsection headings
            if (isSubsectionHeader(line)) {
                line = line.replace("=", "");
            }

            // to avoid newline character in the beginning of the body
            body = body.equals("") ? line : body + "\n" + line;
        }

        // Add the last document
        addBodyAndWrite(doc, body, writer);
        writer.commit();
        sc.close();
    }

    private boolean isTitle(String line) {
        return line.startsWith("[[") && !line.startsWith("[[File:") && line.endsWith("]]");
    }

    private boolean isCategory(String line) {
        return line.startsWith("CATEGORIES:");
    }

    private boolean isSubsectionHeader(String line) {
        return line.startsWith("=") && line.endsWith("=");
    }
    
    private void addDocIdAndTitle(Document doc, int docId, String title) {
        if (positional) {
            FieldType fieldType = new FieldType();
            fieldType.setIndexOptions(IndexOptions.DOCS_AND_FREQS_AND_POSITIONS_AND_OFFSETS);
            fieldType.setStored(true);
            doc.add(new Field("docID", Integer.toString(docId), fieldType));
            doc.add(new Field("title", title, fieldType));
        } else {
            doc.add(new StringField("docId", Integer.toString(docId), Field.Store.YES));
            doc.add(new TextField("title", title, Field.Store.YES));
        }
    }

    private void addBodyAndWrite(Document doc, String body, IndexWriter writer) throws IOException {
        if (lemmatize) {
            body = lemmatizeString(body);
        }
        if (positional) {
            FieldType fieldType = new FieldType();
            fieldType.setStored(true);
            fieldType.setIndexOptions(IndexOptions.DOCS_AND_FREQS_AND_POSITIONS_AND_OFFSETS);
            doc.add(new Field("body", body, fieldType));

        } else {
            doc.add(new TextField("body", body, Field.Store.YES));
        }
        writer.addDocument(doc);
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

}
