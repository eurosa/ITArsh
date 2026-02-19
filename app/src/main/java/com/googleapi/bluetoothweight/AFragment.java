package com.googleapi.bluetoothweight;

import androidx.appcompat.widget.AppCompatButton;
import androidx.fragment.app.Fragment;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;

public class AFragment extends Fragment {

    private AppCompatButton button4a, button5a;
    private EditText serialEditText, vehicleNoEditText, vehicleEditText, materialEditText,
            partyEditText, chargeEditText, grossEditText, tareEditText, manualEditText;
    private TextView txtNetWeight;

    private DatabaseHelper databaseHelper;

    // Button references for T, G, M
    private AppCompatButton buttonT, buttonG;
    private MainActivity mainActivity;

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

        // Setup Save and Print buttons
        setupActionButtons();

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

        button4a = view.findViewById(R.id.button4a);
        button5a = view.findViewById(R.id.button5a);

        buttonT = view.findViewById(R.id.buttonT);
        buttonG = view.findViewById(R.id.buttonG);

        // Make serialEditText non-editable (auto-generated)
        serialEditText.setFocusable(false);
        serialEditText.setClickable(false);
    }

    private void generateNextSerialNumber() {
        // Get the next serial number (max + 1)
        int nextSerial = databaseHelper.getNextSerialNumber();
        serialEditText.setText(String.valueOf(nextSerial));
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

        if (txtNetWeight != null) {
            txtNetWeight.setText("");
        }

        // Generate next serial number
        generateNextSerialNumber();
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
                double gross = Double.parseDouble(grossStr);
                double tare = Double.parseDouble(tareStr);
                double net = gross - tare;

                Log.d("AFragment", "Net Weight: " + net);

                if (txtNetWeight != null) {
                    txtNetWeight.setText(String.valueOf(net));
                }
            } catch (NumberFormatException e) {
                // Ignore parsing errors
            }
        }
    }

    private void setupTGMButtons() {
        buttonT.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(getActivity(), "T button clicked", Toast.LENGTH_SHORT).show();
                if (mainActivity != null && mainActivity.txtCounter != null) {
                    tareEditText.setText(mainActivity.txtCounter.getText());
                }
            }
        });

        buttonG.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(getActivity(), "G button clicked", Toast.LENGTH_SHORT).show();
                if (mainActivity != null && mainActivity.txtCounter != null) {
                    grossEditText.setText(mainActivity.txtCounter.getText());
                }
            }
        });
    }

    /**
     * Method to perform T button action (called from MainActivity)
     */
    public void performTButtonAction() {
        // Same logic as button T click
        Toast.makeText(getActivity(), "T button pressed via keyboard", Toast.LENGTH_SHORT).show();
        if (mainActivity != null && mainActivity.txtCounter != null) {
            tareEditText.setText(mainActivity.txtCounter.getText());
        }
    }

    /**
     * Method to perform G button action (called from MainActivity)
     */
    public void performGButtonAction() {
        // Same logic as button G click
        Toast.makeText(getActivity(), "G button pressed via keyboard", Toast.LENGTH_SHORT).show();
        if (mainActivity != null && mainActivity.txtCounter != null) {
            grossEditText.setText(mainActivity.txtCounter.getText());
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

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

        if (exists) {
            builder.setTitle("Update Entry");
            builder.setMessage("Serial number " + serialNo + " already exists. Do you want to update it?");
        } else {
            builder.setTitle("Save Entry");
            builder.setMessage("Do you want to save this entry?");
        }

        builder.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                long result;
                if (exists) {
                    result = databaseHelper.updateWeighment(entry);
                    if (result > 0) {
                        Toast.makeText(getActivity(), "Entry #" + serialNo + " updated successfully", Toast.LENGTH_SHORT).show();
                        clearAllFields();
                    }
                } else {
                    result = databaseHelper.insertWeighment(entry);
                    if (result > 0) {
                        Toast.makeText(getActivity(), "Entry #" + serialNo + " saved successfully", Toast.LENGTH_SHORT).show();
                        clearAllFields();
                    }
                }

                if (result <= 0) {
                    Toast.makeText(getActivity(), "Error saving entry", Toast.LENGTH_SHORT).show();
                }
            }
        });

        builder.setNegativeButton("No", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });

        builder.show();
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

        // Build print content
        StringBuilder printContent = new StringBuilder();
        printContent.append("=== WEIGHMENT ENTRY #").append(serialNo).append(" ===\n\n");
        printContent.append("Vehicle No: ").append(vehicleNo).append("\n");
        printContent.append("Vehicle Type: ").append(vehicleType).append("\n");
        printContent.append("Material: ").append(material).append("\n");
        printContent.append("Party: ").append(party).append("\n");
        printContent.append("Charge: ").append(charge).append("\n");
        printContent.append("Gross Weight: ").append(gross).append(" kg\n");
        printContent.append("Manual Tare Weight: ").append(manualTare).append(" kg\n");
        printContent.append("Tare Weight: ").append(tare).append(" kg\n");
        printContent.append("Net Weight: ").append(entry.getNet()).append(" kg\n");
        printContent.append("==========================");

        // Show print dialog
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle("Print Preview");
        builder.setMessage(printContent.toString());

        builder.setPositiveButton("Print & Save", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                boolean exists = databaseHelper.isSerialNoExists(serialNo);
                long result;

                if (exists) {
                    result = databaseHelper.updateWeighment(entry);
                } else {
                    result = databaseHelper.insertWeighment(entry);
                }

                if (result > 0) {
                    Toast.makeText(getActivity(), "Entry #" + serialNo + " saved and printing", Toast.LENGTH_SHORT).show();
                    Toast.makeText(getActivity(), "Printing...", Toast.LENGTH_SHORT).show();
                    clearAllFields();
                } else {
                    Toast.makeText(getActivity(), "Error saving entry", Toast.LENGTH_SHORT).show();
                }
            }
        });

        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });

        builder.setNeutralButton("Save Only", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                boolean exists = databaseHelper.isSerialNoExists(serialNo);
                long result;

                if (exists) {
                    result = databaseHelper.updateWeighment(entry);
                } else {
                    result = databaseHelper.insertWeighment(entry);
                }

                if (result > 0) {
                    Toast.makeText(getActivity(), "Entry #" + serialNo + " saved successfully", Toast.LENGTH_SHORT).show();
                    clearAllFields();
                } else {
                    Toast.makeText(getActivity(), "Error saving entry", Toast.LENGTH_SHORT).show();
                }
            }
        });

        builder.show();
    }

    @Override
    public void onResume() {
        super.onResume();
        // Generate next serial number when fragment resumes
        generateNextSerialNumber();
    }
}