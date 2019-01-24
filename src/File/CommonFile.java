package File;

import Unit.SortItem;

import java.io.File;
import java.io.IOException;

public class CommonFile extends AbstractFile<String> {
    public String Regex = "\\s+";

    public CommonFile(String pathname) {
        super(pathname);
    }

    public CommonFile(File f) {
        super(f);
    }


    @Override
    protected String ExtractItem(String s) {
        return s;
    }

    @Override
    public void WriteItem(String item) throws IOException {
        writer.write(item);
    }

    @Override
    public SortItem<String> ReadSortItem() throws IOException {
        String Line = reader.readLine();
        return new SortItem<>(Line, Line.toCharArray());
    }
}
