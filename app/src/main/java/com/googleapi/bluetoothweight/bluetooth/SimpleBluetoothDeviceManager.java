package com.googleapi.bluetoothweight.bluetooth;



import android.content.Context;
import android.content.SharedPreferences;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class SimpleBluetoothDeviceManager {

    private static final String PREF_NAME = "bluetooth_devices";
    private static final String KEY_DEVICE_LIST = "device_addresses";
    private static final String PREFIX_NAME = "device_name_";
    private static final String PREFIX_INFO = "device_info_";
    private static final String PREFIX_AUTO_CONNECT = "auto_connect_";
    private static final String PREFIX_LAST_CONNECTED = "last_connected_";
    private static final String KEY_LAST_CONNECTED_ADDRESS = "last_connected_address";

    private SharedPreferences preferences;

    public SimpleBluetoothDeviceManager(Context context) {
        preferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    // Save a device
    public void saveDevice(BluetoothDeviceModel device) {
        String address = device.getAddress();
        if (address == null || address.isEmpty()) {
            return;
        }

        // Add address to the list
        Set<String> deviceAddresses = getDeviceAddressSet();
        deviceAddresses.add(address);
        preferences.edit().putStringSet(KEY_DEVICE_LIST, deviceAddresses).apply();

        // Save device details
        SharedPreferences.Editor editor = preferences.edit();
        editor.putString(PREFIX_NAME + address, device.getName());
        editor.putString(PREFIX_INFO + address, device.getInfo() != null ? device.getInfo() : "");
        editor.putBoolean(PREFIX_AUTO_CONNECT + address, device.isAutoConnect());
        editor.putLong(PREFIX_LAST_CONNECTED + address, System.currentTimeMillis());
        editor.apply();

        // Save as last connected
        saveLastConnectedDevice(address);
    }

    // Get all saved devices
    public List<BluetoothDeviceModel> getSavedDevices() {
        List<BluetoothDeviceModel> devices = new ArrayList<>();
        Set<String> deviceAddresses = getDeviceAddressSet();

        for (String address : deviceAddresses) {
            BluetoothDeviceModel device = getDeviceByAddress(address);
            if (device != null) {
                devices.add(device);
            }
        }

        return devices;
    }

    // Get device by address
    public BluetoothDeviceModel getDeviceByAddress(String address) {
        if (address == null || address.isEmpty() || !preferences.contains(PREFIX_NAME + address)) {
            return null;
        }

        BluetoothDeviceModel device = new BluetoothDeviceModel();
        device.setAddress(address);
        device.setName(preferences.getString(PREFIX_NAME + address, "Unknown Device"));
        device.setInfo(preferences.getString(PREFIX_INFO + address, ""));
        device.setAutoConnect(preferences.getBoolean(PREFIX_AUTO_CONNECT + address, true));
        device.setLastConnectedTime(preferences.getLong(PREFIX_LAST_CONNECTED + address, 0));

        return device;
    }

    // Remove a device
    public void removeDevice(String address) {
        if (address == null || address.isEmpty()) {
            return;
        }

        // Remove from address list
        Set<String> deviceAddresses = getDeviceAddressSet();
        deviceAddresses.remove(address);
        preferences.edit().putStringSet(KEY_DEVICE_LIST, deviceAddresses).apply();

        // Remove device details
        SharedPreferences.Editor editor = preferences.edit();
        editor.remove(PREFIX_NAME + address);
        editor.remove(PREFIX_INFO + address);
        editor.remove(PREFIX_AUTO_CONNECT + address);
        editor.remove(PREFIX_LAST_CONNECTED + address);
        editor.apply();
    }

    // Get auto-connect devices
    public List<BluetoothDeviceModel> getAutoConnectDevices() {
        List<BluetoothDeviceModel> allDevices = getSavedDevices();
        List<BluetoothDeviceModel> autoConnectDevices = new ArrayList<>();

        for (BluetoothDeviceModel device : allDevices) {
            if (device.isAutoConnect()) {
                autoConnectDevices.add(device);
            }
        }

        return autoConnectDevices;
    }

    // Save last connected device
    public void saveLastConnectedDevice(String address) {
        if (address != null && !address.isEmpty()) {
            preferences.edit().putString(KEY_LAST_CONNECTED_ADDRESS, address).apply();
        }
    }

    // Get last connected device address
    public String getLastConnectedDeviceAddress() {
        return preferences.getString(KEY_LAST_CONNECTED_ADDRESS, "");
    }

    // Get last connected device
    public BluetoothDeviceModel getLastConnectedDevice() {
        String address = getLastConnectedDeviceAddress();
        if (!address.isEmpty()) {
            return getDeviceByAddress(address);
        }
        return null;
    }

    // Update auto-connect status
    public void updateAutoConnect(String address, boolean autoConnect) {
        if (address != null && !address.isEmpty() && preferences.contains(PREFIX_NAME + address)) {
            preferences.edit().putBoolean(PREFIX_AUTO_CONNECT + address, autoConnect).apply();
        }
    }

    // Check if device exists
    public boolean deviceExists(String address) {
        return address != null && !address.isEmpty() && preferences.contains(PREFIX_NAME + address);
    }

    // Get all device addresses
    private Set<String> getDeviceAddressSet() {
        Set<String> addresses = preferences.getStringSet(KEY_DEVICE_LIST, null);
        return addresses != null ? new HashSet<>(addresses) : new HashSet<String>();
    }

    // Clear all devices
    public void clearAllDevices() {
        Set<String> addresses = getDeviceAddressSet();
        SharedPreferences.Editor editor = preferences.edit();

        for (String address : addresses) {
            editor.remove(PREFIX_NAME + address);
            editor.remove(PREFIX_INFO + address);
            editor.remove(PREFIX_AUTO_CONNECT + address);
            editor.remove(PREFIX_LAST_CONNECTED + address);
        }

        editor.remove(KEY_DEVICE_LIST);
        editor.remove(KEY_LAST_CONNECTED_ADDRESS);
        editor.apply();
    }

    // Sort devices by last connected time (most recent first)
    public List<BluetoothDeviceModel> getSortedDevicesByLastConnected() {
        List<BluetoothDeviceModel> devices = getSavedDevices();
        devices.sort((d1, d2) -> Long.compare(d2.getLastConnectedTime(), d1.getLastConnectedTime()));
        return devices;
    }
}