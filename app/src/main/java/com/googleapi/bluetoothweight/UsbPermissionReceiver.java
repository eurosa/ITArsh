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

                if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
                    if (device != null) {
                        Log.d(TAG, "Permission granted for device: " + device.getDeviceName());
                        Toast.makeText(context, "USB permission granted", Toast.LENGTH_SHORT).show();

                        // Broadcast that permission was granted
                        Intent grantedIntent = new Intent("USB_PERMISSION_GRANTED");
                        grantedIntent.putExtra("device", device);
                        context.sendBroadcast(grantedIntent);
                    }
                } else {
                    Log.d(TAG, "Permission denied for device: " + device);
                    Toast.makeText(context, "USB permission denied", Toast.LENGTH_SHORT).show();
                }
            }
        } else if (UsbManager.ACTION_USB_DEVICE_ATTACHED.equals(action)) {
            UsbDevice device = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
            if (device != null) {
                Log.d(TAG, "USB device attached: " + device.getDeviceName());
                Toast.makeText(context, "USB printer connected", Toast.LENGTH_SHORT).show();
            }
        } else if (UsbManager.ACTION_USB_DEVICE_DETACHED.equals(action)) {
            UsbDevice device = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
            if (device != null) {
                Log.d(TAG, "USB device detached: " + device.getDeviceName());
                Toast.makeText(context, "USB printer disconnected", Toast.LENGTH_SHORT).show();
            }
        }
    }
}