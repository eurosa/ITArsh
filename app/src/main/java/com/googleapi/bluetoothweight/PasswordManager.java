package com.googleapi.bluetoothweight;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.Settings;
import android.text.InputType;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;

public class PasswordManager {

    private static final String PREF_NAME = "PasswordManager";
    private static final String SECURITY_PREF_NAME = "SecurityPrefs";
    private static final int PERMISSION_REQUEST_CODE = 1001;

    // User credentials
    private static final String KEY_USERNAME = "username";
    private static final String KEY_USER_PASSWORD_HASH = "user_password_hash";
    private static final String KEY_USER_PASSWORD_SALT = "user_password_salt";
    private static final String KEY_USER_APPROVED = "user_approved";

    // Pending user registration
    private static final String KEY_PENDING_USERNAME = "pending_username";
    private static final String KEY_PENDING_PASSWORD_HASH = "pending_password_hash";
    private static final String KEY_PENDING_PASSWORD_SALT = "pending_password_salt";
    private static final String KEY_PENDING_TIMESTAMP = "pending_timestamp";

    // Admin credentials
    private static final String FIXED_ADMIN_USERNAME = "admin";
    private static final String KEY_ADMIN_PASSWORD_HASH = "admin_password_hash";
    private static final String KEY_ADMIN_PASSWORD_SALT = "admin_password_salt";

    // Security questions
    private static final String KEY_SECURITY_QUESTION_1 = "security_question_1";
    private static final String KEY_SECURITY_QUESTION_2 = "security_question_2";
    private static final String KEY_SECURITY_ANSWER_1 = "security_answer_1";
    private static final String KEY_SECURITY_ANSWER_2 = "security_answer_2";

    // Backup email
    private static final String KEY_BACKUP_EMAIL = "backup_email";
    private static final String KEY_RECOVERY_CODE = "recovery_code";
    private static final String KEY_RECOVERY_CODE_TIMESTAMP = "recovery_code_timestamp";

    // File reset
    private static final String KEY_RESET_FILE_CODE = "reset_file_code";
    private static final String DEFAULT_RESET_CODE = "RESET123";

    private static final long PENDING_EXPIRY = 5 * 60 * 1000; // 5 minutes
    private static final long SESSION_EXPIRY = 8 * 60 * 60 * 1000; // 8 hours
    private static final long RECOVERY_CODE_EXPIRY = 30 * 60 * 1000; // 30 minutes

    private final AppCompatActivity activity;
    private final SharedPreferences prefs;
    private final SharedPreferences securityPrefs;
    private AuthListener listener;

    public interface AuthListener {
        void onUserLoginSuccess(String username);
        void onUserLoginFailed();
        void onUserRegistered(String username);
        void onUserApproved(String username);
        void onUserRejected(String username);
        void onSessionExpired();
        void onPasswordResetSuccess();
        void onPasswordResetFailed(String reason);
    }

    public PasswordManager(AppCompatActivity activity) {
        this.activity = activity;
        this.prefs = activity.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        this.securityPrefs = activity.getSharedPreferences(SECURITY_PREF_NAME, Context.MODE_PRIVATE);

        // Initialize default admin password if not set
        if (!prefs.contains(KEY_ADMIN_PASSWORD_HASH)) {
            setupDefaultAdminPassword();
        }
    }

    public void setAuthListener(AuthListener listener) {
        this.listener = listener;
    }

    /**
     * Setup default admin password
     */
    private void setupDefaultAdminPassword() {
        String defaultPassword = "admin@123";
        String salt = generateSalt();
        String hash = hashPassword(defaultPassword, salt);
        prefs.edit()
                .putString(KEY_ADMIN_PASSWORD_HASH, hash)
                .putString(KEY_ADMIN_PASSWORD_SALT, salt)
                .apply();
    }

    /**
     * Check if user is logged in with valid session
     */
    public boolean isUserLoggedIn() {
        long loginTime = prefs.getLong("login_time", 0);
        String username = prefs.getString("logged_in_user", null);

        if (username == null || loginTime == 0) {
            return false;
        }

        if (System.currentTimeMillis() - loginTime > SESSION_EXPIRY) {
            clearSession();
            if (listener != null) {
                listener.onSessionExpired();
            }
            return false;
        }

        return true;
    }

    /**
     * Get currently logged in username
     */
    public String getCurrentUser() {
        return prefs.getString("logged_in_user", null);
    }

    /**
     * Clear user session
     */
    public void clearSession() {
        prefs.edit()
                .remove("logged_in_user")
                .remove("login_time")
                .apply();
    }

    /**
     * Show admin password reset options dialog with ALL reset methods
     */
    public void showAdminPasswordResetOptions() {
        // Create a custom layout instead of using setItems
        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        builder.setTitle("🔐 Admin Password Reset");

        // Create main layout
        LinearLayout mainLayout = new LinearLayout(activity);
        mainLayout.setOrientation(LinearLayout.VERTICAL);
        mainLayout.setPadding(50, 20, 50, 20);

        // Add message
        TextView messageText = new TextView(activity);
        messageText.setText("Choose recovery method:");
        messageText.setTextSize(16);
        messageText.setTextColor(Color.BLACK);
        messageText.setPadding(0, 0, 0, 20);
        mainLayout.addView(messageText);

        // Option buttons with colors
        String[] options = {

                "📁 File-Based Reset"

                /*
                "📝 Reset via Security Questions",
                "📧 Reset via Recovery Email",
                "📁 File-Based Reset",
                "🔢 Reset via Security Code (9999)",
                "⚠️ Emergency Reset to Default"*/
        };

        int[] colors = {
                Color.parseColor("#2196F3"), // Blue
                Color.parseColor("#4CAF50"), // Green
                Color.parseColor("#FF9800"), // Orange
                Color.parseColor("#9C27B0"), // Purple
                Color.parseColor("#F44336")  // Red
        };

        for (int i = 0; i < options.length; i++) {
            final int index = i;
            Button optionButton = new Button(activity);
            optionButton.setText(options[i]);
            optionButton.setBackgroundColor(colors[i]);
            optionButton.setTextColor(Color.WHITE);
            optionButton.setPadding(30, 20, 30, 20);
            optionButton.setAllCaps(false);
            optionButton.setTextSize(16);

            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT);
            params.setMargins(0, 0, 0, 15);
            optionButton.setLayoutParams(params);

            optionButton.setOnClickListener(v -> {
                switch (index) {
                    case 0:
                        showFileBasedReset();
                    /*case 0:
                        showSecurityQuestionsReset();
                        break;
                    case 1:
                        showEmailRecoveryDialog();
                        break;
                    case 2:
                        showFileBasedReset();
                        break;
                    case 3:
                        showSecurityCodeReset();
                        break;
                    case 4:
                        showEmergencyResetDialog();
                        break;*/
                }
            });
            mainLayout.addView(optionButton);
        }

        // Cancel button
        Button cancelButton = new Button(activity);
        cancelButton.setText("Cancel");
        cancelButton.setBackgroundColor(Color.parseColor("#9E9E9E"));
        cancelButton.setTextColor(Color.WHITE);
        cancelButton.setPadding(30, 20, 30, 20);
        cancelButton.setAllCaps(false);
        cancelButton.setTextSize(16);

        LinearLayout.LayoutParams cancelParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        cancelParams.setMargins(0, 10, 0, 0);
        cancelButton.setLayoutParams(cancelParams);

        mainLayout.addView(cancelButton);

        // Make layout scrollable for small screens
        android.widget.ScrollView scrollView = new android.widget.ScrollView(activity);
        scrollView.addView(mainLayout);

        builder.setView(scrollView);

        AlertDialog dialog = builder.create();
        dialog.show();

        // Set cancel button to dismiss dialog
        cancelButton.setOnClickListener(v -> dialog.dismiss());
    }

    /**
     * ==================== FILE-BASED RESET METHODS ====================
     */

    /**
     * Show file-based reset options
     */
    private void showFileBasedReset() {
        String[] options = {
                "🔍 Check for Reset File",
                "🔧 Check Permissions"
                /*
                "🔍 Check for Reset File",
                "📝 Instructions to Create Reset File",
                "📋 Show Reset Code",
                "⚙️ Setup/Change Reset Code",
                "➕ Create Reset File Now",
                "🔧 Check Permissions"*/
        };

        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        builder.setTitle("📁 File-Based Password Reset");
        builder.setItems(options, (dialog, which) -> {
            switch (which) {
                case 0:
                    checkAndProcessResetFile();
                    break;
                case 1:
                    checkStoragePermission();
                    break;

                /*case 0:
                    checkAndProcessResetFile();
                    break;
                case 1:
                    showResetFileInstructions();
                    break;
                case 2:
                    showResetCodeDialog();
                    break;
                case 3:
                    showFileResetSetupDialog();
                    break;
                case 4:
                    showCreateFileOptions();
                    break;
                case 5:
                    checkStoragePermission();
                    break;*/
            }
        });
        builder.setNegativeButton("Back", (dialog, which) -> showAdminPasswordResetOptions());
        builder.show();
    }

    /**
     * Check storage permission and request if needed
     */
    private void checkStoragePermission() {
        if (hasStoragePermission()) {
            Toast.makeText(activity, "✅ Storage permission already granted", Toast.LENGTH_SHORT).show();
            showFileBasedReset();
        } else {
            requestStoragePermission();
        }
    }

    /**
     * Check if we have storage permissions
     */
    private boolean hasStoragePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // Android 11+ - check if we have MANAGE_EXTERNAL_STORAGE
            return Environment.isExternalStorageManager();
        } else {
            // Android 6-10
            int result = ContextCompat.checkSelfPermission(activity,
                    android.Manifest.permission.READ_EXTERNAL_STORAGE);
            return result == PackageManager.PERMISSION_GRANTED;
        }
    }

    /**
     * Request storage permission based on Android version
     */
    private void requestStoragePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // Android 11+ - guide user to settings
            new AlertDialog.Builder(activity)
                    .setTitle("📁 Storage Permission Required")
                    .setMessage("To read reset files, please grant 'Files and Media' permission.\n\n" +
                            "1. Tap 'Allow' in the next screen\n" +
                            "2. Enable 'Allow management of all files'")
                    .setPositiveButton("Go to Settings", (dialog, which) -> {
                        Intent intent = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
                        intent.setData(Uri.parse("package:" + activity.getPackageName()));
                        activity.startActivity(intent);
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
        } else {
            // Android 6-10
            ActivityCompat.requestPermissions(activity,
                    new String[]{
                            android.Manifest.permission.READ_EXTERNAL_STORAGE,
                            android.Manifest.permission.WRITE_EXTERNAL_STORAGE
                    },
                    PERMISSION_REQUEST_CODE);
        }
    }

    /**
     * Check for reset file and process if found
     */
    private void checkAndProcessResetFile() {
        if (!hasStoragePermission()) {
            Dialog permissionDialog = new Dialog(activity);
            permissionDialog.setTitle("📁 Permission Required");

            LinearLayout layout = new LinearLayout(activity);
            layout.setOrientation(LinearLayout.VERTICAL);
            layout.setPadding(50, 30, 50, 30);

            TextView message = new TextView(activity);
            message.setText("Storage permission is needed to read reset files.");
            message.setTextSize(16);
            message.setPadding(0, 0, 0, 20);
            layout.addView(message);

            LinearLayout buttonLayout = new LinearLayout(activity);
            buttonLayout.setOrientation(LinearLayout.HORIZONTAL);
            buttonLayout.setWeightSum(2);

            Button positiveButton = new Button(activity);
            positiveButton.setText("Grant Permission");
            LinearLayout.LayoutParams positiveParams = new LinearLayout.LayoutParams(
                    0, LinearLayout.LayoutParams.WRAP_CONTENT, 1);
            positiveParams.setMargins(0, 0, 10, 0);
            positiveButton.setLayoutParams(positiveParams);
            positiveButton.setBackgroundColor(Color.parseColor("#4CAF50"));
            positiveButton.setTextColor(Color.WHITE);

            Button negativeButton = new Button(activity);
            negativeButton.setText("Cancel");
            LinearLayout.LayoutParams negativeParams = new LinearLayout.LayoutParams(
                    0, LinearLayout.LayoutParams.WRAP_CONTENT, 1);
            negativeParams.setMargins(10, 0, 0, 0);
            negativeButton.setLayoutParams(negativeParams);
            negativeButton.setBackgroundColor(Color.parseColor("#F44336"));
            negativeButton.setTextColor(Color.WHITE);

            buttonLayout.addView(positiveButton);  // Grant Permission on LEFT
            buttonLayout.addView(negativeButton);  // Cancel on RIGHT
            layout.addView(buttonLayout);

            permissionDialog.setContentView(layout);

            positiveButton.setOnClickListener(v -> {
                permissionDialog.dismiss();
                requestStoragePermission();
            });

            negativeButton.setOnClickListener(v -> permissionDialog.dismiss());

            permissionDialog.show();
            return;
        }

        Toast.makeText(activity, "🔍 Checking for reset file...", Toast.LENGTH_SHORT).show();

        // Check multiple possible locations
        FileResetResult result = findResetCodeInFiles();

        if (result.found) {
            // Found a valid reset code
            if (result.code.equals(getExpectedResetCode())) {
                // Valid code - proceed with reset
                Dialog resetDialog = new Dialog(activity);
                resetDialog.setTitle("✅ Reset File Found");

                LinearLayout layout = new LinearLayout(activity);
                layout.setOrientation(LinearLayout.VERTICAL);
                layout.setPadding(50, 30, 50, 30);

                TextView message = new TextView(activity);
                message.setText("Valid reset file detected at:\n" + result.path +
                        "\n\nDo you want to reset your password?");
                message.setTextSize(16);
                message.setPadding(0, 0, 0, 20);
                layout.addView(message);

                LinearLayout buttonLayout = new LinearLayout(activity);
                buttonLayout.setOrientation(LinearLayout.HORIZONTAL);
                buttonLayout.setWeightSum(2);

                Button positiveButton = new Button(activity);
                positiveButton.setText("Yes, Reset Password");
                LinearLayout.LayoutParams positiveParams = new LinearLayout.LayoutParams(
                        0, LinearLayout.LayoutParams.WRAP_CONTENT, 1);
                positiveParams.setMargins(0, 0, 10, 0);
                positiveButton.setLayoutParams(positiveParams);
                positiveButton.setBackgroundColor(Color.parseColor("#4CAF50"));
                positiveButton.setTextColor(Color.WHITE);

                Button negativeButton = new Button(activity);
                negativeButton.setText("Cancel");
                LinearLayout.LayoutParams negativeParams = new LinearLayout.LayoutParams(
                        0, LinearLayout.LayoutParams.WRAP_CONTENT, 1);
                negativeParams.setMargins(10, 0, 0, 0);
                negativeButton.setLayoutParams(negativeParams);
                negativeButton.setBackgroundColor(Color.parseColor("#F44336"));
                negativeButton.setTextColor(Color.WHITE);

                buttonLayout.addView(positiveButton);  // Yes, Reset Password on LEFT
                buttonLayout.addView(negativeButton);  // Cancel on RIGHT
                layout.addView(buttonLayout);

                resetDialog.setContentView(layout);

                positiveButton.setOnClickListener(v -> {
                    resetDialog.dismiss();
                    // Clean up - delete the reset file
                    deleteResetFiles();
                    // Show new password dialog
                    showNewPasswordDialog("file");
                });

                negativeButton.setOnClickListener(v -> resetDialog.dismiss());

                resetDialog.show();
            } else {
                Dialog invalidDialog = new Dialog(activity);
                invalidDialog.setTitle("❌ Invalid Reset Code");

                LinearLayout layout = new LinearLayout(activity);
                layout.setOrientation(LinearLayout.VERTICAL);
                layout.setPadding(50, 30, 50, 30);

                TextView message = new TextView(activity);
                message.setText("Found file at:\n" + result.path +
                        "\n\nExpected: " + getExpectedResetCode() +
                        "\nFound: " + result.code);
                message.setTextSize(16);
                message.setPadding(0, 0, 0, 20);
                layout.addView(message);

                LinearLayout buttonLayout = new LinearLayout(activity);
                buttonLayout.setOrientation(LinearLayout.HORIZONTAL);

                Button okButton = new Button(activity);
                okButton.setText("OK");
                okButton.setLayoutParams(new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT));
                okButton.setBackgroundColor(Color.parseColor("#2196F3"));
                okButton.setTextColor(Color.WHITE);

                buttonLayout.addView(okButton);
                layout.addView(buttonLayout);

                invalidDialog.setContentView(layout);

                okButton.setOnClickListener(v -> invalidDialog.dismiss());

                invalidDialog.show();
            }
        } else {
            // No reset file found - show debug info
            StringBuilder debug = new StringBuilder();
            debug.append("No reset file found in these locations:\n\n");

            for (String location : getLocations()) {
                if (location != null) {
                    debug.append("📍 ").append(location).append("\n");
                    File dir = new File(location);
                    if (!dir.exists()) {
                        debug.append("   ⚠️ Directory doesn't exist\n");
                    } else if (!dir.canRead()) {
                        debug.append("   ⚠️ Cannot read directory\n");
                    }
                }
            }

            Dialog debugDialog = new Dialog(activity);
            debugDialog.setTitle("❌ No Reset File Found");

            LinearLayout layout = new LinearLayout(activity);
            layout.setOrientation(LinearLayout.VERTICAL);
            layout.setPadding(50, 30, 50, 30);

            TextView message = new TextView(activity);
            message.setText(debug.toString());
            message.setTextSize(14);
            message.setPadding(0, 0, 0, 20);
            layout.addView(message);

            LinearLayout buttonLayout = new LinearLayout(activity);
            buttonLayout.setOrientation(LinearLayout.HORIZONTAL);

            Button neutralButton = new Button(activity);
            neutralButton.setText("Check Permissions");
            neutralButton.setLayoutParams(new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT));
            neutralButton.setBackgroundColor(Color.parseColor("#FF9800"));
            neutralButton.setTextColor(Color.WHITE);

            buttonLayout.addView(neutralButton);
            layout.addView(buttonLayout);

            debugDialog.setContentView(layout);

            neutralButton.setOnClickListener(v -> {
                debugDialog.dismiss();
                checkStoragePermission();
            });

            debugDialog.show();
        }
    }

    /**
     * Result class for file search
     */
    private class FileResetResult {
        boolean found;
        String code;
        String path;

        FileResetResult() {
            this.found = false;
            this.code = null;
            this.path = null;
        }
    }

    /**
     * Get all possible locations for reset files
     */
    private String[] getLocations() {
        return new String[]{
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).getAbsolutePath() + "/",
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS).getAbsolutePath() + "/",
                Environment.getExternalStorageDirectory().getAbsolutePath() + "/",
                activity.getExternalFilesDir(null) != null ? activity.getExternalFilesDir(null).getAbsolutePath() + "/" : null,
                activity.getFilesDir().getAbsolutePath() + "/"
        };
    }

    /**
     * Get all possible filenames for reset files
     */
    private String[] getFilenames() {
        return new String[]{
                "reset.txt",
                "password_reset.txt",
                "reset_code.txt",
                "admin_reset.txt",
                ".reset"
        };
    }

    /**
     * Find reset code in various file locations
     */
    private FileResetResult findResetCodeInFiles() {
        FileResetResult result = new FileResetResult();

        for (String location : getLocations()) {
            if (location == null) continue;

            File dir = new File(location);
            if (!dir.exists() || !dir.canRead()) continue;

            for (String fileName : getFilenames()) {
                try {
                    File file = new File(dir, fileName);
                    if (file.exists() && file.canRead()) {
                        String code = readFileContent(file);
                        if (code != null && !code.isEmpty()) {
                            result.found = true;
                            result.code = code.trim();
                            result.path = file.getAbsolutePath();
                            return result;
                        }
                    }
                } catch (Exception e) {
                    Log.e("FileReset", "Error checking file: " + e.getMessage());
                }
            }
        }

        return result;
    }

    /**
     * Read content from a file with proper encoding
     */
    private String readFileContent(File file) {
        StringBuilder content = new StringBuilder();
        try {
            FileInputStream fis = new FileInputStream(file);
            BufferedReader reader = new BufferedReader(new InputStreamReader(fis, "UTF-8"));
            String line;
            while ((line = reader.readLine()) != null) {
                content.append(line);
            }
            reader.close();
            fis.close();

            // Remove any BOM or special characters
            return content.toString().trim().replace("\uFEFF", "");
        } catch (Exception e) {
            Log.e("FileReset", "Error reading file: " + e.getMessage());
            return null;
        }
    }

    /**
     * Get the expected reset code
     */
    private String getExpectedResetCode() {
        return securityPrefs.getString(KEY_RESET_FILE_CODE, DEFAULT_RESET_CODE);
    }

    /**
     * Delete all reset files after use
     */
    private void deleteResetFiles() {
        for (String location : getLocations()) {
            if (location == null) continue;

            for (String fileName : getFilenames()) {
                try {
                    File file = new File(location, fileName);
                    if (file.exists()) {
                        boolean deleted = file.delete();
                        Log.d("FileReset", "Deleted " + fileName + ": " + deleted);
                    }
                } catch (Exception e) {
                    Log.e("FileReset", "Error deleting file: " + e.getMessage());
                }
            }
        }
    }

    /**
     * Show instructions for creating reset file
     */
    private void showResetFileInstructions() {
        String expectedCode = getExpectedResetCode();

        String instructions =
                "📋 INSTRUCTIONS:\n\n" +
                        "1. Create a text file with one of these names:\n" +
                        "   • reset.txt\n" +
                        "   • password_reset.txt\n" +
                        "   • reset_code.txt\n" +
                        "   • admin_reset.txt\n" +
                        "   • .reset (hidden file)\n\n" +
                        "2. Open the file and type exactly: " + expectedCode + "\n\n" +
                        "3. Save the file in any of these locations:\n" +
                        "   • Internal Storage/Download/\n" +
                        "   • Internal Storage/Documents/\n" +
                        "   • Internal Storage/\n\n" +
                        "4. Then press 'Check for Reset File' again\n\n" +
                        "⚠️ The file will be automatically deleted after successful reset";

        new AlertDialog.Builder(activity)
                .setTitle("📝 How to Create Reset File")
                .setMessage(instructions)
                .setPositiveButton("OK", null)
                .setNegativeButton("Create File Now", (dialog, which) -> showCreateFileOptions())
                .show();
    }

    /**
     * Show the reset code in a dialog
     */
    private void showResetCodeDialog() {
        String resetCode = getExpectedResetCode();

        LinearLayout layout = new LinearLayout(activity);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(50, 20, 50, 20);

        TextView codeDisplay = new TextView(activity);
        codeDisplay.setText(resetCode);
        codeDisplay.setTextSize(32);
        codeDisplay.setTextColor(Color.parseColor("#4CAF50"));
        codeDisplay.setTypeface(null, android.graphics.Typeface.BOLD);
        codeDisplay.setPadding(0, 20, 0, 20);
        codeDisplay.setGravity(android.view.Gravity.CENTER);
        layout.addView(codeDisplay);

        new AlertDialog.Builder(activity)
                .setTitle("🔑 Your Reset Code")
                .setView(layout)
                .setPositiveButton("OK", null)
                .setNegativeButton("Copy", (dialog, which) -> {
                    android.content.ClipboardManager clipboard = (android.content.ClipboardManager)
                            activity.getSystemService(Context.CLIPBOARD_SERVICE);
                    android.content.ClipData clip = android.content.ClipData.newPlainText("Reset Code", resetCode);
                    clipboard.setPrimaryClip(clip);
                    Toast.makeText(activity, "Code copied to clipboard", Toast.LENGTH_SHORT).show();
                })
                .show();
    }

    /**
     * Setup the reset code (call during first time setup)
     */
    public void showFileResetSetupDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        builder.setTitle("📁 File Reset Setup");
        builder.setMessage("Create a custom reset code for file-based password recovery");

        LinearLayout layout = new LinearLayout(activity);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(50, 20, 50, 20);

        TextView infoText = new TextView(activity);
        infoText.setText("Choose a code (6-10 characters):");
        infoText.setTextSize(14);
        infoText.setPadding(0, 0, 0, 10);
        layout.addView(infoText);

        EditText codeInput = new EditText(activity);
        codeInput.setHint("Enter reset code");
        codeInput.setInputType(InputType.TYPE_CLASS_TEXT);
        codeInput.setId(View.generateViewId());
        layout.addView(codeInput);

        EditText confirmInput = new EditText(activity);
        confirmInput.setHint("Confirm reset code");
        confirmInput.setInputType(InputType.TYPE_CLASS_TEXT);
        confirmInput.setId(View.generateViewId());
        layout.addView(confirmInput);

        builder.setView(layout);

        builder.setPositiveButton("Save", (dialog, which) -> {
            String code = codeInput.getText().toString().trim();
            String confirm = confirmInput.getText().toString().trim();

            if (code.isEmpty()) {
                Toast.makeText(activity, "Please enter a code", Toast.LENGTH_SHORT).show();
                return;
            }

            if (code.length() < 4) {
                Toast.makeText(activity, "Code must be at least 4 characters", Toast.LENGTH_SHORT).show();
                return;
            }

            if (!code.equals(confirm)) {
                Toast.makeText(activity, "Codes do not match", Toast.LENGTH_SHORT).show();
                return;
            }

            // Save the reset code
            securityPrefs.edit().putString(KEY_RESET_FILE_CODE, code).apply();

            Toast.makeText(activity, "✅ Reset code saved: " + code, Toast.LENGTH_LONG).show();
        });

        builder.setNegativeButton("Use Default", (dialog, which) -> {
            securityPrefs.edit().putString(KEY_RESET_FILE_CODE, DEFAULT_RESET_CODE).apply();
            Toast.makeText(activity, "✅ Default code set: " + DEFAULT_RESET_CODE, Toast.LENGTH_SHORT).show();
        });

        builder.setNeutralButton("Cancel", null);
        builder.show();
    }

    /**
     * Show options to create reset file
     */
    private void showCreateFileOptions() {
        if (!hasStoragePermission()) {
            requestStoragePermission();
            return;
        }

        String[] options = {
                "📁 Create in Download Folder",
                "📁 Create in Documents Folder",
                "📁 Create in App Storage",
                "📁 Create in All Locations"
        };

        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        builder.setTitle("Create Reset File");
        builder.setItems(options, (dialog, which) -> {
            String resetCode = getExpectedResetCode();
            int successCount = 0;

            switch (which) {
                case 0:
                    successCount = createFileInLocation(
                            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).getAbsolutePath(),
                            resetCode) ? 1 : 0;
                    break;
                case 1:
                    successCount = createFileInLocation(
                            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS).getAbsolutePath(),
                            resetCode) ? 1 : 0;
                    break;
                case 2:
                    if (activity.getExternalFilesDir(null) != null) {
                        successCount = createFileInLocation(
                                activity.getExternalFilesDir(null).getAbsolutePath(),
                                resetCode) ? 1 : 0;
                    }
                    break;
                case 3:
                    successCount = 0;
                    if (createFileInLocation(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).getAbsolutePath(), resetCode)) successCount++;
                    if (createFileInLocation(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS).getAbsolutePath(), resetCode)) successCount++;
                    if (activity.getExternalFilesDir(null) != null) {
                        if (createFileInLocation(activity.getExternalFilesDir(null).getAbsolutePath(), resetCode)) successCount++;
                    }
                    break;
            }

            if (successCount > 0) {
                Toast.makeText(activity,
                        "✅ Created " + successCount + " reset file(s) with code: " + resetCode,
                        Toast.LENGTH_LONG).show();
            } else {
                Toast.makeText(activity, "❌ Failed to create reset files", Toast.LENGTH_LONG).show();
            }
        });
        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    /**
     * Create reset file in specific location
     */
    private boolean createFileInLocation(String location, String resetCode) {
        try {
            File dir = new File(location);
            if (!dir.exists()) {
                dir.mkdirs();
            }

            boolean success = false;
            for (String filename : getFilenames()) {
                File file = new File(dir, filename);
                FileOutputStream fos = new FileOutputStream(file);
                OutputStreamWriter writer = new OutputStreamWriter(fos, "UTF-8");
                writer.write(resetCode);
                writer.close();
                fos.close();
                Log.d("FileReset", "Created: " + file.getAbsolutePath());
                success = true;
            }
            return success;
        } catch (Exception e) {
            Log.e("FileReset", "Error creating files in " + location + ": " + e.getMessage());
            return false;
        }
    }

    /**
     * ==================== SECURITY QUESTIONS RESET ====================
     */

    private void showSecurityQuestionsReset() {
        if (!areSecurityQuestionsSet()) {
            Toast.makeText(activity, "Security questions not set up. Please use another method.", Toast.LENGTH_LONG).show();
            return;
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        builder.setTitle("Security Questions");
        builder.setMessage("Please answer your security questions");

        LinearLayout layout = new LinearLayout(activity);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(50, 20, 50, 20);

        // Question 1
        TextView q1Label = new TextView(activity);
        q1Label.setText(securityPrefs.getString(KEY_SECURITY_QUESTION_1, "What is your mother's maiden name?"));
        q1Label.setTextSize(14);
        q1Label.setPadding(0, 10, 0, 5);
        layout.addView(q1Label);

        EditText q1Input = new EditText(activity);
        q1Input.setHint("Your answer");
        q1Input.setInputType(InputType.TYPE_CLASS_TEXT);
        q1Input.setId(View.generateViewId());
        layout.addView(q1Input);

        // Question 2
        TextView q2Label = new TextView(activity);
        q2Label.setText(securityPrefs.getString(KEY_SECURITY_QUESTION_2, "What was your first pet's name?"));
        q2Label.setTextSize(14);
        q2Label.setPadding(0, 20, 0, 5);
        layout.addView(q2Label);

        EditText q2Input = new EditText(activity);
        q2Input.setHint("Your answer");
        q2Input.setInputType(InputType.TYPE_CLASS_TEXT);
        q2Input.setId(View.generateViewId());
        layout.addView(q2Input);

        builder.setView(layout);
        builder.setPositiveButton("Verify", null);
        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss());

        AlertDialog dialog = builder.create();
        dialog.show();

        Button verifyButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE);

        verifyButton.setOnClickListener(v -> {
            String answer1 = q1Input.getText().toString().trim().toLowerCase();
            String answer2 = q2Input.getText().toString().trim().toLowerCase();

            String savedAnswer1 = securityPrefs.getString(KEY_SECURITY_ANSWER_1, "").toLowerCase();
            String savedAnswer2 = securityPrefs.getString(KEY_SECURITY_ANSWER_2, "").toLowerCase();

            if (answer1.equals(savedAnswer1) && answer2.equals(savedAnswer2)) {
                dialog.dismiss();
                showNewPasswordDialog("security");
            } else {
                Toast.makeText(activity, "Incorrect answers", Toast.LENGTH_SHORT).show();
            }
        });
    }

    /**
     * ==================== EMAIL RECOVERY ====================
     */

    private void showEmailRecoveryDialog() {
        String email = securityPrefs.getString(KEY_BACKUP_EMAIL, "");

        if (email.isEmpty()) {
            Toast.makeText(activity, "No recovery email configured", Toast.LENGTH_LONG).show();
            return;
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        builder.setTitle("Email Recovery");
        builder.setMessage("A recovery code will be sent to " + email);

        builder.setPositiveButton("Send Code", (dialog, which) -> {
            String recoveryCode = generateRecoveryCode();
            saveRecoveryCode(recoveryCode);
            showRecoveryCodeDialog(recoveryCode);
        });

        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    private void showRecoveryCodeDialog(String code) {
        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        builder.setTitle("Recovery Code");
        builder.setMessage("Your recovery code is: " + code + "\n\nThis code will expire in 30 minutes.");

        LinearLayout layout = new LinearLayout(activity);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(50, 20, 50, 20);

        EditText codeInput = new EditText(activity);
        codeInput.setHint("Enter recovery code");
        codeInput.setInputType(InputType.TYPE_CLASS_NUMBER);
        layout.addView(codeInput);

        builder.setView(layout);
        builder.setPositiveButton("Verify", (dialog, which) -> {
            String enteredCode = codeInput.getText().toString().trim();
            if (verifyRecoveryCode(enteredCode)) {
                dialog.dismiss();
                showNewPasswordDialog("email");
            } else {
                Toast.makeText(activity, "Invalid or expired code", Toast.LENGTH_SHORT).show();
            }
        });

        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    /**
     * ==================== SECURITY CODE RESET ====================
     */

    private void showSecurityCodeReset() {
        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        builder.setTitle("Security Code Reset");
        builder.setMessage("Enter your security code to reset password\n\nDefault code: 9999");

        LinearLayout layout = new LinearLayout(activity);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(50, 20, 50, 20);

        EditText codeInput = new EditText(activity);
        codeInput.setHint("Enter security code");
        codeInput.setInputType(InputType.TYPE_CLASS_NUMBER);
        layout.addView(codeInput);

        builder.setView(layout);
        builder.setPositiveButton("Verify", (dialog, which) -> {
            String code = codeInput.getText().toString().trim();
            if (code.equals("9999")) {
                dialog.dismiss();
                showNewPasswordDialog("code");
            } else {
                Toast.makeText(activity, "Invalid security code", Toast.LENGTH_SHORT).show();
            }
        });

        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    /**
     * ==================== EMERGENCY RESET ====================
     */

    private void showEmergencyResetDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        builder.setTitle("⚠️ Emergency Reset");
        builder.setMessage("This will reset admin password to default.\nDefault password: admin@123\n\nAre you sure?");

        builder.setPositiveButton("Reset to Default", (dialog, which) -> {
            setupDefaultAdminPassword();
            Toast.makeText(activity, "Password reset to default: admin@123", Toast.LENGTH_LONG).show();
            if (listener != null) {
                listener.onPasswordResetSuccess();
            }
        });

        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    /**
     * ==================== NEW PASSWORD DIALOG ====================
     */

    private void showNewPasswordDialog(String method) {
        Dialog dialog = new Dialog(activity);
        dialog.setTitle("Set New Password");

        // Main layout
        LinearLayout mainLayout = new LinearLayout(activity);
        mainLayout.setOrientation(LinearLayout.VERTICAL);
        mainLayout.setPadding(50, 30, 50, 30);

        // Message text
        TextView messageText = new TextView(activity);
        messageText.setText("Enter your new admin password");
        messageText.setTextSize(16);
        messageText.setPadding(0, 0, 0, 20);
        mainLayout.addView(messageText);

        // New password field
        EditText newPassInput = new EditText(activity);
        newPassInput.setHint("New password (min 6 characters)");
        newPassInput.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        newPassInput.setPadding(0, 10, 0, 10);
        mainLayout.addView(newPassInput);

        // Confirm password field
        EditText confirmInput = new EditText(activity);
        confirmInput.setHint("Confirm password");
        confirmInput.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        confirmInput.setPadding(0, 10, 0, 10);
        mainLayout.addView(confirmInput);

        // Button layout (horizontal)
        LinearLayout buttonLayout = new LinearLayout(activity);
        buttonLayout.setOrientation(LinearLayout.HORIZONTAL);
        buttonLayout.setPadding(0, 20, 0, 0);
        buttonLayout.setWeightSum(2);

        // Reset Password button (LEFT)
        Button resetButton = new Button(activity);
        resetButton.setText("Reset Password");
        LinearLayout.LayoutParams resetParams = new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1);
        resetParams.setMargins(0, 0, 10, 0);
        resetButton.setLayoutParams(resetParams);
        resetButton.setBackgroundColor(Color.parseColor("#4CAF50"));
        resetButton.setTextColor(Color.WHITE);
        resetButton.setPadding(20, 12, 20, 12);

        // Cancel button (RIGHT)
        Button cancelButton = new Button(activity);
        cancelButton.setText("Cancel");
        LinearLayout.LayoutParams cancelParams = new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1);
        cancelParams.setMargins(10, 0, 0, 0);
        cancelButton.setLayoutParams(cancelParams);
        cancelButton.setBackgroundColor(Color.parseColor("#F44336"));
        cancelButton.setTextColor(Color.WHITE);
        cancelButton.setPadding(20, 12, 20, 12);

        buttonLayout.addView(resetButton);  // Reset Password on LEFT
        buttonLayout.addView(cancelButton); // Cancel on RIGHT

        mainLayout.addView(buttonLayout);

        dialog.setContentView(mainLayout);

        // Reset button click listener
        resetButton.setOnClickListener(v -> {
            String newPass = newPassInput.getText().toString();
            String confirm = confirmInput.getText().toString();

            if (newPass.isEmpty()) {
                Toast.makeText(activity, "Please enter password", Toast.LENGTH_SHORT).show();
                return;
            }

            if (!newPass.equals(confirm)) {
                Toast.makeText(activity, "Passwords do not match", Toast.LENGTH_SHORT).show();
                return;
            }

            if (newPass.length() < 6) {
                Toast.makeText(activity, "Password must be at least 6 characters", Toast.LENGTH_SHORT).show();
                return;
            }

            resetAdminPassword(newPass);
            Toast.makeText(activity, "✅ Password reset successful!", Toast.LENGTH_LONG).show();

            if (listener != null) {
                listener.onPasswordResetSuccess();
            }

            dialog.dismiss();
        });

        // Cancel button click listener
        cancelButton.setOnClickListener(v -> dialog.dismiss());

        // Enter key handler for confirm input
        confirmInput.setOnKeyListener((v, keyCode, event) -> {
            if (event.getAction() == KeyEvent.ACTION_DOWN &&
                    (keyCode == KeyEvent.KEYCODE_ENTER || keyCode == KeyEvent.KEYCODE_NUMPAD_ENTER)) {
                resetButton.performClick();
                return true;
            }
            return false;
        });

        dialog.show();
    }

    /**
     * ==================== SETUP DIALOGS ====================
     */

    public void showSetupSecurityDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        builder.setTitle("Security Setup");
        builder.setMessage("Set up security questions for password recovery");

        LinearLayout layout = new LinearLayout(activity);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(50, 20, 50, 20);

        // Question 1
        TextView q1Label = new TextView(activity);
        q1Label.setText("Security Question 1:");
        q1Label.setTextSize(16);
        q1Label.setPadding(0, 10, 0, 5);
        layout.addView(q1Label);

        EditText q1Custom = new EditText(activity);
        q1Custom.setHint("Enter your security question");
        q1Custom.setInputType(InputType.TYPE_CLASS_TEXT);
        layout.addView(q1Custom);

        EditText a1Input = new EditText(activity);
        a1Input.setHint("Your answer");
        a1Input.setInputType(InputType.TYPE_CLASS_TEXT);
        layout.addView(a1Input);

        // Question 2
        TextView q2Label = new TextView(activity);
        q2Label.setText("Security Question 2:");
        q2Label.setTextSize(16);
        q2Label.setPadding(0, 20, 0, 5);
        layout.addView(q2Label);

        EditText q2Custom = new EditText(activity);
        q2Custom.setHint("Enter your security question");
        q2Custom.setInputType(InputType.TYPE_CLASS_TEXT);
        layout.addView(q2Custom);

        EditText a2Input = new EditText(activity);
        a2Input.setHint("Your answer");
        a2Input.setInputType(InputType.TYPE_CLASS_TEXT);
        layout.addView(a2Input);

        // Backup email
        TextView emailLabel = new TextView(activity);
        emailLabel.setText("Backup Email (optional):");
        emailLabel.setTextSize(16);
        emailLabel.setPadding(0, 20, 0, 5);
        layout.addView(emailLabel);

        EditText emailInput = new EditText(activity);
        emailInput.setHint("Enter backup email");
        emailInput.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);
        layout.addView(emailInput);

        builder.setView(layout);
        builder.setPositiveButton("Save", (dialog, which) -> {
            String q1 = q1Custom.getText().toString().trim();
            String a1 = a1Input.getText().toString().trim();
            String q2 = q2Custom.getText().toString().trim();
            String a2 = a2Input.getText().toString().trim();
            String email = emailInput.getText().toString().trim();

            if (q1.isEmpty() || a1.isEmpty() || q2.isEmpty() || a2.isEmpty()) {
                Toast.makeText(activity, "Please fill all fields", Toast.LENGTH_SHORT).show();
                return;
            }

            securityPrefs.edit()
                    .putString(KEY_SECURITY_QUESTION_1, q1)
                    .putString(KEY_SECURITY_ANSWER_1, a1.toLowerCase())
                    .putString(KEY_SECURITY_QUESTION_2, q2)
                    .putString(KEY_SECURITY_ANSWER_2, a2.toLowerCase())
                    .putString(KEY_BACKUP_EMAIL, email)
                    .apply();

            Toast.makeText(activity, "✅ Security settings saved!", Toast.LENGTH_LONG).show();
        });

        builder.setNegativeButton("Skip", null);
        builder.show();
    }

    /**
     * ==================== UTILITY METHODS ====================
     */

    private boolean areSecurityQuestionsSet() {
        return securityPrefs.contains(KEY_SECURITY_QUESTION_1) &&
                securityPrefs.contains(KEY_SECURITY_ANSWER_1);
    }

    private String generateRecoveryCode() {
        SecureRandom random = new SecureRandom();
        int code = 100000 + random.nextInt(900000);
        return String.valueOf(code);
    }

    private void saveRecoveryCode(String code) {
        securityPrefs.edit()
                .putString(KEY_RECOVERY_CODE, code)
                .putLong(KEY_RECOVERY_CODE_TIMESTAMP, System.currentTimeMillis())
                .apply();
    }

    private boolean verifyRecoveryCode(String code) {
        String savedCode = securityPrefs.getString(KEY_RECOVERY_CODE, "");
        long timestamp = securityPrefs.getLong(KEY_RECOVERY_CODE_TIMESTAMP, 0);

        if (savedCode.isEmpty() || !savedCode.equals(code)) {
            return false;
        }

        if (System.currentTimeMillis() - timestamp > RECOVERY_CODE_EXPIRY) {
            securityPrefs.edit().remove(KEY_RECOVERY_CODE).apply();
            return false;
        }

        securityPrefs.edit().remove(KEY_RECOVERY_CODE).apply();
        return true;
    }

    public void resetAdminPassword(String newPassword) {
        String salt = generateSalt();
        String hash = hashPassword(newPassword, salt);

        prefs.edit()
                .putString(KEY_ADMIN_PASSWORD_HASH, hash)
                .putString(KEY_ADMIN_PASSWORD_SALT, salt)
                .apply();

        Log.d("PasswordManager", "Admin password reset successfully");
    }

    public void showRegistrationDialog() {
        // Your existing registration dialog code
    }

    public boolean isFirstTimeAdmin() {
        String adminHash = prefs.getString(KEY_ADMIN_PASSWORD_HASH, null);
        return adminHash == null;
    }

    public void markAdminConfigured() {
        prefs.edit().putBoolean("admin_configured", true).apply();
    }

    private boolean validateUserLogin(String username, String password) {
        String approvedUser = prefs.getString(KEY_USERNAME, null);
        String savedHash = prefs.getString(KEY_USER_PASSWORD_HASH, null);
        String salt = prefs.getString(KEY_USER_PASSWORD_SALT, null);
        boolean isApproved = prefs.getBoolean(KEY_USER_APPROVED, false);

        if (approvedUser == null || !approvedUser.equals(username) || !isApproved) {
            return false;
        }

        if (savedHash == null || salt == null) {
            return false;
        }

        String hash = hashPassword(password, salt);
        return hash.equals(savedHash);
    }

    public boolean validateAdminLogin(String username, String password) {
        if (!username.equals(FIXED_ADMIN_USERNAME)) {
            return false;
        }

        String savedHash = prefs.getString(KEY_ADMIN_PASSWORD_HASH, null);
        String salt = prefs.getString(KEY_ADMIN_PASSWORD_SALT, null);

        if (savedHash == null || salt == null) {
            return false;
        }

        String hash = hashPassword(password, salt);
        return hash.equals(savedHash);
    }

    String hashPassword(String password, String salt) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            md.update(salt.getBytes());
            byte[] hashedPassword = md.digest(password.getBytes());

            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                return Base64.getEncoder().encodeToString(hashedPassword);
            } else {
                StringBuilder sb = new StringBuilder();
                for (byte b : hashedPassword) {
                    sb.append(String.format("%02x", b));
                }
                return sb.toString();
            }
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            return password;
        }
    }

    private String generateSalt() {
        SecureRandom random = new SecureRandom();
        byte[] salt = new byte[16];
        random.nextBytes(salt);

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            return Base64.getEncoder().encodeToString(salt);
        } else {
            StringBuilder sb = new StringBuilder();
            for (byte b : salt) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        }
    }

    private void styleButton(Button button, String text, String colorHex) {
        button.setText(text);
        button.setBackgroundColor(Color.parseColor(colorHex));
        button.setTextColor(Color.WHITE);
        button.setPadding(30, 15, 30, 15);
        button.setAllCaps(false);
        button.setTextSize(14);
    }
}