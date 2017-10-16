
import models.Movie;
import org.elasticsearch.client.Client;
import org.junit.Test;
import persistence.elastic.ElasticHelper;
import persistence.elastic.client.ElasticSearchClient;
import persistence.elastic.client.bulk.configuration.BulkProcessorConfiguration;
import persistence.elastic.client.bulk.options.BulkProcessingOptions;
import persistence.elastic.utils.ElasticIndices;
import persistence.elastic.utils.ElasticUtils;

import java.net.UnknownHostException;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class TestElasticSearch {

    @Test
    public void searchElastic() {
        try {
            Client client = ElasticHelper.getClient(ElasticHelper.Host.LOCALHOST);

            // Set bulk processing options
            BulkProcessorConfiguration bulkProcessorConfiguration = new BulkProcessorConfiguration(BulkProcessingOptions.builder()
                    .setBulkActions(100)
                    .build());

            ElasticIndices indexName = ElasticIndices.MOVIES;

            // Now wrap the Elastic client in our bulk processing client:
            ElasticSearchClient<Movie> searchClient = new ElasticSearchClient<>(client, indexName, bulkProcessorConfiguration, Movie.class);

            List<Movie> movies = searchClient.matchAll();

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

            // Set bulk processing options
            BulkProcessorConfiguration bulkProcessorConfiguration = new BulkProcessorConfiguration(BulkProcessingOptions.builder()
            .setBulkActions(100)
            .build());

            ElasticIndices indexName = ElasticIndices.MOVIES;

            // Ensure index
            createIndex(client, indexName);

            System.out.println(client.admin().toString());

            // Now wrap the Elastic client in our bulk processing client:
            ElasticSearchClient<Movie> searchClient = new ElasticSearchClient<>(client, indexName, bulkProcessorConfiguration, Movie.class);

            // Get all movies from mongo
            for (Movie movie : Movie.finder().find()) {
                // Add to elasticsearch
                searchClient.index(movie);
                break;
            }

            // The Bulk Insert is asynchronous, we give ElasticSearch some time to do the insert:
            searchClient.awaitClose(1, TimeUnit.SECONDS);
        } catch (UnknownHostException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private void createIndex(Client client, ElasticIndices indexName) {
        if(!ElasticUtils.indexExists(client, indexName).isExists()) {
            ElasticUtils.createIndex(client, indexName);
        }
    }

}
