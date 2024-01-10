package events;

import java.util.EventListener;

public interface DeferMatchingFinishedListener extends EventListener {

    void onDeferMatchingFinishedEvent();
}
