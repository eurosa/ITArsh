package com.googleapi.bluetoothweight;

import androidx.appcompat.widget.AppCompatButton;
import androidx.fragment.app.Fragment;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.hardware.usb.UsbDevice;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.print.PrintAttributes;
import android.print.PrintDocumentAdapter;
import android.print.PrintManager;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.webkit.WebView;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
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

public class AFragment extends Fragment {

    private AppCompatButton button4a, button5a;
    private EditText serialEditText, chargeEditText, grossEditText, tareEditText,
            manualEditText, netWeightEditText;
    private AppCompatButton selectPrinterButton;
    private boolean useUsbPrinting = false;

    // AutoCompleteTextViews for dropdowns
    private AutoCompleteTextView vehicleNoSpinner, vehicleTypeSpinner,
            materialSpinner, partySpinner;
    private PrinterHelper printerHelper;
    private TextView txtNetWeight;
    private View[] focusOrder;
    private DatabaseHelper databaseHelper;

    // Button references for T, G, M
    private AppCompatButton buttonT, buttonG;
    private MainActivity mainActivity;

    // Flag to track manual EditText enabled state
    private boolean isManualEditTextEnabled = false;

    // Adapters for dropdowns
    private ArrayAdapter<String> vehicleNoAdapter;
    private ArrayAdapter<String> vehicleTypeAdapter;
    private ArrayAdapter<String> materialAdapter;
    private ArrayAdapter<String> partyAdapter;

    // WiFi Printer Helper
    private WiFiPrinterHelper wifiPrinterHelper;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_a, container, false);

        // Initialize DatabaseHelper
        databaseHelper = new DatabaseHelper(getActivity());
        mainActivity = (MainActivity) getActivity();

        // Initialize WiFi Printer Helper
        wifiPrinterHelper = new WiFiPrinterHelper(requireContext());

        // Initialize views
        initViews(view);

        // Setup dropdown adapters
        setupDropdownAdapters();

        // Generate next serial number when fragment starts
        generateNextSerialNumber();

        // Setup text watchers for gross and tare to auto-calculate net
        setupCalculationListeners();

        // Setup T, G, M buttons
        setupTGMButtons();

        focusOrder = new View[]{
                vehicleNoSpinner,
                vehicleTypeSpinner,
                materialSpinner,
                partySpinner,
                chargeEditText,
                grossEditText,
                tareEditText,
                manualEditText,
                buttonT,
                buttonG,
                button4a,
                button5a
        };

        // Setup Save and Print buttons
        setupActionButtons();
        setupButtonFocusListeners();
        setupEnterKeyNavigation();
        button5a.setVisibility(View.GONE);

        // Initialize PrinterHelper
        printerHelper = new PrinterHelper(requireContext());

        return view;
    }

    private void printWeighment() {
        String vehicleNo = vehicleNoSpinner.getText().toString().trim();
        String gross = grossEditText.getText().toString().trim();
        String tare = tareEditText.getText().toString().trim();
        String net = netWeightEditText.getText().toString().trim();

        if (vehicleNo.isEmpty()) {
            Toast.makeText(getActivity(), "Enter vehicle number", Toast.LENGTH_SHORT).show();
            return;
        }

        // Print using PrinterHelper
        printerHelper.printTicket(vehicleNo, gross, tare, net);
    }

    private void initViews(View view) {
        serialEditText = view.findViewById(R.id.serialEditText);
        selectPrinterButton = view.findViewById(R.id.selectPrinterButton);
        selectPrinterButton.setVisibility(View.GONE);

        // Initialize AutoCompleteTextViews
        vehicleNoSpinner = view.findViewById(R.id.vehicleNoSpinner);
        vehicleTypeSpinner = view.findViewById(R.id.vehicleTypeSpinner);
        materialSpinner = view.findViewById(R.id.materialSpinner);
        partySpinner = view.findViewById(R.id.partySpinner);

        chargeEditText = view.findViewById(R.id.chargeEditText);
        grossEditText = view.findViewById(R.id.grossEditText);
        tareEditText = view.findViewById(R.id.tareEditText);
        manualEditText = view.findViewById(R.id.manualEditText);
        netWeightEditText = view.findViewById(R.id.netWeightEditText);

        button4a = view.findViewById(R.id.button4a);
        button5a = view.findViewById(R.id.button5a);
        buttonT = view.findViewById(R.id.buttonT);
        buttonG = view.findViewById(R.id.buttonG);

        button4a.setFocusable(true);
        button4a.setFocusableInTouchMode(true);
        button5a.setFocusable(true);
        button5a.setFocusableInTouchMode(true);

        // Make serialEditText non-editable (auto-generated)
        serialEditText.setFocusable(false);
        serialEditText.setClickable(false);

        // Set next focus IDs for buttons
        button4a.setNextFocusDownId(R.id.button5a);
        button4a.setNextFocusDownId(R.id.manualEditText);
        manualEditText.setNextFocusDownId(R.id.button4a);

        // Also set right/left navigation if needed
        button4a.setNextFocusRightId(R.id.button5a);
        button5a.setNextFocusLeftId(R.id.button4a);

        // Initially disable manual EditText
        manualEditText.setEnabled(false);
        manualEditText.setFocusable(false);
        manualEditText.setClickable(false);
        manualEditText.setAlpha(0.5f);

        // Disable weight fields initially
        grossEditText.setEnabled(false);
        tareEditText.setEnabled(false);
        netWeightEditText.setEnabled(false);

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

        // Set item click listeners to automatically add new entries to master data
        setupSpinnerItemClickListeners();
    }

    private void setupSpinnerItemClickListeners() {
        vehicleNoSpinner.setOnItemClickListener((parent, view, position, id) -> {
            String selected = (String) parent.getItemAtPosition(position);
            if (!selected.equals("All") && !databaseHelper.isMasterDataExists(DatabaseHelper.TYPE_VEHICLE_NO, selected)) {
                databaseHelper.insertMasterData(DatabaseHelper.TYPE_VEHICLE_NO, selected);
            }
        });

        vehicleTypeSpinner.setOnItemClickListener((parent, view, position, id) -> {
            String selected = (String) parent.getItemAtPosition(position);
            if (!selected.equals("All") && !databaseHelper.isMasterDataExists(DatabaseHelper.TYPE_VEHICLE_TYPE, selected)) {
                databaseHelper.insertMasterData(DatabaseHelper.TYPE_VEHICLE_TYPE, selected);
            }
        });

        materialSpinner.setOnItemClickListener((parent, view, position, id) -> {
            String selected = (String) parent.getItemAtPosition(position);
            if (!selected.equals("All") && !databaseHelper.isMasterDataExists(DatabaseHelper.TYPE_MATERIAL, selected)) {
                databaseHelper.insertMasterData(DatabaseHelper.TYPE_MATERIAL, selected);
            }
        });

        partySpinner.setOnItemClickListener((parent, view, position, id) -> {
            String selected = (String) parent.getItemAtPosition(position);
            if (!selected.equals("All") && !databaseHelper.isMasterDataExists(DatabaseHelper.TYPE_PARTY, selected)) {
                databaseHelper.insertMasterData(DatabaseHelper.TYPE_PARTY, selected);
            }
        });

        vehicleNoSpinner.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) refreshVehicleNoAdapter();
        });

        vehicleTypeSpinner.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) refreshVehicleTypeAdapter();
        });

        materialSpinner.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) refreshMaterialAdapter();
        });

        partySpinner.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) refreshPartyAdapter();
        });
    }

    private void refreshVehicleNoAdapter() {
        List<String> vehicleNumbers = databaseHelper.getUniqueVehicleNumbers();
        vehicleNoAdapter.clear();
        vehicleNoAdapter.addAll(vehicleNumbers);
        vehicleNoAdapter.notifyDataSetChanged();
    }

    private void refreshVehicleTypeAdapter() {
        List<String> vehicleTypes = databaseHelper.getUniqueVehicleTypes();
        vehicleTypeAdapter.clear();
        vehicleTypeAdapter.addAll(vehicleTypes);
        vehicleTypeAdapter.notifyDataSetChanged();
    }

    private void refreshMaterialAdapter() {
        List<String> materials = databaseHelper.getUniqueMaterials();
        materialAdapter.clear();
        materialAdapter.addAll(materials);
        materialAdapter.notifyDataSetChanged();
    }

    private void refreshPartyAdapter() {
        List<String> parties = databaseHelper.getUniqueParties();
        partyAdapter.clear();
        partyAdapter.addAll(parties);
        partyAdapter.notifyDataSetChanged();
    }

    private void generateNextSerialNumber() {
        int nextSerial = databaseHelper.getNextSerialNumber();
        serialEditText.setText(String.valueOf(nextSerial));
    }

    private void setupEnterKeyNavigation() {
        View[] fields = {
                vehicleNoSpinner,
                vehicleTypeSpinner,
                materialSpinner,
                partySpinner,
                chargeEditText,
                grossEditText,
                tareEditText,
                manualEditText
        };

        for (View field : fields) {
            if (field != null) {
                field.setOnKeyListener((v, keyCode, event) -> {
                    if (keyCode == KeyEvent.KEYCODE_ENTER && event.getAction() == KeyEvent.ACTION_DOWN) {
                        int currentIndex = -1;
                        for (int i = 0; i < fields.length; i++) {
                            if (fields[i] == v) {
                                currentIndex = i;
                                break;
                            }
                        }

                        if (currentIndex >= 0 && currentIndex < fields.length - 1) {
                            fields[currentIndex + 1].requestFocus();
                        } else {
                            button4a.requestFocus();
                        }
                        return true;
                    }
                    return false;
                });
            }
        }

        button4a.setOnKeyListener((v, keyCode, event) -> {
            if (keyCode == KeyEvent.KEYCODE_ENTER && event.getAction() == KeyEvent.ACTION_DOWN) {
                button5a.requestFocus();
                return true;
            }
            return false;
        });
    }

    private void clearAllFields() {
        vehicleNoSpinner.setText("");
        vehicleTypeSpinner.setText("");
        materialSpinner.setText("");
        partySpinner.setText("");
        chargeEditText.setText("");
        grossEditText.setText("");
        tareEditText.setText("");
        manualEditText.setText("");
        netWeightEditText.setText("");

        if (txtNetWeight != null) {
            txtNetWeight.setText("");
        }

        generateNextSerialNumber();
        disableManualEditText();
    }

    private void printWithAndroidFramework() {
        String vehicleNo = vehicleNoSpinner.getText().toString().trim();
        String gross = grossEditText.getText().toString().trim();
        String tare = tareEditText.getText().toString().trim();
        String net = netWeightEditText.getText().toString().trim();

        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault());
        String dateTime = sdf.format(new Date());

        String printText = buildSimplePrintText(vehicleNo, gross, tare, net, dateTime);

        PrintManager printManager = (PrintManager) requireActivity()
                .getSystemService(Context.PRINT_SERVICE);

        MyPrintDocumentAdapter printAdapter = new MyPrintDocumentAdapter(requireContext(), printText);
        String jobName = getString(R.string.app_name) + " - Weighment Ticket #" + vehicleNo;
        printManager.print(jobName, printAdapter, new PrintAttributes.Builder().build());
    }

    private String buildSimplePrintText(String vehicleNo, String gross, String tare, String net, String dateTime) {
        return "================================\n" +
                "     WEIGHMENT TICKET\n" +
                "================================\n" +
                "Vehicle No: " + vehicleNo + "\n" +
                "Gross: " + gross + " kg\n" +
                "Tare: " + tare + " kg\n" +
                "Net: " + net + " kg\n" +
                "================================\n" +
                "Date: " + dateTime + "\n" +
                "================================";
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
        manualEditText.addTextChangedListener(calculationWatcher);
    }

    private void calculateNetWeight() {
        String grossStr = grossEditText.getText().toString().trim();
        String tareStr = manualEditText.getText().toString().trim();

        if (!grossStr.isEmpty() && !tareStr.isEmpty()) {
            try {
                long gross = Long.parseLong(grossStr);
                long tare = Long.parseLong(tareStr);
                long net = gross - tare;

                Log.d("AFragment", "Net Weight: " + net);
                netWeightEditText.setText(String.valueOf(net));
            } catch (NumberFormatException e) {
                // Ignore parsing errors
            }
        }
    }

    private void setupTGMButtons() {
        buttonT.setOnClickListener(v -> {
            if (mainActivity != null && mainActivity.txtCounter != null) {
                String textWithSpaces = mainActivity.txtCounter.getText().toString();
                String textWithoutSpaces = textWithSpaces.replaceAll("\\s", "");
                tareEditText.setText(textWithoutSpaces);
            }
        });

        buttonG.setOnClickListener(v -> {
            if (mainActivity != null && mainActivity.txtCounter != null) {
                String textWithSpaces = mainActivity.txtCounter.getText().toString();
                String textWithoutSpaces = textWithSpaces.replaceAll("\\s", "");
                grossEditText.setText(textWithoutSpaces);
            }
        });
    }

    private void toggleManualEditText() {
        isManualEditTextEnabled = !isManualEditTextEnabled;

        if (isManualEditTextEnabled) {
            enableManualEditText();
        } else {
            disableManualEditText();
        }
    }

    private void enableManualEditText() {
        manualEditText.setEnabled(true);
        manualEditText.setFocusable(true);
        manualEditText.setFocusableInTouchMode(true);
        manualEditText.setClickable(true);
        manualEditText.setAlpha(1.0f);
        manualEditText.requestFocus();

        InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.showSoftInput(manualEditText, InputMethodManager.SHOW_IMPLICIT);
    }

    private void disableManualEditText() {
        manualEditText.setEnabled(false);
        manualEditText.setFocusable(false);
        manualEditText.setFocusableInTouchMode(false);
        manualEditText.setClickable(false);
        manualEditText.setAlpha(0.5f);

        InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(manualEditText.getWindowToken(), 0);
    }

    public void performTButtonAction() {
        if (grossEditText.getText() != null &&
                !grossEditText.getText().toString().trim().isEmpty()) {
            button4a.requestFocus();
            return;
        }
        if (mainActivity != null && mainActivity.txtCounter != null) {
            tareEditText.setText(mainActivity.txtCounter.getText().toString().trim());
            button4a.requestFocus();
        }
    }

    public AppCompatButton getButtonT() { return buttonT; }
    public AppCompatButton getButtonG() { return buttonG; }
    public AppCompatButton getButton4a() { return button4a; }
    public AppCompatButton getButton5a() { return button5a; }

    public void performGButtonAction() {
        if (tareEditText.getText() != null &&
                !tareEditText.getText().toString().trim().isEmpty()) {
            button4a.requestFocus();
            return;
        }
        if (mainActivity != null && mainActivity.txtCounter != null) {
            grossEditText.setText(mainActivity.txtCounter.getText().toString().trim());
            button4a.requestFocus();
        }
    }

    public void performGButtonActionClear() { clearAllFields(); }

    public void performMButtonAction() {
        if (grossEditText.getText() != null &&
                !grossEditText.getText().toString().trim().isEmpty()) {
            toggleManualEditText();
        }
    }

    private void setupButtonFocusListeners() {
        if (buttonT != null) {
            buttonT.setOnFocusChangeListener((v, hasFocus) -> {
                if (hasFocus) {
                    buttonT.setBackgroundColor(Color.parseColor("#FFA500"));
                    buttonT.setTextColor(Color.WHITE);
                } else {
                    buttonT.setBackgroundColor(Color.parseColor("#808080"));
                    buttonT.setTextColor(Color.BLACK);
                }
            });
        }

        if (buttonG != null) {
            buttonG.setOnFocusChangeListener((v, hasFocus) -> {
                if (hasFocus) {
                    buttonG.setBackgroundColor(Color.parseColor("#00FF00"));
                    buttonG.setTextColor(Color.WHITE);
                } else {
                    buttonG.setBackgroundColor(Color.parseColor("#808080"));
                    buttonG.setTextColor(Color.BLACK);
                }
            });
        }

        if (button4a != null) {
            button4a.setOnFocusChangeListener((v, hasFocus) -> {
                if (hasFocus) {
                    button4a.setBackgroundColor(Color.parseColor("#00FF00"));
                    button4a.setTextColor(Color.WHITE);
                } else {
                    button4a.setBackgroundColor(Color.parseColor("#808080"));
                    button4a.setTextColor(Color.BLACK);
                }
            });

            button4a.setOnKeyListener((v, keyCode, event) -> {
                if (event.getAction() == KeyEvent.ACTION_DOWN) {
                    if (keyCode == KeyEvent.KEYCODE_ENTER || keyCode == KeyEvent.KEYCODE_NUMPAD_ENTER) {
                        saveWeighmentEntry();
                        moveToNextFocus(v);
                        return true;
                    }
                }
                return false;
            });
        }

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

            button5a.setOnKeyListener((v, keyCode, event) -> {
                if (event.getAction() == KeyEvent.ACTION_DOWN) {
                    if (keyCode == KeyEvent.KEYCODE_ENTER || keyCode == KeyEvent.KEYCODE_NUMPAD_ENTER) {
                        printWeighmentEntry();
                        moveToNextFocus(v);
                        return true;
                    }
                }
                return false;
            });
        }
    }

    private void moveToNextFocus(View currentView) {
        int currentIndex = -1;
        for (int i = 0; i < focusOrder.length; i++) {
            if (focusOrder[i] == currentView) {
                currentIndex = i;
                break;
            }
        }

        if (currentIndex != -1) {
            int nextIndex = (currentIndex + 1) % focusOrder.length;
            int attempts = 0;

            while (attempts < focusOrder.length) {
                View nextView = focusOrder[nextIndex];

                if (nextView != null && nextView.isFocusable()) {
                    nextView.requestFocus();

                    if (nextView instanceof EditText) {
                        showKeyboard((EditText) nextView);
                    } else {
                        hideKeyboard();
                    }
                    return;
                }

                nextIndex = (nextIndex + 1) % focusOrder.length;
                attempts++;
            }
        }
    }

    private void showKeyboard(EditText editText) {
        if (getActivity() != null) {
            editText.requestFocus();
            InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.showSoftInput(editText, InputMethodManager.SHOW_IMPLICIT);
        }
    }

    private void hideKeyboard() {
        if (getActivity() != null && getActivity().getCurrentFocus() != null) {
            InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(getActivity().getCurrentFocus().getWindowToken(), 0);
        }
    }

    private void setupActionButtons() {
        button4a.setOnClickListener(v -> saveWeighmentEntry());
        button5a.setOnClickListener(v -> printWeighmentEntry());
    }

    private void saveWeighmentEntry() {
        String serialNo = serialEditText.getText().toString().trim();
        String vehicleNo = vehicleNoSpinner.getText().toString().trim();
        String vehicleType = vehicleTypeSpinner.getText().toString().trim();
        String material = materialSpinner.getText().toString().trim();
        String party = partySpinner.getText().toString().trim();
        String charge = chargeEditText.getText().toString().trim();
        String gross = grossEditText.getText().toString().trim();
        String tare = tareEditText.getText().toString().trim();
        String manualTare = manualEditText.getText().toString().trim();

        if (serialNo.isEmpty()) {
            Toast.makeText(getActivity(), "Serial number is required", Toast.LENGTH_SHORT).show();
            serialEditText.requestFocus();
            return;
        }

        if (vehicleNo.isEmpty()) {
            vehicleNoSpinner.setError("Vehicle number is required");
            vehicleNoSpinner.requestFocus();
            return;
        }

        if (!vehicleNo.isEmpty() && !databaseHelper.isMasterDataExists(DatabaseHelper.TYPE_VEHICLE_NO, vehicleNo)) {
            databaseHelper.insertMasterData(DatabaseHelper.TYPE_VEHICLE_NO, vehicleNo);
        }
        if (!vehicleType.isEmpty() && !databaseHelper.isMasterDataExists(DatabaseHelper.TYPE_VEHICLE_TYPE, vehicleType)) {
            databaseHelper.insertMasterData(DatabaseHelper.TYPE_VEHICLE_TYPE, vehicleType);
        }
        if (!material.isEmpty() && !databaseHelper.isMasterDataExists(DatabaseHelper.TYPE_MATERIAL, material)) {
            databaseHelper.insertMasterData(DatabaseHelper.TYPE_MATERIAL, material);
        }
        if (!party.isEmpty() && !databaseHelper.isMasterDataExists(DatabaseHelper.TYPE_PARTY, party)) {
            databaseHelper.insertMasterData(DatabaseHelper.TYPE_PARTY, party);
        }

        boolean hasValidGross = false;
        boolean hasValidTare = false;

        if (!gross.isEmpty()) {
            try {
                double grossValue = Double.parseDouble(gross);
                if (grossValue >= 0) {
                    hasValidGross = true;
                } else {
                    grossEditText.setError("Gross weight cannot be negative");
                    grossEditText.requestFocus();
                    return;
                }
            } catch (NumberFormatException e) {
                grossEditText.setError("Please enter a valid number for Gross weight");
                grossEditText.requestFocus();
                return;
            }
        }

        if (!tare.isEmpty()) {
            try {
                double tareValue = Double.parseDouble(tare);
                if (tareValue >= 0) {
                    hasValidTare = true;
                } else {
                    tareEditText.setError("Tare weight cannot be negative");
                    tareEditText.requestFocus();
                    return;
                }
            } catch (NumberFormatException e) {
                tareEditText.setError("Please enter a valid number for Tare weight");
                tareEditText.requestFocus();
                return;
            }
        }

        if (!hasValidGross && !hasValidTare) {
            Toast.makeText(getActivity(), "Please enter at least Gross weight or Tare weight",
                    Toast.LENGTH_LONG).show();
            grossEditText.requestFocus();
            return;
        }

        String finalGross = gross.isEmpty() ? "0" : gross;
        String finalTare = tare.isEmpty() ? "0" : tare;

        WeighmentEntry entry = new WeighmentEntry();
        entry.setSerialNo(serialNo);
        entry.setVehicleNo(vehicleNo);
        entry.setVehicleType(vehicleType);
        entry.setMaterial(material);
        entry.setParty(party);
        entry.setCharge(charge);
        entry.setGross(finalGross);
        entry.setTare(finalTare);
        entry.setManualTare(manualTare);
        entry.calculateNet();

        boolean exists = databaseHelper.isSerialNoExists(serialNo);

        if (exists) {
            showUpdateConfirmationDialog(entry);
        } else {
            long result = databaseHelper.insertWeighment(entry);
            if (result > 0) {
                Toast.makeText(getActivity(), "Entry #" + serialNo + " saved successfully",
                        Toast.LENGTH_SHORT).show();

                // Auto print after save - try methods sequentially
               // autoDetectAndPrint(entry);
                showPrintOptionsDialog(entry);
                clearAllFields();
                refreshAllAdapters();
                moveFocusToPrintButton();
            } else {
                Toast.makeText(getActivity(), "Error saving entry", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void printWeighmentEntry() {
        String serialNo = serialEditText.getText().toString().trim();
        String vehicleNo = vehicleNoSpinner.getText().toString().trim();
        String vehicleType = vehicleTypeSpinner.getText().toString().trim();
        String material = materialSpinner.getText().toString().trim();
        String party = partySpinner.getText().toString().trim();
        String charge = chargeEditText.getText().toString().trim();
        String gross = grossEditText.getText().toString().trim();
        String tare = tareEditText.getText().toString().trim();
        String manualTare = manualEditText.getText().toString().trim();

        Log.d("AFragment", "printWeighmentEntry called");

        if (!isAdded() || getActivity() == null) {
            Log.e("AFragment", "Fragment not attached");
            return;
        }

        if (serialNo.isEmpty()) {
            Toast.makeText(getActivity(), "Serial number is required", Toast.LENGTH_SHORT).show();
            serialEditText.requestFocus();
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
        entry.setManualTare(manualTare);
        entry.calculateNet();

        Log.d("AFragment", "Calling showPrintOptionsDialog");

        // Show print options dialog
        showPrintOptionsDialog(entry);
    }

    /**
     * Show print options dialog with multiple print methods
     */
    /**
     * Show print options dialog with multiple print methods
     */
    /**
     * Show print options dialog with multiple print methods
     */
    private void showPrintOptionsDialog(WeighmentEntry entry) {
        String[] options = {
                "USB/PCL Print",           // 0
                "Android Print Framework", // 1
                "WiFi Print",              // 2
                "NokoPrint App",           // 3
                "Auto Detect Best Method"  // 4
        };

        Log.d("AFragment", "========== showPrintOptionsDialog ==========");
        Log.d("AFragment", "Entry: " + (entry != null ? entry.getSerialNo() : "null"));
        Log.d("AFragment", "isAdded: " + isAdded());
        Log.d("AFragment", "getActivity: " + (getActivity() != null));
        Log.d("AFragment", "getContext: " + (getContext() != null));

        // Check if fragment is added and context is available
        if (!isAdded()) {
            Log.e("AFragment", "Fragment not attached to activity, cannot show dialog");
            Toast.makeText(requireContext(), "Fragment not attached", Toast.LENGTH_SHORT).show();
            return;
        }

        if (getActivity() == null) {
            Log.e("AFragment", "Activity is null, cannot show dialog");
            return;
        }

        if (getContext() == null) {
            Log.e("AFragment", "Context is null, cannot show dialog");
            return;
        }

        try {
            // Create dialog on UI thread
            getActivity().runOnUiThread(() -> {
                try {
                    Log.d("AFragment", "Creating AlertDialog on UI thread");

                    AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
                    builder.setTitle("Select Print Method")
                            .setItems(options, (dialog, which) -> {
                                Log.d("AFragment", "Selected option: " + which + " - " + options[which]);

                                // Check if fragment is still attached
                                if (!isAdded() || getActivity() == null) {
                                    Log.e("AFragment", "Fragment detached after dialog selection");
                                    return;
                                }

                                // Show progress dialog
                                ProgressDialog progressDialog = new ProgressDialog(getActivity());
                                progressDialog.setMessage("Processing " + options[which] + "...");
                                progressDialog.setCancelable(true);
                                progressDialog.show();

                                // Execute print in background
                                ExecutorService executor = Executors.newSingleThreadExecutor();
                                executor.submit(() -> {
                                    boolean result = false;
                                    String method = options[which];

                                    try {
                                        Log.d("AFragment", "Executing print method: " + method);

                                        switch (which) {
                                            case 0: // USB/PCL Print
                                                result = printWithPCL(entry);
                                                break;
                                            case 1: // Android Print Framework
                                                printUsingAndroidPrintFramework(entry);
                                                result = true;
                                                break;
                                            case 2: // WiFi Print
                                                result = printWithWiFi(entry);
                                                break;
                                            case 3: // NokoPrint App
                                                result = printWithNokoPrint(entry);
                                                break;
                                            case 4: // Auto Detect
                                                autoDetectAndPrint(entry);
                                                result = true;
                                                break;
                                        }

                                        Log.d("AFragment", "Print result for " + method + ": " + result);

                                    } catch (Exception e) {
                                        Log.e("AFragment", "Error in " + method + ": " + e.getMessage());
                                        e.printStackTrace();
                                        result = false;
                                    }

                                    final boolean finalResult = result;
                                    final String finalMethod = method;

                                    if (getActivity() != null) {
                                        getActivity().runOnUiThread(() -> {
                                            try {
                                                progressDialog.dismiss();

                                                if (finalResult) {
                                                    Toast.makeText(getActivity(),
                                                            "✅ " + finalMethod + " Successful",
                                                            Toast.LENGTH_LONG).show();
                                                } else {
                                                    Toast.makeText(getActivity(),
                                                            "❌ " + finalMethod + " Failed",
                                                            Toast.LENGTH_LONG).show();
                                                    showPrintPreviewDialog(buildPrintText(entry), entry);
                                                }
                                            } catch (Exception e) {
                                                Log.e("AFragment", "Error in UI update: " + e.getMessage());
                                            }
                                        });
                                    }
                                });
                                executor.shutdown();
                            })
                            .setNegativeButton("Cancel", (dialog, which) -> {
                                Log.d("AFragment", "Dialog cancelled");
                                dialog.dismiss();
                            });

                    AlertDialog dialog = builder.create();
                    dialog.show();

                    Log.d("AFragment", "Dialog shown successfully");

                } catch (Exception e) {
                    Log.e("AFragment", "Error creating dialog: " + e.getMessage());
                    e.printStackTrace();

                    // Show toast as fallback
                    Toast.makeText(requireContext(),
                            "Error showing print options: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                }
            });

        } catch (Exception e) {
            Log.e("AFragment", "Error in showPrintOptionsDialog: " + e.getMessage());
            e.printStackTrace();

            // Show toast as fallback
            Toast.makeText(requireContext(),
                    "Error: " + e.getMessage(),
                    Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Print with WiFi - Returns boolean success
     */
    private boolean printWithWiFi(WeighmentEntry entry) {
        try {
            Log.d("PRINT", "printWithWiFi: Starting...");

            // Show WiFi print options
            showWiFiPrintOptions(entry);
            return true; // Assume success as it's async

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

        // This is a placeholder - implement actual WiFi scanning
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
                // Here you would implement WiFi printing
                Toast.makeText(getActivity(), "Connecting to WiFi printer: " + ipAddress, Toast.LENGTH_SHORT).show();
                // For now, fallback to Android print
                printUsingAndroidPrintFramework(entry);
            }
        });

        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    /**
     * Auto detect and print - tries methods sequentially until one succeeds
     */
    private void autoDetectAndPrint(WeighmentEntry entry) {
        ProgressDialog progressDialog = new ProgressDialog(getActivity());
        progressDialog.setMessage("Printing...\nTrying PCL method");
        progressDialog.setCancelable(true);
        progressDialog.show();

        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.submit(() -> {
            boolean success = false;
            String successMethod = "";

            // Method 1: Try PCL Print
            if (!success) {
                try {
                    Log.d("PRINT", "Trying PCL Print...");
                    if (getActivity() != null) {
                        getActivity().runOnUiThread(() ->
                                progressDialog.setMessage("Printing...\nTrying PCL method"));
                    }
                    success = printWithPCL(entry);
                    if (success) {
                        successMethod = "PCL";
                        Log.d("PRINT", "✅ PCL Print successful");
                    }
                } catch (Exception e) {
                    Log.e("PRINT", "PCL failed: " + e.getMessage());
                }
            }

            // Method 2: Try USB Direct Print
            if (!success) {
                try {
                    Log.d("PRINT", "Trying USB Direct Print...");
                    if (getActivity() != null) {
                        getActivity().runOnUiThread(() ->
                                progressDialog.setMessage("Printing...\nTrying USB Direct"));
                    }
                    success = printWithUSB(entry);
                    if (success) {
                        successMethod = "USB";
                        Log.d("PRINT", "✅ USB Direct successful");
                    }
                } catch (Exception e) {
                    Log.e("PRINT", "USB failed: " + e.getMessage());
                }
            }

            // Method 3: Try WiFi Print
            if (!success) {
                try {
                    Log.d("PRINT", "Trying WiFi Print...");
                    if (getActivity() != null) {
                        getActivity().runOnUiThread(() ->
                                progressDialog.setMessage("Printing...\nTrying WiFi Print"));
                    }
                    success = printWithWiFi(entry);
                    if (success) {
                        successMethod = "WiFi";
                        Log.d("PRINT", "✅ WiFi Print successful");
                    }
                } catch (Exception e) {
                    Log.e("PRINT", "WiFi failed: " + e.getMessage());
                }
            }

            // Method 4: Try HTML Print
            if (!success) {
                try {
                    Log.d("PRINT", "Trying HTML Print...");
                    if (getActivity() != null) {
                        getActivity().runOnUiThread(() ->
                                progressDialog.setMessage("Printing...\nTrying HTML Print"));
                    }
                    success = printWithHTMLDirect(entry);
                    if (success) {
                        successMethod = "HTML";
                        Log.d("PRINT", "✅ HTML Print successful");
                    }
                } catch (Exception e) {
                    Log.e("PRINT", "HTML failed: " + e.getMessage());
                }
            }

            // Method 5: Try NokoPrint
            if (!success) {
                try {
                    Log.d("PRINT", "Trying NokoPrint...");
                    if (getActivity() != null) {
                        getActivity().runOnUiThread(() ->
                                progressDialog.setMessage("Printing...\nTrying NokoPrint"));
                    }
                    success = printWithNokoPrint(entry);
                    if (success) {
                        successMethod = "NokoPrint";
                        Log.d("PRINT", "✅ NokoPrint successful");
                    }
                } catch (Exception e) {
                    Log.e("PRINT", "NokoPrint failed: " + e.getMessage());
                }
            }

            // Method 6: Try Bluetooth (if implemented)
            if (!success) {
                try {
                    Log.d("PRINT", "Trying Bluetooth...");
                    if (getActivity() != null) {
                        getActivity().runOnUiThread(() ->
                                progressDialog.setMessage("Printing...\nTrying Bluetooth"));
                    }
                    success = printWithBluetooth(entry);
                    if (success) {
                        successMethod = "Bluetooth";
                        Log.d("PRINT", "✅ Bluetooth successful");
                    }
                } catch (Exception e) {
                    Log.e("PRINT", "Bluetooth failed: " + e.getMessage());
                }
            }

            final boolean finalSuccess = success;
            final String finalMethod = successMethod;

            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    progressDialog.dismiss();
                    if (finalSuccess) {
                        Toast.makeText(getActivity(), "✅ Print Successful using " + finalMethod,
                                Toast.LENGTH_LONG).show();
                        clearAllFields();
                        refreshAllAdapters();
                        moveFocusToSaveButton();
                    } else {
                        Toast.makeText(getActivity(), "❌ All print methods failed",
                                Toast.LENGTH_LONG).show();
                    }
                });
            }
        });
        executor.shutdown();
    }

    /**
     * Print with PCL - Returns boolean success
     */
    private boolean printWithPCL(WeighmentEntry entry) {
        if (mainActivity == null || !mainActivity.isPrinterConnected()) {
            return false;
        }

        PrinterManager printerManager = mainActivity.getPrinterManager();
        if (printerManager == null || printerManager.usbPrinterHelper == null) {
            return false;
        }

        try {
            byte[] ticketBytes = buildPCLTicketBytes(entry);
            if (ticketBytes == null || ticketBytes.length == 0) {
                return false;
            }

            return printerManager.usbPrinterHelper.sendRawData(ticketBytes);
        } catch (Exception e) {
            Log.e("PRINT", "printWithPCL exception: " + e.getMessage());
            return false;
        }
    }

    /**
     * Print with USB Direct - Returns boolean success
     */
    private boolean printWithUSB(WeighmentEntry entry) {
        if (mainActivity == null || !mainActivity.isPrinterConnected()) {
            return false;
        }

        PrinterManager printerManager = mainActivity.getPrinterManager();
        if (printerManager == null || printerManager.usbPrinterHelper == null) {
            return false;
        }

        try {
            String textContent = buildPrintText(entry);
            if (textContent == null || textContent.isEmpty()) {
                return false;
            }

            return printerManager.usbPrinterHelper.printText(textContent);
        } catch (Exception e) {
            Log.e("PRINT", "printWithUSB exception: " + e.getMessage());
            return false;
        }
    }

    /**
     * Print with Bluetooth - Returns boolean success
     */
    private boolean printWithBluetooth(WeighmentEntry entry) {
        // Implement Bluetooth printing when needed
        return false;
    }

    /**
     * Print with NokoPrint - Returns boolean success
     */
    private boolean printWithNokoPrint(WeighmentEntry entry) {
        try {
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
     * Print with HTML Direct - Returns boolean success
     */
    private boolean printWithHTMLDirect(WeighmentEntry entry) {
        try {
            SharedPreferences prefs = requireContext().getSharedPreferences("MyAppPrefs", Context.MODE_PRIVATE);
            String field1 = prefs.getString("field_0", "MY WEIGHBRIDGE COMPANY");
            String field2 = prefs.getString("field_1", "123 Industrial Area, City - 123456");
            String field3 = prefs.getString("field_2", "Phone: +91 9876543210");

            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault());
            String dateTime = sdf.format(new Date());

            String htmlContent = buildHTMLTicket(entry, field1, field2, field3, dateTime);
            if (htmlContent == null || htmlContent.isEmpty()) {
                return false;
            }

            PrintManager printManager = (PrintManager) requireActivity()
                    .getSystemService(Context.PRINT_SERVICE);

            String jobName = getString(R.string.app_name) + " - Ticket " + entry.getSerialNo();

            WebView webView = new WebView(requireContext());
            webView.loadDataWithBaseURL(null, htmlContent, "text/html", "UTF-8", null);

            PrintDocumentAdapter printAdapter = webView.createPrintDocumentAdapter(jobName);
            printManager.print(jobName, printAdapter, new PrintAttributes.Builder().build());

            return true;
        } catch (Exception e) {
            Log.e("PRINT", "printWithHTMLDirect exception: " + e.getMessage());
            return false;
        }
    }

    private String buildHTMLTicket(WeighmentEntry entry, String field1, String field2,
                                   String field3, String dateTime) {
        return "<!DOCTYPE html>" +
                "<html>" +
                "<head>" +
                "<style>" +
                "body { font-family: 'Courier New', monospace; max-width: 300px; margin: 0 auto; }" +
                ".field1 { text-align: center; font-size: 24px; font-weight: bold; margin: 10px 0; }" +
                ".field2 { text-align: center; font-size: 16px; }" +
                ".field3 { text-align: center; font-size: 16px; margin-bottom: 15px; }" +
                ".line { text-align: center; font-size: 16px; }" +
                "table { width: 100%; }" +
                "td { padding: 2px 5px; }" +
                ".right { text-align: right; }" +
                ".bold { font-weight: bold; }" +
                "</style>" +
                "</head>" +
                "<body>" +
                "<div class='field1'>" + field1 + "</div>" +
                "<div class='field2'>" + field2 + "</div>" +
                "<div class='field3'>" + field3 + "</div>" +
                "<div class='line'>========================</div>" +
                "<table>" +
                "<tr><td>Ticket #:</td><td class='right'>" + entry.getSerialNo() + "</td></tr>" +
                "<tr><td>Date:</td><td class='right'>" + dateTime + "</td></tr>" +
                "</table><hr/>" +
                "<div class='bold'>VEHICLE DETAILS</div>" +
                "<table>" +
                "<tr><td>Vehicle No:</td><td class='right'>" + entry.getVehicleNo() + "</td></tr>" +
                "<tr><td>Type:</td><td class='right'>" + entry.getVehicleType() + "</td></tr>" +
                "<tr><td>Material:</td><td class='right'>" + entry.getMaterial() + "</td></tr>" +
                "<tr><td>Party:</td><td class='right'>" + entry.getParty() + "</td></tr>" +
                (entry.getCharge() != null && !entry.getCharge().isEmpty() ?
                        "<tr><td>Charge:</td><td class='right'>" + entry.getCharge() + "</td></tr>" : "") +
                "</table><hr/>" +
                "<div class='bold'>WEIGHT DETAILS</div>" +
                "<table>" +
                "<tr><td>Gross Weight:</td><td class='right'>" + formatNumber(entry.getGross()) + " kg</td></tr>" +
                "<tr><td>Tare Weight:</td><td class='right'>" + formatNumber(entry.getTare()) + " kg</td></tr>" +
                (!entry.getManualTare().equals("0") ?
                        "<tr><td>Manual Tare:</td><td class='right'>" + formatNumber(entry.getManualTare()) + " kg</td></tr>" : "") +
                "</table>" +
                "<div style='border-top:1px solid #000; margin:5px 0;'></div>" +
                "<table><tr><td class='bold'>NET WEIGHT:</td><td class='right bold'>" +
                formatNumber(entry.getNet()) + " kg</td></tr></table>" +
                "<div style='border-top:1px solid #000; margin:5px 0;'></div>" +
                "<hr/><table><tr><td>Operator:</td><td class='right'>" + getOperatorName() + "</td></tr></table>" +
                "<p>Signature: __________________</p>" +
                "<div style='text-align:center; margin-top:20px;'>" +
                "***** THANK YOU *****<br/>" +
                "*** This is computer generated ***<br/>" +
                "*** No signature required ***" +
                "</div></body></html>";
    }

    private byte[] buildPCLTicketBytes(WeighmentEntry entry) {
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault());
        String dateTime = sdf.format(new Date());

        String grossStr = entry.getGross().isEmpty() ? "0" : entry.getGross();
        String tareStr = entry.getTare().isEmpty() ? "0" : entry.getTare();
        String manualTareStr = entry.getManualTare().isEmpty() ? "0" : entry.getManualTare();
        String netStr = entry.getNet().isEmpty() ? "0" : entry.getNet();

        SharedPreferences prefs = requireContext().getSharedPreferences("MyAppPrefs", Context.MODE_PRIVATE);
        String field1 = prefs.getString("field_0", "MY WEIGHBRIDGE COMPANY");
        String field2 = prefs.getString("field_1", "123 Industrial Area, City - 123456");
        String field3 = prefs.getString("field_2", "Phone: +91 9876543210");

        int pageWidth = 42;

        String centeredField1 = centerText(field1, pageWidth);
        String centeredField2 = centerText(field2, pageWidth);
        String centeredField3 = centerText(field3, pageWidth);

        StringBuilder ticket = new StringBuilder();

        ticket.append("\n\n\n\n\n\n");
        ticket.append(centeredField1).append("\n");
        ticket.append(centeredField2).append("\n");
        ticket.append(centeredField3).append("\n");
        ticket.append(repeat("=", pageWidth)).append("\n\n");
        ticket.append(String.format("%-20s %s", "Ticket #:", entry.getSerialNo())).append("\n");
        ticket.append(String.format("%-20s %s", "Date:", dateTime)).append("\n");
        ticket.append(repeat("-", pageWidth)).append("\n");
        ticket.append("VEHICLE DETAILS:").append("\n");
        ticket.append(String.format("  %-16s %s", "Vehicle No:", entry.getVehicleNo())).append("\n");
        ticket.append(String.format("  %-16s %s", "Type:", entry.getVehicleType())).append("\n");
        ticket.append(String.format("  %-16s %s", "Material:", entry.getMaterial())).append("\n");
        ticket.append(String.format("  %-16s %s", "Party:", entry.getParty())).append("\n");

        if (entry.getCharge() != null && !entry.getCharge().isEmpty()) {
            ticket.append(String.format("  %-16s %s", "Charge:", entry.getCharge())).append("\n");
        }
        ticket.append(repeat("-", pageWidth)).append("\n");
        ticket.append("WEIGHT DETAILS:").append("\n");
        ticket.append(String.format("  %-16s %10s kg", "Gross Weight:", formatNumber(grossStr))).append("\n");
        ticket.append(String.format("  %-16s %10s kg", "Tare Weight:", formatNumber(tareStr))).append("\n");

        if (!manualTareStr.equals("0")) {
            ticket.append(String.format("  %-16s %10s kg", "Manual Tare:", formatNumber(manualTareStr))).append("\n");
        }

        ticket.append("  " + repeat("-", pageWidth - 4)).append("\n");
        ticket.append(String.format("  %-16s %10s kg", "NET WEIGHT:", formatNumber(netStr))).append("\n");
        ticket.append("  " + repeat("-", pageWidth - 4)).append("\n\n");
        ticket.append(repeat("-", pageWidth)).append("\n");
        ticket.append(String.format("Operator: %s", getOperatorName())).append("\n\n");
        ticket.append(String.format("Signature: %s", "__________________")).append("\n\n");

        String thankYou = centerText("***** THANK YOU *****", pageWidth);
        String generated = centerText("*** This is computer generated ***", pageWidth);
        String signature = centerText("*** No signature required ***", pageWidth);

        ticket.append(thankYou).append("\n");
        ticket.append(generated).append("\n");
        ticket.append(signature).append("\n\n");

        try {
            return ticket.toString().getBytes("UTF-8");
        } catch (Exception e) {
            e.printStackTrace();
            return new byte[0];
        }
    }

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

    private String getOperatorName() {
        SharedPreferences prefs = requireContext().getSharedPreferences("MyAppPrefs", Context.MODE_PRIVATE);
        return prefs.getString("operator_name", "Operator");
    }

    private String repeat(String str, int count) {
        if (count <= 0) return "";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < count; i++) {
            sb.append(str);
        }
        return sb.toString();
    }

    private void showUpdateConfirmationDialog(WeighmentEntry entry) {
        new AlertDialog.Builder(getActivity())
                .setTitle("Update Entry")
                .setMessage("Serial #" + entry.getSerialNo() + " already exists. Do you want to update it?")
                .setPositiveButton("Update", (dialog, which) -> {
                    int result = databaseHelper.updateWeighment(entry);
                    if (result > 0) {
                        Toast.makeText(getActivity(), "Entry #" + entry.getSerialNo() + " updated successfully",
                                Toast.LENGTH_SHORT).show();
                        clearAllFields();
                        refreshAllAdapters();
                        moveFocusToSaveButton();
                    } else {
                        Toast.makeText(getActivity(), "Error updating entry", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void refreshAllAdapters() {
        refreshVehicleNoAdapter();
        refreshVehicleTypeAdapter();
        refreshMaterialAdapter();
        refreshPartyAdapter();
    }

    private void moveFocusToSaveButton() {
        if (button4a != null) {
            button4a.post(() -> {
                button4a.requestFocus();
                Log.d("AFragment", "Focus moved to Save button");
            });
        }
    }

    private void moveFocusToPrintButton() {
        if (button5a != null) {
            button5a.post(() -> {
                button5a.requestFocus();
                Log.d("AFragment", "Focus moved to Print button");
            });
        }
    }

    public void performSaveAction() {
        if (isAdded() && getActivity() != null) {
            saveWeighmentEntry();
        }
    }

    public void performPrintAction() {
        if (isAdded() && getActivity() != null) {
            printWeighmentEntry();
        }
    }

    private void printUsingAndroidPrintFramework(WeighmentEntry entry) {
        PrintHelper printHelper = new PrintHelper(getActivity());
        printHelper.setScaleMode(PrintHelper.SCALE_MODE_FIT);

        TextView printView = new TextView(getActivity());
        printView.setText(buildPrintText(entry));
        printView.setTextSize(12);
        printView.setPadding(50, 50, 50, 50);

        printHelper.printBitmap("weighment_" + entry.getSerialNo(),
                loadBitmapFromView(printView));
    }

    private String buildPrintHtml(WeighmentEntry entry) {
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault());
        String dateTime = sdf.format(new Date());

        return "<html><body style='font-family: monospace; padding: 20px;'>" +
                "<h2 style='text-align: center;'>WEIGHMENT TICKET</h2><hr>" +
                "<p><b>Slip No:</b> " + entry.getSerialNo() + "</p>" +
                "<p><b>Date/Time:</b> " + dateTime + "</p><hr>" +
                "<p><b>Vehicle No:</b> " + entry.getVehicleNo() + "</p>" +
                "<p><b>Vehicle Type:</b> " + entry.getVehicleType() + "</p>" +
                "<p><b>Material:</b> " + entry.getMaterial() + "</p>" +
                "<p><b>Party:</b> " + entry.getParty() + "</p>" +
                "<p><b>Charge:</b> " + entry.getCharge() + "</p><hr>" +
                "<p><b>Gross Weight:</b> " + entry.getGross() + " kg</p>" +
                "<p><b>Tare Weight:</b> " + entry.getTare() + " kg</p>" +
                (entry.getManualTare() != null && !entry.getManualTare().isEmpty() && !entry.getManualTare().equals("0") ?
                        "<p><b>Manual Tare:</b> " + entry.getManualTare() + " kg</p>" : "") +
                "<hr><h3>NET WEIGHT: " + entry.getNet() + " kg</h3><hr></body></html>";
    }

    /**
     * Build plain text print content with PCL-like formatting
     */
    private String buildPrintText(WeighmentEntry entry) {
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault());
        String dateTime = sdf.format(new Date());

        // Load the three fields from SharedPreferences
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

        // Top margin
        ticket.append("\n\n");

        // ===== FIELD 1 - Centered and prominent =====
        ticket.append(centeredField1).append("\n");

        // ===== FIELD 2 - Centered =====
        ticket.append(centeredField2).append("\n");

        // ===== FIELD 3 - Centered =====
        ticket.append(centeredField3).append("\n");
        ticket.append(repeat("=", lineLength)).append("\n");
        ticket.append("\n");

        // ===== TICKET DETAILS =====
        ticket.append(String.format("%-20s %s\n", "Ticket #:", entry.getSerialNo()));
        ticket.append(String.format("%-20s %s\n", "Date:", dateTime));
        ticket.append(repeat("-", lineLength)).append("\n");

        // ===== VEHICLE DETAILS =====
        ticket.append("VEHICLE DETAILS:\n");
        ticket.append(String.format("  %-16s %s\n", "Vehicle No:", entry.getVehicleNo()));
        ticket.append(String.format("  %-16s %s\n", "Type:", entry.getVehicleType()));
        ticket.append(String.format("  %-16s %s\n", "Material:", entry.getMaterial()));
        ticket.append(String.format("  %-16s %s\n", "Party:", entry.getParty()));

        if (entry.getCharge() != null && !entry.getCharge().isEmpty()) {
            ticket.append(String.format("  %-16s %s\n", "Charge:", entry.getCharge()));
        }
        ticket.append(repeat("-", lineLength)).append("\n");

        // ===== WEIGHT DETAILS =====
        ticket.append("WEIGHT DETAILS:\n");
        ticket.append("\n");
        ticket.append(String.format("  %-16s %10s kg\n", "Gross Weight:", grossStr));
        ticket.append(String.format("  %-16s %10s kg\n", "Tare Weight:", tareStr));
        ticket.append("  " + repeat("-", lineLength - 4)).append("\n");
        ticket.append(String.format("  %-16s %10s kg\n", "NET WEIGHT:", netStr));
        ticket.append("  " + repeat("-", lineLength - 4)).append("\n\n");

        // ===== SIGNATURE AREA =====
        ticket.append(repeat("-", lineLength)).append("\n");
        ticket.append(String.format("Operator: %s\n", getOperatorName()));
        ticket.append("\n");
        ticket.append(String.format("Signature: %s\n", "__________________"));
        ticket.append("\n");

        // ===== FOOTER - Centered =====
        String thankYou = centerText("***** THANK YOU *****", lineLength);
        String generated = centerText("*** This is computer generated ***", lineLength);
        String signature = centerText("*** No signature required ***", lineLength);

        ticket.append(thankYou).append("\n");
        ticket.append(generated).append("\n");
        ticket.append(signature).append("\n");
        ticket.append("\n");

        return ticket.toString();
    }

    private void showPrintPreviewDialog(String printContent, WeighmentEntry entry) {
        new AlertDialog.Builder(getActivity())
                .setTitle("Print Preview - Entry #" + entry.getSerialNo())
                .setMessage(printContent)
                .setPositiveButton("OK", null)
                .show();
    }

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

    @Override
    public void onDestroyView() {
        super.onDestroyView();
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    @Override
    public void onResume() {
        super.onResume();
        generateNextSerialNumber();
        refreshAllAdapters();

        if (mainActivity != null) {
            updatePrinterStatus(mainActivity.isPrinterConnected(), mainActivity.getConnectedPrinterName());
        }
    }
}