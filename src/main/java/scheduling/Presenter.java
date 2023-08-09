package scheduling;

import lombok.extern.log4j.Log4j2;
import scheduling.events.Event;

@Log4j2
public class Presenter {
    private EventLoop eventLoop = new EventLoop();

    private Presenter() {}

    /**
     * Lazy initialization through initialization-on-demand holder idiom
     *
     * @see <a href="https://en.wikipedia.org/wiki/Initialization-on-demand_holder_idiom">Wikipedia entry</a>
     */
    private static final class LazyHolder {
        static final Presenter INSTANCE = new Presenter();
    }

    /**
     * Sets up static access for strongly coupled components
     * REFACTOR Remove static state and move this to a constructor
     */
    public static void init() {
        Presenter instance = getInstance();
    }

    public static void submit(Event event) {
        getLoop().submit(event);
    }

    private static Presenter getInstance() {
        return LazyHolder.INSTANCE;
    }

    private static EventLoop getLoop() {
        return getInstance().getEventLoop();
    }

    private EventLoop getEventLoop() {
        return eventLoop;
    }
}
