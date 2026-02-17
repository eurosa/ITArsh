package com.googleapi.bluetoothweight.bluetooth;


import android.Manifest;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.util.Log;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class BluetoothDeviceChecker {

    private static final String TAG = "BluetoothDeviceChecker";
    private Context context;
    private BluetoothAdapter bluetoothAdapter;

    public BluetoothDeviceChecker(Context context) {
        this.context = context;
        this.bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
    }

    /**
     * Check if a Bluetooth device is available (paired) - SAFE VERSION
     */
    public boolean isDeviceAvailable(String address) {
        if (address == null || address.isEmpty()) {
            Log.d(TAG, "Device address is null or empty");
            return false;
        }

        if (bluetoothAdapter == null) {
            Log.d(TAG, "Bluetooth adapter is null");
            return false;
        }

        if (!bluetoothAdapter.isEnabled()) {
            Log.d(TAG, "Bluetooth is not enabled");
            return false;
        }

        if (!hasBluetoothPermissions()) {
            Log.d(TAG, "Missing Bluetooth permissions");
            return false;
        }

        try {
            // For Android 12+, we need BLUETOOTH_CONNECT permission to get bonded devices
            Set<BluetoothDevice> pairedDevices = null;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (canConnectToDevices()) {
                    pairedDevices = bluetoothAdapter.getBondedDevices();
                } else {
                    Log.w(TAG, "Cannot get bonded devices: Missing BLUETOOTH_CONNECT permission");
                    return false;
                }
            } else {
                pairedDevices = bluetoothAdapter.getBondedDevices();
            }

            if (pairedDevices == null) {
                Log.d(TAG, "Paired devices set is null");
                return false;
            }

            for (BluetoothDevice device : pairedDevices) {
                if (device != null) {
                    String deviceAddress = getDeviceAddressSafe(device);
                    if (deviceAddress != null && deviceAddress.equalsIgnoreCase(address)) {
                        Log.d(TAG, "Device found: " + address);
                        return true;
                    }
                }
            }

            Log.d(TAG, "Device not found in paired devices: " + address);
            return false;

        } catch (SecurityException e) {
            Log.e(TAG, "Security exception checking device availability: " + e.getMessage());
            return false;
        } catch (Exception e) {
            Log.e(TAG, "Error checking device availability: " + e.getMessage());
            return false;
        }
    }

    /**
     * Safely get device address with permission check
     */
    private String getDeviceAddressSafe(BluetoothDevice device) {
        if (device == null) return null;

        try {
            return device.getAddress();
        } catch (SecurityException e) {
            Log.e(TAG, "Security exception getting device address: " + e.getMessage());
            return null;
        }
    }

    /**
     * Safely get device name with permission check
     */
    private String getDeviceNameSafe(BluetoothDevice device) {
        if (device == null) return null;

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (!canConnectToDevices()) {
                    return "Device (permission needed)";
                }
            }
            return device.getName();
        } catch (SecurityException e) {
            Log.e(TAG, "Security exception getting device name: " + e.getMessage());
            return "Device (restricted)";
        }
    }

    /**
     * Get device name if available - SAFE VERSION
     */
    public String getDeviceNameIfAvailable(String address) {
        if (!isDeviceAvailable(address)) {
            return null;
        }

        try {
            Set<BluetoothDevice> pairedDevices = null;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (canConnectToDevices()) {
                    pairedDevices = bluetoothAdapter.getBondedDevices();
                } else {
                    return "Device (permission needed)";
                }
            } else {
                pairedDevices = bluetoothAdapter.getBondedDevices();
            }

            if (pairedDevices == null) return null;

            for (BluetoothDevice device : pairedDevices) {
                String deviceAddress = getDeviceAddressSafe(device);
                if (deviceAddress != null && deviceAddress.equalsIgnoreCase(address)) {
                    return getDeviceNameSafe(device);
                }
            }
        } catch (SecurityException e) {
            Log.e(TAG, "Security exception getting device name: " + e.getMessage());
        }

        return null;
    }

    /**
     * Get all paired devices - SAFE VERSION
     */
    public List<BluetoothDevice> getAllPairedDevices() {
        List<BluetoothDevice> devices = new ArrayList<>();

        if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled()) {
            Log.w(TAG, "Bluetooth not available or not enabled");
            return devices;
        }

        if (!hasBluetoothPermissions()) {
            Log.w(TAG, "Missing Bluetooth permissions");
            return devices;
        }

        try {
            Set<BluetoothDevice> pairedDevices = null;

            // Check permissions for Android 12+
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (!canConnectToDevices()) {
                    Log.w(TAG, "Missing BLUETOOTH_CONNECT permission for Android 12+");
                    return devices;
                }
                pairedDevices = bluetoothAdapter.getBondedDevices();
            } else {
                pairedDevices = bluetoothAdapter.getBondedDevices();
            }

            if (pairedDevices != null) {
                devices.addAll(pairedDevices);
                Log.d(TAG, "Found " + devices.size() + " paired devices");
            }
        } catch (SecurityException e) {
            Log.e(TAG, "Security exception getting paired devices: " + e.getMessage());
        } catch (Exception e) {
            Log.e(TAG, "Error getting paired devices: " + e.getMessage());
        }

        return devices;
    }

    /**
     * Get all paired device addresses - SAFE VERSION
     */
    public List<String> getAllPairedDeviceAddresses() {
        List<String> addresses = new ArrayList<>();

        if (!hasBluetoothPermissions()) {
            Log.w(TAG, "Cannot get device addresses: Missing Bluetooth permissions");
            return addresses;
        }

        List<BluetoothDevice> devices = getAllPairedDevices();

        try {
            for (BluetoothDevice device : devices) {
                String address = getDeviceAddressSafe(device);
                if (address != null && !address.isEmpty()) {
                    addresses.add(address);
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error getting device addresses: " + e.getMessage());
        }

        return addresses;
    }

    /**
     * Get all paired device names - SAFE VERSION
     */
    public List<String> getAllPairedDeviceNames() {
        List<String> names = new ArrayList<>();

        if (!hasBluetoothPermissions()) {
            Log.w(TAG, "Cannot get device names: Missing Bluetooth permissions");
            return names;
        }

        List<BluetoothDevice> devices = getAllPairedDevices();

        try {
            for (BluetoothDevice device : devices) {
                String name = getDeviceNameSafe(device);
                names.add(name != null ? name : "Unknown Device");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error getting device names: " + e.getMessage());
        }

        return names;
    }

    /**
     * Get paired devices with names and addresses - SAFE VERSION
     */
    public List<DeviceInfo> getAllPairedDeviceInfo() {
        List<DeviceInfo> deviceInfos = new ArrayList<>();

        if (!hasBluetoothPermissions()) {
            Log.w(TAG, "Cannot get device info: Missing Bluetooth permissions");
            return deviceInfos;
        }

        List<BluetoothDevice> devices = getAllPairedDevices();

        try {
            for (BluetoothDevice device : devices) {
                String address = getDeviceAddressSafe(device);
                String name = getDeviceNameSafe(device);

                if (address != null && !address.isEmpty()) {
                    deviceInfos.add(new DeviceInfo(
                            name != null ? name : "Unknown Device",
                            address,
                            device
                    ));
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error getting device info: " + e.getMessage());
        }

        return deviceInfos;
    }

    /**
     * Device info container class
     */
    public static class DeviceInfo {
        private String name;
        private String address;
        private BluetoothDevice device;

        public DeviceInfo(String name, String address, BluetoothDevice device) {
            this.name = name;
            this.address = address;
            this.device = device;
        }

        public String getName() { return name; }
        public String getAddress() { return address; }
        public BluetoothDevice getDevice() { return device; }

        @Override
        public String toString() {
            return name + " (" + address + ")";
        }
    }

    /**
     * Check if Bluetooth is supported and enabled
     */
    public boolean isBluetoothEnabled() {
        return bluetoothAdapter != null && bluetoothAdapter.isEnabled();
    }

    /**
     * Check required permissions - ENHANCED VERSION
     */
    public boolean hasBluetoothPermissions() {
        // First check if we have basic permissions
        boolean hasBasicPermissions = true;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            // Android 12+ requires BLUETOOTH_CONNECT and BLUETOOTH_SCAN
            hasBasicPermissions = ContextCompat.checkSelfPermission(context,
                    Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED &&
                    ContextCompat.checkSelfPermission(context,
                            Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED;
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            // Android 6-11 requires location permission
            hasBasicPermissions = ContextCompat.checkSelfPermission(context,
                    Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
                    ContextCompat.checkSelfPermission(context,
                            Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED;
        }

        // For all versions, check legacy permissions if needed
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
            hasBasicPermissions = hasBasicPermissions &&
                    ContextCompat.checkSelfPermission(context,
                            Manifest.permission.BLUETOOTH) == PackageManager.PERMISSION_GRANTED &&
                    ContextCompat.checkSelfPermission(context,
                            Manifest.permission.BLUETOOTH_ADMIN) == PackageManager.PERMISSION_GRANTED;
        }

        return hasBasicPermissions;
    }

    /**
     * Check if we have permission to scan for devices
     */
    public boolean canScanForDevices() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            return ContextCompat.checkSelfPermission(context,
                    Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED;
        } else {
            return hasBluetoothPermissions();
        }
    }

    /**
     * Check if we have permission to connect to devices
     */
    public boolean canConnectToDevices() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            return ContextCompat.checkSelfPermission(context,
                    Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED;
        } else {
            return hasBluetoothPermissions();
        }
    }

    /**
     * Request necessary permissions
     */
    public void requestPermissions(Activity activity, int requestCode) {
        List<String> permissionsNeeded = new ArrayList<>();

        // For Android 12+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (!canConnectToDevices()) {
                permissionsNeeded.add(Manifest.permission.BLUETOOTH_CONNECT);
            }
            if (!canScanForDevices()) {
                permissionsNeeded.add(Manifest.permission.BLUETOOTH_SCAN);
            }
        }

        // For Android 6-11, location permissions
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M &&
                Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
            if (ContextCompat.checkSelfPermission(context,
                    Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                permissionsNeeded.add(Manifest.permission.ACCESS_FINE_LOCATION);
            }
        }

        // Legacy permissions for pre-Android 12
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
            if (ContextCompat.checkSelfPermission(context,
                    Manifest.permission.BLUETOOTH) != PackageManager.PERMISSION_GRANTED) {
                permissionsNeeded.add(Manifest.permission.BLUETOOTH);
            }
            if (ContextCompat.checkSelfPermission(context,
                    Manifest.permission.BLUETOOTH_ADMIN) != PackageManager.PERMISSION_GRANTED) {
                permissionsNeeded.add(Manifest.permission.BLUETOOTH_ADMIN);
            }
        }

        if (!permissionsNeeded.isEmpty()) {
            ActivityCompat.requestPermissions(activity,
                    permissionsNeeded.toArray(new String[0]),
                    requestCode);
            Log.d(TAG, "Requesting permissions: " + permissionsNeeded);
        }
    }

    /**
     * Check Bluetooth state
     */
    public String getBluetoothState() {
        if (bluetoothAdapter == null) {
            return "NOT_SUPPORTED";
        }

        try {
            int state = bluetoothAdapter.getState();
            switch (state) {
                case BluetoothAdapter.STATE_OFF: return "OFF";
                case BluetoothAdapter.STATE_TURNING_ON: return "TURNING_ON";
                case BluetoothAdapter.STATE_ON: return "ON";
                case BluetoothAdapter.STATE_TURNING_OFF: return "TURNING_OFF";
                default: return "UNKNOWN";
            }
        } catch (SecurityException e) {
            Log.e(TAG, "Security exception getting Bluetooth state: " + e.getMessage());
            return "PERMISSION_DENIED";
        }
    }
}