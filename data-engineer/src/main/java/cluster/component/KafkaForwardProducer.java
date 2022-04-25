package cluster.component;

import helper.SynchronizedQueue;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;

import java.util.List;
import java.util.Properties;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;

public class KafkaForwardProducer<E> extends KafkaClusterComponent {

    private final Properties properties;
    private final String topic;
    private final SynchronizedQueue<E> q;
    private KafkaProducer<String, String> producer;

    public KafkaForwardProducer(final Properties properties,
                                final String topic,
                                final ExecutorService executorService) {
        super(executorService);
        this.properties = properties;
        this.topic = topic;
        this.producer = new KafkaProducer<String, String>(this.properties);
        this.q = new SynchronizedQueue<>();
    }

    @Override
    public KafkaForwardProducer<E> start() {
        starting();
        submit(task());
        return this;
    }

    @Override
    public KafkaForwardProducer<E> stop() {
        stopping();
        producer.flush();
        producer.close();
        return this;
    }

    public boolean addAll(final List<E> consumerRecordValues) {
        return q.addAll(consumerRecordValues);
    }

    private Callable task() {
        return () -> {
            while (true) {

                sleep(5000);

                iterating();

                for (final E value : q.pollAll()) {
                    final ProducerRecord<String, String> producerRecord =
                            new ProducerRecord<>(topic, topic + "-key", value.toString());

                    producer.send(producerRecord);
                    printSent(producerRecord);
                }
            }
        };
    }
}
