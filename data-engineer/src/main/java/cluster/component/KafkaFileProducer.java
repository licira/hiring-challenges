package cluster.component;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.Properties;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;

public class KafkaFileProducer extends KafkaClusterComponent {

    private final String path;
    private final Properties properties;
    private String topic;
    private KafkaProducer<String, String> producer;

    public KafkaFileProducer(final Properties properties,
                             final String topic,
                             final String path,
                             final ExecutorService executorService) {
        super(executorService);
        this.path = path;
        this.properties = properties;
        this.topic = topic;
        this.producer = new KafkaProducer<String, String>(this.properties);
    }

    @Override
    public KafkaClusterComponent start() {
        starting();
        submit(task());
        return this;
    }

    private Callable task() {
        return () -> {
            try (final BufferedReader br = new BufferedReader(new FileReader(path))) {
                String line = br.readLine();
                while (line != null) {

                    final ProducerRecord<String, String> producerRecord =
                            new ProducerRecord<>(topic, topic + "-key", line);
                    producer.send(producerRecord);
                    printSent(producerRecord);

                    line = br.readLine();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
            return true;
        };
    }

    @Override
    public KafkaClusterComponent stop() {
        stopping();
        producer.flush();
        producer.close();
        return this;
    }
}
