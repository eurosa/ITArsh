package com.googleapi.bluetoothweight;

public class WeighmentEntry {
    private int id;
    private String serialNo;
    private String vehicleNo;
    private String vehicleType;
    private String material;
    private String party;
    private String charge;
    private String gross;
    private String tare;
    private String manualTare;
    private String net;
    private String timestamp;
    private boolean finalized; // New field to track if entry is finalized

    // Default constructor
    public WeighmentEntry() {
        this.finalized = false; // Default to not finalized
    }

    // Getters and Setters
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getSerialNo() {
        return serialNo;
    }

    public void setSerialNo(String serialNo) {
        this.serialNo = serialNo;
    }

    public String getVehicleNo() {
        return vehicleNo;
    }

    public void setVehicleNo(String vehicleNo) {
        this.vehicleNo = vehicleNo;
    }

    public String getVehicleType() {
        return vehicleType;
    }

    public void setVehicleType(String vehicleType) {
        this.vehicleType = vehicleType;
    }

    public String getMaterial() {
        return material;
    }

    public void setMaterial(String material) {
        this.material = material;
    }

    public String getParty() {
        return party;
    }

    public void setParty(String party) {
        this.party = party;
    }

    public String getCharge() {
        return charge;
    }

    public void setCharge(String charge) {
        this.charge = charge;
    }

    public String getGross() {
        return gross;
    }

    public void setGross(String gross) {
        this.gross = gross;
    }

    public String getTare() {
        return tare;
    }

    public void setTare(String tare) {
        this.tare = tare;
    }

    public String getManualTare() {
        return manualTare;
    }

    public void setManualTare(String manualTare) {
        this.manualTare = manualTare;
    }

    public String getNet() {
        return net;
    }

    public void setNet(String net) {
        this.net = net;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }

    public boolean isFinalized() {
        return finalized;
    }

    public void setFinalized(boolean finalized) {
        this.finalized = finalized;
    }

    // Calculate net weight from gross and tare
    public void calculateNet() {
        try {
            long grossValue = gross != null && !gross.isEmpty() ? Long.parseLong(gross) : 0;
            long tareValue = tare != null && !tare.isEmpty() ? Long.parseLong(tare) : 0;
            long manualTareValue = manualTare != null && !manualTare.isEmpty() ? Long.parseLong(manualTare) : 0;

            // Use the greater of tare and manualTare
            long effectiveTare = Math.max(tareValue, manualTareValue);

            long netValue = grossValue - effectiveTare;
            if (netValue < 0) netValue = 0;

            this.net = String.valueOf(netValue);
        } catch (NumberFormatException e) {
            this.net = "0";
        }
    }
}