# MiniWatson

### Description
MiniWatson is a simplified version of IBM Watson's Question Answering (QA) System. Documents used for QA are a sampled set of 280000 wikipedia documents. Indexing program will index all 280000 documents based on the type of indexing desired (options are positional, custom, lemma, porter, and standard). When indexed, all documents will appear in a directory for the program to access when queries for questions are ran. When running the questions program will be given the type of indexing that was done, along with desired scoring function to use (options for scoring are bm25, tfidf, and default)  
Output will be produced in the following format:  
*------------------------------------------------------------------------*  
Currently searching for: The Washington Post  
Document hit for The Washington Post at position: 5  
Currently searching for: Taiwan  
Document hit for Taiwan at position: 1   
...  
...  
...  
Currently searching for: 3M  
Currently searching for: Robert Downey, Jr.  
Document hit for Robert Downey, Jr. at position: 1  
Total hits in top 10 docs: 64  
P@1: 0.40  
Docs in position 1: 40  
Docs in position 2: 10  
...  
...  
...  
Docs in position 9: 0  
Docs in position 10: 2  
*------------------------------------------------------------------------*  
### Usage
Before you can Query you will need to download all of the indexed documents, which can be done by running `./getIndexedDocs`, which will download all of the indexed documents, and place them where they should be in the directory or run the indexing on your own, by running `./getDocs` which will retrieve the subset of wikipedia documents, then after that finishes run `./indexDocs {optional_index_type}`.  
To run the query engine you will need to use a bash shell and have maven installed and run `./runQueries {optional_index_type} {optional_scoring_method}`. If no parameters are passed in, the program will default to positional index type with bm25 scoring  
