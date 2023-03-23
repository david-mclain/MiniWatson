import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Scanner;

import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.IndexWriter;

public class Indexer {
	private static final String WIKI_DATA = "src/main/resources/wiki-example.txt";	// filepath for docs
	private static ArrayList<Document> collection;	// stores each doc as one long String
	private static ArrayList<String> rawCollection;
	
    public static void main(String[] args) {
        /*
    	if (args.length != 1) {
            System.err.println("Error! Missing command line argument.");
            System.err.println("Usage: java Indexer.java {fileName}");
        }
        */
    	
    	// TODO: Create IndexWriter to pass to parse function based on given args
    	// parseDocs(writer);
    	
    	
    	parseDataSimple();
    	for (Document d : collection) {
    		System.out.println(d.get("title"));
    	}
    }
    
    
    // parses data -> docs and indexes them
    private static void parseData(IndexWriter writer) {
    	try {
			collection = new ArrayList<Document>();
			int curDoc = -1;	// index of current doc in the collection
			Scanner scanner;
			scanner = new Scanner(new File(WIKI_DATA));
			while (scanner.hasNextLine()) {		// iterates over each document in file, indexes it
				String line = scanner.nextLine().strip();
				if (line.length() > 0) {		// ignores empty lines
					if (line.startsWith("[[")) {	// checks if current line starts with "[["
						if (!line.contains("File:")) {			// checks if line is a file or doc title
							if (curDoc >= 0) {	// indexes doc if finished
								writer.addDocument(collection.get(curDoc));
							}
							curDoc += 1;
							Document doc = new Document();		// creates new doc for collection
							doc.add(new StringField("docid", Integer.toString(curDoc), Field.Store.YES));
							doc.add(new TextField("title", line, Field.Store.YES));
							collection.add(doc);
						} else {									// adds files to document
							addToDoc(curDoc, "file", line);
						}
					} else if (line.startsWith("CATEGORIES: ")) {	// adds categories to document
						addToDoc(curDoc, "categories", line);
					} else {										// adds body content to document
						addToDoc(curDoc, "body", line);
					}
				}
			}
			writer.addDocument(collection.get(curDoc));	// writes final doc
		} catch (IOException e) {
			e.printStackTrace();
		}
    }
    
    // TODO: delete eventually, debugging only   --   same as above but does not index docs
    private static void parseDataSimple() {
		try {
			collection = new ArrayList<Document>();
			int curDoc = -1;	// index of current doc in the collection
			Scanner scanner;
			scanner = new Scanner(new File(WIKI_DATA));
			while (scanner.hasNextLine()) {		// iterates over each document in file, indexes it
				String line = scanner.nextLine().strip();
				if (line.length() > 0) {		// ignores empty lines
					if (line.startsWith("[[")) {	// checks if current line starts with "[["
						if (!line.contains("File:")) {			// checks if line is a file or doc title
							curDoc += 1;
							Document doc = new Document();
							doc.add(new StringField("docid", Integer.toString(curDoc), Field.Store.YES));
							doc.add(new TextField("title", line, Field.Store.YES));
							collection.add(doc);
						} else {				// adds files to document
							addToDoc(curDoc, "file", line);
						}
					} else if (line.startsWith("CATEGORIES: ")) {	// adds categories to document
						addToDoc(curDoc, "categories", line);
					} else {					// adds body content to document
						addToDoc(curDoc, "body", line);
					}
				}
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
    }
    
    
    // parses data into strings, each doc = 1 long string (lines separated by \n\n)
    private static void parseDataRaw() {
    	try {
    		rawCollection = new ArrayList<String>();
			int curDoc = -1;	// index of current doc in the collection
			Scanner scanner;
			scanner = new Scanner(new File(WIKI_DATA));
			while (scanner.hasNextLine()) {		// iterates over each document in file, indexes it
				String line = scanner.nextLine().strip();
				if (line.length() > 0) {		// ignores empty lines
					if (line.startsWith("[[")) {	// checks if current line starts with "[["
						if (!line.contains("File:")) {			// checks if line is a file or doc title
							curDoc += 1;
							rawCollection.add(curDoc, line + "\n\n");
						} else {				// adds files to document
							String updated = rawCollection.get(curDoc) + line + "\n\n";
							rawCollection.remove(curDoc);
							rawCollection.add(curDoc, updated);
						}
					} else {					// adds body content to document
						String updated = rawCollection.get(curDoc) + line + "\n\n";
						rawCollection.remove(curDoc);
						rawCollection.add(curDoc, updated);
					}
				}
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
    }
    
    
    // helper function -- adds new data to the specified additive field of the given doc
    private static void addToDoc(int curDoc, String field, String line) {
    	if (collection.get(curDoc).get(field) != null) {	// if preexisting data, don't overwrite
			String updated = collection.get(curDoc).get(field) + line + "\n\n";
			collection.get(curDoc).removeField(field);
			collection.get(curDoc).add(new TextField(field, updated, Field.Store.YES));
		} else {											// no preexisting data, instantiate field
			collection.get(curDoc).add(new TextField(field, line + "\n", Field.Store.YES));
		}
    }
}
