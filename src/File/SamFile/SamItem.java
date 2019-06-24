package File.SamFile;

import java.util.HashMap;

/**
 * Created by snowf on 2019/2/17.
 */
public class SamItem implements Comparable<SamItem> {
    public String Title;
    public int Stat;
    public String Chr;
    public int BeginSite;
    public int MappingQuality;
    public String MappingStat;
    public String Sequence;
    public String Quality;
    public HashMap<String, String> ExtendCol = new HashMap<>();
    public boolean SortByName = true;

    public SamItem(String[] s) {
        Title = s[0];
        Stat = Integer.parseInt(s[1]);
        Chr = s[2];
        BeginSite = Integer.parseInt(s[3]);
        MappingQuality = Integer.parseInt(s[4]);
        MappingStat = s[5];
        Sequence = s[9];
        Quality = s[10];
        for (int i = 11; i < s.length; i++) {
            String[] ss = s[i].split(":");
            try {
                ExtendCol.put(ss[0], ss[1]);
            } catch (IndexOutOfBoundsException ignored) {
            }
        }
    }

    @Override
    public int compareTo(SamItem o) {
        if (SortByName) {
            return Title.compareTo(o.Title);
        } else {
            int result = Chr.compareTo(o.Chr);
            if (result == 0) {
                return BeginSite - o.BeginSite;
            } else {
                return result;
            }
        }
    }
}
