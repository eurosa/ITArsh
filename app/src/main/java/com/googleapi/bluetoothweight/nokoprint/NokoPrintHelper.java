package com.googleapi.bluetoothweight.nokoprint;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.util.Log;
import android.webkit.MimeTypeMap;
import android.widget.Toast;
import androidx.core.content.FileProvider;
import java.io.File;

public class NokoPrintHelper {
    private static final String TAG = "NokoPrintHelper";
    private static final String NOKOPRINT_PACKAGE = "com.nokoprint";

    private Context context;

    public NokoPrintHelper(Context context) {
        this.context = context;
    }

    /**
     * Send PDF to NokoPrint app
     */
    public void printWithNokoPrint(File pdfFile, String fileName) {
        try {
            // Get URI using FileProvider
            Uri uri = FileProvider.getUriForFile(
                    context,
                    context.getPackageName() + ".fileprovider",
                    pdfFile
            );

            // Create intent for NokoPrint
            Intent intent = new Intent(Intent.ACTION_SEND);
            intent.setType("application/pdf");
            intent.putExtra(Intent.EXTRA_STREAM, uri);
            intent.putExtra(Intent.EXTRA_TITLE, fileName);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            intent.setPackage(NOKOPRINT_PACKAGE);

            // Start activity
            context.startActivity(intent);

        } catch (ActivityNotFoundException e) {
            handleNokoPrintNotInstalled();
        } catch (Exception e) {
            Log.e(TAG, "Error sending to NokoPrint", e);
            Toast.makeText(context, "Error opening NokoPrint", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Open NokoPrint app directly (just launches the app)
     */
    public void openNokoPrintApp() {
        try {
            Intent launchIntent = context.getPackageManager()
                    .getLaunchIntentForPackage(NOKOPRINT_PACKAGE);

            if (launchIntent != null) {
                context.startActivity(launchIntent);
            } else {
                handleNokoPrintNotInstalled();
            }

        } catch (Exception e) {
            Log.e(TAG, "Error opening NokoPrint app", e);
            handleNokoPrintNotInstalled();
        }
    }

    /**
     * Open specific activity in NokoPrint if available
     */
    public void openNokoPrintActivity(String activityName) {
        try {
            Intent intent = new Intent();
            intent.setClassName(NOKOPRINT_PACKAGE, activityName);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);

        } catch (ActivityNotFoundException e) {
            // Fallback to just opening the app
            openNokoPrintApp();
        }
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
     * Get NokoPrint app info
     */
    public PackageInfo getNokoPrintAppInfo() {
        try {
            return context.getPackageManager().getPackageInfo(NOKOPRINT_PACKAGE, 0);
        } catch (PackageManager.NameNotFoundException e) {
            return null;
        }
    }

    /**
     * Open Play Store to install NokoPrint
     */
    public void installNokoPrint() {
        try {
            // Try Play Store app
            Intent intent = new Intent(Intent.ACTION_VIEW,
                    Uri.parse("market://details?id=" + NOKOPRINT_PACKAGE));
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);

        } catch (ActivityNotFoundException e) {
            // Fallback to browser
            Intent intent = new Intent(Intent.ACTION_VIEW,
                    Uri.parse("https://play.google.com/store/apps/details?id=" + NOKOPRINT_PACKAGE));
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
        }
    }

    private void handleNokoPrintNotInstalled() {
        // Show dialog or toast
        Toast.makeText(context, "NokoPrint is not installed", Toast.LENGTH_LONG).show();
        installNokoPrint();
    }
}