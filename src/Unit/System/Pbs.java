package Unit.System;

import File.CommonFile.CommonFile;

/**
 * Created by snowf on 2019/6/15.
 */

public abstract class Pbs {
    protected CommonFile SubmitFile;
    protected String Nodes;
    protected int Threads;
    protected long Memory;
    protected String Prefix;

    public Pbs(CommonFile file, String nodes, int threads, long memory, String prefix) {
        SubmitFile = file;
        Nodes = nodes;
        Threads = threads;
        Memory = memory;
        Prefix = prefix;
    }

    public abstract void CreateSubmitFile() throws Exception;

    public abstract String run() throws Exception;
}
