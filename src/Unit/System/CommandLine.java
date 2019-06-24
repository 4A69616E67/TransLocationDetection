package Unit.System;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;

/**
 * Created by snowf on 2019/6/15.
 */

public class CommandLine {
    /**
     * close the io stream when you redirect to a file
     */
    public static int run(String CommandStr, PrintWriter Out, PrintWriter Error) throws IOException, InterruptedException {
        int ExitValue;
//        Unit.System.out.println(new Date() + "\t" + CommandStr);
        Process P = Runtime.getRuntime().exec(CommandStr);
        Thread OutThread = new Thread(() -> {
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
                    while (true) {
                        if (bufferedReaderIn.readLine() == null) break;
                    }
                    bufferedReaderIn.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
        Thread ErrThread = new Thread(() -> {
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
                    while (true) {
                        if (bufferedReaderIn.readLine() == null) break;
                    }
                    bufferedReaderIn.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
        OutThread.start();
        ErrThread.start();
        OutThread.join();
        ErrThread.join();
        ExitValue = P.waitFor();
        return ExitValue;
    }

    /**
     * close the io stream when you redirect to a file
     */
    public static int run(String CommandStr) throws IOException, InterruptedException {
        return run(CommandStr, null, null);
    }
}
