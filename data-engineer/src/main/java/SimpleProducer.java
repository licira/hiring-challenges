import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Properties;

public class SimpleProducer {

    public static void main(final String... args) throws IOException, InterruptedException {



        //Assign topicName to string variable
        String topicName = args[0].toString();

        // create instance for properties to access producer configs   
        Properties props = new Properties();

//        //Assign localhost id
//        props.put("bootstrap.servers", "localhost:8080 ");
//
//        //Set acknowledgements for producer requests.
//        props.put("acks", "all");
//
//                //If the request fails, the producer can automatically retry,
//                props.put("retries", 0);
//
//        //Specify buffer size in config
//        props.put("batch.size", 16384);
//
//        //Reduce the no of requests less than 0
//        props.put("linger.ms", 1);
//
//        //The buffer.memory controls the total amount of memory available to the producer for buffering.
//        props.put("buffer.memory", 33554432);
//
        props.put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer");
        props.put("value.serializer", "org.apache.kafka.common.serialization.StringSerializer");
        props.put("key.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
        props.put("value.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");

        props.put("bootstrap.servers", "localhost:9092");
        //props.put("broker-list", "localhost:9092");
        props.put("acks", "all");
        props.put("linger.ms", 1); // set to 0 let Producer can send message immediately


        Producer<String, String> producer = new KafkaProducer<String, String>(props);

        for(int i = 0; i < 10; i++) {
            System.out.println(String.format("attempting to send: %s %s %s", topicName, Integer.toString(i), Integer.toString(i)));

            producer.flush();
            producer.send(new ProducerRecord<String, String>(topicName,
                    Integer.toString(i), Integer.toString(i)));

            System.out.println(String.format("sent: %s %s %s", topicName, Integer.toString(i), Integer.toString(i)));
        }
        System.out.println("Message sent successfully");
        producer.close();
    }
}
