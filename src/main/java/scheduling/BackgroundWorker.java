package scheduling;

import javax.swing.*;

import utils.Logger;

import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;
import java.util.function.Supplier;

final class BackgroundWorker<T> extends SwingWorker<T, Void> {
    private final Supplier<? extends T> backgroundOperation;
    private final Consumer<? super T> guiOperation;

    BackgroundWorker(Supplier<? extends T> backgroundOperation, Consumer<? super T> guiOperation) {
        this.backgroundOperation = backgroundOperation;
        this.guiOperation = guiOperation;
    }

    @Override
    protected T doInBackground() {
        EventLoop.increaseWorkerCount();
        Thread.currentThread().setContextClassLoader(getClass().getClassLoader());
        return backgroundOperation.get();
    }

    @Override
    protected void done() {
        try {
            EventLoop.decreaseWorkerCount();
            guiOperation.accept(get());
        } catch (InterruptedException ignored) {
        } catch (ExecutionException e) {
            Logger.getInstance().logToError(String.format("[%s] ERROR: %s", this.getClass().getName(), e.getMessage()));
        }
    }
}
