package com.googleapi.bluetoothweight;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Environment;
import android.util.Log;

import androidx.core.content.FileProvider;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

public class WeighmentExportHelper {

    private static final String TAG = "WeighmentExportHelper";
    private static final String FILE_PREFIX = "weighment_";
    private static final String FILE_EXTENSION = ".txt";

    /**
     * Create a text file with weighment data
     */
    public static File createWeighmentTextFile(Context context, WeighmentEntry entry) {
        try {
            // Create directory if not exists
            File directory = new File(context.getExternalFilesDir(null), "weighment_exports");
            if (!directory.exists()) {
                directory.mkdirs();
            }

            // Create file name with timestamp
            SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault());
            String timestamp = sdf.format(new Date());
            String fileName = FILE_PREFIX + entry.getSerialNo() + "_" + timestamp + FILE_EXTENSION;

            File file = new File(directory, fileName);

            // Write data to file
            FileOutputStream fos = new FileOutputStream(file);
            OutputStreamWriter writer = new OutputStreamWriter(fos);

            // Write header
            writer.write("=".repeat(50) + "\n");
            writer.write("           WEIGHMENT REPORT\n");
            writer.write("=".repeat(50) + "\n\n");

            // Write entry details
            writer.write("SERIAL NUMBER: " + entry.getSerialNo() + "\n");
            writer.write("VEHICLE NUMBER: " + entry.getVehicleNo() + "\n");
            writer.write("VEHICLE TYPE: " + entry.getVehicleType() + "\n");
            writer.write("MATERIAL: " + entry.getMaterial() + "\n");
            writer.write("PARTY: " + entry.getParty() + "\n");
            writer.write("CHARGE: " + entry.getCharge() + "\n");
            writer.write("-".repeat(50) + "\n");
            writer.write("GROSS WEIGHT: " + formatWeight(entry.getGross()) + " kg\n");
            writer.write("TARE WEIGHT: " + formatWeight(entry.getTare()) + " kg\n");
            writer.write("MANUAL TARE: " + formatWeight(entry.getManualTare()) + " kg\n");
            writer.write("NET WEIGHT: " + formatWeight(entry.getNet()) + " kg\n");
            writer.write("-".repeat(50) + "\n");
            writer.write("TIMESTAMP: " + entry.getTimestamp() + "\n");

            if (entry.isFinalized()) {
                writer.write("STATUS: FINALIZED\n");
                if (entry.getFinalizedTimestamp() != null) {
                    writer.write("FINALIZED AT: " + entry.getFinalizedTimestamp() + "\n");
                }
            } else {
                writer.write("STATUS: PENDING\n");
            }

            writer.write("=".repeat(50) + "\n");
            writer.write("Generated on: " + getCurrentDateTime() + "\n");
            writer.write("=".repeat(50) + "\n");

            writer.flush();
            writer.close();
            fos.close();

            Log.d(TAG, "File created successfully: " + file.getAbsolutePath());
            return file;

        } catch (Exception e) {
            Log.e(TAG, "Error creating file: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Create a CSV file with weighment data (good for Excel)
     */
    public static File createWeighmentCSVFile(Context context, WeighmentEntry entry) {
        try {
            File directory = new File(context.getExternalFilesDir(null), "weighment_exports");
            if (!directory.exists()) {
                directory.mkdirs();
            }

            SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault());
            String timestamp = sdf.format(new Date());
            String fileName = FILE_PREFIX + entry.getSerialNo() + "_" + timestamp + ".csv";

            File file = new File(directory, fileName);

            FileOutputStream fos = new FileOutputStream(file);
            OutputStreamWriter writer = new OutputStreamWriter(fos);

            // Write CSV header
            writer.write("Serial No,Vehicle No,Vehicle Type,Material,Party,Charge,Gross (kg),Tare (kg),Manual Tare (kg),Net (kg),Timestamp,Status,Finalized Timestamp\n");

            // Write data
            writer.write("\"" + entry.getSerialNo() + "\",");
            writer.write("\"" + entry.getVehicleNo() + "\",");
            writer.write("\"" + entry.getVehicleType() + "\",");
            writer.write("\"" + entry.getMaterial() + "\",");
            writer.write("\"" + entry.getParty() + "\",");
            writer.write("\"" + entry.getCharge() + "\",");
            writer.write(entry.getGross() + ",");
            writer.write(entry.getTare() + ",");
            writer.write(entry.getManualTare() + ",");
            writer.write(entry.getNet() + ",");
            writer.write("\"" + entry.getTimestamp() + "\",");
            writer.write(entry.isFinalized() ? "FINALIZED" : "PENDING" + ",");
            writer.write(entry.getFinalizedTimestamp() != null ? "\"" + entry.getFinalizedTimestamp() + "\"" : "");
            writer.write("\n");

            writer.flush();
            writer.close();
            fos.close();

            Log.d(TAG, "CSV file created successfully: " + file.getAbsolutePath());
            return file;

        } catch (Exception e) {
            Log.e(TAG, "Error creating CSV file: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Send file via email
     */
    public static void sendEmailWithAttachment(Context context, File file, String emailAddress, WeighmentEntry entry) {
        try {
            if (file == null || !file.exists()) {
                Log.e(TAG, "File does not exist");
                return;
            }

            // Get URI using FileProvider
            Uri fileUri = FileProvider.getUriForFile(context,
                    context.getPackageName() + ".fileprovider", file);

            Intent emailIntent = new Intent(Intent.ACTION_SEND);
            emailIntent.setType("text/plain");
            emailIntent.putExtra(Intent.EXTRA_STREAM, fileUri);
            emailIntent.putExtra(Intent.EXTRA_EMAIL, new String[]{emailAddress});
            emailIntent.putExtra(Intent.EXTRA_SUBJECT, "Weighment Report - Serial #" + entry.getSerialNo());
            emailIntent.putExtra(Intent.EXTRA_TEXT, buildEmailBody(entry));
            emailIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

            context.startActivity(Intent.createChooser(emailIntent, "Send email via"));

        } catch (Exception e) {
            Log.e(TAG, "Error sending email: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Send multiple files via email
     */
    public static void sendMultipleFilesViaEmail(Context context, ArrayList<File> files,
                                                 String emailAddress, String subject) {
        try {
            Intent emailIntent = new Intent(Intent.ACTION_SEND_MULTIPLE);
            emailIntent.setType("text/plain");

            ArrayList<Uri> uris = new ArrayList<>();
            for (File file : files) {
                Uri fileUri = FileProvider.getUriForFile(context,
                        context.getPackageName() + ".fileprovider", file);
                uris.add(fileUri);
                emailIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            }

            emailIntent.putExtra(Intent.EXTRA_STREAM, uris);
            emailIntent.putExtra(Intent.EXTRA_EMAIL, new String[]{emailAddress});
            emailIntent.putExtra(Intent.EXTRA_SUBJECT, subject);
            emailIntent.putExtra(Intent.EXTRA_TEXT, "Please find attached weighment reports.");

            context.startActivity(Intent.createChooser(emailIntent, "Send email via"));

        } catch (Exception e) {
            Log.e(TAG, "Error sending multiple files: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static String buildEmailBody(WeighmentEntry entry) {
        return "Dear Sir,\n\n" +
                "Please find attached the weighment report.\n\n" +
                "Weighment Details:\n" +
                "-------------------\n" +
                "Serial Number: " + entry.getSerialNo() + "\n" +
                "Vehicle Number: " + entry.getVehicleNo() + "\n" +
                "Vehicle Type: " + entry.getVehicleType() + "\n" +
                "Material: " + entry.getMaterial() + "\n" +
                "Party: " + entry.getParty() + "\n" +
                "Gross Weight: " + formatWeight(entry.getGross()) + " kg\n" +
                "Tare Weight: " + formatWeight(entry.getTare()) + " kg\n" +
                "Net Weight: " + formatWeight(entry.getNet()) + " kg\n" +
                "Timestamp: " + entry.getTimestamp() + "\n" +
                "Status: " + (entry.isFinalized() ? "FINALIZED" : "PENDING") + "\n\n" +
                "Thank you.\n\n" +
                "This is an auto-generated email from Weighment System.";
    }

    private static String formatWeight(String weight) {
        if (weight == null || weight.isEmpty()) return "0";
        try {
            long value = Long.parseLong(weight);
            return String.format("%,d", value);
        } catch (NumberFormatException e) {
            return weight;
        }
    }

    private static String getCurrentDateTime() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
        return sdf.format(new Date());
    }
}