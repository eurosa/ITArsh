package com.googleapi.bluetoothweight.bluetooth;

public class BluetoothUtils {

    // Convert byte array to hex string
    public static String bytesToHex(byte[] bytes) {
        if (bytes == null) return "null";
        char[] hexChars = new char[bytes.length * 3];
        for (int i = 0; i < bytes.length; i++) {
            int v = bytes[i] & 0xFF;
            hexChars[i * 3] = HEX_ARRAY[v >>> 4];
            hexChars[i * 3 + 1] = HEX_ARRAY[v & 0x0F];
            hexChars[i * 3 + 2] = ' '; // Add space between bytes
        }
        return new String(hexChars).trim();
    }

    private static final char[] HEX_ARRAY = "0123456789ABCDEF".toCharArray();

    // Convert hex string to byte array
    public static byte[] hexToBytes(String hex) {
        hex = hex.replaceAll(" ", ""); // Remove any spaces
        int len = hex.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(hex.charAt(i), 16) << 4)
                    + Character.digit(hex.charAt(i + 1), 16));
        }
        return data;
    }

    // Convert single byte to hex string
    public static String byteToHex(byte b) {
        char[] hexChars = new char[2];
        int v = b & 0xFF;
        hexChars[0] = HEX_ARRAY[v >>> 4];
        hexChars[1] = HEX_ARRAY[v & 0x0F];
        return new String(hexChars);
    }

    // Convert int to 2-byte array (big-endian)
    public static byte[] intToBytes(int value) {
        return new byte[] {
                (byte)(value >> 8),
                (byte)value
        };
    }

    // Convert 2-byte array to int (big-endian)
    public static int bytesToInt(byte[] bytes) {
        if (bytes.length < 2) return 0;
        return ((bytes[0] & 0xFF) << 8) | (bytes[1] & 0xFF);
    }
}