import cluster.KafkaCluster;
import cluster.component.Zookeeper;

import java.io.IOException;
import java.util.Properties;
import java.util.concurrent.ExecutorService;

public class KafkaZkBks {

    private static final String PROPERTIES_TEMP_PATH = "src/main/resources/properties/temp";

    public static void main(final String... args) throws IOException {

        // Create executor service.
        final ExecutorService executorService = KafkaCluster.getExecutorService();

        System.out.println("Starting Kafka Cluster...");

        // Read program arguments.
        final String kafkaClusterPropertiesPath = args[0];

        // Read kafka cluster properties.
        final Properties kafkaClusterProperties =
                KafkaCluster.kafkaClusterProperties(kafkaClusterPropertiesPath);

        final String kafkaDir = kafkaClusterProperties.getProperty("kafka.home");
        final String zookeeperPropertiesPath = kafkaClusterProperties.getProperty("zookeeper.properties");
        final String kafkaServerPropertiesPath = kafkaClusterProperties.getProperty("kafka.server.properties.template");

        // Create temp folder.
        KafkaCluster.createFolder(PROPERTIES_TEMP_PATH);
        KafkaCluster.crateKafkaServersPropertiesFiles(kafkaClusterProperties, kafkaServerPropertiesPath, PROPERTIES_TEMP_PATH);

        // Start Zookeeper.
        KafkaCluster.startKafkaComponent(new Zookeeper(executorService, kafkaDir, zookeeperPropertiesPath));

        // Start Kafka Servers (Brokers).
        KafkaCluster.startKafkaServers(executorService,
                kafkaDir,
                kafkaClusterProperties);

        KafkaCluster.stopWithShutdownHook(executorService);

        while (true) ;
    }
}
