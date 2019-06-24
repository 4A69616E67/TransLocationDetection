package Unit;

import java.io.File;

/**
 * Created by snowf on 2019/2/17.
 *
 */
public class StringArrays implements Comparable<StringArrays> {
    private String[] item;
    private int length;

    public StringArrays(String[] i) {
        item = i;
        length = item.length;
    }

    public StringArrays(int l) {
        length = l;
        item = new String[length];
    }

    public StringArrays(String s, String regex) {
        item = s.split(regex);
        length = item.length;
    }

    public StringArrays(String s) {
        this(s, "\\s+");
    }

    @Override
    public int compareTo( StringArrays o) {
        int result = 0;
        int i = 0;
        while (result == 0 && i < item.length && i < o.getItem().length) {
            result = item[i].compareTo(o.getItem()[i]);
            i++;
        }
        if (result == 0) {
            result = item.length - o.getItem().length;
        }
        return result;
    }

    public static int[] toInteger(String[] s) throws NumberFormatException {
        int[] i = new int[s.length];
        for (int j = 0; j < s.length; j++) {
            i[j] = Integer.parseInt(s[j]);
        }
        return i;
    }

    public static double[] toDouble(String[] s) {
        double[] d = new double[s.length];
        for (int i = 0; i < s.length; i++) {
            d[i] = Double.parseDouble(s[i]);
        }
        return d;
    }

    public static File[] toFile(String[] s) {
        File[] files = new File[s.length];
        for (int i = 0; i < s.length; i++) {
            files[i] = new File(s[i]);
        }
        return files;
    }

    public String[] getItem() {
        return item;
    }

    public int getLength() {
        return length;
    }

    public void set(int index, String s) {
        this.item[index] = s;
    }
}
