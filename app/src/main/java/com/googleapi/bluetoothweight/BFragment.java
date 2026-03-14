package com.googleapi.bluetoothweight;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
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
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.webkit.WebView;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
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
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class BFragment extends Fragment {

    private AppCompatButton button4a;
    private EditText serialEditText, chargeEditText, grossEditText, tareEditText, netWeightEditText;

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

    // Flag to track if current entry is finalized (non-editable)
    private boolean isEntryFinalized = false;

    // Store the current serial number being viewed
    private String currentSerialNo = "";

    // Adapters for dropdowns
    private ArrayAdapter<String> vehicleNoAdapter;
    private ArrayAdapter<String> vehicleTypeAdapter;
    private ArrayAdapter<String> materialAdapter;
    private ArrayAdapter<String> partyAdapter;

    // WiFi Printer Helper
    private WiFiPrinterHelper wifiPrinterHelper;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_b, container, false);

        // Initialize DatabaseHelper
        databaseHelper = new DatabaseHelper(getActivity());
        mainActivity = (MainActivity) getActivity();

        // Initialize WiFi Printer Helper
        wifiPrinterHelper = new WiFiPrinterHelper(requireContext());

        // Initialize views
        initViews(view);

        // Setup dropdown adapters
        setupDropdownAdapters();

        // Setup listeners
        setupCalculationListeners();
        setupSearchFunctionality();
        setupGrossTareFunctionality();
        setupActionButtons();
        setupButtonFocusListeners();
        setupTGMButtons();

        disableGrossEditText();
        disableTareEditText();
        disableNetWeightEditText();

        return view;
    }

    private void initViews(View view) {
        // Search field
        searchSerialEditText = view.findViewById(R.id.searchSerialEditText);

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

        button4a.setText("Finalize");

        // Make serialEditText non-editable (will be populated from search)
        serialEditText.setFocusable(false);
        serialEditText.setClickable(false);

        // Initially disable all form fields until search
        setFormFieldsEnabled(false);
        isEntryFinalized = false;

        // Setup focus for buttons
        button4a.setFocusable(true);
        button4a.setFocusableInTouchMode(true);

        // Set IME options
        searchSerialEditText.setImeOptions(EditorInfo.IME_ACTION_SEARCH);
        grossEditText.setImeOptions(EditorInfo.IME_ACTION_NEXT);
        tareEditText.setImeOptions(EditorInfo.IME_ACTION_DONE);
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
        // For Vehicle Number
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
                if (hasFocus && !isEntryFinalized) {
                    refreshVehicleNoAdapter();
                }
            }
        });

        vehicleTypeSpinner.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (hasFocus && !isEntryFinalized) {
                    refreshVehicleTypeAdapter();
                }
            }
        });

        materialSpinner.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (hasFocus && !isEntryFinalized) {
                    refreshMaterialAdapter();
                }
            }
        });

        partySpinner.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (hasFocus && !isEntryFinalized) {
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

    private void refreshAllAdapters() {
        refreshVehicleNoAdapter();
        refreshVehicleTypeAdapter();
        refreshMaterialAdapter();
        refreshPartyAdapter();
    }

    public void performGButtonActionClear() {
        clearAllFields();
    }

    private void clearAllFields() {
        vehicleNoSpinner.setText("");
        vehicleTypeSpinner.setText("");
        materialSpinner.setText("");
        partySpinner.setText("");
        chargeEditText.setText("");
        grossEditText.setText("");
        tareEditText.setText("");
        netWeightEditText.setText("");

        disableGrossEditText();
        disableTareEditText();
        disableNetWeightEditText();
    }

    private void setFormFieldsEnabled(boolean enabled) {
        // Only enable/disable form fields based on finalized status
        // If entry is finalized, fields should be disabled even if enabled=true is passed
        boolean finalEnabled = enabled && !isEntryFinalized;

        vehicleNoSpinner.setEnabled(finalEnabled);
        vehicleTypeSpinner.setEnabled(finalEnabled);
        materialSpinner.setEnabled(finalEnabled);
        partySpinner.setEnabled(finalEnabled);
        chargeEditText.setEnabled(finalEnabled);

        vehicleNoSpinner.setFocusable(finalEnabled);
        vehicleNoSpinner.setFocusableInTouchMode(finalEnabled);
        vehicleTypeSpinner.setFocusable(finalEnabled);
        vehicleTypeSpinner.setFocusableInTouchMode(finalEnabled);
        materialSpinner.setFocusable(finalEnabled);
        materialSpinner.setFocusableInTouchMode(finalEnabled);
        partySpinner.setFocusable(finalEnabled);
        partySpinner.setFocusableInTouchMode(finalEnabled);
        chargeEditText.setFocusable(finalEnabled);
        chargeEditText.setFocusableInTouchMode(finalEnabled);

        float alpha = finalEnabled ? 1.0f : 0.6f;
        vehicleNoSpinner.setAlpha(alpha);
        vehicleTypeSpinner.setAlpha(alpha);
        materialSpinner.setAlpha(alpha);
        partySpinner.setAlpha(alpha);
        chargeEditText.setAlpha(alpha);
        grossEditText.setAlpha(alpha);
        tareEditText.setAlpha(alpha);
        netWeightEditText.setAlpha(alpha);

        // Update button states
        button4a.setEnabled(!isEntryFinalized);
        button4a.setAlpha(isEntryFinalized ? 0.5f : 1.0f);

        // Update button text based on state
        if (isEntryFinalized) {
            button4a.setText("FINALIZED");
        } else {
            button4a.setText("Finalize");
        }
    }

    /**
     * Get the current value from MainActivity's txtCounter
     */
    private String getCounterValue() {
        if (mainActivity == null) {
            Log.e("BFragment", "mainActivity is null in getCounterValue");
            return "";
        }
        if (mainActivity.txtCounter == null) {
            Log.e("BFragment", "mainActivity.txtCounter is null in getCounterValue");
            return "";
        }
        String value = mainActivity.txtCounter.getText().toString();
        String cleaned = value.replaceAll("\\s", "");
        Log.d("BFragment", "getCounterValue - original: '" + value + "', cleaned: '" + cleaned + "'");
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
            Log.d("BFragment", "Search field editor action: " + actionId);

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
                Log.d("BFragment", "Physical Enter on search field");
                loadEntry();
                return true;
            }
            return false;
        });
    }

    /**
     * Setup gross and tare fields with Enter key functionality
     */
    private void setupGrossTareFunctionality() {
        // Gross field - should trigger G action on Enter, then move to tare
        setupEnterKeyHandler(grossEditText, "gross", true);

        // Tare field - should trigger T action on Enter, then move to search field
        setupEnterKeyHandler(tareEditText, "tare", false);
    }

    /**
     * Simple Enter key handler for EditText fields
     */
    private void setupEnterKeyHandler(EditText editText, String fieldName, boolean isGross) {
        if (editText == null) return;

        // Clear any existing listeners
        editText.setOnEditorActionListener(null);
        editText.setOnKeyListener(null);

        // Handle physical Enter key press
        editText.setOnKeyListener((v, keyCode, event) -> {
            if (event.getAction() == KeyEvent.ACTION_DOWN) {
                if (keyCode == KeyEvent.KEYCODE_ENTER || keyCode == KeyEvent.KEYCODE_NUMPAD_ENTER) {
                    Log.d("BFragment", "Physical Enter pressed on " + fieldName + " field");

                    // Only process if entry is not finalized and field is enabled
                    if (!isEntryFinalized && editText.isEnabled()) {
                        String newValue = getCounterValue();
                        if (!newValue.isEmpty()) {
                            if (isGross) {
                                handleGButtonAction(newValue);
                            } else {
                                handleTButtonAction(newValue);
                            }
                        } else {
                            Toast.makeText(getActivity(), "No value from scale", Toast.LENGTH_SHORT).show();
                        }

                        // Move to next field after processing
                        moveToNextFieldAfterGrossTare(isGross);

                    } else if (isEntryFinalized) {
                        Toast.makeText(getActivity(), "Entry is finalized", Toast.LENGTH_SHORT).show();
                        // Even if finalized, allow navigation back to search
                        moveToNextFieldAfterGrossTare(isGross);
                    }
                    return true; // Consume the event
                }
            }
            return false;
        });

        // Handle soft keyboard IME actions
        editText.setOnEditorActionListener((v, actionId, event) -> {
            Log.d("BFragment", fieldName + " field IME action: " + actionId);

            if (actionId == EditorInfo.IME_ACTION_DONE ||
                    actionId == EditorInfo.IME_ACTION_NEXT ||
                    actionId == EditorInfo.IME_ACTION_GO) {

                if (!isEntryFinalized && editText.isEnabled()) {
                    String newValue = getCounterValue();
                    if (!newValue.isEmpty()) {
                        if (isGross) {
                            handleGButtonAction(newValue);
                        } else {
                            handleTButtonAction(newValue);
                        }
                    } else {
                        Toast.makeText(getActivity(), "No value from scale", Toast.LENGTH_SHORT).show();
                    }
                }

                // Always move to next field after processing
                moveToNextFieldAfterGrossTare(isGross);
                return true;
            }
            return false;
        });

        // Set appropriate IME options
        if (isGross) {
            editText.setImeOptions(EditorInfo.IME_ACTION_NEXT);
        } else {
            editText.setImeOptions(EditorInfo.IME_ACTION_DONE);
        }
    }

    /**
     * Helper method to move to next field after gross/tare processing
     * Returns focus to search field after tare
     */
    private void moveToNextFieldAfterGrossTare(boolean isGross) {
        if (isGross) {
            // From gross, move to tare if enabled
            if (tareEditText != null && tareEditText.isEnabled()) {
                tareEditText.requestFocus();
            } else if (searchSerialEditText != null) {
                // If tare is disabled, go back to search
                searchSerialEditText.requestFocus();
                searchSerialEditText.selectAll();
            }
        } else {
            // From tare, ALWAYS return to search field
            if (searchSerialEditText != null) {
                searchSerialEditText.requestFocus();
                searchSerialEditText.selectAll();
            }
        }
    }

    /**
     * Setup action buttons - Finalize returns to search
     */
    private void setupActionButtons() {
        button4a.setOnClickListener(v -> {
            if (!isEntryFinalized) {
                finalizeWeighmentEntry();
            } else {
                Toast.makeText(getActivity(), "Entry is already finalized", Toast.LENGTH_SHORT).show();
            }
        });

        // Handle Enter key on Finalize button
        button4a.setOnKeyListener((v, keyCode, event) -> {
            if (keyCode == KeyEvent.KEYCODE_ENTER && event.getAction() == KeyEvent.ACTION_DOWN) {
                if (!isEntryFinalized) {
                    finalizeWeighmentEntry();
                } else {
                    Toast.makeText(getActivity(), "Entry is already finalized", Toast.LENGTH_SHORT).show();
                    // Even if finalized, return to search
                    if (searchSerialEditText != null) {
                        searchSerialEditText.requestFocus();
                        searchSerialEditText.selectAll();
                    }
                }
                return true;
            }
            return false;
        });
    }

    private void enableGrossEditText() {
        grossEditText.setEnabled(true);
        grossEditText.setFocusable(true);
        grossEditText.setFocusableInTouchMode(true);
        grossEditText.setClickable(true);
        grossEditText.setAlpha(1.0f);
        grossEditText.requestFocus();

        InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.showSoftInput(grossEditText, InputMethodManager.SHOW_IMPLICIT);
    }

    private void disableGrossEditText() {
        grossEditText.setEnabled(false);
        grossEditText.setFocusable(false);
        grossEditText.setFocusableInTouchMode(false);
        grossEditText.setClickable(false);
        grossEditText.setAlpha(0.5f);

        InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(grossEditText.getWindowToken(), 0);
    }

    private void disableTareEditText() {
        tareEditText.setEnabled(false);
        tareEditText.setFocusable(false);
        tareEditText.setFocusableInTouchMode(false);
        tareEditText.setClickable(false);
        tareEditText.setAlpha(0.5f);

        InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(tareEditText.getWindowToken(), 0);
    }

    private void disableNetWeightEditText() {
        netWeightEditText.setEnabled(false);
        netWeightEditText.setFocusable(false);
        netWeightEditText.setFocusableInTouchMode(false);
        netWeightEditText.setClickable(false);
        netWeightEditText.setAlpha(0.5f);
    }

    private void loadEntry() {
        String searchSerial = searchSerialEditText.getText().toString().trim();
        Log.d("BFragment", "Loading entry: " + searchSerial);

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

            // Get finalized status from database
            isEntryFinalized = entry.isFinalized();

            // Enable form fields based on finalized status
            setFormFieldsEnabled(true);

            // Compare and set gross/tare values
            compareAndSetGrossTare(entry);

            if (!isEntryFinalized) {
                // Use postDelayed to ensure view is ready
                materialSpinner.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        materialSpinner.requestFocus();
                        InputMethodManager keyboard = (InputMethodManager) getActivity()
                                .getSystemService(Context.INPUT_METHOD_SERVICE);
                        keyboard.showSoftInput(materialSpinner, InputMethodManager.SHOW_IMPLICIT);
                    }
                }, 200);
            }

            if (isEntryFinalized) {
                Toast.makeText(getActivity(), "Entry #" + searchSerial + " loaded (VIEW ONLY - Finalized)", Toast.LENGTH_LONG).show();
                if (txtDisplayTwoView != null) {
                    txtDisplayTwoView.setText("FINALIZED: " + searchSerial + " | Net: " + entry.getNet() + " KG");
                }
            } else {
                Toast.makeText(getActivity(), "Entry #" + searchSerial + " loaded (Editable)", Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(getActivity(), "No entry found with Serial #" + searchSerial, Toast.LENGTH_SHORT).show();
            clearFormFields();
            setFormFieldsEnabled(false);
            isEntryFinalized = false;
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
            Log.e("BFragment", "Error parsing numbers", e);
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

                if (txtDisplayTwoView != null) {
                    txtDisplayTwoView.setText("Net: " + net + " KG");
                }
            } catch (NumberFormatException e) {
                Log.e("BFragment", "Error calculating net weight", e);
            }
        }
    }

    /**
     * Setup T and G buttons click functionality
     */
    private void setupTGMButtons() {
        buttonT.setOnClickListener(v -> {
            // Only allow if entry is not finalized
            if (!isEntryFinalized) {
                String newValue = getCounterValue();
                if (!newValue.isEmpty()) {
                    handleTButtonAction(newValue);
                } else {
                    Toast.makeText(getActivity(), "No value from scale", Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(getActivity(), "Entry is finalized, cannot modify", Toast.LENGTH_SHORT).show();
            }
        });

        buttonG.setOnClickListener(v -> {
            // Only allow if entry is not finalized
            if (!isEntryFinalized) {
                String newValue = getCounterValue();
                if (!newValue.isEmpty()) {
                    handleGButtonAction(newValue);
                } else {
                    Toast.makeText(getActivity(), "No value from scale", Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(getActivity(), "Entry is finalized, cannot modify", Toast.LENGTH_SHORT).show();
            }
        });
    }

    /**
     * Handle T button action (sets value to Tare with greater value logic)
     */
    private void handleTButtonAction(String newValue) {
        if (newValue.isEmpty() || isEntryFinalized) return;

        long newValueLong = Long.parseLong(newValue);

        String currentGrossStr = grossEditText.getText().toString().trim();
        String currentTareStr = tareEditText.getText().toString().trim();

        long currentGross = currentGrossStr.isEmpty() ? 0 : Long.parseLong(currentGrossStr);
        long currentTare = currentTareStr.isEmpty() ? 0 : Long.parseLong(currentTareStr);

        // Case 1: Both fields are empty
        if (currentGrossStr.isEmpty() && currentTareStr.isEmpty()) {
            grossEditText.setText(newValue);
            tareEditText.setText("");
            Toast.makeText(getActivity(), "Value set as Gross", Toast.LENGTH_SHORT).show();
        }
        // Case 2: Only Gross has value
        else if (!currentGrossStr.isEmpty() && currentTareStr.isEmpty()) {
            if (newValueLong > currentGross) {
                grossEditText.setText(newValue);
                tareEditText.setText(String.valueOf(currentGross));
                Toast.makeText(getActivity(), "New greater value set as Gross", Toast.LENGTH_SHORT).show();
            } else {
                tareEditText.setText(newValue);
                Toast.makeText(getActivity(), "Value set as Tare", Toast.LENGTH_SHORT).show();
            }
        }
        // Case 3: Only Tare has value
        else if (currentGrossStr.isEmpty() && !currentTareStr.isEmpty()) {
            if (newValueLong > currentTare) {
                grossEditText.setText(newValue);
                Toast.makeText(getActivity(), "Value set as Gross", Toast.LENGTH_SHORT).show();
            } else {
                grossEditText.setText(String.valueOf(currentTare));
                tareEditText.setText(newValue);
                Toast.makeText(getActivity(), "Greater value moved to Gross", Toast.LENGTH_SHORT).show();
            }
        }
        // Case 4: Both fields have values
        else {
            long greater = Math.max(currentGross, currentTare);
            long smaller = Math.min(currentGross, currentTare);

            if (newValueLong > greater) {
                grossEditText.setText(newValue);
                tareEditText.setText(String.valueOf(greater));
                Toast.makeText(getActivity(), "New greatest value set as Gross", Toast.LENGTH_SHORT).show();
            } else if (newValueLong > smaller) {
                grossEditText.setText(String.valueOf(greater));
                tareEditText.setText(newValue);
                Toast.makeText(getActivity(), "Value set as Tare", Toast.LENGTH_SHORT).show();
            } else {
                grossEditText.setText(String.valueOf(greater));
                tareEditText.setText(String.valueOf(smaller));
                Toast.makeText(getActivity(), "Value too small, ignored", Toast.LENGTH_SHORT).show();
                return;
            }
        }

        calculateNetWeight();
    }

    /**
     * Handle G button action (sets value to Gross with greater value logic)
     */
    private void handleGButtonAction(String newValue) {
        if (newValue.isEmpty() || isEntryFinalized) return;

        long newValueLong = Long.parseLong(newValue);

        String currentGrossStr = grossEditText.getText().toString().trim();
        String currentTareStr = tareEditText.getText().toString().trim();

        long currentGross = currentGrossStr.isEmpty() ? 0 : Long.parseLong(currentGrossStr);
        long currentTare = currentTareStr.isEmpty() ? 0 : Long.parseLong(currentTareStr);

        // Case 1: Both fields are empty
        if (currentGrossStr.isEmpty() && currentTareStr.isEmpty()) {
            grossEditText.setText(newValue);
            tareEditText.setText("");
            Toast.makeText(getActivity(), "Value set as Gross", Toast.LENGTH_SHORT).show();
        }
        // Case 2: Only Gross has value
        else if (!currentGrossStr.isEmpty() && currentTareStr.isEmpty()) {
            if (newValueLong > currentGross) {
                grossEditText.setText(newValue);
                Toast.makeText(getActivity(), "Gross updated with greater value", Toast.LENGTH_SHORT).show();
            } else {
                tareEditText.setText(newValue);
                Toast.makeText(getActivity(), "Value set as Tare", Toast.LENGTH_SHORT).show();
            }
        }
        // Case 3: Only Tare has value
        else if (currentGrossStr.isEmpty() && !currentTareStr.isEmpty()) {
            if (newValueLong > currentTare) {
                grossEditText.setText(newValue);
                Toast.makeText(getActivity(), "Value set as Gross", Toast.LENGTH_SHORT).show();
            } else {
                grossEditText.setText(String.valueOf(currentTare));
                tareEditText.setText(newValue);
                Toast.makeText(getActivity(), "Greater value moved to Gross", Toast.LENGTH_SHORT).show();
            }
        }
        // Case 4: Both fields have values
        else {
            long greater = Math.max(currentGross, currentTare);
            long smaller = Math.min(currentGross, currentTare);

            if (newValueLong >= greater) {
                grossEditText.setText(newValue);
                tareEditText.setText(String.valueOf(smaller));
                Toast.makeText(getActivity(), "Gross updated with greatest value", Toast.LENGTH_SHORT).show();
            } else {
                grossEditText.setText(String.valueOf(greater));
                tareEditText.setText(newValue);
                Toast.makeText(getActivity(), "Value set as Tare", Toast.LENGTH_SHORT).show();
            }
        }

        calculateNetWeight();
    }

    private void printWeighmentEntry(WeighmentEntry entry) {
        String[] options = {
                "USB/PCL Print",           // 0
                "Android Print Framework", // 1
                "WiFi Print",              // 2
                "NokoPrint App",           // 3
                "Auto Detect Best Method"  // 4
        };

        Log.d("BFragment", "Showing print options dialog");

        if (!isAdded() || getActivity() == null) {
            Log.e("BFragment", "Fragment not attached, cannot show dialog");
            return;
        }

        try {
            AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
            builder.setTitle("Select Print Method")
                    .setItems(options, (dialog, which) -> {
                        if (!isAdded() || getActivity() == null) return;

                        ProgressDialog progressDialog = new ProgressDialog(getActivity());
                        progressDialog.setMessage("Processing...");
                        progressDialog.setCancelable(true);
                        progressDialog.show();

                        ExecutorService executor = Executors.newSingleThreadExecutor();
                        executor.submit(() -> {
                            boolean result = false;
                            String method = "";

                            try {
                                switch (which) {
                                    case 0: // USB/PCL Print
                                        method = "USB/PCL";
                                        result = printWithPCL(entry);
                                        break;
                                    case 1: // Android Print Framework
                                        method = "Android Print";
                                        printUsingAndroidPrintFramework(entry);
                                        result = true;
                                        break;
                                    case 2: // WiFi Print
                                        method = "WiFi";
                                        result = printWithWiFi(entry);
                                        break;
                                    case 3: // NokoPrint App
                                        method = "NokoPrint";
                                        result = printWithNokoPrint(entry);
                                        break;
                                    case 4: // Auto Detect
                                        method = "Auto Detect";
                                        autoDetectAndPrint(entry);
                                        result = true;
                                        break;
                                }
                            } catch (Exception e) {
                                Log.e("BFragment", "Error in print method: " + e.getMessage());
                                e.printStackTrace();
                                result = false;
                            }

                            final boolean finalResult = result;
                            final String finalMethod = method;

                            if (getActivity() != null) {
                                getActivity().runOnUiThread(() -> {
                                    progressDialog.dismiss();
                                    if (finalResult) {
                                        Toast.makeText(getActivity(), "✅ " + finalMethod + " Successful",
                                                Toast.LENGTH_LONG).show();

                                        if (txtDisplayTwoView != null) {
                                            SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss", Locale.getDefault());
                                            txtDisplayTwoView.setText("PRINTED: " + entry.getSerialNo() +
                                                    " | Net: " + entry.getNet() + " KG at " + sdf.format(new Date()));
                                        }
                                    } else {
                                        Toast.makeText(getActivity(), "❌ " + finalMethod + " Failed",
                                                Toast.LENGTH_LONG).show();
                                    }
                                });
                            }
                        });
                        executor.shutdown();
                    })
                    .setNegativeButton("Cancel", null)
                    .show();

            Log.d("BFragment", "Print options dialog shown successfully");

        } catch (Exception e) {
            Log.e("BFragment", "Error showing print options dialog: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Print with PCL - Returns boolean success
     */
    private boolean printWithPCL(WeighmentEntry entry) {
        if (mainActivity == null || !mainActivity.isPrinterConnected()) {
            Toast.makeText(getActivity(), "USB Printer not connected", Toast.LENGTH_SHORT).show();
            return false;
        }

        PrinterManager printerManager = mainActivity.getPrinterManager();
        if (printerManager == null || printerManager.usbPrinterHelper == null) {
            return false;
        }

        try {
            String pclContent = buildPCLTicket(entry);
            if (pclContent == null || pclContent.isEmpty()) {
                return false;
            }

            boolean result = printerManager.usbPrinterHelper.printPCL(pclContent);
            Log.d("PRINT", "printWithPCL: Result = " + result);
            return result;

        } catch (Exception e) {
            Log.e("PRINT", "printWithPCL exception: " + e.getMessage());
            return false;
        }
    }

    /**
     * Print with WiFi - Returns boolean success
     */
    private boolean printWithWiFi(WeighmentEntry entry) {
        try {
            Log.d("PRINT", "printWithWiFi: Starting...");

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
            return true;

        } catch (Exception e) {
            Log.e("PRINT", "printWithWiFi exception: " + e.getMessage());
            return false;
        }
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
     * Auto detect best method and print
     */
    private void autoDetectAndPrint(WeighmentEntry entry) {
        Log.d("PRINT", "autoDetectAndPrint: Starting...");

        if (printWithPCL(entry)) {
            Log.d("PRINT", "autoDetectAndPrint: PCL succeeded");
            return;
        }

        if (printWithWiFi(entry)) {
            Log.d("PRINT", "autoDetectAndPrint: WiFi succeeded");
            return;
        }

        if (printWithNokoPrint(entry)) {
            Log.d("PRINT", "autoDetectAndPrint: NokoPrint succeeded");
            return;
        }

        Log.d("PRINT", "autoDetectAndPrint: Falling back to Android print");
        printUsingAndroidPrintFramework(entry);
    }

    /**
     * Build PCL formatted ticket
     */
    private String buildPCLTicket(WeighmentEntry entry) {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault());
            String dateTime = sdf.format(new Date());

            String grossStr = entry.getGross().isEmpty() ? "0" : entry.getGross();
            String tareStr = entry.getTare().isEmpty() ? "0" : entry.getTare();
            String netStr = entry.getNet().isEmpty() ? "0" : entry.getNet();

            SharedPreferences prefs = requireContext().getSharedPreferences("MyAppPrefs", Context.MODE_PRIVATE);
            String field1 = prefs.getString("field_0", "MY WEIGHBRIDGE COMPANY");
            String field2 = prefs.getString("field_1", "123 Industrial Area, City - 123456");
            String field3 = prefs.getString("field_2", "Phone: +91 9876543210");

            int lineLength = 40;
            String centeredField1 = centerText(field1, lineLength);
            String centeredField2 = centerText(field2, lineLength);
            String centeredField3 = centerText(field3, lineLength);

            StringBuilder pcl = new StringBuilder();

            pcl.append("\u001BE");                    // Reset printer
            pcl.append("\u001B&l1O");                  // Portrait orientation
            pcl.append("\u001B(s10H");                  // 10 pitch
            pcl.append("\u001B(s1Q");                   // Quality
            pcl.append("\u001B&l6D");                   // Vertical motion index
            pcl.append("\r\n\r\n");
            pcl.append("\r\n\r\n");
            pcl.append("\r\n\r\n");
            pcl.append("\r\n\r\n");
            pcl.append("\r\n\r\n");
            pcl.append("\r\n\r\n");
            pcl.append("\r\n\r\n");
            pcl.append("\r\n\r\n");
            pcl.append("\r\n\r\n");
            pcl.append("\r\n\r\n");
            pcl.append("\r\n\r\n");
            pcl.append("\r\n\r\n");
            pcl.append("\r\n\r\n");
            pcl.append("\r\n\r\n");
            pcl.append("\r\n\r\n");
            pcl.append("\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n");
            pcl.append("\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n");
            pcl.append("\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n");
            pcl.append("\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n");
            pcl.append("\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n");
            pcl.append("\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n");
            pcl.append("\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n");

            pcl.append(centeredField1).append("\r\n");
            pcl.append(centeredField2).append("\r\n");
            pcl.append(centeredField3).append("\r\n");
            pcl.append(repeat("=", lineLength)).append("\r\n");
            pcl.append("\r\n");

            pcl.append(String.format("%-20s %s\r\n", "Ticket #:", entry.getSerialNo()));
            pcl.append(String.format("%-20s %s\r\n", "Date:", dateTime));
            pcl.append(repeat("-", lineLength)).append("\r\n");

            pcl.append("VEHICLE DETAILS:\r\n");
            pcl.append(String.format("  %-16s %s\r\n", "Vehicle No:", entry.getVehicleNo()));
            pcl.append(String.format("  %-16s %s\r\n", "Type:", entry.getVehicleType()));
            pcl.append(String.format("  %-16s %s\r\n", "Material:", entry.getMaterial()));
            pcl.append(String.format("  %-16s %s\r\n", "Party:", entry.getParty()));

            if (entry.getCharge() != null && !entry.getCharge().isEmpty()) {
                pcl.append(String.format("  %-16s %s\r\n", "Charge:", entry.getCharge()));
            }
            pcl.append(repeat("-", lineLength)).append("\r\n");

            pcl.append("WEIGHT DETAILS:\r\n\r\n");
            pcl.append(String.format("  %-16s %10s kg\r\n", "Gross Weight:", formatNumber(grossStr)));
            pcl.append(String.format("  %-16s %10s kg\r\n", "Tare Weight:", formatNumber(tareStr)));

            pcl.append("  " + repeat("-", lineLength - 4)).append("\r\n");
            pcl.append(String.format("  %-16s %10s kg\r\n", "NET WEIGHT:", formatNumber(netStr)));
            pcl.append("  " + repeat("-", lineLength - 4)).append("\r\n\r\n");

            pcl.append(repeat("-", lineLength)).append("\r\n");
            pcl.append(String.format("Operator: %s\r\n", getOperatorName()));
            pcl.append("\r\n");
            pcl.append(String.format("Signature: %s\r\n", "__________________"));
            pcl.append("\r\n");

            pcl.append(centerText("***** THANK YOU *****", lineLength)).append("\r\n");
            pcl.append(centerText("*** This is computer generated ***", lineLength)).append("\r\n");
            pcl.append(centerText("*** No signature required ***", lineLength)).append("\r\n");
            pcl.append("\r\n");
            pcl.append("\u001B&l0H");                   // Form feed

            return pcl.toString();

        } catch (Exception e) {
            Log.e("PRINT", "buildPCLTicket exception: " + e.getMessage());
            return buildPrintText(entry);
        }
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

        } catch (Exception e) {
            Log.e("BFragment", "Error in Android printing", e);
            Toast.makeText(getActivity(), "Error creating print job", Toast.LENGTH_SHORT).show();
        }
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
            Log.e("BFragment", "Error creating bitmap", e);
            return Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888);
        }
    }

    public void finalizeWeighmentEntry() {
        String serialNo = serialEditText.getText().toString().trim();
        String vehicleNo = vehicleNoSpinner.getText().toString().trim();

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
        entry.setVehicleType(vehicleTypeSpinner.getText().toString().trim());
        entry.setMaterial(materialSpinner.getText().toString().trim());
        entry.setParty(partySpinner.getText().toString().trim());
        entry.setCharge(chargeEditText.getText().toString().trim());

        // Add to master data if new values
        if (!vehicleNo.isEmpty() && !databaseHelper.isMasterDataExists(DatabaseHelper.TYPE_VEHICLE_NO, vehicleNo)) {
            databaseHelper.insertMasterData(DatabaseHelper.TYPE_VEHICLE_NO, vehicleNo);
        }
        if (!vehicleTypeSpinner.getText().toString().trim().isEmpty() &&
                !databaseHelper.isMasterDataExists(DatabaseHelper.TYPE_VEHICLE_TYPE, vehicleTypeSpinner.getText().toString().trim())) {
            databaseHelper.insertMasterData(DatabaseHelper.TYPE_VEHICLE_TYPE, vehicleTypeSpinner.getText().toString().trim());
        }
        if (!materialSpinner.getText().toString().trim().isEmpty() &&
                !databaseHelper.isMasterDataExists(DatabaseHelper.TYPE_MATERIAL, materialSpinner.getText().toString().trim())) {
            databaseHelper.insertMasterData(DatabaseHelper.TYPE_MATERIAL, materialSpinner.getText().toString().trim());
        }
        if (!partySpinner.getText().toString().trim().isEmpty() &&
                !databaseHelper.isMasterDataExists(DatabaseHelper.TYPE_PARTY, partySpinner.getText().toString().trim())) {
            databaseHelper.insertMasterData(DatabaseHelper.TYPE_PARTY, partySpinner.getText().toString().trim());
        }

        String grossStr = grossEditText.getText().toString().trim();
        String tareStr = tareEditText.getText().toString().trim();

        try {
            long gross = grossStr.isEmpty() ? 0 : Long.parseLong(grossStr);
            long tare = tareStr.isEmpty() ? 0 : Long.parseLong(tareStr);

            if (tare > gross) {
                entry.setGross(String.valueOf(tare));
                entry.setTare(String.valueOf(gross));
            } else {
                entry.setGross(String.valueOf(gross));
                entry.setTare(String.valueOf(tare));
            }
        } catch (NumberFormatException e) {
            entry.setGross(grossStr);
            entry.setTare(tareStr);
        }

        entry.setManualTare("");
        entry.calculateNet();
        entry.setFinalized(true); // Mark as finalized

        // Show dialog with print option
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle("Finalize Entry");
        builder.setMessage("Do you want to finalize entry #" + serialNo + "?\n\nThis entry will no longer be editable.");

        builder.setPositiveButton("Finalize Only", (dialog, which) -> {
            // Finalize without printing
            long result = databaseHelper.updateWeighment(entry);

            if (result > 0) {
                Toast.makeText(getActivity(), "Entry #" + serialNo + " finalized successfully",
                        Toast.LENGTH_SHORT).show();

                if (txtDisplayTwoView != null) {
                    txtDisplayTwoView.setText("FINALIZED: " + serialNo + " | Net: " + entry.getNet() + " KG");
                }

                // Mark as finalized and disable all form fields
                isEntryFinalized = true;
                setFormFieldsEnabled(false);

                Toast.makeText(getActivity(), "Entry is now finalized and view-only", Toast.LENGTH_LONG).show();

                // Focus on search field for next operation
                searchSerialEditText.requestFocus();
                searchSerialEditText.selectAll();

                // Refresh adapters
                refreshAllAdapters();
            } else {
                Toast.makeText(getActivity(), "Error finalizing entry", Toast.LENGTH_SHORT).show();
            }
        });

        builder.setNeutralButton("Finalize & Print", (dialog, which) -> {
            // First finalize, then print
            long result = databaseHelper.updateWeighment(entry);

            if (result > 0) {
                Toast.makeText(getActivity(), "Entry #" + serialNo + " finalized successfully",
                        Toast.LENGTH_SHORT).show();

                if (txtDisplayTwoView != null) {
                    txtDisplayTwoView.setText("FINALIZED: " + serialNo + " | Net: " + entry.getNet() + " KG");
                }

                // Mark as finalized and disable all form fields
                isEntryFinalized = true;
                setFormFieldsEnabled(false);

                // Now show print options
                printWeighmentEntry(entry);

                // Focus on search field for next operation
                searchSerialEditText.requestFocus();
                searchSerialEditText.selectAll();

                // Refresh adapters
                refreshAllAdapters();
            } else {
                Toast.makeText(getActivity(), "Error finalizing entry", Toast.LENGTH_SHORT).show();
            }
        });

        builder.setNegativeButton("Cancel", (dialog, which) -> {
            dialog.dismiss();
            Toast.makeText(getActivity(), "Entry not finalized", Toast.LENGTH_SHORT).show();
        });

        builder.show();
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

    // Public methods for MainActivity
    public void performTButtonAction() {
        button4a.requestFocus();
        if (!isEntryFinalized) {
            String newValue = getCounterValue();
            if (!newValue.isEmpty()) {
                handleTButtonAction(newValue);
            } else {
                Toast.makeText(getActivity(), "No value from scale", Toast.LENGTH_SHORT).show();
            }
        }
    }

    public void performGButtonAction() {
        button4a.requestFocus();
        if (!isEntryFinalized) {
            String newValue = getCounterValue();
            if (!newValue.isEmpty()) {
                handleGButtonAction(newValue);
            } else {
                Toast.makeText(getActivity(), "No value from scale", Toast.LENGTH_SHORT).show();
            }
        }
    }

    public boolean isFragmentVisible() {
        return isAdded() && isVisible();
    }

    @Override
    public void onResume() {
        super.onResume();
        searchSerialEditText.requestFocus();
        // Reset finalized state when fragment resumes
        isEntryFinalized = false;
        setFormFieldsEnabled(false);
        refreshAllAdapters();
    }
}