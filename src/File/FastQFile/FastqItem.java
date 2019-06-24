package File.FastQFile;

/**
 * Created by snowf on 2019/2/17.
 */
public class FastqItem implements Comparable<FastqItem> {
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

    @Override
    public int compareTo(FastqItem o) {
        return Title.compareTo(o.Title);
    }
}

