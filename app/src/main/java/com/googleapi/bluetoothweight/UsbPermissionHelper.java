package com.googleapi.bluetoothweight;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.net.Uri;
import android.provider.Settings;
import android.widget.Toast;

import java.util.HashMap;

public class UsbPermissionHelper {

    private Context context;
    private UsbManager usbManager;

    public UsbPermissionHelper(Context context) {
        this.context = context;
        this.usbManager = (UsbManager) context.getSystemService(Context.USB_SERVICE);
    }

    /**
     * Check if we have permission for any printer
     */
    public boolean hasPrinterPermission() {
        HashMap<String, UsbDevice> deviceList = usbManager.getDeviceList();

        for (UsbDevice device : deviceList.values()) {
            if (isPrinterDevice(device) && usbManager.hasPermission(device)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Check if device is a printer
     */
    private boolean isPrinterDevice(UsbDevice device) {
        if (device.getVendorId() == 0x04A9) return true; // Canon
        if (device.getDeviceClass() == 7) return true;

        for (int i = 0; i < device.getInterfaceCount(); i++) {
            if (device.getInterface(i).getInterfaceClass() == 7) {
                return true;
            }
        }
        return false;
    }

    /**
     * Show system settings to manually grant permission
     */
    public void openAppSettings() {
        Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
        intent.setData(Uri.parse("package:" + context.getPackageName()));
        context.startActivity(intent);
    }

    /**
     * Show permission troubleshooting dialog
     */
    public void showPermissionTroubleshooting() {
        new AlertDialog.Builder(context)
                .setTitle("USB Permission Issues")
                .setMessage(
                        "If you're having trouble with USB permission:\n\n" +
                                "METHOD 1: Clear App Data\n" +
                                "1. Go to Settings → Apps → This App → Storage\n" +
                                "2. Tap 'Clear Data' and 'Clear Cache'\n" +
                                "3. Restart the app and reconnect USB\n\n" +

                                "METHOD 2: Reset USB Permissions\n" +
                                "1. Unplug the USB cable\n" +
                                "2. Go to Settings → Apps → This App\n" +
                                "3. Tap 'Uninstall'\n" +
                                "4. Restart your device\n" +
                                "5. Reinstall the app\n" +
                                "6. Connect printer and grant permission\n\n" +

                                "METHOD 3: Check USB Mode\n" +
                                "1. Make sure printer is powered ON\n" +
                                "2. Try a different USB cable\n" +
                                "3. Try a different USB port\n" +
                                "4. Ensure cable supports data transfer"
                )
                .setPositiveButton("Open App Settings", (dialog, which) -> {
                    openAppSettings();
                })
                .setNegativeButton("Close", null)
                .show();
    }
}