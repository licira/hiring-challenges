import cluster.KafkaCluster;

import java.io.IOException;
import java.util.Properties;
import java.util.concurrent.ExecutorService;

public class KafkaProducers {

    public static void main(final String... args) throws IOException {

        // Create executor service.
        final ExecutorService executorService = KafkaCluster.getExecutorService();

        System.out.println("Starting Kafka Producers...");

        // Read program arguments.
        final String kafkaClusterPropertiesPath = args[0];

        // Read kafka cluster properties.
        final Properties kafkaClusterProperties =
                KafkaCluster.kafkaClusterProperties(kafkaClusterPropertiesPath);

        KafkaCluster.startKafkaProducers(kafkaClusterProperties, executorService);

        KafkaCluster.stopWithShutdownHook(executorService);

        executorService.shutdown();
    }
}
