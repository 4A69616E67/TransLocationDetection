package TLD;

import java.io.*;
import java.util.Date;
import java.util.HashMap;
import java.util.Hashtable;

public class Tools {
    public static void ThreadsWait(Thread[] T) {
        for (Thread t : T) {
            try {
                t.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            } catch (NullPointerException ignored) {
            }
        }
    }

    public static String ArraysToString(Object[] o) {
        String s = "";
        if (o != null) {
            for (Object i : o) {
                s += i + " ";
            }
            s = s.trim();
        }
        return s;
    }

    public static int[] CalculateBinSize(int[] ChrSize, int Resolution) {
        int[] ChrBinSize = new int[ChrSize.length];
        for (int i = 0; i < ChrSize.length; i++) {
            ChrBinSize[i] = ChrSize[i] / Resolution + 1;
        }
        return ChrBinSize;
    }

    public static Hashtable<String, Integer> ExtractChrSize(File ChrSizeFile) throws IOException {
        BufferedReader reader = new BufferedReader(new FileReader(ChrSizeFile));
        String Line;
        String[] Str;
        Hashtable<String, Integer> ChrSize = new Hashtable<>();
        while ((Line = reader.readLine()) != null) {
            Str = Line.split("\\s+");
            ChrSize.put(Str[0], Integer.parseInt(Str[1]));
        }
        reader.close();
        return ChrSize;
    }

    public static double UnitTrans(double Num, String PrimaryUint, String TransedUint) {
        String[] Unit = new String[]{"B", "b", "K", "k", "M", "m", "G", "g"};
        Double[] Value = new Double[]{1D, 1D, 1e3, 1e3, 1e6, 1e6, 1e9, 1e9};
        HashMap<String, Double> UnitMap = new HashMap<>();
        for (int i = 0; i < Unit.length; i++) {
            UnitMap.put(Unit[i], Value[i]);
        }
        return Num * UnitMap.get(PrimaryUint) / UnitMap.get(TransedUint);
    }

    public static int ExecuteCommandStr(String CommandStr, PrintWriter Out, PrintWriter Error) throws IOException, InterruptedException {
        int ExitValue;
        System.out.println(new Date() + "\t" + CommandStr);
        Process P = Runtime.getRuntime().exec(CommandStr);
        Thread OutThread = new Thread(new Runnable() {

            @Override
            public void run() {
                try {
                    String line;
                    BufferedReader bufferedReaderIn = new BufferedReader(new InputStreamReader(P.getInputStream()));
                    if (Out != null) {
                        while ((line = bufferedReaderIn.readLine()) != null) {
                            Out.print(line + "\n");
                            Out.flush();
                        }
                        bufferedReaderIn.close();
                    } else {
                        while (bufferedReaderIn.readLine() != null) ;
                        bufferedReaderIn.close();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
        Thread ErrThread = new Thread(new Runnable() {

            @Override
            public void run() {
                try {
                    String line;
                    BufferedReader bufferedReaderIn = new BufferedReader(new InputStreamReader(P.getErrorStream()));
                    if (Error != null) {
                        while ((line = bufferedReaderIn.readLine()) != null) {
                            Error.write(line + "\n");
                            Error.flush();
                        }
                        bufferedReaderIn.close();
                    } else {
                        while (bufferedReaderIn.readLine() != null) ;
                        bufferedReaderIn.close();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
        OutThread.start();
        ErrThread.start();
        OutThread.join();
        ErrThread.join();
        ExitValue = P.waitFor();
        return ExitValue;
    }
}
