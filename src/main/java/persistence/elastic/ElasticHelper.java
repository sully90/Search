package persistence.elastic;

import org.elasticsearch.client.Client;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.transport.InetSocketTransportAddress;
import org.elasticsearch.xpack.client.PreBuiltXPackTransportClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetAddress;
import java.net.UnknownHostException;

public class ElasticHelper {

    private static final Logger LOGGER = LoggerFactory.getLogger(ElasticHelper.class);

    public static Client getClient(Host host) throws UnknownHostException {
        TransportClient client = new PreBuiltXPackTransportClient(Settings.builder()
        .put("cluster.name", "elasticsearch")
        .put("xpack.security.user", "elastic:changeme")
        .build())
                .addTransportAddress(new InetSocketTransportAddress(InetAddress.getByName(host.getHostName()), 9300))
                .addTransportAddress(new InetSocketTransportAddress(InetAddress.getByName(host.getHostName()), 9301));

        return client;
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
