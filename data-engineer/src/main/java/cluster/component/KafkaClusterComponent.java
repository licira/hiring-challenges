package cluster.component;

import cluster.KafkaCluster;
import helper.StreamGobbler;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.producer.ProducerRecord;

import java.io.IOException;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

public abstract class KafkaClusterComponent {

    protected final ExecutorService executorService;

    protected KafkaClusterComponent(final ExecutorService executorService) {
        this.executorService = executorService;
        stopWithShutdownHook();
    }

    protected void printSent(final ProducerRecord producerRecord) {
        System.out.printf("%s sent: {topic = %s, key = %s, value = %s}\n",
                this,
                producerRecord.topic(),
                producerRecord.key(),
                producerRecord.value());
    }

    protected void printReceived(final ConsumerRecord record) {
        System.out.printf("%s received: {topic = %s, offset = %d, key = %s, value = %s}\n",
                this,
                record.topic(),
                record.offset(),
                record.key(),
                record.value());
    }

    public abstract KafkaClusterComponent start() throws IOException;

    public abstract KafkaClusterComponent stop() throws IOException;

    protected void redirectOutputToStdOut(final Process process) {
        KafkaCluster.redirectOutputToStdOut(process, executorService);
    }

    protected void stopWithShutdownHook() {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                stop();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }));
    }

    protected void sleep(final long millis) throws InterruptedException {
        Thread.sleep(millis);
    }

    protected void starting() {
        System.out.println("Starting: " + this);
    }

    protected void stopping() {
        System.out.println("Stopping: " + this);
    }

    protected void iterating() {
        System.out.println(this + " iterating...");
    }

    protected Future submit(final Callable task) {
        return executorService.submit(task);
    }
}
