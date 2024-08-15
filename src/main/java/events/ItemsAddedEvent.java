package events;

import java.util.EventObject;

public class ItemsAddedEvent extends EventObject {
    public ItemsAddedEvent(Object source) {
        super(source);
    }
}
