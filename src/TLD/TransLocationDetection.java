
package TLD;

import File.BedPeFile.*;
import Unit.*;
import org.apache.commons.cli.*;
import org.apache.commons.io.FileUtils;
import org.apache.commons.math3.distribution.PoissonDistribution;
import org.apache.commons.math3.linear.Array2DRowRealMatrix;
import org.apache.commons.math3.linear.RealMatrix;
import org.apache.commons.math3.stat.StatUtils;
import org.apache.commons.math3.stat.inference.TestUtils;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * @author snowf
 * @version 1.0
 */
public class TransLocationDetection {
    private ChrRegion Chr1;
    private ChrRegion Chr2;
    private File MatrixFile;
    private BedpeFile BedpeFile;
    private int Resolution = 100000;
    private int ExtendLength = 10000;
    private int MergeLength = 1000000;
    private String OutPrefix;
    private int MinCount = 70;
    private int MinRegionLength = 50000;
    private double P_Value = 0.05;
    private PoissonDistribution BackgroundDistribution = new PoissonDistribution(1);
    public static final float Version = 1.0f;
    private Chromosome[] Chromosomes;
    private File OutDir = new File("./Trans");
    private int Threads = 1;
    private File ChrSizeFile;

//    static {
//        Unit.System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
//    }

    public static void main(String[] args) throws ParseException, IOException, InterruptedException {
        TransLocationDetection Tld = new TransLocationDetection(args);
        Tld.Run();
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
        Options Arguement = new Options();
        Arguement.addOption(Option.builder("chr").hasArgs().argName("name:start").desc("Chromosome name and region (such as chr1:100)").build());
        Arguement.addOption(Option.builder("r").longOpt("res").hasArg().argName("int").desc("Resolution").build());
        Arguement.addOption(Option.builder("m").longOpt("matrix").argName("file").hasArg().desc("Inter action matrix").build());
        Arguement.addOption(Option.builder("f").required().longOpt("bedpe").hasArg().argName("string").desc("Interaction bedpe file").build());
        Arguement.addOption(Option.builder("minc").hasArg().argName("int").desc("min cluster count").build());
        Arguement.addOption(Option.builder("minl").hasArg().argName("int").desc("min region distant").build());
        Arguement.addOption(Argument.PREFIX);
        Arguement.addOption(Option.builder("l").hasArg().argName("int").desc("extend length").build());
        Arguement.addOption(Argument.THREAD);
        Arguement.addOption(Option.builder("s").hasArg().argName("file").desc("chr size file").build());
        Arguement.addOption(Option.builder("ml").hasArg().argName("int").desc("cluster merge length").build());
        Arguement.addOption(Argument.OUTPATH);
        final String Helpheader = "Version: " + Version;
        final String Helpfooter = "";
        if (args.length == 0) {
            new HelpFormatter().printHelp("java -cp path/" + Opts.JarFile.getName() + " " + TransLocationDetection.class.getName(), Helpheader, Arguement, Helpfooter, true);
            System.exit(1);
        }
        CommandLine Comline = null;
        try {
            Comline = new DefaultParser().parse(Arguement, args);
        } catch (ParseException e) {
            System.out.println(e.getMessage());
            new HelpFormatter().printHelp("java -cp path/" + Opts.JarFile.getName() + " " + TransLocationDetection.class.getName(), Helpheader, Arguement, Helpfooter, true);
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
        OutDir = Opts.GetFileOpt(Comline, Argument.OUTPATH.getOpt(), OutDir);
//        double[][] data = FileTool.ReadMatrixFile(MatrixFile);
    }

    private void Init() throws IOException {
        if (Chromosomes == null && Opts.ChrSize.keySet().size() == 0) {
            if (ChrSizeFile == null || !ChrSizeFile.isFile()) {
                System.err.println(new Date() + "\tWarning! no chromosome size, there are some matrix won't create");
            } else {
                Opts.ChrSize = Tools.ExtractChrSize(ChrSizeFile);
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
        Init();
        //创建列表
        ArrayList<String> TransLocationRegionPrefix = new ArrayList<>();
        ArrayList<InterAction> TransLocationRegionList = new ArrayList<>();
        ArrayList<InterAction> TransLocationPointList = new ArrayList<>();
        ArrayList<Integer> RegionResolutionList = new ArrayList<>();
        ArrayList<Integer> PointResolutionList = new ArrayList<>();
        ArrayList<String> BreakPointPrefixList = new ArrayList<>();
        Hashtable<String, String[]> ChrMatrixPrefix = new Hashtable<>();
        ArrayList<String> QList = new ArrayList<>();
        PetCluster pet = new PetCluster(BedpeFile, OutDir + "/" + OutPrefix, ExtendLength, Threads);
        //-----------------------------------------Cluster--------------------------------------------------------------
        System.out.println(new Date() + "\tStart cluster, ExtendLength=" + ExtendLength + " Threads=" + Threads);
        ArrayList<InterAction> TempInteractionList = pet.Run();
        ArrayList<InterAction> TempInteractionList1 = new ArrayList<>();
        pet.WriteOut();
        //--------------------merge closed cluster----------------------
        System.out.println(new Date() + "\tMerge Cluster");
        for (InterAction a : TempInteractionList) {
            if (a.Score >= MinCount) {
                TempInteractionList1.add(a);
            }
        }
        TempInteractionList = new PetCluster(TempInteractionList1, MergeLength, Threads).Run();
        //-------------------------------------------------------------
        int order = 0;
        File ClusterFile = new File(OutDir + "/" + OutPrefix + ".Count_ge_" + MinCount + ".cluster");
        BufferedWriter writer = new BufferedWriter(new FileWriter(ClusterFile));
        for (int i = 0; i < TempInteractionList.size(); i++) {
            InterAction action = TempInteractionList.get(i);
            if (action.Score >= MinCount) {
                ChrRegion chr1 = action.getLeft();
                ChrRegion chr2 = action.getRight();
                writer.write("region" + order + "\t" + chr1 + "\t" + chr2 + "\t" + action.Score + "\n");
                if (chr1.region.End - chr1.region.Start > MinRegionLength && chr2.region.End - chr2.region.Start > MinRegionLength) {
                    TransLocationRegionList.add(new InterAction(new ChrRegion(chr1.Chr, Math.max(0, chr1.region.Start * 2 - chr1.region.End), Math.min(chr1.region.End * 2 - chr1.region.Start, Opts.ChrSize.get(chr1.Chr))), new ChrRegion(chr2.Chr, Math.max(0, chr2.region.Start * 2 - chr2.region.End), Math.min(chr2.region.End * 2 - chr2.region.Start, Opts.ChrSize.get(chr2.Chr))), action.Score));
                    RegionResolutionList.add(AutoResolution(TransLocationRegionList.get(TransLocationRegionList.size() - 1)));
                    new File(OutDir + "/" + chr1.Chr + "-" + chr2.Chr).mkdirs();
                    ChrMatrixPrefix.put(OutDir + "/" + chr1.Chr + "-" + chr2.Chr + "/" + OutPrefix + "." + chr1.Chr + "-" + chr2.Chr, new String[]{chr1.Chr, chr2.Chr});
                    TransLocationRegionPrefix.add(OutDir + "/" + chr1.Chr + "-" + chr2.Chr + "/" + OutPrefix + ".r" + order + "." + Tools.UnitTrans(RegionResolutionList.get(RegionResolutionList.size() - 1), "b", "k") + "k");
                }
                order++;
            }
        }
        writer.close();
        System.out.println(new Date() + "\tEnd Cluster, Cluster number=" + TransLocationRegionList.size());
        //-----------------------------------------创建Cluster的矩阵----------------------------------------------------
        System.out.println(new Date() + "\tStart create interaction matrix, list size is " + TransLocationRegionList.size());
        ArrayList<Array2DRowRealMatrix> MatrixList = new CreateMatrix(BedpeFile, Chromosomes, Resolution, null, Threads).Run(TransLocationRegionList, RegionResolutionList);
        System.out.println(new Date() + "\tEnd create interaction matrix, list size is " + MatrixList.size());
        for (int i = 0; i < TransLocationRegionPrefix.size(); i++) {
            Tools.PrintMatrix(MatrixList.get(i), new File(TransLocationRegionPrefix.get(i) + ".2d.matrix"), new File(TransLocationRegionPrefix.get(i) + ".spare.matrix"));
        }
        //----------------------------------------
        //----------------------------------------创建整条染色体间的交互矩阵
        if (Opts.ChrSize.size() > 0) {
            ArrayList<InterAction> ChrMatrixRegion = new ArrayList<>();
            Set<String> ChrMatrixList = ChrMatrixPrefix.keySet();
            for (String s : ChrMatrixList) {
                ChrMatrixRegion.add(new InterAction(new ChrRegion((ChrMatrixPrefix.get(s)[0]), 0, Opts.ChrSize.get(ChrMatrixPrefix.get(s)[0])), new ChrRegion((ChrMatrixPrefix.get(s)[1]), 0, Opts.ChrSize.get(ChrMatrixPrefix.get(s)[1]))));
            }
            System.out.println(new Date() + "\tStart create chromosome interaction matrix, list size is " + ChrMatrixRegion.size());
            MatrixList = new CreateMatrix(BedpeFile, Chromosomes, Resolution, null, Threads).Run(ChrMatrixRegion);
            System.out.println(new Date() + "\tEnd create chromosome interaction matrix, list size is " + ChrMatrixRegion.size());
            final int[] j = {0};
            Thread[] t = new Thread[Threads];
            Iterator<String> pt = ChrMatrixList.iterator();
            for (int i = 0; i < t.length; i++) {
                ArrayList<Array2DRowRealMatrix> finalMatrixList = MatrixList;
                t[i] = new Thread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            String p;
                            int index;
                            while (pt.hasNext()) {
                                synchronized (t) {
                                    try {
                                        p = pt.next();
                                        index = j[0];
                                        j[0]++;
                                    } catch (NoSuchElementException e) {
                                        break;
                                    }
                                }
                                Tools.PrintMatrix(finalMatrixList.get(index), new File(p + ".2d.matrix"), new File(p + ".spare.matrix"));
                                String ComLine = Opts.Python.Exe() + " " + Opts.OutScriptDir + "/RegionPlot.py -i " + p + ".2d.matrix -l " + ClusterFile + " -r " + Resolution + " -c " + ChrMatrixPrefix.get(p)[0] + ":0 " + ChrMatrixPrefix.get(p)[1] + ":0" + " -o " + p + ".Trans.pdf -t region";
                                Opts.CommandOutFile.Append(ComLine + "\n");
                                Tools.ExecuteCommandStr(ComLine, new PrintWriter(System.out), new PrintWriter(System.err));
                            }
                        } catch (IOException | InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                });
                t[i].start();
            }
            Tools.ThreadsWait(t);
        }

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
                            if (new File(prefix + ".spare.matrix").length() > 0) {
                                String ComLine = Opts.Python.Exe() + " " + Opts.OutScriptDir + "/LongCornerDetect.py -i " + prefix + ".2d.matrix" + " -c " + chr1.Chr + ":" + chr1.region.Start + " " + chr2.Chr + ":" + chr2.region.Start + " -r " + Resolution + " -p " + prefix;
                                Opts.CommandOutFile.Append(ComLine + "\n");
                                Tools.ExecuteCommandStr(ComLine, new PrintWriter(System.out), new PrintWriter(System.err));
                                List<String> PointList = FileUtils.readLines(new File(prefix + ".HisD.point"), StandardCharsets.UTF_8);
                                for (String point : PointList) {
                                    String[] str = point.split("\\s+");
                                    double p_value = Double.parseDouble(str[8]) + Double.parseDouble(str[9]);
                                    if (p_value < P_Value) {
                                        int[] chr1index = new int[]{Integer.parseInt(str[3]), Integer.parseInt(str[4])};
                                        int[] chr2index = new int[]{Integer.parseInt(str[6]), Integer.parseInt(str[7])};
                                        ChrRegion region1 = new ChrRegion((str[2]), chr1index[0], chr1index[1]);
                                        ChrRegion region2 = new ChrRegion((str[5]), chr2index[0], chr2index[1]);
                                        synchronized (t) {
                                            QList.add(str[10]);
                                            BreakPointList.add(new BreakPoint("P" + BreakPointList.size(), new ChrRegion((str[2]), (chr1index[0] + chr1index[1]) / 2, (chr1index[0] + chr1index[1]) / 2 + Resolution), new ChrRegion((str[5]), (chr2index[0] + chr2index[1]) / 2, (chr2index[0] + chr2index[1]) / 2 + Resolution), p_value, Resolution));
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
        for (int i = 0; i < BreakPointPrefixList.size(); i++) {
            Tools.PrintMatrix(MatrixList.get(i), new File(BreakPointPrefixList.get(i) + ".2d.matrix"), new File(BreakPointPrefixList.get(i) + ".spare.matrix"));
        }
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
                            Tools.PrintMatrix(Matrix, new File(prefix + ".2d.matrix"), new File(prefix + ".spare.matrix"));
                            int[] BreakPointIndex = BreakPointDetection(Matrix, P_Value, Integer.parseInt(Q));
                            double p = CalculatePValue(Matrix, BreakPointIndex, Integer.parseInt(Q));
                            if (p < P_Value) {
                                BreakPoint breakPoint = new BreakPoint("P" + index, new ChrRegion((region1.Chr), region1.region.Start + BreakPointIndex[0] * Resolution, region1.region.Start + (BreakPointIndex[0] + 1) * Resolution), new ChrRegion((region2.Chr), region2.region.Start + BreakPointIndex[1] * Resolution, region2.region.Start + (BreakPointIndex[1] + 1) * Resolution), p, Resolution);
                                BreakPointList.set(index, breakPoint);
                                FileUtils.write(new File(prefix + ".breakpoint"), breakPoint.toString() + "\n", StandardCharsets.UTF_8);
                                String ComLine = Opts.Python.Exe() + " " + Opts.OutScriptDir + "/RegionPlot.py -i " + prefix + ".2d.matrix -l " + prefix + ".breakpoint -r " + Resolution + " -c " + region1.Chr + ":" + region1.region.Start + " " + region2.Chr + ":" + region2.region.Start + " -o " + prefix + ".breakpoint.png -t point";
                                Opts.CommandOutFile.Append(ComLine + "\n");
                                Tools.ExecuteCommandStr(ComLine, new PrintWriter(System.out), new PrintWriter(System.err));
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

    private int[] BreakPointDetection(RealMatrix matrix, double p_value, int quadrant) {
        int[] BreakPointIndex;
        int[] MatrixSize = new int[]{matrix.getRowDimension(), matrix.getColumnDimension()};
        double[] RowSum = new double[MatrixSize[0]], RowVar = new double[MatrixSize[0]];
        double[] ColSum = new double[MatrixSize[0]], ColVar = new double[MatrixSize[0]];
        int RowExtendLength = MatrixSize[0] / 20 == 0 ? 1 : MatrixSize[0] / 20;
        int ColExtendLength = MatrixSize[1] / 20 == 0 ? 1 : MatrixSize[1] / 20;
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
        RowVar[RowExtendLength] = (LeftValue - RightValue) / (LeftValue + BaseNum) / (RightValue + BaseNum) * (RowSum[RowExtendLength] + 1);
        for (int i = RowExtendLength + 1; i < MatrixSize[0] - RowExtendLength + 1; i++) {
            LeftValue = LeftValue - RowSum[i - RowExtendLength - 1] + RowSum[i - 1];
            RightValue = RightValue - RowSum[i - 1] + RowSum[i + RowExtendLength - 1];
            RowVar[i] = (LeftValue - RightValue) / (LeftValue + BaseNum) / (RightValue + BaseNum) * (RowSum[i] + 1);
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
        ColVar[ColExtendLength] = (LeftValue - RightValue) / (LeftValue + BaseNum) / (RightValue + BaseNum) * (ColSum[ColExtendLength] + 1);
        for (int i = ColExtendLength + 1; i < MatrixSize[1] - ColExtendLength + 1; i++) {
            LeftValue = LeftValue - ColSum[i - ColExtendLength - 1] + ColSum[i - 1];
            RightValue = RightValue - ColSum[i - 1] + ColSum[i + ColExtendLength - 1];
            ColVar[i] = (LeftValue - RightValue) / (LeftValue + BaseNum) / (RightValue + BaseNum) * (ColSum[i] + 1);
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

    private double[] CalculatePValue(RealMatrix matrix, int[] breakpoint_index) {
        int r = (int) StatUtils.min(new double[]{breakpoint_index[0], breakpoint_index[1], matrix.getRowDimension() - 1 - breakpoint_index[0], matrix.getColumnDimension() - 1 - breakpoint_index[1]});
        double sum1 = 1, sum2 = 1, sum3 = 1, sum4 = 1;
        for (int i = breakpoint_index[0] - r; i <= breakpoint_index[0] + r; i++) {
            for (int j = breakpoint_index[1] - r; j <= breakpoint_index[1] + r; j++) {
                if (i < breakpoint_index[0]) {
                    if (j < breakpoint_index[1]) {
                        sum2 += matrix.getEntry(i, j);
                    } else if (j > breakpoint_index[1]) {
                        sum1 += matrix.getEntry(i, j);
                    }
                } else if (i > breakpoint_index[0]) {
                    if (j < breakpoint_index[1]) {
                        sum3 += matrix.getEntry(i, j);
                    } else if (j > breakpoint_index[1]) {
                        sum4 += matrix.getEntry(i, j);
                    }
                }
            }
        }
        double p1 = TestUtils.chiSquareTest(new double[]{sum1, sum2}, new long[]{(long) (sum1 + sum2) / 2, (long) (sum1 + sum2) / 2});
        double p2 = TestUtils.chiSquareTest(new double[]{sum2, sum3}, new long[]{(long) (sum2 + sum3) / 2, (long) (sum2 + sum3) / 2});
        double p3 = TestUtils.chiSquareTest(new double[]{sum3, sum4}, new long[]{(long) (sum3 + sum4) / 2, (long) (sum3 + sum4) / 2});
        double p4 = TestUtils.chiSquareTest(new double[]{sum4, sum1}, new long[]{(long) (sum4 + sum1) / 2, (long) (sum4 + sum1) / 2});
        return new double[]{p1, p2, p3, p4};
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

}
