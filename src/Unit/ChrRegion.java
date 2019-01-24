package Unit;

public class ChrRegion implements Comparable<ChrRegion> {
    public String Name;
    public Chromosome Chr;
    public int Begin;
    public int Terminal;
    public char Orientation = '+';
    public int Length;
    public boolean SortByName = true;

    public ChrRegion(String[] s) {
        Chr = new Chromosome(s[0]);
        Begin = Integer.parseInt(s[1]);
        Terminal = Integer.parseInt(s[2]);
        if (s.length >= 4) {
            Orientation = s[3].charAt(0);
        }
        Length = Terminal - Begin;
    }

    public ChrRegion(Chromosome s, int left, int right) {
        Chr = s;
        Begin = left;
        Terminal = right;
        Length = Terminal - Begin;
    }

    public ChrRegion(Chromosome s, int left, int right, char orientation) {
        this(s, left, right);
        Orientation = orientation;
    }

    public boolean IsOverlap(ChrRegion reg) {
        return this.Chr.Name.equals(reg.Chr.Name) && (this.Terminal + reg.Terminal - this.Begin - reg.Begin) >= (Math.max(this.Terminal, reg.Terminal) - Math.min(this.Begin, reg.Begin));
    }

    public boolean IsBelong(ChrRegion reg) {
        return this.Chr.Name.equals(reg.Chr.Name) && (this.Begin >= reg.Begin && this.Terminal <= reg.Terminal);
    }

    public boolean IsContain(ChrRegion reg) {
        return this.Chr.Name.equals(reg.Chr.Name) && (this.Begin <= reg.Begin && this.Terminal >= reg.Terminal);
    }

    @Override
    public int compareTo(ChrRegion o) {
        if (SortByName) {
            return this.Name.compareTo(o.Name);
        } else {
            if (this.Chr.equals(o.Chr)) {
                if (this.Begin == o.Begin) {
                    return this.Terminal - o.Terminal;
                } else {
                    return this.Begin - o.Begin;
                }
            } else {
                return this.Chr.compareTo(o.Chr);
            }
        }
    }

    public String toString() {
        return Chr.Name + "\t" + Begin + "\t" + Terminal;
    }
}
