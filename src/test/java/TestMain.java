import models.Movie;
import org.junit.Test;

public class TestMain {

    @Test
    public void test() {
        Movie movie = Movie.finder().findOne();

    }

}
