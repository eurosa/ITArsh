package com.googleapi.bluetoothweight;


public class MasterData {
    private int id;
    private String type; // "VEHICLE_TYPE", "VEHICLE_NO", "MATERIAL", "PARTY"
    private String value;
    private String timestamp;

    // Constructors
    public MasterData() {}

    public MasterData(String type, String value) {
        this.type = type;
        this.value = value;
    }

    // Getters and Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getValue() { return value; }
    public void setValue(String value) { this.value = value; }

    public String getTimestamp() { return timestamp; }
    public void setTimestamp(String timestamp) { this.timestamp = timestamp; }
}
