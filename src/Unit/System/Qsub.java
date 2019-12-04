package Unit.System;

import File.CommonFile.CommonFile;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;

/**
 * Created by snowf on 2019/6/15.
 */

public class Qsub extends Pbs {
    public static final String ExeName = "qsub";

    public Qsub(CommonFile file, String nodes, int threads, long memory, String prefix) {
        super(file, nodes, threads, memory, prefix);
    }

    @Override
    public void CreateSubmitFile() throws IOException {
        StringBuilder Header = new StringBuilder();
        Header.append("#PBS -d ./\n");
        this.Nodes = this.Nodes == null ? "1" : this.Nodes;
        this.Threads = this.Threads <= 0 ? 1 : this.Threads;
        Header.append("#PBS -l nodes=").append(Nodes).append(":ppn=").append(Threads);
        if (Memory > 0) {
            Header.append(",mem=").append((int) Math.ceil(Memory / Math.pow(10, 9))).append("g");
        }
        Header.append("\n");
        if (Prefix != null) {
            Header.append("#PBS -N ").append(Prefix).append("\n");
        }
        ArrayList<char[]> Lines = SubmitFile.Read();
        BufferedWriter writer = SubmitFile.WriteOpen();
        writer.write(Header.toString());
        for (char[] item : Lines) {
            writer.write(item);
            writer.write("\n");
        }
        writer.close();
    }

    @Override
    public String run() throws InterruptedException, IOException {
        StringWriter Out = new StringWriter();
        CreateSubmitFile();
        int ExitValue = CommandLine.run(ExeName + " " + SubmitFile, new PrintWriter(Out), new PrintWriter(System.err));
        if (ExitValue != 0) {
            throw new InterruptedException("qsub error");
        }
        return Out.getBuffer().toString();
    }
}
