package com.googleapi.bluetoothweight.bluetooth;



import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.util.ArrayList;
import java.util.List;

public class BluetoothPermissionHelper {

    public static final int REQUEST_CODE_BLUETOOTH_PERMISSIONS = 1001;

    /**
     * Check if all required Bluetooth permissions are granted
     */
    public static boolean hasAllPermissions(Context context) {
        List<String> requiredPermissions = getRequiredPermissions(context);

        for (String permission : requiredPermissions) {
            if (ContextCompat.checkSelfPermission(context, permission)
                    != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }

        return true;
    }

    /**
     * Get list of required permissions based on Android version
     */
    public static List<String> getRequiredPermissions(Context context) {
        List<String> permissions = new ArrayList<>();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            // Android 12+
            permissions.add(Manifest.permission.BLUETOOTH_CONNECT);
            permissions.add(Manifest.permission.BLUETOOTH_SCAN);
            // Location is optional for Android 12+ if using neverForLocation flag
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            // Android 6-11
            permissions.add(Manifest.permission.ACCESS_FINE_LOCATION);
            permissions.add(Manifest.permission.ACCESS_COARSE_LOCATION);
        }

        // Legacy permissions for all versions
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
            permissions.add(Manifest.permission.BLUETOOTH);
            permissions.add(Manifest.permission.BLUETOOTH_ADMIN);
        }

        return permissions;
    }

    /**
     * Request missing permissions
     */
    public static void requestMissingPermissions(Activity activity, int requestCode) {
        List<String> permissionsToRequest = new ArrayList<>();
        List<String> requiredPermissions = getRequiredPermissions(activity);

        for (String permission : requiredPermissions) {
            if (ContextCompat.checkSelfPermission(activity, permission)
                    != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(permission);
            }
        }

        if (!permissionsToRequest.isEmpty()) {
            ActivityCompat.requestPermissions(
                    activity,
                    permissionsToRequest.toArray(new String[0]),
                    requestCode
            );
        }
    }

    /**
     * Check if permission results contain all granted permissions
     */
    public static boolean areAllPermissionsGranted(int[] grantResults) {
        if (grantResults == null || grantResults.length == 0) {
            return false;
        }

        for (int result : grantResults) {
            if (result != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }

        return true;
    }

    /**
     * Get rationale message for permissions
     */
    public static String getPermissionRationale(Context context) {
        StringBuilder rationale = new StringBuilder();
        rationale.append("This app requires Bluetooth permissions to:\n\n");

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            rationale.append("• Connect to Bluetooth devices (BLUETOOTH_CONNECT)\n");
            rationale.append("• Discover nearby Bluetooth devices (BLUETOOTH_SCAN)\n");
        } else {
            rationale.append("• Connect to Bluetooth devices\n");
            rationale.append("• Discover nearby Bluetooth devices\n");
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M &&
                Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
            rationale.append("\nLocation permission is required for Bluetooth device discovery.\n");
            rationale.append("This is a security requirement by Android.");
        }

        return rationale.toString();
    }
}