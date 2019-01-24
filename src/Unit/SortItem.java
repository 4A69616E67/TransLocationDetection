package Unit;


public class SortItem<E extends Comparable<E>> implements Comparable<SortItem<E>> {
    private E Item;
    private char[] Lines;
    public int serial;

    public SortItem(E item, char[] lines) {
        Item = item;
        Lines = lines;
    }

    public E getItem() {
        return Item;
    }

    public char[] getLines() {
        return Lines;
    }

    @Override
    public int compareTo(SortItem<E> o) {
        return Item.compareTo(o.getItem());
    }
}
