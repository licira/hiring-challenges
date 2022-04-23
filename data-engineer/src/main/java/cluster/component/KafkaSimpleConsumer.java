package cluster.component;

import cluster.KafkaCluster;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;

import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;

public class KafkaSimpleConsumer extends KafkaClusterComponent {

    private final Properties properties;
    private final String topic;
    private KafkaConsumer<String, String> consumer;

    public KafkaSimpleConsumer(final Properties properties,
                               final String topic,
                               final ExecutorService executorService) {
        super(executorService);
        this.properties = properties;
        this.topic = topic;
    }

    public static void main(final String... args) throws IOException {
        new KafkaSimpleConsumer(defaultProperties(),
                args[0],
                KafkaCluster.getExecutorService())
                .start();
    }

    private static Properties defaultProperties() {
        // create instance for properties to access producer configs
        final Properties props = new Properties();
        props.put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer");
        props.put("value.serializer", "org.apache.kafka.common.serialization.StringSerializer");
        props.put("key.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
        props.put("value.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
        props.put("bootstrap.servers", "localhost:9092");
        props.put("group.id", "test-group");
        return props;
    }

    @Override
    public KafkaClusterComponent start() throws IOException {
        System.out.println("Starting KafkaSimpleConsumer");

        this.consumer = new KafkaConsumer<String, String>(properties);

        //Kafka Consumer subscribes list of topics here.
        consumer.subscribe(Arrays.asList(topic));

        //print the topic name
        System.out.println("Subscribed to topic: " + topic);

        final Callable<Boolean> consumerTask = () -> {
            while (true) {

                System.out.println("KafkaSimpleConsumer iterating...");

                final ConsumerRecords<String, String> records = consumer.poll(Duration.ofSeconds(1));
                for (ConsumerRecord<String, String> record : records) {
                    // print the offset,key and value for the consumer records.
                    System.out.printf("%s topic = %s, offset = %d, key = %s, value = %s\n",
                            KafkaSimpleConsumer.class.getName(),
                            record.topic(),
                            record.offset(),
                            record.key(),
                            record.value());
                }
                consumer.commitSync();
            }
        };

        final List<Callable<Boolean>> callableTasks = new ArrayList<>();
        callableTasks.add(consumerTask);

        try {
            executorService.invokeAll(callableTasks);
        } catch (final InterruptedException e) {
            throw new RuntimeException(e);
        }

        return this;
    }

    @Override
    public KafkaClusterComponent stop() {
        consumer.close();
        System.out.println("Stopping KafkaSimpleConsumer");
        return this;
    }
}
