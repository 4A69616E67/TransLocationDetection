package File.CommonFile;

import File.AbstractItem;

import java.util.Comparator;

/**
 * Created by snowf on 2019/8/28.
 */

public class CommonItem extends AbstractItem {
    public String item;

    public CommonItem(String s) {
        item = s;
    }

    public static class CommonComparator implements Comparator<CommonItem> {

        @Override
        public int compare(CommonItem o1, CommonItem o2) {
            return o1.item.compareTo(o2.item);
        }
    }

}
