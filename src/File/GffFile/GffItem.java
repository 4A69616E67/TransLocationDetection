package File.GffFile;

import java.util.HashMap;

/**
 * Created by snowf on 2019/5/5.
 */

public class GffItem {
    public String[] Columns = new String[8];
    public HashMap<String, String> map = new HashMap<>();

    public GffItem(String s) {
        String[] ss = s.split("\\t");
        System.arraycopy(ss, 0, Columns, 0, 8);
        String[] comments = ss[8].split("=|;");
        for (int i = 0; i < comments.length - 1; i++) {
            map.put(comments[i], comments[i + 1]);
        }
    }
}
