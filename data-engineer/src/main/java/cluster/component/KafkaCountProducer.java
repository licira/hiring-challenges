package cluster.component;

import helper.Count;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;

import java.util.Properties;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;

public class KafkaCountProducer extends KafkaClusterComponent {

    private final Properties properties;
    private final String topic;
    private final Count synchronizedCounter;
    private KafkaProducer<String, String> producer;

    public KafkaCountProducer(final Properties properties,
                              final String topic,
                              final Count synchronizedCounter,
                              final ExecutorService executorService) {
        super(executorService);
        this.properties = properties;
        this.topic = topic;
        this.synchronizedCounter = synchronizedCounter;
        this.producer = new KafkaProducer<String, String>(this.properties);
    }

    @Override
    public KafkaCountProducer start() {
        starting();
        submit(task());
        return this;
    }

    @Override
    public KafkaCountProducer stop() {
        stopping();
        producer.flush();
        producer.close();
        return this;
    }

    private Callable task() {
        return () -> {
            while (true) {

                sleep(5000);

                final long count = synchronizedCounter.get();
                final ProducerRecord<String, String> producerRecord =
                        new ProducerRecord<>(topic, "count-key", "Count=" + count);

                producer.send(producerRecord);
                printSent(producerRecord);
            }
        };
    }
}
