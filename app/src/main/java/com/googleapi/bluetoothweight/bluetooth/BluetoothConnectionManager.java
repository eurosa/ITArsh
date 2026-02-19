package com.googleapi.bluetoothweight.bluetooth;

import static com.googleapi.bluetoothweight.MainActivity.PERMISSION_REQUEST_CODE;

import android.Manifest;
import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

public class BluetoothConnectionManager {

    // Constants
    private static final String TAG = "BluetoothConnection";
    private static final int PACKET_SIZE = 22;
    private static final long KEEP_ALIVE_INTERVAL_MS = 2000;
    private static final int MAX_RECONNECT_ATTEMPTS = 3;
    private static final int RECONNECT_BASE_DELAY_MS = 5000;
    private static final long MIN_RECONNECT_INTERVAL = 10000; // 10 seconds
    private static final long CONNECTION_MONITOR_INTERVAL = 3000; // 3 seconds
    private static final long READ_TIMEOUT_MS = 5000; // 5 seconds

    // Connection status constants
    public static final int CONNECTION_SUCCESS = 0;
    public static final int CONNECTION_FAILED = 1;
    public static final int CONNECTION_LOST = 6;

    // UUIDs for SPP (Serial Port Profile)
    private static final UUID[] SPP_UUIDS = {
            UUID.fromString("00001101-0000-1000-8000-00805F9B34FB"), // Standard SPP
            UUID.fromString("00001105-0000-1000-8000-00805F9B34FB"), // OBEX
            UUID.fromString("00001124-0000-1000-8000-00805F9B34FB")  // HID
    };

    // Application context and handlers
    private final Context context;
    private final Handler mainHandler;
    private ExecutorService executorService;
    private volatile boolean isShutdown = false;
    private volatile boolean isMonitoring = false;

    // Bluetooth components
    private BluetoothSocket btSocket;
    private InputStream inputStream;
    private OutputStream outputStream;
    private final AtomicBoolean isConnected = new AtomicBoolean(false);
    private final AtomicBoolean isConnecting = new AtomicBoolean(false);

    // Device information
    private String currentDeviceAddress;
    private String currentDeviceInfo;
    private int reconnectAttempts = 0;
    private long lastReconnectTime = 0;
    private long lastDataReceivedTime = 0;

    // Callback interface
    private ConnectionCallback connectionCallback;

    // Data buffers
    private final byte[] rxBuffer = new byte[PACKET_SIZE];

    // Add these near your other state variables
    private int serialtimeout = 0;
    private String cutting;

    public interface ConnectionCallback {
        void onConnectionResult(int resultCode, String message);
        void onDataReceived(byte[] data);
        void onConnectionLost();
        void onSensorDataUpdated(String cutting);
        void onSettingsUpdated(int tempSet, int humidSet, int pressureSet);
    }

    public BluetoothConnectionManager(Context context) {
        this.context = context.getApplicationContext();
        this.mainHandler = new Handler(Looper.getMainLooper());
        this.executorService = Executors.newFixedThreadPool(3);

        // Register Bluetooth state receiver
        registerBluetoothReceiver();
    }

    private void registerBluetoothReceiver() {
        IntentFilter filter = new IntentFilter();
        filter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
        filter.addAction(BluetoothDevice.ACTION_ACL_CONNECTED);
        filter.addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED);

        try {
            context.registerReceiver(bluetoothStateReceiver, filter);
            Log.d(TAG, "Bluetooth state receiver registered");
        } catch (Exception e) {
            Log.e(TAG, "Failed to register Bluetooth receiver", e);
        }
    }

    private final BroadcastReceiver bluetoothStateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            if (BluetoothAdapter.ACTION_STATE_CHANGED.equals(action)) {
                int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR);
                if (state == BluetoothAdapter.STATE_OFF) {
                    Log.d(TAG, "Bluetooth turned OFF");
                    handleConnectionLost("Bluetooth was turned off");
                }
            }

            if (BluetoothDevice.ACTION_ACL_DISCONNECTED.equals(action)) {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                if (device != null && currentDeviceAddress != null &&
                        device.getAddress().equals(currentDeviceAddress)) {
                    Log.d(TAG, "Device disconnected via system broadcast");
                    handleConnectionLost("Device disconnected");
                }
            }
        }
    };

    // Connection Management
    public void connect(String deviceAddress, String deviceInfo, ConnectionCallback callback) {
        // Prevent multiple simultaneous connection attempts
        if (isConnecting.get()) {
            Log.d(TAG, "Already connecting, skipping duplicate attempt");
            return;
        }

        this.connectionCallback = callback;
        this.currentDeviceAddress = deviceAddress;
        this.currentDeviceInfo = deviceInfo;
        this.reconnectAttempts = 0;
        this.lastReconnectTime = System.currentTimeMillis();

        isConnecting.set(true);

        executorService.execute(() -> {
            try {
                // Clean up any existing connection
                disconnectInternal();

                // Check permissions
                if (!checkBluetoothPermissions()) {
                    notifyConnectionResult(CONNECTION_FAILED, "Bluetooth permission required");
                    isConnecting.set(false);
                    return;
                }

                BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
                if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled()) {
                    notifyConnectionResult(CONNECTION_FAILED, "Bluetooth is disabled");
                    isConnecting.set(false);
                    return;
                }

                try {
                    BluetoothDevice device = bluetoothAdapter.getRemoteDevice(deviceAddress);
                    establishConnection(device);
                } catch (IllegalArgumentException e) {
                    notifyConnectionResult(CONNECTION_FAILED, "Invalid device address");
                    isConnecting.set(false);
                }

            } catch (Exception e) {
                Log.e(TAG, "Connection error", e);
                notifyConnectionResult(CONNECTION_FAILED, "Connection failed: " + e.getMessage());
                isConnecting.set(false);
            }
        });
    }

    @SuppressLint("MissingPermission")
    private void establishConnection(BluetoothDevice device) throws IOException {
        BluetoothSocket socket = null;
        IOException lastException = null;

        // Try all known UUIDs
        for (UUID uuid : SPP_UUIDS) {
            try {
                socket = createSocket(device, uuid);
                socket.connect();

                // Connection successful
                btSocket = socket;
                inputStream = btSocket.getInputStream();
                outputStream = btSocket.getOutputStream();

                if (inputStream == null || outputStream == null) {
                    throw new IOException("Failed to establish streams");
                }

                isConnected.set(true);
                isConnecting.set(false);
                lastDataReceivedTime = System.currentTimeMillis();

                // Start monitoring threads
                startListeningThread();
                startConnectionMonitoring();
                startKeepAlive();

                Log.d(TAG, "Connection established successfully");
                notifyConnectionResult(CONNECTION_SUCCESS,
                        "Connected to " + getDeviceDisplayName(device));
                return;

            } catch (IOException e) {
                if (socket != null) {
                    try { socket.close(); } catch (IOException ce) { /* ignore */ }
                }
                lastException = e;
                Log.d(TAG, "Connection attempt with UUID " + uuid + " failed: " + e.getMessage());
            }
        }

        // Try fallback method
        try {
            Log.d(TAG, "Trying fallback connection method...");
            Method m = device.getClass().getMethod("createInsecureRfcommSocket", int.class);
            BluetoothSocket fallbackSocket = (BluetoothSocket) m.invoke(device, 1);
            fallbackSocket.connect();

            btSocket = fallbackSocket;
            inputStream = btSocket.getInputStream();
            outputStream = btSocket.getOutputStream();
            isConnected.set(true);
            isConnecting.set(false);
            lastDataReceivedTime = System.currentTimeMillis();

            startListeningThread();
            startConnectionMonitoring();
            startKeepAlive();

            Log.d(TAG, "Connection established via fallback method");
            notifyConnectionResult(CONNECTION_SUCCESS,
                    "Connected to " + getDeviceDisplayName(device));
            return;

        } catch (Exception e) {
            Log.w(TAG, "Fallback connection method failed", e);
        }

        if (lastException != null) {
            throw lastException;
        } else {
            throw new IOException("Failed to establish Bluetooth connection");
        }
    }

    private void handleConnectionLost(String reason) {
        if (isConnected.compareAndSet(true, false)) {
            Log.d(TAG, "Connection lost: " + reason);

            // Clean up resources
            cleanupResources();

            // Notify about connection loss
            notifyConnectionLost(reason);

            // Start reconnection if auto-reconnect is desired
            startAutoReconnect();
        }
    }

    private void startConnectionMonitoring() {
        if (isMonitoring) return;

        isMonitoring = true;

        executorService.execute(() -> {
            while (isMonitoring && !isShutdown && isConnected.get()) {
                try {
                    Thread.sleep(CONNECTION_MONITOR_INTERVAL);

                    if (!isConnected.get()) break;

                    // Check if we've received data recently
                    long timeSinceLastData = System.currentTimeMillis() - lastDataReceivedTime;

                    if (timeSinceLastData > READ_TIMEOUT_MS) {
                        Log.d(TAG, "No data received for " + timeSinceLastData + "ms");

                        // Try to validate connection
                        if (!validateConnection()) {
                            handleConnectionLost("Connection timeout - no data received");
                        }
                    }

                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
            isMonitoring = false;
        });
    }

    private boolean validateConnection() {
        if (btSocket == null || !btSocket.isConnected() ||
                inputStream == null || outputStream == null) {
            return false;
        }

        try {
            // Try to check if stream is still alive
            if (inputStream.available() >= 0) {
                return true;
            }
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    private void startListeningThread() {
        final int BUFFER_SIZE = 4096;

        executorService.execute(() -> {
            byte[] buffer = new byte[BUFFER_SIZE];

            while (isConnected.get() && !isShutdown) {
                try {
                    if (inputStream == null) break;

                    int available = inputStream.available();
                    if (available > 0) {
                        int bytesRead = inputStream.read(buffer, 0, Math.min(available, BUFFER_SIZE));
                        if (bytesRead > 0) {
                            lastDataReceivedTime = System.currentTimeMillis();
                            processIncomingData(buffer, bytesRead);
                        }
                    }

                    Thread.sleep(50);

                } catch (IOException e) {
                    Log.e(TAG, "Read error", e);
                    if (isConnected.get()) {
                        handleConnectionLost("Read error: " + e.getMessage());
                    }
                    break;
                } catch (Exception e) {
                    Log.e(TAG, "Unexpected error in read thread", e);
                    if (isConnected.get()) {
                        handleConnectionLost("Unexpected error");
                    }
                    break;
                }
            }

            Log.d(TAG, "Listening thread stopped");
        });
    }

    private void startKeepAlive() {
        mainHandler.removeCallbacks(keepAliveRunnable);
        mainHandler.postDelayed(keepAliveRunnable, KEEP_ALIVE_INTERVAL_MS);
    }

    private final Runnable keepAliveRunnable = new Runnable() {
        @Override
        public void run() {
            if (!isConnected.get() || outputStream == null || isShutdown) {
                return;
            }

            try {
                // Send a simple keep-alive packet
                byte[] keepAlive = new byte[]{0x00};
                synchronized (BluetoothConnectionManager.this) {
                    outputStream.write(keepAlive);
                    outputStream.flush();
                }
                Log.d(TAG, "Keep-alive sent");

                // Schedule next keep-alive
                mainHandler.postDelayed(this, KEEP_ALIVE_INTERVAL_MS);

            } catch (IOException e) {
                Log.e(TAG, "Keep-alive failed", e);
                handleConnectionLost("Keep-alive failed");
            }
        }
    };

    private void startAutoReconnect() {
        if (reconnectAttempts < MAX_RECONNECT_ATTEMPTS && currentDeviceAddress != null) {
            reconnectAttempts++;
            long delay = RECONNECT_BASE_DELAY_MS * reconnectAttempts;

            Log.d(TAG, "Scheduling reconnect attempt " + reconnectAttempts + " in " + delay + "ms");

            mainHandler.postDelayed(() -> {
                if (!isConnected.get() && !isConnecting.get() &&
                        currentDeviceAddress != null && connectionCallback != null) {
                    Log.d(TAG, "Attempting auto-reconnect #" + reconnectAttempts);
                    connect(currentDeviceAddress, currentDeviceInfo, connectionCallback);
                }
            }, delay);
        } else {
            Log.d(TAG, "Max reconnect attempts reached or no device to reconnect to");
            reconnectAttempts = 0;
        }
    }

    public void disconnect() {
        Log.d(TAG, "Manual disconnect requested");
        isConnecting.set(false);
        stopAutoReconnect();
        disconnectInternal();
    }

    private void disconnectInternal() {
        isConnected.set(false);
        cleanupResources();
        mainHandler.removeCallbacks(keepAliveRunnable);
    }

    private void cleanupResources() {
        synchronized (this) {
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (IOException e) {
                    Log.e(TAG, "Error closing input stream", e);
                }
                inputStream = null;
            }

            if (outputStream != null) {
                try {
                    outputStream.close();
                } catch (IOException e) {
                    Log.e(TAG, "Error closing output stream", e);
                }
                outputStream = null;
            }

            if (btSocket != null) {
                try {
                    if (btSocket.isConnected()) {
                        btSocket.close();
                    }
                } catch (IOException e) {
                    Log.e(TAG, "Error closing socket", e);
                }
                btSocket = null;
            }
        }
    }

    private void stopAutoReconnect() {
        mainHandler.removeCallbacksAndMessages(null);
    }

    // Callback notification methods
    private void notifyConnectionResult(int resultCode, String message) {
        mainHandler.post(() -> {
            if (connectionCallback != null) {
                connectionCallback.onConnectionResult(resultCode, message);
            }
        });

        // Also send broadcast for UI updates
        sendBluetoothStatusBroadcast(resultCode, message);
    }

    private void notifyConnectionLost(String reason) {
        mainHandler.post(() -> {
            if (connectionCallback != null) {
                connectionCallback.onConnectionLost();
            }
        });

        sendBluetoothStatusBroadcast(CONNECTION_LOST, reason);
    }

    private void sendBluetoothStatusBroadcast(int statusCode, String message) {
        try {
            Intent intent = new Intent("BLUETOOTH_CONNECTION_STATUS");
            intent.putExtra("status", statusCode);
            intent.putExtra("message", message);
            if (currentDeviceAddress != null) {
                intent.putExtra("device", currentDeviceAddress);
            }
            intent.setPackage(context.getPackageName());
            context.sendBroadcast(intent);
            Log.d(TAG, "Broadcast sent: " + statusCode + " - " + message);
        } catch (Exception e) {
            Log.e(TAG, "Error sending broadcast", e);
        }
    }

    private void notifySensorDataUpdated(String cutting) {
        mainHandler.post(() -> {
            if (connectionCallback != null) {
                connectionCallback.onSensorDataUpdated(cutting);
            }
        });
    }

    private void notifySettingsUpdated(int tempSet, int humidSet, int pressureSet) {
        mainHandler.post(() -> {
            if (connectionCallback != null) {
                connectionCallback.onSettingsUpdated(tempSet, humidSet, pressureSet);
            }
        });
    }

    // Data processing methods
    private void processIncomingData(byte[] buffer, int length) {
        if (length >= 14) {
            System.arraycopy(buffer, 0, rxBuffer, 0, Math.min(14, length));
            processSerialPort1();
        }

        if (connectionCallback != null) {
            connectionCallback.onDataReceived(Arrays.copyOf(buffer, length));
        }
    }

    private void processSerialPort1() {
        if (rxBuffer[0] == 2) {
            serialtimeout = 0;

            StringBuilder cuttingBuilder = new StringBuilder();
            for (int i = 1; i <= 8; i++) {
                cuttingBuilder.append(decimalToAscii(rxBuffer[i]));
            }
            cutting = cuttingBuilder.toString();

            notifySensorDataUpdated(cutting);
        }

        if (rxBuffer[0] == 0x02 && rxBuffer[1] == '1' && rxBuffer[2] == 'W' && rxBuffer[11] == 0x20) {
            serialtimeout = 0;

            int tempValue = ((rxBuffer[6] & 0xFF) << 8) | (rxBuffer[5] & 0xFF);
            int SET_TEMP = tempValue / 10;

            tempValue = ((rxBuffer[8] & 0xFF) << 8) | (rxBuffer[7] & 0xFF);
            int SET_HUMD = tempValue / 10;

            tempValue = ((rxBuffer[10] & 0xFF) << 8) | (rxBuffer[9] & 0xFF);
            int SET_AIRP = tempValue;

            notifySettingsUpdated(SET_TEMP, SET_HUMD, SET_AIRP);
        }
    }

    public static char decimalToAscii(int decimal) {
        if (decimal < 0 || decimal > 127) {
            throw new IllegalArgumentException("Invalid ASCII decimal value");
        }
        return (char) decimal;
    }

    // Helper methods
    private BluetoothSocket createSocket(BluetoothDevice device, UUID uuid) throws IOException {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT)
                    != PackageManager.PERMISSION_GRANTED) {
                throw new SecurityException("Bluetooth permission required");
            }
            return device.createInsecureRfcommSocketToServiceRecord(uuid);
        } else {
            return device.createRfcommSocketToServiceRecord(uuid);
        }
    }

    private String getDeviceDisplayName(BluetoothDevice device) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT)
                        == PackageManager.PERMISSION_GRANTED) {
                    return device.getName() != null ? device.getName() : "Unknown Device";
                }
            }
            return device.getName() != null ? device.getName() : "Unknown Device";
        } catch (SecurityException e) {
            return "Unknown Device";
        }
    }

    public boolean isConnected() {
        return isConnected.get() && validateConnection();
    }

    public void release() {
        Log.d(TAG, "Releasing resources");
        isShutdown = true;
        isMonitoring = false;
        stopAutoReconnect();
        disconnectInternal();

        try {
            context.unregisterReceiver(bluetoothStateReceiver);
        } catch (IllegalArgumentException e) {
            // Receiver was not registered
        }

        if (executorService != null && !executorService.isShutdown()) {
            executorService.shutdownNow();
        }
    }

    public void forceCloseConnection() {
        disconnectInternal();
    }

    public static boolean checkBluetoothPermissions(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            return ActivityCompat.checkSelfPermission(context,
                    Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED;
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            return ActivityCompat.checkSelfPermission(context,
                    Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
        }
        return true;
    }

    private boolean checkBluetoothPermissions() {
        return checkBluetoothPermissions(context);
    }

    public static void requestBluetoothPermissions(@NonNull android.app.Activity context, int requestCode) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            ActivityCompat.requestPermissions(context,
                    new String[]{Manifest.permission.BLUETOOTH_CONNECT},
                    PERMISSION_REQUEST_CODE);
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            ActivityCompat.requestPermissions(context,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    PERMISSION_REQUEST_CODE);
        }
    }
}