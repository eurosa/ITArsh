package com.googleapi.bluetoothweight;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.os.Bundle;
import android.os.CancellationSignal;
import android.print.PageRange;
import android.print.PrintAttributes;
import android.print.PrintDocumentAdapter;
import android.print.PrintDocumentInfo;
import android.print.pdf.PrintedPdfDocument;

import java.io.FileOutputStream;
import java.io.IOException;

public class SimplePrintDocumentAdapter extends PrintDocumentAdapter {

    Context context;
    String text;
    PrintedPdfDocument pdfDocument;

    public SimplePrintDocumentAdapter(Context context, String text) {
        this.context = context;
        this.text = text;
    }

    @Override
    public void onLayout(PrintAttributes oldAttributes,
                         PrintAttributes newAttributes,
                         CancellationSignal cancellationSignal,
                         LayoutResultCallback callback,
                         Bundle extras) {

        pdfDocument = new PrintedPdfDocument(context, newAttributes);

        if (cancellationSignal.isCanceled()) {
            callback.onLayoutCancelled();
            return;
        }

        PrintDocumentInfo info = new PrintDocumentInfo
                .Builder("weighment.pdf")
                .setContentType(PrintDocumentInfo.CONTENT_TYPE_DOCUMENT)
                .setPageCount(1)
                .build();

        callback.onLayoutFinished(info, true);
    }

    @Override
    public void onWrite(PageRange[] pages,
                        android.os.ParcelFileDescriptor destination,
                        CancellationSignal cancellationSignal,
                        WriteResultCallback callback) {

        PrintedPdfDocument.Page page = pdfDocument.startPage(0);

        Canvas canvas = page.getCanvas();
        Paint paint = new Paint();
        paint.setTextSize(12);

        int y = 25;
        for (String line : text.split("\n")) {
            canvas.drawText(line, 10, y, paint);
            y += 20;
        }

        pdfDocument.finishPage(page);

        try {
            pdfDocument.writeTo(new FileOutputStream(
                    destination.getFileDescriptor()));
        } catch (IOException e) {
            callback.onWriteFailed(e.toString());
            return;
        } finally {
            pdfDocument.close();
        }

        callback.onWriteFinished(new PageRange[]{PageRange.ALL_PAGES});
    }
}