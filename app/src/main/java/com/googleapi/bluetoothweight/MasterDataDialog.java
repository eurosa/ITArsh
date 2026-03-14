package com.googleapi.bluetoothweight;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Color;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

public class MasterDataDialog {

    private Context context;
    private DatabaseHelper databaseHelper;
    private AlertDialog dialog;
    private String selectedType = DatabaseHelper.TYPE_VEHICLE_TYPE;
    private List<MasterData> currentDataList = new ArrayList<>();
    private MasterDataListAdapter listAdapter;
    private Spinner spinnerDataType;

    // Types for spinner display - REMOVED Vehicle Number
    private final String[] dataTypes = {"Vehicle Type", "Material", "Party"};
    private final String[] dataTypeValues = {
            DatabaseHelper.TYPE_VEHICLE_TYPE,
            DatabaseHelper.TYPE_MATERIAL,
            DatabaseHelper.TYPE_PARTY
    };

    public MasterDataDialog(Context context) {
        this.context = context;
        this.databaseHelper = new DatabaseHelper(context);
    }

    public void show() {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        LayoutInflater inflater = LayoutInflater.from(context);
        View view = inflater.inflate(R.layout.dialog_master_data, null);
        builder.setView(view);

        // Initialize views
        spinnerDataType = view.findViewById(R.id.spinnerDataType);
        ListView listViewData = view.findViewById(R.id.listViewData);
        EditText editTextNewValue = view.findViewById(R.id.editTextNewValue);
        Button btnAdd = view.findViewById(R.id.btnAdd);
        Button btnClose = view.findViewById(R.id.btnClose);

        // Setup spinner
        ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<>(context,
                android.R.layout.simple_spinner_item, dataTypes);
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerDataType.setAdapter(spinnerAdapter);

        // Setup list adapter
        listAdapter = new MasterDataListAdapter();
        listViewData.setAdapter(listAdapter);

        // Load initial data
        loadDataForType(selectedType);

        // Spinner selection listener
        spinnerDataType.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(android.widget.AdapterView<?> parent, View view, int position, long id) {
                selectedType = dataTypeValues[position];
                loadDataForType(selectedType);
            }

            @Override
            public void onNothingSelected(android.widget.AdapterView<?> parent) {}
        });

        // Add button click listener
        btnAdd.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String newValue = editTextNewValue.getText().toString().trim();
                if (newValue.isEmpty()) {
                    Toast.makeText(context, "Please enter a value", Toast.LENGTH_SHORT).show();
                    return;
                }

                // Check if already exists
                if (databaseHelper.isMasterDataExists(selectedType, newValue)) {
                    Toast.makeText(context, "This value already exists!", Toast.LENGTH_SHORT).show();
                    return;
                }

                // Insert into database
                long id = databaseHelper.insertMasterData(selectedType, newValue);
                if (id != -1) {
                    Toast.makeText(context, "Added successfully", Toast.LENGTH_SHORT).show();
                    editTextNewValue.setText("");
                    loadDataForType(selectedType);

                    // Keep focus on spinner after adding
                    setFocusOnSpinner();
                } else {
                    Toast.makeText(context, "Failed to add", Toast.LENGTH_SHORT).show();
                }
            }
        });

        // Close button click listener
        btnClose.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
            }
        });

        builder.setTitle("Master Data Settings")
                .setIcon(android.R.drawable.ic_menu_manage);

        dialog = builder.create();
        dialog.show();

        // Set focus on spinner when dialog opens
        setFocusOnSpinner();
    }

    /**
     * Set focus on spinner with visual highlight
     */
    private void setFocusOnSpinner() {
        if (spinnerDataType == null) return;

        // Use post to ensure view is ready
        new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
            @Override
            public void run() {
                // Request focus
                spinnerDataType.requestFocus();
                spinnerDataType.requestFocusFromTouch();

                // Open dropdown automatically (optional - comment out if not wanted)
                // spinnerDataType.performClick();

                // Highlight the spinner with a different background
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                    spinnerDataType.setBackgroundResource(R.drawable.spinner_focused_background);
                } else {
                    spinnerDataType.setBackgroundColor(Color.parseColor("#E3F2FD")); // Light blue
                }

                // Remove highlight when focus is lost
                spinnerDataType.setOnFocusChangeListener(new View.OnFocusChangeListener() {
                    @Override
                    public void onFocusChange(View v, boolean hasFocus) {
                        if (hasFocus) {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                                spinnerDataType.setBackgroundResource(R.drawable.spinner_focused_background);
                            } else {
                                spinnerDataType.setBackgroundColor(Color.parseColor("#E3F2FD"));
                            }
                        } else {
                            // Restore original background
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                                spinnerDataType.setBackgroundResource(android.R.drawable.editbox_background);
                            } else {
                                spinnerDataType.setBackgroundColor(Color.TRANSPARENT);
                            }
                        }
                    }
                });

                // Hide keyboard since spinner doesn't need it
                InputMethodManager imm = (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(spinnerDataType.getWindowToken(), 0);
            }
        }, 200); // Small delay to ensure dialog is fully rendered
    }

    private void loadDataForType(String type) {
        currentDataList = databaseHelper.getAllMasterDataByType(type);
        if (listAdapter != null) {
            listAdapter.notifyDataSetChanged();
        }
    }

    // Custom adapter for master data list with edit and delete options
    private class MasterDataListAdapter extends BaseAdapter {

        @Override
        public int getCount() {
            return currentDataList.size();
        }

        @Override
        public Object getItem(int position) {
            return currentDataList.get(position);
        }

        @Override
        public long getItemId(int position) {
            return currentDataList.get(position).getId();
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = LayoutInflater.from(context).inflate(R.layout.item_master_data, parent, false);
            }

            MasterData data = currentDataList.get(position);

            TextView textValue = convertView.findViewById(R.id.textValue);
            Button btnEdit = convertView.findViewById(R.id.btnEdit);
            Button btnDelete = convertView.findViewById(R.id.btnDelete);

            textValue.setText(data.getValue());

            // Edit button click
            btnEdit.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    showEditDialog(data);
                }
            });

            // Delete button click
            btnDelete.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    showDeleteConfirmation(data);
                }
            });

            return convertView;
        }
    }

    private void showEditDialog(MasterData data) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle("Edit " + getDisplayType(data.getType()));

        LinearLayout layout = new LinearLayout(context);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(50, 20, 50, 20);

        final EditText input = new EditText(context);
        input.setInputType(InputType.TYPE_CLASS_TEXT);
        input.setText(data.getValue());
        input.setSelectAllOnFocus(true);
        layout.addView(input);

        builder.setView(layout);

        builder.setPositiveButton("Update", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String newValue = input.getText().toString().trim();
                if (newValue.isEmpty()) {
                    Toast.makeText(context, "Value cannot be empty", Toast.LENGTH_SHORT).show();
                    return;
                }

                // Check if exists (excluding current item)
                if (!newValue.equals(data.getValue()) &&
                        databaseHelper.isMasterDataExists(data.getType(), newValue)) {
                    Toast.makeText(context, "This value already exists!", Toast.LENGTH_SHORT).show();
                    return;
                }

                int result = databaseHelper.updateMasterData(data.getId(), newValue);
                if (result > 0) {
                    Toast.makeText(context, "Updated successfully", Toast.LENGTH_SHORT).show();
                    loadDataForType(selectedType);
                } else if (result == -1) {
                    Toast.makeText(context, "Value already exists!", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(context, "Update failed", Toast.LENGTH_SHORT).show();
                }
            }
        });
        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    private void showDeleteConfirmation(MasterData data) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle("Delete " + getDisplayType(data.getType()));
        builder.setMessage("Are you sure you want to delete '" + data.getValue() + "'?\n\nNote: This will not affect existing weighment entries.");

        builder.setPositiveButton("Delete", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                databaseHelper.deleteMasterData(data.getId());
                Toast.makeText(context, "Deleted successfully", Toast.LENGTH_SHORT).show();
                loadDataForType(selectedType);

                // Return focus to main spinner after deletion
                setFocusOnSpinner();
            }
        });
        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    private String getDisplayType(String type) {
        switch (type) {
            case DatabaseHelper.TYPE_VEHICLE_TYPE:
                return "Vehicle Type";
            case DatabaseHelper.TYPE_VEHICLE_NO:
                return "Vehicle Number";
            case DatabaseHelper.TYPE_MATERIAL:
                return "Material";
            case DatabaseHelper.TYPE_PARTY:
                return "Party";
            default:
                return type;
        }
    }
}