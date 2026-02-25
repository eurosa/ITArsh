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
    private static final int DATABASE_VERSION = 3; // Increment version for master data table

    // Table names
    private static final String TABLE_WEIGHMENT = "weighment_entries";
    private static final String TABLE_MASTER_DATA = "master_data";

    // Weighment table column names
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
    private static final String COLUMN_FINALIZED = "finalized";

    // Master data table column names
    private static final String COLUMN_MASTER_ID = "id";
    private static final String COLUMN_MASTER_TYPE = "data_type";
    private static final String COLUMN_MASTER_VALUE = "data_value";
    private static final String COLUMN_MASTER_TIMESTAMP = "timestamp";

    // Master data types
    public static final String TYPE_VEHICLE_TYPE = "VEHICLE_TYPE";
    public static final String TYPE_VEHICLE_NO = "VEHICLE_NO";
    public static final String TYPE_MATERIAL = "MATERIAL";
    public static final String TYPE_PARTY = "PARTY";

    // Create weighment table query
    private static final String CREATE_WEIGHMENT_TABLE = "CREATE TABLE " + TABLE_WEIGHMENT + "("
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
            + COLUMN_FINALIZED + " INTEGER DEFAULT 0,"
            + COLUMN_TIMESTAMP + " DATETIME DEFAULT CURRENT_TIMESTAMP"
            + ")";

    // Create master data table query
    private static final String CREATE_MASTER_TABLE = "CREATE TABLE " + TABLE_MASTER_DATA + "("
            + COLUMN_MASTER_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
            + COLUMN_MASTER_TYPE + " TEXT NOT NULL,"
            + COLUMN_MASTER_VALUE + " TEXT NOT NULL,"
            + COLUMN_MASTER_TIMESTAMP + " DATETIME DEFAULT CURRENT_TIMESTAMP,"
            + "UNIQUE(" + COLUMN_MASTER_TYPE + ", " + COLUMN_MASTER_VALUE + ")"
            + ")";

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(CREATE_WEIGHMENT_TABLE);
        db.execSQL(CREATE_MASTER_TABLE);
        Log.d("DatabaseHelper", "Tables created successfully");

        // Insert default master data
        insertDefaultMasterData(db);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // Handle upgrade from version 1 to 2
        if (oldVersion < 2) {
            try {
                db.execSQL("ALTER TABLE " + TABLE_WEIGHMENT + " ADD COLUMN " + COLUMN_FINALIZED + " INTEGER DEFAULT 0");
                Log.d("DatabaseHelper", "Added finalized column to existing table");
            } catch (Exception e) {
                Log.e("DatabaseHelper", "Error adding finalized column: " + e.getMessage());
            }
        }

        // Handle upgrade from version 2 to 3
        if (oldVersion < 3) {
            try {
                db.execSQL(CREATE_MASTER_TABLE);
                Log.d("DatabaseHelper", "Created master data table");

                // Extract existing unique values from weighment table and add to master data
                migrateExistingDataToMaster(db);

                // Insert default master data for missing values
                insertDefaultMasterData(db);
            } catch (Exception e) {
                Log.e("DatabaseHelper", "Error creating master data table: " + e.getMessage());
            }
        }
    }

    private void insertDefaultMasterData(SQLiteDatabase db) {
        ContentValues values = new ContentValues();

        // Default vehicle types
        String[] defaultVehicleTypes = {"Truck", "Tractor", "Mini Truck", "Container", "Trailer"};
        for (String type : defaultVehicleTypes) {
            if (!isMasterDataExists(db, TYPE_VEHICLE_TYPE, type)) {
                values.clear();
                values.put(COLUMN_MASTER_TYPE, TYPE_VEHICLE_TYPE);
                values.put(COLUMN_MASTER_VALUE, type);
                db.insert(TABLE_MASTER_DATA, null, values);
            }
        }

        // Default vehicle numbers
        String[] defaultVehicleNos = {"MH01AB1234", "MH02CD5678", "MH03EF9012", "MH04GH3456", "MH05IJ7890"};
        for (String vehicleNo : defaultVehicleNos) {
            if (!isMasterDataExists(db, TYPE_VEHICLE_NO, vehicleNo)) {
                values.clear();
                values.put(COLUMN_MASTER_TYPE, TYPE_VEHICLE_NO);
                values.put(COLUMN_MASTER_VALUE, vehicleNo);
                db.insert(TABLE_MASTER_DATA, null, values);
            }
        }

        // Default materials
        String[] defaultMaterials = {"Sand", "Cement", "Bricks", "Steel", "Aggregate", "Stone", "Wood"};
        for (String material : defaultMaterials) {
            if (!isMasterDataExists(db, TYPE_MATERIAL, material)) {
                values.clear();
                values.put(COLUMN_MASTER_TYPE, TYPE_MATERIAL);
                values.put(COLUMN_MASTER_VALUE, material);
                db.insert(TABLE_MASTER_DATA, null, values);
            }
        }

        // Default parties
        String[] defaultParties = {"ABC Construction", "XYZ Builders", "PQR Infrastructure",
                "LMN Enterprises", "DEF Developers", "GHI Materials"};
        for (String party : defaultParties) {
            if (!isMasterDataExists(db, TYPE_PARTY, party)) {
                values.clear();
                values.put(COLUMN_MASTER_TYPE, TYPE_PARTY);
                values.put(COLUMN_MASTER_VALUE, party);
                db.insert(TABLE_MASTER_DATA, null, values);
            }
        }
    }

    private void migrateExistingDataToMaster(SQLiteDatabase db) {
        // Migrate vehicle types
        migrateColumnToMaster(db, COLUMN_VEHICLE_TYPE, TYPE_VEHICLE_TYPE);

        // Migrate vehicle numbers
        migrateColumnToMaster(db, COLUMN_VEHICLE_NO, TYPE_VEHICLE_NO);

        // Migrate materials
        migrateColumnToMaster(db, COLUMN_MATERIAL, TYPE_MATERIAL);

        // Migrate parties
        migrateColumnToMaster(db, COLUMN_PARTY, TYPE_PARTY);
    }

    private void migrateColumnToMaster(SQLiteDatabase db, String columnName, String masterType) {
        String query = "SELECT DISTINCT " + columnName + " FROM " + TABLE_WEIGHMENT
                + " WHERE " + columnName + " IS NOT NULL AND " + columnName + " != ''";

        Cursor cursor = db.rawQuery(query, null);
        ContentValues values = new ContentValues();

        if (cursor.moveToFirst()) {
            do {
                String value = cursor.getString(0);
                if (!isMasterDataExists(db, masterType, value)) {
                    values.clear();
                    values.put(COLUMN_MASTER_TYPE, masterType);
                    values.put(COLUMN_MASTER_VALUE, value);
                    db.insert(TABLE_MASTER_DATA, null, values);
                }
            } while (cursor.moveToNext());
        }

        cursor.close();
    }

    private boolean isMasterDataExists(SQLiteDatabase db, String type, String value) {
        Cursor cursor = db.query(TABLE_MASTER_DATA, new String[]{COLUMN_MASTER_ID},
                COLUMN_MASTER_TYPE + "=? AND " + COLUMN_MASTER_VALUE + "=?",
                new String[]{type, value}, null, null, null);
        boolean exists = cursor.getCount() > 0;
        cursor.close();
        return exists;
    }

    // ==================== WEIGHMENT METHODS ====================

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
        values.put(COLUMN_FINALIZED, entry.isFinalized() ? 1 : 0);

        long id = db.insert(TABLE_WEIGHMENT, null, values);

        // Add new values to master data if they don't exist
        if (entry.getVehicleNo() != null && !entry.getVehicleNo().isEmpty()) {
            addToMasterDataIfNotExists(db, TYPE_VEHICLE_NO, entry.getVehicleNo());
        }
        if (entry.getVehicleType() != null && !entry.getVehicleType().isEmpty()) {
            addToMasterDataIfNotExists(db, TYPE_VEHICLE_TYPE, entry.getVehicleType());
        }
        if (entry.getMaterial() != null && !entry.getMaterial().isEmpty()) {
            addToMasterDataIfNotExists(db, TYPE_MATERIAL, entry.getMaterial());
        }
        if (entry.getParty() != null && !entry.getParty().isEmpty()) {
            addToMasterDataIfNotExists(db, TYPE_PARTY, entry.getParty());
        }

        db.close();
        Log.d("DatabaseHelper", "Inserted entry with serial: " + entry.getSerialNo() + ", ID: " + id);
        return id;
    }

    private void addToMasterDataIfNotExists(SQLiteDatabase db, String type, String value) {
        if (!isMasterDataExists(db, type, value)) {
            ContentValues values = new ContentValues();
            values.put(COLUMN_MASTER_TYPE, type);
            values.put(COLUMN_MASTER_VALUE, value);
            db.insert(TABLE_MASTER_DATA, null, values);
        }
    }

    public List<String> getAllSerialNumbers() {
        List<String> serialNumbers = new ArrayList<>();
        String selectQuery = "SELECT " + COLUMN_SERIAL_NO + " FROM " + TABLE_WEIGHMENT
                + " ORDER BY " + COLUMN_TIMESTAMP + " DESC";

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

            int finalizedIndex = cursor.getColumnIndex(COLUMN_FINALIZED);
            if (finalizedIndex >= 0) {
                entry.setFinalized(cursor.getInt(finalizedIndex) == 1);
            } else {
                entry.setFinalized(false);
            }

            entry.setTimestamp(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_TIMESTAMP)));
            cursor.close();
        }

        db.close();
        return entry;
    }

    public int getNextSerialNumber() {
        SQLiteDatabase db = this.getReadableDatabase();
        String query = "SELECT MAX(CAST(" + COLUMN_SERIAL_NO + " AS INTEGER)) FROM " + TABLE_WEIGHMENT;
        Cursor cursor = db.rawQuery(query, null);

        int nextSerial = 1;

        if (cursor.moveToFirst()) {
            int maxSerial = cursor.getInt(0);
            nextSerial = maxSerial + 1;
        }

        cursor.close();
        db.close();

        return nextSerial;
    }

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
        values.put(COLUMN_FINALIZED, entry.isFinalized() ? 1 : 0);

        int result = db.update(TABLE_WEIGHMENT, values, COLUMN_SERIAL_NO + "=?",
                new String[]{entry.getSerialNo()});

        db.close();
        return result;
    }

    public void deleteWeighment(String serialNo) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_WEIGHMENT, COLUMN_SERIAL_NO + "=?", new String[]{serialNo});
        db.close();
    }

    public boolean isSerialNoExists(String serialNo) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(TABLE_WEIGHMENT, new String[]{COLUMN_ID},
                COLUMN_SERIAL_NO + "=?", new String[]{serialNo}, null, null, null);
        boolean exists = cursor.getCount() > 0;
        cursor.close();
        db.close();
        return exists;
    }

    // ==================== MASTER DATA METHODS ====================

    public long insertMasterData(String type, String value) {
        SQLiteDatabase db = this.getWritableDatabase();

        if (isMasterDataExists(db, type, value)) {
            db.close();
            return -1; // Already exists
        }

        ContentValues values = new ContentValues();
        values.put(COLUMN_MASTER_TYPE, type);
        values.put(COLUMN_MASTER_VALUE, value);

        long id = db.insert(TABLE_MASTER_DATA, null, values);
        db.close();
        return id;
    }

    public List<MasterData> getAllMasterDataByType(String type) {
        List<MasterData> list = new ArrayList<>();
        String selectQuery = "SELECT * FROM " + TABLE_MASTER_DATA
                + " WHERE " + COLUMN_MASTER_TYPE + " = ?"
                + " ORDER BY " + COLUMN_MASTER_VALUE + " COLLATE NOCASE";

        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(selectQuery, new String[]{type});

        if (cursor.moveToFirst()) {
            do {
                MasterData data = new MasterData();
                data.setId(cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_MASTER_ID)));
                data.setType(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_MASTER_TYPE)));
                data.setValue(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_MASTER_VALUE)));
                data.setTimestamp(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_MASTER_TIMESTAMP)));
                list.add(data);
            } while (cursor.moveToNext());
        }

        cursor.close();
        db.close();
        return list;
    }

    public List<String> getMasterDataValuesByType(String type) {
        List<String> list = new ArrayList<>();
        list.add("All"); // Add default option

        List<MasterData> dataList = getAllMasterDataByType(type);
        for (MasterData data : dataList) {
            list.add(data.getValue());
        }

        return list;
    }

    public int updateMasterData(int id, String newValue) {
        SQLiteDatabase db = this.getWritableDatabase();

        // Check if the new value already exists for this type
        MasterData oldData = getMasterDataById(id);
        if (oldData != null) {
            if (isMasterDataExists(db, oldData.getType(), newValue)) {
                db.close();
                return -1; // Already exists
            }
        }

        ContentValues values = new ContentValues();
        values.put(COLUMN_MASTER_VALUE, newValue);

        int result = db.update(TABLE_MASTER_DATA, values, COLUMN_MASTER_ID + "=?",
                new String[]{String.valueOf(id)});
        db.close();
        return result;
    }

    private MasterData getMasterDataById(int id) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(TABLE_MASTER_DATA, null, COLUMN_MASTER_ID + "=?",
                new String[]{String.valueOf(id)}, null, null, null);

        MasterData data = null;
        if (cursor != null && cursor.moveToFirst()) {
            data = new MasterData();
            data.setId(cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_MASTER_ID)));
            data.setType(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_MASTER_TYPE)));
            data.setValue(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_MASTER_VALUE)));
            data.setTimestamp(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_MASTER_TIMESTAMP)));
            cursor.close();
        }

        db.close();
        return data;
    }

    public void deleteMasterData(int id) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_MASTER_DATA, COLUMN_MASTER_ID + "=?", new String[]{String.valueOf(id)});
        db.close();
    }

    public boolean isMasterDataExists(String type, String value) {
        SQLiteDatabase db = this.getReadableDatabase();
        boolean exists = isMasterDataExists(db, type, value);
        db.close();
        return exists;
    }

    // ==================== DROPDOWN METHODS (Using Master Data) ====================

    public List<String> getUniqueVehicleTypes() {
        return getMasterDataValuesByType(TYPE_VEHICLE_TYPE);
    }

    public List<String> getUniqueMaterials() {
        return getMasterDataValuesByType(TYPE_MATERIAL);
    }

    public List<String> getUniqueParties() {
        return getMasterDataValuesByType(TYPE_PARTY);
    }

    public List<String> getUniqueVehicleNumbers() {
        return getMasterDataValuesByType(TYPE_VEHICLE_NO);
    }

    // ==================== SEARCH METHODS ====================

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