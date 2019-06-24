package File.BedFile;

import File.AbstractFile;
import File.CommonFile;
import Unit.ChrRegion;
import Unit.Configure;
import Unit.SortItem;

import java.io.IOException;
import java.util.ArrayList;

/**
 * Created by snowf on 2019/2/17.
 */
public class BedFile extends AbstractFile<BedItem> {
    public BedItem.Sort SortBy = BedItem.Sort.SeqTitle;

    public BedFile(String pathname) {
        super(pathname);
    }

    private BedFile(AbstractFile file) {
        super(file);
    }

    @Override
    protected SortItem<BedItem> ExtractSortItem(String[] s) {
        BedItem Item;
        if (s == null) {
            return null;
        }
        String[] ls = s[0].split("\\s+");
        if (SortBy == BedItem.Sort.SeqTitle) {
            Item = new BedItem(ls[3], null, 0, null);
        } else {
            Item = new BedItem(null, new ChrRegion(ls), 0, null);
            if (ls.length > 5) {
                Item.getLocation().Orientation = ls[5].charAt(0);
            }
        }
        Item.SortBy = SortBy;
        return new SortItem<>(Item);
    }

    @Override
    protected BedItem ExtractItem(String[] s) {
        BedItem Item;
        if (s != null) {
            Item = new BedItem(s[0].split("\\s+"));
            Item.SortBy = SortBy;
        } else {
            Item = null;
        }
        return Item;
    }

    public void SplitSortFile(BedFile OutFile) throws IOException {
        int splitItemNum = 5000000;
        ItemNum = getItemNum();
        if (this.ItemNum > splitItemNum) {
            splitItemNum = (int) Math.ceil(this.ItemNum / Math.ceil((double) this.ItemNum / splitItemNum));
            ArrayList<CommonFile> TempSplitFile = this.SplitFile(this.getPath(), splitItemNum);
            BedFile[] TempSplitSortFile = new BedFile[TempSplitFile.size()];
            for (int i = 0; i < TempSplitFile.size(); i++) {
                TempSplitSortFile[i] = new BedFile(TempSplitFile.get(i).getPath() + ".sort");
                new BedFile(TempSplitFile.get(i)).SortFile(TempSplitSortFile[i]);
            }
            OutFile.MergeSortFile(TempSplitSortFile);
            if (Configure.DeBugLevel < 1) {
                for (int i = 0; i < TempSplitFile.size(); i++) {
                    AbstractFile.delete(TempSplitFile.get(i));
                    AbstractFile.delete(TempSplitSortFile[i]);
                }
            }
        } else {
            this.SortFile(OutFile);
        }
    }

    @Override
    public void WriteItem(BedItem item) throws IOException {
        writer.write(item.toString());
    }

}
