/*
Based on the ElasticSearchClient written by Philipp Wagner:
Copyright (c) Philipp Wagner. All rights reserved.
Licensed under the MIT license. See LICENSE file in the project root for full license information.

@author David Sullivan
Made changes to work with default mappings and also support search and deserialization using Jackson
 */

package persistence.elastic.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.elasticsearch.action.bulk.BulkProcessor;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.search.SearchType;
import org.elasticsearch.client.Client;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import persistence.elastic.client.bulk.configuration.BulkProcessorConfiguration;
import persistence.elastic.utils.ElasticIndices;
import utils.JsonUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

public class ElasticSearchClient<T> implements DefaultElasticSearchClient<T> {

    private final Client client;
    private final ElasticIndices indexName;
    private final IndexType indexType;
    private final BulkProcessor bulkProcessor;
    private final Class<T> returnClass;
    private final ObjectMapper mapper = new ObjectMapper();

    public ElasticSearchClient(final Client client, final ElasticIndices indexName, final BulkProcessorConfiguration bulkProcessorConfiguration, Class<T> returnClass) {
        // Default to document indexing
        this(client, indexName, bulkProcessorConfiguration, returnClass, IndexType.DOCUMENT);
    }

    public ElasticSearchClient(final Client client, final ElasticIndices indexName,
                               final BulkProcessorConfiguration bulkProcessorConfiguration, Class<T> returnClass, IndexType indexType) {
        this.client = client;
        this.indexName = indexName;
        this.indexType = indexType;
        this.bulkProcessor = bulkProcessorConfiguration.build(this.client);
        this.returnClass = returnClass;
    }

    @Override
    public void index(T entity) {
        index(Arrays.asList(entity));
    }

    @Override
    public void index(List<T> entities) {
        index(entities.stream());
    }

    @Override
    public void index(Stream<T> entities) {
        entities
                .map(x -> JsonUtils.convertJsonToBytes(x))
                .filter(x -> x.isPresent())
                .map(x -> createIndexRequest(x.get()))
                .forEach(bulkProcessor::add);
    }

    @Override
    public void flush() {
        this.bulkProcessor.flush();
    }

    @Override
    public synchronized boolean awaitClose(long timeout, TimeUnit unit) throws InterruptedException {
        return bulkProcessor.awaitClose(timeout, unit);
    }

    @Override
    public void close() throws Exception {
        this.bulkProcessor.close();
    }

    private IndexRequest createIndexRequest(byte[] messageBytes) {
        return this.client.prepareIndex()
                .setIndex(this.indexName.getIndexName())
                .setType(this.indexType.getIndexType())
                .setSource(messageBytes)
                .request();
    }

    public List<T> matchAll() {
        return search(QueryBuilders.matchAllQuery());
    }

    public List<T> search(QueryBuilder qb) {
        return search(qb, SearchType.DFS_QUERY_THEN_FETCH);
    }

    public List<T> search(QueryBuilder qb, SearchType searchType) {
        SearchResponse searchResponse = this.client.prepareSearch()
                .setTypes(this.indexType.getIndexType())
                .setSearchType(searchType)
                .setPostFilter(qb)
                .execute().actionGet();

        List<SearchHit> hits = Arrays.asList(searchResponse.getHits().getHits());
        List<T> results = new ArrayList<>();

        hits.forEach(hit -> {
            try {
                results.add(this.mapper.readValue(hit.getSourceAsString(), returnClass));
            } catch (IOException e) {
                e.printStackTrace();
            }
        });

        return results;
    }

    public enum IndexType {
        DOCUMENT("document");

        String indexType;

        IndexType(String indexType) {
            this.indexType = indexType;
        }

        public String getIndexType() {
            return this.indexType;
        }
    }
}
