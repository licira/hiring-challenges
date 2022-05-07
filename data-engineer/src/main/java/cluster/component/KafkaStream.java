package cluster.component;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.gson.JsonObject;
import org.apache.kafka.common.serialization.Deserializer;
import org.apache.kafka.common.serialization.Serde;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.common.serialization.Serializer;
import org.apache.kafka.connect.json.JsonDeserializer;
import org.apache.kafka.connect.json.JsonSerializer;
import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.KeyValue;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.kstream.*;

import java.io.IOException;
import java.time.Duration;
import java.util.Properties;
import java.util.concurrent.ExecutorService;

public class KafkaStream extends KafkaClusterComponent {

    private final Properties properties;
    private KafkaStreams kafkaStreams;

    public KafkaStream(final Properties properties,
                       final ExecutorService executorService) {
        super(executorService);
        this.properties = properties;
    }

    public static Serde<JsonNode> jsonSerdes() {
        final Serializer<JsonNode> jsonNodeSerializer = new JsonSerializer();
        final Deserializer<JsonNode> jsonNodeDeserializer = new JsonDeserializer();
        return Serdes.serdeFrom(jsonNodeSerializer, jsonNodeDeserializer);
    }

    @Override
    public KafkaClusterComponent start() throws IOException {
        starting();

        final StreamsBuilder builder = new StreamsBuilder();
        //kafkaStreams.start();
        // Initial stream
        final KStream<String, JsonNode> stream =
                builder.stream("my-topic", Consumed.with(Serdes.String(), jsonSerdes()));

        // Window by duration
        final KStream<Windowed<String>, Long> byUidGroupedByWindow =
                stream.selectKey((key, value) -> value.get("uid").toString())
                        .groupByKey()
                        .windowedBy(tumblingWindows())
                        .count()
                        .toStream();

        byUidGroupedByWindow
                .selectKey((key, value) -> String.valueOf(key.window().start())
                        .concat("/")
                        .concat(String.valueOf(key.window().end())))
                .groupByKey()
                .count()
                .toStream()
                .map((key, value) -> withPayload(key, value))
                .to("forward-topic", Produced.with(Serdes.String(), Serdes.String()));

        // for debugging purposes
//        stream.groupByKey()
//                .windowedBy(TimeWindows.of(oneMinute))
//                .count()
//                .toStream()
//                .foreach((key, value) -> System.out.println(
//                        "Count all per window: " +
//                                "key: " + key + " value: " + value));

        this.kafkaStreams = new KafkaStreams(builder.build(), this.properties);
        this.kafkaStreams.start();

        return this;
    }

    private KeyValue<String, String> withPayload(String key, Long value) {
        final JsonObject payload = new JsonObject();
        payload.addProperty("window", key);
        payload.addProperty("uniqueUsers", String.valueOf(value));
        payload.addProperty("duration", this.properties.getProperty("window.duration"));
        payload.addProperty("unit", this.properties.getProperty("window.unit"));
        System.out.println("Distinct per window: " + payload.toString());
        return new KeyValue<String, String>(key, payload.toString());
    }

//    public SlidingWindows tumblingWindows() {
//        final long windowDuration = Long.parseLong(this.properties.getProperty("window.duration"));
//        final String windowUnit = this.properties.getProperty("window.unit");
//
//        if ("s".equals(windowUnit)) {
//            final Duration duration = Duration.ofSeconds(windowDuration);
//            return SlidingWindows.withTimeDifferenceAndGrace(duration, Duration.ofSeconds(0));
//        }
//        throw new IllegalArgumentException();
//    }

    public TimeWindows tumblingWindows() {
        final long windowDuration = Long.parseLong(this.properties.getProperty("window.duration"));
        final String windowUnit = this.properties.getProperty("window.unit");

        if ("s".equals(windowUnit)) {
            return TimeWindows.of(Duration.ofSeconds(windowDuration));
        } else if ("d".equals(windowUnit)) {
            return TimeWindows.of(Duration.ofDays(windowDuration));
        } else if ("w".equals(windowUnit)) {
            return TimeWindows.of(Duration.ofDays(windowDuration * 7));
        } else if ("m".equals(windowUnit)) {
            return TimeWindows.of(Duration.ofDays(windowDuration * 31));
        } else if ("y".equals(windowUnit)) {
            return TimeWindows.of(Duration.ofDays(windowDuration * 365));
        }
        throw new IllegalArgumentException();
    }

    @Override
    public KafkaClusterComponent stop() throws IOException {
        kafkaStreams.close();
//        kafkaStreams.cleanUp();
//        kafkaStreams.close();
        return this;
    }
}
