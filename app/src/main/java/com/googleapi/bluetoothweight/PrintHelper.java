package com.googleapi.bluetoothweight;

import android.app.Activity;
import android.content.Context;
import android.print.PrintAttributes;
import android.print.PrintManager;
import android.widget.Toast;

public class PrintHelper {

    private Activity activity;

    public PrintHelper(Activity activity) {
        this.activity = activity;
    }

    /**
     * Check if printing is available
     */
    public boolean isPrintingAvailable() {
        PrintManager printManager = (PrintManager) activity.getSystemService(Context.PRINT_SERVICE);
        return printManager != null;
    }

    /**
     * Show available printers (for debugging)
     */
    public void showAvailablePrinters() {
        PrintManager printManager = (PrintManager) activity.getSystemService(Context.PRINT_SERVICE);

        // Note: Android doesn't provide direct API to list printers
        // But we can show instructions
        Toast.makeText(activity,
                "Click the dropdown in print dialog to select Canon printer",
                Toast.LENGTH_LONG).show();
    }

    /**
     * Get recommended print attributes for thermal/receipt printers
     */
    public PrintAttributes getReceiptPrintAttributes() {
        PrintAttributes.Builder builder = new PrintAttributes.Builder();

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.KITKAT) {
            // Use 80mm paper size (common for receipt printers)
            builder.setMediaSize(PrintAttributes.MediaSize.UNKNOWN_LANDSCAPE);
            builder.setMinMargins(PrintAttributes.Margins.NO_MARGINS);
            builder.setColorMode(PrintAttributes.COLOR_MODE_MONOCHROME);
        }

        return builder.build();
    }
}