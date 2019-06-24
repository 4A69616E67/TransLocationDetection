package File.BedFile;


import File.BedPeFile.BedpeItem;
import Unit.ChrRegion;
import Unit.InterAction;

/**
 * Created by snowf on 2019/2/24.
 */

public class BedItem implements Comparable<BedItem> {
    private String SeqTitle;
    private ChrRegion Location;
    private int Score;
    public String[] Extends = new String[0];
    public Sort SortBy = Sort.Location;

    public enum Sort {
        SeqTitle, Location
    }

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


    /**
     * default compare by location
     *
     * @param o another BedItem
     */
    @Override
    public int compareTo(BedItem o) {
        if (SortBy == Sort.SeqTitle) {
            return SeqTitle.compareTo(o.SeqTitle);
        }
        return Location.compareTo(o.Location);
    }

    public BedpeItem ToBedpe(BedItem b) {
        InterAction r = new InterAction(Location, b.Location);
        String[] ext = new String[Extends.length + b.Extends.length];
        System.arraycopy(Extends, 0, ext, 0, Extends.length);
        System.arraycopy(b.Extends, Extends.length, ext, Extends.length, b.Extends.length);
        return new BedpeItem(SeqTitle, r, Score, ext);
    }

    public ChrRegion getLocation() {
        return Location;
    }
}
