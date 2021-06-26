package jp.cane.android.chrometether;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import jp.cane.java.session.Proxy;
import jp.cane.java.session.Session;
import jp.cane.java.session.Sessions;

/**
 * Created by tam on 1/16/16.
 */
public class MainThread implements Runnable, Sessions.CreateListener {
    Sessions sessions = null;
    private MainService context = null;
    private Thread thread = null;

    public MainThread(MainService context) {
        this.context = context;
    }

    public void start() {
        this.thread = new Thread(this);
        this.thread.start();
    }

    public void stop() {
        if (this.thread != null) {
            this.thread.interrupt();
            this.thread = null;
        }
    }

    @Override
    public void run() {
        BluetoothServerSocket serverSocket = null;
        try {
            System.out.println("start");
            BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
            serverSocket = adapter
                    .listenUsingInsecureRfcommWithServiceRecord("ChromeTether", BluetoothProtocols.HTTP_PROTOCOL_UUID);
            while (true) {
                BluetoothSocket socket = serverSocket.accept();
                try {
                    System.out.println("accepted");
                    this.sessions = new Sessions(new BufferedInputStream(socket.getInputStream(), 1024 * 1024),
                            new BufferedOutputStream(socket.getOutputStream(), 1024 * 1024), this);
                } catch (IOException e) {
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (serverSocket != null) {
                try {
                    serverSocket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            System.out.println("end");
            this.context.stopSelf();
        }
    }

    @Override
    public void created(int port) {
        // System.out.println("created");
        final Session session = this.sessions.getSession(port);
        new Thread(new Runnable() {
            @Override
            public void run() {
                InputStream is = session.getInputStream();
                OutputStream os = session.getOutputStream();
//                try {
//                    Thread.sleep(200);
//                } catch (InterruptedException e) {
//                }
                try {
                    Proxy.connect(is, os);
                } catch (IOException e) {
                    e.printStackTrace();
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
                }
            }
        }).start();
    }
}
