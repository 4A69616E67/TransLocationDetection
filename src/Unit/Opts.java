package Unit;

import File.CommonFile.CommonFile;
import Software.Python;
import org.apache.commons.cli.CommandLine;

import java.io.File;
import java.util.Date;
import java.util.Hashtable;

/**
 * Created by snowf on 2019/2/17.
 */
public class Opts {

    public enum FileFormat{
        Phred33,Phred64,BedpeRegionFormat,ErrorFormat,EmptyFile
    }

    public static String GetStringOpt(CommandLine commandLine, String opt_string, String default_string) {
        return commandLine.hasOption(opt_string) ? commandLine.getOptionValue(opt_string) : default_string;
    }

    public static File GetFileOpt(CommandLine commandLine, String opt_string, File default_file) {
        return commandLine.hasOption(opt_string) ? new File(commandLine.getOptionValue(opt_string)) : default_file;
    }

    public static int GetIntOpt(CommandLine commandLine, String opt_string, int default_int) {
        return commandLine.hasOption(opt_string) ? Integer.parseInt(commandLine.getOptionValue(opt_string)) : default_int;
    }

    public static float GetFloatOpt(CommandLine commandLine, String opt_string, float default_float) {
        return commandLine.hasOption(opt_string) ? Float.parseFloat(commandLine.getOptionValue(opt_string)) : default_float;
    }

    public static String[] GetStringOpts(CommandLine commandLine, String opt_string, String[] default_string) {
        return commandLine.hasOption(opt_string) ? commandLine.getOptionValues(opt_string) : default_string;
    }

    public static File[] GetFileOpts(CommandLine commandLine, String opt_string, File[] default_file) {
        if (commandLine.hasOption(opt_string)) {
            return StringArrays.toFile(commandLine.getOptionValues(opt_string));
        } else {
            return default_file;
        }
    }

    public static int[] GetIntOpts(CommandLine commandLine, String opt_string, int[] default_string) {
        if (commandLine.hasOption(opt_string)) {
            return StringArrays.toInteger(commandLine.getOptionValues(opt_string));
        } else {
            return default_string;
        }
    }

    public static final String OsName = System.getProperty("os.name");
    public static final int MaxBinNum = 1000000;//最大bin的数目
    public static final String InterResourceDir = "Resource";
    public static final String InterArchiveDir = "Archive";
    public static final File JarFile = new File(Opts.class.getProtectionDomain().getCodeSource().getLocation().getFile());
    public static final File OutScriptDir = new File(JarFile.getParent() + "/Script");//脚本文件存放的位置
    public static final File OutResourceDir = new File(JarFile.getParent() + "/Resource");//资源文件存放的位置
    public static CommonFile CommandOutFile = new CommonFile(Configure.OutPath + "/Command.log");
    public static CommonFile StatisticFile = new CommonFile(Configure.OutPath + "/Statistic.txt");
    public static CommonFile ResourceStatFile = new CommonFile(Configure.OutPath + "/JVM_stat.txt");
    public static final String[] ResourceFile = new String[]{"default.conf", "default_adv.conf"};
    public static final String[] ScriptFile = new String[]{"PlotHeatMap.py", "StatisticPlot.py", "RegionPlot.py"};
    public static final CommonFile ConfigFile = new CommonFile(OutResourceDir + "/" + ResourceFile[0]);
    public static final CommonFile AdvConfigFile = new CommonFile(OutResourceDir + "/" + ResourceFile[1]);
    public static final File PlotHeatMapScriptFile = new File(OutScriptDir + "/" + ScriptFile[0]);
    public static final File StatisticPlotFile = new File(OutScriptDir + "/" + ScriptFile[1]);
    public static final File StyleCss = new File("/" + InterResourceDir + "/style.css");
    public static final File JqueryJs = new File("/" + InterResourceDir + "/jquery.min.js");
    public static final File ScriptJs = new File("/" + InterResourceDir + "/script.js");
    public static final File TemplateReportHtml = new File("/" + InterResourceDir + "/Report.html");
    public static final File ReadMeFile = new File("/" + InterResourceDir + "/ReadMe.txt");
    public static final Float Version = 1.0F;
    public static final String Author = "Snowflakes";
    public static final String Email = "john-jh@foxmail.com";
    public static final long MaxMemory = Runtime.getRuntime().maxMemory();//java能获取的最大内存
    public static Hashtable<String, Integer> ChrSize = new Hashtable<>();
    public static final Date StartTime = new Date();
    public static final Python Python = new Python("python");
    //==================================================================================================================
}



