package com.sully90.elasticutils.persistence.elastic;

import com.sully90.elasticutils.persistence.elastic.client.bulk.configuration.BulkProcessorConfiguration;
import com.sully90.elasticutils.persistence.elastic.client.bulk.options.BulkProcessingOptions;
import org.elasticsearch.client.Client;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.transport.InetSocketTransportAddress;
import org.elasticsearch.xpack.client.PreBuiltXPackTransportClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ElasticHelper {

    private static final Logger LOGGER = LoggerFactory.getLogger(ElasticHelper.class);

    private static final Map<String, Client> clientConnectionMap;

    static {
        clientConnectionMap = new ConcurrentHashMap<>();
    }

    public static Client getClient(String host) {
        if (!clientConnectionMap.containsKey(host)) {
            try {
                clientConnectionMap.put(host, connect(host));
            } catch (UnknownHostException e) {
                LOGGER.error("Unable to make connection to ES server at host: " + host);
            }
        }
        return clientConnectionMap.get(host);
    }

    public static Client getClient(Host host) { return getClient(host.getHostName()); }

    private static Client connect(String host) throws UnknownHostException {
        if(LOGGER.isInfoEnabled()) LOGGER.info(String.format("Attempting to make connection to ES db %s", host));

        TransportClient client = new PreBuiltXPackTransportClient(Settings.builder()
                .put("cluster.name", "elasticsearch")
                .put("xpack.security.user", "elastic:changeme")
                .build())
                .addTransportAddress(new InetSocketTransportAddress(InetAddress.getByName(host), 9300))
                .addTransportAddress(new InetSocketTransportAddress(InetAddress.getByName(host), 9301));

        if(LOGGER.isInfoEnabled()) LOGGER.info(String.format("Successfully made connection to ES db %s", host));

        return client;
    }

    public static BulkProcessorConfiguration getDefaultBulkProcessorConfiguration() {
        return getDefaultBulkProcessorConfiguration(100);
    }

    public static BulkProcessorConfiguration getDefaultBulkProcessorConfiguration(int numBulkActions) {
        BulkProcessorConfiguration bulkProcessorConfiguration = new BulkProcessorConfiguration(BulkProcessingOptions.builder()
                .setBulkActions(numBulkActions)
                .build());
        return bulkProcessorConfiguration;
    }

    public enum Host {
        LOCALHOST("localhost");

        String hostName;

        Host(String hostName) {
            this.hostName = hostName;
        }

        String getHostName() {
            return this.hostName;
        }
    }

}
