
import models.Movie;
import org.elasticsearch.client.Client;
import org.elasticsearch.index.query.QueryBuilder;
import org.junit.Test;
import persistence.elastic.ElasticHelper;
import persistence.elastic.client.ElasticSearchClient;
import persistence.elastic.query.QueryHelper;
import persistence.elastic.utils.ElasticIndex;
import persistence.elastic.utils.ElasticUtils;

import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class TestElasticSearch {

    @Test
    public void testDelete() {
        try {
            Client client = ElasticHelper.getClient(ElasticHelper.Host.LOCALHOST);

            ElasticIndex indexName = ElasticIndex.MOVIES;

            // Now wrap the Elastic client in our bulk processing client:
            ElasticSearchClient<Movie> searchClient = new ElasticSearchClient<>(client, indexName, Movie.class);

            List<Movie> movies = searchClient.matchAllAndDeserialize();

            System.out.println(movies.size());

            long deleted = searchClient.deleteAll();

            System.out.println(deleted);

            System.out.println(searchClient.matchAllAndDeserialize().size());
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void searchElastic() {
        try {
            Client client = ElasticHelper.getClient(ElasticHelper.Host.LOCALHOST);

            ElasticIndex indexName = ElasticIndex.MOVIES;

            // Now wrap the Elastic client in our bulk processing client:
            ElasticSearchClient<Movie> searchClient = new ElasticSearchClient<>(client, indexName, Movie.class);

//            List<Movie> movies = searchClient.matchAll();
            QueryBuilder qb = QueryHelper.matchField("title", "Harry Potter");
            List<Movie> movies = searchClient.searchAndDeserialize(qb);

            System.out.println(movies.size());

            for (Movie movie : movies) {
                System.out.println(movie.toString());
            }
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void testElastic() {
        try {
            Client client = ElasticHelper.getClient(ElasticHelper.Host.LOCALHOST);

            ElasticIndex indexName = ElasticIndex.MOVIES;

            // Ensure index
            createIndex(client, indexName);

            System.out.println(client.admin().toString());

            // Now wrap the Elastic client in our bulk processing client:
            ElasticSearchClient<Movie> searchClient = new ElasticSearchClient<>(client, indexName, Movie.class);

            List<Movie> movies = new ArrayList<>();
            for (Movie movie : Movie.finder().find()) {
                movies.add(movie);
            }

            searchClient.index(movies);

//            // Get all movies from mongo
//            for (Movie movie : Movie.finder().find()) {
//                // Add to elasticsearch
//                searchClient.index(movie);
//                System.out.println(movie.getObjectId().toString());
////                searchClient.deleteByMongoId(movie.getObjectId());
////                break;
//            }

            // The Bulk Insert is asynchronous, we give ElasticSearch some time to do the insert:
            searchClient.awaitClose(1, TimeUnit.SECONDS);
        } catch (UnknownHostException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private void createIndex(Client client, ElasticIndex indexName) {
        if(!ElasticUtils.indexExists(client, indexName).isExists()) {
            ElasticUtils.createIndex(client, indexName);
        }
    }

}
