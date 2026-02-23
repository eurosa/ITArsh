package com.googleapi.bluetoothweight;

import androidx.appcompat.widget.AppCompatButton;
import androidx.fragment.app.Fragment;

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
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;

public class AFragment extends Fragment {

    private AppCompatButton button4a, button5a;
    private EditText serialEditText, vehicleNoEditText, vehicleEditText, materialEditText,
            partyEditText, chargeEditText, grossEditText, tareEditText, manualEditText, netWeightEditText;
    private TextView txtNetWeight;
    private View[] focusOrder;
    private DatabaseHelper databaseHelper;

    // Button references for T, G, M
    private AppCompatButton buttonT, buttonG;
    private MainActivity mainActivity;

    // Flag to track manual EditText enabled state
    private boolean isManualEditTextEnabled = false;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_a, container, false);

        // Initialize DatabaseHelper
        databaseHelper = new DatabaseHelper(getActivity());
        mainActivity = (MainActivity) getActivity();

        // Initialize views
        initViews(view);

        // Generate next serial number when fragment starts
        generateNextSerialNumber();

        // Setup text watchers for gross and tare to auto-calculate net
        setupCalculationListeners();

        // Setup T, G, M buttons
        setupTGMButtons();
        focusOrder = new View[]{
                vehicleNoEditText,
                vehicleEditText,
                materialEditText,
                partyEditText,
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
        return view;
    }

    private void initViews(View view) {
        serialEditText = view.findViewById(R.id.serialEditText);
        vehicleNoEditText = view.findViewById(R.id.vehicleNoEditText);
        vehicleEditText = view.findViewById(R.id.vehicleEditText);
        materialEditText = view.findViewById(R.id.materialEditText);
        partyEditText = view.findViewById(R.id.partyEditText);
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
        button4a.setNextFocusDownId(R.id.button5a); // From Save to Print
        button4a.setNextFocusDownId(R.id.manualEditText); // From Print back to first field (optional)
        manualEditText.setNextFocusDownId(R.id.button4a); // From Print back to first field (optional)

        // Also set right/left navigation if needed
        button4a.setNextFocusRightId(R.id.button5a);
        button5a.setNextFocusLeftId(R.id.button4a);
        // Initially disable manual EditText
        manualEditText.setEnabled(false);
        manualEditText.setFocusable(false);
        manualEditText.setClickable(false);
        manualEditText.setAlpha(0.5f); // Visual indication that it's disabled
    }

    private void generateNextSerialNumber() {
        // Get the next serial number (max + 1)
        int nextSerial = databaseHelper.getNextSerialNumber();
        serialEditText.setText(String.valueOf(nextSerial));
    }
    /**
     * Set up Enter key navigation for EditText fields to focus on buttons
     */
    private void setupEnterKeyNavigation() {
        // List of EditText fields in order
        EditText[] editTexts = {
                vehicleNoEditText,
                vehicleEditText,
                materialEditText,
                partyEditText,
                chargeEditText,
                grossEditText,
                tareEditText,
                manualEditText
        };

        // Set OnEditorActionListener for each EditText
        for (EditText editText : editTexts) {
            if (editText != null) {
                editText.setOnEditorActionListener((v, actionId, event) -> {
                    // Check if Enter key was pressed
                    if (actionId == android.view.inputmethod.EditorInfo.IME_ACTION_DONE ||
                            (event != null && event.getKeyCode() == KeyEvent.KEYCODE_ENTER &&
                                    event.getAction() == KeyEvent.ACTION_DOWN)) {

                        // Find the next EditText or focus on button
                        int currentIndex = -1;

                        // Find current EditText index
                        for (int i = 0; i < editTexts.length; i++) {
                            if (editTexts[i] == v) {
                                currentIndex = i;
                                break;
                            }
                        }

                        // If there's a next EditText, focus on it
                        if (currentIndex >= 0 && currentIndex < editTexts.length - 1) {
                            editTexts[currentIndex + 1].requestFocus();
                        } else {
                            // If this is the last EditText (manualEditText), focus on Save button
                            button4a.requestFocus();
                        }

                        return true;
                    }
                    return false;
                });
            }
        }

        // Also handle Save button's Enter key to move to Print button
        button4a.setOnKeyListener((v, keyCode, event) -> {
            if (keyCode == KeyEvent.KEYCODE_ENTER && event.getAction() == KeyEvent.ACTION_DOWN) {
                button5a.requestFocus();
                return true;
            }
            return false;
        });
    }
    private void clearAllFields() {
        vehicleNoEditText.setText("");
        vehicleEditText.setText("");
        materialEditText.setText("");
        partyEditText.setText("");
        chargeEditText.setText("");
        grossEditText.setText("");
        tareEditText.setText("");
        manualEditText.setText("");
        netWeightEditText.setText("");

        if (txtNetWeight != null) {
            txtNetWeight.setText("");
        }

        // Generate next serial number
        generateNextSerialNumber();

        // Reset manual EditText to disabled state
        disableManualEditText();
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

             //   if (txtNetWeight != null) {
                //    txtNetWeight.setText(String.valueOf(net));
                    netWeightEditText.setText(String.valueOf(net));
              //  }
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
                    // Get the text, remove all spaces, and set it
                    String textWithSpaces = mainActivity.txtCounter.getText().toString();
                    String textWithoutSpaces = textWithSpaces.replaceAll("\\s", ""); // Remove all whitespace
                    tareEditText.setText(textWithoutSpaces);
                }
            }
        });

        buttonG.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mainActivity != null && mainActivity.txtCounter != null) {
                    // Get the text, remove all spaces, and set it
                    String textWithSpaces = mainActivity.txtCounter.getText().toString();
                    String textWithoutSpaces = textWithSpaces.replaceAll("\\s", ""); // Remove all whitespace
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

    /**
     * Enable manual EditText
     */
    private void enableManualEditText() {
        manualEditText.setEnabled(true);
        manualEditText.setFocusable(true);
        manualEditText.setFocusableInTouchMode(true);
        manualEditText.setClickable(true);
        manualEditText.setAlpha(1.0f); // Full opacity when enabled
        manualEditText.requestFocus();

       // Toast.makeText(getActivity(), "Manual tare enabled", Toast.LENGTH_SHORT).show();

        // Show keyboard
        InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.showSoftInput(manualEditText, InputMethodManager.SHOW_IMPLICIT);
    }

    /**
     * Disable manual EditText
     */
    private void disableManualEditText() {
        manualEditText.setEnabled(false);
        manualEditText.setFocusable(false);
        manualEditText.setFocusableInTouchMode(false);
        manualEditText.setClickable(false);
        manualEditText.setAlpha(0.5f); // Dimmed when disabled

        // Hide keyboard
        InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(manualEditText.getWindowToken(), 0);

       // Toast.makeText(getActivity(), "Manual tare disabled", Toast.LENGTH_SHORT).show();
    }

    /**
     * Method to perform T button action (called from MainActivity)
     */
    public void performTButtonAction() {
        // Same logic as button T click
        buttonT.requestFocus();
       // Toast.makeText(getActivity(), "T button pressed via keyboard", Toast.LENGTH_SHORT).show();
        if (mainActivity != null && mainActivity.txtCounter != null) {
            tareEditText.setText(mainActivity.txtCounter.getText().toString().trim());
        }
    }
    // Add these methods to your AFragment class
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
    /**
     * Method to perform G button action (called from MainActivity)
     */
    public void performGButtonAction() {
        // Same logic as button G click
        buttonG.requestFocus();
      //  Toast.makeText(getActivity(), "G button pressed via keyboard", Toast.LENGTH_SHORT).show();
        if (mainActivity != null && mainActivity.txtCounter != null) {
            grossEditText.setText(mainActivity.txtCounter.getText().toString().trim());
        }
    }
    public void performGButtonActionClear() {
        // Same logic as button G click
        clearAllFields();
    }

    /**
     * Method to perform M button action (called from MainActivity)
     * Toggles manual EditText enabled/disabled state
     */
    public void performMButtonAction() {
     //   Toast.makeText(getActivity(), "M button pressed via keyboard", Toast.LENGTH_SHORT).show();
        toggleManualEditText();
    }
    private void setupButtonFocusListeners() {
        // Setup focus change listener for button T
        if (buttonT != null) {
            buttonT.setOnFocusChangeListener(new View.OnFocusChangeListener() {
                @Override
                public void onFocusChange(View v, boolean hasFocus) {
                    if (hasFocus) {
                        // Button gained focus
                        buttonT.setBackgroundColor(Color.parseColor("#FFA500")); // Orange color
                        buttonT.setTextColor(Color.WHITE);
                    } else {
                        // Button lost focus
                        buttonT.setBackgroundColor(Color.parseColor("#808080")); // Gray color
                        buttonT.setTextColor(Color.BLACK);
                    }
                }
            });

            // Set key listener for Enter key on button T

        }

        // Setup focus change listener for button G
        if (buttonG != null) {
            buttonG.setOnFocusChangeListener(new View.OnFocusChangeListener() {
                @Override
                public void onFocusChange(View v, boolean hasFocus) {
                    if (hasFocus) {
                        // Button gained focus
                        buttonG.setBackgroundColor(Color.parseColor("#00FF00")); // Green color
                        buttonG.setTextColor(Color.WHITE);
                    } else {
                        // Button lost focus
                        buttonG.setBackgroundColor(Color.parseColor("#808080")); // Gray color
                        buttonG.setTextColor(Color.BLACK);
                    }
                }
            });

            // Set key listener for Enter key on button G
            buttonG.setOnKeyListener(new View.OnKeyListener() {
                @Override
                public boolean onKey(View v, int keyCode, KeyEvent event) {
                    if (event.getAction() == KeyEvent.ACTION_DOWN) {
                        if (keyCode == KeyEvent.KEYCODE_ENTER || keyCode == KeyEvent.KEYCODE_NUMPAD_ENTER) {
                            // Call performGButtonAction when Enter is pressed
                            performGButtonAction();

                            // Optional: Move focus to next view after action
                            moveToNextFocus(v);
                            return true;
                        }
                    }
                    return false;
                }
            });
        }

        // Setup for button4a (Save button)
        if (button4a != null) {
            button4a.setOnFocusChangeListener(new View.OnFocusChangeListener() {
                @Override
                public void onFocusChange(View v, boolean hasFocus) {
                    if (hasFocus) {
                        // Button gained focus
                        button4a.setBackgroundColor(Color.parseColor("#00FF00")); // Green color
                        button4a.setTextColor(Color.WHITE);
                    } else {
                        // Button lost focus
                        button4a.setBackgroundColor(Color.parseColor("#808080")); // Gray color
                        button4a.setTextColor(Color.BLACK);
                    }
                }
            });

            // Set key listener for Enter key on Save button
            button4a.setOnKeyListener(new View.OnKeyListener() {
                @Override
                public boolean onKey(View v, int keyCode, KeyEvent event) {
                    if (event.getAction() == KeyEvent.ACTION_DOWN) {
                        if (keyCode == KeyEvent.KEYCODE_ENTER || keyCode == KeyEvent.KEYCODE_NUMPAD_ENTER) {
                            // Call save action when Enter is pressed
                            printWeighmentEntry();

                            // Optional: Move focus to next view after action
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
                        // Button gained focus
                        button5a.setBackgroundColor(Color.parseColor("#00FF00")); // Green color
                        button5a.setTextColor(Color.WHITE);
                    } else {
                        // Button lost focus
                        button5a.setBackgroundColor(Color.parseColor("#808080")); // Gray color
                        button5a.setTextColor(Color.BLACK);
                    }
                }
            });

            // Set key listener for Enter key on Print button
            button5a.setOnKeyListener(new View.OnKeyListener() {
                @Override
                public boolean onKey(View v, int keyCode, KeyEvent event) {
                    if (event.getAction() == KeyEvent.ACTION_DOWN) {
                        if (keyCode == KeyEvent.KEYCODE_ENTER || keyCode == KeyEvent.KEYCODE_NUMPAD_ENTER) {
                            // Call print action when Enter is pressed
                            printWeighmentEntry();

                            // Optional: Move focus to next view after action
                            moveToNextFocus(v);
                            return true;
                        }
                    }
                    return false;
                }
            });
        }
    }

    /**
     * Helper method to move focus to next view after button action
     */
    private void moveToNextFocus(View currentView) {
        // Find current index in focus order
        int currentIndex = -1;
        for (int i = 0; i < focusOrder.length; i++) {
            if (focusOrder[i] == currentView) {
                currentIndex = i;
                break;
            }
        }

        if (currentIndex != -1) {
            // Calculate next index with wrap-around
            int nextIndex = (currentIndex + 1) % focusOrder.length;
            int attempts = 0;

            // Find next focusable view, skipping disabled views
            while (attempts < focusOrder.length) {
                View nextView = focusOrder[nextIndex];

                // Check if view is focusable and enabled
                if (nextView != null && nextView.isFocusable() &&
                        (!(nextView instanceof EditText) || ((EditText) nextView).isEnabled())) {

                    nextView.requestFocus();

                    // Update keyboard for the next view
                    if (nextView instanceof EditText) {
                        showKeyboard((EditText) nextView);
                    } else {
                        hideKeyboard();
                    }
                    return;
                }

                // Try next view
                nextIndex = (nextIndex + 1) % focusOrder.length;
                attempts++;
            }
        }
    }

// Make sure you have these helper methods in your class:
    /**
     * Show keyboard for EditText
     */
    private void showKeyboard(EditText editText) {
        if (getActivity() != null) {
            editText.requestFocus();
            InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.showSoftInput(editText, InputMethodManager.SHOW_IMPLICIT);
        }
    }

    /**
     * Hide keyboard
     */
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
                printWeighmentEntry();
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
        // Validate required fields
        String serialNo = serialEditText.getText().toString().trim();
        String vehicleNo = vehicleNoEditText.getText().toString().trim();
        String vehicleType = vehicleEditText.getText().toString().trim();
        String material = materialEditText.getText().toString().trim();
        String party = partyEditText.getText().toString().trim();
        String charge = chargeEditText.getText().toString().trim();
        String gross = grossEditText.getText().toString().trim();
        String tare = tareEditText.getText().toString().trim();
        String manualTare = manualEditText.getText().toString().trim();

        if (serialNo.isEmpty()) {
            Toast.makeText(getActivity(), "Serial number is required", Toast.LENGTH_SHORT).show();
            return;
        }

        if (vehicleNo.isEmpty()) {
            vehicleNoEditText.setError("Vehicle number is required");
            vehicleNoEditText.requestFocus();
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

        // Check if serial exists
        boolean exists = databaseHelper.isSerialNoExists(serialNo);
        if (exists) {

        } else {
            long result = databaseHelper.insertWeighment(entry);
            if (result > 0) {
                Toast.makeText(getActivity(), "Entry #" + serialNo + " saved successfully", Toast.LENGTH_SHORT).show();
                moveFocusToPrintButton();
            }

            // clearAllFields();
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
     * Helper method to move focus to Print button
     */
    private void moveFocusToPrintButton() {
        if (button5a != null) {
            // Post to ensure it runs after dialog is dismissed
            button5a.post(new Runnable() {
                @Override
                public void run() {
                    button5a.requestFocus();

                    // Optional: Show a visual indication that Print button is focused
                    // You can also add a toast or animation if desired
                    Log.d("AFragment", "Focus moved to Print button");
                }
            });
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

    /**
     * Check if this fragment is currently visible
     */
    public boolean isFragmentVisible() {
        return isAdded() && isVisible();
    }
    /**
     * Helper method to move focus to Save button
     */
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
    private void printWeighmentEntry() {
        // Validate required fields
        String serialNo = serialEditText.getText().toString().trim();
        String vehicleNo = vehicleNoEditText.getText().toString().trim();
        String vehicleType = vehicleEditText.getText().toString().trim();
        String material = materialEditText.getText().toString().trim();
        String party = partyEditText.getText().toString().trim();
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
            vehicleNoEditText.setError("Vehicle number is required");
            vehicleNoEditText.requestFocus();
            return;
        }

        // Track if we have valid weight entries
        boolean hasValidGross = false;
        boolean hasValidTare = false;

        // Check Gross Weight - can be empty or any valid number (including zero)
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

        // Check Tare Weight - can be empty or any valid number (including zero)
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

        // Check if at least one weight field has a value - SIMPLIFIED: just toast, no dialog
        if (!hasValidGross && !hasValidTare) {
            Toast.makeText(getActivity(), "Please enter at least Gross weight or Tare weight",
                    Toast.LENGTH_LONG).show();
            grossEditText.requestFocus(); // Focus on gross field by default
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

        // Build print content
        StringBuilder printContent = new StringBuilder();
        printContent.append("=== WEIGHMENT ENTRY #").append(serialNo).append(" ===\n\n");
        printContent.append("Vehicle No: ").append(vehicleNo).append("\n");
        printContent.append("Vehicle Type: ").append(vehicleType).append("\n");
        printContent.append("Material: ").append(material).append("\n");
        printContent.append("Party: ").append(party).append("\n");
        printContent.append("Charge: ").append(charge).append("\n");
        printContent.append("Gross Weight: ").append(finalGross).append(" kg\n");
        printContent.append("Manual Tare Weight: ").append(manualTare).append(" kg\n");
        printContent.append("Tare Weight: ").append(finalTare).append(" kg\n");
        printContent.append("Net Weight: ").append(entry.getNet()).append(" kg\n");
        printContent.append("==========================");

        boolean exists = databaseHelper.isSerialNoExists(serialNo);

        if (exists) {
           // showPrintUpdateConfirmationDialog(entry, serialNo);
        } else {
            long result = databaseHelper.insertWeighment(entry);
            if (result > 0) {
                Toast.makeText(getActivity(), "Entry #" + serialNo + " saved and printing", Toast.LENGTH_SHORT).show();
                Toast.makeText(getActivity(), "Printing...", Toast.LENGTH_SHORT).show();
              //  showPrintPreviewDialog(printContent.toString(), entry, serialNo, false);
                moveFocusToSaveButton();
            } else {
                Toast.makeText(getActivity(), "Error saving entry", Toast.LENGTH_SHORT).show();
            }
        }
    }
    /**
     * Alternative simplified version with just the validation logic
     */
/*
private void printWeighmentEntry() {
    // Get values
    String serialNo = serialEditText.getText().toString().trim();
    String vehicleNo = vehicleNoEditText.getText().toString().trim();
    String gross = grossEditText.getText().toString().trim();
    String tare = tareEditText.getText().toString().trim();

    // Basic required fields
    if (serialNo.isEmpty()) {
        Toast.makeText(getActivity(), "Serial number is required", Toast.LENGTH_SHORT).show();
        return;
    }

    if (vehicleNo.isEmpty()) {
        vehicleNoEditText.setError("Vehicle number is required");
        vehicleNoEditText.requestFocus();
        return;
    }

    // Check if at least one of gross or tare has a value
    if (gross.isEmpty() && tare.isEmpty()) {
        Toast.makeText(getActivity(), "Please enter either Gross or Tare weight", Toast.LENGTH_SHORT).show();

        // Ask user which field to fill
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle("Weight Required");
        builder.setMessage("Please enter at least Gross weight or Tare weight");
        builder.setPositiveButton("Enter Gross", (d, w) -> grossEditText.requestFocus());
        builder.setNegativeButton("Enter Tare", (d, w) -> tareEditText.requestFocus());
        builder.setNeutralButton("Cancel", null);
        builder.show();
        return;
    }

    // Validate gross if provided
    if (!gross.isEmpty()) {
        try {
            double g = Double.parseDouble(gross);
            if (g < 0) {
                grossEditText.setError("Gross cannot be negative");
                grossEditText.requestFocus();
                return;
            }
        } catch (NumberFormatException e) {
            grossEditText.setError("Invalid number");
            grossEditText.requestFocus();
            return;
        }
    }

    // Validate tare if provided
    if (!tare.isEmpty()) {
        try {
            double t = Double.parseDouble(tare);
            if (t < 0) {
                tareEditText.setError("Tare cannot be negative");
                tareEditText.requestFocus();
                return;
            }
        } catch (NumberFormatException e) {
            tareEditText.setError("Invalid number");
            tareEditText.requestFocus();
            return;
        }
    }

    // Rest of your printing logic...
}
*/

    @Override
    public void onResume() {
        super.onResume();
        // Generate next serial number when fragment resumes
        generateNextSerialNumber();
    }
}