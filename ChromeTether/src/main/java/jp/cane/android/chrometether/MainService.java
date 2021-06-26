package jp.cane.android.chrometether;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
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
        String channel_id = "jp.cane.android.chrometether";
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(channel_id,
                    "AutoPAN", NotificationManager.IMPORTANCE_NONE);
            NotificationManager notificationManager = (NotificationManager)
                    getSystemService(Context.NOTIFICATION_SERVICE);
            notificationManager.createNotificationChannel(channel);

            Notification notification = new Notification.Builder(this, channel_id)
                    .setContentTitle(getResources().getString(R.string.app_name))
                    .setSmallIcon(R.mipmap.ic_launcher)
                    .setPriority(Notification.PRIORITY_MIN)
                    .build();
            startForeground(1, notification);
        } else {
            Notification notification = new Notification.Builder(this)
                    .setContentTitle(getResources().getString(R.string.app_name))
                    .setSmallIcon(R.mipmap.ic_launcher)
                    .setPriority(Notification.PRIORITY_MIN)
                    .build();
            startForeground(1, notification);
        }
        this.mainThread = new MainThread(this);
        this.mainThread.start();
    }

    @Override
    public void onDestroy() {
        this.mainThread.stop();
        super.onDestroy();
    }
}
