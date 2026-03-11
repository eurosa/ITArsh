package com.googleapi.bluetoothweight;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.hardware.usb.UsbConstants;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbEndpoint;
import android.hardware.usb.UsbInterface;
import android.hardware.usb.UsbManager;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.print.PrintHelper;

import com.dantsu.escposprinter.EscPosPrinter;
import com.dantsu.escposprinter.connection.DeviceConnection;
import com.dantsu.escposprinter.connection.usb.UsbConnection;
import com.dantsu.escposprinter.connection.usb.UsbPrintersConnections;
import com.dantsu.escposprinter.exceptions.EscPosConnectionException;
import com.dantsu.escposprinter.exceptions.EscPosEncodingException;
import com.dantsu.escposprinter.exceptions.EscPosParserException;

import java.io.UnsupportedEncodingException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class UsbPrinterManager {
    private static final String TAG = "UsbPrinterManager";
    private static final String ACTION_USB_PERMISSION = "com.googleapi.bluetoothweight.USB_PERMISSION";
    private static UsbPrinterManager instance;

    private Context context;
    private UsbManager usbManager;
    private UsbDevice currentDevice;
    private UsbDeviceConnection usbConnection;
    private UsbEndpoint usbEndpointOut;
    private EscPosPrinter escPosPrinter;
    private UsbConnection usbConnectionDantSu;

    private boolean isConnected = false;
    private String connectedPrinterName = "";
    private List<PrinterListener> listeners = new ArrayList<>();
    private ExecutorService executor = Executors.newSingleThreadExecutor();
    private Handler mainHandler = new Handler(Looper.getMainLooper());
    private BroadcastReceiver usbReceiver;
    private boolean isReceiverRegistered = false;

    // Canon vendor ID
    private static final int CANON_VENDOR_ID = 0x04A9; // 1193 in decimal

    public interface PrinterListener {
        void onPrinterConnected(String printerName);
        void onPrinterDisconnected();
        void onPrintSuccess();
        void onPrintError(String error);
        void onDebugInfo(String info);
    }

    public static class PrinterAdapter implements PrinterListener {
        @Override public void onPrinterConnected(String printerName) {}
        @Override public void onPrinterDisconnected() {}
        @Override public void onPrintSuccess() {}
        @Override public void onPrintError(String error) {}
        @Override public void onDebugInfo(String info) {}
    }

    public static synchronized UsbPrinterManager getInstance() {
        if (instance == null) {
            instance = new UsbPrinterManager();
        }
        return instance;
    }

    private UsbPrinterManager() {}

    public void init(Context context) {
        this.context = context.getApplicationContext();
        this.usbManager = (UsbManager) context.getSystemService(Context.USB_SERVICE);

        registerUsbReceiver();
        scanForPrinters();
    }

    /**
     * Register USB receiver
     */
    private void registerUsbReceiver() {
        if (isReceiverRegistered) return;

        usbReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();

                if (ACTION_USB_PERMISSION.equals(action)) {
                    synchronized (this) {
                        UsbDevice device = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);

                        if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
                            if (device != null) {
                                Log.d(TAG, "Permission granted for device");
                                showToast("USB permission granted");
                                connectToPrinter(device);
                            }
                        } else {
                            Log.d(TAG, "Permission denied for device");
                            showToast("USB permission denied");
                        }
                    }
                } else if (UsbManager.ACTION_USB_DEVICE_ATTACHED.equals(action)) {
                    UsbDevice device = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
                    if (device != null) {
                        Log.d(TAG, "USB device attached");
                        showToast("USB device connected");
                        scanForPrinters();
                        if (isPrinterDevice(device)) {
                            requestPermission(device);
                        }
                    }
                } else if (UsbManager.ACTION_USB_DEVICE_DETACHED.equals(action)) {
                    UsbDevice device = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
                    if (device != null && currentDevice != null && currentDevice.equals(device)) {
                        Log.d(TAG, "USB device detached");
                        showToast("USB printer disconnected");
                        disconnect();
                    }
                }
            }
        };

        IntentFilter filter = new IntentFilter();
        filter.addAction(ACTION_USB_PERMISSION);
        filter.addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED);
        filter.addAction(UsbManager.ACTION_USB_DEVICE_DETACHED);

        // Register receiver with appropriate flags based on Android version
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.registerReceiver(usbReceiver, filter, Context.RECEIVER_EXPORTED);
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.registerReceiver(usbReceiver, filter, Context.RECEIVER_EXPORTED);
        } else {
            context.registerReceiver(usbReceiver, filter);
        }

        isReceiverRegistered = true;
        Log.d(TAG, "USB receiver registered");
    }

    /**
     * Show toast on UI thread
     */
    private void showToast(final String message) {
        mainHandler.post(() -> {
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
        });
    }

    /**
     * Scan for USB printers
     */
    public List<UsbDevice> scanForPrinters() {
        List<UsbDevice> printers = new ArrayList<>();
        HashMap<String, UsbDevice> deviceList = usbManager.getDeviceList();

        Log.d(TAG, "Scanning for USB devices. Found: " + deviceList.size());
        notifyDebugInfo("Scanning for USB devices. Found: " + deviceList.size());

        for (UsbDevice device : deviceList.values()) {
            String deviceInfo = String.format("Device: VID=0x%04X, PID=0x%04X, Class=%d, Name=%s",
                    device.getVendorId(), device.getProductId(), device.getDeviceClass(), device.getDeviceName());
            Log.d(TAG, deviceInfo);

            if (isPrinterDevice(device)) {
                printers.add(device);
                String printerInfo = "✅ Found printer: " + getDeviceInfo(device);
                Log.d(TAG, printerInfo);
                notifyDebugInfo(printerInfo);
            }
        }

        return printers;
    }

    /**
     * Check if device is a printer
     */
    private boolean isPrinterDevice(UsbDevice device) {
        // Check vendor ID for Canon
        if (device.getVendorId() == CANON_VENDOR_ID) {
            return true;
        }

        // Check device class (7 = Printer class)
        if (device.getDeviceClass() == 7) {
            return true;
        }

        // Check interface class
        for (int i = 0; i < device.getInterfaceCount(); i++) {
            if (device.getInterface(i).getInterfaceClass() == 7) {
                return true;
            }
        }

        return false;
    }

    /**
     * Request USB permission
     */
    public void requestPermission(UsbDevice device) {
        if (usbManager.hasPermission(device)) {
            Log.d(TAG, "Already have permission");
            connectToPrinter(device);
        } else {
            Log.d(TAG, "Requesting permission for device");
            PendingIntent permissionIntent = PendingIntent.getBroadcast(
                    context,
                    0,
                    new Intent(ACTION_USB_PERMISSION),
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
            );
            usbManager.requestPermission(device, permissionIntent);
        }
    }

    /**
     * Connect to printer
     */
    /**
     * Connect to printer with detailed status
     */
    /**
     * Connect to printer with timeout and proper dialog handling
     */
    public void connectToPrinter(UsbDevice device) {
        this.currentDevice = device;

        notifyDebugInfo("🔄 Starting printer connection...");
        showToast("Connecting to printer...");

        // Use a flag to track if connection is complete
        final boolean[] connectionComplete = {false};

        executor.execute(() -> {
            try {
                // Try direct USB connection first
                notifyDebugInfo("📡 Trying direct USB connection...");
                boolean directConnected = connectDirectUsb(device);

                if (directConnected) {
                    notifyDebugInfo("✅ Direct USB connected successfully");
                    connectionComplete[0] = true;
                } else {
                    notifyDebugInfo("❌ Direct USB connection failed");
                }

                // If direct connection failed, try DantSu
                if (!connectionComplete[0]) {
                    notifyDebugInfo("📡 Trying DantSu library connection...");
                    boolean dantsuConnected = connectDantSu(device);

                    if (dantsuConnected) {
                        notifyDebugInfo("✅ DantSu connected successfully");
                        connectionComplete[0] = true;
                    } else {
                        notifyDebugInfo("❌ DantSu connection failed");
                    }
                }

                // Final connection status - post to UI thread
                final boolean finalConnectionStatus = connectionComplete[0];

                mainHandler.post(() -> {
                    if (finalConnectionStatus) {
                        connectedPrinterName = getPrinterName(device);
                        notifyPrinterConnected(connectedPrinterName);
                        showToast("✅ Printer connected: " + connectedPrinterName);

                        // Test the printer automatically
                        testDantSuPrinter();
                    } else {
                        showToast("❌ Failed to connect to printer");
                        // Dismiss any pending dialogs through the helper
                    }
                });

            } catch (Exception e) {
                Log.e(TAG, "Connection error: " + e.getMessage());
                notifyDebugInfo("❌ Connection exception: " + e.getMessage());

                mainHandler.post(() -> {
                    showToast("❌ Connection error: " + e.getMessage());
                });
            }
        });

        // Set a timeout to prevent infinite connection attempts
        mainHandler.postDelayed(() -> {
            if (!connectionComplete[0]) {
                notifyDebugInfo("⏱️ Connection timeout - taking too long");
                mainHandler.post(() -> {
                    showToast("⏱️ Connection timeout - check printer");
                    // Force dismiss any progress dialogs through the helper
                });
            }
        }, 15000); // 15 second timeout
    }

    /**
     * Show connection failed dialog with troubleshooting
     */
    private void showConnectionFailedDialog(UsbDevice device) {
        String deviceInfo = String.format("VID: 0x%04X, PID: 0x%04X",
                device.getVendorId(), device.getProductId());

        new android.app.AlertDialog.Builder(context)
                .setTitle("Connection Failed")
                .setMessage(
                        "Could not connect to printer.\n\n" +
                                "Device: " + deviceInfo + "\n" +
                                "Name: " + device.getDeviceName() + "\n\n" +
                                "Troubleshooting:\n" +
                                "1. Make sure printer is powered ON\n" +
                                "2. Check USB cable connection\n" +
                                "3. Try a different USB port\n" +
                                "4. Restart the printer\n" +
                                "5. Check if printer is in PC mode\n\n" +
                                "Check Logcat for detailed errors."
                )
                .setPositiveButton("OK", null)
                .setNeutralButton("Retry", (dialog, which) -> {
                    connectToPrinter(device);
                })
                .show();
    }

    /**
     * Direct USB connection
     */
    /**
     * Direct USB connection - returns true if successful
     */
    private boolean connectDirectUsb(UsbDevice device) {
        if (!usbManager.hasPermission(device)) {
            notifyDebugInfo("❌ No USB permission for direct connection");
            return false;
        }

        notifyDebugInfo("🔍 Scanning " + device.getInterfaceCount() + " interfaces...");

        for (int i = 0; i < device.getInterfaceCount(); i++) {
            UsbInterface intf = device.getInterface(i);
            notifyDebugInfo("  Interface " + i + ": Class=" + intf.getInterfaceClass());

            UsbDeviceConnection conn = usbManager.openDevice(device);
            if (conn != null && conn.claimInterface(intf, true)) {
                notifyDebugInfo("  ✅ Interface " + i + " claimed");

                for (int j = 0; j < intf.getEndpointCount(); j++) {
                    UsbEndpoint ep = intf.getEndpoint(j);
                    String direction = (ep.getDirection() == UsbConstants.USB_DIR_OUT) ? "OUT" : "IN";
                    notifyDebugInfo("    Endpoint " + j + ": " + direction +
                            " Address=0x" + Integer.toHexString(ep.getAddress()));

                    if (ep.getDirection() == UsbConstants.USB_DIR_OUT) {
                        this.usbConnection = conn;
                        this.usbEndpointOut = ep;
                        this.isConnected = true;
                        notifyDebugInfo("    ✅ Using OUT endpoint " + j);

                        // Test the connection with a small write
                        try {
                            byte[] test = new byte[]{0x0A};
                            int result = conn.bulkTransfer(ep, test, test.length, 1000);
                            notifyDebugInfo("    Test write result: " + result);
                        } catch (Exception e) {
                            notifyDebugInfo("    Test write failed: " + e.getMessage());
                        }

                        return true;
                    }
                }

                conn.releaseInterface(intf);
                conn.close();
            } else {
                notifyDebugInfo("  ❌ Could not claim interface " + i);
            }
        }

        return false;
    }

    /**
     * Connect using DantSu library
     */
    /**
     * Connect using DantSu library
     */
    /**
     * Connect using DantSu library with proper type handling
     */
    /**
     * Connect using DantSu library - Fixed for your library version
     */
    /**
     * Connect using DantSu library - Fixed for your library version
     */
    /**
     * Connect using DantSu library - Fixed version
     */
    /**
     * Connect using DantSu library - returns true if successful
     */
    private boolean connectDantSu(UsbDevice device) {
        try {
            // Create USB connection
            UsbConnection usbConn = new UsbConnection(usbManager, device);
            this.usbConnectionDantSu = usbConn;

            notifyDebugInfo("📦 DantSu connection object created");

            // Check permission
            if (!usbManager.hasPermission(device)) {
                notifyDebugInfo("❌ No USB permission for DantSu");
                return false;
            }

            notifyDebugInfo("🔄 Calling DantSu connect()...");

            // Try to initialize printer directly
            try {
                int printerDpi = 203;
                float printerWidth = 48f;
                int charactersPerLine = 32;

                notifyDebugInfo("🖨️ Creating EscPosPrinter with params: dpi=" + printerDpi +
                        ", width=" + printerWidth + ", chars=" + charactersPerLine);

                this.escPosPrinter = new EscPosPrinter(usbConn, printerDpi, printerWidth, charactersPerLine);

                notifyDebugInfo("✅ EscPosPrinter created");

                // Check if connected
                if (usbConn.isConnected()) {
                    notifyDebugInfo("✅ DantSu printer is connected");
                    this.isConnected = true;
                    return true;
                } else {
                    notifyDebugInfo("❌ DantSu printer created but not connected");

                    // Try to connect explicitly if method exists
                    try {
                        // Try to call connect() if available (may not exist in your version)
                        java.lang.reflect.Method connectMethod = usbConn.getClass().getMethod("connect");
                        if (connectMethod != null) {
                            notifyDebugInfo("🔄 Calling connect() via reflection");
                            Object result = connectMethod.invoke(usbConn);
                            notifyDebugInfo("Connect result: " + result);

                            if (usbConn.isConnected()) {
                                this.isConnected = true;
                                return true;
                            }
                        }
                    } catch (Exception e) {
                        notifyDebugInfo("Could not call connect(): " + e.getMessage());
                    }

                    return false;
                }

            } catch (Exception e) {
                notifyDebugInfo("❌ DantSu initialization error: " + e.getMessage());
                Log.e(TAG, "DantSu error", e);
                return false;
            }

        } catch (Exception e) {
            notifyDebugInfo("❌ DantSu connection error: " + e.getMessage());
            Log.e(TAG, "DantSu error details", e);
            return false;
        }
    }
    /**
     * Test DantSu printer with a simple print
     */
    private void testDantSuPrinter() {
        if (escPosPrinter == null) return;

        try {
            String testData = "[C]<font size='big'>PRINTER READY</font>\n[L]Connected to DantSu library!";
            escPosPrinter.printFormattedText(testData);
            notifyDebugInfo("✅ DantSu test print sent");
        } catch (Exception e) {
            Log.d(TAG, "Test print failed, but connection is established: " + e.getMessage());
        }
    }
    /**
     * Print using DantSu library (Recommended)
     */
    /**
     * Print using DantSu library with better error handling
     */
    public boolean printWithDantSu(String vehicleNo, String gross, String tare, String net) {
        if (!isConnected || escPosPrinter == null) {
            notifyDebugInfo("❌ Printer not connected or printer object is null");
            showToast("Printer not ready");
            return false;
        }

        try {
            String dateTime = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault()).format(new Date());

            // Simplified print data - remove complex formatting first
            String printData =
                    "[C]WEIGHMENT TICKET\n" +
                            "[L]Vehicle: " + vehicleNo + "\n" +
                            "[L]Gross: " + gross + " kg\n" +
                            "[L]Tare: " + tare + " kg\n" +
                            "[L]Net: " + net + " kg\n" +
                            "[L]Date: " + dateTime + "\n" +
                            "[C]----------------\n" +
                            "[C]<cut/>";

            Log.d(TAG, "Sending print data:\n" + printData);
            notifyDebugInfo("Sending print data...");

            // Send to printer
            escPosPrinter.printFormattedText(printData);

            notifyDebugInfo("✅ Print command sent successfully");
            mainHandler.post(() -> notifyPrintSuccess());
            return true;

        } catch (EscPosConnectionException e) {
            String error = "Connection error: " + e.getMessage();
            Log.e(TAG, error);
            notifyPrintError(error);
        } catch (EscPosParserException e) {
            String error = "Parser error: " + e.getMessage();
            Log.e(TAG, error);
            notifyPrintError(error);
        } catch (EscPosEncodingException e) {
            String error = "Encoding error: " + e.getMessage();
            Log.e(TAG, error);
            notifyPrintError(error);
        } catch (Exception e) {
            String error = "Print error: " + e.getMessage();
            Log.e(TAG, error, e);
            notifyPrintError(error);
        }
        return false;
    }

    /**
     * Simple text print using direct USB
     */
    public boolean printSimpleText(String text) {
        if (!isConnected || usbConnection == null || usbEndpointOut == null) {
            notifyDebugInfo("❌ Not connected");
            return false;
        }

        try {
            // Add form feed at the end
            String printData = text + "\n\n\f";
            byte[] data = printData.getBytes("UTF-8");

            int transferred = usbConnection.bulkTransfer(usbEndpointOut, data, data.length, 10000);

            if (transferred == data.length) {
                notifyPrintSuccess();
                return true;
            } else {
                notifyPrintError("Failed: sent " + transferred + " of " + data.length + " bytes");
                return false;
            }
        } catch (UnsupportedEncodingException e) {
            notifyPrintError("Encoding error: " + e.getMessage());
            return false;
        } catch (Exception e) {
            notifyPrintError("Error: " + e.getMessage());
            return false;
        }
    }

    /**
     * Canon-specific ESC/POS print
     */
    public boolean printCanonESCPOS(String vehicleNo, String gross, String tare, String net) {
        if (!isConnected || usbConnection == null || usbEndpointOut == null) {
            return false;
        }

        try {
            java.io.ByteArrayOutputStream output = new java.io.ByteArrayOutputStream();

            // Initialize printer (ESC @)
            output.write(0x1B);
            output.write(0x40);

            // Center align
            output.write(0x1B);
            output.write(0x61);
            output.write(0x01);

            // Print header
            String header = "WEIGHMENT TICKET\n";
            output.write(header.getBytes("CP437"));
            output.write(0x0A);

            // Left align
            output.write(0x1B);
            output.write(0x61);
            output.write(0x00);

            // Print data
            output.write(("Vehicle: " + vehicleNo + "\n").getBytes("CP437"));
            output.write(("Gross: " + gross + " kg\n").getBytes("CP437"));
            output.write(("Tare: " + tare + " kg\n").getBytes("CP437"));
            output.write(("Net: " + net + " kg\n").getBytes("CP437"));

            String dateTime = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault()).format(new Date());
            output.write(("Date: " + dateTime + "\n").getBytes("CP437"));

            // Form feed
            output.write(0x0C);

            byte[] data = output.toByteArray();
            int transferred = usbConnection.bulkTransfer(usbEndpointOut, data, data.length, 10000);

            if (transferred == data.length) {
                notifyPrintSuccess();
                return true;
            } else {
                notifyPrintError("Failed: sent " + transferred + " bytes");
                return false;
            }

        } catch (Exception e) {
            Log.e(TAG, "Print error: " + e.getMessage());
            notifyPrintError(e.getMessage());
            return false;
        }
    }

    /**
     * Print with Android's built-in framework
     */
    public void printWithAndroidFramework(String text) {
        if (!(context instanceof android.app.Activity)) {
            notifyPrintError("Context is not an Activity");
            return;
        }

        android.app.Activity activity = (android.app.Activity) context;

        try {
            PrintHelper printHelper = new PrintHelper(activity);
            printHelper.setScaleMode(PrintHelper.SCALE_MODE_FIT);
            printHelper.setColorMode(PrintHelper.COLOR_MODE_MONOCHROME);

            // Create a TextView with the text
            TextView textView = new TextView(activity);
            textView.setText(text);
            textView.setTextSize(16);
            textView.setPadding(50, 50, 50, 50);
            textView.setBackgroundColor(Color.WHITE);
            textView.setTextColor(Color.BLACK);

            // Measure and layout
            int width = 800;
            int height = 1200;
            textView.measure(
                    View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY),
                    View.MeasureSpec.makeMeasureSpec(height, View.MeasureSpec.EXACTLY)
            );
            textView.layout(0, 0, width, height);

            // Create bitmap
            Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(bitmap);
            textView.draw(canvas);

            // Print
            printHelper.printBitmap("weighment_ticket", bitmap);
            notifyPrintSuccess();

        } catch (Exception e) {
            notifyPrintError("Android print error: " + e.getMessage());
        }
    }

    /**
     * Test print with simple text
     */
    public void testPrint() {
        if (!isConnected) {
            notifyDebugInfo("❌ Printer not connected");
            showToast("Printer not connected");
            return;
        }

        String testText = "\n\n" +
                "================================\n" +
                "         TEST PAGE\n" +
                "================================\n" +
                "Printer: " + connectedPrinterName + "\n" +
                "Time: " + new SimpleDateFormat("HH:mm:ss").format(new Date()) + "\n" +
                "Date: " + new SimpleDateFormat("dd/MM/yyyy").format(new Date()) + "\n" +
                "================================\n" +
                "If you can read this,\n" +
                "the printer is working!\n" +
                "================================\n\n\n";

        showToast("Printing test page...");

        // Try DantSu first
        if (escPosPrinter != null) {
            try {
                String printData =
                        "[C]<font size='big'>TEST PAGE</font>\n" +
                                "[L]Printer: " + connectedPrinterName + "\n" +
                                "[L]Time: " + new SimpleDateFormat("HH:mm:ss").format(new Date()) + "\n" +
                                "[L]Date: " + new SimpleDateFormat("dd/MM/yyyy").format(new Date()) + "\n" +
                                "[C]<cut/>";
                escPosPrinter.printFormattedText(printData);
                return;
            } catch (Exception e) {
                notifyDebugInfo("DantSu test failed, trying direct USB");
            }
        }

        // Fallback to direct USB
        printSimpleText(testText);
    }

    /**
     * Get list of available printers
     */
    public List<UsbDevice> getAvailablePrinters() {
        return scanForPrinters();
    }

    /**
     * Get device info string
     */
    private String getDeviceInfo(UsbDevice device) {
        return String.format("VID:0x%04X PID:0x%04X Name:%s",
                device.getVendorId(), device.getProductId(), device.getDeviceName());
    }

    /**
     * Get printer name
     */
    private String getPrinterName(UsbDevice device) {
        if (device.getVendorId() == CANON_VENDOR_ID) {
            return "Canon Printer (USB)";
        }
        return "USB Printer";
    }

    /**
     * Disconnect printer
     */
    public void disconnect() {
        if (usbConnection != null) {
            usbConnection.close();
            usbConnection = null;
        }
        usbEndpointOut = null;
        escPosPrinter = null;
        usbConnectionDantSu = null;
        isConnected = false;
        connectedPrinterName = "";

        mainHandler.post(() -> {
            notifyPrinterDisconnected();
            showToast("Printer disconnected");
        });
    }

    /**
     * Check if printer is connected
     */
    public boolean isConnected() {
        return isConnected;
    }

    /**
     * Get connected printer name
     */
    public String getConnectedPrinterName() {
        return connectedPrinterName;
    }

    /**
     * Add listener
     */
    public void addListener(PrinterListener listener) {
        if (!listeners.contains(listener)) {
            listeners.add(listener);
        }
    }

    /**
     * Remove listener
     */
    public void removeListener(PrinterListener listener) {
        listeners.remove(listener);
    }

    /**
     * Notify printer connected
     */
    private void notifyPrinterConnected(String name) {
        for (PrinterListener listener : listeners) {
            listener.onPrinterConnected(name);
        }
    }

    /**
     * Notify printer disconnected
     */
    private void notifyPrinterDisconnected() {
        for (PrinterListener listener : listeners) {
            listener.onPrinterDisconnected();
        }
    }

    /**
     * Notify print success
     */
    private void notifyPrintSuccess() {
        for (PrinterListener listener : listeners) {
            listener.onPrintSuccess();
        }
        showToast("✅ Print successful");
    }

    /**
     * Notify print error
     */
    private void notifyPrintError(String error) {
        Log.e(TAG, "Print error: " + error);
        for (PrinterListener listener : listeners) {
            listener.onPrintError(error);
        }
        showToast("❌ " + error);
    }

    /**
     * Notify debug info
     */
    private void notifyDebugInfo(String info) {
        Log.d(TAG, info);
        for (PrinterListener listener : listeners) {
            listener.onDebugInfo(info);
        }
    }

    /**
     * Unregister receiver
     */
    public void unregisterReceiver() {
        if (isReceiverRegistered && usbReceiver != null) {
            try {
                context.unregisterReceiver(usbReceiver);
                isReceiverRegistered = false;
                Log.d(TAG, "USB receiver unregistered");
            } catch (IllegalArgumentException e) {
                Log.e(TAG, "Error unregistering receiver: " + e.getMessage());
            }
        }
    }

    /**
     * Shutdown
     */
    public void shutdown() {
        disconnect();
        unregisterReceiver();
        executor.shutdownNow();
    }
}