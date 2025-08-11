package events;

import utils.ChangeType;

public class ListSubChangeEvent<E> {
    private final ChangeType type;
    private final E element;
    private final int index;

    public ListSubChangeEvent(ChangeType type, E element, int index) {
        this.type = type;
        this.element = element;
        this.index = index;
    }

    public ChangeType getType() {
        return type;
    }

    public E getElement() {
        return element;
    }

    public int getIndex() {
        return index;
    }
}