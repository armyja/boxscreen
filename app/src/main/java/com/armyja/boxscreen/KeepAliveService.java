package com.armyja.boxscreen;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;

/**
 * Lightweight foreground service that keeps BoxScreen out of Android/OEM cached app
 * freezing while an external display Presentation is active, without holding wake locks
 * or running background work.
 */
public final class KeepAliveService extends Service {
    private static final String TAG = "BoxScreen";
    private static final String CHANNEL_ID = "boxscreen_output_keepalive";
    private static final int NOTIFICATION_ID = 1001;

    static void start(Context context) {
        if (context == null) return;
        Context app = context.getApplicationContext();
        try {
            app.startForegroundService(new Intent(app, KeepAliveService.class));
        } catch (RuntimeException e) {
            Log.w(TAG, "unable to start keep-alive foreground service", e);
        }
    }

    static void stop(Context context) {
        if (context == null) return;
        Context app = context.getApplicationContext();
        try {
            app.stopService(new Intent(app, KeepAliveService.class));
        } catch (RuntimeException e) {
            Log.w(TAG, "unable to stop keep-alive foreground service", e);
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        promoteToForeground();
        return START_NOT_STICKY;
    }

    @Override
    public void onTaskRemoved(Intent rootIntent) {
        stopForeground(STOP_FOREGROUND_REMOVE);
        stopSelf();
        super.onTaskRemoved(rootIntent);
    }

    @Override
    public void onDestroy() {
        stopForeground(STOP_FOREGROUND_REMOVE);
        super.onDestroy();
    }

    private void promoteToForeground() {
        ensureNotificationChannel();
        Intent launch = new Intent(this, MainActivity.class)
                .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent contentIntent = PendingIntent.getActivity(
                this,
                0,
                launch,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        Notification.Builder builder = new Notification.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_status_monitor)
                .setContentTitle("BoxScreen 外接屏运行中")
                .setContentText("保持外接屏与鼠标响应，点击返回控制面板")
                .setOngoing(true)
                .setOnlyAlertOnce(true)
                .setCategory(Notification.CATEGORY_SERVICE)
                .setContentIntent(contentIntent);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            builder.setForegroundServiceBehavior(Notification.FOREGROUND_SERVICE_IMMEDIATE);
        }
        Notification notification = builder.build();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(NOTIFICATION_ID, notification,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE);
        } else {
            startForeground(NOTIFICATION_ID, notification);
        }
    }

    private void ensureNotificationChannel() {
        NotificationManager manager = getSystemService(NotificationManager.class);
        if (manager == null || manager.getNotificationChannel(CHANNEL_ID) != null) return;
        NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID,
                "外接屏后台保活",
                NotificationManager.IMPORTANCE_LOW);
        channel.setDescription("外接显示器投屏期间防止系统冻结虚拟屏与鼠标输入");
        channel.setShowBadge(false);
        channel.enableLights(false);
        channel.enableVibration(false);
        channel.setSound(null, null);
        manager.createNotificationChannel(channel);
    }
}
