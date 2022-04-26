# The challenge (Data Engineer)

The current project is meant for demonstrative purposes and it was designed to answer (at least in part) to the
requirements in "README.md".

## Design Approach and Motivation

The current project was designed as a configurable Apache Kafka cluster that:
- launches the Zookeeper
- launches multiple Kafka Brokers
- launches a kafka producer that streams data from stream.json
- launches multiple kafka consumers that consume the data streamed and forward it under a new topic and count the 
messages received

Since the core tool of the requirement was Apache Kafka, the natural choice was to design the project around it.
The language chosen, Java 8, was used as the author is fluent in it an there's are also Apache Kafka Java dependencies
which would enable the proper implementation of the project, given the author's knowledge.

For convenience and simplicity the first idea was to launch a customizable Kafka cluster (Zookeeper + Kafka servers) 
by simply running a far with a one parameter. Naturally, cluster configurations - intended to be also customizable - 
went to .properties files. Same for producers / consumers.

By doing this we enabled the decoupled run of Zookeeper + Kafka Servers and producers / consumers.

Main ideas and justifications for choices:
After running the Zookeeper and the Kafka servers, consumers / producers can be started (both from the jars and from the
console in the Kafka home directory). Launching the cluster components via simpel "java -jar" commands based on .properties
files provides a simple way of running the project. 
When the consumers jar is launched, a custom number of Kafka Consumers start running, thus enabling scalability.
The bootstrap.servers configuration is customized based on the number of kafka.servers provided in the kafka-cluster.properties.
Based on that property, a specified number of Kafka servers are launched, running from port :9092 upwards. This way
we make sure that consumers producers are "wired" to all the kafka brokers available, thus enabling scalability.

## Functionality
Prerequisits:

Apache Kafka and Java 8 installed.
Write the Kafka home folder in the kafka-cluster.properties file.

## Solution to requirements

The author considers that steps 1 to 2 are accomplished based on the functionality of the project.

Data is streamed from the "stream.json" file through a Kafka cluster (launched programatically) and consumed by a 
customizable number of consumers under the topic "my-topic" (satisfying requirements 3-4). 

These messages are counted via a synchronized count and displayed in the console (thus satisfying requirement 5).

Those consumers are coupled to producers that forward the same data under the topic "forward-topic".
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

While this serves well for demonstrative purposes, in a real life environment the author's choice for 
orchestrating current project would have consisted of:
- a script that would launch a cluster of containers where each container would run a different process that runs a 
single kafka entity (i.e. single kafka server per process / consumer / producer / zookeeper) - docker (+kubernetes) 
could be a proper tool for setting up such an infrastructure
- of course, a cluster of machines would run these containers (not a single laptop as in the current design)

How can you scale it to improve throughput?
- multiple topic partitions and Kafka servers, thus enabling parallelism
- multiple producers / consumers 
- twisting kafka properties, such as: 
batch.size, linger.ms, num.replica.fetchers, num.partitions, replica.fetch.max.bytes, compression.type (gzip, lz4, snappy, zstd)
- producer acks
- memory properties:
max.block.ms, max.blocks.ms
- rebalancing the cluster

## Set up

1. Install Apahe Kafka (preferably 2.8.1).
2. Install Java 8.
3. Download the project.
4. In a different terminal window per jar, run each of the following jars:
    java -jar KafkaZkBks-1.0-SNAPSHOT.jar ./config/kafka-cluster.properties 
    java -jar KafkaConsumerDefaultTopic-1.0-SNAPSHOT.jar ./config/kafka-cluster.properties 
    java -jar KafkaConsumers-1.0-SNAPSHOT.jar ./config/kafka-cluster.properties
5. Go to the <apache_kafka_installation_folder>/bin and run the following:
    sh kafka-console-consumer.sh --bootstrap-server localhost:9092 --topic forward-topic --from-beginning
    sh kafka-console-producer.sh --broker-list localhost:9092,localhost:9093,localhost:9094 --topic default-topic
6. In a different terminal window per jar, run the following jar:
    java -jar KafkaProducers-1.0-SNAPSHOT.jar ./config/kafka-cluster.properties  

Notes: 

Receiving and forwarding messages:

Notice that the messages sent from the KafkaProducers terminal appear as received in the KafkaConsumers terminal
   (with the topic "my-topic")

Notice that the messages sent from the KafkaProducers terminal appear also as sent in the KafkaConsumers terminal
       (with the topic "forward-topic")

Notice that the messages sent from the KafkaProducers terminal appear also as received in the kafka-console-consumer terminal
        (with the topic "forward-topic")
Notice that in the KafkaConsumers terminal there is also a synchronized count that counts the messages received under "my-topic"
   
Typing messages in the console:

Notice that messages you type in the kafka-console-producer terminal appear in the KafkaConsumerDefaultTopic terminal
    
