package cluster;

import cluster.component.*;
import helper.Count;
import helper.StreamGobbler;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class KafkaCluster {

    private static final String PROPERTIES_TEMP_PATH = "src/main/resources/properties/temp";

    public static void startKafkaCountProducer(final ExecutorService executorService,
                                               final Count synchronizedCounter,
                                               final Properties kafkaClusterProperties) throws IOException {

        final Properties properties =
                readProperties(kafkaClusterProperties.getProperty("kafka.producer.properties"));

        final String bootstrapServers = bootstrapServers(Integer.parseInt(kafkaClusterProperties.getProperty("kafka.servers")));
        properties.put("bootstrap.servers", bootstrapServers);

        startKafkaComponent(new KafkaCountProducer(properties,
                "count",
                synchronizedCounter,
                executorService));

    }

    public static void startKafkaProducers(final Properties kafkaClusterProperties,
                                           final ExecutorService executorService) throws IOException {
        final Properties properties =
                readProperties(kafkaClusterProperties.getProperty("kafka.producer.properties"));

        final String bootstrapServers = bootstrapServers(Integer.parseInt(kafkaClusterProperties.getProperty("kafka.servers")));
        properties.put("bootstrap.servers", bootstrapServers);

        startKafkaComponent(new KafkaFileProducer(properties,
                kafkaClusterProperties.getProperty("kafka.topic"),
                kafkaClusterProperties.getProperty("kafka.producer.file.path"),
                executorService));
    }

    public static void startKafkaConsumerWithDefaultTopic(final ExecutorService executorService,
                                                          final Properties kafkaClusterProperties) throws IOException {
        final Properties properties = readProperties(kafkaClusterProperties.getProperty("kafka.consumer.properties"));

        final String bootstrapServers = bootstrapServers(Integer.parseInt(kafkaClusterProperties.getProperty("kafka.servers")));
        properties.put("bootstrap.servers", bootstrapServers);

        properties.put("group.id", "default-group");

        startKafkaComponent(new KafkaSimpleConsumer(properties,
                "default-topic",
                executorService));
    }

    public static void startKafkaConsumersWithForwardConsumers(final ExecutorService executorService,
                                                               final Count synchronizedCounter,
                                                               final Properties kafkaClusterProperties) throws IOException {
        final String topic = kafkaClusterProperties.getProperty("kafka.topic");

        final Properties consumerProperties = readProperties(kafkaClusterProperties.getProperty("kafka.consumer.properties"));
        final Properties producerProperties = readProperties(kafkaClusterProperties.getProperty("kafka.producer.properties"));

        final String bootstrapServers = bootstrapServers(Integer.parseInt(kafkaClusterProperties.getProperty("kafka.servers")));
        consumerProperties.put("bootstrap.servers", bootstrapServers);
        producerProperties.put("bootstrap.servers", bootstrapServers);

        for (int i = 0; i < Integer.parseInt(kafkaClusterProperties.getProperty("kafka.consumers")); i++) {
            startKafkaComponent(new KafkaSimpleConsumer(consumerProperties,
                    topic,
                    executorService,
                    synchronizedCounter)
                    .setForwardProducer(
                            new KafkaForwardProducer(producerProperties, "forward-topic", executorService))
            );
        }
    }

    public static String bootstrapServers(final int kafkaServers) {
        final List<String> bootstrapServers = new ArrayList<>();
        for (int i = 0; i < kafkaServers; i++) {
            bootstrapServers.add("localhost:" + (9092 + i));
        }
        return bootstrapServers.stream()
                .collect(Collectors.joining(","));
    }

    public static void startKafkaServers(final ExecutorService executorService,
                                         final String kafkaDir,
                                         final Properties kafkaClusterProperties) throws IOException {

        for (int i = 0; i < Integer.parseInt(kafkaClusterProperties.getProperty("kafka.servers")); i++) {
            startKafkaComponent(new KafkaServer(executorService,
                    kafkaDir,
                    PROPERTIES_TEMP_PATH + "/server" + (i + 1) + ".properties"));
        }
    }

    public static void crateKafkaServersPropertiesFiles(final Properties kafkaClusterProperties,
                                                        final String kafkaServerPropertiesPath,
                                                        final String propertiesTempPath) throws IOException {
        // Read kafka server properties.
        final Properties kafkaServerProperties = readProperties(kafkaServerPropertiesPath);

        // Read number of kafka servers.
        final String kafkaServers = kafkaClusterProperties.getProperty("kafka.servers");

        // Create a server properties file for each server.
        for (int i = 0; i < Integer.parseInt(kafkaServers); i++) {

            final Properties properties = new Properties();
            properties.putAll(kafkaServerProperties);
            properties.put("broker.id", String.valueOf(i));
            // to avoid extra '/'
            properties.put("zookeeper.connect", "localhost:2181");
            properties.put("port", String.valueOf(9092 + i));
            properties.put("log.dirs", "/tmp/kafka-logs-" + (i));

            writeKafkaServerProperties(properties, propertiesTempPath, i + 1);
        }
    }

    private static void writeKafkaServerProperties(final Properties properties,
                                                   final String path,
                                                   final int serverNumber) throws IOException {
        final String filePath = path + "/server" + serverNumber + ".properties";

        final OutputStream outputStream = new FileOutputStream(filePath);

        properties.store(outputStream, null);
    }

    public static void createFolder(final String propertiesTempPath) throws IOException {
        if (new File(propertiesTempPath).exists()) {
            Files.walk(Paths.get(propertiesTempPath))
                    .sorted(Comparator.reverseOrder())
                    .map(path -> path.toFile())
                    .forEach(File::delete);
        }
        if (!new File(propertiesTempPath).mkdirs()) {
            throw new IOException(String.format("Temporary directory %s could not be created", propertiesTempPath));
        }
    }

    public static ExecutorService getExecutorService() {
        return new ThreadPoolExecutor(6, 100, 1L, TimeUnit.MILLISECONDS,
                new LinkedBlockingQueue<Runnable>());
    }

    public static KafkaClusterComponent
    startKafkaComponent(final KafkaClusterComponent kafkaComponent) throws IOException {
        return kafkaComponent.start();
    }

    public static Properties kafkaClusterProperties(String path) throws IOException {
        if (path == null || path.isEmpty()) {
            path = "./src/main/resources/properties/kafka-cluster.properties";
        }
        return readProperties(path);
    }

    private static Properties readProperties(final String path) throws IOException {
        final Properties props = new Properties();
        props.load(new FileInputStream(path));
        return props;
    }

    public static void stopWithShutdownHook(final ExecutorService executorService,
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

    public static void redirectOutputToStdOut(final Process process,
                                       final ExecutorService executorService) {
        final StreamGobbler streamGobbler = new StreamGobbler(process.getInputStream());
        executorService.execute(streamGobbler);
    }
}
