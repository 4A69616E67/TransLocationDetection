package File.BedPeFile;

import File.AbstractFile;
import File.BedFile.BedFile;
import File.BedFile.BedItem;
import File.CommonFile;
import File.GffFile.GffFile;
import TLD.Tools;
import Unit.*;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.util.*;

/**
 * Created by snowf on 2019/2/17.
 */
public class BedpeFile extends AbstractFile<BedpeItem> {
    private AbstractFile.FileFormat Format = AbstractFile.FileFormat.BedpeRegionFormat;
    public BedItem.Sort SortBy = BedItem.Sort.Location;

    public static BedpeFile[] Copy(BedpeFile[] files) {
        BedpeFile[] NewFiles = new BedpeFile[files.length];
        for (int i = 0; i < files.length; i++) {
            NewFiles[i] = new BedpeFile(files[i]);
        }
        return NewFiles;
    }

    public BedpeFile(String pathname) {
        super(pathname);
    }

    public BedpeFile(File f) {
        super(f);
    }

    public BedpeFile(BedpeFile f) {
        super(f);
    }

    protected BedpeItem ExtractItem(String[] s) {
        BedpeItem Item;
        if (s != null) {
            String[] ss = s[0].split("\\s+");
            Item = new BedpeItem(ss);
            Item.SortBy = SortBy;
        } else {
            Item = null;
        }
        return Item;
    }

    @Override
    protected SortItem<BedpeItem> ExtractSortItem(String[] s) {
        BedpeItem Item;
        if (s == null) {
            return null;
        }
        String[] ls = s[0].split("\\s+");
        if (SortBy == BedItem.Sort.SeqTitle) {
            Item = new BedpeItem(ls[3], null, 0, null);
        } else {
            InterAction i = new InterAction(ls);
            Item = new BedpeItem(null, i, 0, null);
            if (ls.length > 9) {
                Item.getLocation().getLeft().Orientation = ls[8].charAt(0);
                Item.getLocation().getRight().Orientation = ls[9].charAt(0);
            }
        }
        Item.SortBy = SortBy;
        return new SortItem<>(Item);
    }

    @Override
    public void WriteItem(BedpeItem item) throws IOException {
        writer.write(item.toString());
    }

    public void SplitSortFile(BedpeFile OutFile) throws IOException {
        int splitItemNum = 5000000;
        this.ItemNum = getItemNum();
        if (this.ItemNum > splitItemNum) {
            splitItemNum = (int) Math.ceil(this.ItemNum / Math.ceil((double) this.ItemNum / splitItemNum));
            ArrayList<CommonFile> TempSplitFile = this.SplitFile(this.getPath(), splitItemNum);
            BedpeFile[] TempSplitSortFile = new BedpeFile[TempSplitFile.size()];
            for (int i = 0; i < TempSplitFile.size(); i++) {
                TempSplitSortFile[i] = new BedpeFile(TempSplitFile.get(i).getPath() + ".sort");
                new BedpeFile(TempSplitFile.get(i).getPath()).SortFile(TempSplitSortFile[i]);
            }
            OutFile.MergeSortFile(TempSplitSortFile);
            if (Configure.DeBugLevel < 1) {
                for (int i = 0; i < TempSplitFile.size(); i++) {
                    AbstractFile.delete(TempSplitFile.get(i));
                    AbstractFile.delete(TempSplitSortFile[i]);
                }
            }
        } else {
            this.SortFile(OutFile);
        }
    }

    public AbstractFile.FileFormat BedpeDetect() throws IOException {//不再支持 BedpePointFormat
        BedpeItem Item = null;
        ReadOpen();
        try {
            Item = ReadItem();
        } catch (IndexOutOfBoundsException | NumberFormatException i) {
            Format = AbstractFile.FileFormat.ErrorFormat;
        }
        if (Item == null) {
            Format = AbstractFile.FileFormat.EmptyFile;
        }
        ReadClose();
        return Format;
    }

    public BedpeFile[] SeparateBedpe(Chromosome[] Chromosome, String Prefix, int Threads) throws IOException {
        System.out.println(new Date() + "\tSeparate Bedpe file\t" + getName());
        ReadOpen();
        BedpeFile[] ChrSameFile = new BedpeFile[Chromosome.length];
        //------------------------------------------------------------
        for (int i = 0; i < Chromosome.length; i++) {
            ChrSameFile[i] = new BedpeFile(Prefix + "." + Chromosome[i].Name + ".same.bedpe");
            ChrSameFile[i].WriteOpen();
        }
        Thread[] t = new Thread[Threads];
        for (int i = 0; i < t.length; i++) {
            t[i] = new Thread(() -> {
                String Line;
                String[] Str;
                try {
                    while ((Line = reader.readLine()) != null) {
                        Str = Line.split("\\s+");
                        for (int j = 0; j < Chromosome.length; j++) {
                            if (Str[0].equals(Chromosome[j].Name)) {
                                synchronized (Chromosome[j]) {
                                    ChrSameFile[j].getWriter().write(Line + "\n");
                                }
                                break;
                            }
                        }
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });
            t[i].start();
        }
        Tools.ThreadsWait(t);
        ReadClose();
        for (int i = 0; i < Chromosome.length; i++) {
            ChrSameFile[i].WriteClose();
        }
        System.out.println(new Date() + "\tEnd separate Bedpe file " + getName());
        return ChrSameFile;
    }

    public void BedToBedpe(BedFile file1, BedFile file2) throws IOException {
        file1.ReadOpen();
        file2.ReadOpen();
        WriteOpen();
        ItemNum = 0;
        BedItem item1 = file1.ReadItem();
        BedItem item2 = file2.ReadItem();
        if (item1 == null || item2 == null) {
            WriteClose();
            return;
        }
        while (item1 != null && item2 != null) {
            item1.SortBy = BedItem.Sort.SeqTitle;
            int res = item1.compareTo(item2);
            if (res == 0) {
                item1.SortBy = BedItem.Sort.Location;
                if (item1.compareTo(item2) > 0) {
                    WriteItemln(item2.ToBedpe(item1));
                } else {
                    WriteItemln(item1.ToBedpe(item2));
                }
                ItemNum++;
                item1 = file1.ReadItem();
                item2 = file2.ReadItem();
            } else if (res > 0) {
                item2 = file2.ReadItem();
            } else {
                item1 = file1.ReadItem();
            }
        }
        file1.ReadClose();
        file2.ReadClose();
        WriteClose();
    }

    public int DistanceCount(int min, int max, int thread) throws IOException {
        if (thread <= 0) {
            thread = 1;
        }
        final int[] Count = {0};
        ReadOpen();
        Thread[] t = new Thread[thread];
        for (int i = 0; i < t.length; i++) {
            t[i] = new Thread(() -> {
                try {
                    String[] Lines;
                    while ((Lines = ReadItemLine()) != null) {
                        InterAction action = new InterAction(Lines[0].split("\\s+"));
                        int dis = action.Distance();
                        if (dis <= max && dis >= min) {
                            synchronized (t) {
                                Count[0]++;
                            }
                        }
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });
            t[i].start();
        }
        Tools.ThreadsWait(t);
        ReadClose();
        return Count[0];
    }

    /**
     * @return {short range, long range}
     */
    public long[] RangeCount(Region ShortRange, Region LongRange, int thread) throws IOException {
        ItemNum = 0;
        thread = thread > 0 ? thread : 1;
        long[] Count = new long[2];
        ReadOpen();
        Thread[] t = new Thread[thread];
        for (int i = 0; i < t.length; i++) {
            t[i] = new Thread(() -> {
                try {
                    BedpeItem item;
                    while ((item = ReadItem()) != null) {
                        if (ShortRange.IsContain(item.getLocation().Distance())) {
                            synchronized (ShortRange) {
                                Count[0]++;
                            }
                        } else if (LongRange.IsContain(item.getLocation().Distance())) {
                            synchronized (LongRange) {
                                Count[1]++;
                            }
                        }
                        ItemNum++;
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });
            t[i].start();
        }
        Tools.ThreadsWait(t);
        ReadClose();
        return Count;
    }

    public HashMap<String, HashMap<String, long[]>> Annotation(GffFile gffFile, BedpeFile outFile, int thread) throws IOException {
        HashMap<String, HashMap<String, long[]>> Stat = new HashMap<>();
        HashMap<String, ArrayList<Gene>> gffList = new HashMap<>();
        HashMap<String, Integer> AttributeMap = new HashMap<>();
        Gene item;
        gffFile.ReadOpen();
        System.out.println(new Date() + "\tCreate index ......");
        BufferedWriter out = outFile.WriteOpen();
        while ((item = gffFile.ReadItem()) != null) {
            if (!gffList.containsKey(item.GeneRegion.Chr)) {
                gffList.put(item.GeneRegion.Chr, new ArrayList<>());
            }
            gffList.get(item.GeneRegion.Chr).add(item);
        }
        for (String key : gffList.keySet()) {
            Collections.sort(gffList.get(key));
        }
        gffFile.ReadClose();
        System.out.println(new Date() + "\tAnnotation begin ......");
        this.ReadOpen();
        Thread[] t = new Thread[thread];
        for (int i = 0; i < t.length; i++) {
            t[i] = new Thread(() -> {
                try {
                    BedpeItem temp;
                    while ((temp = this.ReadItem()) != null) {
                        Gene g1 = GffFile.Search(gffList.get(temp.getLocation().getLeft().Chr), temp.getLocation().getLeft());
                        Gene g2 = GffFile.Search(gffList.get(temp.getLocation().getRight().Chr), temp.getLocation().getRight());
                        String[] extra1 = new String[]{"-"}, extra2 = new String[]{"-"};
                        if (g1 != null) {
                            extra1 = Gene.GeneDistance(g1, temp.getLocation().getLeft());
                        }
                        if (g2 != null) {
                            extra2 = Gene.GeneDistance(g2, temp.getLocation().getRight());
                        }
                        synchronized (t) {
                            if (!Stat.containsKey(extra1[0])) {
                                MapInit(Stat, extra1[0]);
                            }
                            if (!Stat.containsKey(extra2[0])) {
                                MapInit(Stat, extra2[0]);
                            }
                            Stat.get(extra1[0]).get(extra2[0])[0]++;
                            out.write(temp + "\t" + String.join(":", extra1) + "\t" + String.join(":", extra2) + "\n");
                        }
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });
            t[i].start();
        }
        Tools.ThreadsWait(t);
        out.close();
        this.ReadClose();
        System.out.println(new Date() + "\tAnnotation finish");
        return Stat;
    }

    private void MapInit(HashMap<String, HashMap<String, long[]>> map, String key) {
        Set<String> keys = map.keySet();
        map.put(key, new HashMap<>());
        map.get(key).put(key, new long[]{0});
        for (String k : keys) {
            map.get(key).put(k, new long[]{0});
            map.get(k).put(key, new long[]{0});
        }
    }
}

