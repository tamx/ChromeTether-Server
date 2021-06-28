package jp.cane.java.session;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Random;

public class Sessions implements Runnable {
    private DataInputStream is = null;
    private DataOutputStream os = null;
    private CreateListener listener = null;
    private HashMap<Integer, Session> portlist = new HashMap<Integer, Session>();

    public Sessions(InputStream is, OutputStream os, CreateListener listener) {
        this.is = new DataInputStream(is);
        this.os = new DataOutputStream(os);
        this.listener = listener;
        new Thread(this).start();
    }

    public Session getSession(int port) {
        Session session = this.portlist.get(port);
        return session;
    }

    public void deleteSession(int port) {
        this.portlist.remove(port);
    }

    public int create() {
        int port = 0;
        Random random = new Random();
        random.setSeed(System.currentTimeMillis());
        synchronized (this.portlist) {
            while (port == 0 || this.portlist.containsKey(port)) {
                port = random.nextInt();
            }
            Session session = new Session(this, port);
            this.portlist.put(port, session);
        }
        try {
            send('S', port, new byte[0], 0, 0);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return port;
    }

    public void send(int code, int port, byte[] data, int offset,
                     int length) throws IOException {
        // System.out.println("Code send: " + ((char) code));
        synchronized (this.os) {
            this.os.write(code);
            this.os.writeInt(port);
            this.os.writeInt(length);
            this.os.write(data, offset, length);
            this.os.flush();
        }
    }

    @Override
    public void run() {
        try {
            while (true) {
                int code = this.is.readByte();
                final int port = this.is.readInt();
                int length = 0xffff & this.is.readInt();
                byte[] buf = new byte[length];
                this.is.readFully(buf);

                Session session = new Session(this, port);
                if (session == null) {
                    if (code == 'S') {
                        synchronized (this.portlist) {
                            if (this.portlist.containsKey(port)) {
                                try {
                                    send('E', port, new byte[0], 0, 0);
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }
                                continue;
                            }
                            Session newsession = new Session(this, port);
                            this.portlist.put(port, newsession);
                            newsession.start();
                        }
                        try {
                            send('A', port, new byte[0], 0, 0);
                            if (this.listener != null) {
                                this.listener.created(port);
                            }
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    } else {
                        try {
                            send('E', port, new byte[0], 0, 0);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                    continue;
                }
                // System.out.println("Code recv: " + ((char) code));
                switch (code) {
                    case -1:
                        return;
                    case 'S': {
                        try {
                            send('E', port, new byte[0], 0, 0);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                    break;
                    case 'A': {
                        session.start();
                    }
                    break;
                    case 'D': {
                        session.writeDown(buf, 0, length);
                    }
                    break;
                    case 'F': {
                        session.closeDown();
                    }
                    break;
                    case 'E': {
                        deleteSession(port);
                        session.closeUp();
                        session.closeDown();
                    }
                    break;
                    default:
                        System.out.println("parse error!");
                        this.is.close();
                        return;
                }
                Thread.yield();
            }
        } catch (IOException e) {
//            e.printStackTrace();
        }
    }

    public interface CreateListener {
        void created(int port);
    }
}
