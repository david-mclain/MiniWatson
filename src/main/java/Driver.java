/**
 * The main program meant to handle the querying process.
 * 
 */
public class Driver {

   /**
    * Constructs the appropriate query engine and performs a set of
    * predetermined queries.
    * 
    * 
    * @param args - String Array, dictates what type of query engine to 
    *               construct.
    * @return None
    */
    public static void main(String[] args) {
        QueryEngine queryEngine = null;
        try {
            if (args.length == 1) {
                queryEngine = new QueryEngine(args[0], "bm25");
            }
            else if (args.length == 2) {
                queryEngine = new QueryEngine(args[0], args[1]);
            }
            else {
                queryEngine = new QueryEngine("positional", "bm25");
            }
            queryEngine.performQueries();
        }
        catch(Exception e) {
            e.printStackTrace();
            System.err.println("Error occured when beginning search");
        }
    }

}
