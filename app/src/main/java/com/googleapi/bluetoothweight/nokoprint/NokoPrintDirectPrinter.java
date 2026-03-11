package com.googleapi.bluetoothweight.nokoprint;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.util.Log;
import android.widget.Toast;
import androidx.core.content.FileProvider;
import java.io.File;

public class NokoPrintDirectPrinter {
    private static final String TAG = "NokoPrintPrinter";
    private static final String NOKOPRINT_PACKAGE = "com.nokoprint";

    // Activities discovered from dumpsys
    private static final String ACTIVITY_PRINT_DOCS = "com.nokoprint.ActivityPrintDocs";
    private static final String ACTIVITY_PRINT_PHOTOS = "com.nokoprint.ActivityPrintPhotos";
    private static final String ACTIVITY_DEVICES = "com.nokoprint.ActivityDevices";
    private static final String ACTIVITY_HOME = "com.nokoprint.ActivityHome";
    private static final String ACTIVITY_USB = "com.nokoprint.ActivityUSB";

    private Context context;

    public NokoPrintDirectPrinter(Context context) {
        this.context = context;
    }

    /**
     * Print PDF directly to NokoPrint - Main method to use
     */
    public void printPdf(File pdfFile, String documentName) {
        if (!pdfFile.exists()) {
            Log.e(TAG, "PDF file not found: " + pdfFile.getPath());
            Toast.makeText(context, "PDF file not found", Toast.LENGTH_SHORT).show();
            return;
        }

        // Check if NokoPrint is installed
        if (!isNokoPrintInstalled()) {
            promptInstallNokoPrint();
            return;
        }

        // Try all methods in order of preference
        if (printViaPrintDocsActivity(pdfFile, documentName)) {
            return;
        }

        if (printViaViewIntent(pdfFile)) {
            return;
        }

        if (printViaSendIntent(pdfFile)) {
            return;
        }

        // If all else fails, open NokoPrint main activity
        openNokoPrintHome();
    }
    /**
     * Print a text file using NokoPrint
     */
    public void printTextFile(File textFile, String documentName) {
        if (!textFile.exists()) {
            Log.e(TAG, "Text file not found: " + textFile.getPath());
            return;
        }

        try {
            Uri uri = getFileUri(textFile);

            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setDataAndType(uri, "text/plain");
            intent.setPackage(NOKOPRINT_PACKAGE);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

            // Add document info
            intent.putExtra("document_name", documentName);
            intent.putExtra("print_direct", true);

            // For dot matrix printers
            intent.putExtra("printer_type", "dotmatrix");
            intent.putExtra("paper_size", "continuous");

            context.startActivity(intent);
            Log.d(TAG, "Sent to NokoPrint: " + documentName);

        } catch (ActivityNotFoundException e) {
            Log.e(TAG, "NokoPrint not installed");
            // Handle not installed case
        } catch (Exception e) {
            Log.e(TAG, "Error sending to NokoPrint", e);
        }
    }
    /**
     * Method 1: Direct to ActivityPrintDocs (Most promising based on dumpsys)
     */
    private boolean printViaPrintDocsActivity(File pdfFile, String documentName) {
        try {
            Uri uri = getFileUri(pdfFile);

            // Create intent targeting ActivityPrintDocs directly
            Intent intent = new Intent();
            intent.setClassName(NOKOPRINT_PACKAGE, ACTIVITY_PRINT_DOCS);
            intent.setAction(Intent.ACTION_VIEW);
            intent.setDataAndType(uri, "application/pdf");
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

            // Add important extras
            intent.putExtra("document_name", documentName);
            intent.putExtra("print_direct", true);
            intent.putExtra("auto_print", true);
            intent.putExtra("skip_preview", true);

            // Also try with Excel MIME type (since we know it supports it)
            Intent intent2 = new Intent();
            intent2.setClassName(NOKOPRINT_PACKAGE, ACTIVITY_PRINT_DOCS);
            intent2.setAction(Intent.ACTION_VIEW);
            intent2.setDataAndType(uri, "application/vnd.ms-excel.sheet.macroenabled.12");
            intent2.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            intent2.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            intent2.putExtra("original_mime", "application/pdf");
            intent2.putExtra("document_name", documentName);
            intent2.putExtra("print_direct", true);

            try {
                context.startActivity(intent2);
                Log.d(TAG, "Success: Printed via ActivityPrintDocs with Excel MIME");
                return true;
            } catch (Exception e) {
                context.startActivity(intent);
                Log.d(TAG, "Success: Printed via ActivityPrintDocs with PDF MIME");
                return true;
            }

        } catch (Exception e) {
            Log.e(TAG, "Failed to print via ActivityPrintDocs: " + e.getMessage());
            return false;
        }
    }

    /**
     * Method 2: Use VIEW intent with proper flags
     */
    private boolean printViaViewIntent(File pdfFile) {
        try {
            Uri uri = getFileUri(pdfFile);

            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setDataAndType(uri, "application/pdf");
            intent.setPackage(NOKOPRINT_PACKAGE);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

            // Add print extras
            intent.putExtra("print", true);
            intent.putExtra("direct_print", true);
            intent.putExtra("print_mode", "document");

            context.startActivity(intent);
            Log.d(TAG, "Success: Printed via VIEW intent");
            return true;

        } catch (Exception e) {
            Log.d(TAG, "VIEW intent failed: " + e.getMessage());
            return false;
        }
    }

    /**
     * Method 3: Use SEND intent
     */
    private boolean printViaSendIntent(File pdfFile) {
        try {
            Uri uri = getFileUri(pdfFile);

            Intent intent = new Intent(Intent.ACTION_SEND);
            intent.setType("application/pdf");
            intent.putExtra(Intent.EXTRA_STREAM, uri);
            intent.setPackage(NOKOPRINT_PACKAGE);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

            // Add print-specific extras
            intent.putExtra(Intent.EXTRA_SUBJECT, "Print Document");
            intent.putExtra("print_directly", true);
            intent.putExtra("skip_preview", true);
            intent.putExtra("print_copies", 1);

            context.startActivity(intent);
            Log.d(TAG, "Success: Printed via SEND intent");
            return true;

        } catch (Exception e) {
            Log.d(TAG, "SEND intent failed: " + e.getMessage());
            return false;
        }
    }

    /**
     * Method 4: Try USB printing activity (if connected via USB)
     */
    public boolean printViaUSB(File pdfFile) {
        try {
            Uri uri = getFileUri(pdfFile);

            Intent intent = new Intent();
            intent.setClassName(NOKOPRINT_PACKAGE, ACTIVITY_USB);
            intent.setAction(Intent.ACTION_VIEW);
            intent.setDataAndType(uri, "application/pdf");
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

            intent.putExtra("print_direct", true);
            intent.putExtra("connection_type", "usb");

            context.startActivity(intent);
            Log.d(TAG, "Success: USB printing");
            return true;

        } catch (Exception e) {
            Log.d(TAG, "USB printing failed: " + e.getMessage());
            return false;
        }
    }

    /**
     * Open printer devices list
     */
    public void openPrinterDevices() {
        try {
            Intent intent = new Intent();
            intent.setClassName(NOKOPRINT_PACKAGE, ACTIVITY_DEVICES);
            intent.setAction(Intent.ACTION_VIEW);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
            Log.d(TAG, "Opened printer devices");
        } catch (Exception e) {
            Log.e(TAG, "Failed to open printer devices: " + e.getMessage());
            openNokoPrintHome();
        }
    }

    /**
     * Open NokoPrint home/main screen
     */
    public void openNokoPrintHome() {
        try {
            Intent intent = new Intent();
            intent.setClassName(NOKOPRINT_PACKAGE, ACTIVITY_HOME);
            intent.setAction(Intent.ACTION_MAIN);
            intent.addCategory(Intent.CATEGORY_LAUNCHER);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
            Log.d(TAG, "Opened NokoPrint home");
        } catch (Exception e) {
            Log.e(TAG, "Failed to open NokoPrint home: " + e.getMessage());
            // Last resort: try to launch via package manager
            Intent launchIntent = context.getPackageManager()
                    .getLaunchIntentForPackage(NOKOPRINT_PACKAGE);
            if (launchIntent != null) {
                context.startActivity(launchIntent);
            }
        }
    }

    /**
     * Get URI for file using FileProvider
     */
    private Uri getFileUri(File file) {
        return FileProvider.getUriForFile(
                context,
                context.getPackageName() + ".fileprovider",
                file
        );
    }

    /**
     * Check if NokoPrint is installed
     */
    public boolean isNokoPrintInstalled() {
        try {
            context.getPackageManager().getPackageInfo(NOKOPRINT_PACKAGE, 0);
            return true;
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
    }

    /**
     * Get NokoPrint version info
     */
    public String getNokoPrintVersion() {
        try {
            return context.getPackageManager()
                    .getPackageInfo(NOKOPRINT_PACKAGE, 0)
                    .versionName;
        } catch (Exception e) {
            return "Not installed";
        }
    }

    /**
     * Prompt user to install NokoPrint
     */
    private void promptInstallNokoPrint() {
        new androidx.appcompat.app.AlertDialog.Builder(context)
                .setTitle("NokoPrint Required")
                .setMessage("NokoPrint is not installed. Would you like to install it from Play Store?")
                .setPositiveButton("Install", (dialog, which) -> {
                    openPlayStore();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    /**
     * Open Play Store to install NokoPrint
     */
    private void openPlayStore() {
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW,
                    Uri.parse("market://details?id=" + NOKOPRINT_PACKAGE));
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
        } catch (ActivityNotFoundException e) {
            Intent intent = new Intent(Intent.ACTION_VIEW,
                    Uri.parse("https://play.google.com/store/apps/details?id=" + NOKOPRINT_PACKAGE));
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
        }
    }
}
