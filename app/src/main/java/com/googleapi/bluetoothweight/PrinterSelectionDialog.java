package com.googleapi.bluetoothweight;


import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.hardware.usb.UsbDevice;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import java.util.List;

public class PrinterSelectionDialog {

    public interface PrinterSelectionCallback {
        void onPrinterSelected(UsbDevice printer);
        void onRefresh();
        void onCancel();
    }

    public static void show(Context context,
                            List<UsbPrinterHelper.PrinterInfo> printers,
                            PrinterSelectionCallback callback) {

        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        LayoutInflater inflater = LayoutInflater.from(context);
        View dialogView = inflater.inflate(R.layout.dialog_printer_selection, null);

        builder.setView(dialogView);
        builder.setTitle("Select USB Printer");

        ListView printerList = dialogView.findViewById(R.id.printerList);
        ProgressBar progressBar = dialogView.findViewById(R.id.progressBar);
        Button refreshButton = dialogView.findViewById(R.id.refreshButton);
        Button cancelButton = dialogView.findViewById(R.id.cancelButton);
        TextView noPrintersText = dialogView.findViewById(R.id.noPrintersText);

        AlertDialog dialog = builder.create();
        dialog.setCancelable(false);

        if (printers.isEmpty()) {
            progressBar.setVisibility(View.GONE);
            noPrintersText.setVisibility(View.VISIBLE);
            printerList.setVisibility(View.GONE);
        } else {
            progressBar.setVisibility(View.GONE);
            noPrintersText.setVisibility(View.GONE);
            printerList.setVisibility(View.VISIBLE);

            PrinterAdapter adapter = new PrinterAdapter(context, printers);
            printerList.setAdapter(adapter);

            printerList.setOnItemClickListener((parent, view, position, id) -> {
                UsbPrinterHelper.PrinterInfo printer = printers.get(position);
                callback.onPrinterSelected(printer.device);
                dialog.dismiss();
            });
        }

        refreshButton.setOnClickListener(v -> {
            callback.onRefresh();
            dialog.dismiss();
        });

        cancelButton.setOnClickListener(v -> {
            callback.onCancel();
            dialog.dismiss();
        });

        dialog.show();
    }

    private static class PrinterAdapter extends ArrayAdapter<UsbPrinterHelper.PrinterInfo> {

        public PrinterAdapter(Context context, List<UsbPrinterHelper.PrinterInfo> printers) {
            super(context, 0, printers);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = LayoutInflater.from(getContext())
                        .inflate(android.R.layout.simple_list_item_2, parent, false);
            }

            UsbPrinterHelper.PrinterInfo printer = getItem(position);

            TextView text1 = convertView.findViewById(android.R.id.text1);
            TextView text2 = convertView.findViewById(android.R.id.text2);

            text1.setText(printer.toString());
            text2.setText(String.format("Vendor ID: 0x%04X, Product ID: 0x%04X",
                    printer.vendorId, printer.productId));

            return convertView;
        }
    }
}