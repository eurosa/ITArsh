package com.googleapi.bluetoothweight;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
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
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.AppCompatButton;
import androidx.fragment.app.Fragment;

public class BFragment extends Fragment {

    private AppCompatButton button4a;
    private EditText serialEditText, vehicleNoEditText, vehicleEditText, materialEditText,
            partyEditText, chargeEditText, grossEditText, tareEditText, netWeightEditText;
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

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_b, container, false);

        // Initialize DatabaseHelper
        databaseHelper = new DatabaseHelper(getActivity());
        mainActivity = (MainActivity) getActivity();

        // Initialize views
        initViews(view);

        // Setup listeners
        setupCalculationListeners();
        setupSearchFunctionality();
        setupGrossTareFunctionality();
        setupActionButtons();
        setupButtonFocusListeners();
      //  setupRegularFieldNavigation();
        setupTGMButtons();
        disableGrossEditText();
        disableTareEditText();
        disableNetWeightEditText();
        return view;
    }
    public void performGButtonActionClear() {
        // Same logic as button G click
        clearAllFields();
    }
    private void clearAllFields() {
        vehicleNoEditText.setText("");
        vehicleEditText.setText("");
        materialEditText.setText("");
        partyEditText.setText("");
        chargeEditText.setText("");
        grossEditText.setText("");
        tareEditText.setText("");

        netWeightEditText.setText("");




        // Reset manual EditText to disabled state
        disableGrossEditText();
        disableTareEditText();
        disableNetWeightEditText();

    }
    private void initViews(View view) {
        // Search field
        searchSerialEditText = view.findViewById(R.id.searchSerialEditText);

        // Form fields
        serialEditText = view.findViewById(R.id.serialEditText);
        vehicleNoEditText = view.findViewById(R.id.vehicleNoEditText);
        vehicleEditText = view.findViewById(R.id.vehicleEditText);
        materialEditText = view.findViewById(R.id.materialEditText);
        partyEditText = view.findViewById(R.id.partyEditText);
        chargeEditText = view.findViewById(R.id.chargeEditText);
        grossEditText = view.findViewById(R.id.grossEditText);
        tareEditText = view.findViewById(R.id.tareEditText);
        netWeightEditText = view.findViewById(R.id.netWeightEditText);

        // T and G buttons
        buttonT = view.findViewById(R.id.buttonT);
        buttonG = view.findViewById(R.id.buttonG);

        // Display text view
        txtDisplayTwoView = view.findViewById(R.id.txtDisplayTwoView);

        // Update button
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
        //buttonT.setFocusable(true);
       // buttonT.setFocusableInTouchMode(true);
       // buttonG.setFocusable(true);
       // buttonG.setFocusableInTouchMode(true);

        // Set focus chain
       // searchSerialEditText.setNextFocusDownId(R.id.buttonT);
       // buttonT.setNextFocusDownId(R.id.buttonG);
        //buttonG.setNextFocusDownId(R.id.vehicleNoEditText);

        // Set IME options
       // searchSerialEditText.setImeOptions(EditorInfo.IME_ACTION_SEARCH);
        searchSerialEditText.setImeOptions(EditorInfo.IME_ACTION_SEARCH);
        grossEditText.setImeOptions(EditorInfo.IME_ACTION_NEXT);
        tareEditText.setImeOptions(EditorInfo.IME_ACTION_DONE);
    }

    private void setFormFieldsEnabled(boolean enabled) {
        // Only enable/disable form fields based on finalized status
        // If entry is finalized, fields should be disabled even if enabled=true is passed
        boolean finalEnabled = enabled && !isEntryFinalized;

        vehicleNoEditText.setEnabled(finalEnabled);
        vehicleEditText.setEnabled(finalEnabled);
        materialEditText.setEnabled(finalEnabled);
        partyEditText.setEnabled(finalEnabled);
        chargeEditText.setEnabled(finalEnabled);
       // grossEditText.setEnabled(finalEnabled);
       // tareEditText.setEnabled(finalEnabled);
       // netWeightEditText.setEnabled(finalEnabled);

        vehicleNoEditText.setFocusable(finalEnabled);
        vehicleNoEditText.setFocusableInTouchMode(finalEnabled);
        vehicleEditText.setFocusable(finalEnabled);
        vehicleEditText.setFocusableInTouchMode(finalEnabled);
        materialEditText.setFocusable(finalEnabled);
        materialEditText.setFocusableInTouchMode(finalEnabled);
        partyEditText.setFocusable(finalEnabled);
        partyEditText.setFocusableInTouchMode(finalEnabled);
        chargeEditText.setFocusable(finalEnabled);
        chargeEditText.setFocusableInTouchMode(finalEnabled);
        //grossEditText.setFocusable(finalEnabled);
        //grossEditText.setFocusableInTouchMode(finalEnabled);
       // tareEditText.setFocusable(finalEnabled);
        //tareEditText.setFocusableInTouchMode(finalEnabled);
       // netWeightEditText.setFocusable(finalEnabled);
      //  netWeightEditText.setFocusableInTouchMode(finalEnabled);

        float alpha = finalEnabled ? 1.0f : 0.6f;
        vehicleNoEditText.setAlpha(alpha);
        vehicleEditText.setAlpha(alpha);
        materialEditText.setAlpha(alpha);
        partyEditText.setAlpha(alpha);
        chargeEditText.setAlpha(alpha);
        grossEditText.setAlpha(alpha);
        tareEditText.setAlpha(alpha);
        netWeightEditText.setAlpha(alpha);

        // Update button should be disabled if entry is finalized
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
           // if (event.getAction() == KeyEvent.ACTION_DOWN) {
                if (keyCode == KeyEvent.KEYCODE_ENTER || keyCode == KeyEvent.KEYCODE_NUMPAD_ENTER) {
                    Log.d("BFragment", "Physical Enter on search field");

                    loadEntry();
                    return true; // Consume the event
                }
          //  }
            return false;
        });
    }

    /**
     * Setup gross and tare fields with Enter key functionality
     */


    /**
     * Simple Enter key handler for EditText fields
     */


    /**
     * Helper method to move to next field after gross/tare processing
     */

    /**
     * Setup regular fields navigation
     */
    /**
     * Setup regular fields navigation
     */
    /**
     * Setup regular fields navigation - Now returns focus to search field
     */
    private void setupRegularFieldNavigation() {
        EditText[] regularFields = {
                searchSerialEditText,
                vehicleNoEditText,
                vehicleEditText,
                materialEditText,
                partyEditText,
                chargeEditText
        };

        for (int i = 0; i < regularFields.length; i++) {
            final int currentIndex = i;
            EditText field = regularFields[i];

            if (field == null) continue;

            // Clear existing listeners
            field.setOnEditorActionListener(null);
            field.setOnKeyListener(null);

            // Handle physical Enter key for navigation
            field.setOnKeyListener((v, keyCode, event) -> {
                if (event.getAction() == KeyEvent.ACTION_DOWN) {
                    if (keyCode == KeyEvent.KEYCODE_ENTER || keyCode == KeyEvent.KEYCODE_NUMPAD_ENTER) {
                        Log.d("BFragment", "Physical Enter on regular field " + currentIndex);

                        if (field.isEnabled()) {
                            navigateToNextField(regularFields, currentIndex);
                        }
                        return true; // Consume the event
                    }
                }
                return false;
            });

            // Handle soft keyboard IME actions
            field.setOnEditorActionListener((v, actionId, event) -> {
                if (actionId == EditorInfo.IME_ACTION_NEXT) {
                    if (field.isEnabled()) {
                        navigateToNextField(regularFields, currentIndex);
                    }
                    return true;
                }
                return false;
            });

            field.setImeOptions(EditorInfo.IME_ACTION_NEXT);
        }
    }

    /**
     * Navigate to next enabled field - Returns to search after last field
     */
    private void navigateToNextField(EditText[] fields, int currentIndex) {
        // First try to find next enabled field in the regular fields array
        if (currentIndex < fields.length - 1) {
            // Find next enabled field in the same group
            for (int j = currentIndex + 1; j < fields.length; j++) {
                if (fields[j].isEnabled()) {
                    fields[j].requestFocus();
                    return;
                }
            }
        }

        // If we've reached the end of regular fields, go to gross field
        if (grossEditText != null && grossEditText.isEnabled()) {
            grossEditText.requestFocus();
            return;
        }
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

    /**
     * Navigate to next enabled field
     */
    /**
     * Navigate to next enabled field
     */
    private void enableGrossEditText() {
        grossEditText.setEnabled(true);
        grossEditText.setFocusable(true);
        grossEditText.setFocusableInTouchMode(true);
        grossEditText.setClickable(true);
        grossEditText.setAlpha(1.0f); // Full opacity when enabled
        grossEditText.requestFocus();

        // Toast.makeText(getActivity(), "Manual tare enabled", Toast.LENGTH_SHORT).show();

        // Show keyboard
        InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.showSoftInput(grossEditText, InputMethodManager.SHOW_IMPLICIT);
    }

    /**
     * Disable manual EditText
     */
    private void disableGrossEditText() {
        grossEditText.setEnabled(false);
        grossEditText.setFocusable(false);
        grossEditText.setFocusableInTouchMode(false);
        grossEditText.setClickable(false);
        grossEditText.setAlpha(0.5f); // Dimmed when disabled

        // Hide keyboard
        InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(grossEditText.getWindowToken(), 0);

        // Toast.makeText(getActivity(), "Manual tare disabled", Toast.LENGTH_SHORT).show();
    }
    private void disableTareEditText() {
        tareEditText.setEnabled(false);
        tareEditText.setFocusable(false);
        tareEditText.setFocusableInTouchMode(false);
        tareEditText.setClickable(false);
        tareEditText.setAlpha(0.5f); // Dimmed when disabled

        // Hide keyboard
        InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(tareEditText.getWindowToken(), 0);

        // Toast.makeText(getActivity(), "Manual tare disabled", Toast.LENGTH_SHORT).show();
    }
    private void disableNetWeightEditText() {
        netWeightEditText.setEnabled(false);
        netWeightEditText.setFocusable(false);
        netWeightEditText.setFocusableInTouchMode(false);
        netWeightEditText.setClickable(false);
        netWeightEditText.setAlpha(0.5f); // Dimmed when disabled

        // Hide keyboard
        InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(tareEditText.getWindowToken(), 0);

        // Toast.makeText(getActivity(), "Manual tare disabled", Toast.LENGTH_SHORT).show();
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
                materialEditText.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        materialEditText.requestFocus();
                        InputMethodManager keyboard = (InputMethodManager) getActivity()
                                .getSystemService(Context.INPUT_METHOD_SERVICE);
                        keyboard.showSoftInput(materialEditText, InputMethodManager.SHOW_IMPLICIT);
                    }
                }, 200); // 200ms delay
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
        vehicleNoEditText.setText(entry.getVehicleNo());
        vehicleEditText.setText(entry.getVehicleType());
        materialEditText.setText(entry.getMaterial());
        partyEditText.setText(entry.getParty());
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



    public void finalizeWeighmentEntry() {
        String serialNo = serialEditText.getText().toString().trim();
        String vehicleNo = vehicleNoEditText.getText().toString().trim();

        if (serialNo.isEmpty()) {
            Toast.makeText(getActivity(), "Please load an entry first", Toast.LENGTH_SHORT).show();
            searchSerialEditText.requestFocus();
            return;
        }

        if (vehicleNo.isEmpty()) {
            vehicleNoEditText.setError("Vehicle number is required");
            vehicleNoEditText.requestFocus();
            return;
        }

        WeighmentEntry entry = new WeighmentEntry();
        entry.setSerialNo(serialNo);
        entry.setVehicleNo(vehicleNo);
        entry.setVehicleType(vehicleEditText.getText().toString().trim());
        entry.setMaterial(materialEditText.getText().toString().trim());
        entry.setParty(partyEditText.getText().toString().trim());
        entry.setCharge(chargeEditText.getText().toString().trim());

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

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle("Finalize Entry");
        builder.setMessage("Do you want to finalize entry #" + serialNo + "? This entry will no longer be editable.");

        builder.setPositiveButton("Yes", (dialog, which) -> {
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

                // Keep the data visible but not editable
                Toast.makeText(getActivity(), "Entry is now finalized and view-only", Toast.LENGTH_LONG).show();

                // Focus on search field for next operation
                searchSerialEditText.requestFocus();
                searchSerialEditText.selectAll();
            } else {
                Toast.makeText(getActivity(), "Error finalizing entry", Toast.LENGTH_SHORT).show();
            }
        });

        builder.setNegativeButton("No", (dialog, which) -> {
            dialog.dismiss();
            // Keep the form editable
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
        vehicleNoEditText.setText("");
        vehicleEditText.setText("");
        materialEditText.setText("");
        partyEditText.setText("");
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
    }
}