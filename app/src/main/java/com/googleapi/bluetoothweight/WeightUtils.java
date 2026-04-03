package com.googleapi.bluetoothweight;

public class WeightUtils {

    public static boolean isPositiveWeight(String weightStr) {
        if (weightStr == null || weightStr.isEmpty()) return false;
        try {
            return Long.parseLong(weightStr) > 0;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    public static long getWeightValue(String weightStr) {
        if (weightStr == null || weightStr.isEmpty()) return 0;
        try {
            return Long.parseLong(weightStr);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    public static String formatWeight(String weightStr) {
        long value = getWeightValue(weightStr);
        return String.format("%,d", value);
    }

    public static boolean isCompleteWeighment(WeighmentEntry entry) {
        if (entry == null) return false;
        return getWeightValue(entry.getGross()) > 0 &&
                (getWeightValue(entry.getTare()) > 0 || getWeightValue(entry.getManualTare()) > 0);
    }
}

// Usage
/*if (WeightUtils.isPositiveWeight(entry.getGross())) {
String formattedGross = WeightUtils.formatWeight(entry.getGross());
// Use formattedGross for display
}

        if (WeightUtils.isCompleteWeighment(entry)) {
// Both gross and tare are available
String timestamp = entry.getTimestamp();
}*/