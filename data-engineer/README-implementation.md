# The challenge (Data Engineer)

The current project is meant for demonstrative purposes and it was designed to answer (at least in part) to the
requirements in "README.md".

## Design Approach and Motivation

The current project was designed as a configurable Apache Kafka cluster that:
- launches the Zookeeper
- launches multiple Kafka Brokers
- launches a kafka producer that streams data from stream.json
- launches kafka streams using the Stream api that consume the data streamed and count it with respect
 to a predefined time window under a new topic.

Since the core tool of the requirement was Apache Kafka, the natural choice was to design the project around it.
The language chosen, Java 8, was used as the author is fluent in it an there's are also Apache Kafka Java dependencies
which would enable the proper implementation of the project, given the author's knowledge.

For convenience and simplicity the first idea was to launch a customizable Kafka cluster (Zookeeper + Kafka servers) 
by simply running a far with a one parameter. Naturally, cluster configurations - intended to be also customizable - 
went to .properties files. Same for producers / consumers / kafka streams.

By doing this we enabled the decoupled run of Zookeeper + Kafka Servers and producers / consumers / stream API.

Main ideas and justifications for choices:
After running the Zookeeper and the Kafka servers, consumers / producers / steams can be started (both from the jars and from the
console in the Kafka home directory). Launching the cluster components via simpel "java -jar" commands based on .properties
files provides a simple way of running the project. 
When the consumers jar is launched, a custom number of Kafka Consumers start running, thus enabling scalability.
The bootstrap.servers configuration is customized based on the number of kafka.servers provided in the kafka-cluster.properties.
Based on that property, a specified number of Kafka servers are launched, running from port :9092 upwards. This way
we make sure that consumers producers are "wired" to all the kafka brokers available, thus enabling scalability.

## Functionality
Prerequisits:

Apache Kafka and Java 8 installed.
Write the Kafka home folder in the kafka-cluster.properties file. Here the log.retention.hours is set to 8760 hours,
which means the messages will be stored for 2 years (as described in the functional requirements section).

## Solution to requirements

The author considers that steps 1 to 2 are accomplished based on the functionality of the project.

Data is streamed from the "stream.json" file through a Kafka cluster (launched programatically) and consumed by a 
kafka streams (from the Stream API) under the topic "my-topic" (satisfying requirements 3-4). 

These messages are grouped by user id and time windowed and a proper  (thus satisfying requirement 5).

Those kafka streams (that act as consumers) forward the widowed count data under the topic "forward-topic".
Running a kafka consumer from the terminal (as described under the "Set up"
section) will show that messages to get sent under a new topic (satisfying requirement 7).

Requirements 6, 8 haven't been addressed due to time limitation.

Requirement 9: Write about how you could scale
Conceptually speaking the current project is scalable, as it allows a customizable number of Kafka brokers,
consumers to be instantiated, thus coping with large amounts of data.
The author is well aware that in a real life environment the current requirement would have been addressed differently,
but for "learning" purposes the current satisfies scalability by launching several independent Java processes as follows:
- one for Zookeeper and Kafka servers instantiations
- one for each type of consumer / producer servers
Each server mentioned above is launched via a distinct thread branching from the process ran.
Moreover the Kafka Streams Api offers elasticity, scalability, high performance, and fault tolerance by using the the 
kafka consumers provided by he kafka framework. Kafka Streams parallelism can be achieved by configuring the number of
threads within an applicatino instance. 

While this serves well for demonstrative purposes, in a real life environment the author's choice for 
orchestrating current project would have consisted of:
- a script that would launch a cluster of containers where each container would run a different process that runs a 
single kafka entity (i.e. single kafka server per process / consumer / producer / zookeeper) - docker (+kubernetes) 
could be a proper tool for setting up such an infrastructure
- of course, a cluster of machines would run these containers (not a single laptop as in the current design)

Bonus questions / challenges:

How can you scale it to improve throughput?
Some configuration parameters can be set accordingly to optimize throughput. The optimization depends on the particularities
of the system and the requirements. Therefore, no silver bullet solution exists and tuning the application is also 
a trial and error process. Techniques that could improve throughput are the following:

- num.partitions;: creating multiple topic partitions and Kafka servers, thus enabling parallelism: each partition is read by a single 
consumer from a consumer group, thus creating at least the same amount of consumers within a consumer group employs
parallelism, since each partition will have 1 corresponding consumer. The rest of the consumers that do not have a partition
to ingest will stay idle, however they will be useful to take place of a consumer from the group that would fail.

- multiple kafka brokers: each kafka broker can hold topic partitions (either leader partition or replication partition)
from multiple topics. By increasing the number of brokers, we thus have multiple actors that will store partitions and
make them available for consumption. 

- twisting kafka properties, such as: 
batch.size -> with this strategy you can set the size of a batch send to a specific partition. By this, the message
will not get send until the batch size is reached. Larger batch sizes result in fewer requests,
thus reducing the load of the producers and the broker CPU overhead to process each request.; 

linger.ms -> the time to wait before sending messages out to Kafka. The default value is 0, which means that the message
will be send as long as a thread responsible with message sending is available. Kafka producers will send out the next 
batch of messages whenever linger.ms or batch.size is met first. Therefore knowing your data is essential for making 
optimizing these two;
                                                                             
num.replica.fetchers -> represents the number of threads that replicate the message from the source broker. This enables
I/O parallelism, thus obtaining higher fetching throughput, although resulting in higher CPU usage;

replica.fetch.max.bytes (broker) / max.partition.fetch.bytes (consumer) -> maximum data to be fetched from a partition;

replica.fetch.min.bytes (broker) / min.partition.fetch.bytes (consumer) -> minimum data to be fetched from partition, thus
sending data only where a certain threshold is achieved, thus not consuming network bandwidth unless a satisfying amount of data
is reached.

compression.type (gzip, lz4, snappy, zstd) -> by enabling compression on the consumer's side, we could actually send 
less bits over the network but by still employing the same data;

- producer acks: when producers send a message it checks with the broker if the message has arrived. By setting this to zero
 the producer doesn't expect anymore any acknowledge thus it sends data without check time (although this increases the 
 risk of data loss);

You may want count things for different time frames but only do json parsing once.
One possible approach for this would be to deserialize the incoming stream (with json parsed records) and to store it
as a stateful KTable. Afterwards the table would be reused for different (count) statistics, but the json parsing 
won't be necessary as it has already been done before.

Explain how you would cope with failure if the app crashes mid day / mid year.
Since the current system employs the Stream API, fault-tolerance and automatic recovery can be achieved by by using the 
state store which is part of the library. The state store can be used to query data by stream processing applications.
Moreover, stream operations are persisted to Kafka and available later on if the application fails and needs to restore the state.
Kafka Streams restores the associated state stores prior to the failure. This is done by replaying the 
corresponding changelog topics before resuming the processing on the newly started task.

When creating e.g. per minute statistics, how do you handle frames that arrive late or frames with a random timestamp 
(e.g. hit by a bitflip), describe a strategy?
When creating per minute statistics the windowing feature, group by (uid) and count of the Streams API are used. When performing
a group by key, a so called KTable is created which, un like the KStream, represents a stateful abstraction which stores
statistics. Thus, even when frames come late or out of order this state is updated, thus updating the statistics even the 
window was closed long before.

Make it accept data also from std-in (instead of Kafka) for rapid prototyping.
Done - by launching the DefaultTopicConsumer.

Measure also the performance impact / overhead of the json parser.

## Set up

1. Install Apahe Kafka (preferably 2.8.1).
2. Install Java 8.
3. Download the project.
4. In a different terminal window per jar, run each of the following jars:
    java -jar KafkaZkBks-1.0-SNAPSHOT.jar ./config/kafka-cluster.properties 
    java -jar KafkaConsumerDefaultTopic-1.0-SNAPSHOT.jar ./config/kafka-cluster.properties 
    java -jar KafkaStreams-1.0-SNAPSHOT.jar ./config/kafka-cluster.properties
5. Go to the <apache_kafka_installation_folder>/bin and run the following:
    sh kafka-console-consumer.sh --bootstrap-server localhost:9092 --topic forward-topic --from-beginning
    sh kafka-console-producer.sh --broker-list localhost:9092,localhost:9093,localhost:9094 --topic default-topic
6. In a different terminal window per jar, run the following jar:
    java -jar KafkaProducers-1.0-SNAPSHOT.jar ./config/kafka-cluster.properties  

Notes: 

Receiving and forwarding messages:

Notice that the messages sent from the KafkaProducers terminal appear as received in the KafkaStreams terminal
   (with the topic "my-topic")

Notice that the messages sent from the KafkaProducers terminal are counted by predefined duration in the KafkaStreams console  terminal
    and this counting is logged as: "Distinct per window". Afterwards this count is forwarded
       (with the topic "forward-topic")*

Notice that the messages sent from the KafkaProducers terminal appear also as received in the kafka-console-consumer terminal
        (with the topic "forward-topic")
Notice that in the KafkaConsumers terminal there is also a synchronized count that counts the messages received under "my-topic"
   
Typing messages in the console:

Notice that messages you type in the kafka-console-producer terminal appear in the KafkaConsumerDefaultTopic terminal
    
*Windows are kept open all the time to handle out-of-order records that arrive after the window end-time passed. 
 However, windows are not kept forever. 
 They get discarded once their retention time expires.
 There is no special action as to when a window gets discarded. 
 Thus, for each update to an aggregation, a result record is produced (because Kafka Streams also update the aggregation result on out-of-order records).
  Your "final result" would be the latest result record (before a window gets discarded). 
  Manual de-duplication would be a way to resolve the issue 