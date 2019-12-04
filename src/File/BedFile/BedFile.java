package File.BedFile;

import File.AbstractFile;
import File.CommonFile.CommonFile;
import Unit.Configure;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;

/**
 * Created by snowf on 2019/2/17.
 */
public class BedFile extends AbstractFile<BedItem> {
//    public BedItem.Sort SortBy = BedItem.Sort.SeqTitle;

    public BedFile(String pathname) {
        super(pathname);
    }

    private BedFile(AbstractFile file) {
        super(file);
    }

    @Override
    protected BedItem ExtractItem(String[] s) {
        BedItem Item;
        if (s != null) {
            Item = new BedItem(s[0].split("\\s+"));
//            Item.SortBy = SortBy;
        } else {
            Item = null;
        }
        return Item;
    }

    public void SplitSortFile(BedFile OutFile, Comparator<BedItem> comparator) throws IOException {
        int splitItemNum = 1000000;
        ItemNum = getItemNum();
        if (this.ItemNum > splitItemNum) {
            splitItemNum = (int) Math.ceil(this.ItemNum / Math.ceil((double) this.ItemNum / splitItemNum));
            ArrayList<CommonFile> TempSplitFile = this.SplitFile(this.getPath(), splitItemNum);
            BedFile[] TempSplitSortFile = new BedFile[TempSplitFile.size()];
            for (int i = 0; i < TempSplitFile.size(); i++) {
                TempSplitSortFile[i] = new BedFile(TempSplitFile.get(i).getPath() + ".sort");
                new BedFile(TempSplitFile.get(i)).SortFile(TempSplitSortFile[i], comparator);
            }
            OutFile.MergeSortFile(TempSplitSortFile, comparator);
            if (Configure.DeBugLevel < 1) {
                for (int i = 0; i < TempSplitFile.size(); i++) {
                    AbstractFile.delete(TempSplitFile.get(i));
                    AbstractFile.delete(TempSplitSortFile[i]);
                }
            }
        } else {
            this.SortFile(OutFile, comparator);
        }
    }

    @Override
    public void WriteItem(BedItem item) throws IOException {
        writer.write(item.toString());
    }

}
