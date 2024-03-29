package TLD;

import File.BedPeFile.*;
import TLD.Tools;
import Unit.*;
import org.apache.commons.cli.*;

import java.io.*;
import java.util.*;

public class PetCluster {
    private String OutPrefix;
    private int Length1, Length2;
    private BedpeFile InFile;
    private ArrayList<InterAction> List = new ArrayList<>();
    private ArrayList<InterAction> Cluster = new ArrayList<>();
    private Hashtable<Integer, Integer> CountStat = new Hashtable<>();
    float CutOff = 0.2f;
    private int TotalCount;
    private int Threads;
    private HashMap<String, int[]> ChrMatrixCount = new HashMap<>();
    public boolean Sort = false;

    public PetCluster(BedpeFile bedpefile, String outPrefix, int length1, int length2, int threads) {
        InFile = bedpefile;
        OutPrefix = outPrefix;
        Length1 = length1;
        Length2 = length2;
        Threads = threads;
    }

    public PetCluster(ArrayList<InterAction> list, int length1, int length2, int threads) {
        List = list;
        Length1 = length1;
        Length2 = length2;
        Threads = threads;
    }

    private PetCluster(String[] args) throws ParseException {
        Options Argument = new Options();
        Argument.addOption(Option.builder("f").argName("file").hasArg().required().desc("[required] bedpe file").build());
        Argument.addOption(Option.builder("l").argName("int").hasArgs().desc("extend length (default 0, should set when interaction site is a point)").build());
        Argument.addOption(Option.builder("p").longOpt("pre").argName("string").hasArg().desc("out prefix (include path)").build());
        Argument.addOption(Option.builder("t").longOpt("thread").argName("int").hasArg().desc("run threads").build());
        Argument.addOption(Option.builder("c").longOpt("cutoff").argName("float").hasArg().desc("cut off").build());
        Argument.addOption(Option.builder("sort").hasArg(false).desc("if do sort before pet cluster").build());
        if (args.length == 0) {
            new HelpFormatter().printHelp("java -cp " + Opts.JarFile.getName() + " " + PetCluster.class.getName() + " <-f file> [option]", Argument);
            System.exit(1);
        }
        CommandLine ComLine = new DefaultParser().parse(Argument, args);
        InFile = new BedpeFile(ComLine.getOptionValue("f"));
        OutPrefix = ComLine.hasOption("p") ? ComLine.getOptionValue("p") : InFile.getPath();
        Threads = ComLine.hasOption("t") ? Integer.parseInt(ComLine.getOptionValue("t")) : 1;
        CutOff = ComLine.hasOption("c") ? Float.parseFloat(ComLine.getOptionValue("c")) : CutOff;
        Sort = ComLine.hasOption("sort");
        int[] temp_int = Opts.GetIntOpts(ComLine, "l", new int[]{0, 0});
        Length1 = temp_int[0];
        if (temp_int.length >= 2) {
            Length2 = temp_int[1];
        } else {
            Length2 = Length1;
        }
    }


    public static void main(String[] args) throws IOException, ParseException, InterruptedException {
        PetCluster pet = new PetCluster(args);
        pet.Run();
        pet.WriteOut();
    }

    public ArrayList<InterAction> Run() throws IOException {
//        System.out.println("Length1=" + Length1 + "\tLength2=" + Length2);
        Hashtable<String, ArrayList<int[]>> ChrMatrix = new Hashtable<>();
        if (InFile != null) {
            BufferedReader in = new BufferedReader(new FileReader(InFile));
            int[] ChrIndex = new int[0], RegionIndex = new int[0];
            int MaxLength = 0, CountIndex = 0;
            switch (InFile.BedpeDetect()) {
                case BedpeRegionFormat:
                    ChrIndex = new int[]{0, 3};
                    RegionIndex = new int[]{1, 2, 4, 5};
                    CountIndex = 6;
                    MaxLength = 7;
                    break;
                default:
                    System.err.println(PetCluster.class.getName() + ":\tError format !");
                    System.exit(1);
            }
            //---------------------对每两条染色体的交创建一个cluster列表--------------------
            String Line;
            InFile.ItemNum = 0;
            while ((Line = in.readLine()) != null) {
                InFile.ItemNum++;
                String[] str = Line.split("\\s+");
                ChrRegion Chr1 = new ChrRegion(str[ChrIndex[0]], Integer.parseInt(str[RegionIndex[0]]), Integer.parseInt(str[RegionIndex[1]]));
                ChrRegion Chr2 = new ChrRegion(str[ChrIndex[1]], Integer.parseInt(str[RegionIndex[2]]), Integer.parseInt(str[RegionIndex[3]]));
                if (Chr1.compareTo(Chr2) > 0) {
                    ChrRegion TempRegion = Chr1;
                    Chr1 = Chr2;
                    Chr2 = TempRegion;
                }
                String key = Chr1.Chr + "-" + Chr2.Chr;
                int count = 1;
                if (!ChrMatrix.containsKey(key)) {
                    ChrMatrix.put(key, new ArrayList<>());
                    ChrMatrixCount.put(key, new int[]{0});
                }
                if (str.length >= MaxLength) {
                    try {
                        count = Integer.parseInt(str[CountIndex]);
                    } catch (NumberFormatException ignored) {
                    }
                }
                ChrMatrix.get(key).add(new int[]{Chr1.region.Start - Length1, Chr1.region.End + Length1, Chr2.region.Start - Length2, Chr2.region.End + Length2, count});
                ChrMatrixCount.get(key)[0]++;
            }
            in.close();
        } else if (List.size() > 0) {
            for (InterAction a : List) {
                ChrRegion region1 = a.getLeft();
                ChrRegion region2 = a.getRight();
                int count = a.Score <= 0 ? 1 : a.Score;
                String key = region1.Chr + "-" + region2.Chr;
                if (!ChrMatrix.containsKey(key)) {
                    ChrMatrix.put(key, new ArrayList<>());
                }
                ChrMatrix.get(key).add(new int[]{region1.region.Start - Length1, region1.region.End + Length1, region2.region.Start - Length2, region2.region.End + Length2, count});
            }
        } else {
            return Cluster;
        }
        //-----------------------------------------------------------------------
        Set<String> InterList = ChrMatrix.keySet();
        Iterator<String> p = InterList.iterator();
        Thread[] t = new Thread[Threads];
        for (int i = 0; i < t.length; i++) {
            t[i] = new Thread(new Runnable() {
                @Override
                public void run() {
                    String chrinter;
                    while (true) {
                        synchronized (t) {
                            if (p.hasNext()) {
                                chrinter = p.next();
                            } else {
                                break;
                            }
                        }
                        String chr1 = chrinter.split("-")[0];
                        String chr2 = chrinter.split("-")[1];
                        if (Sort) {
//                            System.out.println("Sort interaction: " + chrinter);
                            ChrMatrix.get(chrinter).sort(new ClusterComparator());
                        }
                        ArrayList<int[]> cluster = FindCluster(ChrMatrix.get(chrinter));
                        synchronized (t) {
                            for (int[] aCluster : cluster) {
                                Cluster.add(new InterAction(new ChrRegion(chr1, aCluster[0] + Length1, aCluster[1] - Length1), new ChrRegion(chr2, aCluster[2] + Length2, aCluster[3] - Length2), aCluster[4]));
                            }
                        }
                    }

                }
            });
            t[i].start();
        }
        Tools.ThreadsWait(t);
        return Cluster;
    }

    public void WriteOut() throws IOException {
        BufferedWriter out = new BufferedWriter(new FileWriter(OutPrefix + ".cluster"));
        for (InterAction action : Cluster) {
            ChrRegion chr1 = action.getLeft();
            ChrRegion chr2 = action.getRight();
            out.write(chr1.Chr + "\t" + chr1.region.Start + "\t" + chr1.region.End + "\t" + chr2.Chr + "\t" + chr2.region.Start + "\t" + chr2.region.End + "\t" + action.Score + "\n");
        }
        out.close();
        out = new BufferedWriter(new FileWriter(OutPrefix + ".cluster.stat"));
        ArrayList<Integer> Key = new ArrayList<>(CountStat.keySet());
        Collections.sort(Key);
        for (int i : Key) {
            out.write(i + ":\t" + CountStat.get(i) + "\n");
        }
        out.close();
    }

    private ArrayList<int[]> FindCluster(ArrayList<int[]> Region) {
        int OldSize = 0;
        int NewSize = Region.size();
        int Flag = -1;
        while (OldSize != NewSize) {
            OldSize = NewSize;
            for (int i = 0; i < Region.size(); i++) {
                if (Region.get(i)[0] == Flag) {
                    continue;
                }
                int j = i + 1;
                while (j < Region.size()) {
                    if (Region.get(j)[0] != Flag) {
                        if (Region.get(i)[1] < Region.get(j)[0]) {
                            break;
                        }
                        int LMin = Region.get(i)[0];
                        int LMax = Math.max(Region.get(i)[1], Region.get(j)[1]);
                        int RMin = Math.min(Region.get(i)[2], Region.get(j)[2]);
                        int RMax = Math.max(Region.get(i)[3], Region.get(j)[3]);
                        if ((RMax - RMin) <= (Region.get(i)[3] + Region.get(j)[3] - Region.get(i)[2] - Region.get(j)[2])) {
                            Region.set(i, new int[]{LMin, LMax, RMin, RMax, Region.get(i)[4] + Region.get(j)[4]});
                            Region.set(j, new int[]{Flag});
                            NewSize--;
                        }
                    }
                    j++;
                }
            }
        }
        TotalCount = 0;
        ArrayList<int[]> Cluster = new ArrayList<>();
        for (int[] item : Region) {
            if (item[0] != Flag) {
                Cluster.add(item);
                CountStat.put(item[4], CountStat.getOrDefault(item[4], 0) + 1);
                TotalCount += item[4];
            }
        }
        return Cluster;
    }

    public ArrayList<InterAction> getCluster() {
        return Cluster;
    }

    public Hashtable<Integer, Integer> getCountStat() {
        return CountStat;
    }

    public static class ClusterComparator implements Comparator<int[]> {

        @Override
        public int compare(int[] o1, int[] o2) {
            if (o1[0] == o2[0]) {
                if (o1[1] == o2[1]) {
                    if (o1[2] == o2[2]) {
                        if (o1[3] == o2[3]) {
                            return 0;
                        } else {
                            return o1[3] - o2[3];
                        }
                    } else {
                        return o1[2] - o2[2];
                    }
                } else {
                    return o1[1] - o2[1];
                }
            } else {
                return o1[0] - o2[0];
            }
        }
    }

    public HashMap<String, int[]> getChrMatrixCount() {
        return ChrMatrixCount;
    }
}
