package com.googleapi.bluetoothweight.bluetooth;

import android.Manifest;
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

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class BluetoothConnectionManager2 {

    // Data buffers

    // Global variables (previously parameters)
    public int deviceId;
    public int tempSet;
    public int humidSet;
    public int diffPressureSet;
    public int intensity1;
    public int intensity2;
    public int intensity3;
    public int intensity4;
    public byte buzzerHexGas1;
    public byte buzzerHexGas2;
    public byte buzzerHexGas3;
    public byte buzzerHexGas4;
    public byte buzzerHexGas5;
    public byte buzzerHexGas6;
    public byte buzzerHexGas7;
    public byte buzzerHexGas8;
    public int autoManualStatus;
    private byte[] Rxbuf = new byte[22];
    private byte[] Txbuf = new byte[22];
    private byte[] Txbuf_1;
    private byte[] DigitalMASK = new byte[] { 0x01, 0x02, 0x04, 0x08, 0x10, 0x20, 0x40, (byte)0x80 };
    private byte[] DigitalOUT = new byte[] { 0x00, 0x00 };
    private byte[] DigitalIN = new byte[] { 0x00, 0x00 };
    private byte[] AnalogTMPR = new byte[] { 0x00, 0x00 };
    private byte[] AnalogHUMD = new byte[] { 0x00, 0x00 };
    private byte[] AnalogAIRP = new byte[] { 0x00, 0x00 };

    // Variables from original code
    private int serialtimeout;
    private int ac, pac, swflag, cdflag, isochkflag, sec;
    private int READ_TEMP, READ_HUMD, READ_PRES;
    private int DISP_TEMP, DISP_HUMD, DISP_PRES;
    private int SET_TEMP, SET_HUMD, SET_AIRP, STM, SHM, STP;
    private int  intensity5;
    private int Light1, Light2, Light3, Light4, Light5, ACRUN;
    private short temp;
    private byte buzzer_hex;

    private static final String TAG = "BluetoothConnection";
    private static final UUID[] SPP_UUIDS = {
            UUID.fromString("00001101-0000-1000-8000-00805F9B34FB"), // Standard SPP
            UUID.fromString("00001105-0000-1000-8000-00805F9B34FB"), // OBEX
            UUID.fromString("00001124-0000-1000-8000-00805F9B34FB"),  // HID


            UUID.fromString("00001108-0000-1000-8000-00805F9B34FB"), // Headset (HSP)
            UUID.fromString("00001112-0000-1000-8000-00805F9B34FB"), // Generic (HFP)
            UUID.fromString("0000111E-0000-1000-8000-00805F9B34FB")  // Hands-Free (HFP)
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

    private final Context context;
    private final Handler mainHandler;
    private final ExecutorService executorService;
    private BluetoothSocket btSocket;
    private InputStream mmInputStream;
    private OutputStream mmOutputStream;
    private volatile boolean isBtConnected = false;
    private String currentDeviceAddress;
    private String currentDeviceInfo;
    private ConnectionCallback connectionCallback;
    private final BroadcastReceiver bluetoothStateReceiver;
    private int reconnectAttempts = 0;
    private static final int MAX_RECONNECT_ATTEMPTS = 5;

    public interface ConnectionCallback {
        void onConnectionResult(int resultCode, String message);
        void onDataReceived(byte[] data);
        void onConnectionLost();
    }


    public BluetoothConnectionManager2(Context context) {
        this.context = context.getApplicationContext();
        this.mainHandler = new Handler(Looper.getMainLooper());
        this.executorService = Executors.newSingleThreadExecutor();

        this.deviceId = 0;
        this.tempSet = 0;
        this.humidSet = 0;
        this.diffPressureSet = 0;
        this.intensity1 = 0;
        this.intensity2 = 0;
        this.intensity3 = 0;
        this.intensity4 = 0;
        this.buzzerHexGas1 = 0;
        this.buzzerHexGas2 = 0;
        this.buzzerHexGas3 = 0;
        this.buzzerHexGas4 = 0;
        this.buzzerHexGas5 = 0;
        this.buzzerHexGas6 = 0;
        this.buzzerHexGas7 = 0;
        this.buzzerHexGas8 = 0;
        this.autoManualStatus = 0;

        this.bluetoothStateReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                if (BluetoothAdapter.ACTION_STATE_CHANGED.equals(action)) {
                    int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR);
                    if (state == BluetoothAdapter.STATE_OFF) {
                        disconnect();
                        if (connectionCallback != null) {
                            connectionCallback.onConnectionResult(BLUETOOTH_DISABLED, "Bluetooth was turned off");
                        }
                    }
                }
            }
        };

        IntentFilter filter = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
        context.registerReceiver(bluetoothStateReceiver, filter);
    }

    public void connect(String deviceAddress, String deviceInfo, ConnectionCallback callback) {
        this.connectionCallback = callback;
        this.currentDeviceAddress = deviceAddress;
        this.currentDeviceInfo = deviceInfo;

        executorService.execute(() -> {
            mainHandler.post(() -> {
                if (callback != null) {
                    callback.onConnectionResult(-1, "Connecting...");
                }
            });

            // Check if already connected to this device
            if (validateConnection() && deviceAddress.equals(btSocket.getRemoteDevice().getAddress())) {
                notifyResult(CONNECTION_SUCCESS, "Already connected to this device", callback);
                return;
            }

            // Clean up any existing connection
         ///   disconnect();

            // Check permissions
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (ContextCompat.checkSelfPermission(context,
                        Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                    notifyResult(PERMISSION_DENIED, "Bluetooth permission required", callback);
                    return;
                }
            }

            BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
            if (bluetoothAdapter == null) {
                notifyResult(CONNECTION_FAILED, "Bluetooth not supported", callback);
                return;
            }

            if (!bluetoothAdapter.isEnabled()) {
                notifyResult(BLUETOOTH_DISABLED, "Bluetooth is disabled", callback);
                return;
            }

            try {
                BluetoothDevice device = bluetoothAdapter.getRemoteDevice(deviceAddress);
                establishConnection(device, callback);
            } catch (IllegalArgumentException e) {
                notifyResult(CONNECTION_FAILED, "Invalid device address", callback);
            } catch (SecurityException e) {
                notifyResult(SECURITY_EXCEPTION, "Bluetooth permission denied", callback);
            } catch (Exception e) {
                notifyResult(CONNECTION_FAILED, "Connection failed: " + e.getMessage(), callback);
            }
        });
    }


    // This replaces the BytesToSend method from original code
    public void sendControllerData() {
// Set global variables
       /* deviceId = 123;
        tempSet = 25;
        humidSet = 50;
        diffPressureSet = 100;
        intensity1 = 255;
        intensity2 = 255;
        intensity3 = 255;
        intensity4 = 255;
        buzzerHexGas1 = 0x01;
        buzzerHexGas2 = 0x02;
        buzzerHexGas3 = 0x02;
        buzzerHexGas4 = 0x02;
        buzzerHexGas5 = 0x02;
        buzzerHexGas6 = 0x02;
        buzzerHexGas7 = 0x02;
        buzzerHexGas8 = 0x02;
// ... set other variables as needed ...
        autoManualStatus = 0;*/
        if (!isBtConnected) {
            return;
        }
        Log.e(TAG, "Keep-alive ok");
        // Prepare device ID message
        Txbuf_1 = ("$ID" + deviceId + ";").getBytes();

        // Prepare main controller data
        Txbuf[0] = 0x02;
        Txbuf[1] = (byte)'1';
        Txbuf[2] = (byte)'C';
        Txbuf[3] = DigitalOUT[0];
        Txbuf[4] = DigitalOUT[1];
        Txbuf[5] = (byte)(intensity1 >> 8);
        Txbuf[6] = (byte)intensity1;
        Txbuf[7] = (byte)(intensity2 >> 8);
        Txbuf[8] = (byte)intensity2;
        Txbuf[9] = (byte)(intensity3 >> 8);
        Txbuf[10] = (byte)intensity3;
        Txbuf[11] = (byte)(tempSet >> 8);
        Txbuf[12] = (byte)(tempSet);
        Txbuf[13] = (byte)(humidSet >> 8);
        Txbuf[14] = (byte)(humidSet);
        Txbuf[15] = (byte)(intensity4 >> 8);
        Txbuf[16] = (byte)intensity4;

        SET_TEMP = tempSet;
        SET_HUMD = humidSet;
        SET_AIRP = diffPressureSet;

        if(autoManualStatus == 0) {
            STM = READ_TEMP - (SET_TEMP * 10);
            if (STM < 0) { STM = 0; }
            if (STM > 50) { STM = 50; }
            STM = STM * 20;
            Txbuf[17] = (byte)(STM >> 8);
            Txbuf[18] = (byte)(STM);

            SHM = READ_HUMD - (SET_HUMD * 10);
            if (SHM < 0) { SHM = 0; }
            if (SHM > 100) { SHM = 100; }
            SHM = SHM * 10;
            Txbuf[19] = (byte)(SHM >> 8);
            Txbuf[20] = (byte)(SHM);
        } else {
            STM = SET_TEMP * 10;
            Txbuf[17] = (byte)(STM >> 8);
            Txbuf[18] = (byte)(STM);

            SHM = SET_HUMD * 10;
            Txbuf[19] = (byte)(SHM >> 8);
            Txbuf[20] = (byte)(SHM);
        }

        Txbuf[21] = 0x20;

        buzzer_hex = (byte)(buzzerHexGas1 | buzzerHexGas2 | buzzerHexGas3 | buzzerHexGas4 |
                buzzerHexGas5 | buzzerHexGas6 | buzzerHexGas7 | buzzerHexGas8);

        try {
            if (isBtConnected) {
                // Send the appropriate message based on device status
                if (true) { // Replace with your device status check
                    write(Txbuf);
                } else {
                    write(Txbuf_1);
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error sending data", e);
            disconnect();
            startAutoReconnect();
        }
    }

    public void write(byte[] bytes) throws IOException {
        if (isBtConnected && mmOutputStream  != null) {
            Log.e(TAG, "Error sending data");
            mmOutputStream.write(bytes);
        }
    }

    private boolean validateConnection() {
        if (btSocket == null || !btSocket.isConnected() || mmInputStream == null || mmOutputStream == null) {
            return false;
        }

        try {
            // Simple write to validate connection
            mmOutputStream.write(0x00);
            mmOutputStream.flush();
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    private void establishConnection(BluetoothDevice device, ConnectionCallback callback) throws IOException {
        BluetoothSocket socket = null;
        IOException lastException = null;

        // Try all known UUIDs for SPP devices
        for (UUID uuid : SPP_UUIDS) {
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    if (ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT)
                            != PackageManager.PERMISSION_GRANTED) {
                        notifyResult(PERMISSION_DENIED, "Bluetooth permission required", callback);
                        return;
                    }
                    socket = device.createInsecureRfcommSocketToServiceRecord(uuid);
                } else {
                    socket = device.createRfcommSocketToServiceRecord(uuid);
                }

                // Cancel discovery to speed up connection
                try {
                    BluetoothAdapter.getDefaultAdapter().cancelDiscovery();
                } catch (SecurityException e) {
                    Log.w(TAG, "Couldn't cancel discovery", e);
                }

                // Connect with timeout
                socket.connect();

                // Connection successful
                btSocket = socket;
                mmInputStream = btSocket.getInputStream();
                mmOutputStream = btSocket.getOutputStream();

                // Verify streams
                if (mmInputStream == null || mmOutputStream == null) {
                    throw new IOException("Failed to establish streams");
                }

                isBtConnected = true;
                reconnectAttempts = 0; // Reset on successful connection
                startKeepAlive();
                //startListeningThread();

                String successMessage = "Connected to " +
                        (currentDeviceInfo != null ? currentDeviceInfo.replace(currentDeviceAddress, "") : device.getName());
                notifyResult(CONNECTION_SUCCESS, successMessage, callback);
                return;

            } catch (IOException e) {
                // Close failed socket
                if (socket != null) {
                    try { socket.close(); } catch (IOException ce) { /* ignore */ }
                }
                lastException = e;
                // Try next UUID
            }
        }

        // Fallback method if standard connection fails
        // Fallback method if standard connection fails
        try {
            Method m = device.getClass().getMethod("createInsecureRfcommSocket", int.class);
            BluetoothSocket fallbackSocket = (BluetoothSocket) m.invoke(device, 1);
            fallbackSocket.connect();

            btSocket = fallbackSocket;
            mmInputStream = btSocket.getInputStream();
            mmOutputStream = btSocket.getOutputStream();
            isBtConnected = true;
            reconnectAttempts = 0; // Reset on successful connection

            startKeepAlive();
            //startListeningThread();

            // Safe device name handling
            String deviceName = "unknown device";
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    if (ContextCompat.checkSelfPermission(context,
                            Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED) {
                        deviceName = device.getName() != null ? device.getName() : deviceName;
                    }
                } else {
                    deviceName = device.getName() != null ? device.getName() : deviceName;
                }
            } catch (SecurityException e) {
                Log.w(TAG, "Couldn't get device name", e);
            }

            notifyResult(CONNECTION_SUCCESS,
                    "Connected (fallback method) to " + deviceName,
                    callback);
            return;
        } catch (Exception e) {
            Log.w(TAG, "Fallback connection method failed", e);
        }

        // All attempts failed
        if (lastException != null) {
            throw lastException;
        } else {
            throw new IOException("Failed to establish Bluetooth connection");
        }
    }

    private void startListeningThread() {
        executorService.execute(() -> {
            byte[] buffer = new byte[1024];
            int bytes;
            Log.d(TAG,"startListeningThread");
            while (isBtConnected) {
                try {
                    bytes = mmInputStream.read(buffer);
                    if (bytes > 0) {
                        byte[] data = new byte[bytes];
                        System.arraycopy(buffer, 0, data, 0, bytes);
                        if (connectionCallback != null) {
                            mainHandler.post(() -> connectionCallback.onDataReceived(data));
                        }
                    }
                } catch (IOException e) {
                    Log.e(TAG, "Connection lost", e);
                    if (isBtConnected) {
                        disconnect();
                        if (connectionCallback != null) {
                            mainHandler.post(() -> {
                                connectionCallback.onConnectionLost();
                                connectionCallback.onConnectionResult(CONNECTION_LOST, "Connection lost");
                            });
                        }
                        startAutoReconnect();
                    }
                    break;
                }
            }
        });
    }

    private void startAutoReconnect() {
        if (reconnectAttempts < MAX_RECONNECT_ATTEMPTS) {
            reconnectAttempts++;
            long delay = calculateReconnectDelay(reconnectAttempts);
            Log.d(TAG, "Scheduling reconnect attempt " + reconnectAttempts + " in " + delay + "ms");
            mainHandler.postDelayed(reconnectRunnable, delay);
        } else {
            Log.d(TAG, "Max reconnect attempts reached");
            reconnectAttempts = 0;
        }
    }

    private long calculateReconnectDelay(int attempt) {
        // Exponential backoff: 5s, 10s, 20s, 40s, 80s
        return 5000 * (long)Math.pow(2, attempt-1);
    }

    private final Runnable reconnectRunnable = new Runnable() {
        @Override
        public void run() {
            if (!isBtConnected && currentDeviceAddress != null && connectionCallback != null) {
                Log.d(TAG, "Attempting reconnect...");
                connect(currentDeviceAddress, currentDeviceInfo, connectionCallback);
            }
        }
    };

    private void startKeepAlive() {
        mainHandler.post(keepAliveRunnable);
    }
    private final Runnable keepAliveRunnable = new Runnable() {
        @Override
        public void run() {
            if (!isBtConnected || mmOutputStream == null) {
                return;
            }

            try {
                // Send controller data instead of simple keep-alive
                sendControllerData();
                startListeningThread();
            } catch (Exception e) {
                Log.e(TAG, "Keep-alive/send data failed", e);
                disconnect();
                if (connectionCallback != null) {
                    mainHandler.post(() -> {
                        connectionCallback.onConnectionLost();
                        connectionCallback.onConnectionResult(CONNECTION_LOST, "Keep-alive failed");
                    });
                }
                startAutoReconnect();
                return;
            }
            // Schedule next keep-alive
            mainHandler.postDelayed(this, 3000);
        }
    };
   /* private final Runnable keepAliveRunnable = new Runnable() {
        @Override
        public void run() {
            if (!isBtConnected || mmOutputStream == null) {
                return;
            }

            try {
                // Use a more standard keep-alive message
                Log.e("Sending Data: ", "Keep-alive failed");
                mmOutputStream.write(new byte[]{0x01}); // SOH character
                mmOutputStream.flush();
            } catch (IOException e) {
                Log.e(TAG, "Keep-alive failed", e);
                disconnect();
                if (connectionCallback != null) {
                    mainHandler.post(() -> {
                        connectionCallback.onConnectionLost();
                        connectionCallback.onConnectionResult(CONNECTION_LOST, "Keep-alive failed");
                    });
                }
                startAutoReconnect();
                return;
            }
            // Schedule next keep-alive
            mainHandler.postDelayed(this, 3000);
        }
    };*/

    public void disconnect() {
        executorService.execute(() -> {
            isBtConnected = false;

            // Remove all pending callbacks first
            mainHandler.removeCallbacks(keepAliveRunnable);


            Log.d("asdfgh","sd");
            // Close streams and socket
            if (mmInputStream != null) {
                try { mmInputStream.close(); } catch (IOException e) { Log.e(TAG, "Error closing input stream", e); }
                mmInputStream = null;
            }

            if (mmOutputStream != null) {
                try { mmOutputStream.close(); } catch (IOException e) { Log.e(TAG, "Error closing output stream", e); }
                mmOutputStream = null;
            }

            if (btSocket != null) {
                try { btSocket.close(); } catch (IOException e) { Log.e(TAG, "Error closing socket", e); }
                btSocket = null;
            }

            // Notify UI if this was an explicit disconnection
            if (connectionCallback != null) {
                mainHandler.post(() -> {
                    connectionCallback.onConnectionLost();
                    notifyResult(CONNECTION_LOST, "Connection Loss", connectionCallback);
                    connectionCallback.onConnectionResult(CONNECTION_LOST, "Disconnected from device");
                });
            }
        });
    }

    public void sendData(String data) {
        if (!isBtConnected || mmOutputStream == null) {
            Log.w(TAG, "Cannot send data - not connected");
            return;
        }

        executorService.execute(() -> {
            try {
                mmOutputStream.write(data.getBytes());
                mmOutputStream.flush();
            } catch (IOException e) {
                Log.e(TAG, "Error sending data", e);
                disconnect();
                if (connectionCallback != null) {
                    mainHandler.post(() -> {
                        connectionCallback.onConnectionLost();
                        connectionCallback.onConnectionResult(CONNECTION_LOST, "Error sending data");
                    });
                }
                startAutoReconnect();
            }
        });
    }

    public void sendData(byte[] data) {
        if (!isBtConnected || mmOutputStream == null) {
            Log.w(TAG, "Cannot send data - not connected");
            return;
        }

        executorService.execute(() -> {
            try {
                mmOutputStream.write(data);
                mmOutputStream.flush();
            } catch (IOException e) {
                Log.e(TAG, "Error sending data", e);
                disconnect();
                if (connectionCallback != null) {
                    mainHandler.post(() -> {
                        connectionCallback.onConnectionLost();
                        connectionCallback.onConnectionResult(CONNECTION_LOST, "Error sending data");
                    });
                }
                startAutoReconnect();
            }
        });
    }

    public boolean isConnected() {
        return isBtConnected && validateConnection();
    }

    public void shutdown() {
        disconnect();
        try {
            context.unregisterReceiver(bluetoothStateReceiver);
        } catch (IllegalArgumentException e) {
            // Receiver was not registered
        }
        executorService.shutdown();
    }

    private void notifyResult(int resultCode, String message, ConnectionCallback callback) {
        mainHandler.post(() -> {
            if (callback != null) {
                callback.onConnectionResult(resultCode, message);
            }
        });
    }

    public static boolean checkBluetoothPermissions(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            return ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT)
                    == PackageManager.PERMISSION_GRANTED;
        }
        return true;
    }

    public static void requestBluetoothPermissions(android.app.Activity activity, int requestCode) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            ActivityCompat.requestPermissions(activity,
                    new String[]{Manifest.permission.BLUETOOTH_CONNECT},
                    requestCode);
        }
    }
}