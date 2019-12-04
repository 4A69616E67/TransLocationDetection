package File.BedFile;


import File.AbstractItem;
import File.BedPeFile.BedpeItem;
import Unit.ChrRegion;
import Unit.InterAction;

import java.util.Comparator;

/**
 * Created by snowf on 2019/2/24.
 */

public class BedItem extends AbstractItem {
    private String SeqTitle;
    private ChrRegion Location;
    private int Score;
    public String[] Extends = new String[0];
//    public Sort SortBy = Sort.Location;
//
//    public enum Sort {
//        SeqTitle, Location
//    }

    public BedItem(String[] s) {
        Location = new ChrRegion(s[0], Integer.parseInt(s[1]), Integer.parseInt(s[2]));
        if (s.length > 3) {
            SeqTitle = s[3];
        }
        if (s.length > 4) {
            Score = Integer.parseInt(s[4]);
        }
        if (s.length > 5) {
            Location.Orientation = s[5].charAt(0);
        }
        if (s.length > 6) {
            Extends = new String[s.length - 6];
            System.arraycopy(s, 6, Extends, 0, Extends.length);
        }
    }

    public BedItem(String seqTitle, ChrRegion location, int score, String[] anExtends) {
        SeqTitle = seqTitle;
        Location = location;
        Score = score;
        Extends = anExtends;
    }

    @Override
    public String toString() {
        StringBuilder s = new StringBuilder();
        s.append(Location.Chr).append("\t").append(Location.region).append("\t").append(SeqTitle).append("\t").append(Score).append("\t").append(Location.Orientation);
        for (String Extend : Extends) {
            s.append("\t").append(Extend);
        }
        return s.toString();
    }


    public static BedpeItem ToBedpe(BedItem a, BedItem b) {
        InterAction r = new InterAction(a.Location, b.Location);
        String[] ext = new String[a.Extends.length + b.Extends.length];
        System.arraycopy(a.Extends, 0, ext, 0, a.Extends.length);
        System.arraycopy(b.Extends, a.Extends.length, ext, a.Extends.length, b.Extends.length);
        return new BedpeItem(a.SeqTitle, r, a.Score, ext);
    }

    public ChrRegion getLocation() {
        return Location;
    }

    public static class LocationComparator implements Comparator<BedItem> {

        @Override
        public int compare(BedItem o1, BedItem o2) {
            return o1.Location.compareTo(o2.Location);
        }
    }

    public static class TitleComparator implements Comparator<BedItem> {

        @Override
        public int compare(BedItem o1, BedItem o2) {
            return o1.SeqTitle.compareTo(o2.SeqTitle);
        }
    }
}
