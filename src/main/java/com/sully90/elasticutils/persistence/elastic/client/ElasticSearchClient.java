/*
Based on the ElasticSearchClient written by Philipp Wagner:
Copyright (c) Philipp Wagner. All rights reserved.
Licensed under the MIT license. See LICENSE file in the project root for full license information.

@author David Sullivan
Made changes to work with default mappings and also support search and deserialization using Jackson
 */

package com.sully90.elasticutils.persistence.elastic.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sully90.elasticutils.persistence.elastic.ElasticHelper;
import com.sully90.elasticutils.persistence.elastic.client.bulk.configuration.BulkProcessorConfiguration;
import com.sully90.elasticutils.persistence.elastic.query.QueryHelper;
import com.sully90.elasticutils.utils.JsonUtils;
import org.bson.types.ObjectId;
import org.elasticsearch.action.bulk.BulkProcessor;
import org.elasticsearch.action.delete.DeleteResponse;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.search.SearchType;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.xcontent.XContentType;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.reindex.BulkByScrollResponse;
import org.elasticsearch.index.reindex.DeleteByQueryAction;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

public class ElasticSearchClient<T> implements DefaultSearchClient<T> {

    private static final Logger LOGGER = LoggerFactory.getLogger(ElasticSearchClient.class);

    private final Client client;
    private final String indexName;
    private final IndexType indexType;
    private final BulkProcessor bulkProcessor;
    private final Class<T> returnClass;

    protected static final ObjectMapper MAPPER = new ObjectMapper();

    public ElasticSearchClient(final String indexName, final Class<T> returnClass) {
        this(ElasticHelper.getClient("localhost"), indexName, returnClass);
    }

    public ElasticSearchClient(final Client client, final String indexName, final Class<T> returnClass) {
        // Default to document indexing
        this(client, indexName, returnClass, ElasticHelper.getDefaultBulkProcessorConfiguration(), IndexType.DOCUMENT);
    }

    public ElasticSearchClient(final Client client, final String indexName, final Class<T> returnClass,
                               final BulkProcessorConfiguration bulkProcessorConfiguration) {
        // Default to document indexing
        this(client, indexName, returnClass, bulkProcessorConfiguration, IndexType.DOCUMENT);
    }

    public ElasticSearchClient(final Client client, final String indexName, final Class<T> returnClass,
                               final BulkProcessorConfiguration bulkProcessorConfiguration, final IndexType indexType) {
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

    public void indexWithPipeline(T entity, String pipeline) {
        indexWithPipeline(Arrays.asList(entity), pipeline);
    }

    public void indexWithPipeline(List<T> entities, String pipeline) {
        indexWithPipeline(entities.stream(), pipeline);
    }

    public void indexWithPipeline(Stream<T> entities, String pipeline) {
        entities
                .map(x -> JsonUtils.convertJsonToBytes(x))
                .filter(x -> x.isPresent())
                .map(x -> createIndexRequest(x.get(), pipeline))
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

    protected IndexRequest createIndexRequest(byte[] messageBytes) {
        return createIndexRequest(messageBytes, XContentType.JSON);
    }

    protected IndexRequest createIndexRequest(byte[] messageBytes, XContentType xContentType) {
        return this.client.prepareIndex()
                .setIndex(this.indexName)
                .setType(this.indexType.getIndexType())
                .setSource(messageBytes, XContentType.JSON)
                .request();
    }

    protected IndexRequest createIndexRequest(byte[] messageBytes, String pipeline) {
        return createIndexRequest(messageBytes, pipeline, XContentType.JSON);
    }

    protected IndexRequest createIndexRequest(byte[] messageBytes, String pipeline, XContentType xContentType) {
        return this.client.prepareIndex()
                .setIndex(this.indexName)
                .setType(this.indexType.getIndexType())
                .setSource(messageBytes, xContentType)
                .setPipeline(pipeline)
                .request();
    }

    //------------------------------------- S E A R C H -------------------------------------//

    public SearchHits matchAll() {
        return matchAll(SearchType.DFS_QUERY_THEN_FETCH);
    }

    public SearchHits matchAll(SearchType searchType) {
        return search(QueryBuilders.matchAllQuery(), searchType);
    }

    public SearchHits search(QueryBuilder qb) {
        return search(qb, SearchType.DFS_QUERY_THEN_FETCH);
    }

    public SearchHits search(QueryBuilder qb, SearchType searchType) {
        SearchResponse searchResponse = this.client.prepareSearch()
                .setTypes(this.indexType.getIndexType())
                .setSearchType(searchType)
                .setQuery(qb)
                .setExplain(true)
                .execute().actionGet();

        SearchHits searchHits = searchResponse.getHits();
        return searchHits;
    }

    public SearchHits search(QueryBuilder qb, QueryBuilder filter, SearchType searchType) {
        SearchResponse searchResponse = this.client.prepareSearch()
                .setTypes(this.indexType.getIndexType())
                .setSearchType(searchType)
                .setQuery(qb)
                .setPostFilter(filter)
                .setExplain(true)
                .execute().actionGet();

        SearchHits searchHits = searchResponse.getHits();
        return searchHits;
    }

    public List<T> matchAllAndDeserialize() {
        return matchAllAndDeserialize(SearchType.DFS_QUERY_THEN_FETCH);
    }

    public List<T> matchAllAndDeserialize(SearchType searchType) {
        return searchAndDeserialize(QueryBuilders.matchAllQuery(), searchType);
    }

    public List<T> searchAndDeserialize(QueryBuilder qb) {
        return searchAndDeserialize(qb, SearchType.DFS_QUERY_THEN_FETCH);
    }

    public List<T> searchAndDeserialize(QueryBuilder qb, SearchType searchType) {
        SearchHits searchHits = search(qb, searchType);

        return deserialize(searchHits);
    }

    public List<T> deserialize(SearchHits searchHits) {
        List<SearchHit> hits = Arrays.asList(searchHits.getHits());
        return deserialize(hits);
    }

    public List<T> deserialize(List<SearchHit> searchHits) {
        List<T> results = new ArrayList<>();

        searchHits.forEach(hit -> {
            try {
                results.add(MAPPER.readValue(hit.getSourceAsString(), returnClass));
            } catch (IOException e) {
                e.printStackTrace();
            }
        });

        return results;
    }

    //------------------------------------- D E L E T E -------------------------------------//

    public String deleteById(String id) {
        DeleteResponse response = this.client
                .prepareDelete(this.indexName, this.indexType.getIndexType(), id)
                .setIndex(this.indexName)
                .execute()
                .actionGet();
        return response.getId();
    }

    public long deleteByMongoId(ObjectId id) {
        QueryBuilder qb = QueryHelper.matchField(QueryHelper.Fields.MONGOID.getFieldName(), id.toString());
        return deleteByQuery(qb);
    }

    public long deleteAll() {
        return deleteByQuery(QueryBuilders.matchAllQuery());
    }

    public long deleteByQuery(QueryBuilder qb) {
        BulkByScrollResponse response =
                DeleteByQueryAction.INSTANCE.newRequestBuilder(this.client)
                        .filter(qb)
                        .source(this.indexName)
                        .execute().actionGet();

        long deleted = response.getDeleted();
        return deleted;
    }

    public BulkProcessor getBulkProcessor() {
        return bulkProcessor;
    }

    //------------------------------------- E N D -------------------------------------//

    public enum IndexType {
        DOCUMENT("document");

        private String indexType;

        IndexType(String indexType) {
            this.indexType = indexType;
        }

        public String getIndexType() {
            return this.indexType;
        }
    }
}
