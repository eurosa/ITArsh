package com.googleapi.bluetoothweight;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.Settings;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.AppCompatButton;
import androidx.appcompat.widget.AppCompatSpinner;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.itextpdf.kernel.colors.ColorConstants;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class CFragment extends Fragment {

    private AppCompatSpinner spinnerVehicleType, spinnerMaterial, spinnerParty;
    private AppCompatButton btnFromDate, btnToDate, btnSearch, btnClear, btnExport;
    private TextView txtTotalEntries, txtTotalNetWeight;
    private RecyclerView recyclerView;

    private DatabaseHelper databaseHelper;
    private ReportAdapter adapter;
    private List<WeighmentEntry> entryList;

    private String fromDate = "", toDate = "";
    private Calendar calendar;
    private SimpleDateFormat dateFormat;
    private SimpleDateFormat displayDateFormat;

    private static final int STORAGE_PERMISSION_CODE = 1002;
    private static final int MANAGE_STORAGE_CODE = 1003;

    // Focus management variables
    private int currentFocusIndex = 0;
    private View[] focusableViews;
    private static final int[] FOCUSABLE_IDS = {
            R.id.spinnerVehicleType,
            R.id.spinnerMaterial,
            R.id.spinnerParty,
            R.id.btnFromDate,
            R.id.btnToDate,
            R.id.btnSearch,
            R.id.btnClear,
            R.id.btnExport
    };

    // Colors for focus states
    private static final int COLOR_FOCUSED = Color.parseColor("#FFA500"); // Orange
    private static final int COLOR_NORMAL = Color.parseColor("#666666"); // Dark Gray
    private static final int COLOR_SELECTED = Color.parseColor("#4CAF50"); // Green

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_c, container, false);

        initializeViews(view);
        setupDateFormats();
        setupSpinners();
        setupListeners();
        setupFocusManagement();
        loadInitialData();

        // Set initial focus on vehicle type spinner
        view.post(() -> {
            spinnerVehicleType.requestFocus();
            highlightView(spinnerVehicleType);
        });

        return view;
    }

    private void initializeViews(View view) {
        spinnerVehicleType = view.findViewById(R.id.spinnerVehicleType);
        spinnerMaterial = view.findViewById(R.id.spinnerMaterial);
        spinnerParty = view.findViewById(R.id.spinnerParty);
        btnFromDate = view.findViewById(R.id.btnFromDate);
        btnToDate = view.findViewById(R.id.btnToDate);
        btnSearch = view.findViewById(R.id.btnSearch);
        btnClear = view.findViewById(R.id.btnClear);
        btnExport = view.findViewById(R.id.btnExport);
        txtTotalEntries = view.findViewById(R.id.txtTotalEntries);
        txtTotalNetWeight = view.findViewById(R.id.txtTotalNetWeight);
        recyclerView = view.findViewById(R.id.recyclerViewReports);

        databaseHelper = new DatabaseHelper(getActivity());
        entryList = new ArrayList<>();

        // Setup RecyclerView
        recyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
        adapter = new ReportAdapter(entryList);
        recyclerView.setAdapter(adapter);

        // Make export button visible
        btnExport.setVisibility(View.VISIBLE);

        // Apply normal styling to all views initially
        applyNormalStyle(spinnerVehicleType);
        applyNormalStyle(spinnerMaterial);
        applyNormalStyle(spinnerParty);
        applyButtonNormalStyle(btnFromDate);
        applyButtonNormalStyle(btnToDate);
        applyButtonNormalStyle(btnSearch);
        applyButtonNormalStyle(btnClear);
        applyButtonNormalStyle(btnExport);
    }

    private void setupFocusManagement() {
        // Create array of focusable views
        focusableViews = new View[]{
                spinnerVehicleType,
                spinnerMaterial,
                spinnerParty,
                btnFromDate,
                btnToDate,
                btnSearch,
                btnClear,
                btnExport
        };

        // Set focus change listeners
        for (int i = 0; i < focusableViews.length; i++) {
            final int index = i;
            focusableViews[i].setOnFocusChangeListener((v, hasFocus) -> {
                if (hasFocus) {
                    currentFocusIndex = index;
                    highlightView(v);
                } else {
                    removeHighlight(v);
                }
            });
        }

        // Set key listeners for navigation
        setupKeyNavigation();
    }

    private void setupKeyNavigation() {
        View rootView = getView();
        if (rootView == null) return;

        rootView.setFocusable(true);
        rootView.setFocusableInTouchMode(true);

        rootView.setOnKeyListener((v, keyCode, event) -> {
            if (event.getAction() == KeyEvent.ACTION_DOWN) {
                return handleKeyNavigation(keyCode, event);
            }
            return false;
        });

        // Also set on each focusable view for better key handling
        for (View view : focusableViews) {
            view.setOnKeyListener((v, keyCode, event) -> {
                if (event.getAction() == KeyEvent.ACTION_DOWN) {
                    return handleKeyNavigation(keyCode, event);
                }
                return false;
            });
        }
    }

    private boolean handleKeyNavigation(int keyCode, KeyEvent event) {
        switch (keyCode) {
            case KeyEvent.KEYCODE_DPAD_DOWN:
            case KeyEvent.KEYCODE_TAB:
                // Move to next focusable view
                moveFocus(1);
                return true;

            case KeyEvent.KEYCODE_DPAD_UP:
                // Move to previous focusable view
                moveFocus(-1);
                return true;

            case KeyEvent.KEYCODE_DPAD_RIGHT:
                // For spinners, open dropdown
                if (focusableViews[currentFocusIndex] instanceof AppCompatSpinner) {
                    ((AppCompatSpinner) focusableViews[currentFocusIndex]).performClick();
                    return true;
                }
                break;

            case KeyEvent.KEYCODE_ENTER:
            case KeyEvent.KEYCODE_NUMPAD_ENTER:
            case KeyEvent.KEYCODE_DPAD_CENTER:
                // Activate the current view
                View currentView = focusableViews[currentFocusIndex];
                if (currentView instanceof AppCompatButton) {
                    currentView.performClick();
                    return true;
                } else if (currentView instanceof AppCompatSpinner) {
                    ((AppCompatSpinner) currentView).performClick();
                    return true;
                }
                break;

            case KeyEvent.KEYCODE_F5:
                // Refresh report
                refreshReport();
                return true;
        }
        return false;
    }

    private void moveFocus(int direction) {
        int newIndex = currentFocusIndex + direction;
        if (newIndex >= 0 && newIndex < focusableViews.length) {
            focusableViews[newIndex].requestFocus();
        }
    }

    private void highlightView(View view) {
        if (view instanceof AppCompatSpinner) {
            applyFocusedStyle(view);
        } else if (view instanceof AppCompatButton) {
            applyButtonFocusedStyle((AppCompatButton) view);
        }
    }

    private void removeHighlight(View view) {
        if (view instanceof AppCompatSpinner) {
            applyNormalStyle(view);
        } else if (view instanceof AppCompatButton) {
            applyButtonNormalStyle((AppCompatButton) view);
        }
    }

    private void applyFocusedStyle(View view) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setShape(GradientDrawable.RECTANGLE);
        drawable.setColor(Color.WHITE);
        drawable.setStroke(3, COLOR_FOCUSED);
        drawable.setCornerRadius(8);
        view.setBackground(drawable);

        if (view instanceof TextView) {
            ((TextView) view).setTextColor(Color.BLACK);
        }
    }

    private void applyNormalStyle(View view) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setShape(GradientDrawable.RECTANGLE);
        drawable.setColor(Color.WHITE);
        drawable.setStroke(1, COLOR_NORMAL);
        drawable.setCornerRadius(8);
        view.setBackground(drawable);

        if (view instanceof TextView) {
            ((TextView) view).setTextColor(Color.BLACK);
        }
    }

    private void applyButtonFocusedStyle(AppCompatButton button) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setShape(GradientDrawable.RECTANGLE);
        drawable.setColor(COLOR_FOCUSED);
        drawable.setCornerRadius(8);
        button.setBackground(drawable);
        button.setTextColor(Color.WHITE);
    }

    private void applyButtonNormalStyle(AppCompatButton button) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setShape(GradientDrawable.RECTANGLE);
        drawable.setColor(Color.parseColor("#2196F3")); // Default blue color
        drawable.setCornerRadius(8);
        button.setBackground(drawable);
        button.setTextColor(Color.WHITE);
    }

    private void applyButtonSelectedStyle(AppCompatButton button) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setShape(GradientDrawable.RECTANGLE);
        drawable.setColor(COLOR_SELECTED);
        drawable.setCornerRadius(8);
        button.setBackground(drawable);
        button.setTextColor(Color.WHITE);
    }

    /**
     * Public method to refresh report data
     * This can be called from MainActivity when F5 is pressed
     */
    public void refreshReport() {
        if (isAdded() && getActivity() != null) {
            performSearch();
            Toast.makeText(getActivity(), "Report Refreshed", Toast.LENGTH_SHORT).show();

            // Maintain focus on current view
            if (currentFocusIndex >= 0 && currentFocusIndex < focusableViews.length) {
                focusableViews[currentFocusIndex].requestFocus();
            }
        }
    }

    private void setupDateFormats() {
        calendar = Calendar.getInstance();
        dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        displayDateFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());

        // Set default dates (last 30 days)
        Calendar fromCal = Calendar.getInstance();
        fromCal.add(Calendar.DAY_OF_MONTH, -30);
        fromDate = dateFormat.format(fromCal.getTime());
        toDate = dateFormat.format(calendar.getTime());

        btnFromDate.setText(formatDisplayDate(fromDate));
        btnToDate.setText(formatDisplayDate(toDate));
    }

    private String formatDisplayDate(String yyyyMMdd) {
        try {
            SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            SimpleDateFormat outputFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
            return outputFormat.format(inputFormat.parse(yyyyMMdd));
        } catch (Exception e) {
            return yyyyMMdd;
        }
    }

    private void setupSpinners() {
        // Vehicle Type Spinner
        List<String> vehicleTypes = databaseHelper.getUniqueVehicleTypes();
        vehicleTypes.add(0, "All Vehicle Types");
        ArrayAdapter<String> vehicleAdapter = new ArrayAdapter<>(getActivity(),
                android.R.layout.simple_spinner_item, vehicleTypes);
        vehicleAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerVehicleType.setAdapter(vehicleAdapter);

        // Material Spinner
        List<String> materials = databaseHelper.getUniqueMaterials();
        materials.add(0, "All Materials");
        ArrayAdapter<String> materialAdapter = new ArrayAdapter<>(getActivity(),
                android.R.layout.simple_spinner_item, materials);
        materialAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerMaterial.setAdapter(materialAdapter);

        // Party Spinner
        List<String> parties = databaseHelper.getUniqueParties();
        parties.add(0, "All Parties");
        ArrayAdapter<String> partyAdapter = new ArrayAdapter<>(getActivity(),
                android.R.layout.simple_spinner_item, parties);
        partyAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerParty.setAdapter(partyAdapter);
    }

    private void setupListeners() {
        btnFromDate.setOnClickListener(v -> {
            showDatePicker(true);
            applyButtonSelectedStyle(btnFromDate);
            // Reset to normal after a delay
            v.postDelayed(() -> {
                if (!btnFromDate.hasFocus()) {
                    applyButtonNormalStyle(btnFromDate);
                }
            }, 200);
        });

        btnToDate.setOnClickListener(v -> {
            showDatePicker(false);
            applyButtonSelectedStyle(btnToDate);
            v.postDelayed(() -> {
                if (!btnToDate.hasFocus()) {
                    applyButtonNormalStyle(btnToDate);
                }
            }, 200);
        });

        btnSearch.setOnClickListener(v -> {
            performSearch();
            applyButtonSelectedStyle(btnSearch);
            v.postDelayed(() -> {
                if (!btnSearch.hasFocus()) {
                    applyButtonNormalStyle(btnSearch);
                }
            }, 200);
        });

        btnClear.setOnClickListener(v -> {
            clearFilters();
            applyButtonSelectedStyle(btnClear);
            v.postDelayed(() -> {
                if (!btnClear.hasFocus()) {
                    applyButtonNormalStyle(btnClear);
                }
            }, 200);
        });

        btnExport.setOnClickListener(v -> {
            Toast.makeText(getActivity(), "Preparing export...", Toast.LENGTH_SHORT).show();
            applyButtonSelectedStyle(btnExport);
            checkStoragePermissionAndExport();
            v.postDelayed(() -> {
                if (!btnExport.hasFocus()) {
                    applyButtonNormalStyle(btnExport);
                }
            }, 200);
        });

        // Spinner selection listeners to update focus style
        spinnerVehicleType.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (spinnerVehicleType.hasFocus()) {
                    highlightView(spinnerVehicleType);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        spinnerMaterial.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (spinnerMaterial.hasFocus()) {
                    highlightView(spinnerMaterial);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        spinnerParty.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (spinnerParty.hasFocus()) {
                    highlightView(spinnerParty);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    private void checkStoragePermissionAndExport() {
        if (entryList.isEmpty()) {
            Toast.makeText(getActivity(), "No data to export", Toast.LENGTH_SHORT).show();
            return;
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // Android 11+ (API 30+)
            if (!Environment.isExternalStorageManager()) {
                requestManageStoragePermission();
                return;
            }
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            // Android 6-10 (API 23-29)
            if (ContextCompat.checkSelfPermission(getActivity(),
                    android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    != PackageManager.PERMISSION_GRANTED) {

                requestPermissions(new String[]{
                        android.Manifest.permission.WRITE_EXTERNAL_STORAGE,
                        android.Manifest.permission.READ_EXTERNAL_STORAGE
                }, STORAGE_PERMISSION_CODE);
                return;
            }
        }

        // Permission already granted, show export dialog
        showExportDialog();
    }

    private void requestManageStoragePermission() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle("Storage Permission Required");
        builder.setMessage("To export files, this app needs access to manage files. Please grant the permission in the next screen.");
        builder.setPositiveButton("Grant", (dialog, which) -> {
            try {
                Intent intent = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
                intent.addCategory("android.intent.category.DEFAULT");
                intent.setData(Uri.parse(String.format("package:%s", getActivity().getPackageName())));
                startActivityForResult(intent, MANAGE_STORAGE_CODE);
            } catch (Exception e) {
                Intent intent = new Intent();
                intent.setAction(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION);
                startActivityForResult(intent, MANAGE_STORAGE_CODE);
            }
        });
        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == STORAGE_PERMISSION_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                showExportDialog();
            } else {
                Toast.makeText(getActivity(), "Storage permission required to export files",
                        Toast.LENGTH_LONG).show();
            }
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == MANAGE_STORAGE_CODE) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                if (Environment.isExternalStorageManager()) {
                    showExportDialog();
                } else {
                    Toast.makeText(getActivity(), "Storage permission required to export files",
                            Toast.LENGTH_LONG).show();
                }
            }
        }
    }

    private void showDatePicker(boolean isFromDate) {
        DatePickerDialog datePickerDialog = new DatePickerDialog(
                getActivity(),
                (view, year, month, dayOfMonth) -> {
                    calendar.set(year, month, dayOfMonth);
                    String selectedDate = dateFormat.format(calendar.getTime());

                    if (isFromDate) {
                        fromDate = selectedDate;
                        btnFromDate.setText(formatDisplayDate(selectedDate));
                    } else {
                        toDate = selectedDate;
                        btnToDate.setText(formatDisplayDate(selectedDate));
                    }
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
        );
        datePickerDialog.show();
    }

    private void loadInitialData() {
        performSearch();
    }

    private void performSearch() {
        String vehicleType = spinnerVehicleType.getSelectedItem() != null ?
                spinnerVehicleType.getSelectedItem().toString() : "All Vehicle Types";
        String material = spinnerMaterial.getSelectedItem() != null ?
                spinnerMaterial.getSelectedItem().toString() : "All Materials";
        String party = spinnerParty.getSelectedItem() != null ?
                spinnerParty.getSelectedItem().toString() : "All Parties";

        // Convert "All" selections to empty string for database query
        String queryVehicleType = vehicleType.equals("All Vehicle Types") ? "" : vehicleType;
        String queryMaterial = material.equals("All Materials") ? "" : material;
        String queryParty = party.equals("All Parties") ? "" : party;

        entryList.clear();
        entryList.addAll(databaseHelper.searchWeighments(
                queryVehicleType, queryMaterial, queryParty, fromDate, toDate));

        adapter.notifyDataSetChanged();
        updateSummary();

        Toast.makeText(getActivity(), "Found " + entryList.size() + " entries",
                Toast.LENGTH_SHORT).show();
    }

    private void updateSummary() {
        txtTotalEntries.setText(String.valueOf(entryList.size()));
        long totalNet = databaseHelper.getTotalNetWeight(entryList);
        txtTotalNetWeight.setText(totalNet + " kg");
    }

    private void clearFilters() {
        spinnerVehicleType.setSelection(0);
        spinnerMaterial.setSelection(0);
        spinnerParty.setSelection(0);

        // Reset dates to last 30 days
        Calendar fromCal = Calendar.getInstance();
        fromCal.add(Calendar.DAY_OF_MONTH, -30);
        fromDate = dateFormat.format(fromCal.getTime());
        toDate = dateFormat.format(Calendar.getInstance().getTime());

        btnFromDate.setText(formatDisplayDate(fromDate));
        btnToDate.setText(formatDisplayDate(toDate));

        performSearch();
    }

    private void showExportDialog() {
        if (getActivity() == null || getActivity().isFinishing()) {
            return;
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle("📊 Export Report");
        builder.setMessage("Choose export format:");

        LinearLayout layout = new LinearLayout(getActivity());
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(50, 20, 50, 20);

        TextView csvOption = new TextView(getActivity());
        csvOption.setText("📄 CSV File");
        csvOption.setTextSize(18);
        csvOption.setPadding(20, 20, 20, 20);
        csvOption.setBackgroundResource(android.R.drawable.list_selector_background);
        csvOption.setOnClickListener(v -> {
            exportToCSV();
            if (getActivity() != null) {
                AlertDialog dialog = (AlertDialog) v.getTag();
                if (dialog != null) dialog.dismiss();
            }
        });

        TextView pdfOption = new TextView(getActivity());
        pdfOption.setText("📑 PDF File");
        pdfOption.setTextSize(18);
        pdfOption.setPadding(20, 20, 20, 20);
        pdfOption.setBackgroundResource(android.R.drawable.list_selector_background);
        pdfOption.setOnClickListener(v -> {
            exportToPDF();
            if (getActivity() != null) {
                AlertDialog dialog = (AlertDialog) v.getTag();
                if (dialog != null) dialog.dismiss();
            }
        });

        View divider = new View(getActivity());
        divider.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 1));
        divider.setBackgroundColor(Color.GRAY);

        layout.addView(csvOption);
        layout.addView(divider);
        layout.addView(pdfOption);

        builder.setView(layout);
        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss());

        AlertDialog dialog = builder.create();
        csvOption.setTag(dialog);
        pdfOption.setTag(dialog);
        dialog.show();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setLayout(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT);
        }
    }

    private void exportToCSV() {
        try {
            String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
            String fileName = "Weighment_Report_" + timeStamp + ".csv";

            File downloadDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);

            if (!downloadDir.exists()) {
                downloadDir.mkdirs();
            }

            File file = new File(downloadDir, fileName);

            FileWriter writer = new FileWriter(file);

            writer.append("Serial No,Vehicle No,Vehicle Type,Material,Party,Charge,Gross,Tare,Manual Tare,Net Weight,Date\n");

            SimpleDateFormat displayFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());

            for (WeighmentEntry entry : entryList) {
                writer.append(entry.getSerialNo()).append(",");
                writer.append(escapeCSV(entry.getVehicleNo())).append(",");
                writer.append(escapeCSV(entry.getVehicleType())).append(",");
                writer.append(escapeCSV(entry.getMaterial())).append(",");
                writer.append(escapeCSV(entry.getParty())).append(",");
                writer.append(escapeCSV(entry.getCharge())).append(",");
                writer.append(entry.getGross()).append(",");
                writer.append(entry.getTare()).append(",");
                writer.append(entry.getManualTare()).append(",");
                writer.append(entry.getNet()).append(",");

                String formattedDate = entry.getTimestamp();
                try {
                    SimpleDateFormat dbFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
                    Date date = dbFormat.parse(entry.getTimestamp());
                    formattedDate = displayFormat.format(date);
                } catch (Exception e) {
                    // Use original if parsing fails
                }

                writer.append(formattedDate).append("\n");
            }

            writer.flush();
            writer.close();

            Toast.makeText(getActivity(), "✅ CSV exported: " + fileName, Toast.LENGTH_LONG).show();
            openFile(file);

        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(getActivity(), "❌ Error exporting CSV: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private String escapeCSV(String value) {
        if (value == null) return "";
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }

    private void exportToPDF() {
        try {
            String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
            String fileName = "Weighment_Report_" + timeStamp + ".pdf";

            File downloadDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);

            if (!downloadDir.exists()) {
                downloadDir.mkdirs();
            }

            File file = new File(downloadDir, fileName);

            PdfWriter writer = new PdfWriter(file.getAbsolutePath());
            PdfDocument pdfDoc = new PdfDocument(writer);
            Document document = new Document(pdfDoc);

            Paragraph title = new Paragraph("WEIGHMENT REPORT")
                    .setTextAlignment(TextAlignment.CENTER)
                    .setFontSize(18)
                    .setBold();
            document.add(title);
            document.add(new Paragraph("\n"));

            String vehicleType = spinnerVehicleType.getSelectedItem().toString();
            String material = spinnerMaterial.getSelectedItem().toString();
            String party = spinnerParty.getSelectedItem().toString();

            document.add(new Paragraph("Filters:")
                    .setBold()
                    .setFontSize(12));
            document.add(new Paragraph(String.format("Vehicle Type: %s | Material: %s | Party: %s",
                    vehicleType, material, party))
                    .setFontSize(10));
            document.add(new Paragraph(String.format("Date Range: %s to %s",
                    formatDisplayDate(fromDate), formatDisplayDate(toDate)))
                    .setFontSize(10));
            document.add(new Paragraph("\n"));

            document.add(new Paragraph("Summary:")
                    .setBold()
                    .setFontSize(12));
            document.add(new Paragraph(String.format("Total Entries: %d | Total Net Weight: %d kg",
                    entryList.size(), databaseHelper.getTotalNetWeight(entryList)))
                    .setFontSize(11)
                    .setBold());
            document.add(new Paragraph("\n"));

            float[] columnWidths = {1, 2, 1.5f, 2, 2, 1, 1, 1, 1, 1.5f};
            Table table = new Table(UnitValue.createPercentArray(columnWidths));
            table.setWidth(UnitValue.createPercentValue(100));

            String[] headers = {"Slip No", "Vehicle No", "Type", "Material", "Party",
                    "Gross", "Tare", "Manual", "Net", "Date"};

            for (String header : headers) {
                Cell headerCell = new Cell()
                        .add(new Paragraph(header).setBold())
                        .setTextAlignment(TextAlignment.CENTER)
                        .setBackgroundColor(ColorConstants.LIGHT_GRAY);
                table.addCell(headerCell);
            }

            SimpleDateFormat displayFormat = new SimpleDateFormat("dd/MM/yy", Locale.getDefault());

            for (WeighmentEntry entry : entryList) {
                table.addCell(new Cell().add(new Paragraph(entry.getSerialNo())).setTextAlignment(TextAlignment.CENTER));
                table.addCell(new Cell().add(new Paragraph(entry.getVehicleNo())).setTextAlignment(TextAlignment.LEFT));
                table.addCell(new Cell().add(new Paragraph(entry.getVehicleType())).setTextAlignment(TextAlignment.LEFT));
                table.addCell(new Cell().add(new Paragraph(entry.getMaterial())).setTextAlignment(TextAlignment.LEFT));
                table.addCell(new Cell().add(new Paragraph(entry.getParty())).setTextAlignment(TextAlignment.LEFT));
                table.addCell(new Cell().add(new Paragraph(entry.getGross())).setTextAlignment(TextAlignment.RIGHT));
                table.addCell(new Cell().add(new Paragraph(entry.getTare())).setTextAlignment(TextAlignment.RIGHT));
                table.addCell(new Cell().add(new Paragraph(entry.getManualTare())).setTextAlignment(TextAlignment.RIGHT));
                table.addCell(new Cell().add(new Paragraph(entry.getNet())).setTextAlignment(TextAlignment.RIGHT));

                String formattedDate = entry.getTimestamp();
                try {
                    SimpleDateFormat dbFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
                    Date date = dbFormat.parse(entry.getTimestamp());
                    formattedDate = displayFormat.format(date);
                } catch (Exception e) {
                    if (formattedDate != null && formattedDate.length() > 10) {
                        formattedDate = formattedDate.substring(0, 10);
                    }
                }

                table.addCell(new Cell().add(new Paragraph(formattedDate)).setTextAlignment(TextAlignment.CENTER));
            }

            document.add(table);
            document.close();

            Toast.makeText(getActivity(), "✅ PDF exported: " + fileName, Toast.LENGTH_LONG).show();
            openFile(file);

        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(getActivity(), "❌ Error exporting PDF: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void openFile(File file) {
        try {
            Uri fileUri = FileProvider.getUriForFile(getActivity(),
                    getActivity().getPackageName() + ".fileprovider", file);

            Intent intent = new Intent(Intent.ACTION_VIEW);
            String mimeType = file.getName().endsWith(".csv") ? "text/csv" : "application/pdf";

            intent.setDataAndType(fileUri, mimeType);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

            startActivity(Intent.createChooser(intent, "Open with"));

        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(getActivity(), "Cannot open file. Check Downloads folder.", Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        performSearch();

        // Request focus on vehicle type spinner when fragment resumes
        if (spinnerVehicleType != null) {
            spinnerVehicleType.post(() -> {
                spinnerVehicleType.requestFocus();
                highlightView(spinnerVehicleType);
            });
        }
    }
}