import cluster.KafkaCluster;
import helper.Count;

import java.io.IOException;
import java.util.Properties;
import java.util.concurrent.ExecutorService;

public class KafkaConsumers {

    public static void main(final String... args) throws IOException {

        // Create executor service.
        final ExecutorService executorService = KafkaCluster.getExecutorService();

        System.out.println("Starting Kafka Consumers...");

        // Read program arguments.
        final String kafkaClusterPropertiesPath = args[0];

        // Read kafka cluster properties.
        final Properties kafkaClusterProperties =
                KafkaCluster.kafkaClusterProperties(kafkaClusterPropertiesPath);

        // Create temp folder.
        final Count synchronizedCounter = new Count();

        // Start Kafka Consumers.
        KafkaCluster.startKafkaConsumersWithForwardConsumers(executorService,
                synchronizedCounter,
                kafkaClusterProperties);

        // Count records.
        KafkaCluster.startKafkaCountProducer(executorService,
                synchronizedCounter,
                kafkaClusterProperties);

        KafkaCluster.stopWithShutdownHook(executorService);

        while (true) ;
    }
}
