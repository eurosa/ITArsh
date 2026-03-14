package com.googleapi.bluetoothweight;

import android.app.AlertDialog;
import android.app.PendingIntent;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;


import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class PrinterMenuHelper {

    private AppCompatActivity activity;
    private PrinterManager printerManager;
    private PrinterDialogCallback callback;
    private ProgressDialog progressDialog;
    private Handler mainHandler = new Handler(Looper.getMainLooper());
    private ExecutorService executor = Executors.newSingleThreadExecutor();

    public static final String ACTION_USB_PERMISSION = "com.googleapi.bluetoothweight.USB_PERMISSION";
    public static final String ACTION_USB_PERMISSION_GRANTED = "USB_PERMISSION_GRANTED";

    public static final int MENU_PRINTER_SETTINGS = 2001;
    public static final int MENU_TEST_PRINT = 2002;
    public static final int MENU_DISCONNECT = 2003;

    // Broadcast receiver for USB permission granted events
    private BroadcastReceiver usbPermissionGrantedReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (ACTION_USB_PERMISSION_GRANTED.equals(intent.getAction())) {
                UsbDevice device = intent.getParcelableExtra("device");
                if (device != null) {
                    Log.d("PrinterMenuHelper", "Permission granted received, connecting to printer");

                    // Check if this is a pending connection
                    PrinterPreferences prefs = PrinterPreferences.getInstance(activity);
                    if (prefs.hasPendingConnection()) {
                        connectToPrinter(device, true);
                        prefs.clearPendingConnection();
                    }
                }
            }
        }
    };

    public interface PrinterDialogCallback {
        void onPrinterSelected(UsbDevice printer);
        void onPrinterDisconnected();
        void onTestPrint();
    }

    public PrinterMenuHelper(AppCompatActivity activity, PrinterDialogCallback callback) {
        this.activity = activity;
        this.callback = callback;
        this.printerManager = PrinterManager.getInstance();

        // Register for permission granted broadcasts
        registerPermissionReceiver();
    }

    private void registerPermissionReceiver() {
        IntentFilter filter = new IntentFilter(ACTION_USB_PERMISSION_GRANTED);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            activity.registerReceiver(usbPermissionGrantedReceiver, filter, Context.RECEIVER_NOT_EXPORTED);
        } else {
            activity.registerReceiver(usbPermissionGrantedReceiver, filter);
        }
    }

    public void createPrinterMenu(Menu menu) {
        MenuItem printerItem = menu.add(0, MENU_PRINTER_SETTINGS, 100, "🖨️ Printer Settings");
        printerItem.setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);

        MenuItem testPrintItem = menu.add(0, MENU_TEST_PRINT, 101, "🖨️ Test Print");
        testPrintItem.setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);

        MenuItem disconnectItem = menu.add(0, MENU_DISCONNECT, 102, "🔌 Disconnect Printer");
        disconnectItem.setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);
    }

    public void updateMenuItems(Menu menu) {
        MenuItem printerItem = menu.findItem(MENU_PRINTER_SETTINGS);
        MenuItem disconnectItem = menu.findItem(MENU_DISCONNECT);
        MenuItem testPrintItem = menu.findItem(MENU_TEST_PRINT);

        if (printerManager.isPrinterConnected()) {
            if (printerItem != null) {
                printerItem.setTitle("🖨️ " + printerManager.getConnectedPrinterName());
            }
            if (disconnectItem != null) {
                disconnectItem.setVisible(true);
            }
            if (testPrintItem != null) {
                testPrintItem.setVisible(true);
            }
        } else {
            if (printerItem != null) {
                printerItem.setTitle("🖨️ Printer Settings");
            }
            if (disconnectItem != null) {
                disconnectItem.setVisible(false);
            }
            if (testPrintItem != null) {
                testPrintItem.setVisible(false);
            }
        }
    }

    /**
     * Try to auto-connect to the last used printer
     * Call this when the activity starts/resumes
     */
    public void tryAutoConnectLastPrinter() {
        PrinterPreferences prefs = PrinterPreferences.getInstance(activity);

        if (!prefs.hasLastPrinter()) {
            Log.d("PrinterMenuHelper", "No last printer to auto-connect");
            return;
        }

        String lastPrinterName = prefs.getLastPrinterName();
        int lastVid = prefs.getLastPrinterVid();
        int lastPid = prefs.getLastPrinterPid();

        Log.d("PrinterMenuHelper", "Attempting to auto-connect to last printer: " + lastPrinterName +
                " (VID: 0x" + Integer.toHexString(lastVid) + ", PID: 0x" + Integer.toHexString(lastPid) + ")");

        showProgressDialog("Auto-connecting to last printer...");

        // First, check if the printer is still connected
        UsbManager usbManager = (UsbManager) activity.getSystemService(Context.USB_SERVICE);
        HashMap<String, UsbDevice> deviceList = usbManager.getDeviceList();

        UsbDevice targetDevice = null;

        for (UsbDevice device : deviceList.values()) {
            if (device.getVendorId() == lastVid && device.getProductId() == lastPid) {
                targetDevice = device;
                break;
            }
        }

        if (targetDevice == null) {
            dismissProgressDialog();
            Log.d("PrinterMenuHelper", "Last printer not found in connected devices");
            Toast.makeText(activity, "Last printer not found. Please reconnect.", Toast.LENGTH_LONG).show();
            return;
        }

        // Check permission first
        if (!usbManager.hasPermission(targetDevice)) {
            Log.d("PrinterMenuHelper", "No permission for last printer, requesting...");

            // Save as pending connection
            prefs.savePendingConnection(lastVid, lastPid);

            // Request permission
            requestPermission(targetDevice);
            return;
        }

        // Connect to the found device
        connectToPrinter(targetDevice, true);
    }

    /**
     * Handle printer connection after reboot
     */
    public void handlePrinterConnectionAfterReboot() {
        PrinterPreferences prefs = PrinterPreferences.getInstance(activity);

        if (!prefs.hasLastPrinter()) {
            Log.d("PrinterMenuHelper", "No last printer to handle after reboot");
            return;
        }

        int lastVid = prefs.getLastPrinterVid();
        int lastPid = prefs.getLastPrinterPid();

        UsbManager usbManager = (UsbManager) activity.getSystemService(Context.USB_SERVICE);
        HashMap<String, UsbDevice> deviceList = usbManager.getDeviceList();

        for (UsbDevice device : deviceList.values()) {
            if (device.getVendorId() == lastVid && device.getProductId() == lastPid) {
                // Found the printer
                if (usbManager.hasPermission(device)) {
                    // Has permission, connect directly
                    Log.d("PrinterMenuHelper", "Printer found with permission after reboot, connecting...");
                    connectToPrinter(device, true);
                } else {
                    // No permission, request it
                    Log.d("PrinterMenuHelper", "Printer found but no permission after reboot, requesting...");

                    showProgressDialog("Requesting printer permission...");

                    // Save pending connection
                    prefs.savePendingConnection(lastVid, lastPid);

                    // Request permission
                    requestPermission(device);
                }
                break;
            }
        }
    }

    /**
     * Request USB permission for a device
     */
    private void requestPermission(UsbDevice device) {
        UsbManager usbManager = (UsbManager) activity.getSystemService(Context.USB_SERVICE);
        PendingIntent permissionIntent = PendingIntent.getBroadcast(
                activity,
                0,
                new Intent(ACTION_USB_PERMISSION),
                PendingIntent.FLAG_IMMUTABLE
        );
        usbManager.requestPermission(device, permissionIntent);

        // Dismiss progress after a timeout (permission dialog will show)
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            dismissProgressDialog();
        }, 5000);
    }

    public boolean handlePrinterMenuItem(MenuItem item) {
        int itemId = item.getItemId();

        if (itemId == MENU_PRINTER_SETTINGS) {
            showPrinterSettingsDialog();
            return true;
        } else if (itemId == MENU_TEST_PRINT) {
            showTestPrintDialog();
            return true;
        } else if (itemId == MENU_DISCONNECT) {
            confirmDisconnectPrinter();
            return true;
        }

        return false;
    }

    private void showPrinterSettingsDialog() {
        String[] options;
        if (printerManager.isPrinterConnected()) {
            options = new String[]{
                    "Scan for Printers",
                    "Printer Status",
                    "Diagnose Connection",
                    "List USB Devices",
                    "Request Permission Again",
                    "Disconnect Printer"
            };
        } else {
            options = new String[]{
                    "Scan for Printers",
                    "Diagnose Connection",
                    "List USB Devices",
                    "Request Permission Again"
            };
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        builder.setTitle("Printer Settings")
                .setItems(options, (dialog, which) -> {
                    switch (which) {
                        case 0:
                            scanForPrinters();
                            break;
                        case 1:
                            if (printerManager.isPrinterConnected()) {
                                showPrinterStatus();
                            } else {
                                diagnosePrinter();
                            }
                            break;
                        case 2:
                            diagnosePrinter();
                            break;
                        case 3:
                            checkUsbDevices();
                            break;
                        case 4:
                            requestPermissionAgain();
                            break;
                        case 5:
                            if (printerManager.isPrinterConnected()) {
                                confirmDisconnectPrinter();
                            }
                            break;
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    /**
     * Request permission again for the last known printer
     */
    private void requestPermissionAgain() {
        PrinterPreferences prefs = PrinterPreferences.getInstance(activity);

        if (!prefs.hasLastPrinter()) {
            Toast.makeText(activity, "No printer saved. Scan for printers first.", Toast.LENGTH_SHORT).show();
            return;
        }

        int lastVid = prefs.getLastPrinterVid();
        int lastPid = prefs.getLastPrinterPid();

        UsbManager usbManager = (UsbManager) activity.getSystemService(Context.USB_SERVICE);
        HashMap<String, UsbDevice> deviceList = usbManager.getDeviceList();

        for (UsbDevice device : deviceList.values()) {
            if (device.getVendorId() == lastVid && device.getProductId() == lastPid) {
                showProgressDialog("Requesting permission...");
                requestPermission(device);
                return;
            }
        }

        Toast.makeText(activity, "Printer not found. Please reconnect it.", Toast.LENGTH_LONG).show();
    }

    private void scanForPrinters() {
        Toast.makeText(activity, "Scanning for printers...", Toast.LENGTH_SHORT).show();

        printerManager.addListener(new PrinterManager.PrinterConnectionAdapter() {
            @Override
            public void onPrintersFound(List<UsbPrinterHelper.PrinterInfo> printers) {
                activity.runOnUiThread(() -> {
                    printerManager.removeListener(this);
                    showPrinterSelectionDialog(printers);
                });
            }

            @Override
            public void onDebugInfo(String info) {
                activity.runOnUiThread(() -> {
                    Toast.makeText(activity, info, Toast.LENGTH_SHORT).show();
                });
            }
        });

        printerManager.scanForPrinters();
    }

    private void showPrinterSelectionDialog(List<UsbPrinterHelper.PrinterInfo> printers) {
        if (printers.isEmpty()) {
            Toast.makeText(activity, "No printers found", Toast.LENGTH_SHORT).show();
            return;
        }

        String[] printerNames = new String[printers.size()];
        for (int i = 0; i < printers.size(); i++) {
            printerNames[i] = printers.get(i).toString();
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        builder.setTitle("Select Printer")
                .setItems(printerNames, (dialog, which) -> {
                    UsbPrinterHelper.PrinterInfo selected = printers.get(which);
                    // Save the printer info even before connecting
                    PrinterPreferences.getInstance(activity).saveLastPrinterInfo(
                            selected.toString(),
                            selected.device.getVendorId(),
                            selected.device.getProductId()
                    );

                    // Check permission first
                    UsbManager usbManager = (UsbManager) activity.getSystemService(Context.USB_SERVICE);
                    if (!usbManager.hasPermission(selected.device)) {
                        showProgressDialog("Requesting permission...");
                        // Save as pending
                        PrinterPreferences.getInstance(activity).savePendingConnection(
                                selected.device.getVendorId(),
                                selected.device.getProductId()
                        );
                        requestPermission(selected.device);
                    } else {
                        // Connect directly
                        connectToPrinter(selected.device, false);
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    /**
     * Connect to selected printer with option to specify if it's auto-connect
     */
    /**
     * Connect to selected printer with option to specify if it's auto-connect
     */
    public void connectToPrinter(UsbDevice printer, boolean isAutoConnect) {
        showProgressDialog(isAutoConnect ? "Connecting to printer..." : "Connecting to printer...");

        // Add a timeout handler
        Handler timeoutHandler = new Handler(Looper.getMainLooper());
        Runnable timeoutRunnable = () -> {
            dismissProgressDialog();
            // Don't show timeout as error if we might still connect
            Log.d("PrinterMenuHelper", "Connection taking longer than expected, but may still succeed");
        };
        timeoutHandler.postDelayed(timeoutRunnable, 10000); // Shorter timeout for message

        printerManager.addListener(new PrinterManager.PrinterConnectionAdapter() {
            @Override
            public void onPrinterConnected(String printerName) {
                timeoutHandler.removeCallbacks(timeoutRunnable);
                activity.runOnUiThread(() -> {
                    dismissProgressDialog();
                    printerManager.removeListener(this);

                    // Save the printer for auto-connect
                    PrinterPreferences.getInstance(activity).saveLastPrinter(printer);

                    String message = isAutoConnect ?
                            "✅ Connected to: " + printerName :
                            "✅ Connected to: " + printerName;
                    Toast.makeText(activity, message, Toast.LENGTH_LONG).show();

                    if (callback != null) {
                        callback.onPrinterSelected(printer);
                    }
                });
            }

            @Override
            public void onPrintError(String error) {
                timeoutHandler.removeCallbacks(timeoutRunnable);
                activity.runOnUiThread(() -> {
                    // Check if we're actually connected despite the error
                    if (printerManager.isPrinterConnected()) {
                        Log.d("PrinterMenuHelper", "Connected despite error: " + error);
                        dismissProgressDialog();
                        printerManager.removeListener(this);

                        // Still consider it a success
                        if (callback != null) {
                            callback.onPrinterSelected(printer);
                        }
                    } else {
                        dismissProgressDialog();
                        printerManager.removeListener(this);

                        // Only show error if really not connected
                        String errorMessage = isAutoConnect ?
                                "⚠️ Connection issue: " + error :
                                "⚠️ Connection issue: " + error;
                        Toast.makeText(activity, errorMessage, Toast.LENGTH_LONG).show();
                    }
                });
            }

            @Override
            public void onPermissionDenied() {
                timeoutHandler.removeCallbacks(timeoutRunnable);
                activity.runOnUiThread(() -> {
                    // Check if we're actually connected despite permission denial
                    if (printerManager.isPrinterConnected()) {
                        Log.d("PrinterMenuHelper", "Connected despite permission denial");
                        dismissProgressDialog();
                        printerManager.removeListener(this);

                        // Still consider it a success
                        Toast.makeText(activity, "✅ Printer connected", Toast.LENGTH_SHORT).show();

                        if (callback != null) {
                            callback.onPrinterSelected(printer);
                        }
                    } else {
                        dismissProgressDialog();
                        printerManager.removeListener(this);

                        // Don't show error if it's working anyway
                        Log.d("PrinterMenuHelper", "Permission denied but checking connection");

                        // Try one more time without permission check
                        new Handler(Looper.getMainLooper()).postDelayed(() -> {
                            if (printerManager.isPrinterConnected()) {
                                Toast.makeText(activity, "✅ Printer connected", Toast.LENGTH_SHORT).show();
                                if (callback != null) {
                                    callback.onPrinterSelected(printer);
                                }
                            }
                        }, 2000);
                    }
                });
            }

            @Override
            public void onDebugInfo(String info) {
                Log.d("PrinterConnection", "Debug: " + info);
                activity.runOnUiThread(() -> {
                    if (progressDialog != null && progressDialog.isShowing()) {
                        progressDialog.setMessage(info);
                    }
                });
            }
        });

        printerManager.connectToPrinter(printer);
    }

    private void confirmDisconnectPrinter() {
        if (!printerManager.isPrinterConnected()) {
            Toast.makeText(activity, "No printer connected", Toast.LENGTH_SHORT).show();
            return;
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        builder.setTitle("Disconnect Printer")
                .setMessage("Are you sure you want to disconnect the current printer?")
                .setPositiveButton("Disconnect", (dialog, which) -> {
                    printerManager.disconnectPrinter();
                    PrinterPreferences.getInstance(activity).clearLastPrinter();
                    Toast.makeText(activity, "Printer disconnected", Toast.LENGTH_SHORT).show();
                    if (callback != null) {
                        callback.onPrinterDisconnected();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showPrinterStatus() {
        if (!printerManager.isPrinterConnected()) {
            Toast.makeText(activity, "No printer connected", Toast.LENGTH_SHORT).show();
            return;
        }

        String status = "Printer: " + printerManager.getConnectedPrinterName() + "\n" +
                "Type: " + printerManager.getCurrentPrinterType() + "\n" +
                "Status: Connected";

        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        builder.setTitle("Printer Status")
                .setMessage(status)
                .setPositiveButton("OK", null)
                .show();
    }

    private void diagnosePrinter() {
        showProgressDialog("Diagnosing printer...");

        new Thread(() -> {
            if (printerManager != null) {
                printerManager.diagnosePrinterConnection();
            }

            mainHandler.post(() -> {
                dismissProgressDialog();
                Toast.makeText(activity, "Diagnosis complete. Check Logcat.", Toast.LENGTH_LONG).show();
            });
        }).start();
    }

    private void checkUsbDevices() {
        UsbManager usbManager = (UsbManager) activity.getSystemService(Context.USB_SERVICE);
        HashMap<String, UsbDevice> deviceList = usbManager.getDeviceList();

        StringBuilder sb = new StringBuilder();
        sb.append("USB Devices found: ").append(deviceList.size()).append("\n\n");

        if (deviceList.isEmpty()) {
            sb.append("❌ NO USB DEVICES FOUND!\n");
            sb.append("Please check:\n");
            sb.append("1. Printer is powered on\n");
            sb.append("2. USB cable is connected\n");
            sb.append("3. Cable supports data transfer\n");
        } else {
            for (UsbDevice device : deviceList.values()) {
                sb.append("Device: ").append(device.getDeviceName()).append("\n");
                sb.append("  VID: 0x").append(Integer.toHexString(device.getVendorId())).append("\n");
                sb.append("  PID: 0x").append(Integer.toHexString(device.getProductId())).append("\n");

                boolean hasPermission = usbManager.hasPermission(device);
                sb.append("  Permission: ").append(hasPermission ? "✅ Granted" : "❌ Denied").append("\n\n");
            }
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        builder.setTitle("USB Devices")
                .setMessage(sb.toString())
                .setPositiveButton("OK", null)
                .show();
    }

    private void showTestPrintDialog() {
        if (!printerManager.isPrinterConnected()) {
            Toast.makeText(activity, "No printer connected", Toast.LENGTH_SHORT).show();
            return;
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        builder.setTitle("Test Print");

        final EditText input = new EditText(activity);
        String defaultText = "Test Print\nWeighment System\n" +
                new SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault()).format(new Date());
        input.setText(defaultText);
        input.setSelection(input.getText().length());

        builder.setView(input);
        builder.setPositiveButton("Print", (dialog, which) -> {
            String text = input.getText().toString();
            performTestPrint(text);
        });
        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    private void performTestPrint(String text) {
        if (!printerManager.isPrinterConnected()) {
            Toast.makeText(activity, "Printer not connected", Toast.LENGTH_SHORT).show();
            return;
        }

        ProgressDialog printProgressDialog = new ProgressDialog(activity);
        printProgressDialog.setMessage("Sending to printer...\n(This may take a few seconds)");
        printProgressDialog.setCancelable(true);
        printProgressDialog.setButton(DialogInterface.BUTTON_NEGATIVE, "Cancel",
                (dialog, which) -> {
                    printerManager.cancelCurrentPrint();
                    dialog.dismiss();
                    Toast.makeText(activity, "Print cancelled", Toast.LENGTH_SHORT).show();
                });
        printProgressDialog.show();

        Handler timeoutHandler = new Handler(Looper.getMainLooper());
        Runnable timeoutRunnable = () -> {
            if (printProgressDialog.isShowing()) {
                printProgressDialog.dismiss();
                printerManager.cancelCurrentPrint();
                Toast.makeText(activity, "❌ Print timeout - printer not responding", Toast.LENGTH_LONG).show();
            }
        };
        timeoutHandler.postDelayed(timeoutRunnable, 30000);

        printerManager.addListener(new PrinterManager.PrinterConnectionAdapter() {
            @Override
            public void onPrintSuccess() {
                timeoutHandler.removeCallbacks(timeoutRunnable);
                if (printProgressDialog.isShowing()) {
                    printProgressDialog.dismiss();
                }
                Toast.makeText(activity, "✅ Test print successful", Toast.LENGTH_LONG).show();
                if (callback != null) {
                    callback.onTestPrint();
                }
                printerManager.removeListener(this);
            }

            @Override
            public void onPrintError(String error) {
                timeoutHandler.removeCallbacks(timeoutRunnable);
                if (printProgressDialog.isShowing()) {
                    printProgressDialog.dismiss();
                }
                Toast.makeText(activity, "❌ Test print failed: " + error, Toast.LENGTH_LONG).show();
                printerManager.removeListener(this);
            }

            @Override
            public void onDebugInfo(String info) {
                Log.d("PrinterTest", "Debug: " + info);
            }
        });

        printerManager.autoDetectAndPrintAsync(text, null);
    }

    private void showProgressDialog(String message) {
        if (progressDialog == null) {
            progressDialog = new ProgressDialog(activity);
            progressDialog.setCancelable(false);
        }
        progressDialog.setMessage(message);
        progressDialog.show();
    }

    private void dismissProgressDialog() {
        if (progressDialog != null && progressDialog.isShowing()) {
            progressDialog.dismiss();
        }
    }

    /**
     * Clean up resources
     */
    public void shutdown() {
        try {
            activity.unregisterReceiver(usbPermissionGrantedReceiver);
        } catch (Exception e) {
            Log.e("PrinterMenuHelper", "Error unregistering receiver: " + e.getMessage());
        }

        if (executor != null) {
            executor.shutdownNow();
        }
        dismissProgressDialog();
    }
}