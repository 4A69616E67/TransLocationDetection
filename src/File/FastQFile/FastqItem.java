package File.FastQFile;

import File.AbstractItem;

import java.util.Comparator;

/**
 * Created by snowf on 2019/2/17.
 */
public class FastqItem extends AbstractItem {
    public String Title;
    public String Sequence;
    public String Orientation;
    public String Quality;

    public FastqItem(String title) {
        Title = title;
    }

    public FastqItem(String[] s) {
        Title = s[0];
        Sequence = s[1];
        Orientation = s[2];
        Quality = s[3];
    }

    @Override
    public String toString() {
        return Title + "\n" + Sequence + "\n" + Orientation + "\n" + Quality;
    }

    public static class TitleComparator implements Comparator<FastqItem> {

        @Override
        public int compare(FastqItem o1, FastqItem o2) {
            return o1.Title.compareTo(o2.Title);
        }
    }

//    @Override
//    public int compareTo(FastqItem o) {
//        return Title.compareTo(o.Title);
//    }
}

