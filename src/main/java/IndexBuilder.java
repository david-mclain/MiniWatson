import opennlp.tools.lemmatizer.DictionaryLemmatizer;
import opennlp.tools.postag.POSModel;
import opennlp.tools.postag.POSTaggerME;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.en.EnglishAnalyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.document.FieldType;
import org.apache.lucene.index.*;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import java.io.*;
import java.util.Scanner;

/**
 * Handles the construction of an index, with varying options for doing so.
 * 
 * The following methods are defined:
 *  - indexWiki - Indexes an entire set of wiki documents.
 *  - addToIndex - Adds a document to the index.
 *  - isTitle - determines if a string is a title.
 *  - isCategory - determines if a string is a category
 *  - isSubsectionHeader - determines if a string is a subsection header
 *  - addDocIdAndTitle - adds the title and docID to a document
 *  - addBodyAndWrite - adds the main body of text to a document
 *  - lemmatizeString - lemmatizes a string with OpenNLP
 */
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
    
    /**
     * Constructs the index with varying tokenization and normalization
     * methods. 
     * 
     * The following index types are available:
     *  standard - utilizes lucene's default analyzer
     *  custom - utilizes a custom-made tokenizer
     *  porter - utilizes the Porter-stemming algorithm
     *  position - creates a positional index
     * 
     * @param indexType - String, the configuration type of index to create.
     * @throws IOException
     * @throws FileNotFoundException
     */
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
            directoryPath = directoryPath + "positional-multiquery-indexed-documents";
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

    /**
     * Parses through the entire set of wikipedia documents
     * and adds them to the index.
     * 
     * @param None
     * @return None
     */
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
            throw new RuntimeException(e);
        }
    }

    /**
     * Adds a given file to the index. Any categories are added to
     * the 'categories' section of a document.
     * The 'title' section of the document is the name of the wiki
     * article and the 'body' section contains the rest of the 
     * document.
     * 
     * @param wikiFile -    File, the wikipedia article containing plain
     *                      text to be parsed.
     * @throws IOException
     */
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

            // extracts categories from article, adds it to category field of doc
            if (isCategory(line)) {
                categories = line.substring("CATEGORIES: ".length(), line.length());
                FieldType fieldType = new FieldType();
                fieldType.setStored(true);
                fieldType.setIndexOptions(IndexOptions.DOCS_AND_FREQS_AND_POSITIONS_AND_OFFSETS);
                doc.add(new Field("categories", categories, fieldType));
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

    /**
     * Determines if a line starts with '[[', excludes '[[file' and ends with 
     * ']]'.
     * 
     * @param line - String, the line to parse through.
     * @return boolean - true if the input is a title, false otherwise.
     */
    private boolean isTitle(String line) {
        return line.startsWith("[[") && !line.startsWith("[[File:") && line.endsWith("]]");
    }

    /**
     * Determines if a line starts with 'CATEGORIES'
     * 
     * @param line - String, the line to parse through
     * @return boolean - true if the input is a category, false otherwise.
     */
    private boolean isCategory(String line) {
        return line.startsWith("CATEGORIES:");
    }

    /**
     * Determines if a line starts and ends with '='
     * 
     * @param line - String, the line to parse through.
     * @return  boolean - true if the input is a subsection header, false
     *          otherwise.
     */
    private boolean isSubsectionHeader(String line) {
        return line.startsWith("=") && line.endsWith("=");
    }
    
    /**
     * Adds a title and docID to a given document.
     * 
     * @param doc - Document, the document to add to.
     * @param docId - int, the docID to assign to the document.
     * @param title - String, the title field to be assigned to the document.
     */
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

    /**
     * Adds the body text section to it's respective document. 
     * 
     * Might lemmatize, create the positional information or simply create the 
     * standard tokens for the document, the options for which are extracted
     * from the constructor of the class.
     * 
     * @param doc - Document, the doc to add to.
     * @param body - String, contains the main body of text.
     * @param writer - IndexWriter, the index to add to.
     * @throws IOException
     */
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

    /**
     * Lemmatizes a given string utilizing the openNLP lemmatizer.
     * 
     * @param str - String, the string to lemmatize.
     * @return ret - String, the string once processed.
     */
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
