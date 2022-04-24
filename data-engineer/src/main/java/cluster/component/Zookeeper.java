package cluster.component;

import java.io.IOException;
import java.util.concurrent.ExecutorService;

public class Zookeeper extends KafkaClusterComponent {

    private final String kafkaDir;
    private final String startZookeeperSh;
    private final String stopZookeeperSh;
    private final String zookeeperPropertiesFile;
    private Process zookeeperProcess;
    private Process zookeeperStopProcess;


    public Zookeeper(final ExecutorService executorService,
                     final String... args) {
        super(executorService);
        this.kafkaDir = args[0];
        this.startZookeeperSh = kafkaDir + "/bin/" + "zookeeper-server-start.sh";
        this.stopZookeeperSh = kafkaDir + "/bin/" + "zookeeper-server-stop.sh";
        this.zookeeperPropertiesFile = args[1];
    }

    public Zookeeper start() throws IOException {
        starting();
        this.zookeeperProcess = Runtime.getRuntime().exec(
                startZookeeperSh + " " + zookeeperPropertiesFile
        );
        redirectOutputToStdOut(this.zookeeperProcess);
        return this;
    }

    public Zookeeper stop() throws IOException {
        stopping();
        this.zookeeperStopProcess = Runtime.getRuntime().exec(
                stopZookeeperSh
        );
        redirectOutputToStdOut(this.zookeeperStopProcess);
        return this;
    }
}
