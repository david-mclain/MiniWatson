import java.util.List;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;


public class Test2 {
    @Test
    public void testDocs() {
        try {
            QueryEngine.main(null);
        }
        catch(Exception e) {
            e.printStackTrace();
            System.out.println("exeption");
        }
    }
}
