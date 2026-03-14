package com.googleapi.bluetoothweight;

import android.content.Context;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class WiFiPrinterHelper {

    private static final String TAG = "WiFiPrinterHelper";
    private static final int DEFAULT_PRINT_PORT = 9100; // Default port for most network printers
    private static final int SOCKET_TIMEOUT = 5000; // 5 seconds
    private static final int CONNECTION_TIMEOUT = 3000; // 3 seconds

    private Context context;
    private Socket printerSocket;
    private OutputStream outputStream;
    private PrintWriter printWriter;
    private String connectedPrinterIp;
    private String connectedPrinterName;
    private boolean isConnected = false;

    private ExecutorService executor = Executors.newSingleThreadExecutor();
    private Handler mainHandler = new Handler(Looper.getMainLooper());

    // Callback interface
    public interface WiFiPrinterCallback {
        void onPrinterConnected(String printerName, String ipAddress);
        void onPrinterDisconnected();
        void onPrintSuccess();
        void onPrintError(String error);
        void onPrintersFound(List<WiFiPrinterInfo> printers);
        void onDebugInfo(String info);
    }

    // WiFi Printer Info class
    public static class WiFiPrinterInfo {
        public String ipAddress;
        public String printerName;
        public String manufacturer;
        public int port;
        public boolean isReachable;

        public WiFiPrinterInfo(String ipAddress, String printerName, int port) {
            this.ipAddress = ipAddress;
            this.printerName = printerName;
            this.port = port;
            this.manufacturer = "Unknown";
            this.isReachable = false;
        }

        public WiFiPrinterInfo(String ipAddress, String printerName, String manufacturer, int port) {
            this.ipAddress = ipAddress;
            this.printerName = printerName;
            this.manufacturer = manufacturer;
            this.port = port;
            this.isReachable = false;
        }

        @Override
        public String toString() {
            return printerName + " (" + ipAddress + ":" + port + ")";
        }
    }

    private WiFiPrinterCallback callback;

    public WiFiPrinterHelper(Context context) {
        this.context = context;
    }

    public WiFiPrinterHelper(Context context, WiFiPrinterCallback callback) {
        this.context = context;
        this.callback = callback;
    }

    /**
     * Discover WiFi printers on the local network
     * This scans common IP ranges for printers
     */
    public List<WiFiPrinterInfo> discoverPrinters() {
        List<WiFiPrinterInfo> printers = new ArrayList<>();

        addDebugInfo("Starting WiFi printer discovery...");

        // Get current WiFi network info
        WifiManager wifiManager = (WifiManager) context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        if (wifiManager == null || !wifiManager.isWifiEnabled()) {
            addDebugInfo("WiFi is not enabled");
            return printers;
        }

        WifiInfo wifiInfo = wifiManager.getConnectionInfo();
        if (wifiInfo == null) {
            addDebugInfo("Not connected to any WiFi network");
            return printers;
        }

        int ipAddress = wifiInfo.getIpAddress();
        String ipString = String.format("%d.%d.%d.%d",
                (ipAddress & 0xff),
                (ipAddress >> 8 & 0xff),
                (ipAddress >> 16 & 0xff),
                (ipAddress >> 24 & 0xff));

        addDebugInfo("Current device IP: " + ipString);

        // Extract network prefix (first three octets)
        String[] ipParts = ipString.split("\\.");
        if (ipParts.length == 4) {
            String networkPrefix = ipParts[0] + "." + ipParts[1] + "." + ipParts[2] + ".";

            addDebugInfo("Scanning network: " + networkPrefix + "0/24");

            // Common printer IPs to check (usually .100 to .120)
            int[] commonHosts = {100, 101, 102, 103, 104, 105, 106, 107, 108, 109, 110,
                    111, 112, 113, 114, 115, 116, 117, 118, 119, 120,
                    150, 200, 250, 1, 2, 3, 4, 5, 10, 20, 30, 40, 50};

            for (int host : commonHosts) {
                String testIp = networkPrefix + host;
                if (isPrinterReachable(testIp, DEFAULT_PRINT_PORT)) {
                    String printerName = getPrinterNameFromIp(testIp);
                    printers.add(new WiFiPrinterInfo(testIp, printerName, DEFAULT_PRINT_PORT));
                    addDebugInfo("✅ Found printer at: " + testIp + " - " + printerName);
                }
            }
        }

        // Add known printer manufacturers
        addKnownPrinters(printers);

        if (printers.isEmpty()) {
            addDebugInfo("❌ No WiFi printers found");
        } else {
            addDebugInfo("Found " + printers.size() + " WiFi printer(s)");
        }

        if (callback != null) {
            callback.onPrintersFound(printers);
        }

        return printers;
    }

    /**
     * Check if a printer is reachable at the given IP and port
     */
    private boolean isPrinterReachable(String ipAddress, int port) {
        try {
            InetAddress inetAddress = InetAddress.getByName(ipAddress);
            boolean reachable = inetAddress.isReachable(CONNECTION_TIMEOUT);

            if (reachable) {
                // Try to connect to the printer port
                try (Socket testSocket = new Socket()) {
                    testSocket.connect(new InetSocketAddress(ipAddress, port), CONNECTION_TIMEOUT);
                    return true;
                } catch (IOException e) {
                    // Port not open, but device is reachable
                    return false;
                }
            }
        } catch (UnknownHostException e) {
            Log.e(TAG, "Unknown host: " + ipAddress);
        } catch (IOException e) {
            Log.e(TAG, "Error checking reachability: " + e.getMessage());
        }
        return false;
    }

    /**
     * Get printer name from IP (try to resolve hostname)
     */
    private String getPrinterNameFromIp(String ipAddress) {
        try {
            InetAddress inetAddress = InetAddress.getByName(ipAddress);
            String hostName = inetAddress.getCanonicalHostName();
            if (hostName != null && !hostName.equals(ipAddress)) {
                return hostName;
            }
        } catch (UnknownHostException e) {
            // Ignore
        }
        return "WiFi Printer (" + ipAddress + ")";
    }

    /**
     * Add known printer manufacturers to the list
     */
    private void addKnownPrinters(List<WiFiPrinterInfo> printers) {
        // Common printer manufacturer OUI prefixes
        String[][] knownPrinters = {
                {"192.168.1.100", "HP LaserJet", "HP"},
                {"192.168.1.101", "Canon MF", "Canon"},
                {"192.168.1.102", "Epson WorkForce", "Epson"},
                {"192.168.1.103", "Brother HL", "Brother"},
                {"192.168.1.104", "Samsung ML", "Samsung"},
                {"192.168.1.105", "Xerox", "Xerox"},
                {"192.168.1.106", "Kyocera", "Kyocera"},
                {"192.168.1.107", "Ricoh", "Ricoh"},
                {"192.168.1.108", "Dell", "Dell"},
                {"192.168.1.109", "Lexmark", "Lexmark"}
        };

        for (String[] printer : knownPrinters) {
            String ip = printer[0];
            String name = printer[1];
            String manufacturer = printer[2];

            // Check if printer already in list
            boolean exists = false;
            for (WiFiPrinterInfo p : printers) {
                if (p.ipAddress.equals(ip)) {
                    exists = true;
                    break;
                }
            }

            if (!exists && isPrinterReachable(ip, DEFAULT_PRINT_PORT)) {
                printers.add(new WiFiPrinterInfo(ip, name, manufacturer, DEFAULT_PRINT_PORT));
            }
        }
    }

    /**
     * Connect to a WiFi printer
     * @param ipAddress Printer IP address
     * @param port Printer port (usually 9100 for raw printing)
     * @return true if connected successfully
     */
    public boolean connectToPrinter(String ipAddress, int port) {
        disconnectPrinter();

        addDebugInfo("Connecting to WiFi printer at " + ipAddress + ":" + port);

        try {
            printerSocket = new Socket();
            printerSocket.connect(new InetSocketAddress(ipAddress, port), SOCKET_TIMEOUT);
            printerSocket.setSoTimeout(SOCKET_TIMEOUT);

            outputStream = printerSocket.getOutputStream();
            printWriter = new PrintWriter(outputStream, true);

            connectedPrinterIp = ipAddress;
            connectedPrinterName = getPrinterNameFromIp(ipAddress);
            isConnected = true;

            addDebugInfo("✅ Connected to WiFi printer: " + connectedPrinterName);

            if (callback != null) {
                callback.onPrinterConnected(connectedPrinterName, ipAddress);
            }

            return true;

        } catch (SocketTimeoutException e) {
            addDebugInfo("❌ Connection timeout: " + e.getMessage());
        } catch (IOException e) {
            addDebugInfo("❌ Connection failed: " + e.getMessage());
        }

        return false;
    }

    /**
     * Connect to printer with default port
     */
    public boolean connectToPrinter(String ipAddress) {
        return connectToPrinter(ipAddress, DEFAULT_PRINT_PORT);
    }

    /**
     * Disconnect from printer
     */
    public void disconnectPrinter() {
        try {
            if (printWriter != null) {
                printWriter.close();
                printWriter = null;
            }
            if (outputStream != null) {
                outputStream.close();
                outputStream = null;
            }
            if (printerSocket != null && !printerSocket.isClosed()) {
                printerSocket.close();
                printerSocket = null;
            }
        } catch (IOException e) {
            Log.e(TAG, "Error disconnecting: " + e.getMessage());
        }

        isConnected = false;
        connectedPrinterIp = null;
        connectedPrinterName = null;

        addDebugInfo("Disconnected from WiFi printer");

        if (callback != null) {
            callback.onPrinterDisconnected();
        }
    }

    /**
     * Print text to WiFi printer
     */
    public boolean printText(String text) {
        if (!isConnected || printerSocket == null || outputStream == null) {
            addDebugInfo("❌ Not connected to WiFi printer");
            return false;
        }

        try {
            addDebugInfo("Sending " + text.length() + " characters to WiFi printer");

            String printData = text + "\n\n\f"; // Add form feed at the end
            byte[] data = printData.getBytes("UTF-8");

            outputStream.write(data);
            outputStream.flush();

            addDebugInfo("✅ Print data sent successfully");

            if (callback != null) {
                callback.onPrintSuccess();
            }

            return true;

        } catch (IOException e) {
            addDebugInfo("❌ Print failed: " + e.getMessage());

            if (callback != null) {
                callback.onPrintError(e.getMessage());
            }

            return false;
        }
    }

    /**
     * Print formatted ticket
     */
    public boolean printTicket(String title, String content) {
        String ticket = "\n\n" +
                "================================\n" +
                "     " + title + "\n" +
                "================================\n" +
                content + "\n" +
                "================================\n" +
                "Date: " + new java.text.SimpleDateFormat("dd/MM/yyyy HH:mm:ss",
                java.util.Locale.getDefault()).format(new java.util.Date()) + "\n" +
                "================================\n\n\n";

        return printText(ticket);
    }

    /**
     * Check if printer is connected
     */
    public boolean isPrinterConnected() {
        if (!isConnected || printerSocket == null || !printerSocket.isConnected()) {
            return false;
        }

        // Check if socket is still alive
        try {
            printerSocket.sendUrgentData(0xFF); // Send urgent data to test connection
            return true;
        } catch (IOException e) {
            isConnected = false;
            return false;
        }
    }

    /**
     * Get connected printer name
     */
    public String getConnectedPrinterName() {
        return connectedPrinterName != null ? connectedPrinterName : "No Printer";
    }

    /**
     * Get printer IP address
     */
    public String getPrinterIpAddress() {
        return connectedPrinterIp != null ? connectedPrinterIp : "";
    }

    /**
     * Add debug info
     */
    private void addDebugInfo(String info) {
        Log.d(TAG, info);
        if (callback != null) {
            callback.onDebugInfo(info);
        }
    }
}