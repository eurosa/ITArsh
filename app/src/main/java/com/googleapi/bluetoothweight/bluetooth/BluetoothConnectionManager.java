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
    private static final long KEEP_ALIVE_INTERVAL_MS = 1000;
    private static final int MAX_RECONNECT_ATTEMPTS = 5;
    private static final int RECONNECT_BASE_DELAY_MS = 5000;
    private static final long MIN_RECONNECT_INTERVAL = 10000; // 10 seconds
    private static final long CONNECTION_MONITOR_INTERVAL = 2000; // 2 seconds

    public int sec;

    public static byte[] AnalogTMPR = new byte[]{0x00, 0x00};
    public static byte[] AnalogHUMD = new byte[]{0x00, 0x00};
    public static byte[] AnalogAIRP = new byte[]{0x00, 0x00};
    public static byte[] DigitalMASK = new byte[]{0x01, 0x02, 0x04, 0x08, 0x10, 0x20, 0x40, (byte) 0x80};

    public static byte[] DigitalIN = new byte[]{0x00, 0x00};

    // UUIDs for SPP (Serial Port Profile)
    private static final UUID[] SPP_UUIDS = {
            UUID.fromString("00001101-0000-1000-8000-00805F9B34FB"), // Standard SPP
            UUID.fromString("00001105-0000-1000-8000-00805F9B34FB"), // OBEX
            UUID.fromString("00001124-0000-1000-8000-00805F9B34FB")  // HID
    };

    // Connection status constants
    public static final int CONNECTION_SUCCESS = 0;
    public static final int CONNECTION_FAILED = 1;
    public static final int PERMISSION_DENIED = 2;
    public static final int SECURITY_EXCEPTION = 3;
    public static final int IO_EXCEPTION = 4;
    public static final int BLUETOOTH_DISABLED = 5;
    public static final int CONNECTION_LOST = 6;
    public static final int CONNECTION_TIMEOUT = 7;

    // Application context and handlers
    private final Context context;
    private final Handler mainHandler;
    private ExecutorService executorService;
    private volatile boolean isShutdown = false;
    private volatile boolean isMonitoring = false;
    public int Muteflag;
    public int speakerStatus;

    // Bluetooth components
    private BluetoothSocket btSocket;
    private InputStream inputStream;
    private OutputStream outputStream;
    private final AtomicBoolean isConnected = new AtomicBoolean(false);

    // Device information
    private String currentDeviceAddress;
    private String currentDeviceInfo;
    private int reconnectAttempts = 0;
    private long lastReconnectTime = 0;

    // Callback interface
    private ConnectionCallback connectionCallback;

    // Data buffers
    private final byte[] rxBuffer = new byte[PACKET_SIZE];
    private final byte[] txBuffer = new byte[PACKET_SIZE];
    public boolean toggle;

    // Controller state
    public int deviceId;
    public int ac;
    public int pac;
    public int ac2;
    public int tempSet;
    public int humidSet;
    public int diffPressureSet;
    public byte[] DigitalOUT = new byte[]{0x00, 0x00};
    public int intensity1, intensity2, intensity3, intensity4;
    private int SET_TEMP, SET_HUMD, SET_AIRP, STM, SHM, STP;
    private int READ_TEMP, READ_HUMD, READ_PRES;

    // Add these near your other state variables
    private int serialtimeout = 0;
    private String actual, cutting, DISP_PRES;


    public byte buzzerHexGas1, buzzerHexGas2, buzzerHexGas3, buzzerHexGas4;
    public byte buzzerHexGas5, buzzerHexGas6, buzzerHexGas7, buzzerHexGas8;
    public int autoManualStatus;

    // Sensor readings
    private int readTemp, readHumd, readPres;
    private int dispTemp, dispHumd, dispPres;

    // Bluetooth state receiver
    private final BroadcastReceiver bluetoothStateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            if (BluetoothAdapter.ACTION_STATE_CHANGED.equals(action)) {
                int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR);
                if (state == BluetoothAdapter.STATE_OFF) {
                    Log.d(TAG, "Bluetooth turned OFF");
                    disconnect();
                    notifyConnectionResult(BLUETOOTH_DISABLED, "Bluetooth was turned off");
                } else if (state == BluetoothAdapter.STATE_ON) {
                    Log.d(TAG, "Bluetooth turned ON - attempting to reconnect");
                    // Wait a moment for Bluetooth to fully initialize, then try to reconnect
                    mainHandler.postDelayed(() -> {
                        if (!isConnected.get() && currentDeviceAddress != null) {
                            reconnectToLastDevice();
                        }
                    }, 2000);
                }
            }

            // Handle device connection/disconnection events
            if (BluetoothDevice.ACTION_ACL_CONNECTED.equals(action)) {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                if (device != null && currentDeviceAddress != null &&
                        device.getAddress().equals(currentDeviceAddress)) {
                   // Log.d(TAG, "Our device connected via system: " + device.getName());
                    // Device reconnected at system level, establish our connection
                    if (!isConnected.get()) {
                        mainHandler.postDelayed(() -> {
                            reconnectToLastDevice();
                        }, 1000);
                    }
                }
            }

            if (BluetoothDevice.ACTION_ACL_DISCONNECTED.equals(action)) {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                if (device != null && currentDeviceAddress != null &&
                        device.getAddress().equals(currentDeviceAddress)) {
                    Log.d(TAG, "Our device disconnected via system "+CONNECTION_FAILED);
                    Toast.makeText(context,"Bluetooth device disconnected",Toast.LENGTH_LONG).show();
                   // notifyConnectionResult(CONNECTION_FAILED, "Bluetooth was turned off "+CONNECTION_FAILED);
                    // Set flag to prevent immediate reconnection
                    lastReconnectTime = System.currentTimeMillis();

                    // Update state
                    isConnected.set(false);

                    // Clean up resources
                    cleanupResources();

                    // IMPORTANT: Send broadcast to update UI
                    sendBluetoothStatusBroadcast(CONNECTION_FAILED, "Device disconnected");
                }
            }
        }
    };

    private void sendBluetoothStatusBroadcast(int statusCode, String message) {
        try {
            Intent intent = new Intent("BLUETOOTH_CONNECTION_STATUS");
            intent.putExtra("status", statusCode);
            intent.putExtra("message", message);
            if (currentDeviceAddress != null) {
                intent.putExtra("device", currentDeviceAddress);
            }

            // For Android 8.0+ we need to set package name
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                intent.setPackage(context.getPackageName());
            }

            context.sendBroadcast(intent);
            Log.d(TAG, "Broadcast sent successfully: " + statusCode);

        } catch (Exception e) {
            Log.e(TAG, "Error sending broadcast: " + e.getMessage(), e);

            // Fallback: Try without package name for older Android
            try {
                Intent fallbackIntent = new Intent("BLUETOOTH_CONNECTION_STATUS");
                fallbackIntent.putExtra("status", statusCode);
                fallbackIntent.putExtra("message", message);
                context.sendBroadcast(fallbackIntent);
                Log.d(TAG, "Fallback broadcast sent");
            } catch (Exception ex) {
                Log.e(TAG, "Fallback also failed", ex);
            }
        }
    }

    public interface ConnectionCallback {
        void onConnectionResult(int resultCode, String message);
        void onDataReceived(byte[] data);
        void onConnectionLost();
        void onSensorDataUpdated(String actual, String cutting);
        void onSettingsUpdated(int tempSet, int humidSet, int pressureSet);
    }

    public BluetoothConnectionManager(Context context) {
        this.context = context.getApplicationContext();
        this.mainHandler = new Handler(Looper.getMainLooper());
        initializeExecutorService();
        this.executorService = Executors.newFixedThreadPool(3); // Separate threads for RX, TX, and monitoring

        // Initialize DigitalOUT array
        this.DigitalOUT = new byte[]{0x00, 0x00};
        this.DigitalIN = new byte[]{0x01, 0x02, 0x04, 0x08, 0x10, 0x20, 0x40, (byte) 0x80};
        this.DigitalMASK = new byte[]{0x01, 0x02, 0x04, 0x08, 0x10, 0x20, 0x40, (byte) 0x80};
        this.AnalogTMPR = new byte[]{0x00, 0x00};
        this.AnalogHUMD = new byte[]{0x00, 0x00};
        this.AnalogAIRP = new byte[]{0x00, 0x00};
        this.intensity1 = 0;
        this.intensity2 = 0;
        this.intensity3 = 0;
        this.intensity4 = 0;
        this.tempSet = 0;
        this.humidSet = 0;
        this.ac = 0;
        this.ac2 = 0;
        this.toggle = true;
        this.pac = 0;
        this.Muteflag = 0;
        this.speakerStatus = 0;

        // Register Bluetooth state receiver
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

        // Initialize default values
        resetControllerState();
    }

    private synchronized void initializeExecutorService() {
        if (executorService == null || executorService.isShutdown()) {
            executorService = Executors.newFixedThreadPool(3);
            isShutdown = false;
        }
    }

    private synchronized void ensureExecutorRunning() {
        if (executorService == null || executorService.isShutdown() || executorService.isTerminated()) {
            Log.w(TAG, "Executor was terminated, recreating...");
            executorService = Executors.newFixedThreadPool(3);
            isShutdown = false;
        }
    }

    private void resetControllerState() {
        deviceId = 0;
        tempSet = 0;
        humidSet = 0;
        diffPressureSet = 0;
        intensity1 = intensity2 = intensity3 = intensity4 = 0;
        buzzerHexGas1 = buzzerHexGas2 = buzzerHexGas3 = buzzerHexGas4 = 0;
        buzzerHexGas5 = buzzerHexGas6 = buzzerHexGas7 = buzzerHexGas8 = 0;
        autoManualStatus = 0;
        readTemp = readHumd = readPres = 0;
        dispTemp = dispHumd = dispPres = 0;
    }

    // Connection Management
    public void connect(String deviceAddress, String deviceInfo, ConnectionCallback callback) {
        ensureExecutorRunning();

        this.connectionCallback = callback;
        this.currentDeviceAddress = deviceAddress;
        this.currentDeviceInfo = deviceInfo;
        this.reconnectAttempts = 0;
        this.lastReconnectTime = System.currentTimeMillis();

        executorService.execute(() -> {
            notifyConnectionResult(-1, "Connecting...");

            // Check if already connected to this device
            if (validateConnection() && deviceAddress.equals(btSocket.getRemoteDevice().getAddress())) {
                notifyConnectionResult(CONNECTION_SUCCESS, "Already connected to this device");
                return;
            }

            // Clean up any existing connection
            disconnect();

            // Check permissions
            if (!checkBluetoothPermissions()) {
                notifyConnectionResult(PERMISSION_DENIED, "Bluetooth permission required");
                return;
            }

            BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
            if (bluetoothAdapter == null) {
                notifyConnectionResult(CONNECTION_FAILED, "Bluetooth not supported");
                return;
            }

            if (!bluetoothAdapter.isEnabled()) {
                notifyConnectionResult(BLUETOOTH_DISABLED, "Bluetooth is disabled");
                return;
            }

            try {
                BluetoothDevice device = bluetoothAdapter.getRemoteDevice(deviceAddress);
                establishConnection(context, device, deviceAddress);
                notifyConnectionResult(CONNECTION_SUCCESS, "Connected to " + getDeviceDisplayName(device));

                // Start monitoring after successful connection
                startConnectionMonitoring();

            } catch (IllegalArgumentException e) {
                notifyConnectionResult(CONNECTION_FAILED, "Invalid device address");
            } catch (SecurityException e) {
                notifyConnectionResult(SECURITY_EXCEPTION, "Bluetooth permission denied");
            } catch (IOException e) {
                notifyConnectionResult(CONNECTION_FAILED, "Connection failed: " + e.getMessage());
                 disconnect();
                startAutoReconnect();
            }
        });
    }

    private void establishConnection(Context context, BluetoothDevice device, String deviceAddress) throws IOException {
        BluetoothSocket socket = null;
        IOException lastException = null;

        // Try all known UUIDs for SPP devices
        for (UUID uuid : SPP_UUIDS) {
            try {
                socket = createSocket(device, uuid);

                // Check permissions using passed context
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    if (ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT)
                            != PackageManager.PERMISSION_GRANTED) {
                        throw new IOException("Bluetooth Connect permission not granted");
                    }
                }

                socket.connect();

                // Rest of your connection code...
                btSocket = socket;
                inputStream = btSocket.getInputStream();
                outputStream = btSocket.getOutputStream();

                if (inputStream == null || outputStream == null) {
                    throw new IOException("Failed to establish streams");
                }

                // Send initial configuration command if needed
                String atCommand = "AT+NAME=HELLO_SCP" + "\r\n";
                outputStream.write(atCommand.getBytes());
                outputStream.flush();
                Log.d(TAG, "Sent: AT+NAME=HELLO_SCP");

                isConnected.set(true);
                verifyProtocol();
                startListeningThread();
                startKeepAlive();

                Log.d(TAG, "Connection established successfully");
                return;

            } catch (IOException e) {
                if (socket != null) {
                    try {
                        socket.close();
                    } catch (IOException ce) { /* ignore */ }
                }
                lastException = e;
                Log.d(TAG, "Connection attempt with UUID " + uuid + " failed: " + e.getMessage());
            }
        }

        // Fallback method
        try {
            Log.d(TAG, "Trying fallback connection method...");
            Method m = device.getClass().getMethod("createInsecureRfcommSocket", int.class);
            BluetoothSocket fallbackSocket = (BluetoothSocket) m.invoke(device, 1);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT)
                        != PackageManager.PERMISSION_GRANTED) {
                    throw new IOException("Bluetooth Connect permission not granted");
                }
            }

            fallbackSocket.connect();

            btSocket = fallbackSocket;
            inputStream = btSocket.getInputStream();
            outputStream = btSocket.getOutputStream();
            isConnected.set(true);

            startListeningThread();
            startKeepAlive();
            Log.d(TAG, "Connection established via fallback method");
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

    // Start connection monitoring
    public void startConnectionMonitoring() {
        if (isMonitoring) {
            return;
        }

        isMonitoring = true;
        Log.d(TAG, "Starting connection monitoring");

        ensureExecutorRunning();
        executorService.execute(() -> {
            while (isMonitoring && !isShutdown) {
                try {
                    Thread.sleep(CONNECTION_MONITOR_INTERVAL);

                    // Check if we're supposed to be connected
                    if (isConnected.get()) {
                        // Validate the current connection
                        if (!validateConnection()) {
                            Log.d(TAG, "Connection validation failed");
                            handleConnectionLost();
                        }
                    } else {
                        // We're not connected, check if we should try to reconnect
                        if (currentDeviceAddress != null) {
                            long now = System.currentTimeMillis();
                            long timeSinceLastReconnect = now - lastReconnectTime;

                            if (timeSinceLastReconnect > MIN_RECONNECT_INTERVAL) {
                                Log.d(TAG, "Time to attempt reconnection");
                                lastReconnectTime = now;
                                reconnectToLastDevice();
                            }
                        }
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    Log.d(TAG, "Connection monitoring interrupted");
                    break;
                } catch (Exception e) {
                    Log.e(TAG, "Error in connection monitoring", e);
                }
            }

            Log.d(TAG, "Connection monitoring stopped");
        });
    }

    private void reconnectToLastDevice() {
        if (currentDeviceAddress != null && connectionCallback != null && !isConnected.get()) {
            Log.d(TAG, "Attempting to reconnect to device: " + currentDeviceAddress);

            ensureExecutorRunning();
            executorService.execute(() -> {
                try {
                    // Small delay before reconnection attempt
                    Thread.sleep(1000);

                    if (!isConnected.get() && !isShutdown) {
                        Log.d(TAG, "Initiating reconnection...");
                        connect(currentDeviceAddress, currentDeviceInfo, connectionCallback);
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });
        }
    }

    // Stop connection monitoring
    public void stopConnectionMonitoring() {
        isMonitoring = false;
        Log.d(TAG, "Connection monitoring stopped");
    }

    // Data Transmission
    public void sendControllerData(byte[] txBuffer) {
        if (!isConnected.get()) {
            Log.w(TAG, "Not connected - cannot send data");

            return;
        }

        Log.d("TEMP_DATA", tempSet + " " + humidSet);

        // Initialize buffer properly
       // byte[] txBuffer = new byte[8];
       // Arrays.fill(txBuffer, (byte) 0);

        // Prepare main controller data
        /*txBuffer[0] =  (byte) '$';
        txBuffer[1] = (byte) 'B';
        txBuffer[2] = (byte) 'T';
        txBuffer[3] = (byte) '1';
        txBuffer[4] = (byte) 'S';
        txBuffer[5] = (byte) '+';
        txBuffer[6] = (byte) '1';
        txBuffer[7] = (byte) ';';*/




        try {
            synchronized (this) {
                if (isConnected.get() && outputStream != null) {
                    outputStream.write(txBuffer);
                    outputStream.flush();
                    Log.d(TAG, "Data sent successfully "+Arrays.toString(txBuffer));
                }
            }
        } catch (IOException e) {
            Log.e(TAG, "Error sending data", e);
            handleConnectionLost();
        }
    }

    private void verifyProtocol() throws IOException {
        if (inputStream == null) {
            throw new IOException("Input stream is null");
        }

        if (outputStream == null) {
            throw new IOException("Output stream is null");
        }

        // Send verification packet
        byte[] verificationPacket = new byte[]{0x01, 0x02, 0x03};
        outputStream.write(verificationPacket);
        outputStream.flush();

        // Wait for response
        byte[] response = new byte[3];
        long start = System.currentTimeMillis();
        while (System.currentTimeMillis() - start < 2000) {
            if (inputStream.available() >= 3) {
                inputStream.read(response);
                if (Arrays.equals(response, new byte[]{0x06, 0x06, 0x06})) {
                    Log.d(TAG, "Protocol verified");
                    return;
                }
            }
        }
        throw new IOException("Protocol verification failed");
    }

    public void sendData(byte[] data) {
        if (!isConnected.get() || outputStream == null) {
            Log.w(TAG, "Cannot send data - not connected");
            return;
        }

        executorService.execute(() -> {
            try {
                synchronized (this) {
                    outputStream.write(data);
                    outputStream.flush();
                }
            } catch (IOException e) {
                Log.e(TAG, "Error sending data", e);
                handleConnectionLost();
            }
        });
    }

    private void startListeningThread() {
        final int BUFFER_SIZE = 4096;
        ensureExecutorRunning();
        executorService.execute(() -> {
            byte[] buffer = new byte[BUFFER_SIZE];
            while (isConnected.get() && !isShutdown) {
                try {
                    int available = inputStream.available();
                    if (available > 0) {
                        int bytesRead = inputStream.read(buffer, 0, Math.min(available, BUFFER_SIZE));
                        if (bytesRead > 0) {
                            processIncomingData(buffer, bytesRead);
                        }
                    }
                    Thread.sleep(50);
                } catch (Exception e) {
                    Log.e(TAG, "Read error", e);
                    if (isConnected.get()) {
                        handleConnectionLost();
                    }
                    break;
                }
            }
        });
    }

    private void processIncomingData(byte[] buffer, int length) {
        Log.d(TAG, "Received " + length + " bytes");

        if (length >= 18) {
            System.arraycopy(buffer, 0, rxBuffer, 0, 22);
            processSerialPort1();
        }
    }
    public static char decimalToAscii(int decimal) {

        if (decimal < 0 || decimal > 127) {
            throw new IllegalArgumentException("Invalid ASCII decimal value");
        }

        return (char) decimal;
    }

    @SuppressLint("SuspiciousIndentation")
    private void processSerialPort1() {

        if (rxBuffer[0] == 36) {
            if (rxBuffer[1] == (byte)'B') {
                if (rxBuffer[2] == (byte)'T') {

                        serialtimeout = 0;

                    cutting   = decimalToAscii(rxBuffer[5])+""+decimalToAscii(rxBuffer[6])+""+decimalToAscii(rxBuffer[7])+""+decimalToAscii(rxBuffer[8])+""+decimalToAscii(rxBuffer[9])+""+decimalToAscii(rxBuffer[10]);
                    actual   = decimalToAscii(rxBuffer[12])+""+decimalToAscii(rxBuffer[13])+""+decimalToAscii(rxBuffer[14])+""+decimalToAscii(rxBuffer[15])+""+decimalToAscii(rxBuffer[16])+""+decimalToAscii(rxBuffer[17]);


                        Log.d("Received_Data",Arrays.toString(rxBuffer)+" actual "+actual+" "+cutting);





                        notifySensorDataUpdated(actual, cutting);

                }
            }
        }

        // Second condition block: Rxbuf[2] == 'W' (Settings response)
        if (rxBuffer[0] == 0x02) {
            if (rxBuffer[1] == (byte)'1') {
                if (rxBuffer[2] == (byte)'W') {
                    if (rxBuffer[11] == 0x20) {
                        serialtimeout = 0;

                        int tempValue = ((rxBuffer[6] & 0xFF) << 8) | (rxBuffer[5] & 0xFF);
                        SET_TEMP = tempValue / 10;

                        tempValue = ((rxBuffer[8] & 0xFF) << 8) | (rxBuffer[7] & 0xFF);
                        SET_HUMD = tempValue / 10;

                        tempValue = ((rxBuffer[10] & 0xFF) << 8) | (rxBuffer[9] & 0xFF);
                        SET_AIRP = tempValue;

                        notifySettingsUpdated(SET_TEMP, SET_HUMD, SET_AIRP);
                    }
                }
            }
        }
    }

    // Helper methods to notify callbacks
    private void notifySensorDataUpdated(String actual, String cutting) {
        mainHandler.post(() -> {
            if (connectionCallback != null) {
                connectionCallback.onSensorDataUpdated(actual, cutting);
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

    // Connection Maintenance
    private void startKeepAlive() {
        mainHandler.postDelayed(keepAliveRunnable, KEEP_ALIVE_INTERVAL_MS);
    }

    private final Runnable keepAliveRunnable = new Runnable() {
        @Override
        public void run() {
            if (!isConnected.get() || outputStream == null || isShutdown) {
                return;
            }

            try {
                synchronized (BluetoothConnectionManager.this) {
                   // sendControllerData();
                }
            } catch (Exception e) {
                Log.e(TAG, "Keep-alive failed", e);
                handleConnectionLost();
                return;
            }

            // Schedule next keep-alive
            mainHandler.postDelayed(this, KEEP_ALIVE_INTERVAL_MS);
        }
    };

    private void startAutoReconnect() {
        if (reconnectAttempts < MAX_RECONNECT_ATTEMPTS) {
            reconnectAttempts++;
            long delay = RECONNECT_BASE_DELAY_MS * (long) Math.pow(2, reconnectAttempts - 1);

            Log.d(TAG, "Scheduling reconnect attempt " + reconnectAttempts + " in " + delay + "ms");
            mainHandler.postDelayed(() -> {
                if (!isConnected.get() && currentDeviceAddress != null && connectionCallback != null) {
                    connect(currentDeviceAddress, currentDeviceInfo, connectionCallback);
                }
            }, delay);
        } else {
            Log.d(TAG, "Max reconnect attempts reached");
            reconnectAttempts = 0;
        }
    }

    // Connection State Management
    public void disconnect() {
        if (isShutdown) {
            cleanupResources();
            return;
        }

        ensureExecutorRunning();
        executorService.execute(() -> {
            isConnected.set(false);
            cleanupResources();
            mainHandler.removeCallbacks(keepAliveRunnable);
        });
    }

    private void cleanupResources() {
        isConnected.set(false);
        mainHandler.removeCallbacks(keepAliveRunnable);
        mainHandler.removeCallbacksAndMessages(null);

        synchronized (this) {
            if (inputStream != null) {
                try { inputStream.close(); } catch (IOException e) { Log.e(TAG, "Error closing input stream", e); }
                inputStream = null;
            }

            if (outputStream != null) {
                try { outputStream.close(); } catch (IOException e) { Log.e(TAG, "Error closing output stream", e); }
                outputStream = null;
            }

            if (btSocket != null) {
                try { btSocket.close(); } catch (IOException e) { Log.e(TAG, "Error closing socket", e); }
                btSocket = null;
            }
        }

        notifyConnectionLost();
    }

    private void handleConnectionLost() {
        if (isConnected.compareAndSet(true, false)) {
            Log.d(TAG, "Connection lost detected");
            cleanupResources();
            startAutoReconnect();
        }
    }

    private boolean validateConnection() {
        if (btSocket == null || !btSocket.isConnected() || inputStream == null || outputStream == null) {
            return false;
        }

        try {
            synchronized (this) {
                // Try to write a small test byte
                outputStream.write(0x00);
                outputStream.flush();
                return true;
            }
        } catch (IOException e) {
            return false;
        }
    }

    // Callback Notifications
    private void notifyConnectionResult(int resultCode, String message) {
        Log.d(TAG, "Notifying connection result: " + resultCode + " - " + message);

        // Always send broadcast (most reliable)
        sendBluetoothStatusBroadcast(resultCode, message);

        // Also try callback
        mainHandler.post(() -> {
            if (connectionCallback != null) {
                connectionCallback.onConnectionResult(resultCode, message);
            } else {
                Log.w(TAG, "Callback is null, using broadcast only");
            }
        });
    }

    private void notifyDataReceived(byte[] data) {
        mainHandler.post(() -> {
            if (connectionCallback != null) {
                connectionCallback.onDataReceived(data);
            }
        });
    }

    private void notifyConnectionLost() {
        mainHandler.post(() -> {
            if (connectionCallback != null) {
                connectionCallback.onConnectionLost();
            }
        });
    }

    // Utility Methods
    public boolean isConnected() {
        return isConnected.get() && validateConnection();
    }

    public void shutdown() {
        isShutdown = true;
        isMonitoring = false;
        disconnect();

        try {
            context.unregisterReceiver(bluetoothStateReceiver);
        } catch (IllegalArgumentException e) {
            // Receiver was not registered
        }

        if (executorService != null && !executorService.isShutdown()) {
            executorService.shutdownNow();
        }
    }

    public synchronized void restart() {
        if (isShutdown) {
            disconnect();
            initializeExecutorService();
        }
    }

    public static boolean checkBluetoothPermissions(Context context) {
        // For Android 12+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            return ActivityCompat.checkSelfPermission(context,
                    Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED;
        }

        // For Android 6-10, need location permission for Bluetooth
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            return ActivityCompat.checkSelfPermission(context,
                    Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
        }

        return true; // No special permissions needed for older Android
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

    // Helper method to check if device is available
    public boolean isDeviceAvailable() {
        if (currentDeviceAddress == null) {
            return false;
        }

        BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled()) {
            return false;
        }

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT)
                        != PackageManager.PERMISSION_GRANTED) {
                    return false;
                }
            }

            // Try to get the device
            BluetoothDevice device = bluetoothAdapter.getRemoteDevice(currentDeviceAddress);
            return device != null;

        } catch (Exception e) {
            Log.e(TAG, "Error checking device availability", e);
            return false;
        }
    }
}