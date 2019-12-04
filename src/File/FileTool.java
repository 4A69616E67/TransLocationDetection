package File;

import File.FastQFile.FastqFile;
import File.FastQFile.FastqItem;
import File.FastaFile.FastaFile;
import File.FastaFile.FastaItem;
import Unit.System.CommandLine;
import Unit.*;

import java.io.*;
import java.util.ArrayList;

/**
 * Created by snowf on 2019/2/17.
 */
public class FileTool {

    public static String[] Read4Line(BufferedReader file) {
        String[] Str = new String[4];
        try {
            Str[0] = file.readLine();
            Str[1] = file.readLine();
            Str[2] = file.readLine();
            Str[3] = file.readLine();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return Str;
    }

//    public static Opts.FileFormat ReadsType(FastqFile fastqfile) throws IOException {
//        int LineNumber = 100, i = 0, Count = 0;
//        fastqfile.ReadOpen();
////        BufferedReader reader = new BufferedReader(new FileReader(fastqfile));
//        FastqItem item;
//        while ((item = fastqfile.ReadItem()) != null) {
//            Count += item.Sequence.length();
//            i++;
//            if (i >= LineNumber) {
//                break;
//            }
//        }
//        fastqfile.ReadClose();
//        if (i == 0) {
//            return Opts.FileFormat.ErrorFormat;
//        }
//        if (Count / i >= 70) {
//            return Opts.FileFormat.LongReads;
//        } else {
//            return Opts.FileFormat.ShortReads;
//        }
//    }

    public static InputStreamReader GetFileStream(String s) {
        return new InputStreamReader(FileTool.class.getResourceAsStream(s));
    }

    public static void ExtractFile(String InternalFile, File OutFile) throws IOException {
        BufferedReader reader = new BufferedReader(GetFileStream(InternalFile));
        BufferedWriter writer = new BufferedWriter(new FileWriter(OutFile));
        String Line;
        while ((Line = reader.readLine()) != null) {
            writer.write(Line + "\n");
        }
        writer.close();
    }

    public static void MergeSamFile(AbstractFile[] InFile, AbstractFile MergeFile) throws IOException {
        BufferedWriter out = new BufferedWriter(new FileWriter(MergeFile));
        MergeFile.ItemNum = 0;
        String line;
        BufferedReader gethead = new BufferedReader(new FileReader(InFile[0]));
        while ((line = gethead.readLine()) != null && line.matches("^@.*")) {
            out.write(line + "\n");
        }
        gethead.close();
        for (File file : InFile) {
            BufferedReader in = new BufferedReader(new FileReader(file));
            while ((line = in.readLine()) != null) {
                if (line.matches("^@.*")) {
                    continue;
                }
                out.write(line + "\n");
                MergeFile.ItemNum++;
            }
            in.close();
        }
        out.close();
    }

    public static double[][] ReadMatrixFile(File file) throws IOException {
        BufferedReader in = new BufferedReader(new FileReader(file));
        String line;
        ArrayList<double[]> List = new ArrayList<>();
        while ((line = in.readLine()) != null) {
            List.add(StringArrays.toDouble(line.split("\\s+")));
        }
        in.close();
        double[][] matrix = new double[List.size()][];
        for (int i = 0; i < List.size(); i++) {
            matrix[i] = new double[List.get(i).length];
            if (List.get(i).length >= 0) System.arraycopy(List.get(i), 0, matrix[i], 0, List.get(i).length);
        }
        return matrix;
    }

//    public static String AdapterDetection(FastqFile file, File Prefix, int SubIndex, AbstractFile stat_file) throws IOException, InterruptedException {
//        StringBuilder Adapter = new StringBuilder();
//        int SeqNum = 200;
//        FastaFile HeadFile = new FastaFile(Prefix + ".head" + SeqNum);
//        file.ReadOpen();
//        BufferedWriter writer = new BufferedWriter(new FileWriter(HeadFile));
//        FastqItem fastq_item;
//        int linenumber = 0;
//        while ((fastq_item = file.ReadItem()) != null && ++linenumber <= SeqNum) {
//            writer.write(fastq_item.Title.replace("@", ">") + "\n");
//            writer.write(fastq_item.Sequence.substring(SubIndex) + "\n");
//        }
//        file.ReadClose();
//        writer.close();
//        FastaItem[] SplitAdapter = FindSimilarSequences(HeadFile, stat_file, 0.5f);
//        int MaxValue = 0;
//        for (FastaItem aSplitAdapter : SplitAdapter) {
//            if (aSplitAdapter.Sequence.length() > MaxValue) {
//                MaxValue = aSplitAdapter.Sequence.length();
//                Adapter = aSplitAdapter.Sequence;
//            }
//        }
//        return Adapter.toString();
//    }

//    private static FastaItem[] FindSimilarSequences(FastaFile file, AbstractFile stat_file, float threshold) throws IOException, InterruptedException {
//        FastaFile MsaFile = new FastaFile(file.getPath() + ".msa");
//        StringBuilder SimSeq = new StringBuilder();
//        ArrayList<char[]> MsaStat = new ArrayList<>();
//        ArrayList<float[]> BaseFreq = new ArrayList<>();
//        int[] CountArrays = new int[255];
//        FastaItem[] ResItems;
//        //----------------------------------------------------------------------
//        String ComLine = Configure.Mafft.Exe() + " " + file.getPath();
//        Opts.CommandOutFile.Append(ComLine + "\n");
//        PrintWriter msa = new PrintWriter(MsaFile);
//        if (Configure.DeBugLevel < 1) {
//            CommandLineDhat.run(ComLine, msa, null);
//        } else {
//            CommandLineDhat.run(ComLine, msa, new PrintWriter(System.err));
//        }
//        msa.close();
//        MsaFile.ReadOpen();
//        FastaItem item;
//        while ((item = MsaFile.ReadItem()) != null) {
//            MsaStat.add(item.Sequence.toString().toCharArray());
//        }
//        int SeqNum = MsaStat.size();
//        MsaFile.ReadClose();
//        for (int i = 0; i < MsaStat.get(0).length; i++) {
//            CountArrays['A'] = 0;
//            CountArrays['T'] = 0;
//            CountArrays['C'] = 0;
//            CountArrays['G'] = 0;
//            CountArrays['-'] = 0;
//            for (char[] aMsaStat : MsaStat) {
//                CountArrays[Character.toUpperCase(aMsaStat[i])]++;
//            }
//            int MaxValue = 0;
//            char MaxBase = '-';
//            BaseFreq.add(new float[255]);
//            for (char base : new char[]{'A', 'T', 'C', 'G', '-'}) {
//                BaseFreq.get(i)[base] = (float) CountArrays[base] / SeqNum;
//                if (CountArrays[base] > MaxValue) {
//                    MaxValue = CountArrays[base];
//                    MaxBase = base;
//                }
//            }
//            if (MaxValue > SeqNum * threshold) {
//                SimSeq.append(MaxBase);
//            } else {
//                SimSeq.append('N');
//            }
//        }
//        String[] SplitSeq = SimSeq.toString().replace("-", "").split("N+");
//        ResItems = new FastaItem[SplitSeq.length];
//        for (int i = 0; i < ResItems.length; i++) {
//            ResItems[i] = new FastaItem(">seq" + i);
//            ResItems[i].Sequence.append(SplitSeq[i]);
//        }
//        if (stat_file != null) {
//            BufferedWriter writer = stat_file.WriteOpen();
//            writer.write("Position\tA\tT\tC\tG\t-\n");
//            for (int i = 0; i < BaseFreq.size(); i++) {
//                writer.write(String.valueOf(i + 1));
//                for (char base : new char[]{'A', 'T', 'C', 'G', '-'}) {
//                    writer.write("\t" + String.format("%.2f", BaseFreq.get(i)[base]));
//                }
//                writer.write("\n");
//            }
//            stat_file.WriteClose();
//        }
//        return ResItems;
//    }


}
