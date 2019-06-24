package Unit;

import File.GffFile.GffItem;

import java.util.ArrayList;

/**
 * Created by snowf on 2019/5/4.
 */

public class Gene implements Comparable<Gene> {
    public String ID;
    public String Name;
    public ChrRegion GeneRegion;
    public ArrayList<Transcript> transcripts = new ArrayList<>();

    public static final String GENE = "Gene";
    public static final String PROMOTER = "Promoter";
    public static final String INTERGENIC = "Intergenic";
    public static final String INTRON = "Intron";
    public static final String EXON = "Exon";

    public Gene(GffItem g) {
        GeneRegion = new ChrRegion(g.Columns[0], Integer.parseInt(g.Columns[3]), Integer.parseInt(g.Columns[4]), g.Columns[6].charAt(0));
        ID = g.map.get("ID");
        Name = g.map.get("Name");
    }

    public Gene(String s) {
        this(new GffItem(s));
    }

    @Override
    public int compareTo(Gene o) {
        return GeneRegion.compareTo(o.GeneRegion);
    }

    public static String[] GeneDistance(Gene g, ChrRegion c) {
        int dis;
        String[] res = new String[4];
        res[1] = g.Name;
        res[2] = String.valueOf(g.GeneRegion.Orientation);
        if (g.GeneRegion.IsOverlap(c)) {
            res[0] = GENE;
            res[3] = "0";
            boolean flag = false;
            for (Transcript trp : g.transcripts) {
                for (ChrRegion ord : trp.Order) {
                    if (ord.region.IsContain(c.region)) {
                        res[0] = ord.Chr;
                        flag = true;
                        break;
                    }
                }
            }
            if (!flag) {
                res[0] = "Intron";
            }
            return res;
        } else {
            if (g.GeneRegion.compareTo(c) > 0) {
                if (g.GeneRegion.Orientation == '+') {
                    dis = c.region.End - g.GeneRegion.region.Start;
                } else {
                    dis = g.GeneRegion.region.Start - c.region.End;
                }
            } else {
                if (g.GeneRegion.Orientation == '+') {
                    dis = c.region.Start - g.GeneRegion.region.End;
                } else {
                    dis = g.GeneRegion.region.End - c.region.Start;
                }
            }
            res[3] = String.valueOf(dis);
            if (dis <= 0 && dis >= -10000) {
                res[0] = PROMOTER;
            } else {
                res[0] = INTERGENIC;
            }
        }
        return res;
    }
}
