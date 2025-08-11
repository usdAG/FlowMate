package events;

import utils.ChangeType;

import java.util.ArrayList;
import java.util.List;

public class ListChangeEvent<E> {
    private final List<E> list;
    private final List<ListSubChangeEvent<E>> listSubChangeEvents = new ArrayList<>();
    private int subChangeIndex = -1;

    public ListChangeEvent(List<E> list) {
        this.list = list;
    }

    public void addSubChange(ListSubChangeEvent<E> listSubChangeEvent) {
        listSubChangeEvents.add(listSubChangeEvent);
    }

    public boolean next() {
        if (subChangeIndex < listSubChangeEvents.size() - 1) {
            subChangeIndex++;
            return true;
        }
        return false;
    }

    public boolean wasAdded() {
        return getCurrentSubChange().getType() == ChangeType.ADD;
    }

    public boolean wasRemoved() {
        return getCurrentSubChange().getType() == ChangeType.REMOVE;
    }

    private ListSubChangeEvent<E> getCurrentSubChange() {
        if (subChangeIndex >= 0 && subChangeIndex < listSubChangeEvents.size()) {
            return listSubChangeEvents.get(subChangeIndex);
        }
        throw new IllegalStateException("No current sub-change");
    }
}
