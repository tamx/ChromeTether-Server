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
    private HashMap<Integer, Session> thislist = new HashMap<Integer, Session>();
    private HashMap<Integer, Session> thatlist = new HashMap<Integer, Session>();

    public Sessions(InputStream is, OutputStream os, CreateListener listener) {
        this.is = new DataInputStream(is);
        this.os = new DataOutputStream(os);
        this.listener = listener;
        new Thread(this).start();
    }

    public Session getThisSession(int port) {
        Session session = this.thislist.get(port);
        return session;
    }

    public Session getThatSession(int port) {
        Session session = this.thatlist.get(port);
        return session;
    }

    public void deleteThisSession(int port) {
        this.thislist.remove(port);
    }

    public void deleteThatSession(int port) {
        this.thatlist.remove(port);
    }

    public int create() {
        Random random = new Random();
        random.setSeed(System.currentTimeMillis());
        int port = 0;
        synchronized (this.thislist) {
            while (port == 0 || this.thislist.containsKey(port)) {
                port = random.nextInt();
            }
            Session session = new Session(this, port, false);
            this.thislist.put(port, session);
        }
        try {
            send('O', port, new byte[0], 0, 0);
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

                // System.out.println("Code recv: " + ((char) code));
                switch (code) {
                    case -1:
                        return;
                    case 'O': {
                        Session session = new Session(this, port, true);
                        synchronized (this.thatlist) {
                            this.thatlist.put(port, session);
                        }
                        if (Sessions.this.listener != null) {
                            Sessions.this.listener.created(port);
                        }
                        // session.start();
                        // System.out.println("list size: " + this.thatlist.size());
                    }
                    break;
                    case 'U': {
                        Session session = getThatSession(port);
                        if (session != null) {
                            session.writeDown(buf, 0, length);
                        }
                    }
                    break;
                    case 'V': {
                        Session session = getThatSession(port);
                        if (session != null) {
                            session.closeDown();
                        }
                    }
                    break;
                    case 'D': {
                        Session session = getThisSession(port);
                        if (session != null) {
                            session.writeDown(buf, 0, length);
                        }
                    }
                    break;
                    case 'E': {
                        Session session = getThisSession(port);
                        if (session != null) {
                            session.closeDown();
                        }
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
