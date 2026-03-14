package com.googleapi.bluetoothweight;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.PowerManager;
import android.provider.Settings;
import android.util.Log;

public class AutoStartPermissionHelper {

    private static final String TAG = "AutoStartPermission";

    /**
     * Check if auto-start permission is enabled
     */
    public static boolean isAutoStartPermissionEnabled(Context context) {
        String manufacturer = Build.MANUFACTURER.toLowerCase();

        if (manufacturer.contains("xiaomi")) {
            return checkXiaomiAutoStart(context);
        } else if (manufacturer.contains("oppo") || manufacturer.contains("vivo")) {
            return checkOppoVivoAutoStart(context);
        } else if (manufacturer.contains("samsung")) {
            return checkSamsungAutoStart(context);
        } else if (manufacturer.contains("huawei")) {
            return checkHuaweiAutoStart(context);
        } else if (manufacturer.contains("oneplus")) {
            return checkOnePlusAutoStart(context);
        }

        return true; // Default for other manufacturers
    }

    /**
     * Request auto-start permission based on manufacturer
     */
    public static void requestAutoStartPermission(Context context) {
        String manufacturer = Build.MANUFACTURER.toLowerCase();

        Intent intent = new Intent();

        if (manufacturer.contains("xiaomi")) {
            intent.setClassName("com.miui.securitycenter",
                    "com.miui.permcenter.autostart.AutoStartManagementActivity");
        } else if (manufacturer.contains("oppo") || manufacturer.contains("vivo")) {
            intent.setClassName("com.coloros.safecenter",
                    "com.coloros.safecenter.permission.startup.StartupAppListActivity");
        } else if (manufacturer.contains("samsung")) {
            intent.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
            intent.setData(Uri.parse("package:" + context.getPackageName()));
        } else if (manufacturer.contains("huawei")) {
            intent.setClassName("com.huawei.systemmanager",
                    "com.huawei.systemmanager.startupmgr.ui.StartupNormalAppListActivity");
        } else if (manufacturer.contains("oneplus")) {
            intent.setClassName("com.oneplus.security",
                    "com.oneplus.security.chainlaunch.ChainLaunchAppListActivity");
        } else {
            // Default - open app settings
            intent.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
            intent.setData(Uri.parse("package:" + context.getPackageName()));
        }

        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        try {
            context.startActivity(intent);
        } catch (Exception e) {
            Log.e(TAG, "Error opening auto-start settings: " + e.getMessage());

            // Fallback to app settings
            Intent fallbackIntent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
            fallbackIntent.setData(Uri.parse("package:" + context.getPackageName()));
            fallbackIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(fallbackIntent);
        }
    }

    /**
     * Check battery optimization
     */
    public static boolean isBatteryOptimizationDisabled(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
            return pm.isIgnoringBatteryOptimizations(context.getPackageName());
        }
        return true;
    }

    /**
     * Request to disable battery optimization
     */
    public static void requestDisableBatteryOptimization(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Intent intent = new Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
            intent.setData(Uri.parse("package:" + context.getPackageName()));
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
        }
    }

    private static boolean checkXiaomiAutoStart(Context context) {
        // Implementation for Xiaomi
        return true; // Simplified
    }

    private static boolean checkOppoVivoAutoStart(Context context) {
        // Implementation for Oppo/Vivo
        return true; // Simplified
    }

    private static boolean checkSamsungAutoStart(Context context) {
        // Implementation for Samsung
        return true; // Simplified
    }

    private static boolean checkHuaweiAutoStart(Context context) {
        // Implementation for Huawei
        return true; // Simplified
    }

    private static boolean checkOnePlusAutoStart(Context context) {
        // Implementation for OnePlus
        return true; // Simplified
    }
}