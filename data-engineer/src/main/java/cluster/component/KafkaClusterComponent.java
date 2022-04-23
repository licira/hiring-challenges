package cluster.component;

import helper.StreamGobbler;

import java.io.IOException;
import java.util.concurrent.ExecutorService;

public abstract class KafkaClusterComponent {

    protected final ExecutorService executorService;

    protected KafkaClusterComponent(final ExecutorService executorService) {
        this.executorService = executorService;
        stopWithShutdownHook();
    }

    public abstract KafkaClusterComponent start() throws IOException;

    public abstract KafkaClusterComponent stop() throws IOException;

    protected void redirectOutputToStdOut(final Process process) {
        final StreamGobbler streamGobbler = new StreamGobbler(process.getInputStream());
        executorService.execute(streamGobbler);
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
}
