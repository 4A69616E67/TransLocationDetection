package File.BedPeFile;


import File.AbstractItem;
import Unit.ChrRegion;
import Unit.InterAction;

import java.util.Comparator;

/**
 * Created by snowf on 2019/2/24.
 */

public class BedpeItem extends AbstractItem {
    private String SeqTitle;
    private InterAction Location;
    private int Score;
    public String[] Extends = new String[0];
//    public BedItem.Sort SortBy = BedItem.Sort.Location;

    public BedpeItem(String seqTitle, InterAction location, int score, String[] anExtends) {
        SeqTitle = seqTitle;
        Location = location;
        Score = score;
        Extends = anExtends;
    }

    public BedpeItem(String[] s) {
        Location = new InterAction(s);
        if (s.length > 6) {
            SeqTitle = s[6];
        }
        if (s.length > 7) {
            Score = Integer.parseInt(s[7]);
        }
        if (s.length > 9) {
            Location.getLeft().Orientation = s[8].charAt(0);
            Location.getRight().Orientation = s[9].charAt(0);
        }
        if (s.length > 10) {
            Extends = new String[s.length - 10];
            System.arraycopy(s, 10, Extends, 0, Extends.length);
        }
    }

    public static class LocationComparator implements Comparator<BedpeItem> {

        @Override
        public int compare(BedpeItem o1, BedpeItem o2) {
            return o1.Location.compareTo(o2.Location);
        }
    }

    public static class TitleComparator implements Comparator<BedpeItem> {

        @Override
        public int compare(BedpeItem o1, BedpeItem o2) {
            return o1.SeqTitle.compareTo(o2.SeqTitle);
        }
    }
//    @Override
//    public int compareTo(BedpeItem o) {
//        if (SortBy == BedItem.Sort.SeqTitle) {
//            return SeqTitle.compareTo(o.SeqTitle);
//        } else {
//            return Location.compareTo(o.Location);
//        }
//    }

    @Override
    public String toString() {
        StringBuilder s = new StringBuilder();
        ChrRegion left = Location.getLeft();
        ChrRegion right = Location.getRight();
        s.append(left.Chr).append("\t").append(left.region).append("\t").append(right.Chr).append("\t").append(right.region).append("\t").append(SeqTitle).append("\t").append(Score).append("\t").append(left.Orientation).append("\t").append(right.Orientation);
        for (String Extend : Extends) {
            s.append("\t").append(Extend);
        }
        return s.toString();
    }

    public InterAction getLocation() {
        return Location;
    }

    public String getSeqTitle() {
        return SeqTitle;
    }

    public int getScore() {
        return Score;
    }
}
