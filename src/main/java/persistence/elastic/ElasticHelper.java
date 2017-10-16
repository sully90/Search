package persistence.elastic;

import org.elasticsearch.client.Client;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.transport.InetSocketTransportAddress;
import org.elasticsearch.xpack.client.PreBuiltXPackTransportClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import persistence.elastic.client.bulk.configuration.BulkProcessorConfiguration;
import persistence.elastic.client.bulk.options.BulkProcessingOptions;

import java.net.InetAddress;
import java.net.UnknownHostException;

public class ElasticHelper {

    private static final Logger LOGGER = LoggerFactory.getLogger(ElasticHelper.class);

    public static Client getClient(Host host) throws UnknownHostException {
        if(LOGGER.isInfoEnabled()) LOGGER.info(String.format("Attempting to make connection to ES db %s", host.getHostName()));

        TransportClient client = new PreBuiltXPackTransportClient(Settings.builder()
        .put("cluster.name", "elasticsearch")
        .put("xpack.security.user", "elastic:changeme")
        .build())
                .addTransportAddress(new InetSocketTransportAddress(InetAddress.getByName(host.getHostName()), 9300))
                .addTransportAddress(new InetSocketTransportAddress(InetAddress.getByName(host.getHostName()), 9301));

        if(LOGGER.isInfoEnabled()) LOGGER.info(String.format("Successfully made connection to db to ES db %s", host.getHostName()));

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
