package Unit;

public class BreakPoint {
    private String Name;
    private ChrRegion Start, End;
    private double PValue;
    private int Resolution;

    public BreakPoint(String name, ChrRegion start, ChrRegion end, double pvalue, int resolution) {
        Name = name;
        Start = start;
        End = end;
        PValue = pvalue;
        Resolution = resolution;
    }

    public BreakPoint(ChrRegion start, ChrRegion end, int resolution) {
        this("P0", start, end, 0, resolution);
    }

    public BreakPoint(ChrRegion start, ChrRegion end, double pvalue, int resolution) {
        this("P0", start, end, pvalue, resolution);
    }

    @Override
    public String toString() {
        return Name + "\t" + Start + "\t" + End + "\t" + PValue + "\t" + (Resolution / 1000) + "k";
    }
}
