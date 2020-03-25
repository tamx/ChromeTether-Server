package jp.cane.java.session;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class TestServer {
    public static void main(String[] argv) {
        try {
            Socket socket = new Socket("localhost", 8888);
            Sessions sessions = new Sessions(socket.getInputStream(),
                    socket.getOutputStream(), null);
            ServerSocket proxy = new ServerSocket(8080);
            while (true) {
                Socket c = proxy.accept();
                int port = sessions.create();
                Session session = sessions.getThisSession(port);
                new Pipe(c.getInputStream(), session.getOutputStream());
                new Pipe(session.getInputStream(), c.getOutputStream());
                session.start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
