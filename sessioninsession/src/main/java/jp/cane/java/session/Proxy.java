package jp.cane.java.session;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.Inet4Address;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URL;
import java.net.UnknownHostException;

public class Proxy implements Runnable {
    public static String cookie = "";
    private ServerSocket ssocket = null;

    public Proxy(int port) {
        try {
            this.ssocket = new ServerSocket(port, 0,
                    Inet4Address.getByName("localhost"));
        } catch (UnknownHostException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        new Thread(this).start();
    }

    private static void debug(String msg) {
        if (msg != null) {
            System.out.println(msg.replaceAll("\r\n", "\n"));
        }
    }

    public static void connect(InputStream is, OutputStream os) throws IOException {
        int back_char = -1;
        try {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            while (true) {
                int a = is.read();
                if (a == -1) {
                    break;
                }
                // System.out.println("" + (char) a + ":" + a);
                bos.write(a);
                if (back_char == '\n' && (a == '\r' || a == '\n')) {
                    if (a == '\r') {
                        a = is.read();
                        bos.write(a);
                    }
                    // System.out.println("" + (char) a + ":" + a);
                    break;
                }
                back_char = a;
            }
            bos.flush();
            final BufferedReader reader = new BufferedReader(
                    new InputStreamReader(new ByteArrayInputStream(
                            bos.toByteArray())));
            String line = reader.readLine();
            // debug(line);
            String command = "";
            while (true) {
                String str = reader.readLine();
                if (str == null || str.length() == 0) {
                    break;
                }
                // debug(str);
                // if (str.startsWith("Connection:")) {
                //     // command += "Connection: Close\r\n";
                //     continue;
                // }
                if (str.startsWith("Proxy-Connection:")) {
                    continue;
                }
                command += str + "\r\n";
            }
            reader.close();
            // debug(line);
            // debug(command);
            if (line == null) {
                System.out.println("<===");
                System.out.write(bos.toByteArray());
                System.out.println("===>");
                // new Pipe(is, System.out);
                throw new IOException();
            } else if (line.startsWith("CONNECT")) {
                String[] param1 = line.split("\\s");
                String[] param2 = param1[1].split(":");
                String host = param2[0];
                int port = Integer.valueOf(param2[1]);
                // System.out.println("Socket HOST CONNECT: " + host + ":" + port);
                Socket c = null;
                c = new Socket(host, port);
                new Pipe(is, c.getOutputStream());
                new Pipe(c.getInputStream(), os, ("HTTP/1.1 200 OK\r\n\r\n").getBytes());
            } else {
//                if (line.startsWith("GET http://")) {
//                    line = line.substring("GET http://".length()
//                            + host.length());
//                    line = "GET " + line;
//                } else if (line.startsWith("POST http://")) {
//                    line = line.substring("POST http://".length()
//                            + host.length());
//                    line = "POST " + line;
//                }
                URL url = new URL(line.split("\\s")[1]);
                String host = url.getHost();
                int port = url.getPort();
                if (port == -1) {
                    port = 80;
                }
                // System.out.println("Socket HOST: " + host + ":" + port);
                Socket c = null;
                c = new Socket(host, port);
                c.getOutputStream().write((line + "\r\n").getBytes());
//					c.getOutputStream().write(
//							("Cookie: " + cookie + "\n").getBytes());
                c.getOutputStream().write((command).getBytes());
                // c.getOutputStream().write(("Connection: Close\r\n").getBytes());
                c.getOutputStream().write(("\r\n").getBytes());
                c.getOutputStream().flush();
                new Pipe(is, c.getOutputStream());
                new Pipe(c.getInputStream(), os);
            }
        } catch (IOException e) {
            e.printStackTrace();
//            try {
//                is.close();
//            } catch (IOException e1) {
//                e1.printStackTrace();
//            }
//            try {
//                os.close();
//            } catch (IOException e1) {
//                e1.printStackTrace();
//            }
        }
        // System.out.println("Thread: " + Thread.activeCount());
    }

    @Override
    public void run() {
        try {
            while (true) {
                final Socket socket = this.ssocket.accept();
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        InputStream is = null;
                        OutputStream os = null;
                        try {
                            is = socket.getInputStream();
                            os = socket.getOutputStream();
                            Proxy.connect(is, os);
                        } catch (IOException e) {
                            // e.printStackTrace();
                            try {
                                os.close();
                            } catch (IOException e1) {
//                                e1.printStackTrace();
                            }
                            try {
                                is.close();
                            } catch (IOException e1) {
//                                e1.printStackTrace();
                            }
                        }
                    }
                }).start();
            }
        } catch (IOException e1) {
//            e1.printStackTrace();
        } finally {
            if (this.ssocket != null) {
                try {
                    this.ssocket.close();
                } catch (IOException e) {
//                    e.printStackTrace();
                }
            }
        }
    }
}
