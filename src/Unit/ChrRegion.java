package Unit;

/**
 * Created by snowf on 2019/2/17.
 */
public class ChrRegion implements Comparable<ChrRegion> {
    //    public String Name;
    public String Chr;
    public Region region;
    public char Orientation = '+';
//    public boolean SortByName = true;

    public ChrRegion(String[] s) {
        Chr = s[0];
        region = new Region(Integer.parseInt(s[1]), Integer.parseInt(s[2]));
    }

    public ChrRegion(String s, int left, int right) {
        Chr = s;
        region = new Region(left, right);
    }

    public ChrRegion(String s, int left, int right, char orientation) {
        this(s, left, right);
        Orientation = orientation;
    }

    public boolean IsOverlap(ChrRegion reg) {
        return this.Chr.equals(reg.Chr) && region.IsOverlap(reg.region);
    }

    public boolean IsBelong(ChrRegion reg) {
        return this.Chr.equals(reg.Chr) && region.IsBelong(reg.region);
    }

    public boolean IsContain(ChrRegion reg) {
        return this.Chr.equals(reg.Chr) && region.IsContain(reg.region);
    }

    public int Distance(ChrRegion b) {
        if (Chr.compareToIgnoreCase(b.Chr) != 0) {
            return -1;
        }
        return region.Distance(b.region);
    }

    @Override
    public int compareTo(ChrRegion o) {
        int res = Chr.compareTo(o.Chr);
        if (res == 0) {
            return region.compareTo(o.region);
        } else {
            return res;
        }
    }

    public String toString() {
        return Chr + "\t" + region.Start + "\t" + region.End;
    }
}
