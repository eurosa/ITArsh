package com.googleapi.bluetoothweight;

import androidx.appcompat.widget.AppCompatButton;
import androidx.fragment.app.Fragment;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.hardware.usb.UsbDevice;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.print.PrintAttributes;
import android.print.PrintManager;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.print.PrintHelper;

import com.googleapi.bluetoothweight.nokoprint.NokoPrintDirectPrinter;

import java.io.File;
import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
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

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_a, container, false);

        // Initialize DatabaseHelper
        databaseHelper = new DatabaseHelper(getActivity());
        mainActivity = (MainActivity) getActivity();

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
        // Set click listener
        //buttonG.setOnClickListener(v -> printWeighment());
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
        // For Vehicle Number - add new entries to master data
        vehicleNoSpinner.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                String selected = (String) parent.getItemAtPosition(position);
                if (!selected.equals("All") && !databaseHelper.isMasterDataExists(DatabaseHelper.TYPE_VEHICLE_NO, selected)) {
                    databaseHelper.insertMasterData(DatabaseHelper.TYPE_VEHICLE_NO, selected);
                }
            }
        });

        // For Vehicle Type
        vehicleTypeSpinner.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                String selected = (String) parent.getItemAtPosition(position);
                if (!selected.equals("All") && !databaseHelper.isMasterDataExists(DatabaseHelper.TYPE_VEHICLE_TYPE, selected)) {
                    databaseHelper.insertMasterData(DatabaseHelper.TYPE_VEHICLE_TYPE, selected);
                }
            }
        });

        // For Material
        materialSpinner.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                String selected = (String) parent.getItemAtPosition(position);
                if (!selected.equals("All") && !databaseHelper.isMasterDataExists(DatabaseHelper.TYPE_MATERIAL, selected)) {
                    databaseHelper.insertMasterData(DatabaseHelper.TYPE_MATERIAL, selected);
                }
            }
        });

        // For Party
        partySpinner.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                String selected = (String) parent.getItemAtPosition(position);
                if (!selected.equals("All") && !databaseHelper.isMasterDataExists(DatabaseHelper.TYPE_PARTY, selected)) {
                    databaseHelper.insertMasterData(DatabaseHelper.TYPE_PARTY, selected);
                }
            }
        });

        // Add focus change listeners to update dropdown data when field gains focus
        vehicleNoSpinner.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (hasFocus) {
                    refreshVehicleNoAdapter();
                }
            }
        });

        vehicleTypeSpinner.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (hasFocus) {
                    refreshVehicleTypeAdapter();
                }
            }
        });

        materialSpinner.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (hasFocus) {
                    refreshMaterialAdapter();
                }
            }
        });

        partySpinner.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (hasFocus) {
                    refreshPartyAdapter();
                }
            }
        });
    }

    // Methods to refresh adapters
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

    /**
     * Set up Enter key navigation for fields
     */
    private void setupEnterKeyNavigation() {
        // List of fields in order
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

        // Set OnEditorActionListener for each field
        for (View field : fields) {
            if (field != null) {
                field.setOnKeyListener(new View.OnKeyListener() {
                    @Override
                    public boolean onKey(View v, int keyCode, KeyEvent event) {
                        if (keyCode == KeyEvent.KEYCODE_ENTER && event.getAction() == KeyEvent.ACTION_DOWN) {
                            // Find the next field
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
                    }
                });
            }
        }

        // Handle Save button's Enter key to move to Print button
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
        // Build the text to print
        String vehicleNo = vehicleNoSpinner.getText().toString().trim();
        String gross = grossEditText.getText().toString().trim();
        String tare = tareEditText.getText().toString().trim();
        String net = netWeightEditText.getText().toString().trim();

        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault());
        String dateTime = sdf.format(new Date());

        String printText =
                "================================\n" +
                        "     WEIGHMENT TICKET\n" +
                        "================================\n" +
                        "Vehicle No: " + vehicleNo + "\n" +
                        "Gross: " + gross + " kg\n" +
                        "Tare: " + tare + " kg\n" +
                        "Net: " + net + " kg\n" +
                        "================================\n" +
                        "Date: " + dateTime + "\n" +
                        "================================\n";

        // Get PrintManager
        PrintManager printManager = (PrintManager) requireActivity()
                .getSystemService(Context.PRINT_SERVICE);

        // Create print adapter with the text
        MyPrintDocumentAdapter printAdapter = new MyPrintDocumentAdapter(requireContext(), printText);

        // Start print job
        String jobName = getString(R.string.app_name) + " - Weighment Ticket #" + vehicleNo;
        printManager.print(jobName, printAdapter, new PrintAttributes.Builder().build());
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
        buttonT.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mainActivity != null && mainActivity.txtCounter != null) {
                    String textWithSpaces = mainActivity.txtCounter.getText().toString();
                    String textWithoutSpaces = textWithSpaces.replaceAll("\\s", "");
                    tareEditText.setText(textWithoutSpaces);
                }
            }
        });

        buttonG.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mainActivity != null && mainActivity.txtCounter != null) {
                    String textWithSpaces = mainActivity.txtCounter.getText().toString();
                    String textWithoutSpaces = textWithSpaces.replaceAll("\\s", "");
                    grossEditText.setText(textWithoutSpaces);
                }
            }
        });
    }

    /**
     * Toggle manual EditText enabled/disabled state
     */
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

    public AppCompatButton getButtonT() {
        return buttonT;
    }

    public AppCompatButton getButtonG() {
        return buttonG;
    }

    public AppCompatButton getButton4a() {
        return button4a;
    }

    public AppCompatButton getButton5a() {
        return button5a;
    }

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

    public void performGButtonActionClear() {
        clearAllFields();
    }

    public void performMButtonAction() {
        if (grossEditText.getText() != null &&
                !grossEditText.getText().toString().trim().isEmpty()) {
            toggleManualEditText();
        }
    }

    private void setupButtonFocusListeners() {
        // Setup focus change listener for button T
        if (buttonT != null) {
            buttonT.setOnFocusChangeListener(new View.OnFocusChangeListener() {
                @Override
                public void onFocusChange(View v, boolean hasFocus) {
                    if (hasFocus) {
                        buttonT.setBackgroundColor(Color.parseColor("#FFA500"));
                        buttonT.setTextColor(Color.WHITE);
                    } else {
                        buttonT.setBackgroundColor(Color.parseColor("#808080"));
                        buttonT.setTextColor(Color.BLACK);
                    }
                }
            });
        }

        // Setup focus change listener for button G
        if (buttonG != null) {
            buttonG.setOnFocusChangeListener(new View.OnFocusChangeListener() {
                @Override
                public void onFocusChange(View v, boolean hasFocus) {
                    if (hasFocus) {
                        buttonG.setBackgroundColor(Color.parseColor("#00FF00"));
                        buttonG.setTextColor(Color.WHITE);
                    } else {
                        buttonG.setBackgroundColor(Color.parseColor("#808080"));
                        buttonG.setTextColor(Color.BLACK);
                    }
                }
            });
        }

        // Setup for button4a (Save button)
        if (button4a != null) {
            button4a.setOnFocusChangeListener(new View.OnFocusChangeListener() {
                @Override
                public void onFocusChange(View v, boolean hasFocus) {
                    if (hasFocus) {
                        button4a.setBackgroundColor(Color.parseColor("#00FF00"));
                        button4a.setTextColor(Color.WHITE);
                    } else {
                        button4a.setBackgroundColor(Color.parseColor("#808080"));
                        button4a.setTextColor(Color.BLACK);
                    }
                }
            });

            button4a.setOnKeyListener(new View.OnKeyListener() {
                @Override
                public boolean onKey(View v, int keyCode, KeyEvent event) {
                    if (event.getAction() == KeyEvent.ACTION_DOWN) {
                        if (keyCode == KeyEvent.KEYCODE_ENTER || keyCode == KeyEvent.KEYCODE_NUMPAD_ENTER) {
                            saveWeighmentEntry();
                            moveToNextFocus(v);
                            return true;
                        }
                    }
                    return false;
                }
            });
        }

        // Setup for button5a (Print button)
        if (button5a != null) {
            button5a.setOnFocusChangeListener(new View.OnFocusChangeListener() {
                @Override
                public void onFocusChange(View v, boolean hasFocus) {
                    if (hasFocus) {
                        button5a.setBackgroundColor(Color.parseColor("#00FF00"));
                        button5a.setTextColor(Color.WHITE);
                    } else {
                        button5a.setBackgroundColor(Color.parseColor("#808080"));
                        button5a.setTextColor(Color.BLACK);
                    }
                }
            });

            button5a.setOnKeyListener(new View.OnKeyListener() {
                @Override
                public boolean onKey(View v, int keyCode, KeyEvent event) {
                    if (event.getAction() == KeyEvent.ACTION_DOWN) {
                        if (keyCode == KeyEvent.KEYCODE_ENTER || keyCode == KeyEvent.KEYCODE_NUMPAD_ENTER) {
                            printWeighmentEntry();
                            moveToNextFocus(v);
                            return true;
                        }
                    }
                    return false;
                }
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
        button4a.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveWeighmentEntry();
            }
        });

        button5a.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                printWeighmentEntry();
            }
        });
    }

    private void saveWeighmentEntry() {
        // Get values from fields
        String serialNo = serialEditText.getText().toString().trim();
        String vehicleNo = vehicleNoSpinner.getText().toString().trim();
        String vehicleType = vehicleTypeSpinner.getText().toString().trim();
        String material = materialSpinner.getText().toString().trim();
        String party = partySpinner.getText().toString().trim();
        String charge = chargeEditText.getText().toString().trim();
        String gross = grossEditText.getText().toString().trim();
        String tare = tareEditText.getText().toString().trim();
        String manualTare = manualEditText.getText().toString().trim();

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

        // Add new values to master data if they don't exist
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

        // Check Gross Weight
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

        // Check Tare Weight
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

        // Check if at least one weight field has a value
        if (!hasValidGross && !hasValidTare) {
            Toast.makeText(getActivity(), "Please enter at least Gross weight or Tare weight",
                    Toast.LENGTH_LONG).show();
            grossEditText.requestFocus();
            return;
        }

        // Set default values for empty fields (treat as 0)
        String finalGross = gross.isEmpty() ? "0" : gross;
        String finalTare = tare.isEmpty() ? "0" : tare;

        // Create entry object
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
            // Show update confirmation
            showUpdateConfirmationDialog(entry);
        } else {
            long result = databaseHelper.insertWeighment(entry);
            if (result > 0) {
                Toast.makeText(getActivity(), "Entry #" + serialNo + " saved successfully",
                        Toast.LENGTH_SHORT).show();
                //printWeighmentEntry();
                //printWithAndroidFramework();
                //printWithAndroidFramework1();



                //printUsingAndroidPrintFramework(entry);

                printWeighmentEntry();

                clearAllFields();
                refreshAllAdapters();
                moveFocusToPrintButton();
            } else {
                Toast.makeText(getActivity(), "Error saving entry", Toast.LENGTH_SHORT).show();
            }
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
        String manualTare = manualEditText.getText().toString().trim();

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
        entry.setManualTare(manualTare);
        entry.calculateNet();

        // Show print options dialog
        showPrintOptionsDialog(entry);
    }

    /**
     * Show print options dialog with PCL support
     */
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

    /**
     * Print with PCL (HP LaserJet)
     */
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
                        clearAllFields();
                        refreshAllAdapters();
                        moveFocusToSaveButton();
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
                        clearAllFields();
                        refreshAllAdapters();
                        moveFocusToSaveButton();
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
     * Helper method to repeat strings
     */
    private String repeat(String str, int count) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < count; i++) {
            sb.append(str);
        }
        return sb.toString();
    }
    /*private void printWeighmentEntry() {
        // Get values from fields
        String serialNo = serialEditText.getText().toString().trim();
        String vehicleNo = vehicleNoSpinner.getText().toString().trim();
        String vehicleType = vehicleTypeSpinner.getText().toString().trim();
        String material = materialSpinner.getText().toString().trim();
        String party = partySpinner.getText().toString().trim();
        String charge = chargeEditText.getText().toString().trim();
        String gross = grossEditText.getText().toString().trim();
        String tare = tareEditText.getText().toString().trim();
        String manualTare = manualEditText.getText().toString().trim();

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
        entry.setManualTare(manualTare);
        entry.calculateNet();

        // Show printing options dialog
        showPrintOptionsDialog(entry);
    }*/

    /**
     * Show printing options dialog
     */
 /*   private void showPrintOptionsDialog(WeighmentEntry entry) {
        String[] options = {
                "USB Printer",
                "Bluetooth Printer",
                "NokoPrint App",
                "Android Print",
                "Auto Detect"
        };

        new AlertDialog.Builder(requireContext())
                .setTitle("Select Print Method")
                .setItems(options, (dialog, which) -> {
                    // Show progress
                    ProgressDialog progressDialog = new ProgressDialog(getActivity());
                    progressDialog.setMessage("Printing...\nPlease wait");
                    progressDialog.setCancelable(false);
                    progressDialog.show();

                    // Create printer instance
                    WeighmentTicketPrinter printer = new WeighmentTicketPrinter(requireContext());

                    // Select print method
                    WeighmentTicketPrinter.PrinterType type;
                    switch (which) {
                        case 0:
                            type = WeighmentTicketPrinter.PrinterType.USB;
                            break;
                        case 1:
                            type = WeighmentTicketPrinter.PrinterType.BLUETOOTH;
                            break;
                        case 2:
                            type = WeighmentTicketPrinter.PrinterType.NOKOPRINT;
                            break;
                        case 3:
                            type = WeighmentTicketPrinter.PrinterType.ANDROID_PRINT;
                            break;
                        case 4:
                        default:
                            // Auto detect
                            printer.autoPrintWeighmentEntry(entry, new WeighmentTicketPrinter.PrintCallback() {
                                @Override
                                public void onSuccess(String message) {
                                    handlePrintSuccess(progressDialog, message);
                                }

                                @Override
                                public void onError(String error) {
                                    handlePrintError(progressDialog, error, entry);
                                }

                                @Override
                                public void onProgress(String status) {
                                    updatePrintProgress(progressDialog, status);
                                }
                            });
                            return;
                    }

                    // Print with selected method
                    printer.printWeighmentEntry(entry, type, new WeighmentTicketPrinter.PrintCallback() {
                        @Override
                        public void onSuccess(String message) {
                            handlePrintSuccess(progressDialog, message);
                        }

                        @Override
                        public void onError(String error) {
                            handlePrintError(progressDialog, error, entry);
                        }

                        @Override
                        public void onProgress(String status) {
                            updatePrintProgress(progressDialog, status);
                        }
                    });
                })
                .setNegativeButton("Cancel", null)
                .show();
    }*/

    /**
     * Handle print success
     */
    private void handlePrintSuccess(ProgressDialog progressDialog, String message) {
        if (getActivity() != null) {
            getActivity().runOnUiThread(() -> {
                progressDialog.dismiss();
                Toast.makeText(getActivity(), "✅ " + message, Toast.LENGTH_LONG).show();

                // Clear fields after successful print
                clearAllFields();
                refreshAllAdapters();
                moveFocusToSaveButton();
            });
        }
    }

    /**
     * Handle print error
     */
    private void handlePrintError(ProgressDialog progressDialog, String error, WeighmentEntry entry) {
        if (getActivity() != null) {
            getActivity().runOnUiThread(() -> {
                progressDialog.dismiss();

                new AlertDialog.Builder(requireContext())
                        .setTitle("Print Failed")
                        .setMessage(error + "\n\nWould you like to try another method?")
                        .setPositiveButton("Try Again", (d, w) -> {
                            showPrintOptionsDialog(entry);
                        })
                        .setNegativeButton("Cancel", null)
                        .show();
            });
        }
    }

    /**
     * Update print progress
     */
    private void updatePrintProgress(ProgressDialog progressDialog, String status) {
        if (getActivity() != null) {
            getActivity().runOnUiThread(() -> {
                progressDialog.setMessage("Printing...\n" + status);
            });
        }
    }
  /*  private void printWithAndroidFramework1() {
        // Get values from form
        String vehicleNo = vehicleNoSpinner.getText().toString().trim();
        String gross = grossEditText.getText().toString().trim();
        String tare = tareEditText.getText().toString().trim();
        String net = netWeightEditText.getText().toString().trim();
        String party = partySpinner.getText().toString().trim();
        String material = materialSpinner.getText().toString().trim();

        if (vehicleNo.isEmpty()) {
            Toast.makeText(getActivity(), "Enter vehicle number", Toast.LENGTH_SHORT).show();
            return;
        }

        // Get PrintManager
        PrintManager printManager = (PrintManager) requireActivity()
                .getSystemService(Context.PRINT_SERVICE);

        // Create print adapter
        WeighmentPrintAdapter printAdapter = new WeighmentPrintAdapter(
                requireContext(), vehicleNo, gross, tare, net, party, material);

        // Build print attributes
        PrintAttributes.Builder builder = new PrintAttributes.Builder();

        // Set media size (optional - can use default)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            builder.setMediaSize(PrintAttributes.MediaSize.ISO_A4);
            builder.setMinMargins(PrintAttributes.Margins.NO_MARGINS);
        }

        // Set color mode
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            builder.setColorMode(PrintAttributes.COLOR_MODE_MONOCHROME);
        }

        // Start print job
        String jobName = getString(R.string.app_name) + " - Ticket " + vehicleNo;
        printManager.print(jobName, printAdapter, builder.build());

        // Show instruction toast
        Toast.makeText(getActivity(),
                "📄 Select your Canon printer from the print dialog",
                Toast.LENGTH_LONG).show();
    }*/

 /*   private void printWeighmentEntry() {
        // Get values from fields
        String serialNo = serialEditText.getText().toString().trim();
        String vehicleNo = vehicleNoSpinner.getText().toString().trim();
        String vehicleType = vehicleTypeSpinner.getText().toString().trim();
        String material = materialSpinner.getText().toString().trim();
        String party = partySpinner.getText().toString().trim();
        String charge = chargeEditText.getText().toString().trim();
        String gross = grossEditText.getText().toString().trim();
        String tare = tareEditText.getText().toString().trim();
        String manualTare = manualEditText.getText().toString().trim();

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
        entry.setManualTare(manualTare);
        entry.calculateNet();

        String printContent = buildPrintText(entry);

        if (mainActivity != null && mainActivity.isPrinterConnected()) {
            PrinterManager printerManager = mainActivity.getPrinterManager();

            // Show progress dialog
           //ProgressDialog progressDialog = new ProgressDialog(getActivity());
          //  progressDialog.setMessage("Printing...\nPlease wait");
           // progressDialog.setCancelable(false);
           // progressDialog.show();

            // Get PrintManager
            PrintManager printManager = (PrintManager) requireActivity()
                    .getSystemService(Context.PRINT_SERVICE);

            // Create print adapter with the text
            MyPrintDocumentAdapter printAdapter = new MyPrintDocumentAdapter(requireContext(), printContent);

            // Start print job
            String jobName = getString(R.string.app_name) + " - Weighment Ticket #" + vehicleNo;
            printManager.print(jobName, printAdapter, new PrintAttributes.Builder().build());

            // Create listener
            PrinterManager.PrinterConnectionListener listener = new PrinterManager.PrinterConnectionAdapter() {
                @Override
                public void onPrintSuccess() {
                    if (getActivity() != null) {
                        getActivity().runOnUiThread(() -> {
                            progressDialog.dismiss();
                            Toast.makeText(getActivity(), "✅ Print successful", Toast.LENGTH_SHORT).show();
                        });
                    }
                }

                @Override
                public void onPrintError(String error) {
                    if (getActivity() != null) {
                        getActivity().runOnUiThread(() -> {
                            progressDialog.dismiss();
                            Toast.makeText(getActivity(), "❌ Print error: " + error, Toast.LENGTH_LONG).show();
                            // Fallback to Android print
                            printUsingAndroidPrintFramework(entry);
                        });
                    }
                }
            };

            // Try auto-detect first (async)
            printerManager.autoDetectAndPrintAsync(printContent, listener);

        } else {
            printUsingAndroidPrintFramework(entry);
        }

        clearAllFields();
        refreshAllAdapters();
        moveFocusToSaveButton();
    }*/

    private void showUpdateConfirmationDialog(WeighmentEntry entry) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle("Update Entry");
        builder.setMessage("Serial #" + entry.getSerialNo() + " already exists. Do you want to update it?");
        builder.setPositiveButton("Update", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
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
            }
        });
        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    private void refreshAllAdapters() {
        refreshVehicleNoAdapter();
        refreshVehicleTypeAdapter();
        refreshMaterialAdapter();
        refreshPartyAdapter();
    }

    private void moveFocusToSaveButton() {
        if (button4a != null) {
            button4a.post(new Runnable() {
                @Override
                public void run() {
                    button4a.requestFocus();
                    Log.d("AFragment", "Focus moved to Save button");
                }
            });
        }
    }

    private void moveFocusToPrintButton() {
        if (button5a != null) {
            button5a.post(new Runnable() {
                @Override
                public void run() {
                    button5a.requestFocus();
                    Log.d("AFragment", "Focus moved to Print button");
                }
            });
        }
    }

    /**
     * Public method to trigger save action from MainActivity
     */
    public void performSaveAction() {
        if (isAdded() && getActivity() != null) {
            saveWeighmentEntry();
        }
    }

    /**
     * Public method to trigger print action from MainActivity
     */
    public void performPrintAction() {
        if (isAdded() && getActivity() != null) {
            printWeighmentEntry();
        }
    }

    private void printUsingAndroidPrintFramework(WeighmentEntry entry) {
        // Build print content as HTML or bitmap
        String htmlContent = buildPrintHtml(entry);

        // Create a print job
        PrintHelper printHelper = new PrintHelper(getActivity());
        printHelper.setScaleMode(PrintHelper.SCALE_MODE_FIT);

        // Create a bitmap from the content (simplified - you may want to create a proper view)
        // This is a simplified example - you might want to create a proper layout
        TextView printView = new TextView(getActivity());
        printView.setText(buildPrintText(entry));
        printView.setTextSize(12);
        printView.setPadding(50, 50, 50, 50);

        // Print the view
        printHelper.printBitmap("weighment_" + entry.getSerialNo(),
                loadBitmapFromView(printView));
    }

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
        // No need to disconnect printer here as it's managed by MainActivity
    }

    @Override
    public void onPause() {
        super.onPause();
        // No need to unregister receiver here as it's managed by MainActivity
    }

    @Override
    public void onResume() {
        super.onResume();
        generateNextSerialNumber();
        refreshAllAdapters();

        // Check printer status from MainActivity
        if (mainActivity != null) {
            updatePrinterStatus(mainActivity.isPrinterConnected(), mainActivity.getConnectedPrinterName());
        }
    }
}