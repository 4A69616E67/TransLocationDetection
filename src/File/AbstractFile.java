package File;

import File.CommonFile.CommonFile;

import java.io.*;
import java.util.*;
import java.util.zip.GZIPInputStream;

/**
 * Created by snowf on 2019/2/17.
 */
public abstract class AbstractFile<E extends AbstractItem> extends File {
    public long ItemNum = 0;
    private boolean Sorted = false;
    private int BufferSize = 1024 * 1024;// default 1M
    protected BufferedReader reader;
    protected BufferedWriter writer;

    public AbstractFile(String pathname) {
        super(pathname);
    }

    public AbstractFile(File file) {
        super(file.getPath());
    }

    public AbstractFile(AbstractFile file) {
        super(file.getPath());
        ItemNum = file.ItemNum;
        reader = null;
        writer = null;
    }

    public void CalculateItemNumber() throws IOException {
        ItemNum = 0;
        if (!isFile()) {
            return;
        }
        ReadOpen();
        while (ReadItemLine() != null) {
            ItemNum++;
        }
        ReadClose();
    }

    public void ReadOpen() throws IOException {
        if (getName().matches(".*\\.gz")) {
            reader = new BufferedReader(new InputStreamReader(new GZIPInputStream(new FileInputStream(this))), BufferSize);
        } else {
            reader = new BufferedReader(new FileReader(this), BufferSize);
        }
    }

    public void ReadClose() throws IOException {
        reader.close();
    }

    public BufferedWriter WriteOpen() throws IOException {
        return WriteOpen(false);
    }

    private BufferedWriter WriteOpen(boolean append) throws IOException {
        writer = new BufferedWriter(new FileWriter(this, append), BufferSize);
        return writer;
    }

    public void WriteClose() throws IOException {
        writer.close();
    }

    public ArrayList<char[]> Read() throws IOException {
        ReadOpen();
        ItemNum = 0;
        ArrayList<char[]> List = new ArrayList<>();
        String[] Lines;
        while ((Lines = ReadItemLine()) != null) {
            char[] lines = String.join("\n", Lines).toCharArray();
            List.add(lines);
            ItemNum++;
        }
        ReadClose();
        return List;
    }

    protected abstract E ExtractItem(String[] s);

    public ArrayList<E> Extraction(int num) throws IOException {
        ArrayList<E> list = new ArrayList<>();
        ReadOpen();
        E item;
        int i = 0;
        while ((item = ReadItem()) != null) {
            i++;
            if (i <= num) {
                list.add(item);
            } else {
                break;
            }
        }
        ReadClose();
        return list;
    }

    public synchronized String[] ReadItemLine() throws IOException {
        String line = reader.readLine();
        if (line != null) {
            return new String[]{line};
        }
        return null;
    }

    public E ReadItem() throws IOException {
        return ExtractItem(ReadItemLine());
    }

    public abstract void WriteItem(E item) throws IOException;

    public void WriteItemln(E item) throws IOException {
        WriteItem(item);
        writer.write("\n");
    }

    public BufferedReader getReader() {
        return reader;
    }

    public BufferedWriter getWriter() {
        return writer;
    }

    public synchronized void Append(AbstractFile file) throws IOException {
        System.out.println(new Date() + "\tAppend " + file.getName() + " to " + getName());
        String[] item;
        file.ReadOpen();
        BufferedWriter writer = WriteOpen(true);
        while ((item = file.ReadItemLine()) != null) {
            writer.write(String.join("\n", item) + "\n");
            ItemNum++;
        }
        file.ReadClose();
        this.WriteClose();
        System.out.println(new Date() + "\tEnd append " + file.getName() + " to " + getName());
    }

    public synchronized void Append(ArrayList List) throws IOException {
        BufferedWriter writer = WriteOpen(true);
        for (Object i : List) {
            writer.write(i.toString());
            writer.write("\n");
            ItemNum++;
        }
        WriteClose();
    }

    public synchronized void Append(String item) throws IOException {
        BufferedWriter writer = WriteOpen(true);
        writer.write(item);
        WriteClose();
        ItemNum++;
    }

    public void SortFile(AbstractFile OutFile, Comparator<E> comparator) throws IOException {
        System.out.println(new Date() + "\tSort file: " + getName());
        BufferedWriter outfile = OutFile.WriteOpen();
        ItemNum = 0;
        ReadOpen();
        E Item;
        ArrayList<E> SortList = new ArrayList<>();
        while ((Item = ReadItem()) != null) {
            SortList.add(Item);
            ItemNum++;
        }
        SortList.sort(comparator);
        for (int i = 0; i < SortList.size(); i++) {
            outfile.write(SortList.get(i).toString());
            outfile.write("\n");
            SortList.set(i, null);//及时去除，减少内存占用
        }
        outfile.close();
        ReadClose();
        Sorted = true;
        System.out.println(new Date() + "\tEnd sort file: " + getName());
    }

    public synchronized void MergeSortFile(AbstractFile<E>[] InFile, Comparator<E> comparator) throws IOException {
        ItemNum = 0;
        E[] Lines;
        System.out.print(new Date() + "\tMerge ");
        for (File s : InFile) {
            System.out.print(s.getName() + " ");
        }
        System.out.print("to " + getName() + "\n");
        //=========================================================================================
        LinkedList<E> SortList = new LinkedList<>();
        BufferedWriter writer = WriteOpen();
        if (InFile.length == 0) {
            return;
        }
        for (int i = 0; i < InFile.length; i++) {
            InFile[i].ReadOpen();
            E item = InFile[i].ReadItem();
            if (item != null) {
                item.serial = i;
                SortList.add(item);
            } else {
                InFile[i].ReadClose();
            }
        }
        SortList.sort(comparator);
        while (SortList.size() > 0) {
            E item = SortList.remove(0);
            int serial = item.serial;
            writer.write(item.toString());
            writer.write("\n");
            ItemNum++;
            item = InFile[serial].ReadItem();
            if (item == null) {
                continue;
            }
            item.serial = serial;
            Iterator<E> iterator = SortList.iterator();
            boolean flage = false;
            int i = 0;
            while (iterator.hasNext()) {
                E item1 = iterator.next();
                if (comparator.compare(item, item1) <= 0) {
                    SortList.add(i, item);
                    flage = true;
                    break;
                }
                i++;
            }
            if (!flage) {
                SortList.add(item);
            }
        }
        WriteClose();
        //============================================================================================
        System.out.print(new Date() + "\tEnd merge ");
        for (File s : InFile) {
            System.out.print(s.getName() + " ");
        }
        System.out.print("to " + getName() + "\n");
    }

    public synchronized void Merge(AbstractFile[] files) throws IOException {
        BufferedWriter writer = WriteOpen();
        ItemNum = 0;
        String[] lines;
        for (AbstractFile x : files) {
            System.out.println(new Date() + "\tMerge " + x.getName() + " to " + getName());
            x.ReadOpen();
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


    public ArrayList<CommonFile> SplitFile(String Prefix, long itemNum) throws IOException {
        int filecount = 0;
        int count = 0;
        CommonFile TempFile;
        String[] lines;
        ArrayList<CommonFile> Outfile = new ArrayList<>();
        this.ReadOpen();
        Outfile.add(TempFile = new CommonFile(Prefix + ".Split" + filecount));
        BufferedWriter outfile = TempFile.WriteOpen();
        while ((lines = ReadItemLine()) != null) {
            count++;
            if (count > itemNum) {
                TempFile.ItemNum = itemNum;
                outfile.close();
                filecount++;
                Outfile.add(TempFile = new CommonFile(Prefix + ".Split" + filecount));
                outfile = TempFile.WriteOpen();
                count = 1;
            }
            outfile.write(String.join("\n", lines) + "\n");
        }
        TempFile.ItemNum = count;
        outfile.close();
        this.ReadClose();
        return Outfile;
    }


    public boolean clean() {
        return clean(this);
    }

    public static boolean clean(File f) {
        try {
            BufferedWriter writer = new BufferedWriter(new FileWriter(f));
            writer.close();
        } catch (IOException e) {
            System.err.println("Warning! can't clean " + f.getPath());
            return false;
        }
        return true;
    }

    public static void delete(File f) {
        if (f.exists() && !f.delete()) {
            System.err.println("Warning! can't delete " + f.getPath());
        }
    }

//    public SortItem<E> ReadSortItem() throws IOException {
//        return ExtractSortItem(ReadItemLine());
//    }
//
//    protected abstract SortItem<E> ExtractSortItem(String[] s);

    public long getItemNum() {
        if (ItemNum <= 0) {
            try {
                CalculateItemNumber();
            } catch (IOException e) {
                System.err.println("Warning! can't get accurate item number, current item number: " + getName() + " " + ItemNum);
            }
        }
        return ItemNum;
    }

    public boolean isSorted() {
        return Sorted;
    }

    public void setBufferSize(int bufferSize) {
        BufferSize = bufferSize;
    }
}


