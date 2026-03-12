package com.googleapi.bluetoothweight;

import android.content.Context;
import android.content.SharedPreferences;
import android.hardware.usb.UsbDevice;

public class PrinterPreferences {
    private static final String PREF_NAME = "printer_prefs";
    private static final String KEY_LAST_PRINTER_NAME = "last_printer_name";
    private static final String KEY_LAST_PRINTER_VID = "last_printer_vid";
    private static final String KEY_LAST_PRINTER_PID = "last_printer_pid";
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

    public void saveLastPrinter(UsbDevice printer) {
        if (printer != null) {
            SharedPreferences.Editor editor = preferences.edit();
            editor.putString(KEY_LAST_PRINTER_NAME, printer.getDeviceName());
            editor.putInt(KEY_LAST_PRINTER_VID, printer.getVendorId());
            editor.putInt(KEY_LAST_PRINTER_PID, printer.getProductId());
            editor.putBoolean(KEY_LAST_PRINTER_CONNECTED, true);
            editor.apply();
        }
    }

    public void saveLastPrinterInfo(String printerName, int vid, int pid) {
        SharedPreferences.Editor editor = preferences.edit();
        editor.putString(KEY_LAST_PRINTER_NAME, printerName);
        editor.putInt(KEY_LAST_PRINTER_VID, vid);
        editor.putInt(KEY_LAST_PRINTER_PID, pid);
        editor.putBoolean(KEY_LAST_PRINTER_CONNECTED, true);
        editor.apply();
    }

    public void clearLastPrinter() {
        SharedPreferences.Editor editor = preferences.edit();
        editor.putBoolean(KEY_LAST_PRINTER_CONNECTED, false);
        editor.apply();
    }

    public boolean hasLastPrinter() {
        return preferences.getBoolean(KEY_LAST_PRINTER_CONNECTED, false);
    }

    public String getLastPrinterName() {
        return preferences.getString(KEY_LAST_PRINTER_NAME, "");
    }

    public int getLastPrinterVid() {
        return preferences.getInt(KEY_LAST_PRINTER_VID, 0);
    }

    public int getLastPrinterPid() {
        return preferences.getInt(KEY_LAST_PRINTER_PID, 0);
    }
}