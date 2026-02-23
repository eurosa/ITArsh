package com.googleapi.bluetoothweight;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

public class DatabaseHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "WeighmentDB";
    private static final int DATABASE_VERSION = 2; // Increment version for new column

    // Table name
    private static final String TABLE_WEIGHMENT = "weighment_entries";

    // Column names
    private static final String COLUMN_ID = "id";
    private static final String COLUMN_SERIAL_NO = "serial_no";
    private static final String COLUMN_VEHICLE_NO = "vehicle_no";
    private static final String COLUMN_VEHICLE_TYPE = "vehicle_type";
    private static final String COLUMN_MATERIAL = "material";
    private static final String COLUMN_PARTY = "party";
    private static final String COLUMN_CHARGE = "charge";
    private static final String COLUMN_GROSS = "gross";
    private static final String COLUMN_TARE = "tare";
    private static final String COLUMN_MANUAL_TARE = "manual_tare";
    private static final String COLUMN_NET = "net";
    private static final String COLUMN_TIMESTAMP = "timestamp";
    private static final String COLUMN_FINALIZED = "finalized"; // New column to track finalized status

    // Create table query
    private static final String CREATE_TABLE = "CREATE TABLE " + TABLE_WEIGHMENT + "("
            + COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
            + COLUMN_SERIAL_NO + " TEXT UNIQUE,"
            + COLUMN_VEHICLE_NO + " TEXT,"
            + COLUMN_VEHICLE_TYPE + " TEXT,"
            + COLUMN_MATERIAL + " TEXT,"
            + COLUMN_PARTY + " TEXT,"
            + COLUMN_CHARGE + " TEXT,"
            + COLUMN_GROSS + " TEXT,"
            + COLUMN_TARE + " TEXT,"
            + COLUMN_MANUAL_TARE + " TEXT,"
            + COLUMN_NET + " TEXT,"
            + COLUMN_FINALIZED + " INTEGER DEFAULT 0," // 0 = not finalized, 1 = finalized
            + COLUMN_TIMESTAMP + " DATETIME DEFAULT CURRENT_TIMESTAMP"
            + ")";

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(CREATE_TABLE);
        Log.d("DatabaseHelper", "Table created successfully");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // For version upgrade, add the new column if it doesn't exist
        if (oldVersion < 2) {
            try {
                db.execSQL("ALTER TABLE " + TABLE_WEIGHMENT + " ADD COLUMN " + COLUMN_FINALIZED + " INTEGER DEFAULT 0");
                Log.d("DatabaseHelper", "Added finalized column to existing table");
            } catch (Exception e) {
                Log.e("DatabaseHelper", "Error adding finalized column: " + e.getMessage());
            }
        } else {
            // If upgrading from very old version, drop and recreate
            db.execSQL("DROP TABLE IF EXISTS " + TABLE_WEIGHMENT);
            onCreate(db);
        }
    }

    // Insert weighment entry
    public long insertWeighment(WeighmentEntry entry) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();

        values.put(COLUMN_SERIAL_NO, entry.getSerialNo());
        values.put(COLUMN_VEHICLE_NO, entry.getVehicleNo());
        values.put(COLUMN_VEHICLE_TYPE, entry.getVehicleType());
        values.put(COLUMN_MATERIAL, entry.getMaterial());
        values.put(COLUMN_PARTY, entry.getParty());
        values.put(COLUMN_CHARGE, entry.getCharge());
        values.put(COLUMN_GROSS, entry.getGross());
        values.put(COLUMN_TARE, entry.getTare());
        values.put(COLUMN_MANUAL_TARE, entry.getManualTare());
        values.put(COLUMN_NET, entry.getNet());
        values.put(COLUMN_FINALIZED, entry.isFinalized() ? 1 : 0); // Save finalized status

        long id = db.insert(TABLE_WEIGHMENT, null, values);
        db.close();
        Log.d("DatabaseHelper", "Inserted entry with serial: " + entry.getSerialNo() + ", ID: " + id);
        return id;
    }

    // Get all serial numbers for dropdown
    public List<String> getAllSerialNumbers() {
        List<String> serialNumbers = new ArrayList<>();
        String selectQuery = "SELECT " + COLUMN_SERIAL_NO + " FROM " + TABLE_WEIGHMENT + " ORDER BY " + COLUMN_TIMESTAMP + " DESC";

        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(selectQuery, null);

        if (cursor.moveToFirst()) {
            do {
                serialNumbers.add(cursor.getString(0));
            } while (cursor.moveToNext());
        }

        cursor.close();
        db.close();
        return serialNumbers;
    }

    // Get weighment entry by serial number
    public WeighmentEntry getWeighmentBySerialNo(String serialNo) {
        SQLiteDatabase db = this.getReadableDatabase();

        Cursor cursor = db.query(TABLE_WEIGHMENT, null, COLUMN_SERIAL_NO + "=?",
                new String[]{serialNo}, null, null, null);

        WeighmentEntry entry = null;

        if (cursor != null && cursor.moveToFirst()) {
            entry = new WeighmentEntry();
            entry.setId(cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_ID)));
            entry.setSerialNo(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_SERIAL_NO)));
            entry.setVehicleNo(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_VEHICLE_NO)));
            entry.setVehicleType(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_VEHICLE_TYPE)));
            entry.setMaterial(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_MATERIAL)));
            entry.setParty(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_PARTY)));
            entry.setCharge(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_CHARGE)));
            entry.setGross(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_GROSS)));
            entry.setTare(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_TARE)));
            entry.setManualTare(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_MANUAL_TARE)));
            entry.setNet(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_NET)));

            // Get finalized status (handle if column doesn't exist in older versions)
            int finalizedIndex = cursor.getColumnIndex(COLUMN_FINALIZED);
            if (finalizedIndex >= 0) {
                entry.setFinalized(cursor.getInt(finalizedIndex) == 1);
            } else {
                entry.setFinalized(false); // Default for old entries
            }

            entry.setTimestamp(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_TIMESTAMP)));
            cursor.close();
        }

        db.close();
        return entry;
    }

    // Get next serial number
    public int getNextSerialNumber() {
        SQLiteDatabase db = this.getReadableDatabase();
        String query = "SELECT MAX(CAST(" + COLUMN_SERIAL_NO + " AS INTEGER)) FROM " + TABLE_WEIGHMENT;
        Cursor cursor = db.rawQuery(query, null);

        int nextSerial = 1; // Default to 1 if no records

        if (cursor.moveToFirst()) {
            int maxSerial = cursor.getInt(0);
            nextSerial = maxSerial + 1;
        }

        cursor.close();
        db.close();

        return nextSerial;
    }

    // Update weighment entry
    public int updateWeighment(WeighmentEntry entry) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();

        values.put(COLUMN_VEHICLE_NO, entry.getVehicleNo());
        values.put(COLUMN_VEHICLE_TYPE, entry.getVehicleType());
        values.put(COLUMN_MATERIAL, entry.getMaterial());
        values.put(COLUMN_PARTY, entry.getParty());
        values.put(COLUMN_CHARGE, entry.getCharge());
        values.put(COLUMN_GROSS, entry.getGross());
        values.put(COLUMN_TARE, entry.getTare());
        values.put(COLUMN_MANUAL_TARE, entry.getManualTare());
        values.put(COLUMN_NET, entry.getNet());
        values.put(COLUMN_FINALIZED, entry.isFinalized() ? 1 : 0); // Update finalized status

        return db.update(TABLE_WEIGHMENT, values, COLUMN_SERIAL_NO + "=?",
                new String[]{entry.getSerialNo()});
    }

    // Delete weighment entry
    public void deleteWeighment(String serialNo) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_WEIGHMENT, COLUMN_SERIAL_NO + "=?", new String[]{serialNo});
        db.close();
    }

    // Check if serial number exists
    public boolean isSerialNoExists(String serialNo) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(TABLE_WEIGHMENT, new String[]{COLUMN_ID},
                COLUMN_SERIAL_NO + "=?", new String[]{serialNo}, null, null, null);
        boolean exists = cursor.getCount() > 0;
        cursor.close();
        db.close();
        return exists;
    }


    // Get unique values for dropdowns
    public List<String> getUniqueVehicleTypes() {
        List<String> list = new ArrayList<>();
        list.add("All"); // Add default option

        String query = "SELECT DISTINCT " + COLUMN_VEHICLE_TYPE + " FROM " + TABLE_WEIGHMENT
                + " WHERE " + COLUMN_VEHICLE_TYPE + " IS NOT NULL AND "
                + COLUMN_VEHICLE_TYPE + " != '' ORDER BY " + COLUMN_VEHICLE_TYPE;

        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(query, null);

        if (cursor.moveToFirst()) {
            do {
                list.add(cursor.getString(0));
            } while (cursor.moveToNext());
        }

        cursor.close();
        db.close();
        return list;
    }

    public List<String> getUniqueMaterials() {
        List<String> list = new ArrayList<>();
        list.add("All");

        String query = "SELECT DISTINCT " + COLUMN_MATERIAL + " FROM " + TABLE_WEIGHMENT
                + " WHERE " + COLUMN_MATERIAL + " IS NOT NULL AND "
                + COLUMN_MATERIAL + " != '' ORDER BY " + COLUMN_MATERIAL;

        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(query, null);

        if (cursor.moveToFirst()) {
            do {
                list.add(cursor.getString(0));
            } while (cursor.moveToNext());
        }

        cursor.close();
        db.close();
        return list;
    }

    public List<String> getUniqueParties() {
        List<String> list = new ArrayList<>();
        list.add("All");

        String query = "SELECT DISTINCT " + COLUMN_PARTY + " FROM " + TABLE_WEIGHMENT
                + " WHERE " + COLUMN_PARTY + " IS NOT NULL AND "
                + COLUMN_PARTY + " != '' ORDER BY " + COLUMN_PARTY;

        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(query, null);

        if (cursor.moveToFirst()) {
            do {
                list.add(cursor.getString(0));
            } while (cursor.moveToNext());
        }

        cursor.close();
        db.close();
        return list;
    }

    // Search method with filters
    public List<WeighmentEntry> searchWeighments(String vehicleType, String material,
                                                 String party, String fromDate, String toDate) {
        List<WeighmentEntry> entryList = new ArrayList<>();

        StringBuilder queryBuilder = new StringBuilder();
        queryBuilder.append("SELECT * FROM ").append(TABLE_WEIGHMENT).append(" WHERE 1=1");

        List<String> args = new ArrayList<>();

        if (vehicleType != null && !vehicleType.isEmpty() && !vehicleType.equals("All")) {
            queryBuilder.append(" AND ").append(COLUMN_VEHICLE_TYPE).append(" = ?");
            args.add(vehicleType);
        }

        if (material != null && !material.isEmpty() && !material.equals("All")) {
            queryBuilder.append(" AND ").append(COLUMN_MATERIAL).append(" = ?");
            args.add(material);
        }

        if (party != null && !party.isEmpty() && !party.equals("All")) {
            queryBuilder.append(" AND ").append(COLUMN_PARTY).append(" = ?");
            args.add(party);
        }

        if (fromDate != null && !fromDate.isEmpty()) {
            queryBuilder.append(" AND date(").append(COLUMN_TIMESTAMP).append(") >= ?");
            args.add(fromDate);
        }

        if (toDate != null && !toDate.isEmpty()) {
            queryBuilder.append(" AND date(").append(COLUMN_TIMESTAMP).append(") <= ?");
            args.add(toDate);
        }

        queryBuilder.append(" ORDER BY ").append(COLUMN_TIMESTAMP).append(" DESC");

        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(queryBuilder.toString(), args.toArray(new String[0]));

        if (cursor.moveToFirst()) {
            do {
                WeighmentEntry entry = new WeighmentEntry();
                entry.setId(cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_ID)));
                entry.setSerialNo(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_SERIAL_NO)));
                entry.setVehicleNo(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_VEHICLE_NO)));
                entry.setVehicleType(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_VEHICLE_TYPE)));
                entry.setMaterial(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_MATERIAL)));
                entry.setParty(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_PARTY)));
                entry.setGross(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_GROSS)));
                entry.setTare(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_TARE)));
                entry.setManualTare(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_MANUAL_TARE)));
                entry.setNet(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_NET)));
                entry.setTimestamp(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_TIMESTAMP)));
                entryList.add(entry);
            } while (cursor.moveToNext());
        }

        cursor.close();
        db.close();
        return entryList;
    }

    // Get total net weight sum
    public long getTotalNetWeight(List<WeighmentEntry> entries) {
        long total = 0;
        for (WeighmentEntry entry : entries) {
            try {
                total += Long.parseLong(entry.getNet());
            } catch (NumberFormatException e) {
                // Ignore parsing errors
            }
        }
        return total;
    }
}