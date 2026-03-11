package com.googleapi.bluetoothweight;

import android.content.Context;
import android.os.Bundle;
import android.os.CancellationSignal;
import android.os.ParcelFileDescriptor;
import android.print.PageRange;
import android.print.PrintAttributes;
import android.print.PrintDocumentAdapter;
import android.print.PrintDocumentInfo;
import android.print.pdf.PrintedPdfDocument;
import android.graphics.pdf.PdfDocument;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Color;
import android.graphics.Typeface;
import android.text.Layout;
import android.text.StaticLayout;
import android.text.TextPaint;
import android.text.TextUtils;

import java.io.FileOutputStream;
import java.io.IOException;

public class MyPrintDocumentAdapter extends PrintDocumentAdapter {

    private Context context;
    private PrintedPdfDocument pdfDocument;
    private String printText;
    private int totalPages = 1;

    public MyPrintDocumentAdapter(Context context, String printText) {
        this.context = context;
        this.printText = printText;
    }

    @Override
    public void onLayout(PrintAttributes oldAttributes,
                         PrintAttributes newAttributes,
                         CancellationSignal cancellationSignal,
                         LayoutResultCallback callback,
                         Bundle metadata) {

        // Create a PDF document with the requested attributes
        pdfDocument = new PrintedPdfDocument(context, newAttributes);

        // Check if cancelled
        if (cancellationSignal.isCanceled()) {
            callback.onLayoutCancelled();
            return;
        }

        // Calculate page count (simplified - assume 1 page)
        totalPages = 1;

        if (totalPages > 0) {
            // Return print info
            PrintDocumentInfo info = new PrintDocumentInfo.Builder("print_output.pdf")
                    .setContentType(PrintDocumentInfo.CONTENT_TYPE_DOCUMENT)
                    .setPageCount(totalPages)
                    .build();

            callback.onLayoutFinished(info, true);
        } else {
            callback.onLayoutFailed("Page calculation failed");
        }
    }

    @Override
    public void onWrite(PageRange[] pageRanges,
                        ParcelFileDescriptor destination,
                        CancellationSignal cancellationSignal,
                        WriteResultCallback callback) {

        // Start a page
        PdfDocument.Page page = pdfDocument.startPage(0);

        // Check if cancelled
        if (cancellationSignal.isCanceled()) {
            callback.onWriteCancelled();
            pdfDocument.close();
            pdfDocument = null;
            return;
        }

        // Draw content on the page
        drawPage(page);

        // Finish the page
        pdfDocument.finishPage(page);

        // Write the PDF to the destination
        try {
            pdfDocument.writeTo(new FileOutputStream(destination.getFileDescriptor()));

            // Success
            callback.onWriteFinished(new PageRange[]{PageRange.ALL_PAGES});

        } catch (IOException e) {
            callback.onWriteFailed(e.toString());
        } finally {
            pdfDocument.close();
            pdfDocument = null;
        }
    }

    private void drawPage(PdfDocument.Page page) {
        Canvas canvas = page.getCanvas();

        // Set background
        canvas.drawColor(Color.WHITE);

        // Create paint for text
        Paint paint = new Paint();
        paint.setColor(Color.BLACK);
        paint.setTextSize(30);
        paint.setTypeface(Typeface.create(Typeface.MONOSPACE, Typeface.NORMAL));

        // Starting position
        float x = 50;
        float y = 100;
        float lineHeight = 40;

        // Draw the text line by line
        String[] lines = printText.split("\n");
        for (String line : lines) {
            canvas.drawText(line, x, y, paint);
            y += lineHeight;

            // Check if we need a new page (simplified - you might want to handle multi-page)
            if (y > page.getInfo().getPageHeight() - 50) {
                break;
            }
        }

        // Draw a border
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(2);
        canvas.drawRect(20, 20, page.getInfo().getPageWidth() - 20,
                page.getInfo().getPageHeight() - 20, paint);
    }
}