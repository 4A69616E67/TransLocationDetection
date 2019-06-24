package Unit;
/**
 * Created by snowf on 2019/2/17.
 *
 */
public class SortItem<E extends Comparable<E>> implements Comparable<SortItem<E>> {
    private E Item;
    public int index;
    //    private char[] Lines;
    public int serial;

    public SortItem(E item) {
        Item = item;
    }

    public E getItem() {
        return Item;
    }

    @Override
    public int compareTo( SortItem<E> o) {
        return Item.compareTo(o.getItem());
    }
}
