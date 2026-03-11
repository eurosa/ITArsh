package com.googleapi.bluetoothweight;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import java.util.HashMap;
import java.util.List;

public class PrinterHelper {

    private static final String TAG = "PrinterHelper";
    private Context context;
    private UsbPrinterManager printerManager;
    private ProgressDialog progressDialog;
    private Handler mainHandler = new Handler(Looper.getMainLooper());
    private boolean isConnecting = false;
    private Runnable timeoutRunnable;

    public PrinterHelper(Context context) {
        this.context = context;
        this.printerManager = UsbPrinterManager.getInstance();
    }

    /**
     * Show printer selection dialog
     */
    public void showPrinterDialog() {
        List<UsbDevice> printers = printerManager.getAvailablePrinters();

        if (printers.isEmpty()) {
            // Check if any USB devices at all
            UsbManager usbManager = (UsbManager) context.getSystemService(Context.USB_SERVICE);
            HashMap<String, UsbDevice> deviceList = usbManager.getDeviceList();

            if (deviceList.isEmpty()) {
                showAlert("No USB Devices",
                        "No USB devices detected.\n\n" +
                                "Please check:\n" +
                                "• Printer is powered ON\n" +
                                "• USB cable is connected\n" +
                                "• Cable supports data transfer");
            } else {
                showAlert("No Printer Found",
                        "Found " + deviceList.size() + " USB device(s), but none are recognized as printers.\n\n" +
                                "Please check if your printer is properly connected.");
            }
            return;
        }

        String[] printerNames = new String[printers.size()];
        for (int i = 0; i < printers.size(); i++) {
            UsbDevice device = printers.get(i);
            String name = getPrinterDisplayName(device);
            printerNames[i] = name;
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle("Select Printer")
                .setItems(printerNames, (dialog, which) -> {
                    UsbDevice selected = printers.get(which);
                    connectToPrinter(selected);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    /**
     * Connect to selected printer with proper dialog handling
     */
    private void connectToPrinter(UsbDevice device) {
        if (isConnecting) {
            Toast.makeText(context, "Already connecting...", Toast.LENGTH_SHORT).show();
            return;
        }

        isConnecting = true;

        // Show progress dialog
        showProgressDialog("Connecting to printer...\nPlease wait");

        // Set timeout to prevent infinite dialog
        timeoutRunnable = () -> {
            if (isConnecting) {
                isConnecting = false;
                dismissProgressDialog();
                Toast.makeText(context, "⏱️ Connection timeout - printer not responding", Toast.LENGTH_LONG).show();

                showAlert("Connection Timeout",
                        "Printer is not responding.\n\n" +
                                "Please check:\n" +
                                "• Printer is powered ON\n" +
                                "• USB cable is connected securely\n" +
                                "• Printer is in PC mode\n\n" +
                                "Try disconnecting and reconnecting the USB cable.");
            }
        };
        mainHandler.postDelayed(timeoutRunnable, 20000); // 20 second timeout

        // Add listener for connection events
        printerManager.addListener(new UsbPrinterManager.PrinterAdapter() {
            @Override
            public void onPrinterConnected(String printerName) {
                mainHandler.post(() -> {
                    // Connection successful - dismiss dialog
                    isConnecting = false;
                    mainHandler.removeCallbacks(timeoutRunnable);
                    dismissProgressDialog();
                    Toast.makeText(context, "✅ Connected to: " + printerName, Toast.LENGTH_SHORT).show();

                    // Remove this listener
                    printerManager.removeListener(this);
                });
            }

            @Override
            public void onPrinterDisconnected() {
                // Not relevant for connection
            }

            @Override
            public void onPrintError(String error) {
                mainHandler.post(() -> {
                    if (isConnecting) {
                        isConnecting = false;
                        mainHandler.removeCallbacks(timeoutRunnable);
                        dismissProgressDialog();
                        Toast.makeText(context, "❌ Connection failed: " + error, Toast.LENGTH_LONG).show();
                        printerManager.removeListener(this);
                    }
                });
            }

            @Override
            public void onDebugInfo(String info) {
                Log.d(TAG, "Debug: " + info);
            }
        });

        // Start connection
        printerManager.connectToPrinter(device);
    }

    /**
     * Print ticket with proper feedback
     */
    public void printTicket(String vehicleNo, String gross, String tare, String net) {
        if (!printerManager.isConnected()) {
            Toast.makeText(context, "Printer not connected", Toast.LENGTH_SHORT).show();
            showPrinterDialog();
            return;
        }

        showProgressDialog("Printing...");

        // Set timeout for printing
        mainHandler.postDelayed(() -> {
            dismissProgressDialog();
            Toast.makeText(context, "⏱️ Print timeout - check printer", Toast.LENGTH_LONG).show();
        }, 15000);

        // Try DantSu first
        boolean printed = printerManager.printWithDantSu(vehicleNo, gross, tare, net);

        if (!printed) {
            // Fallback to direct USB
            String text = buildPrintText(vehicleNo, gross, tare, net);
            printed = printerManager.printSimpleText(text);
        }

        mainHandler.removeCallbacksAndMessages(null);
        dismissProgressDialog();

        if (printed) {
            Toast.makeText(context, "✅ Print job sent", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(context, "❌ Print failed", Toast.LENGTH_LONG).show();
        }
    }

    /**
     * Show progress dialog with cancel option
     */
    private void showProgressDialog(String message) {
        dismissProgressDialog(); // Dismiss any existing dialog

        progressDialog = new ProgressDialog(context);
        progressDialog.setMessage(message);
        progressDialog.setCancelable(true);
        progressDialog.setCanceledOnTouchOutside(false);
        progressDialog.setButton(DialogInterface.BUTTON_NEGATIVE, "Cancel",
                (dialog, which) -> {
                    isConnecting = false;
                    mainHandler.removeCallbacks(timeoutRunnable);
                    Toast.makeText(context, "Operation cancelled", Toast.LENGTH_SHORT).show();
                });
        progressDialog.show();
    }

    /**
     * Dismiss progress dialog safely
     */
    private void dismissProgressDialog() {
        if (progressDialog != null && progressDialog.isShowing()) {
            try {
                progressDialog.dismiss();
            } catch (Exception e) {
                Log.e(TAG, "Error dismissing dialog: " + e.getMessage());
            }
            progressDialog = null;
        }
    }

    /**
     * Show alert dialog
     */
    private void showAlert(String title, String message) {
        new AlertDialog.Builder(context)
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton("OK", null)
                .show();
    }

    /**
     * Get display name for printer
     */
    private String getPrinterDisplayName(UsbDevice device) {
        String name = "USB Printer";
        if (device.getVendorId() == 0x04A9) {
            name = "🖨️ Canon Printer";
        } else if (device.getVendorId() == 0x04B8) {
            name = "🖨️ Epson Printer";
        } else if (device.getVendorId() == 0x03F0) {
            name = "🖨️ HP Printer";
        }

        UsbManager usbManager = (UsbManager) context.getSystemService(Context.USB_SERVICE);
        boolean hasPermission = usbManager.hasPermission(device);
        String permissionIcon = hasPermission ? "✅" : "❌";

        return permissionIcon + " " + name + " (VID: " +
                String.format("0x%04X", device.getVendorId()) + ")";
    }

    /**
     * Build print text
     */
    private String buildPrintText(String vehicleNo, String gross, String tare, String net) {
        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("dd/MM/yyyy HH:mm:ss",
                java.util.Locale.getDefault());
        String dateTime = sdf.format(new java.util.Date());

        return "\n\n" +
                "================================\n" +
                "     WEIGHMENT TICKET\n" +
                "================================\n" +
                "Vehicle No: " + vehicleNo + "\n" +
                "Gross: " + gross + " kg\n" +
                "Tare: " + tare + " kg\n" +
                "Net: " + net + " kg\n" +
                "================================\n" +
                "Date: " + dateTime + "\n" +
                "================================\n\n\n";
    }

    /**
     * Check USB status
     */
    public void checkUsbStatus() {
        UsbManager usbManager = (UsbManager) context.getSystemService(Context.USB_SERVICE);
        HashMap<String, UsbDevice> deviceList = usbManager.getDeviceList();

        StringBuilder status = new StringBuilder();
        status.append("USB Devices: ").append(deviceList.size()).append("\n\n");

        if (deviceList.isEmpty()) {
            status.append("❌ NO USB DEVICES FOUND\n");
            status.append("Please check:\n");
            status.append("• Printer is powered ON\n");
            status.append("• USB cable is connected\n");
            status.append("• Cable supports data transfer\n");
        } else {
            for (UsbDevice device : deviceList.values()) {
                status.append("Device: ").append(device.getDeviceName()).append("\n");
                status.append("  VID: 0x").append(Integer.toHexString(device.getVendorId())).append("\n");
                status.append("  PID: 0x").append(Integer.toHexString(device.getProductId())).append("\n");
                status.append("  Class: ").append(device.getDeviceClass()).append("\n");

                boolean hasPermission = usbManager.hasPermission(device);
                status.append("  Permission: ").append(hasPermission ? "✅ GRANTED" : "❌ DENIED").append("\n");
                status.append("\n");
            }
        }

        status.append("\nPrinter Manager Status:\n");
        status.append("  Connected: ").append(printerManager.isConnected() ? "✅" : "❌").append("\n");
        status.append("  Printer: ").append(printerManager.getConnectedPrinterName()).append("\n");

        Log.d("USB_STATUS", status.toString());

        new AlertDialog.Builder(context)
                .setTitle("USB Status")
                .setMessage(status.toString())
                .setPositiveButton("OK", null)
                .setNegativeButton("Refresh", (dialog, which) -> {
                    checkUsbStatus();
                })
                .show();
    }
}