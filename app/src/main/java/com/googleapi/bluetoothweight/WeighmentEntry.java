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
    private String finalizedTimestamp; // Track when entry was finalized
    private boolean finalized;

    // Default constructor
    public WeighmentEntry() {
        this.finalized = false;
    }

    // Parameterized constructor for easy creation
    public WeighmentEntry(String serialNo, String vehicleNo, String vehicleType,
                          String material, String party, String charge,
                          String gross, String tare, String manualTare,
                          String timestamp) {
        this.serialNo = serialNo;
        this.vehicleNo = vehicleNo;
        this.vehicleType = vehicleType;
        this.material = material;
        this.party = party;
        this.charge = charge;
        this.gross = gross;
        this.tare = tare;
        this.manualTare = manualTare;
        this.timestamp = timestamp;
        this.finalized = false;
        calculateNet(); // Auto-calculate net on creation
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
        calculateNet(); // Recalculate when gross changes
    }

    public String getTare() {
        return tare;
    }

    public void setTare(String tare) {
        this.tare = tare;
        calculateNet(); // Recalculate when tare changes
    }

    public String getManualTare() {
        return manualTare;
    }

    public void setManualTare(String manualTare) {
        this.manualTare = manualTare;
        calculateNet(); // Recalculate when manualTare changes
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

    public String getFinalizedTimestamp() {
        return finalizedTimestamp;
    }

    public void setFinalizedTimestamp(String finalizedTimestamp) {
        this.finalizedTimestamp = finalizedTimestamp;
    }

    public boolean isFinalized() {
        return finalized;
    }

    public void setFinalized(boolean finalized) {
        this.finalized = finalized;
    }

    // Calculate net weight from gross and effective tare
    public void calculateNet() {
        try {
            long grossValue = parseWeightValue(gross);
            long tareValue = parseWeightValue(tare);
            long manualTareValue = parseWeightValue(manualTare);

            // Prioritize manualTare over tare if manualTare is provided
            long effectiveTare;
            if (manualTareValue > 0) {
                effectiveTare = manualTareValue;
            } else if (tareValue > 0) {
                effectiveTare = tareValue;
            } else {
                effectiveTare = 0;
            }

            long netValue = grossValue - effectiveTare;
            if (netValue < 0) netValue = 0;

            this.net = String.valueOf(netValue);
        } catch (NumberFormatException e) {
            this.net = "0";
        }
    }

    // Helper method to parse weight values safely
    private long parseWeightValue(String value) {
        if (value != null && !value.isEmpty()) {
            try {
                return Long.parseLong(value);
            } catch (NumberFormatException e) {
                return 0;
            }
        }
        return 0;
    }

    // Get the effective tare value (what's actually used in calculation)
    public long getEffectiveTare() {
        long tareValue = parseWeightValue(tare);
        long manualTareValue = parseWeightValue(manualTare);

        if (manualTareValue > 0) {
            return manualTareValue;
        }
        return tareValue;
    }

    // Check if manual tare is being used
    public boolean isUsingManualTare() {
        return parseWeightValue(manualTare) > 0;
    }

    // Get gross weight as long
    public long getGrossValue() {
        return parseWeightValue(gross);
    }

    // Get tare weight as long
    public long getTareValue() {
        return parseWeightValue(tare);
    }

    // Get manual tare as long
    public long getManualTareValue() {
        return parseWeightValue(manualTare);
    }

    // Get net weight as long
    public long getNetValue() {
        return parseWeightValue(net);
    }

    // Check if all required fields are filled
    public boolean isComplete() {
        return serialNo != null && !serialNo.isEmpty() &&
                vehicleNo != null && !vehicleNo.isEmpty() &&
                gross != null && !gross.isEmpty() &&
                getGrossValue() > 0;
    }

    // Check if entry is ready for finalization
    public boolean isReadyForFinalization() {
        return isComplete() &&
                (getTareValue() > 0 || getManualTareValue() > 0) &&
                getNetValue() > 0;
    }

    @Override
    public String toString() {
        return "WeighmentEntry{" +
                "id=" + id +
                ", serialNo='" + serialNo + '\'' +
                ", vehicleNo='" + vehicleNo + '\'' +
                ", vehicleType='" + vehicleType + '\'' +
                ", material='" + material + '\'' +
                ", party='" + party + '\'' +
                ", charge='" + charge + '\'' +
                ", gross='" + gross + '\'' +
                ", tare='" + tare + '\'' +
                ", manualTare='" + manualTare + '\'' +
                ", net='" + net + '\'' +
                ", timestamp='" + timestamp + '\'' +
                ", finalizedTimestamp='" + finalizedTimestamp + '\'' +
                ", finalized=" + finalized +
                '}';
    }

    // Builder pattern for easier object creation
    public static class Builder {
        private WeighmentEntry entry;

        public Builder() {
            entry = new WeighmentEntry();
        }

        public Builder serialNo(String serialNo) {
            entry.setSerialNo(serialNo);
            return this;
        }

        public Builder vehicleNo(String vehicleNo) {
            entry.setVehicleNo(vehicleNo);
            return this;
        }

        public Builder vehicleType(String vehicleType) {
            entry.setVehicleType(vehicleType);
            return this;
        }

        public Builder material(String material) {
            entry.setMaterial(material);
            return this;
        }

        public Builder party(String party) {
            entry.setParty(party);
            return this;
        }

        public Builder charge(String charge) {
            entry.setCharge(charge);
            return this;
        }

        public Builder gross(String gross) {
            entry.setGross(gross);
            return this;
        }

        public Builder tare(String tare) {
            entry.setTare(tare);
            return this;
        }

        public Builder manualTare(String manualTare) {
            entry.setManualTare(manualTare);
            return this;
        }

        public Builder timestamp(String timestamp) {
            entry.setTimestamp(timestamp);
            return this;
        }

        public Builder finalized(boolean finalized) {
            entry.setFinalized(finalized);
            return this;
        }

        public WeighmentEntry build() {
            entry.calculateNet();
            return entry;
        }
    }
}