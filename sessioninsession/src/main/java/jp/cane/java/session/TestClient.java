package jp.cane.java.session;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;

import jp.cane.java.session.Sessions.CreateListener;

public class TestClient implements CreateListener {
    Sessions sessions = null;

    public TestClient() {
        try {
            ServerSocket ssocket = new ServerSocket(8888);
            Socket socket = ssocket.accept();
            ssocket.close();
            this.sessions = new Sessions(socket.getInputStream(),
                    socket.getOutputStream(), this);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] argv) {
        new TestClient();
    }

    @Override
    public void created(int port) {
        System.out.println("created");
        final Session session = this.sessions.getThatSession(port);
        new Thread(new Runnable() {
            @Override
            public void run() {
                InputStream is = session.getInputStream();
                OutputStream os = session.getOutputStream();
                session.start();
//                try {
//                    Thread.sleep(200);
//                } catch (InterruptedException e) {
//                }
                try {
                    Proxy.connect(is, os);
                } catch (IOException e) {
                    // e.printStackTrace();
                    try {
                        os.close();
                    } catch (IOException e1) {
                        e1.printStackTrace();
                    }
                    try {
                        is.close();
                    } catch (IOException e1) {
                        e1.printStackTrace();
                    }
                    session.receiveDown();
                }
            }
        }).start();
    }
}
