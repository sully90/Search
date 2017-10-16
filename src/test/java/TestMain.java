import com.fasterxml.jackson.core.JsonProcessingException;
import models.Movie;
import org.elasticsearch.client.Client;
import org.junit.Test;
import persistence.elastic.ElasticHelper;

import java.net.UnknownHostException;

public class TestMain {

    @Test
    public void test() {
        Movie movie = Movie.finder().findOne();

    }

}
