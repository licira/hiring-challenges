package cluster;

import cluster.component.KafkaClusterComponent;
import cluster.component.KafkaServer;
import cluster.component.Zookeeper;

import java.io.IOException;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class KafkaCluster {

    public static void main(final String... args) throws IOException, InterruptedException {

        final ExecutorService executorService = getExecutorService();

        System.out.println("Starting Kafka Cluster.");

        final String kafkaDir = args[0];
        final String zookeeperPropertiesPath = args[1];
        final String kafkaServerPropertiesPath = args[2];
        final String topic = args[3];

        final KafkaClusterComponent zookeeper =
                startKafkaComponent(new Zookeeper(executorService, kafkaDir, zookeeperPropertiesPath));

        final KafkaClusterComponent kafkaServer1 =
                startKafkaComponent(new KafkaServer(executorService, kafkaDir, kafkaServerPropertiesPath));

        final Properties properties = producerConsumerProperties();

        Thread.sleep(5000);

//        final KafkaClusterComponent kafkaSimpleConsumer =
//                startKafkaComponent(new KafkaSimpleConsumer(properties,
//                        topic,
//                        executorService));
//
//        final KafkaClusterComponent kafkaFileProducer =
//                startKafkaComponent(new KafkaFileProducer(
//                        properties,
//                        topic,
//                        "/Users/liviuc/Documents/work/hiring-challenges/data-engineer/src/main/resources/stream.json",
//                        executorService));

//        stopWithShutdownHook(executorService);

        while (true);
    }

    public static ExecutorService getExecutorService() {
        return new ThreadPoolExecutor(6, 10, 1L, TimeUnit.MILLISECONDS,
                new LinkedBlockingQueue<Runnable>());
    }

    private static KafkaClusterComponent
    startKafkaComponent(final KafkaClusterComponent kafkaComponent) throws IOException {
        return kafkaComponent.start();
    }

    private static Properties producerConsumerProperties() {
        final Properties props = new Properties();
        // producer:
        props.put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer");
        props.put("value.serializer", "org.apache.kafka.common.serialization.StringSerializer");
        props.put("key.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
        props.put("value.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
        props.put("bootstrap.servers", "localhost:9092");
        props.put("acks", "all");
        props.put("linger.ms", 1); // set to 0 let Producer can send message immediately
        // consumer:
        props.put("group.id", "test-group");
        return props;
    }

    private static void stopWithShutdownHook(final ExecutorService executorService,
                                             final KafkaClusterComponent... components) {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                for (final KafkaClusterComponent component : components) {
                    component.stop();
                }
            } catch (final IOException e) {
                e.printStackTrace();
            }
            executorService.shutdownNow();
        }));
    }
}
