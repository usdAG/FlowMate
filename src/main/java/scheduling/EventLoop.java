package scheduling;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.extern.log4j.Log4j2;
import scheduling.events.Event;
import scheduling.events.SubscribeEvent;
import scheduling.events.UnsubscribeEvent;
import scheduling.events.WorkerCountUpdatedEvent;
import utils.Logger;

import javax.swing.*;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.function.Supplier;

@SuppressWarnings({"rawtypes", "unchecked"})
@Log4j2
public class EventLoop implements Runnable {

    private static final AtomicInteger workerCount = new AtomicInteger();

    @Getter
    private boolean running;

    private final BlockingQueue<Event> queue;
    private final List<Subscription> subscribers;

    private ExecutorService executor;
    private Thread thread;

    public EventLoop() {
        this.queue = new LinkedBlockingQueue<>();
        this.subscribers = new ArrayList<>();
        this.executor = Executors.newFixedThreadPool(20);
    }

    public void start() {
        log.debug("starting...");
        if (this.thread != null && this.thread.isAlive()) {
            log.warn("tried to start a thread, which was already running");
            return;
        }

        this.thread = new Thread(this);
        this.running = true;
        this.thread.setName("xo event loop");
        // epic fix for burp
        this.thread.setContextClassLoader(getClass().getClassLoader());
        this.thread.start();
    }

    public void waitForQueue() throws InterruptedException {
        synchronized (queue) {
            while (!queue.isEmpty())
                queue.wait();
        }
    }

    public void submit(Event event) {
        this.queue.add(event);
        if (!this.running) {
            log.info("An event was submitted, starting event loop...");
            this.start();
        }
    }

    /**
     * Utility function to schedule a SwingWorker which runs {@code runnable} and then exits
     *
     * @param runnable the function to execute
     * @return the SwingWorker
     */
    public static RunnableWorker submit(Runnable runnable) {
        RunnableWorker runnableWorker = new RunnableWorker(runnable);
        runnableWorker.execute();
        return runnableWorker;
    }

    /**
     * Utility function to schedule a SwingWorker which executes {@code backgroundOperation} in a background thread and
     * executes {@code guiOperation} in the EventDispatch Thread using the value provided by the former
     *
     * @param <T>                 the type of the value supplied by the background operation and consumed by the gui
     *                            operation
     * @param backgroundOperation task to execute in the background
     * @param guiOperation        task to execute in the event dispatch (gui) thread using the supplied value
     * @return the SwingWorker
     */
    public static <T> BackgroundWorker<? extends T> submit(Supplier<? extends T> backgroundOperation, Consumer<?
            super T> guiOperation) {
        BackgroundWorker<? extends T> backgroundWorker = new BackgroundWorker<>(backgroundOperation, guiOperation);
        backgroundWorker.execute();
        return backgroundWorker;
    }

    public void unsubscribe(Object obj) {
        this.submit(new UnsubscribeEvent(obj));
    }

    public void subscribe(Object obj) {
        this.submit(new SubscribeEvent(obj));
    }

    @Override
    public void run() {
        log.debug("starting event loop thread");
        this.running = true;
        while (running) {
            try {
                Event nextEvent = this.queue.take();
                if (nextEvent instanceof SubscribeEvent) {
                    SubscribeEvent subEvent = (SubscribeEvent) nextEvent;
                    this.addSubscriber(subEvent.getSubscriber());
                } else if (nextEvent instanceof UnsubscribeEvent) {
                    Object subscriber = ((UnsubscribeEvent) nextEvent).getSubscriber();
                    boolean success = this.subscribers.removeIf(sub -> sub.obj.equals(subscriber));
                    log.debug("Unsubscribed, sucess: {}, object: {}", success, subscriber);
                } else {
                    log.debug("publishing event: {}", nextEvent);
                    this.publish(nextEvent);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                Logger.getInstance().logToError(String.format("[%s] ERROR: %s", this.getClass().getName(), e.getMessage()));
                running = false;
            }

            synchronized (queue) {
                if (queue.isEmpty()) {
                    queue.notifyAll();
                }
            }
        }
        log.debug("Event loop was shut down!");
    }

    private void addSubscriber(Object obj) {
        log.debug("new subscriber: {}", obj.getClass().getSimpleName());
        if (this.isAlreadyRegistered(obj)) {
            log.warn("tried to register '{}', which was already registered", obj);
            return;
        }
        Class clazz = obj.getClass();
        while (clazz != Object.class) {
            for (Method method : clazz.getDeclaredMethods()) {
                if (!method.isAnnotationPresent(Subscribe.class)) {
                    continue;
                }

                Subscribe subscribe = method.getAnnotation(Subscribe.class);

                Parameter[] params = method.getParameters();
                if (params.length != 1) {
                    log.warn("Skipped annotation for {} in class {} because it did not have exactly one parameter",
                            method.getName(), clazz);
                    continue;
                }

                if (!Event.class.isAssignableFrom(params[0].getType())) {
                    log.warn("Skipped annotation for {} in class {} because its parameter is not an event!",
                            method.getName(), clazz);
                    continue;
                }
                method.setAccessible(true);

                Class<?> eventType = params[0].getType();

                Subscription sub = new Subscription(obj, method, subscribe.useUiThead(), subscribe.longRunning(),
                        eventType);
                this.subscribers.add(sub);
            }
            clazz = clazz.getSuperclass();
        }
    }

    private boolean isAlreadyRegistered(Object obj) {
        return this.subscribers.stream().anyMatch(sub -> sub.getObj().equals(obj));
    }

    private void publish(Event event) {
        for (Subscription sub : this.subscribers) {
            Class cls = sub.getCls();
            Class eventClass = event.getClass();
            if (!cls.equals(eventClass) && !cls.isAssignableFrom(eventClass)) {
                continue;
            }

            Method method = sub.getMethod();

            if (sub.useUiThread) {
                SwingUtilities.invokeLater(() -> this.invoke(method, sub.getObj(), event));
            } else if (sub.longRunning) {
                this.executor.submit(() -> this.invoke(method, sub.getObj(), event));
            } else {
                this.invoke(method, sub.getObj(), event);
            }
        }
    }

    private void invoke(Method method, Object src, Object... params) {
        try {
            method.invoke(src, params);
        } catch (IllegalAccessException | IllegalArgumentException e) {
            Logger.getInstance().logToError(String.format("[%s] ERROR: %s", this.getClass().getName(), e.getMessage()));
        } catch (InvocationTargetException e) {
            Logger.getInstance().logToError(String.format("[%s] ERROR: %s", this.getClass().getName(), e.getTargetException()));
        }
    }

    public static void increaseWorkerCount() {
        Presenter.submit(new WorkerCountUpdatedEvent(workerCount.incrementAndGet()));
    }

    public static void decreaseWorkerCount() {
        Presenter.submit(new WorkerCountUpdatedEvent(workerCount.decrementAndGet()));
    }

    @Getter
    @Setter
    @ToString
    @AllArgsConstructor
    private static class Subscription {
        private Object obj;
        private Method method;
        private boolean useUiThread;
        private boolean longRunning;
        private Class cls;
    }

}
