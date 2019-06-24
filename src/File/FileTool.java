package File;

import File.FastQFile.FastqFile;
import File.FastQFile.FastqItem;
import Unit.StringArrays;

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

    public static AbstractFile.FileFormat ReadsType(FastqFile fastqfile) throws IOException {
        int LineNumber = 100, i = 0, Count = 0;
        fastqfile.ReadOpen();
//        BufferedReader reader = new BufferedReader(new FileReader(fastqfile));
        FastqItem item;
        while ((item = fastqfile.ReadItem()) != null) {
            Count += item.Sequence.length();
            i++;
            if (i >= LineNumber) {
                break;
            }
        }
        fastqfile.ReadClose();
        if (i == 0) {
            return AbstractFile.FileFormat.ErrorFormat;
        }
        if (Count / i >= 70) {
            return AbstractFile.FileFormat.LongReads;
        } else {
            return AbstractFile.FileFormat.ShortReads;
        }
    }

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

}
