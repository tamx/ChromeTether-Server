package jp.cane.java.session;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;

public class Session implements Runnable {
    private Sessions sessions = null;
    private int port = 0;
    private boolean ready = false;
    private boolean up_alive = true;
    private PipedInputStream up = new PipedInputStream();
    private boolean down_alive = true;
    private PipedOutputStream down = new PipedOutputStream();
    private PipedInputStream down_p = null;
    private PipedOutputStream up_p = null;

    public Session(Sessions sessions, int port) {
        this.sessions = sessions;
        this.port = port;
        try {
            down_p = new PipedInputStream(down);
            up_p = new PipedOutputStream(up);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    protected void start() {
        this.ready = true;
        new Thread(this).start();
    }

    public InputStream getInputStream() {
        return this.down_p;
    }

    public OutputStream getOutputStream() {
        return this.up_p;
    }

    protected void writeDown(byte[] buffer, int offset, int count) {
        try {
            // System.out.println(new String(buffer, offset, count));
            this.down.write(buffer, offset, count);
            this.down.flush();
        } catch (IOException e) {
            // e.printStackTrace();
            closeDown();
        }
    }

    protected void closeDown() {
        this.down_alive = false;
        try {
            this.down.close();
            // System.out.println("close down: " + this.port);
        } catch (IOException e) {
            e.printStackTrace();
        }
        checkAlive();
    }

    public void closeUp() {
        this.up_alive = false;
        try {
            this.up.close();
//            System.out.println("close up: " + this.port);
        } catch (IOException e) {
            // e.printStackTrace();
        }
        try {
            this.sessions.send('F', this.port, new byte[0], 0, 0);
        } catch (Exception e) {
        }
        checkAlive();
    }

    private void checkAlive() {
        if (!this.up_alive && !this.down_alive) {
            this.sessions.deleteSession(this.port);
        }
    }

    @Override
    public void run() {
//        try {
//            Thread.sleep(200);
//        } catch (InterruptedException e) {
//        }
        try {
            byte[] buf = new byte[1000];
            InputStream is = this.up;
            while (this.up_alive) {
//                int available = this.up.available();
//                if (available == -1) {
//                    break;
//                } else if (available == 0) {
//                    available = buf.length;
//                }
//                available = Math.min(available, buf.length);
                int len = is.read(buf);
                // System.out.println("read len: " + len);
                if (len == -1) {
                    break;
                }
                this.sessions.send('D', this.port, buf, 0, len);
                Thread.yield();
            }
            closeUp();
        } catch (IOException e) {
            // e.printStackTrace();
            try {
                this.sessions.send('E', this.port, new byte[0], 0, 0);
            } catch (Exception e2) {
            }
        } finally {
            this.up_alive = false;
            checkAlive();
        }
    }
}
