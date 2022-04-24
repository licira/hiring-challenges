import cluster.KafkaCluster;

import java.io.IOException;
import java.util.Properties;
import java.util.concurrent.ExecutorService;

import static cluster.KafkaCluster.bootstrapServers;

public class KafkaConsoleDefaultProducer {
    // not working!!!
    public static void main(final String... args) throws IOException {

        // Create executor service.
        final ExecutorService executorService = KafkaCluster.getExecutorService();

        System.out.println("Starting Kafka Console Default Producer...");

        // Read program arguments.
        final String kafkaClusterPropertiesPath = args[0];

        // Read kafka cluster properties.
        final Properties kafkaClusterProperties =
                KafkaCluster.kafkaClusterProperties(kafkaClusterPropertiesPath);

        final String bootstrapServers =
                bootstrapServers(Integer.parseInt(kafkaClusterProperties.getProperty("kafka.servers")));
        final String kafkaDir = kafkaClusterProperties.getProperty("kafka.home");

        final Process process = Runtime.getRuntime().exec("sh " +
                kafkaDir + "/bin/ " + "kafka-console-producer.sh" +
                        " --broker-list " +
                        bootstrapServers +
                        " --topic default-topic"
        );

        KafkaCluster.redirectOutputToStdOut(process, executorService);
    }
}
