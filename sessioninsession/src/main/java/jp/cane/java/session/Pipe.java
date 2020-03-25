package jp.cane.java.session;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class Pipe implements Runnable {
    private InputStream is = null;
    private OutputStream os = null;
    private byte[] initialCode = null;

    public Pipe(InputStream is, OutputStream os) {
        this(is, os, null);
    }

    public Pipe(InputStream is, OutputStream os, byte[] initialCode) {
        this.is = new BufferedInputStream(is);
        this.os = new BufferedOutputStream(os);
        this.initialCode = initialCode;
        new Thread(this).start();
    }

    @Override
    public void run() {
        try {
            if (this.initialCode != null) {
                os.write(this.initialCode);
                os.flush();
            }
            byte[] buf = new byte[1024];
            // System.out.println("is start.");
            while (true) {
                int len = is.read(buf);
                if (len == -1) {
                    break;
                }
//                System.out.write(len);
                os.write(buf, 0, len);
                os.flush();
                Thread.yield();
            }
        } catch (IOException e) {
//            e.printStackTrace();
        } finally {
            if (os != null) {
                try {
                    os.close();
                } catch (IOException e) {
//                    e.printStackTrace();
                }
            }
            if (is != null) {
                try {
                    is.close();
                } catch (IOException e) {
//                    e.printStackTrace();
                }
            }
            // System.out.println("is closed.");
        }
    }
}
