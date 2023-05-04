/**
 * Creates an index of the batch of wikipedia documents.
 */

public class CreateIndex {

    /**
     * Constructs the index with the given specifications and
     * indexes the entire set of wikipedia documents.
     * 
     * @param args  - String Array, determines what type of index 
     *              specifications to use, or defaults to a positinal 
     *              index if none were given.
     */
    public static void main(String[] args) {
        IndexBuilder indexBuilder = null;
        try {
            if (args.length == 1) {
                indexBuilder = new IndexBuilder(args[0]);
            }
            else {
                indexBuilder = new IndexBuilder("positional");
            }
            indexBuilder.indexWiki();
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }
}
