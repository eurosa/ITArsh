package com.googleapi.bluetoothweight;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;
import androidx.core.app.NotificationCompat;

public class BootService extends Service {

    private static final String TAG = "BootService";
    private static final String CHANNEL_ID = "boot_service_channel";
    private static final int NOTIFICATION_ID = 1001;

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "BootService created");

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            createNotificationChannel();
            startForeground(NOTIFICATION_ID, createNotification());
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "BootService onStartCommand");

        // Start MainActivity after a short delay
        new android.os.Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                try {
                    Intent activityIntent = new Intent(BootService.this, MainActivity.class);
                    activityIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    activityIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    startActivity(activityIntent);

                    Log.d(TAG, "MainActivity started from BootService");

                    // Stop service after starting activity
                    stopSelf();

                } catch (Exception e) {
                    Log.e(TAG, "Error starting activity from service: " + e.getMessage());
                    stopSelf();
                }
            }
        }, 2000); // 2 seconds delay

        return START_NOT_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "BootService destroyed");
    }

    @androidx.annotation.RequiresApi(api = Build.VERSION_CODES.O)
    private void createNotificationChannel() {
        NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID,
                "Boot Service Channel",
                NotificationManager.IMPORTANCE_LOW
        );
        channel.setDescription("Channel for boot service");

        NotificationManager manager = getSystemService(NotificationManager.class);
        if (manager != null) {
            manager.createNotificationChannel(channel);
        }
    }

    private Notification createNotification() {
        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Auto Launcher")
                .setContentText("Starting application...")
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .build();
    }
}