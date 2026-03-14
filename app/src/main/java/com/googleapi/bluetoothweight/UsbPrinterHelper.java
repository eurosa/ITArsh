    package com.googleapi.bluetoothweight;
    
    import android.app.PendingIntent;
    import android.content.BroadcastReceiver;
    import android.content.Context;
    import android.content.Intent;
    import android.content.IntentFilter;
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
    
    import java.io.ByteArrayOutputStream;
    import java.io.UnsupportedEncodingException;
    import java.text.SimpleDateFormat;
    import java.util.ArrayList;
    import java.util.Date;
    import java.util.HashMap;
    import java.util.List;
    import java.util.Locale;
    import java.util.concurrent.ExecutorService;
    import java.util.concurrent.Executors;
    
    public class UsbPrinterHelper {
        private static final String TAG = "UsbPrinterHelper";
        private static final String ACTION_USB_PERMISSION = "com.googleapi.bluetoothweight.USB_PERMISSION";
    
        // Common printer vendor IDs
        private static final int[] COMMON_PRINTER_VENDORS = {
                0x04A9, // Canon
                0x04B8, // Epson
                0x0519, // Star Micronics
                0x0FE6, // Zebra
                0x0416, // Bixolon
                0x067B, // Prolific (many generic printers)
                0x0525, // Netchip (generic)
                0x03F0, // HP
                0x04F9, // Brother
                0x0471, // Philips
                0x0922, // Dymo
                0x0B97  // Oki Data
        };
    
        // Printer class codes
        private static final int USB_CLASS_PRINTER = 0x07;
    
        private Context context;
        private UsbManager usbManager;
        private UsbDeviceConnection usbConnection;
        private UsbEndpoint usbEndpointOut;
        private UsbDevice usbDevice;
        private PrinterCallback callback;
        private BroadcastReceiver usbReceiver;
        private PrinterType currentPrinterType = PrinterType.UNKNOWN;
        private boolean isPermissionRequestPending = false;
        private boolean isReceiverRegistered = false;
        private boolean isConnected = false;
    
        private ExecutorService executor = Executors.newSingleThreadExecutor();
        private Handler mainHandler = new Handler(Looper.getMainLooper());
    
        // Debug info
        private StringBuilder debugInfo = new StringBuilder();
        private List<UsbDevice> availablePrinters = new ArrayList<>();
    
        public enum PrinterType {
            UNKNOWN,
            ESCPOS,     // Epson ESC/POS
            PCL,        // HP PCL
            POSTSCRIPT, // Adobe PostScript
            CANON,      // Canon proprietary
            ZEBRA,      // Zebra ZPL
            STAR,       // Star Micronics
            GENERIC     // Generic plain text
        }
        /**
         * Get the current USB device
         */
        public UsbDevice getCurrentDevice() {
            return usbDevice;
        }
        public interface PrinterCallback {
            void onPrinterConnected();
            void onPrinterDisconnected();
            void onPrintSuccess();
            void onPrintError(String error);
            void onPermissionDenied();
            void onDebugInfo(String info);
            void onPrintersFound(List<PrinterInfo> printers);
        }
    
        public static class PrinterInfo {
            public String deviceName;
            public int vendorId;
            public int productId;
            public String manufacturer;
            public PrinterType type;
            public UsbDevice device;
    
            public PrinterInfo(UsbDevice device, PrinterType type) {
                this.device = device;
                this.deviceName = device.getDeviceName();
                this.vendorId = device.getVendorId();
                this.productId = device.getProductId();
                this.manufacturer = getVendorName(vendorId);
                this.type = type;
            }
    
            private String getVendorName(int vendorId) {
                switch (vendorId) {
                    case 0x04A9: return "Canon";
                    case 0x04B8: return "Epson";
                    case 0x0519: return "Star Micronics";
                    case 0x0FE6: return "Zebra";
                    case 0x0416: return "Bixolon";
                    case 0x03F0: return "HP";
                    case 0x04F9: return "Brother";
                    default: return "Unknown";
                }
            }
    
            @Override
            public String toString() {
                return manufacturer + " Printer (USB)";
            }
        }
    
        public UsbPrinterHelper(Context context, PrinterCallback callback) {
            this.context = context;
            this.callback = callback;
            usbManager = (UsbManager) context.getSystemService(Context.USB_SERVICE);
        }
    
        /**
         * Register receiver - call this in onResume or when needed
         */
        public void registerReceiver() {
            if (isReceiverRegistered) {
                return;
            }
    
            usbReceiver = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    String action = intent.getAction();
    
                    if (ACTION_USB_PERMISSION.equals(action)) {
                        synchronized (this) {
                            isPermissionRequestPending = false;
                            UsbDevice device = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
    
                            if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
                                if (device != null) {
                                    Log.d(TAG, "Permission granted for device: " + device.getDeviceName());
                                    addDebugInfo("✅ Permission granted for: " + getDeviceInfo(device));
                                    connectDevice(device);
                                }
                            } else {
                                Log.d(TAG, "Permission denied for device " + device);
                                addDebugInfo("❌ Permission denied for: " + (device != null ? getDeviceInfo(device) : "unknown"));
                                if (callback != null) {
                                    callback.onPermissionDenied();
                                    callback.onPrintError("USB permission denied");
                                }
                            }
                        }
                    } else if (UsbManager.ACTION_USB_DEVICE_ATTACHED.equals(action)) {
                        UsbDevice device = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
                        if (device != null) {
                            Log.d(TAG, "USB device attached: " + device.getDeviceName());
                            addDebugInfo("🔌 Device attached: " + getDeviceInfo(device));
                            scanForPrinters();
                            if (isPrinterDevice(device)) {
                                addDebugInfo("Printer detected, requesting permission...");
                                requestPermission(device);
                            }
                        }
                    } else if (UsbManager.ACTION_USB_DEVICE_DETACHED.equals(action)) {
                        UsbDevice device = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
                        if (device != null) {
                            Log.d(TAG, "USB device detached: " + device.getDeviceName());
                            addDebugInfo("🔌 Device detached: " + getDeviceInfo(device));
                            if (usbDevice != null && usbDevice.equals(device)) {
                                isConnected = false;
                                disconnect();
                                if (callback != null) {
                                    callback.onPrinterDisconnected();
                                }
                            }
                        }
                        scanForPrinters();
                    }
                }
            };
    
            IntentFilter filter = new IntentFilter();
            filter.addAction(ACTION_USB_PERMISSION);
            filter.addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED);
            filter.addAction(UsbManager.ACTION_USB_DEVICE_DETACHED);
    
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    context.registerReceiver(usbReceiver, filter, Context.RECEIVER_EXPORTED);
                    isReceiverRegistered = true;
                } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    context.registerReceiver(usbReceiver, filter, Context.RECEIVER_EXPORTED);
                    isReceiverRegistered = true;
                } else {
                    context.registerReceiver(usbReceiver, filter);
                    isReceiverRegistered = true;
                }
                Log.d(TAG, "Broadcast receiver registered successfully");
                addDebugInfo("Receiver registered");
            } catch (Exception e) {
                Log.e(TAG, "Error registering receiver: " + e.getMessage());
                addDebugInfo("Error registering receiver: " + e.getMessage());
                isReceiverRegistered = false;
            }
        }
    
        public void scanForPrinters() {
            availablePrinters.clear();
            HashMap<String, UsbDevice> deviceList = usbManager.getDeviceList();
    
            addDebugInfo("Scanning for printers... Found " + deviceList.size() + " USB devices");
    
            List<PrinterInfo> printerList = new ArrayList<>();
    
            for (UsbDevice device : deviceList.values()) {
                addDebugInfo("Checking device: " + getDeviceInfo(device));
                if (isPrinterDevice(device)) {
                    detectPrinterType(device);
                    PrinterInfo info = new PrinterInfo(device, currentPrinterType);
                    printerList.add(info);
                    availablePrinters.add(device);
                    addDebugInfo("✅ Found printer: " + info.toString());
                }
            }
    
            if (callback != null) {
                callback.onPrintersFound(printerList);
            }
        }
    
        private String getDeviceInfo(UsbDevice device) {
            if (device == null) return "null";
            return String.format("Vendor:0x%04X Product:0x%04X Class:%d Name:%s",
                    device.getVendorId(), device.getProductId(), device.getDeviceClass(), device.getDeviceName());
        }
    
        private void addDebugInfo(String info) {
            debugInfo.append(info).append("\n");
            Log.d(TAG, info);
            if (callback != null) {
                callback.onDebugInfo(info);
            }
        }
    
        public void connectToPrinter(UsbDevice device) {
            if (device == null) {
                addDebugInfo("Cannot connect: device is null");
                return;
            }
    
            addDebugInfo("Connecting to printer: " + getDeviceInfo(device));
    
            if (usbManager.hasPermission(device)) {
                addDebugInfo("Already have permission, connecting...");
                connectDevice(device);
            } else {
                addDebugInfo("Requesting permission...");
                requestPermission(device);
            }
        }
    
        private boolean isPrinterDevice(UsbDevice device) {
            int vendorId = device.getVendorId();
    
            for (int id : COMMON_PRINTER_VENDORS) {
                if (vendorId == id) {
                    Log.d(TAG, "Found printer by vendor ID: " + String.format("0x%04X", vendorId));
                    addDebugInfo("Found printer by vendor ID: " + String.format("0x%04X", vendorId));
                    return true;
                }
            }
    
            if (device.getDeviceClass() == USB_CLASS_PRINTER) {
                addDebugInfo("Found printer by device class");
                return true;
            }
    
            for (int i = 0; i < device.getInterfaceCount(); i++) {
                UsbInterface usbInterface = device.getInterface(i);
                if (usbInterface.getInterfaceClass() == USB_CLASS_PRINTER) {
                    addDebugInfo("Found printer by interface class on interface " + i);
                    return true;
                }
            }
    
            return false;
        }
    
        private void detectPrinterType(UsbDevice device) {
            int vendorId = device.getVendorId();
    
            switch (vendorId) {
                case 0x04B8: // Epson
                    currentPrinterType = PrinterType.ESCPOS;
                    break;
                case 0x03F0: // HP
                    currentPrinterType = PrinterType.PCL;
                    break;
                case 0x04A9: // Canon
                    currentPrinterType = PrinterType.CANON;
                    break;
                case 0x0FE6: // Zebra
                    currentPrinterType = PrinterType.ZEBRA;
                    break;
                case 0x0519: // Star Micronics
                    currentPrinterType = PrinterType.STAR;
                    break;
                default:
                    currentPrinterType = PrinterType.GENERIC;
                    break;
            }
    
            addDebugInfo(String.format("Detected printer type: %s", currentPrinterType));
        }
    
        private void requestPermission(UsbDevice device) {
            if (isPermissionRequestPending) {
                Log.d(TAG, "Permission request already pending");
                addDebugInfo("Permission request already pending");
                return;
            }
    
            this.usbDevice = device;
    
            if (usbManager.hasPermission(device)) {
                Log.d(TAG, "Already have permission for device");
                addDebugInfo("Already have permission for device");
                connectDevice(device);
                return;
            }
    
            try {
                isPermissionRequestPending = true;
                PendingIntent permissionIntent = PendingIntent.getBroadcast(context, 0,
                        new Intent(ACTION_USB_PERMISSION), PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
                usbManager.requestPermission(device, permissionIntent);
                Log.d(TAG, "Permission requested for device: " + device.getDeviceName());
                addDebugInfo("Permission requested - please check for permission dialog");
            } catch (Exception e) {
                Log.e(TAG, "Error requesting permission: " + e.getMessage());
                addDebugInfo("Error requesting permission: " + e.getMessage());
                isPermissionRequestPending = false;
                if (callback != null) {
                    callback.onPrintError("Error requesting USB permission");
                }
            }
        }
    
        private void connectDevice(UsbDevice device) {
            this.usbDevice = device;
    
            addDebugInfo("========== CONNECTING TO PRINTER ==========");
            addDebugInfo("Device: " + getDeviceInfo(device));
    
            detectPrinterType(device);
    
            for (int i = 0; i < device.getInterfaceCount(); i++) {
                UsbInterface usbInterface = device.getInterface(i);
    
                addDebugInfo(String.format("Trying interface %d (Class:%d, Subclass:%d, Protocol:%d)",
                        i, usbInterface.getInterfaceClass(), usbInterface.getInterfaceSubclass(), usbInterface.getInterfaceProtocol()));
    
                try {
                    usbConnection = usbManager.openDevice(device);
    
                    if (usbConnection != null) {
                        addDebugInfo("✅ Device opened successfully");
    
                        if (usbConnection.claimInterface(usbInterface, true)) {
                            addDebugInfo("✅ Interface " + i + " claimed successfully");
    
                            for (int j = 0; j < usbInterface.getEndpointCount(); j++) {
                                UsbEndpoint endpoint = usbInterface.getEndpoint(j);
    
                                if (endpoint.getDirection() == UsbConstants.USB_DIR_OUT) {
                                    usbEndpointOut = endpoint;
                                    addDebugInfo(String.format("✅ Found output endpoint %d: Address=0x%02X, MaxPacket=%d",
                                            j, endpoint.getAddress(), endpoint.getMaxPacketSize()));
                                    break;
                                }
                            }
    
                            if (usbEndpointOut != null) {
                                Log.d(TAG, "USB printer connected successfully");
                                addDebugInfo("✅ USB printer connected successfully!");
                                isConnected = true;
    
                                if (callback != null) {
                                    callback.onPrinterConnected();
                                }
                                return;
                            } else {
                                addDebugInfo("❌ No output endpoint found on interface " + i);
                                usbConnection.releaseInterface(usbInterface);
                                usbConnection.close();
                                usbConnection = null;
                            }
                        } else {
                            addDebugInfo("❌ Could not claim interface " + i);
                            usbConnection.close();
                            usbConnection = null;
                        }
                    } else {
                        addDebugInfo("❌ Could not open device");
                    }
                } catch (Exception e) {
                    addDebugInfo("❌ Error connecting to device: " + e.getMessage());
                    Log.e(TAG, "Error connecting to device: " + e.getMessage(), e);
    
                    if (usbConnection != null) {
                        try {
                            usbConnection.close();
                        } catch (Exception ex) {
                            // Ignore
                        }
                        usbConnection = null;
                    }
                }
            }
    
            isConnected = false;
            addDebugInfo("❌ FAILED: Could not connect to USB printer on any interface");
            Log.e(TAG, "Failed to connect to USB printer");
    
            if (callback != null) {
                callback.onPrintError("Failed to connect to USB printer");
            }
    
            addDebugInfo("==========================================");
        }
    
        // ==================== PRINT METHODS ====================
    
        /**
         * Canon-specific print method
         */
        public boolean printCanonStyle(String text) {
            addDebugInfo("========== CANON STYLE PRINT ==========");
    
            if (!isConnected || usbConnection == null || usbEndpointOut == null) {
                addDebugInfo("❌ Not connected");
                return false;
            }
    
            try {
                String[] lines = text.split("\n");
                StringBuilder canonText = new StringBuilder();
                canonText.append("\r\n\r\n");
    
                for (String line : lines) {
                    canonText.append(line).append("\r\n");
                }
    
                canonText.append("\r\n\r\n");
                canonText.append("\f");
    
                addDebugInfo("Formatted text length: " + canonText.length());
    
                String[] encodings = {"CP437", "CP850", "ISO-8859-1", "UTF-8", "US-ASCII"};
                String[] encodingNames = {"CP437", "CP850", "ISO-8859-1", "UTF-8", "US-ASCII"};
    
                for (int i = 0; i < encodings.length; i++) {
                    try {
                        byte[] data = canonText.toString().getBytes(encodings[i]);
                        addDebugInfo("Trying encoding: " + encodingNames[i] + " (" + data.length + " bytes)");
    
                        int transferred = usbConnection.bulkTransfer(usbEndpointOut, data, data.length, 8000);
    
                        if (transferred == data.length) {
                            addDebugInfo("✅ Success with encoding: " + encodingNames[i]);
    
                            byte[] ff = new byte[]{0x0C};
                            usbConnection.bulkTransfer(usbEndpointOut, ff, ff.length, 1000);
    
                            return true;
                        } else {
                            addDebugInfo("❌ Failed with " + encodingNames[i] + ": " + transferred);
                        }
                    } catch (Exception e) {
                        addDebugInfo("❌ Error with " + encodingNames[i] + ": " + e.getMessage());
                    }
                }
    
                return false;
    
            } catch (Exception e) {
                addDebugInfo("❌ Exception: " + e.getMessage());
                return false;
            }
        }

        /**
         * Send raw byte data directly to the printer
         * This is useful when you have pre-formatted data including PCL commands
         */
        public boolean sendRawData(byte[] data) {
            addDebugInfo("========== SEND RAW DATA ==========");

            if (!isConnected || usbConnection == null || usbEndpointOut == null) {
                addDebugInfo("❌ Not connected");
                if (callback != null) {
                    callback.onPrintError("Printer not connected");
                }
                return false;
            }

            try {
                addDebugInfo("Sending " + data.length + " raw bytes");
                addDebugInfo("Endpoint: 0x" + Integer.toHexString(usbEndpointOut.getAddress()));
                addDebugInfo("Max packet size: " + usbEndpointOut.getMaxPacketSize());

                // For large data, we might need to split into chunks
                int maxPacketSize = usbEndpointOut.getMaxPacketSize();
                int offset = 0;
                int totalTransferred = 0;

                while (offset < data.length) {
                    int chunkSize = Math.min(maxPacketSize, data.length - offset);
                    byte[] chunk = new byte[chunkSize];
                    System.arraycopy(data, offset, chunk, 0, chunkSize);

                    int transferred = usbConnection.bulkTransfer(usbEndpointOut, chunk, chunkSize, 10000);

                    if (transferred < 0) {
                        addDebugInfo("❌ Bulk transfer failed at offset " + offset);
                        if (callback != null) {
                            callback.onPrintError("USB write failed at offset " + offset);
                        }
                        return false;
                    }

                    totalTransferred += transferred;
                    offset += transferred;

                    addDebugInfo("  Transferred chunk: " + transferred + " bytes (total: " + totalTransferred + ")");
                }

                if (totalTransferred == data.length) {
                    addDebugInfo("✅ Successfully sent " + totalTransferred + " bytes");
                    if (callback != null) {
                        callback.onPrintSuccess();
                    }
                    return true;
                } else {
                    addDebugInfo("❌ Failed: sent " + totalTransferred + " of " + data.length + " bytes");
                    if (callback != null) {
                        callback.onPrintError("Sent " + totalTransferred + " of " + data.length + " bytes");
                    }
                    return false;
                }

            } catch (Exception e) {
                addDebugInfo("❌ Exception: " + e.getMessage());
                Log.e(TAG, "Error in sendRawData: " + e.getMessage(), e);
                if (callback != null) {
                    callback.onPrintError("Exception: " + e.getMessage());
                }
                return false;
            }
        }

        /**
         * Overloaded method to send raw data with custom timeout
         */
        public boolean sendRawData(byte[] data, int timeout) {
            addDebugInfo("========== SEND RAW DATA (timeout: " + timeout + "ms) ==========");

            if (!isConnected || usbConnection == null || usbEndpointOut == null) {
                addDebugInfo("❌ Not connected");
                return false;
            }

            try {
                addDebugInfo("Sending " + data.length + " raw bytes");

                int transferred = usbConnection.bulkTransfer(usbEndpointOut, data, data.length, timeout);

                if (transferred == data.length) {
                    addDebugInfo("✅ Successfully sent " + transferred + " bytes");
                    return true;
                } else {
                    addDebugInfo("❌ Failed: sent " + transferred + " of " + data.length + " bytes");
                    return false;
                }
            } catch (Exception e) {
                addDebugInfo("❌ Exception: " + e.getMessage());
                return false;
            }
        }

        /**
         * Send raw string data (converted to bytes using specified encoding)
         */
        public boolean sendRawString(String data, String encoding) {
            addDebugInfo("========== SEND RAW STRING ==========");

            try {
                byte[] bytes = data.getBytes(encoding);
                addDebugInfo("Converted string to " + bytes.length + " bytes using " + encoding);
                return sendRawData(bytes);
            } catch (UnsupportedEncodingException e) {
                addDebugInfo("❌ Encoding not supported: " + encoding);
                return false;
            }
        }

        /**
         * PCL print method
         */
        public boolean printPCL(String text) {
            addDebugInfo("========== PCL PRINT ==========");
    
            if (!isConnected || usbConnection == null || usbEndpointOut == null) {
                addDebugInfo("❌ Not connected");
                return false;
            }
    
            try {
                StringBuilder pcl = new StringBuilder();
                pcl.append("\u001BE"); // Reset
                pcl.append("\u001B&l1O"); // Portrait orientation
                pcl.append("\u001B(s10H"); // 10 pitch
                pcl.append("\u001B(s1Q"); // Quality
                pcl.append("\r\n\r\n");
                pcl.append(text);
                pcl.append("\r\n\r\n");
                pcl.append("\u001B&l0H"); // Form feed
    
                byte[] data = pcl.toString().getBytes("CP437");
    
                addDebugInfo("Sending " + data.length + " bytes of PCL");
    
                int transferred = usbConnection.bulkTransfer(usbEndpointOut, data, data.length, 8000);
    
                if (transferred == data.length) {
                    addDebugInfo("✅ PCL print successful");
                    return true;
                } else {
                    addDebugInfo("❌ PCL failed: " + transferred + " of " + data.length);
                    return false;
                }
            } catch (Exception e) {
                addDebugInfo("❌ Exception: " + e.getMessage());
                return false;
            }
        }
    
        /**
         * ASCII print method
         */
        public boolean printASCII(String text) {
            addDebugInfo("========== ASCII PRINT ==========");
    
            if (!isConnected || usbConnection == null || usbEndpointOut == null) {
                addDebugInfo("❌ Not connected");
                return false;
            }
    
            try {
                StringBuilder ascii = new StringBuilder();
                ascii.append("\n\n");
                ascii.append("================================\n");
                ascii.append("        WEIGHMENT TICKET        \n");
                ascii.append("================================\n");
                ascii.append("\n");
    
                for (char c : text.toCharArray()) {
                    if (c <= 127) {
                        ascii.append(c);
                    } else {
                        ascii.append('?');
                    }
                }
    
                ascii.append("\n\n");
                ascii.append("================================\n");
                ascii.append("\n\n\n");
                ascii.append("\f");
    
                byte[] data = ascii.toString().getBytes("US-ASCII");
    
                addDebugInfo("Sending " + data.length + " bytes of ASCII");
    
                int transferred = usbConnection.bulkTransfer(usbEndpointOut, data, data.length, 8000);
    
                if (transferred == data.length) {
                    addDebugInfo("✅ ASCII print successful");
                    return true;
                } else {
                    addDebugInfo("❌ ASCII failed: " + transferred + " of " + data.length);
                    return false;
                }
            } catch (Exception e) {
                addDebugInfo("❌ Exception: " + e.getMessage());
                return false;
            }
        }
    
        /**
         * Simple test to check if printer is responding
         */
        public boolean simpleTestPrint() {
            addDebugInfo("========== SIMPLE TEST PRINT ==========");
    
            if (!isConnected || usbConnection == null || usbEndpointOut == null) {
                addDebugInfo("❌ Not connected");
                return false;
            }
    
            try {
                byte[] testData = new byte[]{0x0A, 0x0D, 0x0A, 0x0D};
    
                addDebugInfo("Sending simple test (4 bytes)");
    
                int transferred = usbConnection.bulkTransfer(usbEndpointOut, testData, testData.length, 3000);
    
                addDebugInfo("Result: " + transferred);
    
                if (transferred == 4) {
                    addDebugInfo("✅ Simple test PASSED - printer responded");
                    return true;
                } else {
                    addDebugInfo("❌ Simple test FAILED - no response");
                    return false;
                }
            } catch (Exception e) {
                addDebugInfo("❌ Exception: " + e.getMessage());
                return false;
            }
        }
    
        /**
         * Very simple print method that just sends text as-is
         */
        public boolean printSimple(String text) {
            addDebugInfo("========== SIMPLE PRINT ==========");
    
            if (!isConnected || usbConnection == null || usbEndpointOut == null) {
                addDebugInfo("❌ Not connected");
                return false;
            }
    
            try {
                String printData = "\n\n" + text + "\n\n\n";
                byte[] data = printData.getBytes("UTF-8");
    
                addDebugInfo("Sending " + data.length + " bytes");
    
                int transferred = usbConnection.bulkTransfer(usbEndpointOut, data, data.length, 5000);
    
                if (transferred == data.length) {
                    addDebugInfo("✅ Simple print successful");
                    return true;
                } else {
                    addDebugInfo("❌ Failed: " + transferred + " of " + data.length);
                    return false;
                }
            } catch (Exception e) {
                addDebugInfo("❌ Exception: " + e.getMessage());
                return false;
            }
        }
    
        /**
         * RAW printing - just send raw bytes without any formatting
         */
        public boolean printRaw(String text) {
            addDebugInfo("========== RAW PRINT ==========");
    
            if (!isConnected || usbConnection == null || usbEndpointOut == null) {
                addDebugInfo("❌ Not connected");
                return false;
            }
    
            try {
                byte[] data = text.getBytes("UTF-8");
    
                addDebugInfo("Sending " + data.length + " raw bytes");
    
                int transferred = usbConnection.bulkTransfer(usbEndpointOut, data, data.length, 8000);
    
                if (transferred == data.length) {
                    addDebugInfo("✅ RAW print successful");
    
                    byte[] ff = new byte[]{0x0C};
                    usbConnection.bulkTransfer(usbEndpointOut, ff, ff.length, 1000);
    
                    return true;
                } else {
                    addDebugInfo("❌ Failed: " + transferred + " of " + data.length);
                    return false;
                }
            } catch (Exception e) {
                addDebugInfo("❌ Exception: " + e.getMessage());
                return false;
            }
        }
    
        /**
         * Try different paper feed and formatting commands
         */
        public boolean tryAllPaperCommands(String text) {
            addDebugInfo("========== TRYING PAPER COMMANDS ==========");
    
            if (!isConnected || usbConnection == null || usbEndpointOut == null) {
                addDebugInfo("❌ Not connected");
                return false;
            }
    
            String[][] testPatterns = {
                    {"Simple FF", "\n\n" + text + "\n\n\f"},
                    {"Multiple FF", "\n\n" + text + "\n\n\f\f\f"},
                    {"Only LF", "\n\n" + text + "\n\n\n\n"},
                    {"CR+LF", "\r\n\r\n" + text.replace("\n", "\r\n") + "\r\n\r\n"},
                    {"With Cut", "\n\n" + text + "\n\n" + (char)0x1D + (char)0x56 + (char)0x41},
                    {"Many Newlines", "\n\n\n\n" + text + "\n\n\n\n\f"},
                    {"Text+FF", text + "\f"},
                    {"Page Eject", "\n\n" + text + "\n\n" + (char)0x0C},
                    {"Reset First", (char)0x1B + (char)0x40 + "\n\n" + text + "\n\n\f"},
                    {"Canon Specific", "\n\n" + text + "\n\n" + (char)0x1B + (char)0x69}
            };
    
            boolean anySuccess = false;
    
            for (String[] pattern : testPatterns) {
                try {
                    addDebugInfo("\n📄 Testing: " + pattern[0]);
                    byte[] data = pattern[1].getBytes("UTF-8");
                    addDebugInfo("  Length: " + data.length + " bytes");
    
                    int transferred = usbConnection.bulkTransfer(usbEndpointOut, data, data.length, 5000);
    
                    if (transferred == data.length) {
                        addDebugInfo("  ✅ Sent " + transferred + " bytes");
                        anySuccess = true;
                    } else {
                        addDebugInfo("  ❌ Failed: " + transferred + " of " + data.length);
                    }
    
                    Thread.sleep(2000);
    
                } catch (Exception e) {
                    addDebugInfo("  ❌ Error: " + e.getMessage());
                }
            }
    
            addDebugInfo("==========================================");
            return anySuccess;
        }
    
        /**
         * Send raw ESC/POS commands
         */
        public boolean sendESCCommands() {
            addDebugInfo("========== SENDING ESC COMMANDS ==========");
    
            if (!isConnected || usbConnection == null || usbEndpointOut == null) {
                addDebugInfo("❌ Not connected");
                return false;
            }
    
            try {
                byte[][] commands = {
                        new byte[]{0x1B, 0x40}, // Initialize printer
                        new byte[]{0x1B, 0x32}, // Set line spacing
                        new byte[]{0x1B, 0x4D, 0x00}, // Select font
                        "TEST LINE 1\n".getBytes(),
                        "TEST LINE 2\n".getBytes(),
                        new byte[]{0x0C}, // Form feed
                        new byte[]{0x1D, 0x56, 0x41} // Cut paper
                };
    
                for (byte[] cmd : commands) {
                    int transferred = usbConnection.bulkTransfer(usbEndpointOut, cmd, cmd.length, 2000);
                    addDebugInfo("Sent " + cmd.length + " bytes: " + transferred);
                    Thread.sleep(500);
                }
    
                addDebugInfo("✅ All ESC commands sent");
                return true;
    
            } catch (Exception e) {
                addDebugInfo("❌ Error: " + e.getMessage());
                return false;
            }
        }
        /**
         * Canon specific initialization and print
         * Based on common Canon printer protocols
         */
        public boolean canonDirectPrint(String text) {
            addDebugInfo("========== CANON DIRECT PRINT ==========");
    
            if (!isConnected || usbConnection == null || usbEndpointOut == null) {
                addDebugInfo("❌ Not connected");
                return false;
            }
    
            try {
                // Canon printers often expect data in specific chunks
                // First, send initialization sequence
                byte[][] initSequences = {
                        new byte[]{0x1B, 0x40}, // ESC @ - Initialize
                        new byte[]{0x1B, 0x32}, // ESC 2 - Set line spacing
                        new byte[]{0x1B, 0x61, 0x00}, // ESC a 0 - Left alignment
                        new byte[]{0x1B, 0x21, 0x00}, // ESC ! 0 - Normal text
                };
    
                for (byte[] cmd : initSequences) {
                    int result = usbConnection.bulkTransfer(usbEndpointOut, cmd, cmd.length, 2000);
                    addDebugInfo("Init command result: " + result);
                    Thread.sleep(100);
                }
    
                // Now send the text in chunks
                String[] lines = text.split("\n");
                for (String line : lines) {
                    String lineWithNewline = line + "\r\n";
                    byte[] lineData = lineWithNewline.getBytes("CP437");
    
                    // Send line
                    int transferred = usbConnection.bulkTransfer(usbEndpointOut, lineData, lineData.length, 5000);
                    addDebugInfo("Line sent: " + transferred + " bytes");
    
                    if (transferred != lineData.length) {
                        addDebugInfo("❌ Failed to send line");
                        return false;
                    }
    
                    Thread.sleep(50);
                }
    
                // Send form feed and cut commands
                byte[] formFeed = new byte[]{0x0C}; // Form feed
                usbConnection.bulkTransfer(usbEndpointOut, formFeed, formFeed.length, 1000);
    
                // Try cut command (may not work on all printers)
                byte[] cut = new byte[]{0x1D, 0x56, 0x41}; // GS V A
                usbConnection.bulkTransfer(usbEndpointOut, cut, cut.length, 1000);
    
                addDebugInfo("✅ Canon direct print complete");
                return true;
    
            } catch (Exception e) {
                addDebugInfo("❌ Exception: " + e.getMessage());
                return false;
            }
        }
        /**
         * Try PCL commands (common for Canon)
         */
        public boolean sendPCLCommands() {
            addDebugInfo("========== SENDING PCL COMMANDS ==========");
    
            if (!isConnected || usbConnection == null || usbEndpointOut == null) {
                addDebugInfo("❌ Not connected");
                return false;
            }
    
            try {
                byte esc = 0x1B;
    
                byte[][] commands = {
                        new byte[]{esc, 'E'}, // PCL Reset
                        new byte[]{esc, '&', 'l', '2', 'A'}, // Page Size - Letter
                        new byte[]{esc, '&', 'l', '0', 'O'}, // Portrait
                        new byte[]{esc, '(', 's', '1', '0', 'H'}, // Font
                        "This is a PCL test\n".getBytes(),
                        "Line 2 of test\n".getBytes(),
                        new byte[]{esc, '&', 'l', '0', 'H'}, // Form Feed
                        new byte[]{0x0C} // Form feed
                };
    
                for (byte[] cmd : commands) {
                    int transferred = usbConnection.bulkTransfer(usbEndpointOut, cmd, cmd.length, 2000);
                    addDebugInfo("Sent " + cmd.length + " bytes: " + transferred);
                    Thread.sleep(500);
                }
    
                addDebugInfo("✅ All PCL commands sent");
                return true;
    
            } catch (Exception e) {
                addDebugInfo("❌ Error: " + e.getMessage());
                return false;
            }
        }
    
        /**
         * Check printer status and capabilities
         */
        public void checkPrinterStatus() {
            addDebugInfo("========== PRINTER STATUS CHECK ==========");
    
            if (!isConnected || usbConnection == null) {
                addDebugInfo("❌ Not connected");
                return;
            }
    
            try {
                byte[] deviceId = new byte[255];
                int result = usbConnection.controlTransfer(
                        0xA1, 0x00, 0x0000, 0x00, deviceId, deviceId.length, 5000
                );
    
                if (result > 0) {
                    String id = new String(deviceId, 0, result, "UTF-8");
                    addDebugInfo("✅ Got device ID: " + id);
                } else {
                    addDebugInfo("❌ Could not get device ID: " + result);
                }
    
                byte[] portStatus = new byte[1];
                result = usbConnection.controlTransfer(
                        0xA1, 0x01, 0x0000, 0x00, portStatus, 1, 5000
                );
    
                if (result == 1) {
                    int status = portStatus[0] & 0xFF;
                    addDebugInfo("Port status: 0x" + Integer.toHexString(status));
    
                    if ((status & 0x10) != 0) {
                        addDebugInfo("✅ Printer is selected and ready");
                    }
                    if ((status & 0x08) != 0) {
                        addDebugInfo("❌ Paper empty");
                    }
                    if ((status & 0x20) != 0) {
                        addDebugInfo("❌ Printer error");
                    }
                }
    
            } catch (Exception e) {
                addDebugInfo("❌ Error checking status: " + e.getMessage());
            }
    
            addDebugInfo("==========================================");
        }
    
        /**
         * Try all endpoints to find one that works
         */
        public boolean connectWithAllEndpoints() {
            addDebugInfo("========== TRYING ALL ENDPOINTS ==========");
    
            if (usbDevice == null) {
                addDebugInfo("❌ No device");
                return false;
            }
    
            boolean foundWorking = false;
    
            for (int i = 0; i < usbDevice.getInterfaceCount(); i++) {
                UsbInterface intf = usbDevice.getInterface(i);
                addDebugInfo("\nTrying Interface " + i);
    
                UsbDeviceConnection testConn = usbManager.openDevice(usbDevice);
                if (testConn == null) {
                    addDebugInfo("❌ Cannot open device");
                    continue;
                }
    
                if (!testConn.claimInterface(intf, true)) {
                    addDebugInfo("❌ Cannot claim interface");
                    testConn.close();
                    continue;
                }
    
                for (int j = 0; j < intf.getEndpointCount(); j++) {
                    UsbEndpoint ep = intf.getEndpoint(j);
                    if (ep.getDirection() == UsbConstants.USB_DIR_OUT) {
                        addDebugInfo("Testing OUT endpoint " + j +
                                " (0x" + Integer.toHexString(ep.getAddress()) +
                                ", max packet: " + ep.getMaxPacketSize() + ")");
    
                        byte[] test = new byte[]{0x0A, 0x0D};
                        int result = testConn.bulkTransfer(ep, test, test.length, 2000);
    
                        if (result == 2) {
                            addDebugInfo("✅ Endpoint works!");
                            foundWorking = true;
    
                            if (usbEndpointOut == null ||
                                    ep.getMaxPacketSize() > usbEndpointOut.getMaxPacketSize()) {
                                usbEndpointOut = ep;
                                addDebugInfo("📝 Using this endpoint");
                            }
                        } else {
                            addDebugInfo("❌ Endpoint failed: " + result);
                        }
                    }
                }
    
                testConn.releaseInterface(intf);
                testConn.close();
            }
    
            addDebugInfo("==========================================");
            return foundWorking;
        }
    
        /**
         * Comprehensive USB diagnostics
         */
        public void comprehensiveDiagnostics() {
            addDebugInfo("========== COMPREHENSIVE USB DIAGNOSTICS ==========");
    
            addDebugInfo("\n1. USB Manager Check:");
            if (usbManager == null) {
                addDebugInfo("   ❌ USB Manager is null");
            } else {
                addDebugInfo("   ✅ USB Manager exists");
            }
    
            addDebugInfo("\n2. Connected USB Devices:");
            HashMap<String, UsbDevice> deviceList = usbManager.getDeviceList();
            if (deviceList.isEmpty()) {
                addDebugInfo("   ❌ No USB devices connected");
            } else {
                addDebugInfo("   ✅ Found " + deviceList.size() + " USB devices:");
                for (UsbDevice device : deviceList.values()) {
                    addDebugInfo("   - " + getDeviceInfo(device));
                    boolean hasPermission = usbManager.hasPermission(device);
                    addDebugInfo("     Permission: " + (hasPermission ? "✅ Granted" : "❌ Denied"));
                }
            }
    
            addDebugInfo("\n3. Current Connection State:");
            addDebugInfo("   isConnected flag: " + isConnected);
            addDebugInfo("   usbDevice: " + (usbDevice != null ? "✅ Present" : "❌ Null"));
            addDebugInfo("   usbConnection: " + (usbConnection != null ? "✅ Open" : "❌ Closed"));
            addDebugInfo("   usbEndpointOut: " + (usbEndpointOut != null ? "✅ Found" : "❌ Not found"));
    
            if (usbDevice != null) {
                addDebugInfo("   Current Device: " + getDeviceInfo(usbDevice));
            }
    
            if (usbDevice != null) {
                addDebugInfo("\n4. Detailed Endpoint Analysis:");
                for (int i = 0; i < usbDevice.getInterfaceCount(); i++) {
                    UsbInterface intf = usbDevice.getInterface(i);
                    addDebugInfo("   Interface " + i + ":");
                    addDebugInfo("     Class: " + intf.getInterfaceClass());
                    addDebugInfo("     Subclass: " + intf.getInterfaceSubclass());
                    addDebugInfo("     Protocol: " + intf.getInterfaceProtocol());
                    addDebugInfo("     Endpoints: " + intf.getEndpointCount());
    
                    for (int j = 0; j < intf.getEndpointCount(); j++) {
                        UsbEndpoint ep = intf.getEndpoint(j);
                        String direction = ep.getDirection() == UsbConstants.USB_DIR_OUT ? "OUT" : "IN";
                        addDebugInfo("     Endpoint " + j + ": " + direction +
                                ", Address: 0x" + Integer.toHexString(ep.getAddress()) +
                                ", Max Packet: " + ep.getMaxPacketSize());
                    }
                }
            }
    
            addDebugInfo("\n5. Testing Current Connection:");
            if (isConnected && usbEndpointOut != null) {
                try {
                    byte[] testData = new byte[]{0x0A};
                    int result = usbConnection.bulkTransfer(usbEndpointOut, testData, testData.length, 2000);
                    addDebugInfo("   Test write result: " + result);
                } catch (Exception e) {
                    addDebugInfo("   ❌ Test write failed: " + e.getMessage());
                }
            }
    
            addDebugInfo("\n========== DIAGNOSTICS COMPLETE ==========");
        }
        /**
         * Canon-specific ESC/POS commands
         */
        public boolean printCanonESCPOS(String text) {
            addDebugInfo("========== CANON ESC/POS ==========");
    
            try {
                ByteArrayOutputStream output = new ByteArrayOutputStream();
    
                // Initialize printer
                output.write(0x1B);
                output.write(0x40); // ESC @
    
                // Set character set (Canon specific)
                output.write(0x1B);
                output.write(0x52);
                output.write(0x06); // Code page 437
    
                // Set text size normal
                output.write(0x1D);
                output.write(0x21);
                output.write(0x00);
    
                // Print text with line feeds
                String[] lines = text.split("\n");
                for (String line : lines) {
                    output.write(line.getBytes("CP437"));
                    output.write(0x0D); // CR
                    output.write(0x0A); // LF
                }
    
                // Cut paper (Canon specific)
                output.write(0x1B);
                output.write(0x69); // ESC i - cut
    
                // Form feed
                output.write(0x0C);
    
                byte[] data = output.toByteArray();
                int transferred = usbConnection.bulkTransfer(usbEndpointOut, data, data.length, 10000);
    
                return transferred == data.length;
            } catch (Exception e) {
                Log.e("Print", "Error: " + e.getMessage());
                return false;
            }
        }
        /**
         * Direct USB write - sends raw bytes to printer
         */
        public boolean directUsbWrite(byte[] data) {
            addDebugInfo("========== DIRECT USB WRITE ==========");
    
            if (!isConnected || usbConnection == null || usbEndpointOut == null) {
                addDebugInfo("❌ Not connected");
                return false;
            }
    
            try {
                addDebugInfo("Writing " + data.length + " bytes directly to USB");
                addDebugInfo("Endpoint: 0x" + Integer.toHexString(usbEndpointOut.getAddress()));
                addDebugInfo("Max packet size: " + usbEndpointOut.getMaxPacketSize());
    
                int transferred = usbConnection.bulkTransfer(usbEndpointOut, data, data.length, 10000);
    
                if (transferred == data.length) {
                    addDebugInfo("✅ Successfully wrote " + transferred + " bytes");
                    return true;
                } else {
                    addDebugInfo("❌ Failed: wrote " + transferred + " of " + data.length + " bytes");
                    return false;
                }
            } catch (Exception e) {
                addDebugInfo("❌ Exception: " + e.getMessage());
                return false;
            }
        }
    
        /**
         * Force reconnect
         */
        public void forceReconnect() {
            addDebugInfo("========== FORCE RECONNECT ==========");
    
            if (usbDevice == null) {
                addDebugInfo("❌ No device to reconnect to");
                return;
            }
    
            disconnect();
    
            try {
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
    
            connectToPrinter(usbDevice);
    
            addDebugInfo("======================================");
        }
    
        /**
         * Print text using default method
         */
        public boolean printText(String text) {
            addDebugInfo("========== PRINT TEXT ==========");
    
            if (!isConnected || usbConnection == null || usbEndpointOut == null) {
                addDebugInfo("❌ Not connected");
                if (callback != null) {
                    callback.onPrintError("Printer not connected");
                }
                return false;
            }
    
            try {
                String printData = text + "\n\n\f";
                byte[] data = printData.getBytes("UTF-8");
    
                addDebugInfo("Sending " + data.length + " bytes");
    
                int transferred = usbConnection.bulkTransfer(usbEndpointOut, data, data.length, 10000);
    
                if (transferred == data.length) {
                    addDebugInfo("✅ Print successful");
                    if (callback != null) {
                        callback.onPrintSuccess();
                    }
                    return true;
                } else {
                    addDebugInfo("❌ Failed: " + transferred + " of " + data.length);
                    if (callback != null) {
                        callback.onPrintError("Print failed: sent " + transferred + " bytes");
                    }
                    return false;
                }
            } catch (Exception e) {
                addDebugInfo("❌ Exception: " + e.getMessage());
                if (callback != null) {
                    callback.onPrintError("Exception: " + e.getMessage());
                }
                return false;
            }
        }
    
        /**
         * Print plain text
         */
        public boolean printPlainText(String text) {
            return printText(text);
        }
    
        /**
         * Auto detect and print
         */
        public boolean autoDetectAndPrint(String text) {
            addDebugInfo("========== AUTO DETECT AND PRINT ==========");
    
            if (!isConnected || usbConnection == null || usbEndpointOut == null) {
                addDebugInfo("❌ Not connected");
                return false;
            }
    
            if (printCanonStyle(text)) {
                addDebugInfo("✅ Canon style worked");
                return true;
            }
    
            if (printPCL(text)) {
                addDebugInfo("✅ PCL worked");
                return true;
            }
    
            if (printASCII(text)) {
                addDebugInfo("✅ ASCII worked");
                return true;
            }
    
            if (printSimple(text)) {
                addDebugInfo("✅ Simple print worked");
                return true;
            }
    
            if (printRaw(text)) {
                addDebugInfo("✅ Raw print worked");
                return true;
            }
    
            if (sendESCCommands()) {
                addDebugInfo("✅ ESC commands worked");
                return true;
            }
    
            if (sendPCLCommands()) {
                addDebugInfo("✅ PCL commands worked");
                return true;
            }
    
            return tryAllPaperCommands(text);
        }
        /**
         * Comprehensive test that tries all possible print formats
         * Based on the fact that NokoPrint works
         */
        public boolean tryAllPossibleFormats() {
            addDebugInfo("========== TRYING ALL POSSIBLE FORMATS ==========");
    
            if (!isConnected || usbConnection == null || usbEndpointOut == null) {
                addDebugInfo("❌ Not connected");
                return false;
            }
    
            String testText = "TEST PRINT\nNokoPrint Compatible Test\n" +
                    new SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault()).format(new Date()) +
                    "\n\n";
    
            // Different formats that NokoPrint might use
            Object[][] tests = {
                    {"Plain Text with CR+LF+FF", (testText + "\f").replace("\n", "\r\n")},
                    {"PCL Simple", "\u001B%-12345X" + testText + "\u001B%-12345X"},
                    {"PCL Reset", "\u001BE" + testText + "\u001B&l0H"},
                    {"ESC/POS", "\u001B@" + testText + "\u001Bd\u0004" + "\u001DVA"},
                    {"Windows Format", (testText + "\r\n\r\n").replace("\n", "\r\n")},
                    {"Page Eject", testText + "\u000C"},
                    {"Multiple FF", testText + "\f\f\f"},
                    {"Reset+Print", "\u001B\u0040" + testText + "\u001B\u0069"},
                    {"PDF Header", "%PDF-1.4\n" + testText + "\n%%EOF"},
                    {"PostScript", "%!\n/Times-Roman findfont\n12 scalefont setfont\n" +
                            "100 700 moveto\n(" + testText + ") show\nshowpage"},
                    {"HP GL/2", "\u001B%1BIN;SP1;PA1000,1000;PD1000,2000;PU;"},
                    {"Simple", "\n\n\n" + testText + "\n\n\n"},
                    {"Canon CAPT", "\u001B%9" + testText + "\u001B%0"},
                    {"DVI", "\\documentclass{article}\\begin{document}" + testText + "\\end{document}"},
                    {"TIFF", "II*\u0000"},
            };
    
            boolean anySuccess = false;
            String[] encodings = {"UTF-8", "CP437", "CP850", "ISO-8859-1", "US-ASCII"};
    
            for (Object[] test : tests) {
                String formatName = (String) test[0];
                String dataStr = (String) test[1];
    
                addDebugInfo("\n📄 Testing: " + formatName);
                addDebugInfo("Data length: " + dataStr.length() + " chars");
    
                for (String encoding : encodings) {
                    try {
                        byte[] data = dataStr.getBytes(encoding);
                        addDebugInfo("  Encoding: " + encoding + " (" + data.length + " bytes)");
    
                        int transferred = usbConnection.bulkTransfer(usbEndpointOut, data, data.length, 8000);
    
                        if (transferred == data.length) {
                            addDebugInfo("  ✅ SUCCESS with " + formatName + " (" + encoding + ")");
                            anySuccess = true;
    
                            // Wait a bit to see if printer responds
                            try {
                                Thread.sleep(3000);
                            } catch (InterruptedException e) {
                                Thread.currentThread().interrupt();
                            }
                            break; // Found working encoding for this format
                        } else {
                            addDebugInfo("  ❌ Failed: " + transferred + " of " + data.length);
                        }
                    } catch (Exception e) {
                        addDebugInfo("  ❌ Error with " + encoding + ": " + e.getMessage());
                    }
                }
    
                // Wait between different formats
                try {
                    Thread.sleep(2000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
    
            addDebugInfo("\n✅ All tests completed. Any success: " + anySuccess);
            addDebugInfo("===============================================");
    
            return anySuccess;
        }
    
        /**
         * Try to print using the most basic USB write possible
         */
        /**
         * Try every possible endpoint configuration
         */
        public void tryEveryEndpoint() {
            addDebugInfo("========== TRYING EVERY ENDPOINT ==========");
    
            if (usbDevice == null) {
                addDebugInfo("❌ No device");
                return;
            }
    
            String testText = "TEST\n";
            byte[] testData;
            try {
                testData = testText.getBytes("UTF-8");
            } catch (Exception e) {
                testData = testText.getBytes();
            }
    
            for (int i = 0; i < usbDevice.getInterfaceCount(); i++) {
                UsbInterface intf = usbDevice.getInterface(i);
                addDebugInfo("\n=== Testing Interface " + i + " ===");
    
                UsbDeviceConnection testConn = null;
    
                try {
                    testConn = usbManager.openDevice(usbDevice);
                    if (testConn == null) {
                        addDebugInfo("❌ Cannot open device");
                        continue;
                    }
    
                    // Try with and without claiming interface
                    boolean claimed = testConn.claimInterface(intf, true);
                    addDebugInfo("Interface claimed: " + claimed);
    
                    for (int j = 0; j < intf.getEndpointCount(); j++) {
                        UsbEndpoint ep = intf.getEndpoint(j);
                        String direction = ep.getDirection() == UsbConstants.USB_DIR_OUT ? "OUT" : "IN";
    
                        addDebugInfo("  Endpoint " + j + ": " + direction +
                                " (0x" + Integer.toHexString(ep.getAddress()) + ")");
    
                        // Try writing to OUT endpoints
                        if (ep.getDirection() == UsbConstants.USB_DIR_OUT) {
                            addDebugInfo("    Attempting write...");
    
                            int transferred = testConn.bulkTransfer(ep, testData, testData.length, 2000);
    
                            if (transferred == testData.length) {
                                addDebugInfo("    ✅ SUCCESS! This endpoint works!");
    
                                // Save this endpoint
                                if (usbEndpointOut == null) {
                                    usbEndpointOut = ep;
                                    addDebugInfo("    📝 Saved as working endpoint");
                                }
                            } else {
                                addDebugInfo("    ❌ Failed: " + transferred);
                            }
                        }
                    }
    
                    if (claimed) {
                        testConn.releaseInterface(intf);
                    }
    
                } catch (Exception e) {
                    addDebugInfo("❌ Error: " + e.getMessage());
                } finally {
                    if (testConn != null) {
                        testConn.close();
                    }
                }
            }
    
            addDebugInfo("============================================");
        }
        /**
         * Completely reset the USB connection
         */
        public void hardReset() {
            addDebugInfo("========== HARD RESET ==========");
    
            disconnect();
    
            try {
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
    
            // Re-scan for devices
            HashMap<String, UsbDevice> deviceList = usbManager.getDeviceList();
    
            for (UsbDevice device : deviceList.values()) {
                if (isPrinterDevice(device)) {
                    addDebugInfo("Found printer again: " + getDeviceInfo(device));
                    usbDevice = device;
    
                    if (usbManager.hasPermission(device)) {
                        connectDevice(device);
                    } else {
                        requestPermission(device);
                    }
                    break;
                }
            }
    
            addDebugInfo("=================================");
        }
        public boolean ultraBasicPrint(String text) {
            addDebugInfo("========== ULTRA BASIC PRINT ==========");
    
            if (!isConnected || usbConnection == null || usbEndpointOut == null) {
                addDebugInfo("❌ Not connected");
                return false;
            }
    
            try {
                // The absolute simplest: just send the bytes with NO formatting
                byte[] data = text.getBytes("UTF-8");
    
                addDebugInfo("Sending " + data.length + " raw bytes with NO formatting");
    
                // Try with a very short timeout first
                int transferred = usbConnection.bulkTransfer(usbEndpointOut, data, data.length, 5000);
    
                if (transferred == data.length) {
                    addDebugInfo("✅ Data sent successfully");
    
                    // Try to eject paper
                    byte[] ff = new byte[]{0x0C};
                    usbConnection.bulkTransfer(usbEndpointOut, ff, ff.length, 1000);
    
                    return true;
                } else {
                    addDebugInfo("❌ Failed: " + transferred + " of " + data.length);
                    return false;
                }
            } catch (Exception e) {
                addDebugInfo("❌ Exception: " + e.getMessage());
                return false;
            }
        }
    
        public void disconnect() {
            if (usbConnection != null) {
                try {
                    usbConnection.close();
                    addDebugInfo("Connection closed");
                } catch (Exception e) {
                    Log.e(TAG, "Error closing connection: " + e.getMessage());
                }
                usbConnection = null;
            }
            usbEndpointOut = null;
            usbDevice = null;
            currentPrinterType = PrinterType.UNKNOWN;
            isConnected = false;
        }
    
        public void unregisterReceiver() {
            try {
                if (isReceiverRegistered && usbReceiver != null) {
                    context.unregisterReceiver(usbReceiver);
                    isReceiverRegistered = false;
                    Log.d(TAG, "Broadcast receiver unregistered");
                    addDebugInfo("Receiver unregistered");
                }
            } catch (IllegalArgumentException e) {
                Log.e(TAG, "Error unregistering receiver: " + e.getMessage());
            }
        }
    
        public boolean isConnected() {
            boolean connected = isConnected && usbConnection != null && usbEndpointOut != null;
            Log.d(TAG, "isConnected check: " + connected);
            return connected;
        }
    
        public PrinterType getCurrentPrinterType() {
            return currentPrinterType;
        }
    
        public String getDebugInfo() {
            return debugInfo.toString();
        }
    
        private String bytesToHex(byte[] bytes) {
            StringBuilder sb = new StringBuilder();
            for (byte b : bytes) {
                sb.append(String.format("%02X ", b));
            }
            return sb.toString();
        }
    
        public void shutdown() {
            if (executor != null) {
                executor.shutdownNow();
            }
            disconnect();
        }
    }