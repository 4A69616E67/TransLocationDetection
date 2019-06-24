package Unit;

import File.GffFile.GffItem;

import java.util.ArrayList;

/**
 * Created by snowf on 2019/5/4.
 */

public class Transcript {
    public String ID;
    public String Name;
    public String Parent;
    public ChrRegion TranscriptRegion;
    public ArrayList<ChrRegion> Order = new ArrayList<>();

    public Transcript(GffItem g) {
        TranscriptRegion = new ChrRegion(g.Columns[0], Integer.parseInt(g.Columns[3]), Integer.parseInt(g.Columns[4]), g.Columns[6].charAt(0));
        ID = g.map.get("ID");
        Parent = g.map.get("Parent");
        Name = g.map.get("Name");
    }

    public Transcript(String s) {
        this(new GffItem(s));
    }

    public void add(String[] s) {
        ChrRegion temp = new ChrRegion(s[2], Integer.parseInt(s[3]), Integer.parseInt(s[4]), s[6].charAt(0));
        add(temp);
    }

    public void add(ChrRegion c) {
        boolean flag = false;
        for (int i = 0; i < Order.size(); i++) {
            if (c.region.compareTo(Order.get(i).region) <= 0) {
                Order.add(i, c);
                flag = true;
                break;
            }
        }
        if (!flag) {
            Order.add(c);
        }
    }
//    public Region Enhancer;
//    public Region Promoter;
//    public Region TSS;
//    public ArrayList<Region> Introns;
//    public ArrayList<Region> Exons;
//    public ArrayList<Region> CDS;
//    public Region TTS;
}
