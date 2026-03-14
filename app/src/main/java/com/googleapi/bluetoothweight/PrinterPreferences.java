package com.googleapi.bluetoothweight;

import android.content.Context;
import android.content.SharedPreferences;
import android.hardware.usb.UsbDevice;

public class PrinterPreferences {
    private static final String PREF_NAME = "printer_prefs";
    private static final String KEY_LAST_PRINTER_NAME = "last_printer_name";
    private static final String KEY_LAST_PRINTER_VID = "last_printer_vid";
    private static final String KEY_LAST_PRINTER_PID = "last_printer_pid";
    private static final String KEY_PENDING_VID = "pending_vid";
    private static final String KEY_PENDING_PID = "pending_pid";
    private static final String KEY_PENDING_TIMESTAMP = "pending_timestamp";

    private static PrinterPreferences instance;
    private SharedPreferences prefs;

    private PrinterPreferences(Context context) {
        prefs = context.getApplicationContext().getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    public static synchronized PrinterPreferences getInstance(Context context) {
        if (instance == null) {
            instance = new PrinterPreferences(context);
        }
        return instance;
    }

    /**
     * Save last printer info
     */
    public void saveLastPrinterInfo(String printerName, int vid, int pid) {
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(KEY_LAST_PRINTER_NAME, printerName);
        editor.putInt(KEY_LAST_PRINTER_VID, vid);
        editor.putInt(KEY_LAST_PRINTER_PID, pid);
        editor.apply();
    }

    /**
     * Save last printer from UsbDevice
     */
    public void saveLastPrinter(UsbDevice printer) {
        if (printer != null) {
            saveLastPrinterInfo(
                    printer.getDeviceName(),
                    printer.getVendorId(),
                    printer.getProductId()
            );
        }
    }

    /**
     * Check if there is a last printer saved
     */
    public boolean hasLastPrinter() {
        return prefs.contains(KEY_LAST_PRINTER_VID) && prefs.contains(KEY_LAST_PRINTER_PID);
    }

    /**
     * Get last printer name
     */
    public String getLastPrinterName() {
        return prefs.getString(KEY_LAST_PRINTER_NAME, "Unknown Printer");
    }

    /**
     * Get last printer vendor ID
     */
    public int getLastPrinterVid() {
        return prefs.getInt(KEY_LAST_PRINTER_VID, -1);
    }

    /**
     * Get last printer product ID
     */
    public int getLastPrinterPid() {
        return prefs.getInt(KEY_LAST_PRINTER_PID, -1);
    }

    /**
     * Clear last printer
     */
    public void clearLastPrinter() {
        SharedPreferences.Editor editor = prefs.edit();
        editor.remove(KEY_LAST_PRINTER_NAME);
        editor.remove(KEY_LAST_PRINTER_VID);
        editor.remove(KEY_LAST_PRINTER_PID);
        editor.apply();
    }

    /**
     * Save pending connection (for after reboot)
     */
    public void savePendingConnection(int vid, int pid) {
        SharedPreferences.Editor editor = prefs.edit();
        editor.putInt(KEY_PENDING_VID, vid);
        editor.putInt(KEY_PENDING_PID, pid);
        editor.putLong(KEY_PENDING_TIMESTAMP, System.currentTimeMillis());
        editor.apply();
    }

    /**
     * Check if there is a pending connection
     */
    public boolean hasPendingConnection() {
        // Also check if the pending connection is not too old (e.g., within last 5 minutes)
        long timestamp = prefs.getLong(KEY_PENDING_TIMESTAMP, 0);
        boolean hasPending = prefs.contains(KEY_PENDING_VID) && prefs.contains(KEY_PENDING_PID);

        if (hasPending && (System.currentTimeMillis() - timestamp) > 300000) { // 5 minutes
            // Pending connection is too old, clear it
            clearPendingConnection();
            return false;
        }

        return hasPending;
    }

    /**
     * Get pending vendor ID
     */
    public int getPendingVid() {
        return prefs.getInt(KEY_PENDING_VID, -1);
    }

    /**
     * Get pending product ID
     */
    public int getPendingPid() {
        return prefs.getInt(KEY_PENDING_PID, -1);
    }

    /**
     * Clear pending connection
     */
    public void clearPendingConnection() {
        SharedPreferences.Editor editor = prefs.edit();
        editor.remove(KEY_PENDING_VID);
        editor.remove(KEY_PENDING_PID);
        editor.remove(KEY_PENDING_TIMESTAMP);
        editor.apply();
    }

    /**
     * Check if a device matches the pending connection
     */
    public boolean isPendingDevice(UsbDevice device) {
        if (device == null || !hasPendingConnection()) return false;
        return device.getVendorId() == getPendingVid() &&
                device.getProductId() == getPendingPid();
    }
}