package jp.cane.java.session;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;

public class Session implements Runnable {
    private Sessions sessions = null;
    private int port = 0;
    private boolean that = false;
    private boolean up_alive = true;
    private PipedInputStream up = new PipedInputStream();
    private boolean down_alive = true;
    private PipedOutputStream down = new PipedOutputStream();
    private PipedInputStream down_p = null;
    private PipedOutputStream up_p = null;

    public Session(Sessions sessions, int port, boolean that) {
        this.sessions = sessions;
        this.port = port;
        this.that = that;
        try {
            down_p = new PipedInputStream(down);
            up_p = new PipedOutputStream(up);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void start() {
        new Thread(this).start();
    }

    public int getPort() {
        return this.port;
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
        try {
            this.down_alive = false;
            this.down.close();
            // System.out.println("close down: " + this.port);
        } catch (IOException e) {
            e.printStackTrace();
        }
        checkAlive();
    }

    public void receiveDown() {
        try {
            this.up_alive = false;
            this.up.close();
//            System.out.println("close up: " + this.port);
        } catch (IOException e) {
            // e.printStackTrace();
        }
        checkAlive();
    }

    private void checkAlive() {
        if (!this.up_alive && !this.down_alive) {
            if (this.that) {
                this.sessions.deleteThatSession(this.port);
            } else {
                this.sessions.deleteThisSession(this.port);
            }
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
                int code = this.that ? 'D' : 'U';
                this.sessions.send(code, this.port, buf, 0, len);
                Thread.yield();
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                int code = this.that ? 'E' : 'V';
                this.sessions.send(code, this.port, new byte[0], 0, 0);
            } catch (Exception e) {
            }
        }
    }
}
