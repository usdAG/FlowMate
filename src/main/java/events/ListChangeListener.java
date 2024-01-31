package events;

public interface ListChangeListener<E> {
    void onChanged(ListChangeEvent<? extends E> change);
}