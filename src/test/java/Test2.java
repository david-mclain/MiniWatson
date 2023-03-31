import java.util.List;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;


public class Test2 {
    @Test
    public void testDocs() {
        try {
            Main.main(null);
        }
        catch(Exception e){
            System.out.println("exeption");
        }
    }
}
