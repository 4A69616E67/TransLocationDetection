package Unit;

/**
 * Created by snowf on 2019/2/17.
 */
public class Region implements Comparable<Region> {
    public int Start;
    public int End;
    private int Length;

    public Region(int start, int end) {
        Start = start;
        End = end;
        Length = end - start;
    }

    public boolean IsOverlap(Region b) {
        return this.Start <= b.End && this.End >= b.Start;
    }

    public boolean IsBelong(Region b) {
        return this.Start >= b.Start && this.End <= b.End;
    }

    public boolean IsContain(int point) {
        return this.Start < point && this.End >= point;
    }

    public boolean IsContain(Region reg) {
        return this.Start <= reg.Start && this.End >= reg.End;
    }

    public int Distance(Region b) {
        return Math.abs((Start + End) / 2 - (b.Start + b.End) / 2);
    }

    @Override

    public int compareTo(Region o) {
        if (this.Start == o.Start) {
            return this.End - o.End;
        } else {
            return this.Start - o.Start;
        }
    }

    public int getLength() {
        return Length;
    }

    @Override
    public String toString() {
        return Start + "\t" + End;
    }
}
