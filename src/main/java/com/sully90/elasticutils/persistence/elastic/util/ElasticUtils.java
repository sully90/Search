package com.sully90.elasticutils.persistence.elastic.util;

import org.elasticsearch.action.admin.indices.create.CreateIndexRequestBuilder;
import org.elasticsearch.action.admin.indices.create.CreateIndexResponse;
import org.elasticsearch.action.admin.indices.exists.indices.IndicesExistsResponse;
import org.elasticsearch.client.Client;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ElasticUtils {

    private static Logger LOGGER = LoggerFactory.getLogger(ElasticUtils.class);

    private ElasticUtils() {

    }

    public static IndicesExistsResponse indexExists(Client client, String index) {
        return client.admin().indices()
                .prepareExists(index)
                .execute().actionGet();
    }

    public static CreateIndexResponse createIndex(Client client, String index) {
        final CreateIndexRequestBuilder createIndexRequestBuilder = client.admin()
                .indices()
                .prepareCreate(index);

        final CreateIndexResponse createIndexResponse = createIndexRequestBuilder.execute().actionGet();

        if(LOGGER.isDebugEnabled()) LOGGER.debug("ElasticUtils: CreateIndexResponse: isAcknowledged {}", createIndexResponse.isAcknowledged());

        return createIndexResponse;
    }

}
