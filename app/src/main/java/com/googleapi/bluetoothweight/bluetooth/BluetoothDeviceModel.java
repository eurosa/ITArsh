package com.googleapi.bluetoothweight.bluetooth;

// BluetoothDeviceModel.java


public class BluetoothDeviceModel {
    private String name;
    private String address;
    private String info;
    private boolean autoConnect = true;
    private long lastConnectedTime;

    // Default constructor
    public BluetoothDeviceModel() {
        this.lastConnectedTime = System.currentTimeMillis();
    }

    // Constructor with parameters
    public BluetoothDeviceModel(String name, String address, String info) {
        this.name = name;
        this.address = address;
        this.info = info;
        this.autoConnect = true;
        this.lastConnectedTime = System.currentTimeMillis();
    }

    // Getters and setters
    public String getName() {
        return name != null ? name : "Unknown Device";
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getAddress() {
        return address != null ? address : "";
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getInfo() {
        return info != null ? info : "";
    }

    public void setInfo(String info) {
        this.info = info;
    }

    public boolean isAutoConnect() {
        return autoConnect;
    }

    public void setAutoConnect(boolean autoConnect) {
        this.autoConnect = autoConnect;
    }

    public long getLastConnectedTime() {
        return lastConnectedTime;
    }

    public void setLastConnectedTime(long lastConnectedTime) {
        this.lastConnectedTime = lastConnectedTime;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        BluetoothDeviceModel that = (BluetoothDeviceModel) obj;
        return address != null && address.equals(that.address);
    }

    @Override
    public int hashCode() {
        return address != null ? address.hashCode() : 0;
    }

    @Override
    public String toString() {
        return name + " (" + address + ")";
    }
}