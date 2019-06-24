package File.GffFile;

import File.AbstractFile;
import Unit.ChrRegion;
import Unit.Gene;
import Unit.SortItem;
import Unit.Transcript;

import java.io.IOException;
import java.util.ArrayList;

/**
 * Created by snowf on 2019/5/4.
 */

public class GffFile extends AbstractFile<Gene> {
    public GffFile(String pathname) {
        super(pathname);
    }

    public static void main(String[] args) throws IOException {
        GffFile file = new GffFile("GRCh37.p13_genomic.gff");
        Gene gene;
        file.ReadOpen();
        ArrayList<Gene> list = new ArrayList<>();
        while ((gene = file.ReadItem()) != null) {
            list.add(gene);
        }
        System.out.println(list.size());
    }

    @Override
    protected Gene ExtractItem(String[] s) {
        Gene Item = null;
        if (s == null || s.length == 0) {
            return null;
        }
        for (String line : s) {
//            String[] columns = line.split("\\s+");
            GffItem item = new GffItem(line);
            if (!item.map.containsKey("Parent")) {
                Item = new Gene(line);
            } else {
                if (Item == null) {
                    Item = new Gene(line);
                }
                boolean flag = false;
                for (int i = 0; i < Item.transcripts.size(); i++) {
                    if (item.map.get("Parent").equals(Item.transcripts.get(i).ID)) {
                        Item.transcripts.get(i).add(new ChrRegion(item.Columns[2], Integer.parseInt(item.Columns[3]), Integer.parseInt(item.Columns[4]), item.Columns[6].charAt(0)));
                        flag = true;
                        break;
                    }
                }
                if (!flag) {
                    Item.transcripts.add(new Transcript(line));
                }
            }
        }
        return Item;
    }

    @Override
    public void WriteItem(Gene item) throws IOException {

    }

    @Override
    protected SortItem<Gene> ExtractSortItem(String[] s) {
        return null;
    }

    @Override
    public synchronized String[] ReadItemLine() throws IOException {
        ArrayList<String> list = new ArrayList<>();
        String line = reader.readLine();
        if (line == null) {
            return null;
        }
        String[] columns = line.split("\\t");
        while (line.matches("^#.*") || columns[2].compareToIgnoreCase("gene") != 0) {
            line = reader.readLine();
            columns = line.split("\\s+");
        }
        list.add(line);
        reader.mark(1000);
        while ((line = reader.readLine()) != null) {
            if (line.matches("^#.*")) {
                continue;
            }
            columns = line.split("\\t");
            if (columns[2].compareToIgnoreCase("gene") == 0) {
                reader.reset();
                break;
            }
            list.add(line);
            reader.mark(1000);
        }
        return list.size() > 0 ? list.toArray(new String[0]) : null;
    }

    /**
     * @apiNote list must been sorted.
     */
    public static Gene Search(ArrayList<Gene> list, ChrRegion item) {
        if (list == null) {
            return null;
        }
        int i = 0, j = list.size() - 1;
        int p = 0;
        Gene tempGene;
        //二分法查找
        while (i < j) {
            p = (i + j) / 2;
            tempGene = list.get(p);
            if (item.IsOverlap(tempGene.GeneRegion)) {
                return tempGene;
            } else {
                if (item.compareTo(tempGene.GeneRegion) > 0) {
                    i = p + 1;
                } else {
                    j = p - 1;
                }
            }
        }
        if (i >= j) {
            p = i;
        }
        int MinLen = Integer.MAX_VALUE, MinIndex = p;
        for (int k = p - 1; k <= p + 1; k++) {
            if (k >= 0 && k < list.size()) {
                tempGene = list.get(k);
                if (tempGene.GeneRegion.IsOverlap(item)) {
                    return tempGene;
                } else {
                    int len;
                    if (tempGene.GeneRegion.compareTo(item) > 0) {
                        len = tempGene.GeneRegion.region.Start - item.region.End;
                    } else {
                        len = item.region.Start - tempGene.GeneRegion.region.End;
                    }
                    if (len < MinLen) {
                        MinLen = len;
                        MinIndex = k;
                    }
                }
            }
        }
        return list.get(MinIndex);
    }
}
