package com.googleapi.bluetoothweight;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.util.Log;
import android.widget.Toast;

public class UsbPermissionReceiver extends BroadcastReceiver {
    private static final String TAG = "UsbPermissionReceiver";
    public static final String ACTION_USB_PERMISSION = "com.googleapi.bluetoothweight.USB_PERMISSION";

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();

        if (ACTION_USB_PERMISSION.equals(action)) {
            synchronized (this) {
                UsbDevice device = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
                boolean permissionGranted = intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false);

                if (device != null) {
                    Log.d(TAG, "Permission response for device: " + device.getDeviceName() +
                            ", granted: " + permissionGranted);

                    // Always broadcast that we can try to connect regardless of permission result
                    Intent connectIntent = new Intent("USB_PRINTER_READY");
                    connectIntent.putExtra("device", device);
                    connectIntent.putExtra("permission_granted", permissionGranted);
                    context.sendBroadcast(connectIntent);

                    // Only show toast for actual denial if we're sure it's a problem
                    // But since it's connecting anyway, we'll be quiet
                    if (!permissionGranted) {
                        Log.d(TAG, "Permission not officially granted, but device may still work");
                        // Optional: Show a less alarming message
                        // Toast.makeText(context, "Printer connected (limited mode)", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(context, "Printer ready", Toast.LENGTH_SHORT).show();
                    }
                }
            }
        } else if (UsbManager.ACTION_USB_DEVICE_ATTACHED.equals(action)) {
            UsbDevice device = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
            if (device != null) {
                Log.d(TAG, "USB device attached: " + device.getDeviceName());
                Toast.makeText(context, "Printer connected", Toast.LENGTH_SHORT).show();
            }
        } else if (UsbManager.ACTION_USB_DEVICE_DETACHED.equals(action)) {
            UsbDevice device = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
            if (device != null) {
                Log.d(TAG, "USB device detached: " + device.getDeviceName());
                Toast.makeText(context, "Printer disconnected", Toast.LENGTH_SHORT).show();
            }
        }
    }
}