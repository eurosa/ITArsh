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
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.List;

public class AFragment extends Fragment {

    private AppCompatButton button4a, button5a;
    private EditText serialEditText, chargeEditText, grossEditText, tareEditText,
            manualEditText, netWeightEditText;

    // AutoCompleteTextViews for dropdowns
    private AutoCompleteTextView vehicleNoSpinner, vehicleTypeSpinner,
            materialSpinner, partySpinner;

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

        return view;
    }

    private void initViews(View view) {
        serialEditText = view.findViewById(R.id.serialEditText);

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
      //  Toast.makeText(getActivity(), "M button pressed via keyboard", Toast.LENGTH_SHORT).show();
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
                            printWeighmentEntry();
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
                clearAllFields();
                refreshAllAdapters();
                moveFocusToSaveButton();
            } else {
                Toast.makeText(getActivity(), "Error saving entry", Toast.LENGTH_SHORT).show();
            }
        }
    }

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

    /**
     * Save weighment entry to database
     */
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
            // Show update confirmation
            showUpdateConfirmationDialog(entry);
        } else {
            long result = databaseHelper.insertWeighment(entry);
            if (result > 0) {
                Toast.makeText(getActivity(), "Entry #" + serialNo + " saved successfully",
                        Toast.LENGTH_SHORT).show();
                clearAllFields();
                refreshAllAdapters();
                moveFocusToPrintButton();
            } else {
                Toast.makeText(getActivity(), "Error saving entry", Toast.LENGTH_SHORT).show();
            }
        }
    }

    /**
     * Helper method to move focus to Print button
     */
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

    @Override
    public void onResume() {
        super.onResume();
        generateNextSerialNumber();
        refreshAllAdapters();
    }
}