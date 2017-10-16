
import models.Movie;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.similarities.TFIDFSimilarity;
import org.elasticsearch.action.search.SearchType;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.lucene.search.function.FiltersFunctionScoreQuery;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.query.functionscore.FunctionScoreQueryBuilder;
import org.elasticsearch.index.query.functionscore.ScoreFunctionBuilders;
import org.elasticsearch.script.Script;
import org.elasticsearch.search.SearchHits;
import org.junit.Test;
import persistence.elastic.ElasticHelper;
import persistence.elastic.client.ElasticSearchClient;
import persistence.elastic.utils.ElasticIndices;
import utils.Duration;

import java.net.UnknownHostException;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class TestMain {

    @Test
    public void test() {
        try {
            Client client = ElasticHelper.getClient(ElasticHelper.Host.LOCALHOST);

            ElasticSearchClient<Movie> searchClient = new ElasticSearchClient<>(client, ElasticIndices.MOVIES, Movie.class);

//            FunctionScoreQueryBuilder.FilterFunctionBuilder[] functions = {
//                    new FunctionScoreQueryBuilder.FilterFunctionBuilder(
//                        ScoreFunctionBuilders.scriptFunction("_score * doc['popularity'].value")
//                    )
//            };

            QueryBuilder match = QueryBuilders.matchQuery(
                    "overview",
                    "James Bond"
            );

            QueryBuilder qb = QueryBuilders.functionScoreQuery(match, ScoreFunctionBuilders.scriptFunction("_score * doc['popularity'].value"));

//            QueryBuilder qb = QueryBuilders.functionScoreQuery(functions);

            System.out.println(qb.toString());

            SearchHits searchHits = searchClient.search(qb, SearchType.DFS_QUERY_THEN_FETCH);
            List<Movie> movies = searchClient.deserialize(searchHits);
            int nhits = movies.size();

            System.out.println("Max score = " + searchHits.getMaxScore());

            for (int i = 0; i < nhits; i++) {
                System.out.println(movies.get(i).toString() + ": " + searchHits.getAt(i).getScore());
                System.out.println(searchHits.getAt(i).getExplanation().toString());
                System.out.println(movies.get(i).getReleaseDate().toString());
                System.out.println();
            }
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void testDuration() throws InterruptedException {
        Date date1 = new Date();

        Thread.sleep(1000);

        Date date2 = new Date();

        Duration duration = new Duration(date1, date2);
        System.out.println(String.format("%f", (float) duration.getDuration()));
        System.out.println(String.format("%f", (float) duration.getDuration(TimeUnit.SECONDS)));
    }

}
