package Unit;

import org.apache.commons.cli.Option;

/**
 * Created by snowf on 2019/11/4.
 */

public class Argument {

    public static final Option INPUT = Option.builder("i").longOpt("input").hasArg().argName("file").desc("input file").build();
    public static final Option PREFIX = Option.builder("p").longOpt("prefix").hasArg().argName("string").desc("output prefix").build();
    public static final Option OUTPATH = Option.builder("o").longOpt("out").hasArg().argName("path").desc("output path").build();
    public static final Option THREAD = Option.builder("t").longOpt("thread").hasArg().argName("int").desc("threads").build();

}
