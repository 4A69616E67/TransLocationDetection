package File.FastaFile;

import File.AbstractItem;

import java.util.Comparator;

/**
 * Created by snowf on 2019/2/17.
 */

public class FastaItem extends AbstractItem {
    public String Title;
    public StringBuilder Sequence = new StringBuilder();

    public FastaItem(String title) {
        Title = title;
    }

    public static class TitleComparator implements Comparator<FastaItem> {

        @Override
        public int compare(FastaItem o1, FastaItem o2) {
            return o1.Title.compareTo(o2.Title);
        }
    }
}
