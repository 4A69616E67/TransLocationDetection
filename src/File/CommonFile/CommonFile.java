package File.CommonFile;

import File.AbstractFile;

import java.io.File;
import java.io.IOException;

/**
 * Created by snowf on 2019/2/17.
 */
public class CommonFile extends AbstractFile<CommonItem> {
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
    protected CommonItem ExtractItem(String[] s) {
        if (s != null)
            return new CommonItem(s[0]);
        return null;
    }

    @Override
    public void WriteItem(CommonItem item) throws IOException {
        writer.write(item.item);
    }

    public void WriteItem(String s) throws IOException {
        writer.write(s);
    }

//    @Override
//    protected SortItem<String> ExtractSortItem(String[] s) {
//        if (s == null) {
//            return null;
//        }
//        return new SortItem<>(s[0]);
//    }
}
