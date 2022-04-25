package cluster.component;

import cluster.KafkaCluster;
import helper.Count;
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
    private KafkaForwardProducer<String> forwardProducer;
    private Count synchronizedCounter;

    public KafkaSimpleConsumer(final Properties properties,
                               final String topic,
                               final ExecutorService executorService) {
        super(executorService);
        this.properties = properties;
        this.topic = topic;
        this.consumer = new KafkaConsumer<String, String>(this.properties);
    }

    public KafkaSimpleConsumer(final Properties properties,
                               final String topic,
                               final ExecutorService executorService,
                               final Count synchronizedCounter) {
        super(executorService);
        this.properties = properties;
        this.topic = topic;
        this.synchronizedCounter = synchronizedCounter;
        this.consumer = new KafkaConsumer<String, String>(this.properties);
    }

    public KafkaSimpleConsumer setForwardProducer(final KafkaForwardProducer forwardProducer) {
        this.forwardProducer = forwardProducer;
        return this;
    }

    @Override
    public KafkaSimpleConsumer start() {
        starting();
        // Kafka Consumer subscribes list of topics here.
        consumer.subscribe(Arrays.asList(topic));
        // Print the topic name.
        System.out.println(prettyDate() + this + " subscribed to topic: " + topic);

        if (forwardProducer != null) {
            forwardProducer.start();
        }

        submit(task());

        return this;
    }

    @Override
    public KafkaSimpleConsumer stop() {
        stopping();
        consumer.close();
        forwardProducer.stop();
        return this;
    }

    private Callable task() {
        return () -> {
            while (true) {

                iterating();

                final ConsumerRecords<String, String> records = consumer.poll(Duration.ofSeconds(1));

                final List<String> consumerRecordValues = new ArrayList<>(records.count());

                for (ConsumerRecord<String, String> consumerRecord : records) {
                    // print the offset,key and value for the consumer records.
                    printReceived(consumerRecord);
                    consumerRecordValues.add(consumerRecord.value());
                }
                if (forwardProducer != null) {
                    forwardProducer.addAll(consumerRecordValues);
                }
                if (synchronizedCounter != null) {
                    synchronizedCounter.add(records.count());
                }
                consumer.commitSync();
            }
        };
    }
}
