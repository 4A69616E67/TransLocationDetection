package File.FastQFile;

import File.AbstractFile;
import TLD.Tools;
import Unit.*;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;

/**
 * Created by snowf on 2019/2/17.
 */
public class FastqFile extends AbstractFile<FastqItem> {

    public FastqFile(File file) {
        super(file);
    }

    public FastqFile(String s) {
        this(new File(s));
    }

    public FastqFile(FastqFile file) {
        super(file);
    }

    @Override
    protected FastqItem ExtractItem(String[] s) {
        FastqItem Item;
        if (s == null) {
            Item = null;
        } else {
            Item = new FastqItem(s[0]);
            Item.Sequence = s[1];
            Item.Orientation = s[2];
            Item.Quality = s[3];
        }
        return Item;
    }


    @Override
    public synchronized String[] ReadItemLine() throws IOException {
        String[] s = new String[4];
        for (int i = 1; i <= 4; i++) {
            String line = reader.readLine();
            if (line != null) {
                s[i - 1] = line;
            } else {
                return null;
            }
        }
        return s;
    }

    @Override
    public void WriteItem(FastqItem item) throws IOException {
        writer.write(item.toString());
    }

    @Override
    protected SortItem<FastqItem> ExtractSortItem(String[] s) {
        if (s == null) {
            return null;
        }
        return new SortItem<>(new FastqItem(s[0]));
    }


    public AbstractFile.FileFormat FastqPhred() throws IOException {
        FastqItem Item;
        ReadOpen();
        int[] FormatEdge = new int[]{(int) '9', (int) 'K'};
        int[] Count = new int[2];
        int LineNum = 0;
        while ((Item = ReadItem()) != null && ++LineNum <= 100) {
            for (int i = 0; i < Item.Quality.length(); i++) {
                if ((int) Item.Quality.charAt(i) <= FormatEdge[0]) {
                    Count[0]++;
                } else if ((int) Item.Quality.charAt(i) >= FormatEdge[1]) {
                    Count[1]++;
                }
            }
        }
        ReadClose();
        return Count[0] >= Count[1] ? AbstractFile.FileFormat.Phred33 : AbstractFile.FileFormat.Phred64;
    }

    public ArrayList<FastqItem> ExtractID(Collection<String> List) throws IOException {
        return ExtractID(List, 1);
    }

    public ArrayList<FastqItem> ExtractID(Collection<String> List, int threads) throws IOException {
        ArrayList<FastqItem> ResList = new ArrayList<>();
        ReadOpen();
        Thread[] t = new Thread[threads];
        for (int i = 0; i < t.length; i++) {
            t[i] = new Thread(() -> {
                FastqItem item;
                try {
                    while ((item = ReadItem()) != null) {
                        if (List.contains(item.Title)) {
                            synchronized (this) {
                                ResList.add(item);
                                List.remove(item.Title);
                                if (List.size() <= 0) {
                                    break;
                                }
                            }
                        }
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });
            t[i].start();
        }
        Tools.ThreadsWait(t);
        ReadClose();
        return ResList;
    }

    public void ExtractID(Collection<String> List, int threads, FastqFile OutFile) throws IOException {
        ReadOpen();
        OutFile.WriteOpen();
        Thread[] t = new Thread[threads];
        for (int i = 0; i < t.length; i++) {
            t[i] = new Thread(() -> {
                FastqItem item;
                try {
                    while ((item = ReadItem()) != null) {
                        if (List.contains(item.Title)) {
                            synchronized (this) {
                                OutFile.WriteItemln(item);
                                List.remove(item.Title);
                                if (List.size() <= 0) {
                                    break;
                                }
                            }
                        }
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });
            t[i].start();
        }
        Tools.ThreadsWait(t);
        ReadClose();
        OutFile.WriteClose();
    }
}
