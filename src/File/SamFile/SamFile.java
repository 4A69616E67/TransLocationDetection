package File.SamFile;

import File.AbstractFile;
import File.BedFile.BedFile;
import Unit.SortItem;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;

/**
 * Created by snowf on 2019/2/17.
 */

public class SamFile extends AbstractFile<SamItem> {
    private ArrayList<String> Header = new ArrayList<>();
    private boolean SortByName = true;

    public SamFile(String pathname) {
        super(pathname);
    }

//    public SamFile(SamFile file) {
//        super(file);
//    }

    @Override
    protected SamItem ExtractItem(String[] s) {
        SamItem Item;
        if (s == null) {
            return null;
        }
        String[] line_split = s[0].split("\\s+");
        Item = new SamItem(s);
        Item.SortByName = SortByName;
        return Item;
    }

    @Override
    public void WriteItem(SamItem item) throws IOException {
        writer.write(item.toString());
    }

    @Override
    protected SortItem<SamItem> ExtractSortItem(String[] s) {
        return null;
    }


    private static int CalculateFragLength(String s) {
        int Length = 0;
        StringBuilder Str = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            switch (s.charAt(i)) {
                case 'M':
                case 'D':
                case 'N':
                    Length += Integer.parseInt(Str.toString());
                    Str.setLength(0);
                    break;
                case 'I':
                case 'S':
                case 'P':
                case 'H':
                    Str.setLength(0);
                    break;
                default:
                    Str.append(s.charAt(i));
            }
        }
        return Length;
    }

    public void ToBedFile(BedFile bedFile) throws IOException {
        System.out.println(new Date() + "\tBegin\t" + getName() + " to " + bedFile.getName());
        ReadOpen();
        BufferedReader reader = getReader();
        BufferedWriter writer = bedFile.WriteOpen();
        String Line;
        String[] Str;
        String Orientation;
        while ((Line = reader.readLine()) != null) {
            if (Line.matches("^@.*")) {
                continue;
            }
            Str = Line.split("\\s+");
            Orientation = (Integer.parseInt(Str[1]) & 16) == 16 ? "-" : "+";
            writer.write(Str[2] + "\t" + Str[3] + "\t" + (Integer.parseInt(Str[3]) + CalculateFragLength(Str[5]) - 1) + "\t" + Str[0] + "\t" + Str[4] + "\t" + Orientation + "\n");
        }
        writer.close();
        reader.close();
    }

    public ArrayList<String> getHeader() {
        return Header;
    }

    public void ReadHeader() throws IOException {
        String[] lines;
        reader.mark(1000);
        while ((lines = ReadItemLine()) != null) {
            if (lines[0].matches("^@.*")) {
                reader.reset();
                break;
            }
            Header.add(lines[0]);
            reader.mark(1000);
        }
    }

    public synchronized void Merge(SamFile[] files) throws IOException {
        BufferedWriter writer = WriteOpen();
        files[0].ReadOpen();
        files[0].ReadHeader();
        Append(files[0].getHeader());
        ItemNum = 0;
        String[] lines;
        for (SamFile x : files) {
            System.out.println(new Date() + "\tMerge " + x.getName() + " to " + getName());
            x.ReadOpen();
            x.ReadHeader();
            while ((lines = x.ReadItemLine()) != null) {
                for (String line : lines) {
                    writer.write(line);
                    writer.write("\n");
                }
                ItemNum++;
            }
            x.ReadClose();
        }
        WriteClose();
        System.out.println(new Date() + "\tDone merge");
    }
}

