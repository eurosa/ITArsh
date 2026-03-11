package com.googleapi.bluetoothweight;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.AppCompatButton;
import androidx.fragment.app.Fragment;
import androidx.print.PrintHelper;

import com.googleapi.bluetoothweight.nokoprint.NokoPrintDirectPrinter;

import java.io.File;
import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class DFragment extends Fragment {

    private AppCompatButton button4a, button5a;
    private EditText serialEditText, chargeEditText, grossEditText, tareEditText, netWeightEditText;
    private Button selectPrinterButton;
    private boolean useUsbPrinting = false;

    // AutoCompleteTextViews for dropdowns
    private AutoCompleteTextView vehicleNoSpinner, vehicleTypeSpinner,
            materialSpinner, partySpinner;

    private TextView txtDisplayTwoView;

    private DatabaseHelper databaseHelper;
    private MainActivity mainActivity;

    // Button references for T, G
    private AppCompatButton buttonT, buttonG;

    // Search field
    private EditText searchSerialEditText;

    // Store the current serial number being viewed
    private String currentSerialNo = "";

    // Adapters for dropdowns
    private ArrayAdapter<String> vehicleNoAdapter;
    private ArrayAdapter<String> vehicleTypeAdapter;
    private ArrayAdapter<String> materialAdapter;
    private ArrayAdapter<String> partyAdapter;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_d, container, false);

        // Initialize DatabaseHelper
        databaseHelper = new DatabaseHelper(getActivity());
        mainActivity = (MainActivity) getActivity();

        // Initialize views
        initViews(view);

        // Setup dropdown adapters
        setupDropdownAdapters();

        // Setup listeners
        setupCalculationListeners();
        setupSearchFunctionality();
        setupActionButtons();
        setupButtonFocusListeners();
        setupTGMButtons();

        // Make all fields non-editable initially
        setFormFieldsNonEditable();

        // Hide T and G buttons as they're not needed for view-only mode
        if (buttonT != null) buttonT.setVisibility(View.GONE);
        if (buttonG != null) buttonG.setVisibility(View.GONE);

        return view;
    }

    private void initViews(View view) {
        // Search field
        searchSerialEditText = view.findViewById(R.id.searchSerialEditText);

        // Printer selection button
        selectPrinterButton = view.findViewById(R.id.selectPrinterButton);

        selectPrinterButton.setVisibility(View.GONE);
        // Form fields
        serialEditText = view.findViewById(R.id.serialEditText);

        // Initialize AutoCompleteTextViews
        vehicleNoSpinner = view.findViewById(R.id.vehicleNoSpinner);
        vehicleTypeSpinner = view.findViewById(R.id.vehicleTypeSpinner);
        materialSpinner = view.findViewById(R.id.materialSpinner);
        partySpinner = view.findViewById(R.id.partySpinner);

        chargeEditText = view.findViewById(R.id.chargeEditText);
        grossEditText = view.findViewById(R.id.grossEditText);
        tareEditText = view.findViewById(R.id.tareEditText);
        netWeightEditText = view.findViewById(R.id.netWeightEditText);

        // T and G buttons
        buttonT = view.findViewById(R.id.buttonT);
        buttonG = view.findViewById(R.id.buttonG);

        // Display text view
        txtDisplayTwoView = view.findViewById(R.id.txtDisplayTwoView);

        // Action buttons
        button4a = view.findViewById(R.id.button4a);
        button5a = view.findViewById(R.id.button5a);

        // Make Print button visible
        button5a.setVisibility(View.VISIBLE);
        button5a.setText("Print");

        // Make serialEditText non-editable (will be populated from search)
        serialEditText.setFocusable(false);
        serialEditText.setClickable(false);

        // Setup focus for buttons
        button4a.setFocusable(true);
        button4a.setFocusableInTouchMode(true);
        button5a.setFocusable(true);
        button5a.setFocusableInTouchMode(true);

        // Set IME options
        searchSerialEditText.setImeOptions(EditorInfo.IME_ACTION_SEARCH);

        // Setup printer button based on MainActivity's printer status
        if (mainActivity != null) {
            updatePrinterStatus(mainActivity.isPrinterConnected(), mainActivity.getConnectedPrinterName());
        }
    }

    /**
     * Update printer status from MainActivity
     */
    public void updatePrinterStatus(boolean connected, String printerName) {
        this.useUsbPrinting = connected;

        if (selectPrinterButton != null) {
            if (connected) {
                selectPrinterButton.setText("Printer: " + printerName);
                selectPrinterButton.setEnabled(false);
            } else {
                selectPrinterButton.setText("Select USB Printer");
                selectPrinterButton.setEnabled(true);
            }
        }
    }

    /**
     * Make all form fields non-editable
     */
    private void setFormFieldsNonEditable() {
        // Disable all input fields
        vehicleNoSpinner.setEnabled(false);
        vehicleTypeSpinner.setEnabled(false);
        materialSpinner.setEnabled(false);
        partySpinner.setEnabled(false);
        chargeEditText.setEnabled(false);
        grossEditText.setEnabled(false);
        tareEditText.setEnabled(false);
        netWeightEditText.setEnabled(false);

        // Set focusable to false
        vehicleNoSpinner.setFocusable(false);
        vehicleNoSpinner.setFocusableInTouchMode(false);
        vehicleTypeSpinner.setFocusable(false);
        vehicleTypeSpinner.setFocusableInTouchMode(false);
        materialSpinner.setFocusable(false);
        materialSpinner.setFocusableInTouchMode(false);
        partySpinner.setFocusable(false);
        partySpinner.setFocusableInTouchMode(false);
        chargeEditText.setFocusable(false);
        chargeEditText.setFocusableInTouchMode(false);
        grossEditText.setFocusable(false);
        grossEditText.setFocusableInTouchMode(false);
        tareEditText.setFocusable(false);
        tareEditText.setFocusableInTouchMode(false);
        netWeightEditText.setFocusable(false);
        netWeightEditText.setFocusableInTouchMode(false);

        // Set alpha for visual indication
        float alpha = 0.8f;
        vehicleNoSpinner.setAlpha(alpha);
        vehicleTypeSpinner.setAlpha(alpha);
        materialSpinner.setAlpha(alpha);
        partySpinner.setAlpha(alpha);
        chargeEditText.setAlpha(alpha);
        grossEditText.setAlpha(alpha);
        tareEditText.setAlpha(alpha);
        netWeightEditText.setAlpha(alpha);

        // Hide T and G buttons
        if (buttonT != null) buttonT.setVisibility(View.GONE);
        if (buttonG != null) buttonG.setVisibility(View.GONE);

        // Hide the Finalize button and show Print button
        button4a.setVisibility(View.GONE);
        button5a.setVisibility(View.VISIBLE);
    }

    private void setupDropdownAdapters() {
        // Vehicle Number dropdown
        List<String> vehicleNumbers = databaseHelper.getUniqueVehicleNumbers();
        vehicleNoAdapter = new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_dropdown_item_1line, vehicleNumbers);
        vehicleNoSpinner.setAdapter(vehicleNoAdapter);
        vehicleNoSpinner.setThreshold(1);

        // Vehicle Type dropdown
        List<String> vehicleTypes = databaseHelper.getUniqueVehicleTypes();
        vehicleTypeAdapter = new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_dropdown_item_1line, vehicleTypes);
        vehicleTypeSpinner.setAdapter(vehicleTypeAdapter);
        vehicleTypeSpinner.setThreshold(1);

        // Material dropdown
        List<String> materials = databaseHelper.getUniqueMaterials();
        materialAdapter = new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_dropdown_item_1line, materials);
        materialSpinner.setAdapter(materialAdapter);
        materialSpinner.setThreshold(1);

        // Party dropdown
        List<String> parties = databaseHelper.getUniqueParties();
        partyAdapter = new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_dropdown_item_1line, parties);
        partySpinner.setAdapter(partyAdapter);
        partySpinner.setThreshold(1);
    }

    private void refreshAllAdapters() {
        List<String> vehicleNumbers = databaseHelper.getUniqueVehicleNumbers();
        vehicleNoAdapter.clear();
        vehicleNoAdapter.addAll(vehicleNumbers);
        vehicleNoAdapter.notifyDataSetChanged();

        List<String> vehicleTypes = databaseHelper.getUniqueVehicleTypes();
        vehicleTypeAdapter.clear();
        vehicleTypeAdapter.addAll(vehicleTypes);
        vehicleTypeAdapter.notifyDataSetChanged();

        List<String> materials = databaseHelper.getUniqueMaterials();
        materialAdapter.clear();
        materialAdapter.addAll(materials);
        materialAdapter.notifyDataSetChanged();

        List<String> parties = databaseHelper.getUniqueParties();
        partyAdapter.clear();
        partyAdapter.addAll(parties);
        partyAdapter.notifyDataSetChanged();
    }

    /**
     * Get the current value from MainActivity's txtCounter
     */
    private String getCounterValue() {
        if (mainActivity == null) {
            Log.e("DFragment", "mainActivity is null in getCounterValue");
            return "";
        }
        if (mainActivity.txtCounter == null) {
            Log.e("DFragment", "mainActivity.txtCounter is null in getCounterValue");
            return "";
        }
        String value = mainActivity.txtCounter.getText().toString();
        String cleaned = value.replaceAll("\\s", "");
        Log.d("DFragment", "getCounterValue - original: '" + value + "', cleaned: '" + cleaned + "'");
        return cleaned;
    }

    /**
     * Setup search functionality for searchSerialEditText
     */
    private void setupSearchFunctionality() {
        // Clear any existing listeners
        searchSerialEditText.setOnEditorActionListener(null);
        searchSerialEditText.setOnKeyListener(null);

        // Editor action listener for soft keyboard
        searchSerialEditText.setOnEditorActionListener((v, actionId, event) -> {
            Log.d("DFragment", "Search field editor action: " + actionId);

            if (actionId == EditorInfo.IME_ACTION_SEARCH ||
                    actionId == EditorInfo.IME_ACTION_DONE ||
                    actionId == EditorInfo.IME_ACTION_NEXT ||
                    actionId == EditorInfo.IME_ACTION_GO) {

                loadEntry();
                return true;
            }
            return false;
        });

        // Hardware key listener for physical Enter key
        searchSerialEditText.setOnKeyListener((v, keyCode, event) -> {
            if (keyCode == KeyEvent.KEYCODE_ENTER || keyCode == KeyEvent.KEYCODE_NUMPAD_ENTER) {
                Log.d("DFragment", "Physical Enter on search field");
                loadEntry();
                return true;
            }
            return false;
        });
    }

    /**
     * Setup action buttons - Print button always available
     */
    private void setupActionButtons() {
        // Print button click listener
        button5a.setOnClickListener(v -> {
           // printCurrentEntry();
            printWeighmentEntry();
        });

        // Handle Enter key on Print button
        button5a.setOnKeyListener((v, keyCode, event) -> {
            if (keyCode == KeyEvent.KEYCODE_ENTER && event.getAction() == KeyEvent.ACTION_DOWN) {
               // printCurrentEntry();
                printWeighmentEntry();
                return true;
            }
            return false;
        });
    }

    /**
     * Print the currently loaded entry
     */
    private void printCurrentEntry() {
        if (currentSerialNo.isEmpty()) {
            Toast.makeText(getActivity(), "Please load an entry first", Toast.LENGTH_SHORT).show();
            searchSerialEditText.requestFocus();
            return;
        }

        WeighmentEntry entry = databaseHelper.getWeighmentBySerialNo(currentSerialNo);
        if (entry == null) {
            Toast.makeText(getActivity(), "Entry not found", Toast.LENGTH_SHORT).show();
            return;
        }

        // Build print content
        String printContent = buildPrintText(entry);

        // Use shared printer manager from MainActivity
        if (mainActivity != null && mainActivity.isPrinterConnected()) {
            PrinterManager printerManager = mainActivity.getPrinterManager();

            printerManager.addListener(new PrinterManager.PrinterConnectionAdapter() {
                @Override
                public void onPrintSuccess() {
                    if (getActivity() != null) {
                        getActivity().runOnUiThread(() -> {
                            printerManager.removeListener(this);
                            Toast.makeText(getActivity(), "Print successful", Toast.LENGTH_SHORT).show();

                            if (txtDisplayTwoView != null) {
                                SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss", Locale.getDefault());
                                txtDisplayTwoView.setText("PRINTED: " + entry.getSerialNo() +
                                        " | Net: " + entry.getNet() + " KG at " + sdf.format(new Date()));
                            }
                        });
                    }
                }

                @Override
                public void onPrintError(String error) {
                    if (getActivity() != null) {
                        getActivity().runOnUiThread(() -> {
                            printerManager.removeListener(this);
                            Toast.makeText(getActivity(), "Print error: " + error, Toast.LENGTH_LONG).show();
                            showPrintPreviewDialog(printContent, entry);
                        });
                    }
                }
            });

            boolean printed = printerManager.autoDetectAndPrint(printContent);
            if (!printed) {
                printed = printerManager.printText(printContent);
            }
            if (!printed) {
                printed = printerManager.printPlainText(printContent);
            }
            if (!printed) {
                printerManager.removeListener(printerManager.listeners.get(printerManager.listeners.size() - 1));
                showPrintPreviewDialog(printContent, entry);
            }
        } else {
            showPrintPreviewDialog(printContent, entry);
        }
    }

    private void printWeighmentEntry() {
        // Get values from fields
        String serialNo = serialEditText.getText().toString().trim();
        String vehicleNo = vehicleNoSpinner.getText().toString().trim();
        String vehicleType = vehicleTypeSpinner.getText().toString().trim();
        String material = materialSpinner.getText().toString().trim();
        String party = partySpinner.getText().toString().trim();
        String charge = chargeEditText.getText().toString().trim();
        String gross = grossEditText.getText().toString().trim();
        String tare = tareEditText.getText().toString().trim();

        // Validate serial number
        if (serialNo.isEmpty()) {
            Toast.makeText(getActivity(), "Serial number is required", Toast.LENGTH_SHORT).show();
            serialEditText.requestFocus();
            return;
        }

        // Validate vehicle number
        if (vehicleNo.isEmpty()) {
            vehicleNoSpinner.setError("Vehicle number is required");
            vehicleNoSpinner.requestFocus();
            return;
        }

        // Create entry object
        WeighmentEntry entry = new WeighmentEntry();
        entry.setSerialNo(serialNo);
        entry.setVehicleNo(vehicleNo);
        entry.setVehicleType(vehicleType);
        entry.setMaterial(material);
        entry.setParty(party);
        entry.setCharge(charge);
        entry.setGross(gross);
        entry.setTare(tare);

        entry.calculateNet();

        // Show print options dialog
        showPrintOptionsDialog(entry);
    }

    private void showPrintOptionsDialog(WeighmentEntry entry) {
        String[] options = {
                "PCL Print (HP LaserJet)",      // 0 - New PCL option
                "Android Print Framework",      // 1 - Your existing method
                "USB Direct Print",             // 2
                "Bluetooth Print",              // 3
                "NokoPrint App",                // 4
                "Auto Detect Best Method"       // 5
        };

        new AlertDialog.Builder(requireContext())
                .setTitle("Select Print Method")
                .setItems(options, (dialog, which) -> {
                    switch (which) {
                        case 0: // PCL Print
                            printWithPCL(entry);
                            break;
                        case 1: // Android Print Framework
                            printUsingAndroidPrintFramework(entry);
                            break;
                        case 2: // USB Direct Print
                            printWithUSB(entry);
                            break;
                        case 3: // Bluetooth Print
                            printWithBluetooth(entry);
                            break;
                        case 4: // NokoPrint App
                            printWithNokoPrint(entry);
                            break;
                        case 5: // Auto Detect
                            autoDetectAndPrint(entry);
                            break;
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void printWithPCL(WeighmentEntry entry) {
        if (mainActivity == null || !mainActivity.isPrinterConnected()) {
            Toast.makeText(getActivity(), "Printer not connected", Toast.LENGTH_SHORT).show();
            return;
        }

        ProgressDialog progressDialog = new ProgressDialog(getActivity());
        progressDialog.setMessage("Printing with PCL...\nPlease wait");
        progressDialog.setCancelable(false);
        progressDialog.show();

        // Build PCL formatted ticket
        String pclContent = buildPCLTicket(entry);

        PrinterManager printerManager = mainActivity.getPrinterManager();

        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.submit(() -> {
            boolean result = false;
            if (printerManager.usbPrinterHelper != null) {
                result = printerManager.usbPrinterHelper.printPCL(pclContent);
            }

            final boolean finalResult = result;
            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    progressDialog.dismiss();
                    if (finalResult) {
                        Toast.makeText(getActivity(), "✅ PCL Print Successful", Toast.LENGTH_LONG).show();

                        refreshAllAdapters();

                    } else {
                        // Fallback to Android print if PCL fails
                        Toast.makeText(getActivity(), "PCL failed, trying Android print...", Toast.LENGTH_SHORT).show();
                        printUsingAndroidPrintFramework(entry);
                    }
                });
            }
        });
        executor.shutdown();
    }
    /**
     * Build PCL formatted ticket
     */
    private String buildPCLTicket(WeighmentEntry entry) {
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault());
        String dateTime = sdf.format(new Date());

        String grossStr = entry.getGross().isEmpty() ? "0" : entry.getGross();
        String tareStr = entry.getTare().isEmpty() ? "0" : entry.getTare();
        String manualTareStr = entry.getManualTare().isEmpty() ? "0" : entry.getManualTare();
        String netStr = entry.getNet().isEmpty() ? "0" : entry.getNet();

        StringBuilder pcl = new StringBuilder();

        // PCL commands
        pcl.append("\u001BE");                    // Reset printer
        pcl.append("\u001B&l1O");                  // Portrait orientation
        pcl.append("\u001B(s10H");                  // 10 pitch
        pcl.append("\u001B(s1Q");                   // Quality
        pcl.append("\u001B&l6D");                   // Vertical motion index
        pcl.append("\r\n\r\n");

        // Header
        pcl.append("\u001B(s3B");                   // Bold on
        pcl.append("                WEIGHMENT TICKET\r\n");
        pcl.append("\u001B(s0B");                   // Bold off
        pcl.append("\r\n");

        // Company details
        pcl.append("========================================\r\n");
        pcl.append("MY WEIGHBRIDGE COMPANY\r\n");
        pcl.append("123 Industrial Area, City - 123456\r\n");
        pcl.append("Phone: +91 9876543210\r\n");
        pcl.append("========================================\r\n");
        pcl.append("\r\n");

        // Ticket details
        pcl.append(String.format("Ticket #: %-15s Date: %s\r\n", entry.getSerialNo(), dateTime));
        pcl.append("----------------------------------------\r\n");

        // Vehicle details
        pcl.append("\u001B(s3B");                   // Bold on
        pcl.append("VEHICLE DETAILS:\r\n");
        pcl.append("\u001B(s0B");                   // Bold off
        pcl.append(String.format("Vehicle No: %-20s Type: %s\r\n",
                entry.getVehicleNo(), entry.getVehicleType()));
        pcl.append(String.format("Material: %-20s Party: %s\r\n",
                entry.getMaterial(), entry.getParty()));
        pcl.append(String.format("Charge: %s\r\n", entry.getCharge()));
        pcl.append("----------------------------------------\r\n");

        // Weight details
        pcl.append("\u001B(s3B");                   // Bold on
        pcl.append("WEIGHT DETAILS:\r\n");
        pcl.append("\u001B(s0B");                   // Bold off
        pcl.append("\r\n");
        pcl.append(String.format("  Gross Weight (kg)  : %10s\r\n", grossStr));
        pcl.append(String.format("  Tare Weight (kg)    : %10s\r\n", tareStr));
        if (!manualTareStr.equals("0")) {
            pcl.append(String.format("  Manual Tare (kg)    : %10s\r\n", manualTareStr));
        }
        pcl.append("  " + repeat("-", 40) + "\r\n");
        pcl.append("\u001B(s3B");                   // Bold on
        pcl.append(String.format("  NET WEIGHT (kg)     : %10s\r\n", netStr));
        pcl.append("\u001B(s0B");                   // Bold off
        pcl.append("  " + repeat("-", 40) + "\r\n");
        pcl.append("\r\n");

        // Signature area
        pcl.append("----------------------------------------\r\n");
        // pcl.append(String.format("Operator: %-20s\r\n", getOperatorName()));
        pcl.append("\r\n");
        pcl.append("Signature: __________________    Date: ___________\r\n");
        pcl.append("\r\n");

        // Footer
        pcl.append("\u001B(s3B");                   // Bold on
        pcl.append("           ***** THANK YOU *****\r\n");
        pcl.append("\u001B(s0B");                   // Bold off
        pcl.append("     *** This is computer generated ***\r\n");
        pcl.append("     *** No signature required ***\r\n");
        pcl.append("\r\n");
        pcl.append("\r\n");

        // Form feed
        pcl.append("\u001B&l0H");                   // Form feed

        return pcl.toString();
    }
    /**
     * Helper method to repeat strings
     */
    private String repeat(String str, int count) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < count; i++) {
            sb.append(str);
        }
        return sb.toString();
    }
    /**
     * Show retry dialog when print fails
     */
    private void showRetryDialog(WeighmentEntry entry, String error) {
        new AlertDialog.Builder(requireContext())
                .setTitle("Print Failed")
                .setMessage(error + "\n\nWould you like to try another method?")
                .setPositiveButton("Try PCL", (d, w) -> printWithPCL(entry))
                .setNeutralButton("Try Android Print", (d, w) -> printUsingAndroidPrintFramework(entry))
                .setNegativeButton("Cancel", null)
                .show();
    }

    /**
     * Print with USB Direct
     */
    private void printWithUSB(WeighmentEntry entry) {
        if (mainActivity == null || !mainActivity.isPrinterConnected()) {
            Toast.makeText(getActivity(), "Printer not connected", Toast.LENGTH_SHORT).show();
            return;
        }

        ProgressDialog progressDialog = new ProgressDialog(getActivity());
        progressDialog.setMessage("USB Direct Print...\nPlease wait");
        progressDialog.setCancelable(false);
        progressDialog.show();

        String textContent = buildPrintText(entry);

        PrinterManager printerManager = mainActivity.getPrinterManager();
        PrinterManager.PrinterConnectionListener listener = new PrinterManager.PrinterConnectionAdapter() {
            @Override
            public void onPrintSuccess() {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        progressDialog.dismiss();
                        Toast.makeText(getActivity(), "✅ USB Print Successful", Toast.LENGTH_LONG).show();

                        refreshAllAdapters();

                    });
                }
            }

            @Override
            public void onPrintError(String error) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        progressDialog.dismiss();
                        showRetryDialog(entry, error);
                    });
                }
            }
        };

        printerManager.autoDetectAndPrintAsync(textContent, listener);
    }

    /**
     * Print with Bluetooth
     */
    private void printWithBluetooth(WeighmentEntry entry) {
        // Similar to USB but for Bluetooth
        // You'll need to implement Bluetooth printing
        Toast.makeText(getActivity(), "Bluetooth printing not implemented", Toast.LENGTH_SHORT).show();
    }

    /**
     * Print with NokoPrint
     */
    private void printWithNokoPrint(WeighmentEntry entry) {
        String printContent = buildPrintText(entry);
        NokoPrintDirectPrinter nokoPrinter = new NokoPrintDirectPrinter(requireContext());

        // Create a text file
        try {
            File textFile = new File(requireContext().getCacheDir(),
                    "weighment_" + System.currentTimeMillis() + ".txt");
            FileOutputStream fos = new FileOutputStream(textFile);
            fos.write(printContent.getBytes());
            fos.close();

            nokoPrinter.printTextFile(textFile, "Weighment_Ticket.txt");
        } catch (Exception e) {
            Toast.makeText(getActivity(), "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Auto detect best method and print
     */
    private void autoDetectAndPrint(WeighmentEntry entry) {
        if (mainActivity != null && mainActivity.isPrinterConnected()) {
            // Try PCL first (best for HP)
            PrinterManager printerManager = mainActivity.getPrinterManager();
            if (printerManager.usbPrinterHelper != null) {
                // Check if it's likely an HP printer
                boolean isHP = false;
                if (printerManager.usbPrinterHelper.getCurrentDevice() != null) {
                    int vid = printerManager.usbPrinterHelper.getCurrentDevice().getVendorId();
                    isHP = (vid == 0x03F0 || vid == 0x03F1 || vid == 0x03F2 || vid == 0x03F3);
                }

                if (isHP) {
                    printWithPCL(entry);
                } else {
                    // For non-HP, try standard print
                    printUsingAndroidPrintFramework(entry);
                }
            } else {
                printUsingAndroidPrintFramework(entry);
            }
        } else {
            printUsingAndroidPrintFramework(entry);
        }
    }


    /**
     * Show print preview dialog
     */
    private void showPrintPreviewDialog(String printContent, WeighmentEntry entry) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle("Print Preview - Entry #" + entry.getSerialNo());

        TextView textView = new TextView(getActivity());
        textView.setText(printContent);
        textView.setTextSize(16);
        textView.setPadding(50, 30, 50, 30);
        textView.setTextColor(Color.BLACK);

        builder.setView(textView);
        builder.setPositiveButton("Print via Android", (dialog, which) -> {
            printUsingAndroidPrintFramework(entry);
        });
        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    /**
     * Print using Android's built-in print framework
     */
    private void printUsingAndroidPrintFramework(WeighmentEntry entry) {
        try {
            // Build HTML content for better formatting
            String htmlContent = buildPrintHtml(entry);

            // Create a print job
            PrintHelper printHelper = new PrintHelper(getActivity());
            printHelper.setScaleMode(PrintHelper.SCALE_MODE_FIT);

            // Create a bitmap from a TextView
            TextView printView = new TextView(getActivity());
            printView.setText(buildPrintText(entry));
            printView.setTextSize(12);
            printView.setPadding(50, 50, 50, 50);
            printView.setTextColor(Color.BLACK);
            printView.setBackgroundColor(Color.WHITE);

            // Print the view
            printHelper.printBitmap("weighment_" + entry.getSerialNo(),
                    loadBitmapFromView(printView));

            Toast.makeText(getActivity(), "Print job created for Entry #" + entry.getSerialNo(),
                    Toast.LENGTH_SHORT).show();

            // Update display
            if (txtDisplayTwoView != null) {
                SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss", Locale.getDefault());
                txtDisplayTwoView.setText("PRINT JOB: " + entry.getSerialNo() +
                        " | Net: " + entry.getNet() + " KG at " + sdf.format(new Date()));
            }

        } catch (Exception e) {
            Log.e("DFragment", "Error in Android printing", e);
            Toast.makeText(getActivity(), "Error creating print job", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Build HTML formatted print content
     */
    private String buildPrintHtml(WeighmentEntry entry) {
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault());
        String dateTime = sdf.format(new Date());

        return "<html><body style='font-family: monospace; padding: 20px;'>" +
                "<h2 style='text-align: center;'>WEIGHMENT TICKET</h2>" +
                "<hr>" +
                "<p><b>Slip No:</b> " + entry.getSerialNo() + "</p>" +
                "<p><b>Date/Time:</b> " + dateTime + "</p>" +
                "<hr>" +
                "<p><b>Vehicle No:</b> " + entry.getVehicleNo() + "</p>" +
                "<p><b>Vehicle Type:</b> " + entry.getVehicleType() + "</p>" +
                "<p><b>Material:</b> " + entry.getMaterial() + "</p>" +
                "<p><b>Party:</b> " + entry.getParty() + "</p>" +
                "<p><b>Charge:</b> " + entry.getCharge() + "</p>" +
                "<hr>" +
                "<p><b>Gross Weight:</b> " + entry.getGross() + " kg</p>" +
                "<p><b>Tare Weight:</b> " + entry.getTare() + " kg</p>" +
                (entry.getManualTare() != null && !entry.getManualTare().isEmpty() && !entry.getManualTare().equals("0") ?
                        "<p><b>Manual Tare:</b> " + entry.getManualTare() + " kg</p>" : "") +
                "<hr>" +
                "<h3>NET WEIGHT: " + entry.getNet() + " kg</h3>" +
                "<hr>" +
                "</body></html>";
    }

    /**
     * Build plain text print content
     */
    private String buildPrintText(WeighmentEntry entry) {
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault());
        String dateTime = sdf.format(new Date());

        return "================================\n" +
                "     WEIGHMENT TICKET\n" +
                "================================\n" +
                "Slip No: " + entry.getSerialNo() + "\n" +
                "Date/Time: " + dateTime + "\n" +
                "--------------------------------\n" +
                "Vehicle No: " + entry.getVehicleNo() + "\n" +
                "Vehicle Type: " + entry.getVehicleType() + "\n" +
                "Material: " + entry.getMaterial() + "\n" +
                "Party: " + entry.getParty() + "\n" +
                "Charge: " + entry.getCharge() + "\n" +
                "--------------------------------\n" +
                "Gross Weight: " + entry.getGross() + " kg\n" +
                "Tare Weight: " + entry.getTare() + " kg\n" +
                (entry.getManualTare() != null && !entry.getManualTare().isEmpty() && !entry.getManualTare().equals("0") ?
                        "Manual Tare: " + entry.getManualTare() + " kg\n" : "") +
                "--------------------------------\n" +
                "NET WEIGHT: " + entry.getNet() + " kg\n" +
                "================================";
    }

    /**
     * Convert a View to Bitmap for printing
     */
    private Bitmap loadBitmapFromView(View v) {
        v.measure(View.MeasureSpec.makeMeasureSpec(500, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED));
        v.layout(0, 0, v.getMeasuredWidth(), v.getMeasuredHeight());
        Bitmap b = Bitmap.createBitmap(v.getMeasuredWidth(), v.getMeasuredHeight(),
                Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(b);
        v.draw(c);
        return b;
    }

    private void loadEntry() {
        String searchSerial = searchSerialEditText.getText().toString().trim();
        Log.d("DFragment", "Loading entry: " + searchSerial);

        if (searchSerial.isEmpty()) {
            Toast.makeText(getActivity(), "Please enter serial number", Toast.LENGTH_SHORT).show();
            searchSerialEditText.requestFocus();
            return;
        }

        // Hide keyboard
        InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(searchSerialEditText.getWindowToken(), 0);

        WeighmentEntry entry = databaseHelper.getWeighmentBySerialNo(searchSerial);

        if (entry != null) {
            // Store current serial number
            currentSerialNo = searchSerial;

            // Populate form with entry data
            populateFormWithEntry(entry);

            // Compare and set gross/tare values
            compareAndSetGrossTare(entry);

            Toast.makeText(getActivity(), "Entry #" + searchSerial + " loaded", Toast.LENGTH_SHORT).show();

            // Update display
            if (txtDisplayTwoView != null) {
                txtDisplayTwoView.setText("Loaded: " + searchSerial + " | Net: " + entry.getNet() + " KG");
            }

            // Focus on Print button
            button5a.requestFocus();

        } else {
            Toast.makeText(getActivity(), "No entry found with Serial #" + searchSerial, Toast.LENGTH_SHORT).show();
            clearFormFields();
            currentSerialNo = "";
        }
    }

    private void compareAndSetGrossTare(WeighmentEntry entry) {
        try {
            long gross = 0;
            long tare = 0;

            if (entry.getGross() != null && !entry.getGross().isEmpty()) {
                gross = Long.parseLong(entry.getGross());
            }
            if (entry.getTare() != null && !entry.getTare().isEmpty()) {
                tare = Long.parseLong(entry.getTare());
            }
            if (entry.getManualTare() != null && !entry.getManualTare().isEmpty()) {
                long manualTare = Long.parseLong(entry.getManualTare());
                if (manualTare > tare) {
                    tare = manualTare;
                }
            }

            // Always put greater value in gross
            if (tare > gross) {
                grossEditText.setText(String.valueOf(tare));
                tareEditText.setText(String.valueOf(gross));
            } else {
                grossEditText.setText(String.valueOf(gross));
                tareEditText.setText(String.valueOf(tare));
            }

            calculateNetWeight();

        } catch (NumberFormatException e) {
            Log.e("DFragment", "Error parsing numbers", e);
        }
    }

    private void populateFormWithEntry(WeighmentEntry entry) {
        serialEditText.setText(entry.getSerialNo());
        vehicleNoSpinner.setText(entry.getVehicleNo());
        vehicleTypeSpinner.setText(entry.getVehicleType());
        materialSpinner.setText(entry.getMaterial());
        partySpinner.setText(entry.getParty());
        chargeEditText.setText(entry.getCharge());
    }

    private void setupCalculationListeners() {
        TextWatcher calculationWatcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                calculateNetWeight();
            }
        };

        grossEditText.addTextChangedListener(calculationWatcher);
        tareEditText.addTextChangedListener(calculationWatcher);
    }

    private void calculateNetWeight() {
        String grossStr = grossEditText.getText().toString().trim();
        String tareStr = tareEditText.getText().toString().trim();

        if (!grossStr.isEmpty() && !tareStr.isEmpty()) {
            try {
                long gross = Long.parseLong(grossStr);
                long tare = Long.parseLong(tareStr);
                long net = gross - tare;

                if (net < 0) {
                    net = 0;
                }

                netWeightEditText.setText(String.valueOf(net));
            } catch (NumberFormatException e) {
                Log.e("DFragment", "Error calculating net weight", e);
            }
        }
    }

    /**
     * Setup T and G buttons click functionality (hidden but kept for compatibility)
     */
    private void setupTGMButtons() {
        // T and G buttons are hidden, but we keep the listeners for compatibility
        buttonT.setOnClickListener(v -> {
            // Do nothing - buttons are hidden
        });

        buttonG.setOnClickListener(v -> {
            // Do nothing - buttons are hidden
        });
    }

    private void setupButtonFocusListeners() {
        if (button5a != null) {
            button5a.setOnFocusChangeListener((v, hasFocus) -> {
                if (hasFocus) {
                    button5a.setBackgroundColor(Color.parseColor("#00FF00"));
                    button5a.setTextColor(Color.WHITE);
                } else {
                    button5a.setBackgroundColor(Color.parseColor("#808080"));
                    button5a.setTextColor(Color.BLACK);
                }
            });
        }

        searchSerialEditText.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus) {
                String serial = searchSerialEditText.getText().toString().trim();
                if (!serial.isEmpty()) {
                    loadEntry();
                }
            }
        });
    }

    private void clearFormFields() {
        serialEditText.setText("");
        vehicleNoSpinner.setText("");
        vehicleTypeSpinner.setText("");
        materialSpinner.setText("");
        partySpinner.setText("");
        chargeEditText.setText("");
        grossEditText.setText("");
        tareEditText.setText("");
        netWeightEditText.setText("");

        if (txtDisplayTwoView != null) {
            txtDisplayTwoView.setText("");
        }
    }

    // Public methods for MainActivity (kept for compatibility)
    public void performTButtonAction() {
        // Do nothing - view-only mode
    }

    public void performGButtonAction() {
        // Do nothing - view-only mode
    }

    public void performGButtonActionClear() {
        // Do nothing - view-only mode
    }

    public boolean isFragmentVisible() {
        return isAdded() && isVisible();
    }

    @Override
    public void onResume() {
        super.onResume();
        searchSerialEditText.requestFocus();
        refreshAllAdapters();
        setFormFieldsNonEditable();

        // Check printer status from MainActivity
        if (mainActivity != null) {
            updatePrinterStatus(mainActivity.isPrinterConnected(), mainActivity.getConnectedPrinterName());
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        // No need to unregister receiver here as it's managed by MainActivity
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // No need to disconnect printer here as it's managed by MainActivity
    }
}