package banyaninbreeze.demo.simple;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.AbstractExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * An implementation of ExecutorService that always executes commands in the current thread.
 * 
 * When using this class as Populator's global executor sevice (through Populator.setGlobalExecutor(executor)),
 *  or using it in individual Populator objects (through Populator.of(executor, parents)),
 * runParallel() or runWithMode(true) will run population sequentially in the current thread, just like run().
 */
public class SameThreadExecutorService extends AbstractExecutorService {
    private volatile boolean isShutdown = false;

    @Override
    public void execute(Runnable command) {
        // Runs immediately on whatever thread called .execute() or .submit()
        command.run(); 
    }

    @Override
    public void shutdown() { isShutdown = true; }

    @Override
    public List<Runnable> shutdownNow() {
        shutdown();
        return Collections.emptyList();
    }

    @Override
    public boolean isShutdown() { return isShutdown; }

    @Override
    public boolean isTerminated() { return isShutdown; }

    @Override
    public boolean awaitTermination(long timeout, TimeUnit unit) { return true; }
}
