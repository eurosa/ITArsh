package com.googleapi.bluetoothweight;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.AppCompatButton;
import androidx.appcompat.widget.AppCompatSpinner;
import androidx.core.content.FileProvider;
import androidx.core.content.ContextCompat;
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

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_c, container, false);

        initializeViews(view);
        setupDateFormats();
        setupSpinners();
        setupListeners();
        loadInitialData();

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
    }

    /**
     * Public method to refresh report data
     * This can be called from MainActivity when F5 is pressed
     */
    public void refreshReport() {
        if (isAdded() && getActivity() != null) {
            performSearch();
            Toast.makeText(getActivity(), "Report Refreshed", Toast.LENGTH_SHORT).show();
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
        ArrayAdapter<String> vehicleAdapter = new ArrayAdapter<>(getActivity(),
                android.R.layout.simple_spinner_item, vehicleTypes);
        vehicleAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerVehicleType.setAdapter(vehicleAdapter);

        // Material Spinner
        List<String> materials = databaseHelper.getUniqueMaterials();
        ArrayAdapter<String> materialAdapter = new ArrayAdapter<>(getActivity(),
                android.R.layout.simple_spinner_item, materials);
        materialAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerMaterial.setAdapter(materialAdapter);

        // Party Spinner
        List<String> parties = databaseHelper.getUniqueParties();
        ArrayAdapter<String> partyAdapter = new ArrayAdapter<>(getActivity(),
                android.R.layout.simple_spinner_item, parties);
        partyAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerParty.setAdapter(partyAdapter);
    }

    private void setupListeners() {
        btnFromDate.setOnClickListener(v -> showDatePicker(true));
        btnToDate.setOnClickListener(v -> showDatePicker(false));
        btnSearch.setOnClickListener(v -> performSearch());
        btnClear.setOnClickListener(v -> clearFilters());
        btnExport.setOnClickListener(v -> {
            // Show toast to confirm button click
            Toast.makeText(getActivity(), "Preparing export...", Toast.LENGTH_SHORT).show();
            checkStoragePermissionAndExport();
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
                spinnerVehicleType.getSelectedItem().toString() : "All";
        String material = spinnerMaterial.getSelectedItem() != null ?
                spinnerMaterial.getSelectedItem().toString() : "All";
        String party = spinnerParty.getSelectedItem() != null ?
                spinnerParty.getSelectedItem().toString() : "All";

        entryList.clear();
        entryList.addAll(databaseHelper.searchWeighments(
                vehicleType, material, party, fromDate, toDate));

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

    /**
     * Fixed showExportDialog method with better visibility
     */
    private void showExportDialog() {
        if (getActivity() == null || getActivity().isFinishing()) {
            return;
        }

        // Create and show the dialog
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle("📊 Export Report");
        builder.setMessage("Choose export format:");

        // Create custom view for dialog
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
                // Dismiss all dialogs
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

        // Add divider
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

        // Set tags for click listeners
        csvOption.setTag(dialog);
        pdfOption.setTag(dialog);

        dialog.show();

        // Make sure dialog window is properly sized
        if (dialog.getWindow() != null) {
            dialog.getWindow().setLayout(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT);
        }
    }

    /**
     * Alternative simple dialog if the custom view doesn't work
     */
    /*
    private void showExportDialog() {
        if (getActivity() == null || getActivity().isFinishing()) {
            return;
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle("Export Report");
        builder.setMessage("Choose export format:");

        String[] options = {"CSV File", "PDF File"};

        builder.setItems(options, (dialog, which) -> {
            if (which == 0) {
                exportToCSV();
            } else {
                exportToPDF();
            }
        });

        builder.setNegativeButton("Cancel", null);

        AlertDialog dialog = builder.create();
        dialog.show();
    }
    */

    private void exportToCSV() {
        try {
            String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
            String fileName = "Weighment_Report_" + timeStamp + ".csv";

            File downloadDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);

            // Create directory if it doesn't exist
            if (!downloadDir.exists()) {
                downloadDir.mkdirs();
            }

            File file = new File(downloadDir, fileName);

            FileWriter writer = new FileWriter(file);

            // Write CSV header
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

            // Open the file
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

            // Create directory if it doesn't exist
            if (!downloadDir.exists()) {
                downloadDir.mkdirs();
            }

            File file = new File(downloadDir, fileName);

            // Initialize PDF writer
            PdfWriter writer = new PdfWriter(file.getAbsolutePath());
            PdfDocument pdfDoc = new PdfDocument(writer);
            Document document = new Document(pdfDoc);

            // Add title
            Paragraph title = new Paragraph("WEIGHMENT REPORT")
                    .setTextAlignment(TextAlignment.CENTER)
                    .setFontSize(18)
                    .setBold();
            document.add(title);
            document.add(new Paragraph("\n"));

            // Add filter information
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

            // Add summary
            document.add(new Paragraph("Summary:")
                    .setBold()
                    .setFontSize(12));
            document.add(new Paragraph(String.format("Total Entries: %d | Total Net Weight: %d kg",
                    entryList.size(), databaseHelper.getTotalNetWeight(entryList)))
                    .setFontSize(11)
                    .setBold());
            document.add(new Paragraph("\n"));

            // Create table
            float[] columnWidths = {1, 2, 1.5f, 2, 2, 1, 1, 1, 1, 1.5f};
            Table table = new Table(UnitValue.createPercentArray(columnWidths));
            table.setWidth(UnitValue.createPercentValue(100));

            // Add headers
            String[] headers = {"Slip No", "Vehicle No", "Type", "Material", "Party",
                    "Gross", "Tare", "Manual", "Net", "Date"};

            for (String header : headers) {
                Cell headerCell = new Cell()
                        .add(new Paragraph(header).setBold())
                        .setTextAlignment(TextAlignment.CENTER)
                        .setBackgroundColor(ColorConstants.LIGHT_GRAY);
                table.addCell(headerCell);
            }

            // Add data rows
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

            // Open the file
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
    }
}