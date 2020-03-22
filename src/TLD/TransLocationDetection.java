
package TLD;

import File.BedPeFile.*;
import File.MatrixFile.MatrixFile;
import File.MatrixFile.MatrixItem;
import Unit.*;
import com.sun.rowset.internal.Row;
import org.apache.commons.cli.*;
import org.apache.commons.io.FileUtils;
import org.apache.commons.math3.linear.Array2DRowRealMatrix;
import org.apache.commons.math3.linear.RealMatrix;
import org.apache.commons.math3.stat.StatUtils;
import org.apache.commons.math3.stat.inference.ChiSquareTest;
import org.apache.commons.math3.stat.inference.TestUtils;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.List;

/**
 * @author snowf
 * @version 1.0
 */
public class TransLocationDetection {
    private ChrRegion Chr1;
    private ChrRegion Chr2;
    /**
     * Useless, we will support the matrix as input in the future
     */
    private File MatrixFile;
    /**
     * Input file, bedpe format(must be sorted by position)
     */
    private BedpeFile BedpeFile;
    /**
     * Original resolution, and it will decrease when detect precise breakpoint position
     */
    private int Resolution = 100000;
    /**
     * The length(unit bp) need to extension when process "Cluster"
     */
    private int ExtendLength = 10000;
    /**
     * The min distance between two Anchor(cluster). If the distance less than this value, program will merge corresponding anchor to an anchor
     */
    private int MergeLength = 1000000;
    /**
     * Prefix of output file
     */
    private String OutPrefix = "out";
    /**
     * Min count value for each anchor, the anchor which count value less than this value will be filtered.
     */
    private int MinCount = 70;
    /**
     * Min
     */
    private int MinRegionLength = 50000;
    /**
     * Min breakpoint distance, merge two breakpoint which distance less than this value
     */
    private int BreakpointDis = 100000;
    /**
     * The max P-Value of Chi-square, program only remain the breakpoint which Chi-square less than this value
     */
    private double P_Value = 5e-5;//
    /**
     * The version of this program
     */
    public static final float Version = 1.0f;
    /**
     *
     */
    private Chromosome[] Chromosomes;
    /**
     * The output directory
     */
    private File OutDir = new File("./Trans");
    /**
     * The number of threads' you want to used
     */
    private int Threads = 1;
    /**
     *
     */
    private File ChrSizeFile;

    private double AverageDensity;

    private boolean Sort = false;

    /**
     * @param args
     * @throws ParseException
     * @throws IOException
     * @throws InterruptedException
     */

    //private PoissonDistribution BackgroundDistribution = new PoissonDistribution(1);
//    static {
//        Unit.System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
//    }
    public static void main(String[] args) throws ParseException, IOException, InterruptedException {
//        MatrixFile file = new MatrixFile("test.r10.50.0k-P0.5.0k.dense.matrix");
//        file.ReadOpen();
//        MatrixItem item = file.ReadItem();
//        new TransLocationDetection().BreakPointDetection(item.item, 2);


        TransLocationDetection Tld = new TransLocationDetection(args);
        Tld.Run();
    }

    public TransLocationDetection() {
    }

    public TransLocationDetection(File outDir, String prefix, BedpeFile bedpefile, Chromosome[] chromosomes, int resolution, int threads) {
        BedpeFile = bedpefile;
        OutDir = outDir;
        OutPrefix = prefix;
        Chromosomes = chromosomes;
        Resolution = resolution;
        Threads = threads;
    }

    public TransLocationDetection(File outDir, String prefix, BedpeFile bedpefile, Chromosome[] chromosomes, int resolution, int extendLength, int minCount, int threads) {
        this(outDir, prefix, bedpefile, chromosomes, resolution, threads);
        ExtendLength = extendLength;
        MinCount = minCount;
    }

    private TransLocationDetection(ChrRegion chr1, ChrRegion chr2, File matrixfile, BedpeFile bedpefile, String prefix) {
        Chr1 = chr1;
        Chr2 = chr2;
        MatrixFile = matrixfile;
        BedpeFile = bedpefile;
        OutPrefix = prefix;
    }

    public TransLocationDetection(ChrRegion chr1, ChrRegion chr2, File matrixfile, BedpeFile bedpefile, int Resolution, String prefix) {
        this(chr1, chr2, matrixfile, bedpefile, prefix);
        this.setResolution(Resolution);
    }

    private TransLocationDetection(String[] args) throws IOException, InterruptedException {
        Options argument = new Options();
//        argument.addOption(Option.builder("chr").hasArgs().argName("name:start").desc("Chromosome name and region (such as chr1:100)(useless at present)").build());
//        argument.addOption(Option.builder("r").longOpt("res").hasArg().argName("int").desc("Resolution (useless at present)").build());
//        argument.addOption(Option.builder("m").longOpt("matrix").argName("file").hasArg().desc("Inter action matrix").build());
        argument.addOption(Option.builder("f").required().longOpt("bedpe").hasArg().argName("string").required().desc("Interaction bedpe file").build());
        argument.addOption(Option.builder("minc").hasArg().argName("int").desc("min cluster count (default 70)").build());
        argument.addOption(Option.builder("minl").hasArg().argName("int").desc("min region distance (default 5k)").build());
        argument.addOption(Option.builder("sort").hasArg(false).desc("if your input file don't sort before, add this argument").build());
        argument.addOption(Argument.PREFIX);
        argument.addOption(Option.builder("l").hasArg().argName("int").desc("extend length (defalut 10k)").build());
        argument.addOption(Argument.THREAD);
        argument.addOption(Option.builder("s").hasArg().argName("file").desc("chr size file").required().build());
        argument.addOption(Option.builder("ml").hasArg().argName("int").desc("cluster merge length (defalut 1M)").build());
        argument.addOption(Option.builder("bd").hasArg().argName("int").desc("min breakpoint distance, if two breakpoints distance less than this value, it will be merged (defalut 100k)").build());
        argument.addOption(Option.builder("pv").hasArg().argName("float").desc("P value (default 5e-5)").build());
        argument.addOption(Argument.OUTPATH);
        final String Helpheader = "Version: " + Version;
        final String Helpfooter = "";
        if (args.length == 0) {
            new HelpFormatter().printHelp("java -cp path/" + Opts.JarFile.getName() + " " + TransLocationDetection.class.getName(), Helpheader, argument, Helpfooter, true);
            System.exit(1);
        }
        CommandLine Comline = null;
        try {
            Comline = new DefaultParser().parse(argument, args);
        } catch (ParseException e) {
            System.out.println(e.getMessage());
            new HelpFormatter().printHelp("java -cp path/" + Opts.JarFile.getName() + " " + TransLocationDetection.class.getName(), Helpheader, argument, Helpfooter, true);
            System.exit(1);
        }
//        ChrRegion chr1=null, chr2=null;
        if (Comline.hasOption("chr")) {
            String[] s = Comline.getOptionValues("chr");
            Chr1 = new ChrRegion(s[0].split(":")[0], Integer.parseInt(s[0].split(":")[1]), Integer.parseInt(s[0].split(":")[1]));
            if (s.length > 1) {
                Chr2 = new ChrRegion(s[1].split(":")[0], Integer.parseInt(s[1].split(":")[1]), Integer.parseInt(s[1].split(":")[1]));
            } else {
                Chr2 = Chr1;
            }
        }
//        Resolution = Integer.parseInt(Comline.getOptionValue("r"));
        BedpeFile = new BedpeFile(Comline.getOptionValue("f"));
        OutPrefix = Opts.GetStringOpt(Comline, Argument.PREFIX.getOpt(), "test");
        ExtendLength = Opts.GetIntOpt(Comline, "l", ExtendLength);
        Threads = Opts.GetIntOpt(Comline, Argument.THREAD.getOpt(), 1);
        ChrSizeFile = Opts.GetFileOpt(Comline, "s", null);
        MinCount = Opts.GetIntOpt(Comline, "minc", MinCount);
        MinRegionLength = Opts.GetIntOpt(Comline, "minl", MinRegionLength);
        MergeLength = Opts.GetIntOpt(Comline, "ml", MergeLength);
        BreakpointDis = Opts.GetIntOpt(Comline, "bd", BreakpointDis);
        OutDir = Opts.GetFileOpt(Comline, Argument.OUTPATH.getOpt(), OutDir);
        P_Value = Opts.GetFloatOpt(Comline, "pv", (float) P_Value);
        Sort = Comline.hasOption("sort");
//        double[][] data = FileTool.ReadMatrixFile(MatrixFile);
    }

    private void Init() throws IOException {
        if (Chromosomes == null && Opts.ChrSize.keySet().size() == 0) {
            if (ChrSizeFile == null || !ChrSizeFile.isFile()) {
                System.err.println(new Date() + "\tWarning! no chromosome size, there are some matrix won't create");
            } else {
                Opts.ChrSize = Tools.ExtractChrSize(ChrSizeFile);
                System.out.println(Opts.ChrSize.toString());
            }
        }
        if (!OutDir.isDirectory()) {
            OutDir.mkdirs();
        }
    }

    /***
     * Cluster+ 图像识别算法
     *
     * @throws IOException
     */
    public void Run() throws IOException {
        Init();// initialize variables
        //create list
        ArrayList<String> TransLocationRegionPrefix = new ArrayList<>();
        ArrayList<InterAction> TransLocationRegionList = new ArrayList<>();
        ArrayList<InterAction> TransLocationPointList = new ArrayList<>();
        ArrayList<Integer> RegionResolutionList = new ArrayList<>();
        ArrayList<Integer> PointResolutionList = new ArrayList<>();
        ArrayList<String> BreakPointPrefixList = new ArrayList<>();
        Hashtable<String, String[]> ChrMatrixPrefix = new Hashtable<>();
        ArrayList<String> QList = new ArrayList<>();
        PetCluster pet = new PetCluster(BedpeFile, OutDir + "/" + OutPrefix, ExtendLength, ExtendLength, Threads);//create a "PetCluster" class,the interaction which have overlap will be merged
        //-----------------------------------------Cluster--------------------------------------------------------------
        System.out.println(new Date() + "\tStart cluster, ExtendLength=" + ExtendLength + " Threads=" + Threads);
        pet.Sort = Sort;
        ArrayList<InterAction> TempInteractionList = pet.Run();//execute the "cluster" process
        ArrayList<InterAction> TempInteractionList1 = new ArrayList<>();// a temporary list save the merged interaction pet
        pet.WriteOut();// output the cluster result and statistic information
        System.out.println(new Date() + "\tCluster end, there are " + TempInteractionList.size() + " clusters obtain");
        //---------------------calculate average density------------------------
        long ChrSquare = 0;
        double ChrCount = 0;
        ArrayList<String> key = new ArrayList<>(Opts.ChrSize.keySet());
        for (int i = 0; i < key.size() - 1; i++) {
            for (int j = i + 1; j < key.size(); j++) {
                ChrSquare += Opts.ChrSize.get(key.get(i)) / 1000 / 1000 * Opts.ChrSize.get(key.get(j));
                if (pet.getChrMatrixCount().containsKey(key.get(i) + "-" + key.get(j))) {
                    ChrCount += pet.getChrMatrixCount().get(key.get(i) + "-" + key.get(j))[0];
                } else if (pet.getChrMatrixCount().containsKey(key.get(j) + "-" + key.get(i))) {
                    ChrCount += pet.getChrMatrixCount().get(key.get(j) + "-" + key.get(i))[0];
                }
            }
        }
        AverageDensity = ChrCount / ChrSquare;
        System.out.println(new Date() + "\tAverage density is " + ChrCount + "/" + ChrSquare + " = " + AverageDensity + "/kb2");
        //--------------------merge closed cluster----------------------
        System.out.println(new Date() + "\tFilter and merge Cluster\t Min size: " + MinRegionLength + "\tMerge length: " + MergeLength);
        //remove the cluster which have low count value
        for (InterAction a : TempInteractionList) {
            ChrRegion chr1 = a.getLeft();
            ChrRegion chr2 = a.getRight();
            //-------------------------remove the cluster which have low size and prepare the region of used to breakpoint detection------------------------------------
            if ((double) (a.Score) > MinCount && Opts.ChrSize.containsKey(chr1.Chr) && Opts.ChrSize.containsKey(chr2.Chr) && chr1.region.getLength() > MinRegionLength && chr2.region.getLength() > MinRegionLength) {
                TempInteractionList1.add(a);
            }
        }
        System.out.println(new Date() + "\t" + TempInteractionList1.size() + " cluster remained after filter");
        TempInteractionList = new PetCluster(TempInteractionList1, MergeLength, MergeLength, Threads).Run();//execute "cluster" again, but the extend length replace to "MergeLength",so that we can merge some closed cluster
        System.out.println(new Date() + "\tFilter and merge end " + TempInteractionList.size() + " clusters have remained");
        int order = 0;
        File ClusterFile = new File(OutDir + "/" + OutPrefix + ".filtered.cluster");
//        System.out.println(new Date() + "\tRemove low size region: min size is: " + MinRegionLength);
        //====================================构建染色体间的交互矩阵=======================
        BufferedWriter writer = new BufferedWriter(new FileWriter(ClusterFile));
        HashSet<String> temp_set = new HashSet<>();
        ArrayList<Array2DRowRealMatrix> ChrMatrix;
        ArrayList<InterAction> temp_chr_interaction_list = new ArrayList<>();
        for (InterAction action : TempInteractionList) {
            ChrRegion chr1 = action.getLeft();
            ChrRegion chr2 = action.getRight();
            new File(OutDir + "/" + chr1.Chr + "-" + chr2.Chr).mkdirs();
            writer.write("region" + order + "\t" + chr1 + "\t" + chr2 + "\t" + action.Score + "\n");//output the final cluster information
            order++;
            if (!temp_set.contains(chr1.Chr + "-" + chr2.Chr) && Opts.ChrSize.containsKey(chr1.Chr) && Opts.ChrSize.containsKey(chr2.Chr)) {
//                String pre = OutDir + "/" + chr1.Chr + "-" + chr2.Chr + "/" + OutPrefix + "." + chr1.Chr + "-" + chr2.Chr;
                temp_chr_interaction_list.add(new InterAction(new ChrRegion(chr1.Chr, 0, Opts.ChrSize.get(chr1.Chr)), new ChrRegion(chr2.Chr, 0, Opts.ChrSize.get(chr2.Chr))));
//                Tools.PrintMatrix(new CreateMatrix(BedpeFile, Chromosomes, Resolution, null, Threads).Run()), new File(pre + ".dense.matrix"), new File(pre + ".sparse.matrix"));
                temp_set.add(chr1.Chr + "-" + chr2.Chr);
            }
        }
        writer.close();
        ChrMatrix = new CreateMatrix(BedpeFile, Chromosomes, Resolution, null, Threads).Run(temp_chr_interaction_list);
        //====================================绘制出所有的cluster区域===========================
        for (int i = 0; i < temp_chr_interaction_list.size(); i++) {
            InterAction action1 = temp_chr_interaction_list.get(i);
            MatrixItem item = new MatrixItem(ChrMatrix.get(i).getData());
            //----------------------------------------打印矩阵------------------------------------------------
            Tools.PrintMatrix(item.item, new File(OutDir + "/" + action1.getLeft().Chr + "-" + action1.getRight().Chr + "/" + OutPrefix + "." + action1.getLeft().Chr + "-" + action1.getRight().Chr + ".dense.matrix"), new File(OutDir + "/" + action1.getLeft().Chr + "-" + action1.getRight().Chr + "/" + OutPrefix + "." + action1.getLeft().Chr + "-" + action1.getRight().Chr + ".sparse.matrix"));
            System.out.println(new Date() + "\tDraw cluster in " + action1.getLeft().Chr + "-" + action1.getRight().Chr);
            BufferedImage image = item.DrawHeatMap(action1.getLeft().Chr, 0, action1.getRight().Chr, 0, Resolution, 0.999f);
            for (InterAction action2 : TempInteractionList) {
                ChrRegion chr1 = action2.getLeft();
                ChrRegion chr2 = action2.getRight();
                if (action1.getLeft().Chr.equals(chr1.Chr) && action1.getRight().Chr.equals(chr2.Chr)) {
                    InterAction action_temp = new InterAction(new ChrRegion(chr1.Chr, (int) Math.round((double) chr1.region.Start / Resolution * item.getFold()), (int) Math.round((double) chr1.region.End / Resolution * item.getFold())), new ChrRegion(chr2.Chr, (int) Math.round((double) chr2.region.Start / Resolution * item.getFold()), (int) Math.round((double) chr2.region.End / Resolution * item.getFold())));
                    DrawRegion(image, item, action_temp);
                }
            }
            ImageIO.write(image, "png", new File(OutDir + "/" + action1.getLeft().Chr + "-" + action1.getRight().Chr + "/" + OutPrefix + "." + action1.getLeft().Chr + "-" + action1.getRight().Chr + ".cluster.png"));
        }
        ChrMatrix.clear();
        //====================================================================================
        order = 0;
        for (InterAction action : TempInteractionList) {
            ChrRegion chr1 = action.getLeft();
            ChrRegion chr2 = action.getRight();

            //remove some clusters which size is too small(height or weight less than "MinRegionLength")
//            if (Opts.ChrSize.containsKey(chr1.Chr) && Opts.ChrSize.containsKey(chr2.Chr) && chr1.region.getLength() > MinRegionLength && chr2.region.getLength() > MinRegionLength) {
            //create a new region which is triple original size
            TransLocationRegionList.add(new InterAction(new ChrRegion(chr1.Chr, Math.max(0, chr1.region.Start * 2 - chr1.region.End), Math.min(chr1.region.End * 2 - chr1.region.Start, Opts.ChrSize.get(chr1.Chr))), new ChrRegion(chr2.Chr, Math.max(0, chr2.region.Start * 2 - chr2.region.End), Math.min(chr2.region.End * 2 - chr2.region.Start, Opts.ChrSize.get(chr2.Chr))), action.Score));
            //establish corresponding resolution value, resolution will be calculated automatically to ensure the number of bin would large than 20 and less than 200
            RegionResolutionList.add(AutoResolution(TransLocationRegionList.get(TransLocationRegionList.size() - 1)));
            new File(OutDir + "/" + chr1.Chr + "-" + chr2.Chr).mkdirs();
            ChrMatrixPrefix.put(OutDir + "/" + chr1.Chr + "-" + chr2.Chr + "/" + OutPrefix + "." + chr1.Chr + "-" + chr2.Chr, new String[]{chr1.Chr, chr2.Chr});
            TransLocationRegionPrefix.add(OutDir + "/" + chr1.Chr + "-" + chr2.Chr + "/" + OutPrefix + ".r" + order + "." + Tools.UnitTrans(RegionResolutionList.get(RegionResolutionList.size() - 1), "b", "k") + "k");
//            }
            order++;
        }
//        System.out.println(new Date() + "\tEnd Cluster, Cluster number=" + TransLocationRegionList.size());
        //-----------------------------------------创建Cluster的矩阵----------------------------------------------------
        System.out.println(new Date() + "\tStart create interaction matrix, list size is " + TransLocationRegionList.size());
        ArrayList<Array2DRowRealMatrix> MatrixList = new CreateMatrix(BedpeFile, Chromosomes, Resolution, null, Threads).Run(TransLocationRegionList, RegionResolutionList);
        System.out.println(new Date() + "\tEnd create interaction matrix, list size is " + MatrixList.size());
        for (int i = 0; i < TransLocationRegionPrefix.size(); i++) {
            Tools.PrintMatrix(MatrixList.get(i), new File(TransLocationRegionPrefix.get(i) + ".dense.matrix"), new File(TransLocationRegionPrefix.get(i) + ".sparse.matrix"));
        }
        //----------------------------------------
        //---------------------------------------------------------------------------------------------
        ArrayList<BreakPoint> BreakPointList = new ArrayList<>();
        Thread[] t = new Thread[Threads];//创建多个线程
        for (int i = 0; i < t.length; i++) {
            t[i] = new Thread(new Runnable() {
                @Override
                public void run() {
                    while (TransLocationRegionPrefix.size() > 0) {
                        InterAction interAction;
                        ChrRegion chr1, chr2;
                        String prefix;
                        int Resolution;
                        synchronized (t) {
                            try {
                                interAction = TransLocationRegionList.remove(0);
                                chr1 = interAction.getLeft();
                                chr2 = interAction.getRight();
                                prefix = TransLocationRegionPrefix.remove(0);
                                Resolution = RegionResolutionList.remove(0);
                            } catch (IndexOutOfBoundsException e) {
                                break;
                            }
                        }
                        try {
                            if (new File(prefix + ".sparse.matrix").length() > 0) {
                                String ComLine = Opts.Python.Exe() + " " + Opts.OutScriptDir + "/LongCornerDetect.py -i " + prefix + ".dense.matrix" + " -c " + chr1.Chr + ":" + chr1.region.Start + " " + chr2.Chr + ":" + chr2.region.Start + " -r " + Resolution + " -p " + prefix;
                                Opts.CommandOutFile.Append(ComLine + "\n");
                                Tools.ExecuteCommandStr(ComLine, new PrintWriter(System.out), new PrintWriter(System.err));
                                List<String> PointList = FileUtils.readLines(new File(prefix + ".HisD.point"), StandardCharsets.UTF_8);
                                //==========================merge breakpoint=============================
                                System.out.println(new Date() + "\t Merge closed corner points, max distance: " + BreakpointDis);
                                PointList = BreakpointMerge(PointList);
                                //=====================================================================================
                                for (String point : PointList) {
                                    String[] str = point.split("\\s+");
                                    double p_value = Double.parseDouble(str[8]) + Double.parseDouble(str[9]);
                                    if (p_value < P_Value) {
                                        int[] chr1index = new int[]{Integer.parseInt(str[3]), Integer.parseInt(str[4])};
                                        int[] chr2index = new int[]{Integer.parseInt(str[6]), Integer.parseInt(str[7])};
                                        int ExtendL = 5;
                                        if (Resolution < 10000) {
                                            ExtendL = 40;
                                        }
                                        ChrRegion region1 = new ChrRegion((str[2]), chr1index[0] - ExtendL * Resolution, chr1index[1] + ExtendL * Resolution);
                                        ChrRegion region2 = new ChrRegion((str[5]), chr2index[0] - ExtendL * Resolution, chr2index[1] + ExtendL * Resolution);
                                        synchronized (t) {
                                            QList.add(str[10]);
                                            BreakPointList.add(new BreakPoint("P" + BreakPointList.size(), new ChrRegion((str[2]), chr1index[0], chr1index[1] + Resolution), new ChrRegion((str[5]), chr2index[0], chr2index[1] + Resolution), p_value, Resolution));
                                            TransLocationPointList.add(new InterAction(region1, region2));
                                            if (Resolution < 10000) {
                                                PointResolutionList.add(Resolution);
                                            } else {
                                                PointResolutionList.add((int) (Resolution / Math.pow(10D, Math.floor(Math.log10(Resolution)) - 3)));
                                            }
                                            BreakPointPrefixList.add(prefix + "-" + str[0] + "." + (double) PointResolutionList.get(PointResolutionList.size() - 1) / 1000 + "k");
                                        }
                                    }
                                }
                            }
                        } catch (IOException | InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                }
            });
            t[i].start();
        }
        Tools.ThreadsWait(t);
        //--------------------------------------------------------------------------------------------------------------
        System.out.println(new Date() + "\tStart create interaction matrix, list size is " + TransLocationPointList.size());
        MatrixList = new CreateMatrix(BedpeFile, Chromosomes, 0, null, Threads).Run(TransLocationPointList, PointResolutionList);
        System.out.println(new Date() + "\tEnd create interaction matrix, list size is " + TransLocationPointList.size());
//        for (int i = 0; i < BreakPointPrefixList.size(); i++) {
//            Tools.PrintMatrix(MatrixList.get(i), new File(BreakPointPrefixList.get(i) + ".dense.matrix"), new File(BreakPointPrefixList.get(i) + ".sparse.matrix"));
//        }
        int[] Index = new int[]{-1};
        for (int i = 0; i < t.length; i++) {
            ArrayList<Array2DRowRealMatrix> finalMatrixList1 = MatrixList;
            t[i] = new Thread(new Runnable() {
                @Override
                public void run() {
                    while (true) {
                        ChrRegion region1, region2;
                        String prefix, Q;
                        int Resolution, index;
                        Array2DRowRealMatrix Matrix;
                        synchronized (t) {
                            index = ++Index[0];
                            if (index >= BreakPointPrefixList.size()) {
                                break;
                            }
                            Matrix = finalMatrixList1.get(index);
                            InterAction Temp = TransLocationPointList.get(index);
                            region1 = Temp.getLeft();
                            region2 = Temp.getRight();
                            prefix = BreakPointPrefixList.get(index);
                            Q = QList.get(index);
                            Resolution = PointResolutionList.get(index);
                        }
                        try {
                            Tools.PrintMatrix(Matrix, new File(prefix + ".dense.matrix"), new File(prefix + ".sparse.matrix"));
                            MatrixItem item = new MatrixItem(Matrix.getData());
                            BufferedImage image = item.DrawHeatMap(region1.Chr, region1.region.Start, region2.Chr, region2.region.Start, Resolution, 1.0f);
                            int[] BreakPointIndex = BreakPointDetection(Matrix, Integer.parseInt(Q));
                            DrawBreakPoint(image, item, BreakPointIndex);
//                            ImageIO.write(image, "png", new File(prefix + ".breakpoint.ori.png"));
                            double p = CalculatePValue(Matrix, BreakPointIndex, Integer.parseInt(Q));
                            if (p < P_Value) {
                                BreakPoint breakPoint = new BreakPoint("P" + index, new ChrRegion((region1.Chr), region1.region.Start + BreakPointIndex[0] * Resolution, region1.region.Start + (BreakPointIndex[0] + 1) * Resolution), new ChrRegion((region2.Chr), region2.region.Start + BreakPointIndex[1] * Resolution, region2.region.Start + (BreakPointIndex[1] + 1) * Resolution), p, Resolution);
                                BreakPointList.set(index, breakPoint);
                                FileUtils.write(new File(prefix + ".breakpoint"), breakPoint.toString() + "\n", StandardCharsets.UTF_8);
//                                String ComLine = Opts.Python.Exe() + " " + Opts.OutScriptDir + "/RegionPlot.py -i " + prefix + ".dense.matrix -l " + prefix + ".breakpoint -r " + Resolution + " -c " + region1.Chr + ":" + region1.region.Start + " " + region2.Chr + ":" + region2.region.Start + " -o " + prefix + ".breakpoint.png -t point";
//                                Opts.CommandOutFile.Append(ComLine + "\n");
//                                Tools.ExecuteCommandStr(ComLine, new PrintWriter(System.out), new PrintWriter(System.err));
                                ImageIO.write(image, "png", new File(prefix + ".breakpoint.png"));
                            } else {
                                ImageIO.write(image, "png", new File(prefix + ".ori.png"));
                            }
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
            });
            t[i].start();
        }
        Tools.ThreadsWait(t);
        writer = new BufferedWriter(new FileWriter(OutDir + "/" + OutPrefix + ".breakpoint"));
        for (BreakPoint b : BreakPointList) {
            writer.write(b + "\n");
        }
        writer.close();
    }

    public void setP_Value(double p_Value) {
        P_Value = p_Value;
    }

    public void setResolution(int resolution) {
        Resolution = resolution;
    }

    private int AutoResolution(InterAction interAction) {
        int len1 = interAction.getLeft().region.End - interAction.getLeft().region.Start;
        int len2 = interAction.getRight().region.End - interAction.getRight().region.Start;
        return AutoResolution(len1, len2);
    }

    private int AutoResolution(int len1, int len2) {
        double[] resolution_step = new double[]{1e2, 2e2, 5e2, 1e3, 2e3, 5e3, 1e4, 2e4, 5e4, 1e5, 2e5, 5e5, 1e6, 2e6, 5e6};
        for (int i = 0; i < resolution_step.length; i++) {
            int bin_num = (int) Math.min(len1 / resolution_step[i], len2 / resolution_step[i]);
            if (bin_num >= 20 && bin_num <= 200) {
                return (int) resolution_step[i];
            }
        }
        return 100;
    }

    private int AutoResolution(int len1, int len2, int count) {
        double[] resolution_step = new double[]{1e2, 2e2, 5e2, 1e3, 2e3, 5e3, 1e4, 2e4, 5e4, 1e5, 2e5, 5e5, 1e6, 2e6, 5e6};
        for (double res : resolution_step) {
            double bin1 = Math.ceil(len1 / res);
            double bin2 = Math.ceil(len2 / res);
            if (count / bin1 / bin2 > 0.5) {
                return (int) res;
            }
        }
        return (int) 1e7;
    }

    private static int[] BreakPointDetection1(RealMatrix matrix, int quadrant) {
        int[] BreakPointIndex;
        int[] MatrixSize = new int[]{matrix.getRowDimension(), matrix.getColumnDimension()};
        double[] RowSum = new double[MatrixSize[0]], RowVar = new double[MatrixSize[0]];
        double[] ColSum = new double[MatrixSize[0]], ColVar = new double[MatrixSize[0]];
        //--------------------------------------------------------------------------------
        double LeftValue = 0, RightValue = 0, BaseNum = 3;
        double RowMaxValue = 0, RowMaxIndex = 0, RowMinValue = 0, RowMinIndex = 0;
        double ColMaxValue = 0, ColMaxIndex = 0, ColMinValue = 0, ColMinIndex = 0;
        //--------------------------------------------------------------------------------
        for (int i = 0; i < MatrixSize[0]; i++) {
            RowSum[i] = StatUtils.sum(matrix.getRow(i));
        }
        for (int i = 0; i < MatrixSize[1]; i++) {
            ColSum[i] = StatUtils.sum(matrix.getColumn(i));
        }
        //---------------------------------------row---------------------------------
        for (int i = 0; i < MatrixSize[0]; i++) {
            int ExtendLength = Math.min(i, MatrixSize[0] - 1 - i);
            if (ExtendLength <= 0) {
                RowVar[i] = 0;
            } else {
                LeftValue = StatUtils.sum(RowSum, i - ExtendLength, ExtendLength) / ExtendLength;
                RightValue = StatUtils.sum(RowSum, i + 1, ExtendLength) / ExtendLength;
                if (LeftValue + RightValue == 0) {
                    RowVar[i] = 0;
                } else {
//                    RowVar[i] = (LeftValue - RightValue) / (LeftValue + BaseNum) / (RightValue + BaseNum) * (RowSum[i]);
                    RowVar[i] = -Math.log10(new ChiSquareTest().chiSquareTest(new double[]{(LeftValue + RightValue) / 2, (LeftValue + RightValue) / 2}, new long[]{(long) LeftValue, (long) RightValue}));
                }
                if (LeftValue < RightValue) {
                    RowVar[i] = -RowVar[i];
                }
            }
            if (RowVar[i] <= RowMinValue) {
                RowMinValue = RowVar[i];
                RowMinIndex = i;
            }
            if (RowVar[i] >= RowMaxValue) {
                RowMaxValue = RowVar[i];
                RowMaxIndex = i;
            }
        }
        //-----------------------------------------col---------------------------------------
        for (int j = 0; j < MatrixSize[1]; j++) {
            int ExtendLength = Math.min(j, MatrixSize[1] - 1 - j);
            if (ExtendLength <= 0) {
                ColVar[j] = 0;
            } else {
                LeftValue = StatUtils.sum(ColSum, j - ExtendLength, ExtendLength) / ExtendLength;
                RightValue = StatUtils.sum(ColSum, j + 1, ExtendLength) / ExtendLength;
                if (LeftValue + RightValue == 0) {
                    ColVar[j] = 0;
                } else {
//                    ColVar[j] = (LeftValue - RightValue) / (LeftValue + BaseNum) / (RightValue + BaseNum) * (RowSum[j]);
                    ColVar[j] = -Math.log10(new ChiSquareTest().chiSquareTest(new double[]{(LeftValue + RightValue) / 2, (LeftValue + RightValue) / 2}, new long[]{(long) LeftValue, (long) RightValue}));
                }
                if (LeftValue < RightValue) {
                    ColVar[j] = -ColVar[j];
                }
            }
            if (ColVar[j] <= ColMinValue) {
                ColMinValue = ColVar[j];
                ColMinIndex = j;
            }
            if (ColVar[j] >= ColMaxValue) {
                ColMaxValue = ColVar[j];
                ColMaxIndex = j;
            }
        }

        switch (quadrant) {
            case 1:
                BreakPointIndex = new int[]{(int) RowMaxIndex, (int) ColMinIndex};
                break;
            case 2:
                BreakPointIndex = new int[]{(int) RowMaxIndex, (int) ColMaxIndex};
                break;
            case 3:
                BreakPointIndex = new int[]{(int) RowMinIndex, (int) ColMaxIndex};
                break;
            case 4:
                BreakPointIndex = new int[]{(int) RowMinIndex, (int) ColMinIndex};
                break;
            default:
                throw new NullPointerException();
        }
        return BreakPointIndex;

    }

    private int[] BreakPointDetection(RealMatrix matrix, int quadrant) {
        int[] BreakPointIndex;
        int interval = 15;
        int[] MatrixSize = new int[]{matrix.getRowDimension(), matrix.getColumnDimension()};
        double[] RowSum = new double[MatrixSize[0]], RowVar = new double[MatrixSize[0]];
        double[] ColSum = new double[MatrixSize[0]], ColVar = new double[MatrixSize[0]];
        int RowExtendLength = MatrixSize[0] / interval == 0 ? 1 : MatrixSize[0] / interval;
        int ColExtendLength = MatrixSize[1] / interval == 0 ? 1 : MatrixSize[1] / interval;
        int BaseNum = 3;
        double LeftValue = 0, RightValue = 0;
        double RowMaxValue = 0, RowMaxIndex = 0, RowMinValue = 0, RowMinIndex = 0;
        double ColMaxValue = 0, ColMaxIndex = 0, ColMinValue = 0, ColMinIndex = 0;
        //-------------------------------------------------row--------------------------------
        for (int i = 0; i < MatrixSize[0]; i++) {
            RowSum[i] = StatUtils.sum(matrix.getRow(i));
            RowVar[i] = 0;
            if (i < RowExtendLength) {
                LeftValue += RowSum[i];
            } else if (i < RowExtendLength * 2) {
                RightValue += RowSum[i];
            }
        }
        RowVar[RowExtendLength] = CalculateChiSquare(LeftValue, RightValue);
//        RowVar[RowExtendLength] = (LeftValue - RightValue) / (LeftValue + BaseNum) / (RightValue + BaseNum) * (RowSum[RowExtendLength] + 1);
        for (int i = RowExtendLength + 1; i < MatrixSize[0] - RowExtendLength + 1; i++) {
            LeftValue = LeftValue - RowSum[i - RowExtendLength - 1] + RowSum[i - 1];
            RightValue = RightValue - RowSum[i - 1] + RowSum[i + RowExtendLength - 1];
            RowVar[i] = CalculateChiSquare(LeftValue, RightValue);
//            RowVar[i] = (LeftValue - RightValue) / (LeftValue + BaseNum) / (RightValue + BaseNum) * (RowSum[i] + 1);
            if (RowVar[i] > RowMaxValue) {
                RowMaxValue = RowVar[i];
                RowMaxIndex = i;
            } else if (RowVar[i] < RowMinValue) {
                RowMinValue = RowVar[i];
                RowMinIndex = i;
            }
        }
        //---------------------------------------col---------------------------------------
        LeftValue = 0;
        RightValue = 0;
        for (int i = 0; i < MatrixSize[1]; i++) {
            ColSum[i] = StatUtils.sum(matrix.getColumn(i));
            ColVar[i] = 0;
            if (i < ColExtendLength) {
                LeftValue += ColSum[i];
            } else if (i < ColExtendLength * 2) {
                RightValue += ColSum[i];
            }
        }
        ColVar[ColExtendLength] = CalculateChiSquare(LeftValue, RightValue);
//        ColVar[ColExtendLength] = (LeftValue - RightValue) / (LeftValue + BaseNum) / (RightValue + BaseNum) * (ColSum[ColExtendLength] + 1);
        for (int i = ColExtendLength + 1; i < MatrixSize[1] - ColExtendLength + 1; i++) {
            LeftValue = LeftValue - ColSum[i - ColExtendLength - 1] + ColSum[i - 1];
            RightValue = RightValue - ColSum[i - 1] + ColSum[i + ColExtendLength - 1];
            ColVar[i] = CalculateChiSquare(LeftValue, RightValue);
//            ColVar[i] = (LeftValue - RightValue) / (LeftValue + BaseNum) / (RightValue + BaseNum) * (ColSum[i] + 1);
            if (ColVar[i] > ColMaxValue) {
                ColMaxValue = ColVar[i];
                ColMaxIndex = i;
            } else if (ColVar[i] < ColMinValue) {
                ColMinValue = ColVar[i];
                ColMinIndex = i;
            }
        }
        //---------------------------------------------------------------
        switch (quadrant) {
            case 1:
                BreakPointIndex = new int[]{(int) RowMaxIndex - 1, (int) ColMinIndex};
                break;
            case 2:
                BreakPointIndex = new int[]{(int) RowMaxIndex - 1, (int) ColMaxIndex - 1};
                break;
            case 3:
                BreakPointIndex = new int[]{(int) RowMinIndex, (int) ColMaxIndex - 1};
                break;
            case 4:
                BreakPointIndex = new int[]{(int) RowMinIndex, (int) ColMinIndex};
                break;
            default:
                throw new NullPointerException();
        }
        return BreakPointIndex;
    }

    private double CalculateChiSquare(double left, double right) {
        double value = 0;
        if (left + right > 0) {
            value = new ChiSquareTest().chiSquare(new double[]{(left + right) / 2, (left + right) / 2}, new long[]{(long) left, (long) right});
        }
        value = left - right < 0 ? -value : value;
        return value;
    }

    private double[] CalculatePValue(RealMatrix matrix, int[] breakpoint_index) {
        int r = (int) StatUtils.min(new double[]{breakpoint_index[0], breakpoint_index[1], matrix.getRowDimension() - 1 - breakpoint_index[0], matrix.getColumnDimension() - 1 - breakpoint_index[1]});
        double[] sum = new double[]{1, 1, 1, 1};
        for (int i = breakpoint_index[0] - r; i <= breakpoint_index[0] + r; i++) {
            for (int j = breakpoint_index[1] - r; j <= breakpoint_index[1] + r; j++) {
                if (i < breakpoint_index[0]) {
                    if (j < breakpoint_index[1]) {
                        sum[1] += matrix.getEntry(i, j);
                    } else if (j > breakpoint_index[1]) {
                        sum[0] += matrix.getEntry(i, j);
                    }
                } else if (i > breakpoint_index[0]) {
                    if (j < breakpoint_index[1]) {
                        sum[2] += matrix.getEntry(i, j);
                    } else if (j > breakpoint_index[1]) {
                        sum[3] += matrix.getEntry(i, j);
                    }
                }
            }
        }
        double[] p = new double[sum.length];
        for (int i = 0; i < p.length; i++) {
            double sum1 = sum[i % 4], sum2 = sum[(i + 1) % 4];
            p[i] = TestUtils.chiSquareTest(new double[]{(sum1 + sum2) / 2, (sum1 + sum2) / 2}, new long[]{(long) sum1, (long) sum2});
        }
        return p;
    }

    private double CalculatePValue(RealMatrix matrix, int[] breakpoint_index, int quadrant) {
        double[] p = CalculatePValue(matrix, breakpoint_index);
        double P;
        switch (quadrant) {
            case 1:
                P = p[3] + p[0];
                break;
            case 2:
                P = p[0] + p[1];
                break;
            case 3:
                P = p[1] + p[2];
                break;
            case 4:
                P = p[2] + p[3];
                break;
            default:
                return 1;
        }
        return P;
    }

    private List<String> BreakpointMerge(List<String> breakpoint_list) {
        System.out.println(new Date() + "\tMerge closed breakpoint");
        List<Double> P_valueList = new ArrayList<>();
        for (String aPointList : breakpoint_list) {
            P_valueList.add(Double.parseDouble(aPointList.split("\\s+")[8]) + Double.parseDouble(aPointList.split("\\s+")[9]));
        }
        double[][] dis_matrix = new double[breakpoint_list.size()][breakpoint_list.size()];
        for (int j = 0; j < dis_matrix.length; j++) {
            String[] s1 = breakpoint_list.get(j).split("\\s+");
            for (int k = j; k < dis_matrix[j].length; k++) {
                String[] s2 = breakpoint_list.get(k).split("\\s+");
                double d1 = new Region(Integer.parseInt(s1[3]), Integer.parseInt(s1[4])).Distance(new Region(Integer.parseInt(s2[3]), Integer.parseInt(s2[4])));
                double d2 = new Region(Integer.parseInt(s1[6]), Integer.parseInt(s1[7])).Distance(new Region(Integer.parseInt(s2[6]), Integer.parseInt(s2[7])));
                dis_matrix[j][k] = Math.sqrt(d1 * d1 + d2 * d2);
                dis_matrix[k][j] = dis_matrix[j][k];
            }
        }
        //--------------------------------------------------------------------------
        int min_k = 0, min_j = 0;
        while (true) {
            double min_dis = Double.POSITIVE_INFINITY;
            for (int j = 0; j < dis_matrix.length; j++) {
                for (int k = 0; k < dis_matrix.length; k++) {
                    if (dis_matrix[j][k] > 0 && dis_matrix[j][k] < min_dis) {
                        min_dis = dis_matrix[j][k];
                        min_j = j;
                        min_k = k;
                    }
                }
            }
            if (min_dis > BreakpointDis) {
                break;
            }
            int delete_index = P_valueList.get(min_j) <= P_valueList.get(min_k) ? min_k : min_j;
            for (int j = 0; j < dis_matrix.length; j++) {
                dis_matrix[j][delete_index] = -1;
                dis_matrix[delete_index][j] = -1;
            }
        }
        //--------------------------------------------------------------------------
        List<String> PointList1 = new ArrayList<>();
        for (int j = 0; j < dis_matrix.length; j++) {
            if (dis_matrix[j][j] == 0) {
                PointList1.add(breakpoint_list.get(j));
            }
        }
        return PointList1;
    }

    private void DrawBreakPoint(BufferedImage image, MatrixItem item, int[] point_index) {
        int Marginal = item.getMarginal();
        int Fold = item.getFold();
        int matrix_height = item.item.getRowDimension() * Fold;
        int matrix_width = item.item.getColumnDimension() * Fold;
//        int Fold = Math.max((image.getHeight() - Marginal * 2) / item.item.getRowDimension(), 1);
        int[] index = new int[point_index.length];
        for (int i = 0; i < point_index.length; i++) {
            index[i] = point_index[i] * Fold + Fold / 2;
        }
        Graphics2D graphics = image.createGraphics();
        graphics.setColor(Color.GREEN);
        graphics.setStroke(new BasicStroke(2.0f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER));
        graphics.drawLine(Marginal, matrix_height - index[0] + Marginal, matrix_width + Marginal, matrix_height - index[0] + Marginal);
        graphics.drawLine(Marginal + index[1], Marginal, index[1] + Marginal, matrix_height + Marginal);
        graphics.setColor(Color.BLUE);
        graphics.setBackground(Color.BLUE);
        int r = 6;
        graphics.fillOval(index[1] + Marginal - r / 2, matrix_height - index[0] + Marginal - r / 2, r, r);
    }

    private void DrawRegion(BufferedImage image, MatrixItem item, InterAction action) {
        ChrRegion chr1 = action.getLeft();
        ChrRegion chr2 = action.getRight();
        int Marginal = item.getMarginal();
        int Fold = item.getFold();
        int matrix_height = item.item.getRowDimension() * Fold;
        int matrix_width = item.item.getColumnDimension() * Fold;
        Graphics2D graphics = image.createGraphics();
        graphics.setColor(Color.BLACK);
        graphics.setStroke(new BasicStroke(3.0f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER));
        graphics.drawRect(Marginal + chr2.region.Start, Marginal + matrix_height - chr1.region.End, chr2.region.getLength(), chr1.region.getLength());
    }

    private static short[][] FilterLeftTop = new short[][]{
            new short[]{1, 1, 1, 1, 0, -1, -1, -1, -1},
            new short[]{1, 1, 1, 1, 0, -1, -1, -1, -1},
            new short[]{1, 1, 1, 1, 0, -1, -1, -1, -1},
            new short[]{1, 1, 1, 1, 0, -1, -1, -1, -1},
            new short[]{0, 0, 0, 0, 0, 0, 0, 0, 0},
            new short[]{-1, -1, -1, -1, 0, 1, 1, 1, 1},
            new short[]{-1, -1, -1, -1, 0, 1, 1, 1, 1},
            new short[]{-1, -1, -1, -1, 0, 1, 1, 1, 1},
            new short[]{-1, -1, -1, -1, 0, 1, 1, 1, 1}
    };

    private static short[][] FilterRightTop = new short[][]{
            new short[]{-1, -1, -1, -1, 0, 1, 1, 1, 1},
            new short[]{-1, -1, -1, -1, 0, 1, 1, 1, 1},
            new short[]{-1, -1, -1, -1, 0, 1, 1, 1, 1},
            new short[]{-1, -1, -1, -1, 0, 1, 1, 1, 1},
            new short[]{0, 0, 0, 0, 0, 0, 0, 0, 0},
            new short[]{1, 1, 1, 1, 0, -1, -1, -1, -1},
            new short[]{1, 1, 1, 1, 0, -1, -1, -1, -1},
            new short[]{1, 1, 1, 1, 0, -1, -1, -1, -1},
            new short[]{1, 1, 1, 1, 0, -1, -1, -1, -1}
    };

}
