public class CreateIndex {
    public static void main(String[] args) {
        System.out.println("indexing");
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
