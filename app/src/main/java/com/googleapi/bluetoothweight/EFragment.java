package com.googleapi.bluetoothweight;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class EFragment extends Fragment {

    private EditText searchSerialEditText;
    private Button btnSearch, btnDeleteSingle, btnDeleteAll, btnClear, btnClose;
    private LinearLayout entryDetailsLayout;
    private TextView txtSerialNo, txtVehicleNo, txtVehicleType, txtMaterial, txtParty,
            txtGross, txtTare, txtNet, txtFinalized, txtTimestamp, txtTotalEntries;

    private DatabaseHelper databaseHelper;
    private String currentSerialNo = "";
    private WeighmentEntry currentEntry = null;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_e, container, false);

        // Initialize DatabaseHelper
        databaseHelper = new DatabaseHelper(getActivity());

        // Initialize views
        initViews(view);

        // Setup listeners
        setupListeners();

        // Update total entries count
        updateTotalEntriesCount();

        return view;
    }

    private void initViews(View view) {
        searchSerialEditText = view.findViewById(R.id.searchSerialEditText);
        btnSearch = view.findViewById(R.id.btnSearch);
        btnDeleteSingle = view.findViewById(R.id.btnDeleteSingle);
        btnDeleteAll = view.findViewById(R.id.btnDeleteAll);
        btnClear = view.findViewById(R.id.btnClear);
        btnClose = view.findViewById(R.id.btnClose);

        entryDetailsLayout = view.findViewById(R.id.entryDetailsLayout);

        txtSerialNo = view.findViewById(R.id.txtSerialNo);
        txtVehicleNo = view.findViewById(R.id.txtVehicleNo);
        txtVehicleType = view.findViewById(R.id.txtVehicleType);
        txtMaterial = view.findViewById(R.id.txtMaterial);
        txtParty = view.findViewById(R.id.txtParty);
        txtGross = view.findViewById(R.id.txtGross);
        txtTare = view.findViewById(R.id.txtTare);
        txtNet = view.findViewById(R.id.txtNet);
        txtFinalized = view.findViewById(R.id.txtFinalized);
        txtTimestamp = view.findViewById(R.id.txtTimestamp);
        txtTotalEntries = view.findViewById(R.id.txtTotalEntries);

        // Initially hide entry details
        entryDetailsLayout.setVisibility(View.GONE);
    }

    private void setupListeners() {
        // Search button click
        btnSearch.setOnClickListener(v -> searchEntry());

        // Enter key on search field
        searchSerialEditText.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH ||
                    actionId == EditorInfo.IME_ACTION_DONE ||
                    (event != null && event.getKeyCode() == KeyEvent.KEYCODE_ENTER)) {
                searchEntry();
                return true;
            }
            return false;
        });

        // Delete single entry button
        btnDeleteSingle.setOnClickListener(v -> {
            if (currentEntry != null) {
                showDeleteConfirmationDialog(currentEntry);
            } else {
                Toast.makeText(getActivity(), "No entry selected", Toast.LENGTH_SHORT).show();
            }
        });

        // Delete all entries button
        btnDeleteAll.setOnClickListener(v -> showDeleteAllConfirmationDialog());

        // Clear button
        btnClear.setOnClickListener(v -> clearSearch());

        // Close button
        btnClose.setOnClickListener(v -> {
            if (getActivity() instanceof MainActivity) {
                ((MainActivity) getActivity()).hideFragmentAndShowCounter();
            }
        });
    }

    private void searchEntry() {
        String serialNo = searchSerialEditText.getText().toString().trim();

        if (serialNo.isEmpty()) {
            Toast.makeText(getActivity(), "Please enter serial number", Toast.LENGTH_SHORT).show();
            searchSerialEditText.requestFocus();
            return;
        }

        // Hide keyboard
        InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(searchSerialEditText.getWindowToken(), 0);

        WeighmentEntry entry = databaseHelper.getWeighmentBySerialNo(serialNo);

        if (entry != null) {
            currentSerialNo = serialNo;
            currentEntry = entry;
            displayEntryDetails(entry);
            entryDetailsLayout.setVisibility(View.VISIBLE);
        } else {
            Toast.makeText(getActivity(), "No entry found with Serial #" + serialNo,
                    Toast.LENGTH_SHORT).show();
            clearSearch();
        }
    }

    private void displayEntryDetails(WeighmentEntry entry) {
        txtSerialNo.setText("Serial No: " + entry.getSerialNo());
        txtVehicleNo.setText("Vehicle No: " + entry.getVehicleNo());
        txtVehicleType.setText("Vehicle Type: " + entry.getVehicleType());
        txtMaterial.setText("Material: " + entry.getMaterial());
        txtParty.setText("Party: " + entry.getParty());
        txtGross.setText("Gross: " + entry.getGross() + " kg");
        txtTare.setText("Tare: " + entry.getTare() + " kg");
        txtNet.setText("Net: " + entry.getNet() + " kg");
        txtFinalized.setText("Finalized: " + (entry.isFinalized() ? "Yes" : "No"));

        // Format timestamp
        String timestamp = entry.getTimestamp();
        if (timestamp != null && !timestamp.isEmpty()) {
            try {
                SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
                SimpleDateFormat outputFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault());
                Date date = inputFormat.parse(timestamp);
                txtTimestamp.setText("Date/Time: " + outputFormat.format(date));
            } catch (Exception e) {
                txtTimestamp.setText("Date/Time: " + timestamp);
            }
        } else {
            txtTimestamp.setText("Date/Time: N/A");
        }
    }

    private void showDeleteConfirmationDialog(WeighmentEntry entry) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle("Delete Entry");
        builder.setMessage("Are you sure you want to delete entry #" + entry.getSerialNo() + "?\n\n" +
                "Vehicle: " + entry.getVehicleNo() + "\n" +
                "Net Weight: " + entry.getNet() + " kg");

        builder.setPositiveButton("Delete", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                deleteEntry(entry.getSerialNo());
            }
        });

        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    private void deleteEntry(String serialNo) {
        databaseHelper.deleteWeighment(serialNo);
        Toast.makeText(getActivity(), "Entry #" + serialNo + " deleted successfully",
                Toast.LENGTH_SHORT).show();

        // Clear search and update count
        clearSearch();
        updateTotalEntriesCount();
    }

    private void showDeleteAllConfirmationDialog() {
        List<String> allSerials = databaseHelper.getAllSerialNumbers();
        int count = allSerials.size();

        if (count == 0) {
            Toast.makeText(getActivity(), "No entries to delete", Toast.LENGTH_SHORT).show();
            return;
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle("Delete All Entries");
        builder.setMessage("Are you sure you want to delete ALL " + count + " entries?\n\n" +
                "This action cannot be undone!");

        builder.setPositiveButton("Delete All", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                confirmDeleteAll();
            }
        });

        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    private void confirmDeleteAll() {
        // Second confirmation for safety
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle("FINAL WARNING");
        builder.setMessage("This will permanently delete ALL weighment entries.\n\n" +
                "Type 'DELETE' to confirm:");

        final EditText input = new EditText(getActivity());
        input.setHint("Type DELETE here");
        input.setInputType(EditorInfo.TYPE_CLASS_TEXT);
        input.setPadding(50, 20, 50, 20);
        builder.setView(input);

        builder.setPositiveButton("Confirm Delete All", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String confirmation = input.getText().toString().trim();
                if ("DELETE".equals(confirmation)) {
                    performDeleteAll();
                } else {
                    Toast.makeText(getActivity(), "Confirmation failed. Type 'DELETE' exactly.",
                            Toast.LENGTH_LONG).show();
                }
            }
        });

        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    private void performDeleteAll() {
        List<String> allSerials = databaseHelper.getAllSerialNumbers();
        int deletedCount = 0;

        for (String serial : allSerials) {
            databaseHelper.deleteWeighment(serial);
            deletedCount++;
        }

        Toast.makeText(getActivity(), deletedCount + " entries deleted successfully",
                Toast.LENGTH_SHORT).show();

        // Clear search and update count
        clearSearch();
        updateTotalEntriesCount();
    }

    private void clearSearch() {
        searchSerialEditText.setText("");
        entryDetailsLayout.setVisibility(View.GONE);
        currentSerialNo = "";
        currentEntry = null;
        searchSerialEditText.requestFocus();
    }

    private void updateTotalEntriesCount() {
        List<String> allSerials = databaseHelper.getAllSerialNumbers();
        txtTotalEntries.setText("Total entries: " + allSerials.size());
    }

    public boolean isFragmentVisible() {
        return isAdded() && isVisible();
    }

    @Override
    public void onResume() {
        super.onResume();
        updateTotalEntriesCount();
        searchSerialEditText.requestFocus();
    }
}