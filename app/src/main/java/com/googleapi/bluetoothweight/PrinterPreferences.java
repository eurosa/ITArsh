package com.googleapi.bluetoothweight;

import android.content.Context;
import android.content.SharedPreferences;
import android.hardware.usb.UsbDevice;

public class PrinterPreferences {
    private static final String PREF_NAME = "printer_prefs";
    private static final String KEY_LAST_PRINTER_TYPE = "last_printer_type";
    private static final String KEY_LAST_PRINTER_NAME = "last_printer_name";
    private static final String KEY_LAST_PRINTER_VID = "last_printer_vid";
    private static final String KEY_LAST_PRINTER_PID = "last_printer_pid";
    private static final String KEY_LAST_WIFI_PRINTER_IP = "last_wifi_printer_ip";
    private static final String KEY_LAST_WIFI_PRINTER_NAME = "last_wifi_printer_name";
    private static final String KEY_LAST_PRINTER_CONNECTED = "last_printer_connected";

    private SharedPreferences preferences;
    private static PrinterPreferences instance;

    private PrinterPreferences(Context context) {
        preferences = context.getApplicationContext().getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    public static synchronized PrinterPreferences getInstance(Context context) {
        if (instance == null) {
            instance = new PrinterPreferences(context);
        }
        return instance;
    }

    // Save USB printer with device
    public void saveLastPrinter(UsbDevice printer) {
        if (printer != null) {
            SharedPreferences.Editor editor = preferences.edit();
            editor.putString(KEY_LAST_PRINTER_TYPE, "USB");
            editor.putString(KEY_LAST_PRINTER_NAME, printer.getDeviceName());
            editor.putInt(KEY_LAST_PRINTER_VID, printer.getVendorId());
            editor.putInt(KEY_LAST_PRINTER_PID, printer.getProductId());
            editor.putBoolean(KEY_LAST_PRINTER_CONNECTED, true);
            editor.apply();
        }
    }

    // Save USB printer info (existing method)
    public void saveLastPrinterInfo(String printerName, int vid, int pid) {
        SharedPreferences.Editor editor = preferences.edit();
        editor.putString(KEY_LAST_PRINTER_TYPE, "USB");
        editor.putString(KEY_LAST_PRINTER_NAME, printerName);
        editor.putInt(KEY_LAST_PRINTER_VID, vid);
        editor.putInt(KEY_LAST_PRINTER_PID, pid);
        editor.putBoolean(KEY_LAST_PRINTER_CONNECTED, true);
        editor.apply();
    }

    // Save USB printer with device and name (new method)
    public void saveLastUsbPrinter(UsbDevice printer, String printerName) {
        SharedPreferences.Editor editor = preferences.edit();
        editor.putString(KEY_LAST_PRINTER_TYPE, "USB");
        editor.putString(KEY_LAST_PRINTER_NAME, printerName);
        editor.putInt(KEY_LAST_PRINTER_VID, printer.getVendorId());
        editor.putInt(KEY_LAST_PRINTER_PID, printer.getProductId());
        editor.putBoolean(KEY_LAST_PRINTER_CONNECTED, true);
        editor.apply();
    }

    // Save USB printer info (new method - alias for saveLastPrinterInfo)
    public void saveLastUsbPrinterInfo(String printerName, int vid, int pid) {
        saveLastPrinterInfo(printerName, vid, pid);
    }

    // Save WiFi printer
    public void saveLastWifiPrinter(String ipAddress, String printerName) {
        SharedPreferences.Editor editor = preferences.edit();
        editor.putString(KEY_LAST_PRINTER_TYPE, "WIFI");
        editor.putString(KEY_LAST_PRINTER_NAME, printerName);
        editor.putString(KEY_LAST_WIFI_PRINTER_IP, ipAddress);
        editor.putString(KEY_LAST_WIFI_PRINTER_NAME, printerName);
        editor.putBoolean(KEY_LAST_PRINTER_CONNECTED, true);
        editor.apply();
    }

    // Clear last printer
    public void clearLastPrinter() {
        SharedPreferences.Editor editor = preferences.edit();
        editor.putBoolean(KEY_LAST_PRINTER_CONNECTED, false);
        editor.apply();
    }

    // Check if last printer exists
    public boolean hasLastPrinter() {
        return preferences.getBoolean(KEY_LAST_PRINTER_CONNECTED, false);
    }

    // Get last printer type (USB or WIFI)
    public String getLastPrinterType() {
        return preferences.getString(KEY_LAST_PRINTER_TYPE, "");
    }

    // Get last printer name
    public String getLastPrinterName() {
        return preferences.getString(KEY_LAST_PRINTER_NAME, "");
    }

    // Get last printer VID
    public int getLastPrinterVid() {
        return preferences.getInt(KEY_LAST_PRINTER_VID, 0);
    }

    // Get last printer PID
    public int getLastPrinterPid() {
        return preferences.getInt(KEY_LAST_PRINTER_PID, 0);
    }

    // Get last WiFi printer IP
    public String getLastWifiPrinterIp() {
        return preferences.getString(KEY_LAST_WIFI_PRINTER_IP, "");
    }

    // Get last WiFi printer name
    public String getLastWifiPrinterName() {
        return preferences.getString(KEY_LAST_WIFI_PRINTER_NAME, "");
    }
}