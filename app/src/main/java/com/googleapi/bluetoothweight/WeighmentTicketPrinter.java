package com.googleapi.bluetoothweight;



import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.Intent;
import android.hardware.usb.UsbConstants;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.print.PrintAttributes;
import android.print.PrintDocumentAdapter;
import android.print.PrintManager;
import android.util.Log;
import android.widget.Toast;

import com.googleapi.bluetoothweight.nokoprint.NokoPrintDirectPrinter;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.lang.reflect.Method;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

public class WeighmentTicketPrinter {
    private static final String TAG = "WeighmentPrinter";
    private Context context;

    // ESC/P commands for dot matrix
    private static final byte ESC = 0x1B;
    private static final byte LF = 0x0A;
    private static final byte CR = 0x0D;
    private static final byte FF = 0x0C;
    private static final byte CAN = 0x18;

    // Printer connection types
    public enum PrinterType {
        USB,
        BLUETOOTH,
        ANDROID_PRINT,
        NOKOPRINT
    }

    // Callback interface
    public interface PrintCallback {
        void onSuccess(String message);
        void onError(String error);
        void onProgress(String status);
    }

    public WeighmentTicketPrinter(Context context) {
        this.context = context;
    }

    /**
     * Main method to print weighment entry
     */
    public void printWeighmentEntry(WeighmentEntry entry, PrinterType preferredType, PrintCallback callback) {
        try {
            callback.onProgress("Preparing print data...");

            // Build the ticket content with proper formatting
            String printContent = buildWeighmentTicket(entry);

            // Try to print based on preferred type
            switch (preferredType) {
                case USB:
                    callback.onProgress("Attempting USB printing...");
                    if (printViaUSB(printContent, callback)) {
                        return;
                    }
                    // Fall through to next method

                case BLUETOOTH:
                    callback.onProgress("Attempting Bluetooth printing...");
                    if (printViaBluetooth(printContent, callback)) {
                        return;
                    }
                    // Fall through to next method

                case NOKOPRINT:
                    callback.onProgress("Attempting NokoPrint...");
                    if (printViaNokoPrint(printContent, callback)) {
                        return;
                    }
                    // Fall through to next method

                case ANDROID_PRINT:
                default:
                    callback.onProgress("Using Android Print framework...");
                    printViaAndroidPrint(entry, printContent, callback);
                    break;
            }

        } catch (Exception e) {
            Log.e(TAG, "Print error", e);
            callback.onError("Print failed: " + e.getMessage());
        }
    }

    /**
     * Build the weighment ticket with proper dot matrix formatting
     */
    private String buildWeighmentTicket(WeighmentEntry entry) {
        StringBuilder sb = new StringBuilder();

        // ESC/P commands for formatting
        String BOLD_ON = "\u001BE\u0001";    // ESC E 1
        String BOLD_OFF = "\u001BE\u0000";   // ESC E 0
        String DOUBLE_WIDTH_ON = "\u001BW\u0001";  // ESC W 1
        String DOUBLE_WIDTH_OFF = "\u001BW\u0000"; // ESC W 0
        String CONDENSED_ON = "\u001B\u000F";      // ESC SO
        String CONDENSED_OFF = "\u001B\u0012";     // ESC DC2

        // Company Header (Double width, bold)
        sb.append(DOUBLE_WIDTH_ON);
        sb.append(BOLD_ON);
        sb.append("      WEIGHMENT TICKET\n");
        sb.append(BOLD_OFF);
        sb.append(DOUBLE_WIDTH_OFF);

        // Company details
        sb.append(CONDENSED_ON); // Condensed for more columns
        sb.append(repeat("=", 60)).append("\n");
        sb.append("MY WEIGHBRIDGE COMPANY\n");
        sb.append("123 Industrial Area, City - 123456\n");
        sb.append("Phone: +91 9876543210 | GST: 27ABCDE1234F1Z5\n");
        sb.append(repeat("=", 60)).append("\n");

        // Ticket details
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault());
        String dateTime = sdf.format(new Date());

        sb.append(String.format("Ticket #: %-15s Date: %s\n",
                entry.getSerialNo(), dateTime));
        sb.append(repeat("-", 60)).append("\n");

        // Weighment details
        sb.append(BOLD_ON);
        sb.append("VEHICLE DETAILS:\n");
        sb.append(BOLD_OFF);
        sb.append(String.format("Vehicle No: %-20s Type: %s\n",
                entry.getVehicleNo(), entry.getVehicleType()));
        sb.append(String.format("Material: %-20s Party: %s\n",
                entry.getMaterial(), entry.getParty()));
        sb.append(repeat("-", 60)).append("\n");

        // Weight details in box format
        sb.append(BOLD_ON);
        sb.append("WEIGHT DETAILS:\n");
        sb.append(BOLD_OFF);

        String grossStr = entry.getGross().isEmpty() ? "0" : entry.getGross();
        String tareStr = entry.getTare().isEmpty() ? "0" : entry.getTare();
        String manualTareStr = entry.getManualTare().isEmpty() ? "0" : entry.getManualTare();
        String netStr = entry.getNet().isEmpty() ? "0" : entry.getNet();

        // Format weights in columns
        sb.append("┌────────────────────┬────────────────────┐\n");
        sb.append(String.format("│ %-18s │ %18s │\n", "Gross Weight (kg)", grossStr));
        sb.append("├────────────────────┼────────────────────┤\n");
        sb.append(String.format("│ %-18s │ %18s │\n", "Tare Weight (kg)", tareStr));
        sb.append("├────────────────────┼────────────────────┤\n");
        sb.append(String.format("│ %-18s │ %18s │\n", "Manual Tare (kg)", manualTareStr));
        sb.append("├────────────────────┼────────────────────┤\n");
        sb.append(String.format("│ %-18s │ %18s │\n", "Charges", entry.getCharge()));
        sb.append("├────────────────────┼────────────────────┤\n");
        sb.append(BOLD_ON);
        sb.append(String.format("│ %-18s │ %18s │\n", "NET WEIGHT (kg)", netStr));
        sb.append(BOLD_OFF);
        sb.append("└────────────────────┴────────────────────┘\n");

        // Signature area
        sb.append("\n");
        sb.append(repeat("-", 60)).append("\n");
        sb.append(String.format("Operator: %-20s\n", getOperatorName()));
        sb.append("\n");
        sb.append("Signature: __________________    Date: ___________\n");
        sb.append("\n");

        // Footer
        sb.append(CONDENSED_OFF);
        sb.append(BOLD_ON);
        sb.append("       ***** THANK YOU *****\n");
        sb.append(BOLD_OFF);
        sb.append("   *** This is computer generated ***\n");
        sb.append("   *** No signature required ***\n");

        // Form feed (eject paper)
        sb.append("\f");

        return sb.toString();
    }

    // Helper method for repeating strings
    private String repeat(String str, int count) {
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < count; i++) {
            result.append(str);
        }
        return result.toString();
    }

    /**
     * Print via USB connection
     */
    private boolean printViaUSB(String printContent, PrintCallback callback) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            callback.onError("USB printing requires Android 5.0+");
            return false;
        }

        UsbManager usbManager = (UsbManager) context.getSystemService(Context.USB_SERVICE);
        if (usbManager == null) return false;

        try {
            // Find printer device
            UsbDevice printerDevice = null;
            for (UsbDevice device : usbManager.getDeviceList().values()) {
                if (isUsbPrinter(device)) {
                    printerDevice = device;
                    break;
                }
            }

            if (printerDevice == null) {
                callback.onError("No USB printer found");
                return false;
            }

            callback.onProgress("USB printer found: " + printerDevice.getDeviceName());

            // Request permission and print (simplified - you'd need full USB communication)
            // For simplicity, we'll fall back to Android print
            return false;

        } catch (Exception e) {
            Log.e(TAG, "USB print error", e);
            return false;
        }
    }

    /**
     * Print via Bluetooth
     */
    private boolean printViaBluetooth(String printContent, PrintCallback callback) {
        BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (bluetoothAdapter == null) {
            callback.onError("Bluetooth not supported");
            return false;
        }

        if (!bluetoothAdapter.isEnabled()) {
            callback.onError("Please enable Bluetooth");
            return false;
        }

        try {
            // Find paired printers
            Set<BluetoothDevice> pairedDevices = bluetoothAdapter.getBondedDevices();
            BluetoothDevice printerDevice = null;

            for (BluetoothDevice device : pairedDevices) {
                String deviceName = device.getName().toLowerCase();
                if (deviceName.contains("printer") ||
                        deviceName.contains("pos") ||
                        deviceName.contains("thermal") ||
                        deviceName.contains("dot")) {
                    printerDevice = device;
                    break;
                }
            }

            if (printerDevice == null) {
                callback.onError("No Bluetooth printer found. Please pair a printer first.");
                return false;
            }

            callback.onProgress("Connecting to: " + printerDevice.getName());

            // Connect and print (simplified - you'd need proper BluetoothSocket handling)
            return false;

        } catch (Exception e) {
            Log.e(TAG, "Bluetooth print error", e);
            return false;
        }
    }

    /**
     * Print via NokoPrint
     */
    /**
     * Print via NokoPrint
     */
    private boolean printViaNokoPrint(String printContent, PrintCallback callback) {
        try {
            callback.onProgress("Sending to NokoPrint...");

            // Create a text file with the print content
            File textFile = new File(context.getCacheDir(),
                    "weighment_" + System.currentTimeMillis() + ".txt");

            // Write with proper encoding for dot matrix
            try (FileOutputStream fos = new FileOutputStream(textFile)) {
                // Convert to bytes with IBM437 encoding (DOS character set)
                byte[] bytes = printContent.getBytes("IBM437");
                fos.write(bytes);
                fos.flush();
                Log.d(TAG, "File written successfully: " + textFile.getAbsolutePath());
                Log.d(TAG, "File size: " + bytes.length + " bytes");
            } catch (IOException e) {
                Log.e(TAG, "Error writing file", e);
                callback.onError("Failed to create print file: " + e.getMessage());
                return false;
            }

            // Use NokoPrint
            NokoPrintDirectPrinter nokoPrinter = new NokoPrintDirectPrinter(context);

            if (nokoPrinter.isNokoPrintInstalled()) {
                android.net.Uri uri = androidx.core.content.FileProvider.getUriForFile(
                        context,
                        context.getPackageName() + ".fileprovider",
                        textFile
                );

                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setDataAndType(uri, "text/plain");
                intent.setPackage("com.nokoprint");
                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

                // Add printer type hint
                intent.putExtra("printer_type", "dotmatrix");
                intent.putExtra("paper_size", "continuous");
                intent.putExtra("character_set", "ibm437");
                intent.putExtra("document_name", "Weighment_Ticket.txt");

                context.startActivity(intent);
                callback.onSuccess("Sent to NokoPrint");
                return true;
            } else {
                callback.onError("NokoPrint not installed");
                return false;
            }

        } catch (Exception e) {
            Log.e(TAG, "NokoPrint error", e);
            callback.onError("NokoPrint error: " + e.getMessage());
            return false;
        }
    }

    /**
     * Print via Android's print framework
     */
    private void printViaAndroidPrint(WeighmentEntry entry, String printContent, PrintCallback callback) {
        try {
            PrintManager printManager = (PrintManager) context.getSystemService(Context.PRINT_SERVICE);

            String jobName = "Weighment Ticket #" + entry.getVehicleNo();

            // Create custom print adapter
            PrintDocumentAdapter printAdapter = new WeighmentPrintAdapter(context, printContent, entry);

            PrintAttributes attributes = new PrintAttributes.Builder()
                    .setMediaSize(PrintAttributes.MediaSize.ISO_A4)
                    .setResolution(new PrintAttributes.Resolution("print_id", "Print", 300, 300))
                    .setMinMargins(PrintAttributes.Margins.NO_MARGINS)
                    .build();

            printManager.print(jobName, printAdapter, attributes);

            callback.onSuccess("Print job sent to Android print system");

        } catch (Exception e) {
            Log.e(TAG, "Android print error", e);
            callback.onError("Android print failed: " + e.getMessage());
        }
    }

    /**
     * Auto-detect best printing method
     */
    public void autoPrintWeighmentEntry(WeighmentEntry entry, PrintCallback callback) {
        // Try USB first
        String printContent = buildWeighmentTicket(entry);

        if (printViaUSB(printContent, callback)) {
            return;
        }

        // Then Bluetooth
        if (printViaBluetooth(printContent, callback)) {
            return;
        }

        // Then NokoPrint
        if (printViaNokoPrint(printContent, callback)) {
            return;
        }

        // Finally Android Print
        printViaAndroidPrint(entry, printContent, callback);
    }

    /**
     * Check if USB device is a printer
     */
    private boolean isUsbPrinter(UsbDevice device) {
        int printerClass = 0x07; // USB class for printers
        return device.getDeviceClass() == printerClass ||
                device.getInterfaceCount() > 0 &&
                        device.getInterface(0).getInterfaceClass() == printerClass;
    }

    /**
     * Get current operator name (you can modify this)
     */
    private String getOperatorName() {
        // You can get this from SharedPreferences or your app's user session
        return "Admin";
    }
}