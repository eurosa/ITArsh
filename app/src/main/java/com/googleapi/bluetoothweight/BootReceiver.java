package com.googleapi.bluetoothweight;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

public class BootReceiver extends BroadcastReceiver {

    private static final String TAG = "BootReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        Log.d(TAG, "Boot completed with action: " + action);

        // Use string literal for QUICKBOOT_POWERON since it's not in SDK
        if (Intent.ACTION_BOOT_COMPLETED.equals(action) ||
                "android.intent.action.QUICKBOOT_POWERON".equals(action) ||
                Intent.ACTION_REBOOT.equals(action) ||
                "android.intent.action.REBOOT".equals(action)) {

            Log.d(TAG, "System reboot detected - starting application");

            try {
                // Method 1: Start MainActivity directly
                startMainActivity(context);

                // Method 2: Start a foreground service (optional)
                startBootService(context);

                // Method 3: Schedule a delayed start (for Android 12+)
                scheduleDelayedStart(context);

            } catch (Exception e) {
                Log.e(TAG, "Error starting app after boot: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }

    /**
     * Method 1: Directly start MainActivity
     */
    private void startMainActivity(Context context) {
        Intent intent = new Intent(context, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);

        // For Android 12+ (API 31+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            // For Android 12+, we need to use PendingIntent
            // This is handled by the alarm manager method
            Log.d(TAG, "Android 12+ detected - using alarm manager for startup");
        } else {
            context.startActivity(intent);
            Log.d(TAG, "MainActivity started after boot");
        }
    }

    /**
     * Method 2: Start a foreground service
     */
    private void startBootService(Context context) {
        Intent serviceIntent = new Intent(context, BootService.class);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(serviceIntent);
        } else {
            context.startService(serviceIntent);
        }

        Log.d(TAG, "BootService started");
    }

    /**
     * Method 3: Schedule delayed start for Android 12+
     */
    @SuppressLint("ScheduleExactAlarm")
    private void scheduleDelayedStart(Context context) {
        // Using AlarmManager for delayed start
        android.app.AlarmManager alarmManager = (android.app.AlarmManager)
                context.getSystemService(Context.ALARM_SERVICE);

        Intent intent = new Intent(context, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);

        android.app.PendingIntent pendingIntent = android.app.PendingIntent.getActivity(
                context,
                0,
                intent,
                android.app.PendingIntent.FLAG_UPDATE_CURRENT |
                        android.app.PendingIntent.FLAG_IMMUTABLE
        );

        long triggerTime = System.currentTimeMillis() + 5000; // 5 seconds delay

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(
                        android.app.AlarmManager.RTC_WAKEUP,
                        triggerTime,
                        pendingIntent
                );
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                alarmManager.setExact(
                        android.app.AlarmManager.RTC_WAKEUP,
                        triggerTime,
                        pendingIntent
                );
            } else {
                alarmManager.set(
                        android.app.AlarmManager.RTC_WAKEUP,
                        triggerTime,
                        pendingIntent
                );
            }

            Log.d(TAG, "Delayed start scheduled for 5 seconds");
        } catch (SecurityException e) {
            Log.e(TAG, "SecurityException scheduling alarm: " + e.getMessage());
            // Fallback to starting activity directly if alarm fails
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
                context.startActivity(intent);
            }
        }
    }
}