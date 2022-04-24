package cluster.component;

import cluster.KafkaCluster;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.Properties;
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
    }

    public static void main(final String... args) throws IOException {
        new KafkaFileProducer(defaultProperties(),
                args[0],
                args[1],
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
        props.put("acks", "all");
        props.put("linger.ms", 1); // set to 0 let Producer can send message immediately
        props.put("bootstrap.servers", "localhost:9092");
        props.put("broker.id", "0");
        return props;
    }

    @Override
    public KafkaClusterComponent start() throws IOException {
        System.out.println("Starting KafkaFileProducer");

        this.producer = new KafkaProducer<String, String>(properties);

        try (final BufferedReader br = new BufferedReader(new FileReader(path))) {
            String line = br.readLine();
            while (line != null) {
                final ProducerRecord<String, String> producerRecord = new ProducerRecord<>(topic, "default-key", line);
                producer.send(producerRecord);
                System.out.printf("%s sent topic = %s, key = %s, value = %s\n",
                        KafkaFileProducer.class.getName(),
                        producerRecord.topic(),
                        producerRecord.key(),
                        producerRecord.value());
                line = br.readLine();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return this;
    }

    @Override
    public KafkaClusterComponent stop() throws IOException {
        producer.flush();
        producer.close();
        System.out.println("Stopping KafkaFileProducer");
        return this;
    }
}
