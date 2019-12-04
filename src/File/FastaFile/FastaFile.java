package File.FastaFile;

import File.AbstractFile;

import java.io.IOException;
import java.util.ArrayList;

/**
 * Created by snowf on 2019/2/17.
 */
public class FastaFile extends AbstractFile<FastaItem> {
    public FastaFile(String pathname) {
        super(pathname);
    }

    public FastaFile(FastaFile file) {
        super(file);
    }

    @Override
    protected FastaItem ExtractItem(String[] s) {
        FastaItem Item;
        if (s == null) {
            Item = null;
        } else {
            String title;
            String[] str = s[0].split("\\s+");
            if (str[0].equals(">")) {
                title = str[0] + str[1];
            } else {
                title = str[0];
            }
            Item = new FastaItem(title.replaceAll("^>\\s*", "").trim());
            for (int i = 1; i < s.length; i++) {
                Item.Sequence.append(s[i]);
            }
        }
        return Item;
    }

    @Override
    public synchronized String[] ReadItemLine() throws IOException {
        ArrayList<String> seq = new ArrayList<>();
        String line = reader.readLine();
        if (line == null) {
            return null;
        }
        seq.add(line);
        reader.mark(1000);
        line = reader.readLine();
        while (line != null && !line.matches("^>.*")) {
            seq.add(line);
            reader.mark(1000);
            line = reader.readLine();
        }
        reader.reset();
        return seq.toArray(new String[0]);
    }

    @Override
    public void WriteItem(FastaItem item) throws IOException {
        writer.write(item.toString());
    }

//    @Override
//    protected SortItem<FastaItem> ExtractSortItem(String[] s) {
//        if (s == null) {
//            return null;
//        }
//        return new SortItem<>(new FastaItem(s[0]));
//    }

}
