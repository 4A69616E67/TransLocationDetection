package File;

import Unit.SortItem;

import java.io.*;
import java.util.*;

public abstract class AbstractFile<E extends Comparable<E>> extends File {
    protected E Item;
    public long ItemNum;
    protected BufferedReader reader;
    protected BufferedWriter writer;

    public AbstractFile(String pathname) {
        super(pathname);
    }

    public AbstractFile(File file) {
        this(file.getPath());
    }

    public long CalculateItemNumber() throws IOException {
        if (!isFile()) {
            return 0;
        }
        long ItemNumber = 0;
        ReadOpen();
        while (ReadItemLine() != null) {
            ItemNumber++;
        }
        ReadClose();
        ItemNum = ItemNumber;
        return ItemNum;
    }

    public BufferedReader ReadOpen() throws IOException {
        reader = new BufferedReader(new FileReader(this));
        return reader;
    }

    public void ReadClose() throws IOException {
        reader.close();
    }

    public BufferedWriter WriteOpen() throws IOException {
        return WriteOpen(false);
    }

    public BufferedWriter WriteOpen(boolean append) throws IOException {
        writer = new BufferedWriter(new FileWriter(this, append));
        return writer;
    }

    public void WriteClose() throws IOException {
        writer.close();
    }

    protected abstract E ExtractItem(String s);

    public synchronized String ReadItemLine() throws IOException {
        return reader.readLine();
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

    public void Append(AbstractFile<E> file) throws IOException {
        System.out.println(new Date() + "\tAppend " + file.getName() + " to " + getName());
        String item;
        file.ReadOpen();
        this.WriteOpen(true);
        while ((item = file.ReadItemLine()) != null) {
            this.getWriter().write(item + "\n");
        }
        file.ReadClose();
        this.WriteClose();
        System.out.println(new Date() + "\tEnd append " + file.getName() + " to " + getName());
    }

    public void Append(String item) throws IOException {
        this.WriteOpen(true);
        this.getWriter().write(item);
        WriteClose();
    }

    public void SortFile(AbstractFile<E> OutFile) throws IOException {
        System.out.println(new Date() + "\tBegin to sort file " + getName());
        BufferedWriter outfile = OutFile.WriteOpen();
        long ItemCount = 0;
        ReadOpen();
        SortItem<E> sortItem;
        ArrayList<SortItem<E>> SortList = new ArrayList<>();
        while ((sortItem = ReadSortItem()) != null) {
            SortList.add(sortItem);
            ItemCount++;
        }
        ItemNum = ItemCount;
        Collections.sort(SortList);
        for (SortItem aSortList : SortList) {
            outfile.write(aSortList.getLines());
            outfile.write("\n");
        }
        SortList.clear();
        outfile.close();
        ReadClose();
        System.out.println(new Date() + "\tEnd sort file " + getName());
    }

    public void MergeSortFile(AbstractFile<E>[] InFile) throws IOException {
        System.out.print(new Date() + "\tMerge ");
        for (File s : InFile) {
            System.out.print(s.getName() + " ");
        }
        System.out.print("to " + getName() + "\n");
        //=========================================================================================
        LinkedList<SortItem<E>> SortList = new LinkedList<>();
        WriteOpen();
        if (InFile.length == 0) {
            return;
        }
        for (int i = 0; i < InFile.length; i++) {
            InFile[i].ReadOpen();
            SortItem<E> item = InFile[i].ReadSortItem();
            if (item != null) {
                item.serial = i;
                SortList.add(item);
            } else {
                InFile[i].ReadClose();
            }
        }
        Collections.sort(SortList);
        while (SortList.size() > 0) {
            SortItem<E> item = SortList.removeFirst();
            int serial = item.serial;
            this.getWriter().write(item.getLines());
            this.getWriter().write("\n");
            item = InFile[serial].ReadSortItem();
            if (item == null) {
                continue;
            }
            item.serial = serial;
            Iterator<SortItem<E>> iterator = SortList.iterator();
            boolean flage = false;
            int i = 0;
            while (iterator.hasNext()) {
                SortItem<E> item1 = iterator.next();
                if (item.compareTo(item1) <= 0) {
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

    public void Merge(AbstractFile[] File) throws IOException {
        this.WriteOpen();
        String line;
        for (AbstractFile x : File) {
            System.out.println(new Date() + "\tMerge " + x.getName() + " to " + getName());
            x.ReadOpen();
            while ((line = x.ReadItemLine()) != null) {
                this.getWriter().write(line + "\n");
            }
            x.ReadClose();
        }
        this.WriteClose();
        System.out.println(new Date() + "\tDone merge");
    }

    public ArrayList<CommonFile> SplitFile(String Prefix, long itemNum) throws IOException {
        int filecount = 0;
        int count = 0;
        String line;
        ArrayList<CommonFile> Outfile = new ArrayList<>();
        this.ReadOpen();
        Outfile.add(new CommonFile(Prefix + ".Split" + filecount));
        BufferedWriter outfile = Outfile.get(Outfile.size() - 1).WriteOpen();
        while ((line = ReadItemLine()) != null) {
            count++;
            if (count > itemNum) {
                outfile.close();
                filecount++;
                Outfile.add(new CommonFile(Prefix + ".Split" + filecount));
                outfile = Outfile.get(Outfile.size() - 1).WriteOpen();
                count = 1;
            }
            outfile.write(line + "\n");
        }
        outfile.close();
        this.ReadClose();
        return Outfile;
    }

    public E getItem() {
        return Item;
    }

    public abstract SortItem<E> ReadSortItem() throws IOException;


}


