package com.googleapi.bluetoothweight;


import android.content.Context;
import android.os.Bundle;
import android.os.CancellationSignal;
import android.os.ParcelFileDescriptor;
import android.print.PageRange;
import android.print.PrintAttributes;
import android.print.PrintDocumentAdapter;
import android.print.PrintDocumentInfo;
import android.util.Log;

import java.io.FileOutputStream;
import java.io.IOException;

public class WeighmentPrintAdapter extends PrintDocumentAdapter {
    private static final String TAG = "WeighmentPrintAdapter";
    private Context context;
    private String printContent;
    private WeighmentEntry entry;

    public WeighmentPrintAdapter(Context context, String printContent, WeighmentEntry entry) {
        this.context = context;
        this.printContent = printContent;
        this.entry = entry;
    }

    @Override
    public void onLayout(PrintAttributes oldAttributes, PrintAttributes newAttributes,
                         CancellationSignal cancellationSignal,
                         LayoutResultCallback callback, Bundle extras) {

        if (cancellationSignal.isCanceled()) {
            callback.onLayoutCancelled();
            return;
        }

        PrintDocumentInfo info = new PrintDocumentInfo.Builder("weighment_" + entry.getVehicleNo() + ".txt")
                .setContentType(PrintDocumentInfo.CONTENT_TYPE_DOCUMENT)
                .setPageCount(1)
                .build();

        callback.onLayoutFinished(info, true);
    }

    @Override
    public void onWrite(PageRange[] pages, ParcelFileDescriptor destination,
                        CancellationSignal cancellationSignal, WriteResultCallback callback) {

        try {
            if (cancellationSignal.isCanceled()) {
                callback.onWriteCancelled();
                return;
            }

            FileOutputStream output = new FileOutputStream(destination.getFileDescriptor());

            // Write the content with proper encoding for dot matrix
            output.write(printContent.getBytes("IBM437"));
            output.close();

            callback.onWriteFinished(new PageRange[]{PageRange.ALL_PAGES});

        } catch (IOException e) {
            Log.e(TAG, "Error writing print content", e);
            callback.onWriteFailed(e.getMessage());
        }
    }
}