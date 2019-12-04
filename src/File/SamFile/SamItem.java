package File.SamFile;

import File.AbstractItem;

import java.util.Comparator;
import java.util.HashMap;

/**
 * Created by snowf on 2019/2/17.
 */
public class SamItem extends AbstractItem {
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
                ExtendCol.put(ss[0] + ":" + ss[1], ss[2]);
            } catch (IndexOutOfBoundsException ignored) {
            }
        }
    }

    public static class NameComparator implements Comparator<SamItem> {

        @Override
        public int compare(SamItem o1, SamItem o2) {
            return o1.Title.compareTo(o2.Title);
        }
    }

    public static class LocationComparator implements Comparator<SamItem> {

        @Override
        public int compare(SamItem o1, SamItem o2) {
            int result = o1.Chr.compareTo(o2.Chr);
            if (result == 0) {
                return o1.BeginSite - o2.BeginSite;
            } else {
                return result;
            }
        }
    }

//    @Override
//    public int compareTo(SamItem o) {
//        if (SortByName) {
//            return Title.compareTo(o.Title);
//        } else {
//            int result = Chr.compareTo(o.Chr);
//            if (result == 0) {
//                return BeginSite - o.BeginSite;
//            } else {
//                return result;
//            }
//        }
//    }
}
