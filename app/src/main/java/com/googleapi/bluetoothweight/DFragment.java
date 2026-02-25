package com.googleapi.bluetoothweight;

import android.app.AlertDialog;
import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
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
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.AppCompatButton;
import androidx.fragment.app.Fragment;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class DFragment extends Fragment {

    private AppCompatButton button4a, button5a; // Added button5a for Print
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
            printCurrentEntry();
        });

        // Handle Enter key on Print button
        button5a.setOnKeyListener((v, keyCode, event) -> {
            if (keyCode == KeyEvent.KEYCODE_ENTER && event.getAction() == KeyEvent.ACTION_DOWN) {
                printCurrentEntry();
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
        StringBuilder printContent = new StringBuilder();
        printContent.append("=== WEIGHMENT ENTRY #").append(entry.getSerialNo()).append(" ===\n\n");
        printContent.append("Vehicle No: ").append(entry.getVehicleNo()).append("\n");
        printContent.append("Vehicle Type: ").append(entry.getVehicleType()).append("\n");
        printContent.append("Material: ").append(entry.getMaterial()).append("\n");
        printContent.append("Party: ").append(entry.getParty()).append("\n");
        printContent.append("Charge: ").append(entry.getCharge()).append("\n");
        printContent.append("Gross Weight: ").append(entry.getGross()).append(" kg\n");
        printContent.append("Tare Weight: ").append(entry.getTare()).append(" kg\n");
        if (entry.getManualTare() != null && !entry.getManualTare().isEmpty() && !entry.getManualTare().equals("0")) {
            printContent.append("Manual Tare: ").append(entry.getManualTare()).append(" kg\n");
        }
        printContent.append("Net Weight: ").append(entry.getNet()).append(" kg\n");

        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault());
        printContent.append("Date/Time: ").append(sdf.format(new Date())).append("\n");
        printContent.append("==========================");

        // Show print preview dialog
        showPrintPreviewDialog(printContent.toString(), entry);
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
        builder.setPositiveButton("Print", (dialog, which) -> {
            // Here you would implement actual printing
            // For now, just show a toast
            Toast.makeText(getActivity(), "Printing Entry #" + entry.getSerialNo(),
                    Toast.LENGTH_SHORT).show();

            // You can also update the display
            if (txtDisplayTwoView != null) {
                txtDisplayTwoView.setText("PRINTED: " + entry.getSerialNo() +
                        " | Net: " + entry.getNet() + " KG");
            }
        });
        builder.setNegativeButton("Cancel", null);
        builder.show();
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
    }
}