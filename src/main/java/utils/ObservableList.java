package utils;

import events.ListChangeEvent;
import events.ListChangeListener;
import events.ListSubChangeEvent;

import java.util.*;

public class ObservableList<E> implements List<E> {
    private final List<E> delegate = new ArrayList<>();
    private final List<ListChangeListener<E>> listeners = new ArrayList<>();

    public void addListener(ListChangeListener<E> listener) {
        listeners.add(listener);
    }

    public void removeListener(ListChangeListener<E> listener) {
        listeners.remove(listener);
    }

    protected void notifyListeners(ListChangeEvent<E> change) {
        for (ListChangeListener<E> listener : listeners) {
            listener.onChanged(change);
        }
    }

    @Override
    public boolean add(E e) {
        boolean modified = delegate.add(e);
        if (modified) {
            ListChangeEvent<E> change = new ListChangeEvent<>(this);
            change.addSubChange(new ListSubChangeEvent<>(ChangeType.ADD, e, delegate.size() - 1));
            notifyListeners(change);
        }
        return modified;
    }

    @Override
    public void add(int index, E element) {
        delegate.add(index, element);
        ListChangeEvent<E> change = new ListChangeEvent<>(this);
        change.addSubChange(new ListSubChangeEvent<>(ChangeType.ADD, element, index));
        notifyListeners(change);
    }

    @Override
    public boolean addAll(Collection<? extends E> collection) {
        if (collection.isEmpty()) return false;
        int startIndex = delegate.size();
        boolean modified = delegate.addAll(collection);
        if (modified) {
            ListChangeEvent<E> change = new ListChangeEvent<>(this);
            for (E e : collection) {
                change.addSubChange(new ListSubChangeEvent<>(ChangeType.BULK_ADD, e, startIndex++));
            }
            notifyListeners(change);
        }
        return modified;
    }

    @Override
    public boolean addAll(int index, Collection<? extends E> collection) {
        if (collection.isEmpty()) return false;
        boolean modified = delegate.addAll(index, collection);
        if (modified) {
            ListChangeEvent<E> change = new ListChangeEvent<>(this);
            for (E e : collection) {
                change.addSubChange(new ListSubChangeEvent<>(ChangeType.BULK_ADD, e, index++));
            }
            notifyListeners(change);
        }
        return modified;
    }

    @Override
    public boolean removeAll(Collection<?> collection) {
        boolean modified = false;
        for (Object item : collection) {
            modified |= remove(item);
        }
        return modified;
    }

    @Override
    public boolean retainAll(Collection<?> collection) {
        boolean modified = false;
        for (Iterator<E> it = delegate.iterator(); it.hasNext(); ) {
            E item = it.next();
            if (!collection.contains(item)) {
                it.remove();
                ListChangeEvent<E> change = new ListChangeEvent<>(this);
                change.addSubChange(new ListSubChangeEvent<>(ChangeType.BULK_REMOVE, item, -1));
                notifyListeners(change);
                modified = true;
            }
        }
        return modified;
    }

    @Override
    public void clear() {
        if (!delegate.isEmpty()) {
            List<E> removedElements = new ArrayList<>(delegate);
            delegate.clear();
            ListChangeEvent<E> change = new ListChangeEvent<>(this);
            for (E element : removedElements) {
                change.addSubChange(new ListSubChangeEvent<>(ChangeType.CLEAR, element, -1));
            }
            notifyListeners(change);
        }
    }

    @Override
    public E remove(int index) {
        E removedElement = delegate.remove(index);
        ListChangeEvent<E> change = new ListChangeEvent<>(this);
        change.addSubChange(new ListSubChangeEvent<>(ChangeType.REMOVE, removedElement, index));
        notifyListeners(change);
        return removedElement;
    }

    @Override
    public boolean remove(Object o) {
        int index = delegate.indexOf(o);
        boolean removed = delegate.remove(o);
        if (removed) {
            ListChangeEvent<E> change = new ListChangeEvent<>(this);
            change.addSubChange(new ListSubChangeEvent<>(ChangeType.REMOVE, (E) o, index));
            notifyListeners(change);
        }
        return removed;
    }

    @Override
    public E set(int index, E element) {
        E oldElement = delegate.set(index, element);
        ListChangeEvent<E> change = new ListChangeEvent<>(this);
        change.addSubChange(new ListSubChangeEvent<>(ChangeType.SET, element, index));
        notifyListeners(change);
        return oldElement;
    }

    @Override
    public boolean containsAll(Collection<?> collection) {
        return delegate.containsAll(collection);
    }
    @Override
    public int size() {
        return delegate.size();
    }

    @Override
    public boolean isEmpty() {
        return delegate.isEmpty();
    }

    @Override
    public boolean contains(Object o) {
        return delegate.contains(o);
    }

    @Override
    public Iterator<E> iterator() {
        return delegate.iterator();
    }

    @Override
    public Object[] toArray() {
        return delegate.toArray();
    }

    @Override
    public <T> T[] toArray(T[] ts) {
        return delegate.toArray(ts);
    }

    @Override
    public E get(int index) {
        return delegate.get(index);
    }

    @Override
    public int indexOf(Object o) {
        return delegate.indexOf(o);
    }

    @Override
    public int lastIndexOf(Object o) {
        return delegate.lastIndexOf(o);
    }

    @Override
    public ListIterator<E> listIterator() {
        return delegate.listIterator();
    }

    @Override
    public ListIterator<E> listIterator(int index) {
        return delegate.listIterator(index);
    }

    @Override
    public List<E> subList(int fromIndex, int toIndex) {
        return delegate.subList(fromIndex, toIndex);
    }

}


