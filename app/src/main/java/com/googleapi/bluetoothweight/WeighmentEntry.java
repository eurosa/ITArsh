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

    // Constructors
    public WeighmentEntry() {
    }

    public WeighmentEntry(String serialNo, String vehicleNo, String vehicleType,
                          String material, String party, String charge,
                          String gross, String tare, String net, String manualTare) {
        this.serialNo = serialNo;
        this.vehicleNo = vehicleNo;
        this.vehicleType = vehicleType;
        this.material = material;
        this.party = party;
        this.charge = charge;
        this.gross = gross;
        this.tare = tare;
        this.net = net;
        this.manualTare = manualTare;
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
    public String getManualTare() {
        return manualTare;
    }

    public void setManualTare(String manualTare) {
        this.manualTare = manualTare;
    }
    public void setTare(String tare) {
        this.tare = tare;
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

    // Calculate net weight
    public void calculateNet() {
        try {
            double grossValue = Double.parseDouble(gross);
            double tareValue = Double.parseDouble(tare);
            double netValue = grossValue - tareValue;
            this.net = String.valueOf(netValue);
        } catch (NumberFormatException e) {
            this.net = "0";
        }
    }
}