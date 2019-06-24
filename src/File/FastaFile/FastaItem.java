package File.FastaFile;

/**
 * Created by snowf on 2019/2/17.
 */

public class FastaItem implements Comparable<FastaItem> {
    public String Title;
    public StringBuilder Sequence = new StringBuilder();

    public FastaItem(String title) {
        Title = title;
    }

    @Override
    public int compareTo(FastaItem o) {
        return 0;
    }
}
