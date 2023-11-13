package scheduling;

import utils.Logger;

import javax.swing.*;
import java.util.concurrent.ExecutionException;

final class RunnableWorker extends SwingWorker<Void, Void> {
    private final Runnable runnable;

    RunnableWorker(Runnable runnable) {this.runnable = runnable;}

    @Override
    protected Void doInBackground() {
        EventLoop.increaseWorkerCount();
        Thread.currentThread().setContextClassLoader(getClass().getClassLoader());
        runnable.run();
        return null;
    }

    @Override
    protected void done() {
        try {
            EventLoop.decreaseWorkerCount();
            get();
        } catch (InterruptedException ignored) {
        } catch (ExecutionException e) {
            Logger.getInstance().logToError(String.format("[%s] ERROR: %s", this.getClass().getName(), e.getMessage()));
        }
    }
}
