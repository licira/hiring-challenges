package cluster.component;

import java.io.IOException;
import java.util.concurrent.ExecutorService;

public class KafkaServer extends KafkaClusterComponent {

    private final String kafkaDir;
    private final String startZookeeperSh;
    private final String stopZookeeperSh;
    private final String kafkaServerPropertiesFile;
    private Process zookeeperProcess;
    private Process zookeeperStopProcess;

    public KafkaServer(final ExecutorService executorService,
                       final String... args) {
        super(executorService);
        this.kafkaDir = args[0];
        this.startZookeeperSh = kafkaDir + "/bin/" + "kafka-server-start.sh";
        this.stopZookeeperSh = kafkaDir + "/bin/" + "kafka-server-stop.sh";
        this.kafkaServerPropertiesFile = args[1];
    }

    public KafkaServer start() throws IOException {
        starting();
        this.zookeeperProcess = Runtime.getRuntime().exec(
                startZookeeperSh + " " + kafkaServerPropertiesFile
        );
        redirectOutputToStdOut(this.zookeeperProcess);
        return this;
    }

    public KafkaServer stop() throws IOException {
        stopping();
        this.zookeeperStopProcess = Runtime.getRuntime().exec(
                stopZookeeperSh
        );
        redirectOutputToStdOut(this.zookeeperStopProcess);
        return this;
    }
}
