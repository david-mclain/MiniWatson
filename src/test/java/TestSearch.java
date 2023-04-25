import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class TestSearch {
    @Test
    public void testDocs() {
        try {
            QueryEngine.main(null);
        }
        catch(Exception e) {}
    }
}
