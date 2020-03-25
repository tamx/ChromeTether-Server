package jp.cane.android.chrometether;

import android.app.Notification;
import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

/**
 * Created by tam on 16/06/11.
 */
public class MainService extends Service {
    private MainThread mainThread = null;

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Notification notification = new Notification.Builder(this)
                .setContentTitle(getResources().getString(R.string.app_name))
                .setSmallIcon(R.drawable.ic_stat_name)
                .setPriority(Notification.PRIORITY_MIN)
                .build();
        startForeground(1, notification);
        this.mainThread = new MainThread(this);
        this.mainThread.start();
    }

    @Override
    public void onDestroy() {
        this.mainThread.stop();
        super.onDestroy();
    }
}
