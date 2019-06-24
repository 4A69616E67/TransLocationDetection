package File;

import Unit.SortItem;

import java.io.File;
import java.io.IOException;

/**
 * Created by snowf on 2019/2/17.
 */
public class CommonFile extends AbstractFile<String> {
    public String Regex = "\\s+";

    public CommonFile(String pathname) {
        super(pathname);
    }

    public CommonFile(File f) {
        super(f);
    }

    public CommonFile(CommonFile file) {
        super(file);
    }


    @Override
    protected String ExtractItem(String[] s) {
        if (s != null)
            return s[0];
        return null;
    }

    @Override
    public void WriteItem(String item) throws IOException {
        writer.write(item);
    }

    @Override
    protected SortItem<String> ExtractSortItem(String[] s) {
        if (s == null) {
            return null;
        }
        return new SortItem<>(s[0]);
    }
}
