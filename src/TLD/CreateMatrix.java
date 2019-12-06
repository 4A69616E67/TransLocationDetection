package TLD;

import File.BedPeFile.*;
import Unit.*;
import org.apache.commons.cli.*;
import org.apache.commons.io.FileUtils;
import org.apache.commons.math3.linear.Array2DRowRealMatrix;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Date;
import java.util.Hashtable;
import java.util.List;

public class CreateMatrix {
    private BedpeFile BedpeFile;
    private Chromosome[] Chromosomes;
    private ChrRegion Region1;
    private ChrRegion Region2;
    private int Resolution;
    private String Prefix;
    private File TwoDMatrixFile;
    private File SpareMatrixFile;
    private File RegionFile;
    private File BinSizeFile;
    private int Threads;
    private float Version = 1.0f;

    public CreateMatrix(BedpeFile BedpeFile, Chromosome[] Chrs, int Resolution, String Prefix, int Threads) {
        this.BedpeFile = BedpeFile;
        this.Chromosomes = Chrs;
        this.Resolution = Resolution;
        this.Prefix = Prefix;
        this.Threads = Threads;
        Init();
    }

    private CreateMatrix(String[] args) throws IOException {
        Options Argument = new Options();
        Argument.addOption(Option.builder("f").hasArg().argName("file").required().desc("[required] bedpefile").build());
        Argument.addOption(Option.builder("s").hasArg().longOpt("size").argName("file").desc("Chromosomes size file").build());
        Argument.addOption(Option.builder("chr").hasArgs().argName("strings").desc("The chromosome name which you want to calculator").build());
        Argument.addOption(Option.builder("res").hasArg().argName("int").desc("Resolution (default 1M)").build());
        Argument.addOption(Option.builder("region").hasArgs().argName("strings").desc("(sample chr1:0:100 chr4:100:400) region you want to calculator, if not set, will calculator chromosome size").build());
        Argument.addOption(Option.builder("t").hasArg().argName("int").desc("Threads (default 1)").build());
        Argument.addOption(Option.builder("p").hasArg().argName("string").desc("out prefix (default bedpefile)").build());
        final String helpHeader = "Version: " + Version + "\nAuthor: " + Opts.Author;
        final String helpFooter = "Note:\n" +
                "you can set -chr like \"Chr:ChrSize\" or use -s to define the \"ChrSize\"\n" +
                "If you set -s, you can set -chr like \"Chr\"\n" +
                "The file format of option -s is \"Chromosomes    Size\" for each row\n" +
                "We will calculate all chromosome in Chromosomes size file if you don't set -chr\n" +
                "You needn't set -s and -chr if you set -region";
        if (args.length == 0) {
            new HelpFormatter().printHelp("java -cp Path/" + Opts.JarFile.getName() + " " + CreateMatrix.class.getName(), helpHeader, Argument, helpFooter, true);
            System.exit(1);
        }
        CommandLine ComLine = null;
        try {
            ComLine = new DefaultParser().parse(Argument, args);
        } catch (ParseException e) {
            System.err.println(e.getMessage());
            new HelpFormatter().printHelp("java -cp Path/" + Opts.JarFile.getName() + " " + CreateMatrix.class.getName(), helpHeader, Argument, helpFooter, true);
            System.exit(1);
        }
        BedpeFile = new BedpeFile(ComLine.getOptionValue("f"));
        String[] Chr = ComLine.hasOption("chr") ? ComLine.getOptionValues("chr") : null;
        if (Chr != null) {
            Chromosomes = new Chromosome[Chr.length];
            for (int i = 0; i < Chr.length; i++) {
                Chromosomes[i] = new Chromosome(Chr[i].split(":"));
            }
        }
        String SizeFile = ComLine.hasOption("size") ? ComLine.getOptionValue("size") : null;
        Resolution = ComLine.hasOption("res") ? Integer.parseInt(ComLine.getOptionValue("res")) : 100000;
        Prefix = ComLine.hasOption("p") ? ComLine.getOptionValue("p") : BedpeFile.getPath();
        Threads = ComLine.hasOption("t") ? Integer.parseInt(ComLine.getOptionValue("t")) : 1;
        Region1 = ComLine.hasOption("region") ? new ChrRegion(ComLine.getOptionValue("region").split(":")) : null;
        Region2 = ComLine.hasOption("region") && ComLine.getOptionValues("region").length > 1 ? new ChrRegion(ComLine.getOptionValues("region")[1].split(":")) : Region1;
        if (SizeFile != null) {
            List<String> ChrSizeList = FileUtils.readLines(new File(SizeFile), StandardCharsets.UTF_8);
            if (Chromosomes == null) {
                Chromosomes = new Chromosome[ChrSizeList.size()];
                for (int i = 0; i < Chromosomes.length; i++) {
                    Chromosomes[i] = new Chromosome(ChrSizeList.get(i).split("\\s+"));
                }
            } else {
                for (String aChrSizeList : ChrSizeList) {
                    for (Chromosome aChromosome : Chromosomes) {
                        if (aChromosome.Name.equals(aChrSizeList.split("\\s+")[0])) {
                            aChromosome.Size = Integer.parseInt(aChrSizeList.split("\\s+")[1]);
                            break;
                        }
                    }
                }
            }
        }
        Init();
    }

    private void Init() {
        TwoDMatrixFile = new File(Prefix + ".2d.matrix");
        SpareMatrixFile = new File(Prefix + ".spare.matrix");
        RegionFile = new File(Prefix + ".matrix.Region");
        BinSizeFile = new File(Prefix + ".matrix.BinSize");
    }

    public static void main(String[] args) throws IOException {

        new CreateMatrix(args).Run();

    }

    public Array2DRowRealMatrix Run() throws IOException {
        if (Region1 != null) {
            return Run(Region1, Region2);
        }
        if (Chromosomes == null) {
            System.err.println("Error! no -chr  argument");
            System.exit(1);
        }
        int[] ChrSize = new int[Chromosomes.length];
        System.out.println(new Date() + "\tBegin to creat interaction matrix " + BedpeFile.getName() + " Resolution=" + Resolution + " Threads=" + Threads);
        for (int i = 0; i < Chromosomes.length; i++) {
            ChrSize[i] = Chromosomes[i].Size;
        }
        int SumBin = 0;
        int[] ChrBinSize = Tools.CalculateBinSize(ChrSize, Resolution);
        Hashtable<String, Integer> IndexBias = new Hashtable<>();
        //计算bin的总数
        for (int i = 0; i < ChrBinSize.length; i++) {
            IndexBias.put(Chromosomes[i].Name, SumBin);
            SumBin = SumBin + ChrBinSize[i];
        }
        if (SumBin > Opts.MaxBinNum) {
            System.err.println("Error ! too many bins, there are " + SumBin + " bins.");
            System.exit(1);
        }
        double[][] intermatrix = new double[SumBin][SumBin];
        int[] DataIndex = IndexParse(BedpeFile);
        BufferedReader infile = new BufferedReader(new FileReader(BedpeFile));
        Thread[] Process = new Thread[Threads];
        //----------------------------------------------------------------------------
        for (int i = 0; i < Threads; i++) {
            int finalSumBin = SumBin;
            Process[i] = new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        String line;
                        String[] str;
                        while ((line = infile.readLine()) != null) {
                            str = line.split("\\s+");
                            int row = (Integer.parseInt(str[DataIndex[1]]) + Integer.parseInt(str[DataIndex[2]])) / 2 / Resolution;
                            int col = (Integer.parseInt(str[DataIndex[4]]) + Integer.parseInt(str[DataIndex[5]])) / 2 / Resolution;
                            row += IndexBias.get(str[DataIndex[0]]);
                            if (row >= finalSumBin) {
                                continue;
                            }
                            col += IndexBias.get(str[DataIndex[3]]);
                            if (col >= finalSumBin) {
                                continue;
                            }
                            synchronized (Process) {
                                intermatrix[row][col]++;
                                if (row != col) {
                                    intermatrix[col][row]++;
                                }
                            }
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            });
            Process[i].start();
        }
        //-------------------------------------------------
        Tools.ThreadsWait(Process);
        infile.close();
        //--------------------------------------------------------
        //打印矩阵
        Array2DRowRealMatrix InterMatrix = new Array2DRowRealMatrix(intermatrix);
        Tools.PrintMatrix(InterMatrix, TwoDMatrixFile, SpareMatrixFile);
        System.out.println(new Date() + "\tEnd to create interaction matrix");
        //--------------------------------------------------------------------
        int temp = 0;
        BufferedWriter outfile = new BufferedWriter(new FileWriter(BinSizeFile));
        for (int i = 0; i < Chromosomes.length; i++) {
            temp = temp + 1;
            outfile.write(Chromosomes[i].Name + "\t" + temp + "\t");
            temp = temp + ChrBinSize[i] - 1;
            outfile.write(temp + "\n");
        }
        outfile.close();
        return InterMatrix;
    }//OK

    public Array2DRowRealMatrix Run(ChrRegion reg1, ChrRegion reg2) throws IOException {
        System.out.println(new Date() + "\tBegin to creat interaction matrix " + reg1.toString().replace("\t", ":") + " " + reg2.toString().replace("\t", ":"));
        int[] ChrBinSize;
        ChrBinSize = Tools.CalculateBinSize(new int[]{reg1.region.getLength(), reg2.region.getLength()}, Resolution);
        if (Math.max(ChrBinSize[0], ChrBinSize[1]) > Opts.MaxBinNum) {
            System.err.println("Error ! too many bins, there are " + Math.max(ChrBinSize[0], ChrBinSize[1]) + " bins.");
            System.exit(0);
        }
        Array2DRowRealMatrix InterMatrix = new Array2DRowRealMatrix(ChrBinSize[0], ChrBinSize[1]);
        for (int i = 0; i < InterMatrix.getRowDimension(); i++) {
            for (int j = 0; j < InterMatrix.getColumnDimension(); j++) {
                InterMatrix.setEntry(i, j, 0);//数组初始化为0
            }
        }
        BufferedReader infile = new BufferedReader(new FileReader(BedpeFile));
        int[] DataIndex = IndexParse(BedpeFile);
        Thread[] Process = new Thread[Threads];
        //----------------------------------------------------------------------------
        for (int i = 0; i < Threads; i++) {
            Process[i] = new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        String line;
                        String[] str;
                        while ((line = infile.readLine()) != null) {
                            str = line.split("\\s+");
                            ChrRegion left = new ChrRegion(new String[]{str[DataIndex[0]], str[DataIndex[1]], str[DataIndex[2]]});
                            ChrRegion right = new ChrRegion(new String[]{str[DataIndex[3]], str[DataIndex[4]], str[DataIndex[5]]});
                            if (left.IsBelong(reg1) && right.IsBelong(reg2)) {
                                synchronized (InterMatrix) {
                                    InterMatrix.addToEntry((left.region.Start - reg1.region.Start) / Resolution, (right.region.Start - reg2.region.Start) / Resolution, 1);
                                }
                            } else if (right.IsBelong(reg1) && left.IsBelong(reg2)) {
                                synchronized (InterMatrix) {
                                    InterMatrix.addToEntry((right.region.Start - reg1.region.Start) / Resolution, (left.region.Start - reg2.region.Start) / Resolution, 1);
                                }
                            }
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            });
            Process[i].start();
        }
        //-------------------------------------------------
        Tools.ThreadsWait(Process);
        infile.close();
        //--------------------------------------------------------
        //打印矩阵
        Tools.PrintMatrix(InterMatrix, TwoDMatrixFile, SpareMatrixFile);
        System.out.println(new Date() + "\tEnd to creat interaction matrix");
        //--------------------------------------------------------------------
        BufferedWriter outfile = new BufferedWriter(new FileWriter(RegionFile));
        outfile.write(reg1.toString() + "\n");
        outfile.write(reg2.toString() + "\n");
        outfile.close();
        return InterMatrix;
    }

    public ArrayList<Array2DRowRealMatrix> Run(List<InterAction> list) throws IOException {
        //初始化矩阵列表
        ArrayList<Array2DRowRealMatrix> MatrixList = new ArrayList<>();
        for (InterAction aList : list) {
            Array2DRowRealMatrix aMatrix = new Array2DRowRealMatrix((aList.getLeft().region.End - aList.getLeft().region.Start + 1) / Resolution + 1, (aList.getRight().region.End - aList.getRight().region.Start + 1) / Resolution + 1);
            MatrixList.add(aMatrix);
            for (int i = 0; i < aMatrix.getRowDimension(); i++) {
                for (int j = 0; j < aMatrix.getColumnDimension(); j++) {
                    aMatrix.setEntry(i, j, 0);
                }
            }
        }
        BufferedReader reader = new BufferedReader(new FileReader(BedpeFile));
        //多线程构建矩阵
        Thread[] t = new Thread[Threads];
        for (int i = 0; i < t.length; i++) {
            t[i] = new Thread(new Runnable() {
                @Override
                public void run() {
                    String Line;
                    String[] Str;
                    try {
                        int[] DataIndex = IndexParse(new BedpeFile(BedpeFile));
                        while ((Line = reader.readLine()) != null) {
                            Str = Line.split("\\s+");
                            ChrRegion left = new ChrRegion(new String[]{Str[DataIndex[0]], Str[DataIndex[1]], Str[DataIndex[2]]});
                            ChrRegion right = new ChrRegion(new String[]{Str[DataIndex[3]], Str[DataIndex[4]], Str[DataIndex[5]]});
                            for (int j = 0; j < list.size(); j++) {
                                if (left.IsBelong(list.get(j).getLeft()) && right.IsBelong(list.get(j).getRight())) {
                                    synchronized (MatrixList.get(j)) {
                                        MatrixList.get(j).addToEntry(((left.region.Start + left.region.End) / 2 - list.get(j).getLeft().region.Start + 1) / Resolution, ((right.region.Start + right.region.End) / 2 - list.get(j).getRight().region.Start + 1) / Resolution, 1);
                                    }
                                    break;
                                } else if (right.IsBelong(list.get(j).getLeft()) && left.IsBelong(list.get(j).getRight())) {
                                    synchronized (MatrixList.get(j)) {
                                        MatrixList.get(j).addToEntry(((right.region.Start + right.region.End) / 2 - list.get(j).getLeft().region.Start + 1) / Resolution, ((left.region.Start + left.region.End) / 2 - list.get(j).getRight().region.Start + 1) / Resolution, 1);
                                    }
                                    break;
                                }
                            }
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                }
            });
            t[i].start();
        }
        Tools.ThreadsWait(t);
        return MatrixList;
    }

    public ArrayList<Array2DRowRealMatrix> Run(List<InterAction> list, ArrayList<Integer> Resolution) throws IOException {
        //初始化矩阵列表
        ArrayList<Array2DRowRealMatrix> MatrixList = new ArrayList<>();
        for (int i = 0; i < list.size(); i++) {
            Array2DRowRealMatrix aMatrix = new Array2DRowRealMatrix(list.get(i).getLeft().region.getLength() / Resolution.get(i) + 1, list.get(i).getRight().region.getLength() / Resolution.get(i) + 1);
            MatrixList.add(aMatrix);
            for (int j = 0; j < aMatrix.getRowDimension(); j++) {
                for (int k = 0; k < aMatrix.getColumnDimension(); k++) {
                    aMatrix.setEntry(j, k, 0);
                }
            }
        }
        BufferedReader reader = new BufferedReader(new FileReader(BedpeFile));
        //多线程构建矩阵
        Thread[] t = new Thread[Threads];
        for (int i = 0; i < t.length; i++) {
            t[i] = new Thread(new Runnable() {
                @Override
                public void run() {
                    String Line;
                    String[] Str;
                    try {
                        int[] DataIndex = IndexParse(new BedpeFile(BedpeFile));
                        while ((Line = reader.readLine()) != null) {
                            Str = Line.split("\\s+");
                            ChrRegion left = new ChrRegion(new String[]{Str[DataIndex[0]], Str[DataIndex[1]], Str[DataIndex[2]]});
                            ChrRegion right = new ChrRegion(new String[]{Str[DataIndex[3]], Str[DataIndex[4]], Str[DataIndex[5]]});
                            for (int j = 0; j < list.size(); j++) {
                                if (left.IsBelong(list.get(j).getLeft()) && right.IsBelong(list.get(j).getRight())) {
                                    synchronized (MatrixList.get(j)) {
                                        MatrixList.get(j).addToEntry(((left.region.Start + left.region.End) / 2 - list.get(j).getLeft().region.Start) / Resolution.get(j), ((right.region.Start + right.region.End) / 2 - list.get(j).getRight().region.Start) / Resolution.get(j), 1);
                                    }
//                                    break;
                                } else if (right.IsBelong(list.get(j).getLeft()) && left.IsBelong(list.get(j).getRight())) {
                                    synchronized (MatrixList.get(j)) {
                                        MatrixList.get(j).addToEntry(((right.region.Start + right.region.End) / 2 - list.get(j).getLeft().region.Start) / Resolution.get(j), ((left.region.Start + left.region.End) / 2 - list.get(j).getRight().region.Start) / Resolution.get(j), 1);
                                    }
//                                    break;
                                }
                            }
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                }
            });
            t[i].start();
        }
        Tools.ThreadsWait(t);
        return MatrixList;
    }

    public File getSpareMatrixFile() {
        return SpareMatrixFile;
    }

    public File getTwoDMatrixFile() {
        return TwoDMatrixFile;
    }

    public File getBinSizeFile() {
        return BinSizeFile;
    }

    private int[] IndexParse(BedpeFile file) throws IOException {
        int[] Index = new int[6];
        switch (file.BedpeDetect()) {
            case BedpeRegionFormat:
                Index = new int[]{0, 1, 2, 3, 4, 5};
                break;
            default:
                System.err.println("Error foramt!");
                System.exit(1);
        }
        return Index;
    }
}
