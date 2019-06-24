package Unit;

import TLD.Tools;

import java.io.*;
import java.util.ArrayList;
import java.util.Properties;

public class Configure {

    //-----------------------------------------------------------
    public static String Restriction;
    public static File GenomeFile;
    //-----------------------------------------------------------
    public static File OutPath = new File("./");
    public static String Prefix = "out";
    public static Chromosome[] Chromosome;
    public static int[] DrawResolution = new int[]{1000000};
    public static int DetectResolution = 100000;
    public static int Thread = 8;
    //-----------------------------------------------------------------
    public static int DeBugLevel = 0;

    public enum Require {
        Restriction("Restriction", Configure.Restriction), GenomeFile("GenomeFile", Configure.GenomeFile);
        private String Str;
        public Object Value;

        Require(String s, Object v) {
            this.Str = s;
            this.Value = v;
        }

        @Override
        public String toString() {
            return Str;
        }
    }

    private enum Optional {
        OutPath("OutPath", Configure.OutPath), Prefix("Prefix", Configure.Prefix), Chromosomes("Chromosomes", Tools.ArraysToString(Configure.Chromosome)), DetectResolution("DetectRes", Configure.DetectResolution), Thread("Thread", Configure.Thread);
        private String Str;
        public Object Value;

        Optional(String s, Object v) {
            this.Str = s;
            this.Value = v;
        }

        @Override
        public String toString() {
            return Str;
        }
    }

    private enum Advance {
        DeBugLevel("DeBugLevel", Configure.DeBugLevel);
        private String Str;
        public Object Value;

        Advance(String s, Object v) {
            this.Str = s;
            this.Value = v;
        }

        @Override
        public String toString() {
            return Str;
        }
    }

    public static void GetOption(File ConfFile, File AdvConfFile) throws IOException {
        Properties Config = new Properties();
        if (AdvConfFile != null && AdvConfFile.isFile()) {
            Config.load(new FileReader(AdvConfFile));
        }
        Config.load(new FileReader(ConfFile));
        for (Require r : Require.values()) {
            if (Config.getProperty(r.toString()) != null && !Config.getProperty(r.toString()).trim().equals(""))
                r.Value = Config.getProperty(r.toString()).trim();
        }
        for (Optional o : Optional.values()) {
            if (Config.getProperty(o.toString()) != null && !Config.getProperty(o.toString()).trim().equals(""))
                o.Value = Config.getProperty(o.toString()).trim();
        }
        for (Advance a : Advance.values()) {
            if (Config.getProperty(a.toString()) != null && !Config.getProperty(a.toString()).trim().equals(""))
                a.Value = Config.getProperty(a.toString()).trim();
        }
        Init();
    }

    public static String ShowParameter() {
        Update();
        ArrayList<String> ParameterStr = new ArrayList<>();
        for (Require opt : Require.values()) {
            String s = opt + ":\t" + (opt.Value == null ? "" : opt.Value);
            ParameterStr.add(s);
//            Unit.System.out.println(s);
        }
        ParameterStr.add("======================================================================================");
        for (Optional opt : Optional.values()) {
            String s = opt + ":\t" + (opt.Value == null ? "" : opt.Value);
            ParameterStr.add(s);
//            Unit.System.out.println(opt + ":\t" + (opt.Value == null ? "" : opt.Value));
        }
        ParameterStr.add("======================================================================================");
        for (Advance opt : Advance.values()) {
            String s = opt + ":\t" + (opt.Value == null ? "" : opt.Value);
            ParameterStr.add(s);
//            Unit.System.out.println(opt + ":\t" + (opt.Value == null ? "" : opt.Value));
        }
        return String.join("\n", ParameterStr.toArray(new String[0]));
    }

    public static void SaveParameter(File file) throws IOException {
        Update();
        BufferedWriter writer = new BufferedWriter(new FileWriter(file));
        for (Require opt : Require.values()) {
            writer.write(opt + " = " + (opt.Value == null ? "" : opt.Value) + "\n");
        }
        writer.write("#======================================================================================\n");
        for (Optional opt : Optional.values()) {
            writer.write(opt + " = " + (opt.Value == null ? "" : opt.Value) + "\n");
        }
        writer.write("#======================================================================================\n");
        for (Advance opt : Advance.values()) {
            writer.write(opt + " = " + (opt.Value == null ? "" : opt.Value) + "\n");
        }
        writer.close();
    }

    public static void Update() {
        Require.Restriction.Value = Restriction;
        Require.GenomeFile.Value = GenomeFile;
        //-------------------------------------------
        Optional.OutPath.Value = OutPath;
        Optional.Prefix.Value = Prefix;
        Optional.Chromosomes.Value = Tools.ArraysToString(Chromosome);
        Optional.DetectResolution.Value = DetectResolution;
        Optional.Thread.Value = Thread;
        //----------------------------------------------
        Advance.DeBugLevel.Value = DeBugLevel;
    }

    private static void Init() {
        Restriction = Require.Restriction.Value != null ? Require.Restriction.Value.toString().trim() : null;
        GenomeFile = Require.GenomeFile.Value != null ? new File(Require.GenomeFile.Value.toString()) : null;
        //----------------------------------------------------------------------------------------------------
        OutPath = Optional.OutPath.Value != null ? new File(Optional.OutPath.Value.toString().trim()) : OutPath;
        Prefix = Optional.Prefix.Value != null ? Optional.Prefix.Value.toString().trim() : Prefix;
        if (Optional.Chromosomes.Value != null && !Optional.Chromosomes.Value.toString().trim().equals("")) {
            String[] str = Optional.Chromosomes.Value.toString().trim().split("\\s+");
            Chromosome = new Chromosome[str.length];
            for (int i = 0; i < Chromosome.length; i++) {
                Chromosome[i] = new Chromosome(str[i]);
            }
        }
        DetectResolution = GetIntItem(Optional.DetectResolution.Value, DetectResolution);
        Thread = GetIntItem(Optional.Thread.Value, Thread);
        //----------------------------------------------------------------------------------------------------
        DeBugLevel = Integer.parseInt(Advance.DeBugLevel.Value.toString());
    }

    public static int GetIntItem(Object o, int d) {
        if (o != null) {
            try {
                int i = Integer.parseInt(o.toString().trim());
                if (i == 0) {
                    return d;
                } else {
                    return i;
                }
            } catch (NumberFormatException e) {
                return d;
            }
        }
        return d;
    }

}
