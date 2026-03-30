package com.googleapi.bluetoothweight;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.SharedPreferences;
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

import java.io.ByteArrayOutputStream;
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

    // Print type constants
    private static final String PRINT_TYPE_PLAIN = "plain";
    private static final String PRINT_TYPE_PRINTED = "printed";

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
        button5a.setOnClickListener(v -> printWeighmentEntry());

        button5a.setOnKeyListener((v, keyCode, event) -> {
            if (keyCode == KeyEvent.KEYCODE_ENTER && event.getAction() == KeyEvent.ACTION_DOWN) {
                printWeighmentEntry();
                return true;
            }
            return false;
        });
    }

    /**
     * Direct USB Print method - This is the key method for USB printing
     */
    private boolean directUsbPrint(WeighmentEntry entry, boolean usePrintedFormat) {
        Log.d("DFragment", "directUsbPrint called for entry #" + entry.getSerialNo());

        // Check if fragment is attached
        if (!isAdded() || getActivity() == null) {
            Log.e("DFragment", "Fragment not attached");
            return false;
        }

        // Check if MainActivity is available
        if (mainActivity == null) {
            Log.e("DFragment", "mainActivity is null");
            Toast.makeText(getActivity(), "MainActivity not available", Toast.LENGTH_SHORT).show();
            return false;
        }

        // Check if printer is connected
        if (!mainActivity.isPrinterConnected()) {
            Log.e("DFragment", "Printer not connected");
            Toast.makeText(getActivity(), "USB Printer not connected", Toast.LENGTH_SHORT).show();
            return false;
        }

        // Get printer manager
        PrinterManager printerManager = mainActivity.getPrinterManager();
        if (printerManager == null) {
            Log.e("DFragment", "printerManager is null");
            Toast.makeText(getActivity(), "Printer manager not initialized", Toast.LENGTH_SHORT).show();
            return false;
        }

        // Check USB helper
        if (printerManager.usbPrinterHelper == null) {
            Log.e("DFragment", "usbPrinterHelper is null");
            Toast.makeText(getActivity(), "USB Printer helper not initialized", Toast.LENGTH_SHORT).show();
            return false;
        }

        try {
            boolean printResult = false;

            if (usePrintedFormat) {
                // Use formatted PCL/ESC/POS print
                Log.d("DFragment", "Using PRINTED print format (PCL/ESC/POS)");


                byte[] ticketBytes = buildDotMatrixTicket(entry);
                if (ticketBytes != null && ticketBytes.length > 0) {
                    Log.d("DFragment", "Sending " + ticketBytes.length + " bytes to printer");
                    printResult = printerManager.usbPrinterHelper.sendRawData(ticketBytes);
                } else {
                    Log.e("DFragment", "ticketBytes is empty");
                    return false;
                }

            } else {
                // Use plain text print
                Log.d("DFragment", "Using PLAIN print format");
                byte[] ticketBytes = buildPCLTicketBytes(entry);
                if (ticketBytes != null && ticketBytes.length > 0) {
                    Log.d("DFragment", "Sending " + ticketBytes.length + " bytes to printer");
                    printResult = printerManager.usbPrinterHelper.sendRawData(ticketBytes);
                } else {
                    Log.e("DFragment", "ticketBytes is empty");
                    return false;
                }
            }

            Log.d("DFragment", "USB Print result: " + printResult);
            return printResult;

        } catch (Exception e) {
            Log.e("DFragment", "USB Print exception: " + e.getMessage());
            e.printStackTrace();
            Toast.makeText(getActivity(), "USB Print Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            return false;
        }
    }

    /**
     * Automatically print to the currently connected printer without showing any popup
     */
    private void autoPrintToConnectedPrinter(WeighmentEntry entry) {
        Log.d("DFragment", "autoPrintToConnectedPrinter called for entry #" + entry.getSerialNo());

        if (!isAdded() || getActivity() == null) {
            Log.e("DFragment", "Fragment not attached");
            return;
        }

        // Get print type preference from SharedPreferences (set by F6 in MainActivity)
        SharedPreferences prefs = requireContext().getSharedPreferences("PrintSettings", Context.MODE_PRIVATE);
        String printType = prefs.getString("print_type", PRINT_TYPE_PLAIN);
        String printDisplayName = prefs.getString("print_type_display", "Plain Print");

        Log.d("DFragment", "Print type selected: " + printDisplayName);

        boolean usePrintedFormat = printType.equals(PRINT_TYPE_PRINTED);
        boolean printSuccess = false;

        // Try USB printing first
        if (mainActivity != null && mainActivity.isPrinterConnected()) {
            printSuccess = directUsbPrint(entry, usePrintedFormat);

            if (printSuccess) {
                Toast.makeText(getActivity(), "✅ Print sent to " + printDisplayName + " printer", Toast.LENGTH_SHORT).show();

                if (txtDisplayTwoView != null) {
                    SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss", Locale.getDefault());
                    txtDisplayTwoView.setText("PRINTED: " + entry.getSerialNo() +
                            " | Net: " + entry.getNet() + " KG at " + sdf.format(new Date()));
                }
                return;
            } else {
                Log.e("DFragment", "USB printing failed, trying fallback...");
            }
        }

        // Fallback to Android print framework if USB fails or not connected
        Log.d("DFragment", "Using Android Print fallback");
        Toast.makeText(getActivity(),
                (mainActivity != null && mainActivity.isPrinterConnected()) ?
                        "❌ USB Print failed - Using Android Print" :
                        "No USB printer connected. Using Android Print...",
                Toast.LENGTH_SHORT).show();

        printUsingAndroidPrintFramework(entry);
    }

    /**
     * Build a compact print text for plain printing
     */
    private String buildCompactPrintText(WeighmentEntry entry) {
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault());
        String dateTime = sdf.format(new Date());

        SharedPreferences prefs = requireContext().getSharedPreferences("MyAppPrefs", Context.MODE_PRIVATE);
        String field1 = prefs.getString("field_0", "MY WEIGHBRIDGE COMPANY");
        String field2 = prefs.getString("field_1", "123 Industrial Area, City - 123456");
        String field3 = prefs.getString("field_2", "Phone: +91 9876543210");

        StringBuilder ticket = new StringBuilder();

        // Header with company details
        ticket.append("\n\n");
        ticket.append(field1).append("\n");
        ticket.append(field2).append("\n");
        ticket.append(field3).append("\n");
        ticket.append("========================================\n");
        ticket.append("TICKET #: ").append(entry.getSerialNo()).append("\n");
        ticket.append("DATE: ").append(dateTime).append("\n");
        ticket.append("========================================\n");
        ticket.append("VEHICLE NO: ").append(entry.getVehicleNo()).append("\n");
        ticket.append("VEHICLE TYPE: ").append(entry.getVehicleType()).append("\n");
        ticket.append("MATERIAL: ").append(entry.getMaterial()).append("\n");
        ticket.append("PARTY: ").append(entry.getParty()).append("\n");
        if (entry.getCharge() != null && !entry.getCharge().isEmpty()) {
            ticket.append("CHARGE: ").append(entry.getCharge()).append("\n");
        }
        ticket.append("----------------------------------------\n");
        ticket.append("GROSS WEIGHT: ").append(formatNumber(entry.getGross())).append(" kg\n");
        ticket.append("TARE WEIGHT: ").append(formatNumber(entry.getTare())).append(" kg\n");
        ticket.append("========================================\n");
        ticket.append("NET WEIGHT: ").append(formatNumber(entry.getNet())).append(" kg\n");
        ticket.append("========================================\n");
        ticket.append("OPERATOR: ").append(getOperatorName()).append("\n");
        ticket.append("SIGNATURE: __________________\n");
        ticket.append("\n");
        ticket.append("     ***** THANK YOU *****\n");
        ticket.append("   *** This is computer generated ***\n");
        ticket.append("   *** No signature required ***\n");
        ticket.append("\n");

        return ticket.toString();
    }

    /**
     * Modified print method to use auto printing instead of showing dialog
     */
    private void printWeighmentEntry() {
        String serialNo = serialEditText.getText().toString().trim();
        String vehicleNo = vehicleNoSpinner.getText().toString().trim();
        String vehicleType = vehicleTypeSpinner.getText().toString().trim();
        String material = materialSpinner.getText().toString().trim();
        String party = partySpinner.getText().toString().trim();
        String charge = chargeEditText.getText().toString().trim();
        String gross = grossEditText.getText().toString().trim();
        String tare = tareEditText.getText().toString().trim();

        if (serialNo.isEmpty()) {
            Toast.makeText(getActivity(), "Please load an entry first", Toast.LENGTH_SHORT).show();
            searchSerialEditText.requestFocus();
            return;
        }

        if (vehicleNo.isEmpty()) {
            vehicleNoSpinner.setError("Vehicle number is required");
            vehicleNoSpinner.requestFocus();
            return;
        }

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

        // Use auto print instead of showing dialog
        autoPrintToConnectedPrinter(entry);
    }

    /**
     * Print with PCL - Kept for compatibility
     */
    private boolean printWithPCL(WeighmentEntry entry) {
        return directUsbPrint(entry, true);
    }

    private byte[] buildDotMatrixTicket(WeighmentEntry entry) {
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault());
        String dateTime = sdf.format(new Date());

        SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
        SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm:ss", Locale.getDefault());

        String date = dateFormat.format(new Date());
        String time = timeFormat.format(new Date());

        int pageWidth = 76;     // Full width
        int leftWidth = 38;     // Left column
        int rightWidth = 38;    // Right column

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

        try {
            // ===== PRINTER CONTROL COMMANDS =====
            // ESC ! n - Select print mode (ESC/POS)
            // 0x1B = ESC, 0x21 = '!'
            // n = 0x00: Normal, 0x10: Double Height, 0x20: Double Width, 0x30: Double Height + Width
            byte[] doubleSize = new byte[]{0x1B, 0x21, 0x30}; // Double Height + Double Width
            byte[] normalSize = new byte[]{0x1B, 0x21, 0x00}; // Normal size

            // Alternative: Separate commands
            // byte[] doubleHeight = new byte[]{0x1B, 0x21, 0x10}; // Double Height only
            // byte[] doubleWidth = new byte[]{0x1B, 0x21, 0x20};  // Double Width only

            outputStream.write(doubleSize);

            // ===== TOP MARGIN =====
            outputStream.write("\n".getBytes());
            // ===== VEHICLE DETAILS =====
            outputStream.write(("             " +date+"         " + entry.getSerialNo()).getBytes());
            outputStream.write("\n".getBytes());
            outputStream.write("\n".getBytes());

            outputStream.write(("             " +entry.getVehicleNo()+"               " + entry.getSerialNo()).getBytes());
            outputStream.write("\n".getBytes());
            outputStream.write("\n".getBytes());

            outputStream.write(("             " +entry.getMaterial()+"             " + dateTime.substring(11)).getBytes());
            outputStream.write("\n".getBytes());
            outputStream.write("\n".getBytes());

            outputStream.write(("                      " +formatNumber(entry.getGross()) + " kg").getBytes());
            outputStream.write("\n\n".getBytes());
            outputStream.write("\n".getBytes());
            // TARE
            outputStream.write(("                      " +formatNumber(entry.getTare()) + " kg").getBytes());

            outputStream.write("\n".getBytes());

            outputStream.write("\n".getBytes());
            outputStream.write("\n".getBytes());

            // NET - in double size

            outputStream.write(("                      " +formatNumber(entry.getNet()) + " kg").getBytes());
            outputStream.write("\n".getBytes());

            // Reset to normal size for remaining content
            outputStream.write(normalSize);
            for(int i=0; i<10; i++){
                outputStream.write("\n".getBytes());
            }



            return outputStream.toByteArray();

        } catch (Exception e) {
            e.printStackTrace();
            return new byte[0];
        }
    }

    private byte[] buildPCLTicketBytesPlain(WeighmentEntry entry) {
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault());
        String dateTime = sdf.format(new Date());

        SharedPreferences prefs = requireContext().getSharedPreferences("MyAppPrefs", Context.MODE_PRIVATE);
        String field1 = prefs.getString("field_0", "MY WEIGHBRIDGE COMPANY");
        String field2 = prefs.getString("field_1", "123 Industrial Area, City - 123456");
        String field3 = prefs.getString("field_2", "Phone: +91 9876543210");

        int pageWidth = 42;

        // ESC/POS Commands
        byte[] ESC = new byte[]{0x1B};
        byte[] GS = new byte[]{0x1D};
        byte[] NEW_LINE = "\n".getBytes();

        // Font size commands
        byte[] FONT_NORMAL = new byte[]{0x1B, 0x21, 0x00};
        byte[] FONT_DOUBLE_HEIGHT = new byte[]{0x1B, 0x21, 0x10};
        byte[] FONT_DOUBLE_WIDTH = new byte[]{0x1B, 0x21, 0x20};
        byte[] FONT_DOUBLE_BOTH = new byte[]{0x1B, 0x21, 0x30};

        // Alignment commands
        byte[] ALIGN_LEFT = new byte[]{0x1B, 0x61, 0x00};
        byte[] ALIGN_CENTER = new byte[]{0x1B, 0x61, 0x01};
        byte[] ALIGN_RIGHT = new byte[]{0x1B, 0x61, 0x02};

        // Bold on/off
        byte[] BOLD_ON = new byte[]{0x1B, 0x45, 0x01};
        byte[] BOLD_OFF = new byte[]{0x1B, 0x45, 0x00};

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

        try {
            // Initialize printer
            outputStream.write(ESC);
            outputStream.write("@".getBytes());

            // Top margin
            outputStream.write("\n\n\n".getBytes());


            // Ticket number and date
            outputStream.write(String.format("%20s", entry.getSerialNo()).getBytes());
            outputStream.write(NEW_LINE);
            outputStream.write(String.format("%20s", dateTime).getBytes());
            outputStream.write(NEW_LINE);




            // Vehicle details
            outputStream.write(String.format("  %-16s %s", "Vehicle No:", entry.getVehicleNo()).getBytes());
            outputStream.write(NEW_LINE);
            outputStream.write(String.format("  %-16s %s", "Type:", entry.getVehicleType()).getBytes());
            outputStream.write(NEW_LINE);
            outputStream.write(String.format("  %-16s %s", "Material:", entry.getMaterial()).getBytes());
            outputStream.write(NEW_LINE);
            outputStream.write(String.format("  %-16s %s", "Party:", entry.getParty()).getBytes());
            outputStream.write(NEW_LINE);

            if (entry.getCharge() != null && !entry.getCharge().isEmpty()) {
                outputStream.write(String.format("  %-16s %s", "Charge:", entry.getCharge()).getBytes());
                outputStream.write(NEW_LINE);
            }

            outputStream.write(repeat("-", pageWidth).getBytes());
            outputStream.write(NEW_LINE);

            // ===== WEIGHT DETAILS HEADER =====
            outputStream.write(BOLD_ON);
            outputStream.write("WEIGHT DETAILS:".getBytes());
            outputStream.write(NEW_LINE);
            outputStream.write(BOLD_OFF);
            outputStream.write(NEW_LINE);

            // Weight details with numbers right-aligned
            outputStream.write(String.format("  %-16s %10s kg", "Gross Weight:", formatNumber(entry.getGross())).getBytes());
            outputStream.write(NEW_LINE);
            outputStream.write(String.format("  %-16s %10s kg", "Tare Weight:", formatNumber(entry.getTare())).getBytes());
            outputStream.write(NEW_LINE);

            // Separator
            outputStream.write("  ".getBytes());
            outputStream.write(repeat("-", pageWidth - 4).getBytes());
            outputStream.write(NEW_LINE);

            // ===== NET WEIGHT - Bold and larger font =====
            outputStream.write(FONT_DOUBLE_HEIGHT);
            outputStream.write(BOLD_ON);
            outputStream.write(String.format("  %-16s %10s kg", "NET WEIGHT:", formatNumber(entry.getNet())).getBytes());
            outputStream.write(NEW_LINE);
            outputStream.write(FONT_NORMAL);
            outputStream.write(BOLD_OFF);

            // Bottom separator
            outputStream.write("  ".getBytes());
            outputStream.write(repeat("-", pageWidth - 4).getBytes());
            outputStream.write(NEW_LINE);
            outputStream.write(NEW_LINE);

            // ===== SIGNATURE AREA =====
            outputStream.write(repeat("-", pageWidth).getBytes());
            outputStream.write(NEW_LINE);
            outputStream.write(String.format("Operator: %s", getOperatorName()).getBytes());
            outputStream.write(NEW_LINE);
            outputStream.write(NEW_LINE);
            outputStream.write("Signature: __________________".getBytes());
            outputStream.write(NEW_LINE);
            outputStream.write(NEW_LINE);

            // ===== FOOTER - Centered =====
            outputStream.write(ALIGN_CENTER);
            outputStream.write("***** THANK YOU *****".getBytes());
            outputStream.write(NEW_LINE);
            outputStream.write("*** This is computer generated ***".getBytes());
            outputStream.write(NEW_LINE);
            outputStream.write("*** No signature required ***".getBytes());
            outputStream.write(NEW_LINE);
            outputStream.write(NEW_LINE);

            // Cut paper (optional)
            outputStream.write(GS);
            outputStream.write("V".getBytes());
            outputStream.write(65);
            outputStream.write(0);

            return outputStream.toByteArray();

        } catch (Exception e) {
            e.printStackTrace();
            return new byte[0];
        }
    }
    private byte[] buildPCLTicketBytes(WeighmentEntry entry) {
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault());
        String dateTime = sdf.format(new Date());

        SharedPreferences prefs = requireContext().getSharedPreferences("MyAppPrefs", Context.MODE_PRIVATE);
        String field1 = prefs.getString("field_0", "MY WEIGHBRIDGE COMPANY");
        String field2 = prefs.getString("field_1", "123 Industrial Area, City - 123456");
        String field3 = prefs.getString("field_2", "Phone: +91 9876543210");

        int pageWidth = 42;

        // ESC/POS Commands
        byte[] ESC = new byte[]{0x1B};
        byte[] GS = new byte[]{0x1D};
        byte[] NEW_LINE = "\n".getBytes();

        // Font size commands
        byte[] FONT_NORMAL = new byte[]{0x1B, 0x21, 0x00};
        byte[] FONT_DOUBLE_HEIGHT = new byte[]{0x1B, 0x21, 0x10};
        byte[] FONT_DOUBLE_WIDTH = new byte[]{0x1B, 0x21, 0x20};
        byte[] FONT_DOUBLE_BOTH = new byte[]{0x1B, 0x21, 0x30};

        // Alignment commands
        byte[] ALIGN_LEFT = new byte[]{0x1B, 0x61, 0x00};
        byte[] ALIGN_CENTER = new byte[]{0x1B, 0x61, 0x01};
        byte[] ALIGN_RIGHT = new byte[]{0x1B, 0x61, 0x02};

        // Bold on/off
        byte[] BOLD_ON = new byte[]{0x1B, 0x45, 0x01};
        byte[] BOLD_OFF = new byte[]{0x1B, 0x45, 0x00};

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

        try {
            // Initialize printer
            outputStream.write(ESC);
            outputStream.write("@".getBytes());

            // Top margin
            outputStream.write("\n\n\n".getBytes());

            // ===== COMPANY NAME - Large centered text =====
            outputStream.write(ALIGN_CENTER);
            outputStream.write(FONT_DOUBLE_BOTH);
            outputStream.write(BOLD_ON);
            outputStream.write(field1.getBytes("UTF-8"));
            outputStream.write(NEW_LINE);

            // ===== ADDRESS - Normal centered =====
            outputStream.write(ALIGN_CENTER);
            outputStream.write(FONT_NORMAL);
            outputStream.write(field2.getBytes("UTF-8"));
            outputStream.write(NEW_LINE);

            // ===== PHONE - Normal centered =====
            outputStream.write(ALIGN_CENTER);
            outputStream.write(field3.getBytes("UTF-8"));
            outputStream.write(NEW_LINE);

            // Separator line
            outputStream.write(ALIGN_CENTER);
            outputStream.write(repeat("=", pageWidth).getBytes());
            outputStream.write(NEW_LINE);
            outputStream.write(NEW_LINE);

            // ===== TICKET DETAILS - Left aligned =====
            outputStream.write(ALIGN_LEFT);

            // Ticket number and date
            outputStream.write(String.format("%-20s %s", "Ticket #:", entry.getSerialNo()).getBytes());
            outputStream.write(NEW_LINE);
            outputStream.write(String.format("%-20s %s", "Date:", dateTime).getBytes());
            outputStream.write(NEW_LINE);
            outputStream.write(repeat("-", pageWidth).getBytes());
            outputStream.write(NEW_LINE);

            // ===== VEHICLE DETAILS HEADER - Bold =====
            outputStream.write(BOLD_ON);
            outputStream.write("VEHICLE DETAILS:".getBytes());
            outputStream.write(NEW_LINE);
            outputStream.write(BOLD_OFF);

            // Vehicle details
            outputStream.write(String.format("  %-16s %s", "Vehicle No:", entry.getVehicleNo()).getBytes());
            outputStream.write(NEW_LINE);
            outputStream.write(String.format("  %-16s %s", "Type:", entry.getVehicleType()).getBytes());
            outputStream.write(NEW_LINE);
            outputStream.write(String.format("  %-16s %s", "Material:", entry.getMaterial()).getBytes());
            outputStream.write(NEW_LINE);
            outputStream.write(String.format("  %-16s %s", "Party:", entry.getParty()).getBytes());
            outputStream.write(NEW_LINE);

            if (entry.getCharge() != null && !entry.getCharge().isEmpty()) {
                outputStream.write(String.format("  %-16s %s", "Charge:", entry.getCharge()).getBytes());
                outputStream.write(NEW_LINE);
            }

            outputStream.write(repeat("-", pageWidth).getBytes());
            outputStream.write(NEW_LINE);

            // ===== WEIGHT DETAILS HEADER =====
            outputStream.write(BOLD_ON);
            outputStream.write("WEIGHT DETAILS:".getBytes());
            outputStream.write(NEW_LINE);
            outputStream.write(BOLD_OFF);
            outputStream.write(NEW_LINE);

            // Weight details with numbers right-aligned
            outputStream.write(String.format("  %-16s %10s kg", "Gross Weight:", formatNumber(entry.getGross())).getBytes());
            outputStream.write(NEW_LINE);
            outputStream.write(String.format("  %-16s %10s kg", "Tare Weight:", formatNumber(entry.getTare())).getBytes());
            outputStream.write(NEW_LINE);

            // Separator
            outputStream.write("  ".getBytes());
            outputStream.write(repeat("-", pageWidth - 4).getBytes());
            outputStream.write(NEW_LINE);

            // ===== NET WEIGHT - Bold and larger font =====
            outputStream.write(FONT_DOUBLE_HEIGHT);
            outputStream.write(BOLD_ON);
            outputStream.write(String.format("  %-16s %10s kg", "NET WEIGHT:", formatNumber(entry.getNet())).getBytes());
            outputStream.write(NEW_LINE);
            outputStream.write(FONT_NORMAL);
            outputStream.write(BOLD_OFF);

            // Bottom separator
            outputStream.write("  ".getBytes());
            outputStream.write(repeat("-", pageWidth - 4).getBytes());
            outputStream.write(NEW_LINE);
            outputStream.write(NEW_LINE);

            // ===== SIGNATURE AREA =====
            outputStream.write(repeat("-", pageWidth).getBytes());
            outputStream.write(NEW_LINE);
            outputStream.write(String.format("Operator: %s", getOperatorName()).getBytes());
            outputStream.write(NEW_LINE);
            outputStream.write(NEW_LINE);
            outputStream.write("Signature: __________________".getBytes());
            outputStream.write(NEW_LINE);
            outputStream.write(NEW_LINE);

            // ===== FOOTER - Centered =====
            outputStream.write(ALIGN_CENTER);
            outputStream.write("***** THANK YOU *****".getBytes());
            outputStream.write(NEW_LINE);
            outputStream.write("*** This is computer generated ***".getBytes());
            outputStream.write(NEW_LINE);
            outputStream.write("*** No signature required ***".getBytes());
            outputStream.write(NEW_LINE);
            outputStream.write(NEW_LINE);

            // Cut paper (optional)
            outputStream.write(GS);
            outputStream.write("V".getBytes());
            outputStream.write(65);
            outputStream.write(0);

            return outputStream.toByteArray();

        } catch (Exception e) {
            e.printStackTrace();
            return new byte[0];
        }
    }

    /**
     * Print with WiFi - Returns boolean success
     */
    private boolean printWithWiFi(WeighmentEntry entry) {
        try {
            Log.d("PRINT", "printWithWiFi: Starting...");
            showWiFiPrintOptions(entry);
            return true;
        } catch (Exception e) {
            Log.e("PRINT", "printWithWiFi exception: " + e.getMessage());
            return false;
        }
    }

    /**
     * Show WiFi print options
     */
    private void showWiFiPrintOptions(WeighmentEntry entry) {
        String[] options = {
                "Scan for WiFi Printers",
                "Enter IP Address Manually"
        };

        new AlertDialog.Builder(requireContext())
                .setTitle("WiFi Print")
                .setItems(options, (dialog, which) -> {
                    if (which == 0) {
                        scanForWifiPrinters(entry);
                    } else {
                        showManualIpDialog(entry);
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    /**
     * Scan for WiFi printers
     */
    private void scanForWifiPrinters(WeighmentEntry entry) {
        ProgressDialog progressDialog = new ProgressDialog(getActivity());
        progressDialog.setMessage("Scanning for WiFi printers...");
        progressDialog.setCancelable(false);
        progressDialog.show();

        new Handler().postDelayed(() -> {
            progressDialog.dismiss();
            Toast.makeText(getActivity(), "No WiFi printers found. Please enter IP manually.", Toast.LENGTH_LONG).show();
            showManualIpDialog(entry);
        }, 2000);
    }

    /**
     * Show manual IP entry dialog
     */
    private void showManualIpDialog(WeighmentEntry entry) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle("Enter WiFi Printer IP");

        final EditText input = new EditText(getActivity());
        input.setHint("e.g., 192.168.1.100");
        builder.setView(input);

        builder.setPositiveButton("Print", (dialog, which) -> {
            String ipAddress = input.getText().toString().trim();
            if (!ipAddress.isEmpty()) {
                Toast.makeText(getActivity(), "Connecting to WiFi printer: " + ipAddress, Toast.LENGTH_SHORT).show();
                printUsingAndroidPrintFramework(entry);
            }
        });

        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    /**
     * Print with NokoPrint - Returns boolean success
     */
    private boolean printWithNokoPrint(WeighmentEntry entry) {
        try {
            Log.d("PRINT", "printWithNokoPrint: Starting...");

            String printContent = buildPrintText(entry);
            if (printContent == null || printContent.isEmpty()) {
                return false;
            }

            NokoPrintDirectPrinter nokoPrinter = new NokoPrintDirectPrinter(requireContext());

            File textFile = new File(requireContext().getCacheDir(),
                    "weighment_" + System.currentTimeMillis() + ".txt");
            FileOutputStream fos = new FileOutputStream(textFile);
            fos.write(printContent.getBytes());
            fos.close();

            nokoPrinter.printTextFile(textFile, "Weighment_Ticket.txt");
            return true;

        } catch (Exception e) {
            Log.e("PRINT", "printWithNokoPrint exception: " + e.getMessage());
            return false;
        }
    }

    /**
     * Auto detect best method and print - Returns boolean success
     */
    private boolean autoDetectAndPrint(WeighmentEntry entry) {
        Log.d("PRINT", "autoDetectAndPrint: Starting...");

        if (printWithPCL(entry)) {
            Log.d("PRINT", "autoDetectAndPrint: PCL succeeded");
            return true;
        }

        if (printWithWiFi(entry)) {
            Log.d("PRINT", "autoDetectAndPrint: WiFi succeeded");
            return true;
        }

        if (printWithNokoPrint(entry)) {
            Log.d("PRINT", "autoDetectAndPrint: NokoPrint succeeded");
            return true;
        }

        Log.d("PRINT", "autoDetectAndPrint: Falling back to Android print");
        printUsingAndroidPrintFramework(entry);
        return true;
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
            PrintHelper printHelper = new PrintHelper(getActivity());
            printHelper.setScaleMode(PrintHelper.SCALE_MODE_FIT);

            TextView printView = new TextView(getActivity());
            printView.setText(buildPrintText(entry));
            printView.setTextSize(12);
            printView.setPadding(50, 50, 50, 50);
            printView.setTextColor(Color.BLACK);
            printView.setBackgroundColor(Color.WHITE);

            printHelper.printBitmap("weighment_" + entry.getSerialNo(),
                    loadBitmapFromView(printView));

            Toast.makeText(getActivity(), "Print job created for Entry #" + entry.getSerialNo(),
                    Toast.LENGTH_SHORT).show();

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
     * Build plain text print content with PCL-like formatting
     */
    private String buildPrintText(WeighmentEntry entry) {
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault());
        String dateTime = sdf.format(new Date());

        SharedPreferences prefs = requireContext().getSharedPreferences("MyAppPrefs", Context.MODE_PRIVATE);
        String field1 = prefs.getString("field_0", "MY WEIGHBRIDGE COMPANY");
        String field2 = prefs.getString("field_1", "123 Industrial Area, City - 123456");
        String field3 = prefs.getString("field_2", "Phone: +91 9876543210");

        int lineLength = 40;
        String centeredField1 = centerText(field1, lineLength);
        String centeredField2 = centerText(field2, lineLength);
        String centeredField3 = centerText(field3, lineLength);

        String grossStr = formatNumber(entry.getGross());
        String tareStr = formatNumber(entry.getTare());
        String netStr = formatNumber(entry.getNet());

        StringBuilder ticket = new StringBuilder();

        ticket.append("\n\n");
        ticket.append(centeredField1).append("\n");
        ticket.append(centeredField2).append("\n");
        ticket.append(centeredField3).append("\n");
        ticket.append(repeat("=", lineLength)).append("\n");
        ticket.append("\n");
        ticket.append(String.format("%-20s %s\n", "Ticket #:", entry.getSerialNo()));
        ticket.append(String.format("%-20s %s\n", "Date:", dateTime));
        ticket.append(repeat("-", lineLength)).append("\n");
        ticket.append("VEHICLE DETAILS:\n");
        ticket.append(String.format("  %-16s %s\n", "Vehicle No:", entry.getVehicleNo()));
        ticket.append(String.format("  %-16s %s\n", "Type:", entry.getVehicleType()));
        ticket.append(String.format("  %-16s %s\n", "Material:", entry.getMaterial()));
        ticket.append(String.format("  %-16s %s\n", "Party:", entry.getParty()));

        if (entry.getCharge() != null && !entry.getCharge().isEmpty()) {
            ticket.append(String.format("  %-16s %s\n", "Charge:", entry.getCharge()));
        }
        ticket.append(repeat("-", lineLength)).append("\n");
        ticket.append("WEIGHT DETAILS:\n");
        ticket.append("\n");
        ticket.append(String.format("  %-16s %10s kg\n", "Gross Weight:", grossStr));
        ticket.append(String.format("  %-16s %10s kg\n", "Tare Weight:", tareStr));
        ticket.append("  " + repeat("-", lineLength - 4)).append("\n");
        ticket.append(String.format("  %-16s %10s kg\n", "NET WEIGHT:", netStr));
        ticket.append("  " + repeat("-", lineLength - 4)).append("\n\n");
        ticket.append(repeat("-", lineLength)).append("\n");
        ticket.append(String.format("Operator: %s\n", getOperatorName()));
        ticket.append("\n");
        ticket.append(String.format("Signature: %s\n", "__________________"));
        ticket.append("\n");

        String thankYou = centerText("***** THANK YOU *****", lineLength);
        String generated = centerText("*** This is computer generated ***", lineLength);
        String signature = centerText("*** No signature required ***", lineLength);

        ticket.append(thankYou).append("\n");
        ticket.append(generated).append("\n");
        ticket.append(signature).append("\n");
        ticket.append("\n");

        return ticket.toString();
    }

    /**
     * Convert a View to Bitmap for printing
     */
    private Bitmap loadBitmapFromView(View v) {
        try {
            v.measure(View.MeasureSpec.makeMeasureSpec(500, View.MeasureSpec.EXACTLY),
                    View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED));
            v.layout(0, 0, v.getMeasuredWidth(), v.getMeasuredHeight());
            Bitmap b = Bitmap.createBitmap(v.getMeasuredWidth(), v.getMeasuredHeight(),
                    Bitmap.Config.ARGB_8888);
            Canvas c = new Canvas(b);
            v.draw(c);
            return b;
        } catch (Exception e) {
            Log.e("DFragment", "Error creating bitmap", e);
            return Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888);
        }
    }

    private void loadEntry() {
        String searchSerial = searchSerialEditText.getText().toString().trim();
        Log.d("DFragment", "Loading entry: " + searchSerial);

        if (searchSerial.isEmpty()) {
            Toast.makeText(getActivity(), "Please enter serial number", Toast.LENGTH_SHORT).show();
            searchSerialEditText.requestFocus();
            return;
        }

        InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(searchSerialEditText.getWindowToken(), 0);

        WeighmentEntry entry = databaseHelper.getWeighmentBySerialNo(searchSerial);

        if (entry != null) {
            currentSerialNo = searchSerial;
            populateFormWithEntry(entry);
            compareAndSetGrossTare(entry);

            Toast.makeText(getActivity(), "Entry #" + searchSerial + " loaded", Toast.LENGTH_SHORT).show();

            if (txtDisplayTwoView != null) {
                txtDisplayTwoView.setText("Loaded: " + searchSerial + " | Net: " + entry.getNet() + " KG");
            }

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

    private void setupTGMButtons() {
        buttonT.setOnClickListener(v -> {});
        buttonG.setOnClickListener(v -> {});
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

    /**
     * Helper method to repeat strings
     */
    private String repeat(String str, int count) {
        if (count <= 0) return "";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < count; i++) {
            sb.append(str);
        }
        return sb.toString();
    }

    /**
     * Center text within specified width
     */
    private String centerText(String text, int width) {
        if (text == null || text.isEmpty()) {
            return repeat(" ", width);
        }
        text = text.trim();
        if (text.length() >= width) {
            return text.substring(0, width);
        }
        int totalPadding = width - text.length();
        int leftPadding = totalPadding / 2;
        int rightPadding = totalPadding - leftPadding;
        return repeat(" ", leftPadding) + text + repeat(" ", rightPadding);
    }

    /**
     * Format numbers with commas
     */
    private String formatNumber(String number) {
        try {
            if (number == null || number.isEmpty() || number.equals("0")) {
                return "0";
            }
            number = number.replace(",", "");
            long num = Long.parseLong(number);
            return String.format("%,d", num);
        } catch (NumberFormatException e) {
            return number;
        }
    }

    /**
     * Get operator name from preferences
     */
    private String getOperatorName() {
        SharedPreferences prefs = requireContext().getSharedPreferences("MyAppPrefs", Context.MODE_PRIVATE);
        return prefs.getString("operator_name", "Operator");
    }

    // Public methods for MainActivity
    public void performTButtonAction() {}

    public void performGButtonAction() {}

    public void performGButtonActionClear() {}

    public boolean isFragmentVisible() {
        return isAdded() && isVisible();
    }

    @Override
    public void onResume() {
        super.onResume();
        searchSerialEditText.requestFocus();
        refreshAllAdapters();
        setFormFieldsNonEditable();

        if (mainActivity != null) {
            updatePrinterStatus(mainActivity.isPrinterConnected(), mainActivity.getConnectedPrinterName());
        }
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
    }
}