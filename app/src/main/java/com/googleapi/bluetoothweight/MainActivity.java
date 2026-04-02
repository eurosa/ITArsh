package com.googleapi.bluetoothweight;

import static com.googleapi.bluetoothweight.bluetooth.BluetoothConnectionManager.checkBluetoothPermissions;
import static com.googleapi.bluetoothweight.bluetooth.BluetoothConnectionManager.requestBluetoothPermissions;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.ActivityOptions;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.InputType;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.drawable.DrawableCompat;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;

import com.google.android.material.navigation.NavigationView;
import com.googleapi.bluetoothweight.bluetooth.BluetoothConnectionManager;

import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class MainActivity extends BaseActivity implements NavigationView.OnNavigationItemSelectedListener, BluetoothConnectionManager.ConnectionCallback {
    private Button buttonT, buttonG;
    private boolean isAdminMode = false;
    private UsbPermissionReceiver usbPermissionReceiver;
    private PasswordManager passwordManager;
    private boolean isUserLoggedIn = false;
    private String currentUser = null;
    private boolean isWaitingForPermission = false;
    private Fragment visibleFragment = null;
    private long lastClockUpdate = 0;
    private ScheduledFuture<?> unifiedTimerTask;
    private ScheduledExecutorService unifiedScheduler;
    private BroadcastReceiver usbPermissionResultReceiver;
    private AtomicBoolean isTimerScheduled = new AtomicBoolean(false);
    String address = null;
    private ProgressDialog progress;
    private String currentConnectionAddress;
    private String currentConnectionInfo;
    private Intent pendingScanIntent;
    private SharedPreferences lastDevicePrefs;
    public static String EXTRA_ADDRESS = "device_address";
    public static String EXTRA_INFO = "device_info";
    private boolean isScanPending = false;
    public BluetoothConnectionManager bluetoothManager;
    private String info_address;
    private static final int SCAN_ACTIVITY_REQUEST_CODE = 1001;
    private BluetoothAdapter myBluetooth = null;
    private final Handler uiHandler = new Handler(Looper.getMainLooper());
    private volatile boolean isExecutorShutdown = false;
    private ExecutorService backgroundExecutor;
    Button selectedButton = null;
    public DrawerLayout drawerLayout;
    public ActionBarDrawerToggle actionBarDrawerToggle;
    private NavigationView mNavigationView;
    TextView txtCounter, uNit;
    double value = 0.0;
    public static final int PERMISSION_REQUEST_CODE = 1001;
    DecimalFormat df = new DecimalFormat("000000");
    private ImageView connectionStatusIcon;
    private TextView connectionStatusTxt;
    private Button buttonA, buttonB;
    private LinearLayout abcLayout;
    private FrameLayout fragmentContainer;
    private View counterContainer;

    // Fragment instances
    private AFragment aFragment;
    private BFragment bFragment;
    private CFragment cFragment;
    private DFragment dFragment;
    private EFragment eFragment;

    // Key event handling flags
    private AtomicBoolean isProcessingKeyEvent = new AtomicBoolean(false);
    private long lastKeyEventTime = 0;
    private static final long KEY_EVENT_DEBOUNCE_MS = 500;

    // Auto-reconnect variables
    private static final int AUTO_RECONNECT_DELAY_MS = 8000;
    private static final int MAX_RECONNECT_ATTEMPTS = 5;
    private int reconnectAttempts = 0;
    private Handler reconnectHandler = new Handler(Looper.getMainLooper());
    private Runnable reconnectRunnable;
    private boolean isReconnecting = false;
    private AtomicBoolean isAutoConnectEnabled = new AtomicBoolean(true);
    private SharedPreferences autoConnectPrefs;
    private static final String PREF_AUTO_CONNECT = "auto_connect_enabled";

    // Connection state tracking
    private AtomicBoolean isConnected = new AtomicBoolean(false);
    private AtomicBoolean isConnecting = new AtomicBoolean(false);
    private long lastConnectionAttemptTime = 0;
    private static final long MIN_CONNECTION_INTERVAL_MS = 15000;

    // Printer management
    private PrinterManager printerManager;
    private PrinterMenuHelper printerMenuHelper;
    private boolean isPrinterConnected = false;
    private String currentPrinterName = "";
    private UsbPrinterManager usbPrinterManager;
    private UsbPermissionHelper permissionHelper;

    // USB Printer connection state tracking
    private AtomicBoolean isUsbConnecting = new AtomicBoolean(false);
    private AtomicBoolean isUsbConnected = new AtomicBoolean(false);
    private String lastConnectedUsbDevice = "";
    private static final long USB_CONNECTION_COOLDOWN_MS = 10000;
    private long lastUsbConnectionAttempt = 0;
    private AtomicBoolean isUsbAutoConnectAttempted = new AtomicBoolean(false);
    private AtomicBoolean isUsbPermissionRequested = new AtomicBoolean(false);

    // Track current state
    private enum FragmentState {
        NONE, FRAGMENT_A, FRAGMENT_B, FRAGMENT_C, FRAGMENT_D, FRAGMENT_E
    }
    private FragmentState currentFragmentState = FragmentState.NONE;

    private AtomicBoolean isCleaningUp = new AtomicBoolean(false);
    private boolean isAutoConnectAfterRebootAttempted = false;
    private static final String PREF_NAME = "PrintSettings";
    private static final String KEY_PRINT_TYPE = "print_type";
    private static final String KEY_PRINT_TYPE_DISPLAY = "print_type_display";

    // Print type constants
    public static final String PRINT_TYPE_PLAIN = "plain";
    public static final String PRINT_TYPE_PRINTED = "printed";
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initializeBasicComponents();
        setupToolbar();
        initializeUIComponents();
        initializeManagers();
        initializeFragments();
        setupListeners();

        executeInBackground(() -> {
            initializeBackgroundComponents();
            uiHandler.post(this::initializeUIAfterBackground);
        });

        handleIncomingIntent();

        // Call the missing method
        setupDelayedAutoStart();


        // Initialize PasswordManager
        passwordManager = new PasswordManager(this);
        passwordManager.setAuthListener(new PasswordManager.AuthListener() {
            @Override
            public void onPasswordResetSuccess() {

            }

            @Override
            public void onPasswordResetFailed(String reason) {

            }

            @Override
            public void onUserLoginSuccess(String username) {
                isUserLoggedIn = true;
                currentUser = username;
                runOnUiThread(() -> {
                    Toast.makeText(MainActivity.this, "Welcome " + username + "! Press F10 for settings", Toast.LENGTH_SHORT).show();
                });
            }

            @Override
            public void onUserLoginFailed() {
                runOnUiThread(() -> {
                    Toast.makeText(MainActivity.this, "Login failed", Toast.LENGTH_SHORT).show();
                });
            }

            @Override
            public void onUserRegistered(String username) {
                runOnUiThread(() -> {
                    Toast.makeText(MainActivity.this, "Registration request sent for " + username, Toast.LENGTH_SHORT).show();
                });
            }

            @Override
            public void onUserApproved(String username) {
                runOnUiThread(() -> {
                    Toast.makeText(MainActivity.this, "User " + username + " approved! They can now login.", Toast.LENGTH_LONG).show();
                });
            }

            @Override
            public void onUserRejected(String username) {
                runOnUiThread(() -> {
                    Toast.makeText(MainActivity.this, "User " + username + " rejected", Toast.LENGTH_SHORT).show();
                });
            }

            @Override
            public void onSessionExpired() {
                isUserLoggedIn = false;
                currentUser = null;
                runOnUiThread(() -> {
                    Toast.makeText(MainActivity.this, "Session expired. Please login again.", Toast.LENGTH_LONG).show();
                });
            }
        });

        // Check if already logged in with valid session
        if (passwordManager.isUserLoggedIn()) {
            isUserLoggedIn = true;
            currentUser = passwordManager.getCurrentUser();
            Toast.makeText(this, "Welcome back " + currentUser, Toast.LENGTH_SHORT).show();
        }

        if (passwordManager != null) {
        //checkAndSetupSecurity();
        }
    }
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == 1001) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "✅ Storage permission granted", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "❌ Storage permission denied", Toast.LENGTH_SHORT).show();
            }
        }
    }
    // Add this missing method
    private void setupDelayedAutoStart() {
        // Check permission after a short delay
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            checkUsbPermission();
        }, 2000);

        // Try USB auto-connect after a delay
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            safeUsbAutoConnect();
        }, 3000);

        // Try Bluetooth auto-connect after a delay
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            if (!isAutoConnectAfterRebootAttempted) {
                isAutoConnectAfterRebootAttempted = true;
                performDelayedAutoConnect();
            }
        }, 5000);
    }

    private void initializeBasicComponents() {
        lastDevicePrefs = getSharedPreferences("last_device", MODE_PRIVATE);
        autoConnectPrefs = getSharedPreferences("auto_connect_settings", MODE_PRIVATE);
        isAutoConnectEnabled.set(autoConnectPrefs.getBoolean(PREF_AUTO_CONNECT, true));
        bluetoothManager = new BluetoothConnectionManager(this);
        initializeReconnectRunnable();
    }

    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        Drawable drawable = toolbar.getOverflowIcon();
        if (drawable != null) {
            drawable = DrawableCompat.wrap(drawable);
            DrawableCompat.setTint(drawable.mutate(), getResources().getColor(R.color.white));
            toolbar.setOverflowIcon(drawable);
        }

        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setHomeButtonEnabled(true);
        }
    }

    private void initializeUIComponents() {
        connectionStatusIcon = findViewById(R.id.connectionStatus);
        connectionStatusTxt = findViewById(R.id.connectionStatusTxt);
        buttonA = findViewById(R.id.buttonA);
        buttonB = findViewById(R.id.buttonB);
        txtCounter = findViewById(R.id.txtCounter);
        uNit = findViewById(R.id.uNit);
        abcLayout = findViewById(R.id.abc);
        fragmentContainer = findViewById(R.id.fragment_container);

        counterContainer = findViewById(R.id.counter_container);
        if (counterContainer == null) {
            counterContainer = new View(this);
        }

        showCounterViews();

        drawerLayout = findViewById(R.id.draw_layout);
        actionBarDrawerToggle = new ActionBarDrawerToggle(this, drawerLayout, R.string.nav_open, R.string.nav_close) {
            public void onDrawerClosed(View view) { super.onDrawerClosed(view); }
            public void onDrawerOpened(View drawerView) { super.onDrawerOpened(drawerView); }
            public void onDrawerStateChanged(int i) { }
            public void onDrawerSlide(View view, float v) { }
        };

        drawerLayout.addDrawerListener(actionBarDrawerToggle);
        actionBarDrawerToggle.syncState();
        actionBarDrawerToggle.getDrawerArrowDrawable().setColor(getResources().getColor(R.color.white));

        mNavigationView = findViewById(R.id.nav_view);
        mNavigationView.setNavigationItemSelectedListener(MainActivity.this);
        mNavigationView.setItemIconTintList(null);
    }

    private void initializeManagers() {
        printerManager = PrinterManager.getInstance();
        printerManager.init(this);

        usbPrinterManager = UsbPrinterManager.getInstance();
        usbPrinterManager.init(this);

        permissionHelper = new UsbPermissionHelper(this);

        initializePrinterMenuHelper();
        setupUsbPrinterListener();
        setupPrinterManagerListener();
    }

    private void initializePrinterMenuHelper() {
        printerMenuHelper = new PrinterMenuHelper(this, new PrinterMenuHelper.PrinterDialogCallback() {
            @Override
            public void onPrinterSelected(UsbDevice printer) {
                if (isUsbConnecting.get() || isUsbConnected.get()) {
                    Log.d("USB", "Already connecting/connected to USB printer");
                    return;
                }

                long currentTime = System.currentTimeMillis();
                if (currentTime - lastUsbConnectionAttempt < USB_CONNECTION_COOLDOWN_MS) {
                    Log.d("USB", "USB connection attempt too frequent, skipping");
                    return;
                }

                if (printer != null && printer.getDeviceName().equals(lastConnectedUsbDevice) && isUsbConnected.get()) {
                    Log.d("USB", "Already connected to this printer");
                    return;
                }

                lastUsbConnectionAttempt = currentTime;
                isUsbConnecting.set(true);

                if (printer != null) {
                    lastConnectedUsbDevice = printer.getDeviceName();
                }

                isPrinterConnected = true;
                currentPrinterName = printerManager.getConnectedPrinterName();
                if (currentPrinterName == null || currentPrinterName.isEmpty()) {
                    currentPrinterName = "USB Printer";
                }

                isUsbConnected.set(true);
                isUsbConnecting.set(false);

                invalidateOptionsMenu();
                updatePrinterStatusInFragments();

                Toast.makeText(MainActivity.this,
                        "✅ Printer ready: " + currentPrinterName,
                        Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onPrinterDisconnected() {
                isPrinterConnected = false;
                isUsbConnected.set(false);
                isUsbConnecting.set(false);
                isUsbAutoConnectAttempted.set(false);
                isUsbPermissionRequested.set(false);
                currentPrinterName = "";
                lastConnectedUsbDevice = "";
                invalidateOptionsMenu();
                updatePrinterStatusInFragments();
            }

            @Override
            public void onTestPrint() {
                Toast.makeText(MainActivity.this, "Test print completed", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setupUsbPrinterListener() {
        usbPrinterManager.addListener(new UsbPrinterManager.PrinterAdapter() {
            @Override
            public void onPrinterConnected(String printerName) {
                runOnUiThread(() -> {
                    if (isUsbConnected.get() && printerName.equals(currentPrinterName)) {
                        Log.d("USB", "Already connected to this printer, ignoring duplicate event");
                        return;
                    }

                    isUsbConnected.set(true);
                    isUsbConnecting.set(false);
                    isUsbAutoConnectAttempted.set(true);
                    isPrinterConnected = true;
                    currentPrinterName = printerName;
                    lastConnectedUsbDevice = printerName;

                    Toast.makeText(MainActivity.this,
                            "✅ Printer connected: " + printerName, Toast.LENGTH_SHORT).show();
                    invalidateOptionsMenu();
                    updatePrinterStatusInFragments();
                });
            }

            @Override
            public void onPrinterDisconnected() {
                runOnUiThread(() -> {
                    isUsbConnected.set(false);
                    isUsbConnecting.set(false);
                    isUsbAutoConnectAttempted.set(false);
                    isUsbPermissionRequested.set(false);
                    isPrinterConnected = false;
                    currentPrinterName = "";
                    lastConnectedUsbDevice = "";

                    Toast.makeText(MainActivity.this,
                            "❌ Printer disconnected", Toast.LENGTH_SHORT).show();
                    invalidateOptionsMenu();
                    updatePrinterStatusInFragments();
                });
            }

            @Override
            public void onPrintSuccess() {
                runOnUiThread(() -> {
                    Toast.makeText(MainActivity.this,
                            "✅ Print successful", Toast.LENGTH_SHORT).show();
                });
            }

            @Override
            public void onPrintError(String error) {
                runOnUiThread(() -> {
                    Toast.makeText(MainActivity.this,
                            "❌ Print error: " + error, Toast.LENGTH_LONG).show();
                });
            }
        });
    }

    private void setupPrinterManagerListener() {
        printerManager.addListener(new PrinterManager.PrinterConnectionAdapter() {
            @Override
            public void onPrinterConnected(String printerName) {
                runOnUiThread(() -> {
                    if (isUsbConnected.get() && printerName.equals(currentPrinterName)) {
                        return;
                    }

                    isPrinterConnected = true;
                    isUsbConnected.set(true);
                    isUsbConnecting.set(false);
                    isUsbAutoConnectAttempted.set(true);
                    currentPrinterName = printerName;
                    lastConnectedUsbDevice = printerName;
                    invalidateOptionsMenu();
                    updatePrinterStatusInFragments();
                    Toast.makeText(MainActivity.this, "Printer connected: " + printerName, Toast.LENGTH_SHORT).show();
                });
            }

            @Override
            public void onPrinterDisconnected() {
                runOnUiThread(() -> {
                    isPrinterConnected = false;
                    isUsbConnected.set(false);
                    isUsbConnecting.set(false);
                    isUsbAutoConnectAttempted.set(false);
                    isUsbPermissionRequested.set(false);
                    currentPrinterName = "";
                    lastConnectedUsbDevice = "";
                    invalidateOptionsMenu();
                    updatePrinterStatusInFragments();
                    Toast.makeText(MainActivity.this, "Printer disconnected", Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    private void initializeFragments() {
        aFragment = new AFragment();
        bFragment = new BFragment();
        cFragment = new CFragment();
        dFragment = new DFragment();
        eFragment = new EFragment();
    }

    private void setupListeners() {
        setupButtonClickListener(buttonA, aFragment, FragmentState.FRAGMENT_A, "#FFA500");
        setupButtonClickListener(buttonB, bFragment, FragmentState.FRAGMENT_B, "#00FF00");

        updateUIBasedOnState();
        setupClock();
    }

    private void safeUsbAutoConnect() {
        if (isUsbAutoConnectAttempted.get()) {
            Log.d("USB", "USB auto-connect already attempted, skipping");
            return;
        }

        if (isUsbConnected.get() || isUsbConnecting.get()) {
            Log.d("USB", "Already connected or connecting to USB, skipping auto-connect");
            return;
        }

        long currentTime = System.currentTimeMillis();
        if (currentTime - lastUsbConnectionAttempt < USB_CONNECTION_COOLDOWN_MS) {
            Log.d("USB", "USB connection attempt too frequent, skipping");
            return;
        }

        isUsbAutoConnectAttempted.set(true);
        lastUsbConnectionAttempt = currentTime;

        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            if (!isUsbConnected.get() && !isUsbConnecting.get() && printerMenuHelper != null) {
                Log.d("USB", "Attempting USB auto-connect");
                printerMenuHelper.tryAutoConnectLastPrinter();
            }
        }, 1000);
    }

    private void performDelayedAutoConnect() {
        if (isBluetoothConnected()) {
            Log.d("AutoConnect", "Already connected");
            updateConnectionUI(true);
            return;
        }

        if (!isAutoConnectEnabled.get()) {
            Log.d("AutoConnect", "Auto-connect is disabled");
            updateConnectionStatus("Disconnected", R.drawable.ic_bluetooth_disconnected);
            return;
        }

        String savedAddress = lastDevicePrefs.getString("address", "");
        String savedInfo = lastDevicePrefs.getString("info", "");

        if (!savedAddress.isEmpty()) {
            Log.d("AutoConnect", "Found saved device: " + savedAddress);
            updateConnectionStatus("Auto-connecting...", R.drawable.ic_bluetooth_connecting);

            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                connectToDevice(savedAddress, savedInfo);
            }, 1000);
        } else {
            Log.d("AutoConnect", "No saved device found");
            updateConnectionStatus("Disconnected", R.drawable.ic_bluetooth_disconnected);
        }
    }

    private void checkUsbPermission() {
        if (isUsbPermissionRequested.get()) {
            return;
        }

        UsbPermissionHelper permissionHelper = new UsbPermissionHelper(this);

        if (!permissionHelper.hasPrinterPermission()) {
            UsbManager usbManager = (UsbManager) getSystemService(Context.USB_SERVICE);
            HashMap<String, UsbDevice> deviceList = usbManager.getDeviceList();

            if (deviceList.isEmpty()) {
                new AlertDialog.Builder(this)
                        .setTitle("No USB Device")
                        .setMessage("No USB devices detected.\n\nPlease connect your Canon printer via USB and ensure it's powered on.")
                        .setPositiveButton("OK", null)
                        .show();
            } else {
                isUsbPermissionRequested.set(true);
                permissionHelper.showPermissionTroubleshooting();
            }
        }
    }

    private void updatePrinterStatusInFragments() {
        if (aFragment != null && aFragment.isAdded()) {
            aFragment.updatePrinterStatus(isPrinterConnected, currentPrinterName);
        }
        if (dFragment != null && dFragment.isAdded()) {
            dFragment.updatePrinterStatus(isPrinterConnected, currentPrinterName);
        }
    }

    public PrinterManager getPrinterManager() {
        return printerManager;
    }

    public boolean isPrinterConnected() {
        return isPrinterConnected;
    }

    public String getConnectedPrinterName() {
        return currentPrinterName;
    }

    private void initializeReconnectRunnable() {
        reconnectRunnable = new Runnable() {
            @Override
            public void run() {
                if (isReconnecting && isAutoConnectEnabled.get()) {

                    boolean actuallyConnected = isBluetoothConnected();

                    if (actuallyConnected) {
                        Log.d("Reconnect", "Already connected, stopping reconnection");
                        runOnUiThread(() -> {
                            stopReconnectionAttempts();
                            updateConnectionUI(true);
                        });
                        return;
                    }

                    if (!isConnected.get() && !isConnecting.get()) {
                        if (reconnectAttempts < MAX_RECONNECT_ATTEMPTS) {
                            Log.d("Reconnect", "Attempting to reconnect... Attempt " + (reconnectAttempts + 1));

                            String savedAddress = lastDevicePrefs.getString("address", "");
                            String savedInfo = lastDevicePrefs.getString("info", "");

                            if (!savedAddress.isEmpty() && !savedAddress.equals(currentConnectionAddress)) {
                                cleanupConnection();

                                runOnUiThread(() -> {
                                    Toast.makeText(MainActivity.this,
                                            "Attempting to reconnect... (" + (reconnectAttempts + 1) + "/" + MAX_RECONNECT_ATTEMPTS + ")",
                                            Toast.LENGTH_SHORT).show();
                                    updateConnectionStatus("Reconnecting...", R.drawable.ic_bluetooth_connecting);
                                });

                                new Handler(Looper.getMainLooper()).postDelayed(() -> {
                                    if (!isBluetoothConnected() && !isConnecting.get()) {
                                        connectToDevice(savedAddress, savedInfo);
                                    }
                                }, 1000);

                                reconnectAttempts++;
                            } else {
                                stopReconnectionAttempts();
                                runOnUiThread(() -> {
                                    Toast.makeText(MainActivity.this,
                                            "No previously connected device found.",
                                            Toast.LENGTH_LONG).show();
                                    updateConnectionStatus("Disconnected", R.drawable.ic_bluetooth_disconnected);
                                });
                            }
                        } else {
                            stopReconnectionAttempts();
                            runOnUiThread(() -> {
                                Toast.makeText(MainActivity.this,
                                        "Max reconnection attempts reached. Please connect manually.",
                                        Toast.LENGTH_LONG).show();
                                updateConnectionStatus("Disconnected", R.drawable.ic_bluetooth_disconnected);
                            });
                        }
                    } else {
                        Log.d("Reconnect", "Already connected or connecting, stopping reconnection");
                        stopReconnectionAttempts();
                    }
                }
            }
        };
    }

    private void cleanupConnection() {
        if (isCleaningUp.get()) return;

        isCleaningUp.set(true);
        try {
            if (bluetoothManager != null) {
                bluetoothManager.disconnect();

                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }

                bluetoothManager.forceCloseConnection();
            }
        } catch (Exception e) {
            Log.e("Cleanup", "Error during cleanup: " + e.getMessage());
        } finally {
            isCleaningUp.set(false);
        }
    }

    private void startReconnectionAttempts() {
        if (isBluetoothConnected() || isReconnecting || isConnecting.get()) {
            Log.d("Reconnect", "Cannot start reconnection - connected: " + isBluetoothConnected() +
                    ", isReconnecting: " + isReconnecting +
                    ", isConnecting: " + isConnecting.get());
            return;
        }

        if (isAutoConnectEnabled.get()) {
            isReconnecting = true;
            reconnectAttempts = 0;
            Log.d("Reconnect", "Starting reconnection attempts");
            scheduleNextReconnectAttempt();
        }
    }

    private void stopReconnectionAttempts() {
        if (isReconnecting) {
            isReconnecting = false;
            reconnectAttempts = 0;
            reconnectHandler.removeCallbacks(reconnectRunnable);
            Log.d("Reconnect", "Stopped reconnection attempts");
        }
    }

    private void scheduleNextReconnectAttempt() {
        reconnectHandler.removeCallbacks(reconnectRunnable);

        if (isBluetoothConnected()) {
            stopReconnectionAttempts();
            return;
        }

        reconnectHandler.postDelayed(reconnectRunnable, AUTO_RECONNECT_DELAY_MS);
    }

    private void updateConnectionStatus(String status, int iconResId) {
        runOnUiThread(() -> {
            if (connectionStatusTxt != null && connectionStatusIcon != null) {
                connectionStatusTxt.setText(status);
                connectionStatusIcon.setImageResource(iconResId);
            }
        });
    }

    private void updateConnectionUI(boolean connected) {
        isConnected.set(connected);

        if (connected) {
            isConnecting.set(false);
            isReconnecting = false;
            reconnectAttempts = 0;
            reconnectHandler.removeCallbacks(reconnectRunnable);
            updateConnectionStatus("Connected", R.drawable.ic_bluetooth_connected);
        } else {
            updateConnectionStatus("Disconnected", R.drawable.ic_bluetooth_disconnected);
        }
    }

    private void showCounterViews() {
        if (txtCounter != null && abcLayout != null && uNit != null && fragmentContainer != null) {
            txtCounter.setVisibility(View.VISIBLE);
            abcLayout.setVisibility(View.VISIBLE);
            uNit.setVisibility(View.VISIBLE);
            fragmentContainer.setVisibility(View.GONE);
            txtCounter.setTextColor(Color.parseColor("#FFFFFF"));
        }
    }

    private void hideCounterViews() {
        if (txtCounter != null && abcLayout != null && uNit != null && fragmentContainer != null) {
            txtCounter.setVisibility(View.GONE);
            abcLayout.setVisibility(View.GONE);
            uNit.setVisibility(View.GONE);
            fragmentContainer.setVisibility(View.VISIBLE);
        }
    }

    private void updateUIBasedOnState() {
        if (currentFragmentState == FragmentState.NONE) {
            showCounterViews();
            if (selectedButton != null) {
                selectedButton.setSelected(false);
                selectedButton = null;
            }
        } else {
            hideCounterViews();
            String colorHex;
            switch (currentFragmentState) {
                case FRAGMENT_A:
                    colorHex = "#FFA500";
                    break;
                case FRAGMENT_B:
                    colorHex = "#00FF00";
                    break;
                case FRAGMENT_C:
                    colorHex = "#2196F3";
                    break;
                case FRAGMENT_D:
                    colorHex = "#FF5722";
                    break;
                case FRAGMENT_E:
                    colorHex = "#FF5732";
                    break;
                default:
                    colorHex = "#FFFFFF";
            }
            if (txtCounter != null) {
                txtCounter.setTextColor(Color.parseColor(colorHex));
            }
        }
    }

    void hideFragmentAndShowCounter() {
        if (visibleFragment != null) {
            getSupportFragmentManager().beginTransaction()
                    .hide(visibleFragment)
                    .commitAllowingStateLoss();

            visibleFragment = null;
            currentFragmentState = FragmentState.NONE;

            if (selectedButton != null) {
                selectedButton.setSelected(false);
                selectedButton = null;
            }

            updateUIBasedOnState();
        }
    }

    private void showFragmentAndHideCounter(Button button, Fragment fragment, FragmentState state, String colorHex) {
        if (visibleFragment != null && visibleFragment != fragment) {
            getSupportFragmentManager().beginTransaction()
                    .hide(visibleFragment)
                    .commitAllowingStateLoss();

            if (selectedButton != null) {
                selectedButton.setSelected(false);
            }
        }

        if (!fragment.isAdded()) {
            getSupportFragmentManager().beginTransaction()
                    .add(R.id.fragment_container, fragment, state.name())
                    .commitAllowingStateLoss();
        } else {
            getSupportFragmentManager().beginTransaction()
                    .show(fragment)
                    .commitAllowingStateLoss();
        }

        visibleFragment = fragment;
        currentFragmentState = state;

        updateUIBasedOnState();

        if (button != null) {
            button.setSelected(true);
            selectedButton = button;
        }
    }

    private void toggleFragment(Button button, Fragment fragment, FragmentState state, String colorHex) {
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastKeyEventTime < KEY_EVENT_DEBOUNCE_MS) {
            return;
        }
        lastKeyEventTime = currentTime;

        if (currentFragmentState == state) {
            hideFragmentAndShowCounter();
        } else {
            showFragmentAndHideCounter(button, fragment, state, colorHex);
        }
    }

    private void toggleEFragment() {
        if (currentFragmentState == FragmentState.FRAGMENT_E) {
            hideFragmentAndShowCounter();
            Toast.makeText(this, "Delete Dialog Closed", Toast.LENGTH_SHORT).show();
        } else {
            if (visibleFragment != null && visibleFragment != eFragment) {
                getSupportFragmentManager().beginTransaction()
                        .hide(visibleFragment)
                        .commitAllowingStateLoss();

                if (selectedButton != null) {
                    selectedButton.setSelected(false);
                    selectedButton = null;
                }
            }

            if (!eFragment.isAdded()) {
                getSupportFragmentManager().beginTransaction()
                        .add(R.id.fragment_container, eFragment, "FRAGMENT_E")
                        .commitAllowingStateLoss();
            } else {
                getSupportFragmentManager().beginTransaction()
                        .show(eFragment)
                        .commitAllowingStateLoss();
            }

            visibleFragment = eFragment;
            currentFragmentState = FragmentState.FRAGMENT_E;

            updateUIBasedOnState();
            if (txtCounter != null) {
                txtCounter.setTextColor(Color.parseColor("#FF5722"));
            }
            Toast.makeText(this, "Delete Dialog Opened (F3)", Toast.LENGTH_SHORT).show();
        }
    }

    private void toggleReportFragment() {
        if (currentFragmentState == FragmentState.FRAGMENT_C) {
            hideFragmentAndShowCounter();
            Toast.makeText(this, "Report View Closed", Toast.LENGTH_SHORT).show();
        } else {
            if (visibleFragment != null && visibleFragment != cFragment) {
                getSupportFragmentManager().beginTransaction()
                        .hide(visibleFragment)
                        .commitAllowingStateLoss();

                if (selectedButton != null) {
                    selectedButton.setSelected(false);
                    selectedButton = null;
                }
            }

            if (!cFragment.isAdded()) {
                getSupportFragmentManager().beginTransaction()
                        .add(R.id.fragment_container, cFragment, "FRAGMENT_C")
                        .commitAllowingStateLoss();
            } else {
                getSupportFragmentManager().beginTransaction()
                        .show(cFragment)
                        .commitAllowingStateLoss();

                if (cFragment.isAdded()) {
                    cFragment.refreshReport();
                }
            }

            visibleFragment = cFragment;
            currentFragmentState = FragmentState.FRAGMENT_C;

            updateUIBasedOnState();
            if (txtCounter != null) {
                txtCounter.setTextColor(Color.parseColor("#2196F3"));
            }
            Toast.makeText(this, "Report View Opened (F5)", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onBackPressed() {
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START);
        } else if (currentFragmentState != FragmentState.NONE) {
            hideFragmentAndShowCounter();
        } else {
            super.onBackPressed();
        }
    }

    private void setupButtonClickListener(Button button, Fragment fragment, FragmentState state, String colorHex) {
        button.setOnClickListener(v -> toggleFragment(button, fragment, state, colorHex));
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        if (event.getAction() == KeyEvent.ACTION_DOWN) {
            int keyCode = event.getKeyCode();

            if (keyCode == KeyEvent.KEYCODE_BACK) {
                if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
                    drawerLayout.closeDrawer(GravityCompat.START);
                    return true;
                } else if (currentFragmentState != FragmentState.NONE) {
                    hideFragmentAndShowCounter();
                    return true;
                }
                return false;
            }

            if (keyCode == KeyEvent.KEYCODE_ESCAPE) {
                if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
                    drawerLayout.closeDrawer(GravityCompat.START);
                    return true;
                } else if (currentFragmentState != FragmentState.NONE) {
                    hideFragmentAndShowCounter();
                    Toast.makeText(this, "Returned to Home", Toast.LENGTH_SHORT).show();
                    return true;
                } else {
                    if (aFragment != null && aFragment.isAdded()) {
                        aFragment.performGButtonActionClear();
                    }
                    if (bFragment != null && bFragment.isAdded()) {
                        bFragment.performGButtonActionClear();
                    }
                    return true;
                }
            }

            View currentFocus = getCurrentFocus();
            boolean isEditTextFocused = currentFocus instanceof EditText;
            boolean isButtonFocused = currentFocus instanceof Button ||
                    currentFocus instanceof androidx.appcompat.widget.AppCompatButton;

            if (keyCode == KeyEvent.KEYCODE_ENTER || keyCode == KeyEvent.KEYCODE_NUMPAD_ENTER) {
                if (isButtonFocused) {
                    if (currentFocus.getId() == R.id.button4a) {
                        if (visibleFragment instanceof AFragment && visibleFragment.isVisible()) {
                            ((AFragment) visibleFragment).performPrintAction();
                            return true;
                        }
                        if (visibleFragment instanceof BFragment && visibleFragment.isVisible()) {
                            ((BFragment) visibleFragment).finalizeWeighmentEntry();
                            return true;
                        }
                    } else if (currentFocus.getId() == R.id.button5a) {
                        if (visibleFragment instanceof AFragment && visibleFragment.isVisible()) {
                            ((AFragment) visibleFragment).performPrintAction();
                            return true;
                        }
                    }
                    return true;
                }
                return false;
            }

            if ((keyCode == KeyEvent.KEYCODE_T) && !isEditTextFocused) {
                if (visibleFragment instanceof AFragment && visibleFragment.isVisible()) {
                    AFragment aFragment = (AFragment) visibleFragment;
                    aFragment.performTButtonAction();
                    return true;
                }
                if (visibleFragment instanceof BFragment && visibleFragment.isVisible()) {
                    BFragment bFragment = (BFragment) visibleFragment;
                    bFragment.performTButtonAction();
                    return true;
                }
                return true;
            }

            if ((keyCode == KeyEvent.KEYCODE_G) && !isEditTextFocused) {
                if (visibleFragment instanceof AFragment && visibleFragment.isVisible()) {
                    AFragment aFragment = (AFragment) visibleFragment;
                    aFragment.performGButtonAction();
                    return true;
                }
                if (visibleFragment instanceof BFragment && visibleFragment.isVisible()) {
                    BFragment bFragment = (BFragment) visibleFragment;
                    bFragment.performGButtonAction();
                    return true;
                }
                return true;
            }

            if (keyCode == KeyEvent.KEYCODE_F5) {
                showAdminLoginForF5();
                return true;
            }

            if (keyCode == KeyEvent.KEYCODE_F1) {
                toggleDFragment(); // Open fragment in admin mode
                //showAccessTypeDialogF1(); // Show dialog to choose access type
                return true;
            }

            if (keyCode == KeyEvent.KEYCODE_F3) {

                showAdminLoginForF3();
                return true;
            }

            if (keyCode == KeyEvent.KEYCODE_F4) {
                if (visibleFragment instanceof AFragment && visibleFragment.isVisible()) {
                    AFragment aFragment = (AFragment) visibleFragment;
                    aFragment.performMButtonAction();
                    return true;
                }
                     //showAdminLoginForF4();

                return true;
            }

            if (keyCode == KeyEvent.KEYCODE_F2) {
                if (keyCode == KeyEvent.KEYCODE_F2) {
                    showAdminLoginForMasterData();
                    return true;
                }
                return true;
            }

            if ((keyCode == KeyEvent.KEYCODE_1 || keyCode == KeyEvent.KEYCODE_NUMPAD_1) && !isEditTextFocused) {

               // showAccessTypeDialog1(); // Show dialog to choose access type
                toggleFragment(buttonA, aFragment, FragmentState.FRAGMENT_A, "#FFA500");
                return true;
            }

            if ((keyCode == KeyEvent.KEYCODE_2 || keyCode == KeyEvent.KEYCODE_NUMPAD_2) && !isEditTextFocused) {

                toggleFragment(buttonB, bFragment, FragmentState.FRAGMENT_B, "#00FF00");
                // showAccessTypeDialog2();

                return true;
            }
        }

        return super.dispatchKeyEvent(event);
    }

    /**
     * Show dialog to choose access type (User or Admin)
     */
    private void showAccessTypeDialogF1() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Select Access Type");
        builder.setMessage("Choose how you want to access:");

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(50, 20, 50, 20);

        Button userButton = new Button(this);
        userButton.setText("User Access");
        userButton.setBackgroundColor(Color.parseColor("#4CAF50"));
        userButton.setTextColor(Color.WHITE);
        userButton.setPadding(30, 15, 30, 15);
        userButton.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT));
        layout.addView(userButton);

        // Add some space between buttons
        TextView spacer = new TextView(this);
        spacer.setText("");
        spacer.setHeight(20);
        layout.addView(spacer);

        Button adminButton = new Button(this);
        adminButton.setText("Admin Access");
        adminButton.setBackgroundColor(Color.parseColor("#2196F3"));
        adminButton.setTextColor(Color.WHITE);
        adminButton.setPadding(30, 15, 30, 15);
        adminButton.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT));
        layout.addView(adminButton);

        builder.setView(layout);

        builder.setNegativeButton("Cancel", (dialogInterface, which) -> dialogInterface.dismiss());

        AlertDialog dialog = builder.create();
        dialog.show();

        // Set click listeners after dialog is created
        userButton.setOnClickListener(v -> {
            dialog.dismiss(); // Now dialog is in scope
            handleUserAccessF1();
        });

        adminButton.setOnClickListener(v -> {
            dialog.dismiss(); // Now dialog is in scope
            showAdminLoginForF1();
        });
    }

    private void showAccessTypeDialog2() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Select Access Type");
        builder.setMessage("Choose how you want to access:");

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(50, 20, 50, 20);

        Button userButton = new Button(this);
        userButton.setText("User Access");
        userButton.setBackgroundColor(Color.parseColor("#4CAF50"));
        userButton.setTextColor(Color.WHITE);
        userButton.setPadding(30, 15, 30, 15);
        userButton.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT));
        layout.addView(userButton);

        // Add some space between buttons
        TextView spacer = new TextView(this);
        spacer.setText("");
        spacer.setHeight(20);
        layout.addView(spacer);

        Button adminButton = new Button(this);
        adminButton.setText("Admin Access");
        adminButton.setBackgroundColor(Color.parseColor("#2196F3"));
        adminButton.setTextColor(Color.WHITE);
        adminButton.setPadding(30, 15, 30, 15);
        adminButton.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT));
        layout.addView(adminButton);

        builder.setView(layout);

        builder.setNegativeButton("Cancel", (dialogInterface, which) -> dialogInterface.dismiss());

        AlertDialog dialog = builder.create();
        dialog.show();

        // Set click listeners after dialog is created
        userButton.setOnClickListener(v -> {
            dialog.dismiss(); // Now dialog is in scope
            handleUserAccess2();
        });

        adminButton.setOnClickListener(v -> {
            dialog.dismiss(); // Now dialog is in scope
            showAdminLoginFor2();
        });
    }
    private void showAccessTypeDialog1() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Select Access Type");
        builder.setMessage("Choose how you want to access:");

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(50, 20, 50, 20);

        Button userButton = new Button(this);
        userButton.setText("User Access");
        userButton.setBackgroundColor(Color.parseColor("#4CAF50"));
        userButton.setTextColor(Color.WHITE);
        userButton.setPadding(30, 15, 30, 15);
        userButton.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT));
        layout.addView(userButton);

        // Add some space between buttons
        TextView spacer = new TextView(this);
        spacer.setText("");
        spacer.setHeight(20);
        layout.addView(spacer);

        Button adminButton = new Button(this);
        adminButton.setText("Admin Access");
        adminButton.setBackgroundColor(Color.parseColor("#2196F3"));
        adminButton.setTextColor(Color.WHITE);
        adminButton.setPadding(30, 15, 30, 15);
        adminButton.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT));
        layout.addView(adminButton);

        builder.setView(layout);

        builder.setNegativeButton("Cancel", (dialogInterface, which) -> dialogInterface.dismiss());

        AlertDialog dialog = builder.create();
        dialog.show();

        // Set click listeners after dialog is created
        userButton.setOnClickListener(v -> {
            dialog.dismiss(); // Now dialog is in scope
            handleUserAccess1();
        });

        adminButton.setOnClickListener(v -> {
            dialog.dismiss(); // Now dialog is in scope
            showAdminLoginFor1();
        });
    }

    /**
     * Handle user access (existing user login)
     */
    private void handleUserAccessF1() {
        if (isUserLoggedIn) {
            // Already logged in, open user fragment directly
            toggleDFragment();
        } else {
            // Show login dialog with option to register
            showLoginOrRegisterDialogF1();
        }
    }
    private void handleUserAccess1() {
        if (isUserLoggedIn) {
            // Already logged in, open user fragment directly
            toggleFragment(buttonA, aFragment, FragmentState.FRAGMENT_A, "#FFA500");
        } else {
            // Show login dialog with option to register
            showLoginOrRegisterDialog1();
        }
    }
    private void handleUserAccess2() {
        if (isUserLoggedIn) {
            // Already logged in, open user fragment directly
            toggleFragment(buttonB, bFragment, FragmentState.FRAGMENT_B, "#00FF00");
        } else {
            // Show login dialog with option to register
            showLoginOrRegisterDialog2();
        }
    }

    /**
     * Show admin login for F1 access
     */
    private void showAdminLoginFor1() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Admin Login Required");
        builder.setMessage("Enter admin credentials to access admin panel");

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(50, 20, 50, 20);

        // Username field
        TextView usernameLabel = new TextView(this);
        usernameLabel.setText("Username:");
        usernameLabel.setTextSize(16);
        usernameLabel.setPadding(0, 10, 0, 5);
        layout.addView(usernameLabel);

        EditText usernameInput = new EditText(this);
        usernameInput.setHint("Enter admin username");
        usernameInput.setText("admin");
        usernameInput.setInputType(InputType.TYPE_CLASS_TEXT);
        usernameInput.setId(View.generateViewId());
        layout.addView(usernameInput);

        // Password field
        TextView passwordLabel = new TextView(this);
        passwordLabel.setText("Password:");
        passwordLabel.setTextSize(16);
        passwordLabel.setPadding(0, 20, 0, 5);
        layout.addView(passwordLabel);

        EditText passwordInput = new EditText(this);
        passwordInput.setHint("Enter admin password");
        passwordInput.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        passwordInput.setId(View.generateViewId());
        layout.addView(passwordInput);

        builder.setView(layout);

        builder.setPositiveButton("Login", null);
        builder.setNegativeButton("Cancel", (dialogInterface, which) -> dialogInterface.dismiss());

        AlertDialog dialog = builder.create();
        dialog.show();

        Button loginButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
        Button cancelButton = dialog.getButton(AlertDialog.BUTTON_NEGATIVE);

        // Style buttons
        loginButton.setBackgroundColor(Color.parseColor("#4CAF50"));
        loginButton.setTextColor(Color.WHITE);
        loginButton.setPadding(30, 15, 30, 15);

        cancelButton.setBackgroundColor(Color.parseColor("#F44336"));
        cancelButton.setTextColor(Color.WHITE);
        cancelButton.setPadding(30, 15, 30, 15);

        loginButton.setOnClickListener(v -> {
            String username = usernameInput.getText().toString().trim();
            String password = passwordInput.getText().toString();

            // Check against fixed admin credentials
            if (username.equals("admin") && password.equals("admin@123")) {
                dialog.dismiss();
                // Set admin mode flag if needed
                isAdminMode = true;
                toggleFragment(buttonA, aFragment, FragmentState.FRAGMENT_A, "#FFA500");

            } else {
                Toast.makeText(this, "Invalid admin credentials", Toast.LENGTH_SHORT).show();
                passwordInput.setText("");
                passwordInput.requestFocus();
            }
        });

        // Enter key handler
        passwordInput.setOnKeyListener((v, keyCode, event) -> {
            if (event.getAction() == KeyEvent.ACTION_DOWN &&
                    (keyCode == KeyEvent.KEYCODE_ENTER || keyCode == KeyEvent.KEYCODE_NUMPAD_ENTER)) {
                loginButton.performClick();
                return true;
            }
            return false;
        });

        usernameInput.requestFocus();
    }
    private void showAdminLoginFor2() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Admin Login Required");
        builder.setMessage("Enter admin credentials to access admin panel");

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(50, 20, 50, 20);

        // Username field
        TextView usernameLabel = new TextView(this);
        usernameLabel.setText("Username:");
        usernameLabel.setTextSize(16);
        usernameLabel.setPadding(0, 10, 0, 5);
        layout.addView(usernameLabel);

        EditText usernameInput = new EditText(this);
        usernameInput.setHint("Enter admin username");
        usernameInput.setText("admin");
        usernameInput.setInputType(InputType.TYPE_CLASS_TEXT);
        usernameInput.setId(View.generateViewId());
        layout.addView(usernameInput);

        // Password field
        TextView passwordLabel = new TextView(this);
        passwordLabel.setText("Password:");
        passwordLabel.setTextSize(16);
        passwordLabel.setPadding(0, 20, 0, 5);
        layout.addView(passwordLabel);

        EditText passwordInput = new EditText(this);
        passwordInput.setHint("Enter admin password");
        passwordInput.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        passwordInput.setId(View.generateViewId());
        layout.addView(passwordInput);

        builder.setView(layout);

        builder.setPositiveButton("Login", null);
        builder.setNegativeButton("Cancel", (dialogInterface, which) -> dialogInterface.dismiss());

        AlertDialog dialog = builder.create();
        dialog.show();

        Button loginButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
        Button cancelButton = dialog.getButton(AlertDialog.BUTTON_NEGATIVE);

        // Style buttons
        loginButton.setBackgroundColor(Color.parseColor("#4CAF50"));
        loginButton.setTextColor(Color.WHITE);
        loginButton.setPadding(30, 15, 30, 15);

        cancelButton.setBackgroundColor(Color.parseColor("#F44336"));
        cancelButton.setTextColor(Color.WHITE);
        cancelButton.setPadding(30, 15, 30, 15);

        loginButton.setOnClickListener(v -> {
            String username = usernameInput.getText().toString().trim();
            String password = passwordInput.getText().toString();

            // Check against fixed admin credentials
            if (username.equals("admin") && password.equals("admin@123")) {
                dialog.dismiss();
                // Set admin mode flag if needed
                isAdminMode = true;
                toggleFragment(buttonB, bFragment, FragmentState.FRAGMENT_B, "#00FF00");

            } else {
                Toast.makeText(this, "Invalid admin credentials", Toast.LENGTH_SHORT).show();
                passwordInput.setText("");
                passwordInput.requestFocus();
            }
        });

        // Enter key handler
        passwordInput.setOnKeyListener((v, keyCode, event) -> {
            if (event.getAction() == KeyEvent.ACTION_DOWN &&
                    (keyCode == KeyEvent.KEYCODE_ENTER || keyCode == KeyEvent.KEYCODE_NUMPAD_ENTER)) {
                loginButton.performClick();
                return true;
            }
            return false;
        });

        usernameInput.requestFocus();
    }
    private void showAdminLoginForF1() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Admin Login Required");
        builder.setMessage("Enter admin credentials to access admin panel");

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(50, 20, 50, 20);

        // Username field
        TextView usernameLabel = new TextView(this);
        usernameLabel.setText("Username:");
        usernameLabel.setTextSize(16);
        usernameLabel.setPadding(0, 10, 0, 5);
        layout.addView(usernameLabel);

        EditText usernameInput = new EditText(this);
        usernameInput.setHint("Enter admin username");
        usernameInput.setText("admin");
        usernameInput.setInputType(InputType.TYPE_CLASS_TEXT);
        usernameInput.setId(View.generateViewId());
        layout.addView(usernameInput);

        // Password field
        TextView passwordLabel = new TextView(this);
        passwordLabel.setText("Password:");
        passwordLabel.setTextSize(16);
        passwordLabel.setPadding(0, 20, 0, 5);
        layout.addView(passwordLabel);

        EditText passwordInput = new EditText(this);
        passwordInput.setHint("Enter admin password");
        passwordInput.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        passwordInput.setId(View.generateViewId());
        layout.addView(passwordInput);

        builder.setView(layout);

        builder.setPositiveButton("Login", null);
        builder.setNegativeButton("Cancel", (dialogInterface, which) -> dialogInterface.dismiss());

        AlertDialog dialog = builder.create();
        dialog.show();

        Button loginButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
        Button cancelButton = dialog.getButton(AlertDialog.BUTTON_NEGATIVE);

        // Style buttons
        loginButton.setBackgroundColor(Color.parseColor("#4CAF50"));
        loginButton.setTextColor(Color.WHITE);
        loginButton.setPadding(30, 15, 30, 15);

        cancelButton.setBackgroundColor(Color.parseColor("#F44336"));
        cancelButton.setTextColor(Color.WHITE);
        cancelButton.setPadding(30, 15, 30, 15);

        loginButton.setOnClickListener(v -> {
            String username = usernameInput.getText().toString().trim();
            String password = passwordInput.getText().toString();

            // Check against fixed admin credentials
            if (username.equals("admin") && password.equals("admin@123")) {
                dialog.dismiss();
                // Set admin mode flag if needed
                isAdminMode = true;
                toggleDFragment(); // Open fragment in admin mode
            } else {
                Toast.makeText(this, "Invalid admin credentials", Toast.LENGTH_SHORT).show();
                passwordInput.setText("");
                passwordInput.requestFocus();
            }
        });

        // Enter key handler
        passwordInput.setOnKeyListener((v, keyCode, event) -> {
            if (event.getAction() == KeyEvent.ACTION_DOWN &&
                    (keyCode == KeyEvent.KEYCODE_ENTER || keyCode == KeyEvent.KEYCODE_NUMPAD_ENTER)) {
                loginButton.performClick();
                return true;
            }
            return false;
        });

        usernameInput.requestFocus();
    }

    private void showAdminLoginForF5() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Admin Login Required");
        builder.setMessage("Enter admin credentials to access Master Data");

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(50, 20, 50, 20);

        // Username field
        TextView usernameLabel = new TextView(this);
        usernameLabel.setText("Username:");
        usernameLabel.setTextSize(16);
        usernameLabel.setPadding(0, 10, 0, 5);
        layout.addView(usernameLabel);

        EditText usernameInput = new EditText(this);
        usernameInput.setHint("Enter admin username");
        usernameInput.setText("admin");
        usernameInput.setInputType(InputType.TYPE_CLASS_TEXT);
        usernameInput.setId(View.generateViewId());
        layout.addView(usernameInput);

        // Password field
        TextView passwordLabel = new TextView(this);
        passwordLabel.setText("Password:");
        passwordLabel.setTextSize(16);
        passwordLabel.setPadding(0, 20, 0, 5);
        layout.addView(passwordLabel);

        EditText passwordInput = new EditText(this);
        passwordInput.setHint("Enter admin password");
        passwordInput.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        passwordInput.setId(View.generateViewId());
        layout.addView(passwordInput);

        builder.setView(layout);

        builder.setPositiveButton("Login", null);
        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss());

        AlertDialog dialog = builder.create();
        dialog.show();

        Button loginButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
        Button cancelButton = dialog.getButton(AlertDialog.BUTTON_NEGATIVE);

        // After getting the button references
        loginButton.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                loginButton.setBackgroundColor(Color.parseColor("#2E7D32")); // Darker green
                loginButton.setScaleX(1.02f);
                loginButton.setScaleY(1.02f);
            } else {
                loginButton.setBackgroundColor(Color.parseColor("#4CAF50")); // Normal green
                loginButton.setScaleX(1.0f);
                loginButton.setScaleY(1.0f);
            }
        });

        cancelButton.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                cancelButton.setBackgroundColor(Color.parseColor("#C62828")); // Darker red
                cancelButton.setScaleX(1.02f);
                cancelButton.setScaleY(1.02f);
            } else {
                cancelButton.setBackgroundColor(Color.parseColor("#F44336")); // Normal red
                cancelButton.setScaleX(1.0f);
                cancelButton.setScaleY(1.0f);
            }
        });

// Make buttons focusable for navigation
        loginButton.setFocusable(true);
        loginButton.setFocusableInTouchMode(true);
        cancelButton.setFocusable(true);
        cancelButton.setFocusableInTouchMode(true);

// Set navigation order
        passwordInput.setNextFocusDownId(loginButton.getId());
        loginButton.setNextFocusLeftId(cancelButton.getId());
        cancelButton.setNextFocusRightId(loginButton.getId());

        // Style buttons
        loginButton.setBackgroundColor(Color.parseColor("#4CAF50"));
        loginButton.setTextColor(Color.WHITE);
        cancelButton.setBackgroundColor(Color.parseColor("#F44336"));
        cancelButton.setTextColor(Color.WHITE);

        loginButton.setOnClickListener(v -> {
            String username = usernameInput.getText().toString().trim();
            String password = passwordInput.getText().toString();

            // Check against fixed admin credentials
            if (passwordManager.validateAdminLogin(username, password)) {
                dialog.dismiss();
                toggleReportFragment();
            } else {
                Toast.makeText(this, "Invalid admin credentials", Toast.LENGTH_SHORT).show();
                passwordInput.setText("");
                passwordInput.requestFocus();
            }
        });

        // Enter key handler
        passwordInput.setOnKeyListener((v, keyCode, event) -> {
            if (event.getAction() == KeyEvent.ACTION_DOWN &&
                    (keyCode == KeyEvent.KEYCODE_ENTER || keyCode == KeyEvent.KEYCODE_NUMPAD_ENTER)) {
                loginButton.performClick();
                return true;
            }
            return false;
        });

        usernameInput.requestFocus();
    }
    /**
     * Show admin login before opening Master Data
     */
    private void showAdminLoginForF3() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Admin Login Required");
        builder.setMessage("Enter admin credentials to access Master Data");

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(50, 20, 50, 20);

        // Username field
        TextView usernameLabel = new TextView(this);
        usernameLabel.setText("Username:");
        usernameLabel.setTextSize(16);
        usernameLabel.setPadding(0, 10, 0, 5);
        layout.addView(usernameLabel);

        EditText usernameInput = new EditText(this);
        usernameInput.setHint("Enter admin username");
        usernameInput.setText("admin");
        usernameInput.setInputType(InputType.TYPE_CLASS_TEXT);
        usernameInput.setId(View.generateViewId());
        layout.addView(usernameInput);

        // Password field
        TextView passwordLabel = new TextView(this);
        passwordLabel.setText("Password:");
        passwordLabel.setTextSize(16);
        passwordLabel.setPadding(0, 20, 0, 5);
        layout.addView(passwordLabel);

        EditText passwordInput = new EditText(this);
        passwordInput.setHint("Enter admin password");
        passwordInput.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        passwordInput.setId(View.generateViewId());
        layout.addView(passwordInput);

        builder.setView(layout);

        builder.setPositiveButton("Login", null);
        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss());

        AlertDialog dialog = builder.create();
        dialog.show();

        Button loginButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
        Button cancelButton = dialog.getButton(AlertDialog.BUTTON_NEGATIVE);

        // After getting the button references
        loginButton.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                loginButton.setBackgroundColor(Color.parseColor("#2E7D32")); // Darker green
                loginButton.setScaleX(1.02f);
                loginButton.setScaleY(1.02f);
            } else {
                loginButton.setBackgroundColor(Color.parseColor("#4CAF50")); // Normal green
                loginButton.setScaleX(1.0f);
                loginButton.setScaleY(1.0f);
            }
        });

        cancelButton.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                cancelButton.setBackgroundColor(Color.parseColor("#C62828")); // Darker red
                cancelButton.setScaleX(1.02f);
                cancelButton.setScaleY(1.02f);
            } else {
                cancelButton.setBackgroundColor(Color.parseColor("#F44336")); // Normal red
                cancelButton.setScaleX(1.0f);
                cancelButton.setScaleY(1.0f);
            }
        });

// Make buttons focusable for navigation
        loginButton.setFocusable(true);
        loginButton.setFocusableInTouchMode(true);
        cancelButton.setFocusable(true);
        cancelButton.setFocusableInTouchMode(true);

// Set navigation order
        passwordInput.setNextFocusDownId(loginButton.getId());
        loginButton.setNextFocusLeftId(cancelButton.getId());
        cancelButton.setNextFocusRightId(loginButton.getId());

        // Style buttons
        loginButton.setBackgroundColor(Color.parseColor("#4CAF50"));
        loginButton.setTextColor(Color.WHITE);
        cancelButton.setBackgroundColor(Color.parseColor("#F44336"));
        cancelButton.setTextColor(Color.WHITE);

        loginButton.setOnClickListener(v -> {
            String username = usernameInput.getText().toString().trim();
            String password = passwordInput.getText().toString();

            // Check against fixed admin credentials
            if (passwordManager.validateAdminLogin(username, password)) {
                dialog.dismiss();
                toggleEFragment();
            } else {
                Toast.makeText(this, "Invalid admin credentials", Toast.LENGTH_SHORT).show();
                passwordInput.setText("");
                passwordInput.requestFocus();
            }
        });

        // Enter key handler
        passwordInput.setOnKeyListener((v, keyCode, event) -> {
            if (event.getAction() == KeyEvent.ACTION_DOWN &&
                    (keyCode == KeyEvent.KEYCODE_ENTER || keyCode == KeyEvent.KEYCODE_NUMPAD_ENTER)) {
                loginButton.performClick();
                return true;
            }
            return false;
        });

        usernameInput.requestFocus();
    }

    private void showAdminLoginForMasterData() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Admin Login Required");
        builder.setMessage("Enter admin credentials to access Master Data");

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(50, 20, 50, 20);

        // Username field
        TextView usernameLabel = new TextView(this);
        usernameLabel.setText("Username:");
        usernameLabel.setTextSize(16);
        usernameLabel.setPadding(0, 10, 0, 5);
        layout.addView(usernameLabel);

        EditText usernameInput = new EditText(this);
        usernameInput.setHint("Enter admin username");
        usernameInput.setText("admin");
        usernameInput.setInputType(InputType.TYPE_CLASS_TEXT);
        usernameInput.setId(View.generateViewId());
        layout.addView(usernameInput);

        // Password field
        TextView passwordLabel = new TextView(this);
        passwordLabel.setText("Password:");
        passwordLabel.setTextSize(16);
        passwordLabel.setPadding(0, 20, 0, 5);
        layout.addView(passwordLabel);

        EditText passwordInput = new EditText(this);
        passwordInput.setHint("Enter admin password");
        passwordInput.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        passwordInput.setId(View.generateViewId());
        layout.addView(passwordInput);

        builder.setView(layout);

        builder.setPositiveButton("Login", null);
        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss());

        AlertDialog dialog = builder.create();
        dialog.show();

        Button loginButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
        Button cancelButton = dialog.getButton(AlertDialog.BUTTON_NEGATIVE);

        // After getting the button references
        loginButton.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                loginButton.setBackgroundColor(Color.parseColor("#2E7D32")); // Darker green
                loginButton.setScaleX(1.02f);
                loginButton.setScaleY(1.02f);
            } else {
                loginButton.setBackgroundColor(Color.parseColor("#4CAF50")); // Normal green
                loginButton.setScaleX(1.0f);
                loginButton.setScaleY(1.0f);
            }
        });

        cancelButton.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                cancelButton.setBackgroundColor(Color.parseColor("#C62828")); // Darker red
                cancelButton.setScaleX(1.02f);
                cancelButton.setScaleY(1.02f);
            } else {
                cancelButton.setBackgroundColor(Color.parseColor("#F44336")); // Normal red
                cancelButton.setScaleX(1.0f);
                cancelButton.setScaleY(1.0f);
            }
        });

// Make buttons focusable for navigation
        loginButton.setFocusable(true);
        loginButton.setFocusableInTouchMode(true);
        cancelButton.setFocusable(true);
        cancelButton.setFocusableInTouchMode(true);

// Set navigation order
        passwordInput.setNextFocusDownId(loginButton.getId());
        loginButton.setNextFocusLeftId(cancelButton.getId());
        cancelButton.setNextFocusRightId(loginButton.getId());

        // Style buttons
        loginButton.setBackgroundColor(Color.parseColor("#4CAF50"));
        loginButton.setTextColor(Color.WHITE);
        cancelButton.setBackgroundColor(Color.parseColor("#F44336"));
        cancelButton.setTextColor(Color.WHITE);

        loginButton.setOnClickListener(v -> {
            String username = usernameInput.getText().toString().trim();
            String password = passwordInput.getText().toString();

            // Check against fixed admin credentials
            if (passwordManager.validateAdminLogin(username, password)) {
                dialog.dismiss();
                openMasterDataDialog(); // Open master data after successful login
            } else {
                Toast.makeText(this, "Invalid admin credentials", Toast.LENGTH_SHORT).show();
                passwordInput.setText("");
                passwordInput.requestFocus();
            }
        });

        // Enter key handler
        passwordInput.setOnKeyListener((v, keyCode, event) -> {
            if (event.getAction() == KeyEvent.ACTION_DOWN &&
                    (keyCode == KeyEvent.KEYCODE_ENTER || keyCode == KeyEvent.KEYCODE_NUMPAD_ENTER)) {
                loginButton.performClick();
                return true;
            }
            return false;
        });

        usernameInput.requestFocus();
    }
    /**
     * Handle F2 key press - Admin login for Master Data access
     */


    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        // F10 for settings dialog (requires user login)
        if (keyCode == KeyEvent.KEYCODE_F10) {

            showAdminLoginForF10();
            return true;
        }
        if (keyCode == KeyEvent.KEYCODE_F6) {
            Log.d("MainActivity", "F6 pressed - Opening print type selection dialog");

            showAdminLoginForF6();
            return true; // Event handled
        }


        if (keyCode == KeyEvent.KEYCODE_F9) {
            if (passwordManager != null) {
                passwordManager.showAdminPasswordResetOptions();
            }
            return true;
        }


        return super.onKeyDown(keyCode, event);
    }

    /**
     * Handle F10 press - shows login if needed, then settings
     */

// Optional: Add setup security during first run
    // Optional: Add setup security during first run
    private void checkAndSetupSecurity() {
        if (passwordManager != null) {
            // Check if security questions are set
            boolean securityQuestionsSet = checkIfSecurityQuestionsSet();

            // If no security questions set and it's first time, show setup
            if (!securityQuestionsSet && isFirstTimeAppUse()) {
                new Handler(Looper.getMainLooper()).postDelayed(() -> {
                    passwordManager.showSetupSecurityDialog();
                }, 2000);
            }
        }
    }

    /**
     * Check if security questions are set
     */
    private boolean checkIfSecurityQuestionsSet() {
        SharedPreferences securityPrefs = getSharedPreferences("SecurityPrefs", MODE_PRIVATE);
        return securityPrefs.contains("security_question_1") &&
                securityPrefs.contains("security_answer_1");
    }

    /**
     * Check if this is first time app use
     */
    private boolean isFirstTimeAppUse() {
        SharedPreferences prefs = getSharedPreferences("AppPrefs", MODE_PRIVATE);
        boolean isFirstTime = prefs.getBoolean("is_first_time", true);

        if (isFirstTime) {
            prefs.edit().putBoolean("is_first_time", false).apply();
        }

        return isFirstTime;
    }
    private void showAdminLoginForF10() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Admin Login Required");
        builder.setMessage("Enter admin credentials to access Master Data");

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(50, 20, 50, 20);

        // Username field
        TextView usernameLabel = new TextView(this);
        usernameLabel.setText("Username:");
        usernameLabel.setTextSize(16);
        usernameLabel.setPadding(0, 10, 0, 5);
        layout.addView(usernameLabel);

        EditText usernameInput = new EditText(this);
        usernameInput.setHint("Enter admin username");
        usernameInput.setText("admin");
        usernameInput.setInputType(InputType.TYPE_CLASS_TEXT);
        usernameInput.setId(View.generateViewId());
        layout.addView(usernameInput);

        // Password field
        TextView passwordLabel = new TextView(this);
        passwordLabel.setText("Password:");
        passwordLabel.setTextSize(16);
        passwordLabel.setPadding(0, 20, 0, 5);
        layout.addView(passwordLabel);

        EditText passwordInput = new EditText(this);
        passwordInput.setHint("Enter admin password");
        passwordInput.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        passwordInput.setId(View.generateViewId());
        layout.addView(passwordInput);

        builder.setView(layout);

        builder.setPositiveButton("Login", null);
        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss());

        AlertDialog dialog = builder.create();
        dialog.show();

        Button loginButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
        Button cancelButton = dialog.getButton(AlertDialog.BUTTON_NEGATIVE);


        // After getting the button references
        loginButton.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                loginButton.setBackgroundColor(Color.parseColor("#2E7D32")); // Darker green
                loginButton.setScaleX(1.02f);
                loginButton.setScaleY(1.02f);
            } else {
                loginButton.setBackgroundColor(Color.parseColor("#4CAF50")); // Normal green
                loginButton.setScaleX(1.0f);
                loginButton.setScaleY(1.0f);
            }
        });

        cancelButton.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                cancelButton.setBackgroundColor(Color.parseColor("#C62828")); // Darker red
                cancelButton.setScaleX(1.02f);
                cancelButton.setScaleY(1.02f);
            } else {
                cancelButton.setBackgroundColor(Color.parseColor("#F44336")); // Normal red
                cancelButton.setScaleX(1.0f);
                cancelButton.setScaleY(1.0f);
            }
        });

// Make buttons focusable for navigation
        loginButton.setFocusable(true);
        loginButton.setFocusableInTouchMode(true);
        cancelButton.setFocusable(true);
        cancelButton.setFocusableInTouchMode(true);

// Set navigation order
        passwordInput.setNextFocusDownId(loginButton.getId());
        loginButton.setNextFocusLeftId(cancelButton.getId());
        cancelButton.setNextFocusRightId(loginButton.getId());


        // Style buttons
        loginButton.setBackgroundColor(Color.parseColor("#4CAF50"));
        loginButton.setTextColor(Color.WHITE);
        cancelButton.setBackgroundColor(Color.parseColor("#F44336"));
        cancelButton.setTextColor(Color.WHITE);

        loginButton.setOnClickListener(v -> {
            String username = usernameInput.getText().toString().trim();
            String password = passwordInput.getText().toString();

            // Check against fixed admin credentials
            if (passwordManager.validateAdminLogin(username, password)) {
                dialog.dismiss();
                showThreeFieldDialog();
            } else {
                Toast.makeText(this, "Invalid admin credentials", Toast.LENGTH_SHORT).show();
                passwordInput.setText("");
                passwordInput.requestFocus();
            }
        });

        // Enter key handler
        passwordInput.setOnKeyListener((v, keyCode, event) -> {
            if (event.getAction() == KeyEvent.ACTION_DOWN &&
                    (keyCode == KeyEvent.KEYCODE_ENTER || keyCode == KeyEvent.KEYCODE_NUMPAD_ENTER)) {
                loginButton.performClick();
                return true;
            }
            return false;
        });

        usernameInput.requestFocus();
    }

    private void showAdminLoginForF6() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Admin Login Required");
        builder.setMessage("Enter admin credentials to access Master Data");

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(50, 20, 50, 20);

        // Username field
        TextView usernameLabel = new TextView(this);
        usernameLabel.setText("Username:");
        usernameLabel.setTextSize(16);
        usernameLabel.setPadding(0, 10, 0, 5);
        layout.addView(usernameLabel);

        EditText usernameInput = new EditText(this);
        usernameInput.setHint("Enter admin username");
        usernameInput.setText("admin");
        usernameInput.setInputType(InputType.TYPE_CLASS_TEXT);
        usernameInput.setId(View.generateViewId());
        layout.addView(usernameInput);

        // Password field
        TextView passwordLabel = new TextView(this);
        passwordLabel.setText("Password:");
        passwordLabel.setTextSize(16);
        passwordLabel.setPadding(0, 20, 0, 5);
        layout.addView(passwordLabel);

        EditText passwordInput = new EditText(this);
        passwordInput.setHint("Enter admin password");
        passwordInput.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        passwordInput.setId(View.generateViewId());
        layout.addView(passwordInput);

        builder.setView(layout);

        builder.setPositiveButton("Login", null);
        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss());

        AlertDialog dialog = builder.create();
        dialog.show();

        Button loginButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
        Button cancelButton = dialog.getButton(AlertDialog.BUTTON_NEGATIVE);
// After getting the button references
        loginButton.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                loginButton.setBackgroundColor(Color.parseColor("#2E7D32")); // Darker green
                loginButton.setScaleX(1.02f);
                loginButton.setScaleY(1.02f);
            } else {
                loginButton.setBackgroundColor(Color.parseColor("#4CAF50")); // Normal green
                loginButton.setScaleX(1.0f);
                loginButton.setScaleY(1.0f);
            }
        });

        cancelButton.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                cancelButton.setBackgroundColor(Color.parseColor("#C62828")); // Darker red
                cancelButton.setScaleX(1.02f);
                cancelButton.setScaleY(1.02f);
            } else {
                cancelButton.setBackgroundColor(Color.parseColor("#F44336")); // Normal red
                cancelButton.setScaleX(1.0f);
                cancelButton.setScaleY(1.0f);
            }
        });

// Make buttons focusable for navigation
        loginButton.setFocusable(true);
        loginButton.setFocusableInTouchMode(true);
        cancelButton.setFocusable(true);
        cancelButton.setFocusableInTouchMode(true);

// Set navigation order
        passwordInput.setNextFocusDownId(loginButton.getId());
        loginButton.setNextFocusLeftId(cancelButton.getId());
        cancelButton.setNextFocusRightId(loginButton.getId());


        // Style buttons
        loginButton.setBackgroundColor(Color.parseColor("#4CAF50"));
        loginButton.setTextColor(Color.WHITE);
        cancelButton.setBackgroundColor(Color.parseColor("#F44336"));
        cancelButton.setTextColor(Color.WHITE);

        loginButton.setOnClickListener(v -> {
            String username = usernameInput.getText().toString().trim();
            String password = passwordInput.getText().toString();

            // Check against fixed admin credentials
            if (passwordManager.validateAdminLogin(username, password)) {
                dialog.dismiss();
                showPrintTypeSelectionDialog();
            } else {
                Toast.makeText(this, "Invalid admin credentials", Toast.LENGTH_SHORT).show();
                passwordInput.setText("");
                passwordInput.requestFocus();
            }
        });

        // Enter key handler
        passwordInput.setOnKeyListener((v, keyCode, event) -> {
            if (event.getAction() == KeyEvent.ACTION_DOWN &&
                    (keyCode == KeyEvent.KEYCODE_ENTER || keyCode == KeyEvent.KEYCODE_NUMPAD_ENTER)) {
                loginButton.performClick();
                return true;
            }
            return false;
        });

        usernameInput.requestFocus();
    }
    // In your activity, check admin credentials
    private void checkAdminLogin(String username, String password) {
        if (passwordManager.validateAdminLogin(username, password)) {
            // Admin login successful
            showPrintTypeSelectionDialog();
        } else {
            Toast.makeText(this, "Invalid admin credentials", Toast.LENGTH_SHORT).show();
        }
    }
    /**
     * Show dialog with login/register options
     */
    private void showLoginOrRegisterDialogF1() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Access Required");
        builder.setMessage("You need to login to access F10 settings");

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(50, 20, 50, 20);

        TextView infoText = new TextView(this);
        infoText.setText("Choose an option:");
        infoText.setTextSize(16);
        infoText.setPadding(0, 0, 0, 20);
        layout.addView(infoText);

        builder.setView(layout);

        builder.setPositiveButton("Login", (dialog, which) -> {
            showLoginDialogF1();
        });

        builder.setNegativeButton("Register", (dialog, which) -> {
            passwordManager.showRegistrationDialog();
        });

        builder.setNeutralButton("Cancel", null);

        AlertDialog dialog = builder.create();
        dialog.show();

        Button loginButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
        Button registerButton = dialog.getButton(AlertDialog.BUTTON_NEGATIVE);
        Button cancelButton = dialog.getButton(AlertDialog.BUTTON_NEUTRAL);

        // Style buttons
        loginButton.setBackgroundColor(getColor(android.R.color.holo_green_dark));
        loginButton.setTextColor(android.graphics.Color.WHITE);

        registerButton.setBackgroundColor(getColor(android.R.color.holo_orange_dark));
        registerButton.setTextColor(android.graphics.Color.WHITE);

        cancelButton.setBackgroundColor(getColor(android.R.color.darker_gray));
        cancelButton.setTextColor(android.graphics.Color.WHITE);
    }

    private void showLoginOrRegisterDialog1() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Access Required");


        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(50, 20, 50, 20);

        TextView infoText = new TextView(this);
        infoText.setText("Choose an option:");
        infoText.setTextSize(16);
        infoText.setPadding(0, 0, 0, 20);
        layout.addView(infoText);

        builder.setView(layout);

        builder.setPositiveButton("Login", (dialog, which) -> {
            showLoginDialog1();
        });

        builder.setNegativeButton("Register", (dialog, which) -> {
            passwordManager.showRegistrationDialog();
        });

        builder.setNeutralButton("Cancel", null);

        AlertDialog dialog = builder.create();
        dialog.show();

        Button loginButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
        Button registerButton = dialog.getButton(AlertDialog.BUTTON_NEGATIVE);
        Button cancelButton = dialog.getButton(AlertDialog.BUTTON_NEUTRAL);

        // Style buttons
        loginButton.setBackgroundColor(getColor(android.R.color.holo_green_dark));
        loginButton.setTextColor(android.graphics.Color.WHITE);

        registerButton.setBackgroundColor(getColor(android.R.color.holo_orange_dark));
        registerButton.setTextColor(android.graphics.Color.WHITE);

        cancelButton.setBackgroundColor(getColor(android.R.color.darker_gray));
        cancelButton.setTextColor(android.graphics.Color.WHITE);
    }
    private void showLoginOrRegisterDialog2() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Access Required");


        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(50, 20, 50, 20);

        TextView infoText = new TextView(this);
        infoText.setText("Choose an option:");
        infoText.setTextSize(16);
        infoText.setPadding(0, 0, 0, 20);
        layout.addView(infoText);

        builder.setView(layout);

        builder.setPositiveButton("Login", (dialog, which) -> {
            showLoginDialog2();
        });

        builder.setNegativeButton("Register", (dialog, which) -> {
            passwordManager.showRegistrationDialog();
        });

        builder.setNeutralButton("Cancel", null);

        AlertDialog dialog = builder.create();
        dialog.show();

        Button loginButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
        Button registerButton = dialog.getButton(AlertDialog.BUTTON_NEGATIVE);
        Button cancelButton = dialog.getButton(AlertDialog.BUTTON_NEUTRAL);

        // Style buttons
        loginButton.setBackgroundColor(getColor(android.R.color.holo_green_dark));
        loginButton.setTextColor(android.graphics.Color.WHITE);

        registerButton.setBackgroundColor(getColor(android.R.color.holo_orange_dark));
        registerButton.setTextColor(android.graphics.Color.WHITE);

        cancelButton.setBackgroundColor(getColor(android.R.color.darker_gray));
        cancelButton.setTextColor(android.graphics.Color.WHITE);
    }
    public void showLoginDialogF1() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("User Login");
        builder.setMessage("Please enter your credentials");

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(50, 20, 50, 20);

        // Username field
        TextView usernameLabel = new TextView(this);
        usernameLabel.setText("Username:");
        usernameLabel.setTextSize(16);
        usernameLabel.setPadding(0, 10, 0, 5);
        layout.addView(usernameLabel);

        EditText usernameInput = new EditText(this);
        usernameInput.setHint("Enter username");
        usernameInput.setInputType(InputType.TYPE_CLASS_TEXT);
        usernameInput.setId(View.generateViewId());
        layout.addView(usernameInput);

        // Password field
        TextView passwordLabel = new TextView(this);
        passwordLabel.setText("Password:");
        passwordLabel.setTextSize(16);
        passwordLabel.setPadding(0, 20, 0, 5);
        layout.addView(passwordLabel);

        EditText passwordInput = new EditText(this);
        passwordInput.setHint("Enter password");
        passwordInput.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        passwordInput.setId(View.generateViewId());
        layout.addView(passwordInput);

        // Remember me
        CheckBox rememberMe = new CheckBox(this);
        rememberMe.setText("Remember me for 8 hours");
        rememberMe.setPadding(0, 20, 0, 10);
        layout.addView(rememberMe);

        // Register link
        TextView registerLink = new TextView(this);
        registerLink.setText("Don't have an account? Register");
        registerLink.setTextColor(Color.BLUE);
        registerLink.setPadding(0, 10, 0, 10);
        layout.addView(registerLink);

        builder.setView(layout);

        builder.setPositiveButton("Login", null);
        builder.setNegativeButton("Cancel", (dialogInterface, which) -> dialogInterface.dismiss());

        AlertDialog dialog = builder.create();
        dialog.show();

        // Set register link click listener after dialog is created
        registerLink.setOnClickListener(v -> {
            dialog.dismiss();
            if (passwordManager != null) {
                passwordManager.showRegistrationDialog();
            }
        });

        Button loginButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
        Button cancelButton = dialog.getButton(AlertDialog.BUTTON_NEGATIVE);

        styleButton(loginButton, "Login", "#4CAF50");
        styleButton(cancelButton, "Cancel", "#F44336");

        loginButton.setOnClickListener(v -> {
            String username = usernameInput.getText().toString().trim();
            String password = passwordInput.getText().toString();

            if (username.isEmpty()) {
                Toast.makeText(this, "Please enter username", Toast.LENGTH_SHORT).show();
                return;
            }

            if (password.isEmpty()) {
                Toast.makeText(this, "Please enter password", Toast.LENGTH_SHORT).show();
                return;
            }

            if (validateUserLogin(username, password)) {
                if (rememberMe.isChecked()) {
                    // Save session with timestamp
                    getSharedPreferences("PasswordManager", MODE_PRIVATE)
                            .edit()
                            .putString("logged_in_user", username)
                            .putLong("login_time", System.currentTimeMillis())
                            .apply();
                }

                Toast.makeText(this, "Login successful! Welcome " + username, Toast.LENGTH_SHORT).show();
                dialog.dismiss();

                // Handle successful login - open appropriate fragment
                isUserLoggedIn = true;
                currentUser = username;
                isAdminMode = false;

                // You can open a default fragment here
                toggleDFragment();

            } else {
                Toast.makeText(this, "Invalid username or password", Toast.LENGTH_SHORT).show();
                passwordInput.setText("");
                passwordInput.requestFocus();
            }
        });

        // Enter key handler
        passwordInput.setOnKeyListener((v, keyCode, event) -> {
            if (event.getAction() == KeyEvent.ACTION_DOWN &&
                    (keyCode == KeyEvent.KEYCODE_ENTER || keyCode == KeyEvent.KEYCODE_NUMPAD_ENTER)) {
                loginButton.performClick();
                return true;
            }
            return false;
        });

        usernameInput.requestFocus();
    }
    public void showLoginDialog1() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("User Login");
        builder.setMessage("Please enter your credentials");

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(50, 20, 50, 20);

        // Username field
        TextView usernameLabel = new TextView(this);
        usernameLabel.setText("Username:");
        usernameLabel.setTextSize(16);
        usernameLabel.setPadding(0, 10, 0, 5);
        layout.addView(usernameLabel);

        EditText usernameInput = new EditText(this);
        usernameInput.setHint("Enter username");
        usernameInput.setInputType(InputType.TYPE_CLASS_TEXT);
        usernameInput.setId(View.generateViewId());
        layout.addView(usernameInput);

        // Password field
        TextView passwordLabel = new TextView(this);
        passwordLabel.setText("Password:");
        passwordLabel.setTextSize(16);
        passwordLabel.setPadding(0, 20, 0, 5);
        layout.addView(passwordLabel);

        EditText passwordInput = new EditText(this);
        passwordInput.setHint("Enter password");
        passwordInput.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        passwordInput.setId(View.generateViewId());
        layout.addView(passwordInput);

        // Remember me
        CheckBox rememberMe = new CheckBox(this);
        rememberMe.setText("Remember me for 8 hours");
        rememberMe.setPadding(0, 20, 0, 10);
        layout.addView(rememberMe);

        // Register link
        TextView registerLink = new TextView(this);
        registerLink.setText("Don't have an account? Register");
        registerLink.setTextColor(Color.BLUE);
        registerLink.setPadding(0, 10, 0, 10);
        layout.addView(registerLink);

        builder.setView(layout);

        builder.setPositiveButton("Login", null);
        builder.setNegativeButton("Cancel", (dialogInterface, which) -> dialogInterface.dismiss());

        AlertDialog dialog = builder.create();
        dialog.show();

        // Set register link click listener after dialog is created
        registerLink.setOnClickListener(v -> {
            dialog.dismiss();
            if (passwordManager != null) {
                passwordManager.showRegistrationDialog();
            }
        });

        Button loginButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
        Button cancelButton = dialog.getButton(AlertDialog.BUTTON_NEGATIVE);

        styleButton(loginButton, "Login", "#4CAF50");
        styleButton(cancelButton, "Cancel", "#F44336");

        loginButton.setOnClickListener(v -> {
            String username = usernameInput.getText().toString().trim();
            String password = passwordInput.getText().toString();

            if (username.isEmpty()) {
                Toast.makeText(this, "Please enter username", Toast.LENGTH_SHORT).show();
                return;
            }

            if (password.isEmpty()) {
                Toast.makeText(this, "Please enter password", Toast.LENGTH_SHORT).show();
                return;
            }

            if (validateUserLogin(username, password)) {
                if (rememberMe.isChecked()) {
                    // Save session with timestamp
                    getSharedPreferences("PasswordManager", MODE_PRIVATE)
                            .edit()
                            .putString("logged_in_user", username)
                            .putLong("login_time", System.currentTimeMillis())
                            .apply();
                }

                Toast.makeText(this, "Login successful! Welcome " + username, Toast.LENGTH_SHORT).show();
                dialog.dismiss();

                // Handle successful login - open appropriate fragment
                isUserLoggedIn = true;
                currentUser = username;
                isAdminMode = false;

                // You can open a default fragment here
                toggleFragment(buttonA, aFragment, FragmentState.FRAGMENT_A, "#FFA500");

            } else {
                Toast.makeText(this, "Invalid username or password", Toast.LENGTH_SHORT).show();
                passwordInput.setText("");
                passwordInput.requestFocus();
            }
        });

        // Enter key handler
        passwordInput.setOnKeyListener((v, keyCode, event) -> {
            if (event.getAction() == KeyEvent.ACTION_DOWN &&
                    (keyCode == KeyEvent.KEYCODE_ENTER || keyCode == KeyEvent.KEYCODE_NUMPAD_ENTER)) {
                loginButton.performClick();
                return true;
            }
            return false;
        });

        usernameInput.requestFocus();
    }
    public void showLoginDialog2() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("User Login");
        builder.setMessage("Please enter your credentials");

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(50, 20, 50, 20);

        // Username field
        TextView usernameLabel = new TextView(this);
        usernameLabel.setText("Username:");
        usernameLabel.setTextSize(16);
        usernameLabel.setPadding(0, 10, 0, 5);
        layout.addView(usernameLabel);

        EditText usernameInput = new EditText(this);
        usernameInput.setHint("Enter username");
        usernameInput.setInputType(InputType.TYPE_CLASS_TEXT);
        usernameInput.setId(View.generateViewId());
        layout.addView(usernameInput);

        // Password field
        TextView passwordLabel = new TextView(this);
        passwordLabel.setText("Password:");
        passwordLabel.setTextSize(16);
        passwordLabel.setPadding(0, 20, 0, 5);
        layout.addView(passwordLabel);

        EditText passwordInput = new EditText(this);
        passwordInput.setHint("Enter password");
        passwordInput.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        passwordInput.setId(View.generateViewId());
        layout.addView(passwordInput);

        // Remember me
        CheckBox rememberMe = new CheckBox(this);
        rememberMe.setText("Remember me for 8 hours");
        rememberMe.setPadding(0, 20, 0, 10);
        layout.addView(rememberMe);

        // Register link
        TextView registerLink = new TextView(this);
        registerLink.setText("Don't have an account? Register");
        registerLink.setTextColor(Color.BLUE);
        registerLink.setPadding(0, 10, 0, 10);
        layout.addView(registerLink);

        builder.setView(layout);

        builder.setPositiveButton("Login", null);
        builder.setNegativeButton("Cancel", (dialogInterface, which) -> dialogInterface.dismiss());

        AlertDialog dialog = builder.create();
        dialog.show();

        // Set register link click listener after dialog is created
        registerLink.setOnClickListener(v -> {
            dialog.dismiss();
            if (passwordManager != null) {
                passwordManager.showRegistrationDialog();
            }
        });

        Button loginButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
        Button cancelButton = dialog.getButton(AlertDialog.BUTTON_NEGATIVE);

        styleButton(loginButton, "Login", "#4CAF50");
        styleButton(cancelButton, "Cancel", "#F44336");

        loginButton.setOnClickListener(v -> {
            String username = usernameInput.getText().toString().trim();
            String password = passwordInput.getText().toString();

            if (username.isEmpty()) {
                Toast.makeText(this, "Please enter username", Toast.LENGTH_SHORT).show();
                return;
            }

            if (password.isEmpty()) {
                Toast.makeText(this, "Please enter password", Toast.LENGTH_SHORT).show();
                return;
            }

            if (validateUserLogin(username, password)) {
                if (rememberMe.isChecked()) {
                    // Save session with timestamp
                    getSharedPreferences("PasswordManager", MODE_PRIVATE)
                            .edit()
                            .putString("logged_in_user", username)
                            .putLong("login_time", System.currentTimeMillis())
                            .apply();
                }

                Toast.makeText(this, "Login successful! Welcome " + username, Toast.LENGTH_SHORT).show();
                dialog.dismiss();

                // Handle successful login - open appropriate fragment
                isUserLoggedIn = true;
                currentUser = username;
                isAdminMode = false;

                // You can open a default fragment here
                toggleFragment(buttonB, bFragment, FragmentState.FRAGMENT_B, "#00FF00");

            } else {
                Toast.makeText(this, "Invalid username or password", Toast.LENGTH_SHORT).show();
                passwordInput.setText("");
                passwordInput.requestFocus();
            }
        });

        // Enter key handler
        passwordInput.setOnKeyListener((v, keyCode, event) -> {
            if (event.getAction() == KeyEvent.ACTION_DOWN &&
                    (keyCode == KeyEvent.KEYCODE_ENTER || keyCode == KeyEvent.KEYCODE_NUMPAD_ENTER)) {
                loginButton.performClick();
                return true;
            }
            return false;
        });

        usernameInput.requestFocus();
    }

    /**
     * Validate user login against approved credentials
     */
    private boolean validateUserLogin(String username, String password) {
        SharedPreferences prefs = getSharedPreferences("PasswordManager", MODE_PRIVATE);
        String approvedUser = prefs.getString("username", null);
        String savedHash = prefs.getString("user_password_hash", null);
        String salt = prefs.getString("user_password_salt", null);
        boolean isApproved = prefs.getBoolean("user_approved", false);

        if (approvedUser == null || !approvedUser.equals(username) || !isApproved) {
            return false;
        }

        if (savedHash == null || salt == null) {
            return false;
        }

        // You need to implement hashPassword method or use PasswordManager's method
        if (passwordManager != null) {
            String hash = passwordManager.hashPassword(password, salt);
            return hash.equals(savedHash);
        }

        return false;
    }

    /**
     * Style button helper method
     */
    private void styleButton(Button button, String text, String colorHex) {
        button.setText(text);
        button.setBackgroundColor(Color.parseColor(colorHex));
        button.setTextColor(Color.WHITE);
        button.setPadding(30, 15, 30, 15);
        button.setAllCaps(false);
        button.setTextSize(14);
    }
    /**
     * Show dialog with two options: Plain Print and Printed Print
     */
    private void showPrintTypeSelectionDialog() {
        // Get current saved print type
        SharedPreferences prefs = getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        String currentPrintType = prefs.getString(KEY_PRINT_TYPE, PRINT_TYPE_PLAIN);

        // Print type options for display
        String[] printTypes = {"Plain Print", "Printed Print"};

        // Find index of current selection
        int selectedIndex = currentPrintType.equals(PRINT_TYPE_PRINTED) ? 1 : 0;

        // Build and show the dialog
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Select Print Type");
        builder.setSingleChoiceItems(printTypes, selectedIndex, (dialog, which) -> {
            // Determine selected type
            String selectedType = which == 0 ? PRINT_TYPE_PLAIN : PRINT_TYPE_PRINTED;
            String displayName = printTypes[which];

            // Save to SharedPreferences
            savePrintTypePreference(selectedType, displayName);

            // Show confirmation
            String message = "Print type set to: " + displayName;
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
            Log.d("MainActivity", message);

            dialog.dismiss();
        });

        builder.setNegativeButton("Cancel", (dialog, which) -> {
            Toast.makeText(this, "Selection cancelled", Toast.LENGTH_SHORT).show();
            dialog.dismiss();
        });

        // Show the dialog
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    /**
     * Save print type preference to SharedPreferences
     */
    private void savePrintTypePreference(String printType, String displayName) {
        SharedPreferences prefs = getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(KEY_PRINT_TYPE, printType);
        editor.putString(KEY_PRINT_TYPE_DISPLAY, displayName);
        editor.apply();
    }

    /**
     * Get current print type
     * @return "plain" or "printed"
     */
    public String getCurrentPrintType() {
        SharedPreferences prefs = getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        return prefs.getString(KEY_PRINT_TYPE, PRINT_TYPE_PLAIN);
    }

    /**
     * Get current print type display name
     * @return "Plain Print" or "Printed Print"
     */
    public String getCurrentPrintTypeDisplay() {
        SharedPreferences prefs = getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        return prefs.getString(KEY_PRINT_TYPE_DISPLAY, "Plain Print");
    }

    /**
     * Check if Printed Print is selected
     * @return true if Printed Print, false for Plain Print
     */
    public boolean isPrintedPrintSelected() {
        return getCurrentPrintType().equals(PRINT_TYPE_PRINTED);
    }

    /**
     * Check if Plain Print is selected
     * @return true if Plain Print, false for Printed Print
     */
    public boolean isPlainPrintSelected() {
        return getCurrentPrintType().equals(PRINT_TYPE_PLAIN);
    }

    // Other existing methods...
    public void updatePrinterStatus(boolean connected, String printerName) {
        this.isPrinterConnected = connected;
       // this.connectedPrinterName = printerName;
    }

    private void showThreeFieldDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Enter Information");

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(50, 20, 50, 20);

        String[] fieldNames = {"Field 1", "Field 2", "Field 3"};
        EditText[] editTexts = new EditText[3];

        for (int i = 0; i < 3; i++) {
            TextView label = new TextView(this);
            label.setText(fieldNames[i] + ":");
            label.setTextSize(16);
            label.setPadding(0, 20, 0, 5);
            layout.addView(label);

            EditText input = new EditText(this);
            input.setHint("Enter " + fieldNames[i]);
            input.setInputType(InputType.TYPE_CLASS_TEXT);
            layout.addView(input);
            editTexts[i] = input;
        }

        SharedPreferences prefs = getSharedPreferences("MyAppPrefs", MODE_PRIVATE);
        for (int i = 0; i < 3; i++) {
            String savedValue = prefs.getString("field_" + i, "");
            editTexts[i].setText(savedValue);
        }

        builder.setView(layout);

        builder.setPositiveButton("Save", null); // Set later to get button reference
        builder.setNegativeButton("Cancel", null); // Set later to get button reference

        AlertDialog dialog = builder.create();
        dialog.show();

        // Get the buttons from the dialog
        Button saveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
        Button cancelButton = dialog.getButton(AlertDialog.BUTTON_NEGATIVE);

        // Set IDs for focus navigation
        saveButton.setId(View.generateViewId());
        cancelButton.setId(View.generateViewId());

        // Create a parent layout for buttons to add spacing
        LinearLayout buttonLayout = new LinearLayout(this);
        buttonLayout.setOrientation(LinearLayout.HORIZONTAL);
        buttonLayout.setPadding(50, 10, 50, 20);

        // Remove buttons from dialog's default layout
        ViewGroup parent = (ViewGroup) saveButton.getParent();
        if (parent != null) {
            parent.removeView(saveButton);
            parent.removeView(cancelButton);
        }

        // Add buttons to custom layout with spacing
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.0f
        );
        params.setMargins(5, 0, 5, 0);

        saveButton.setLayoutParams(params);
        cancelButton.setLayoutParams(params);

        buttonLayout.addView(saveButton);
        buttonLayout.addView(cancelButton);

        // Add the button layout to the main layout
        layout.addView(buttonLayout);

        // Set initial button styles
        saveButton.setBackgroundColor(Color.LTGRAY);
        saveButton.setTextColor(Color.BLACK);
        cancelButton.setBackgroundColor(Color.LTGRAY);
        cancelButton.setTextColor(Color.BLACK);

        // Set padding for better appearance
        saveButton.setPadding(30, 15, 30, 15);
        cancelButton.setPadding(30, 15, 30, 15);

        // Set click listeners
        saveButton.setOnClickListener(v -> {
            SharedPreferences.Editor editor = prefs.edit();
            for (int i = 0; i < 3; i++) {
                String value = editTexts[i].getText().toString().trim();
                editor.putString("field_" + i, value);
            }
            editor.apply();
            Toast.makeText(this, "Data saved successfully!", Toast.LENGTH_SHORT).show();
            dialog.dismiss();
        });

        cancelButton.setOnClickListener(v -> dialog.dismiss());

        // Set focus change listener for Save button
        saveButton.setOnFocusChangeListener((v, hasFocus) -> {
            Button btn = (Button) v;
            if (hasFocus) {
                // Focused state
                btn.setBackgroundColor(Color.parseColor("#4CAF50")); // Green
                btn.setTextColor(Color.WHITE);
                btn.setPadding(35, 18, 35, 18); // Slightly larger when focused

                // Optional: Add elevation for focused state
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    btn.setElevation(10);
                }
            } else {
                // Normal state
                btn.setBackgroundColor(Color.LTGRAY);
                btn.setTextColor(Color.BLACK);
                btn.setPadding(30, 15, 30, 15);

                // Reset elevation
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    btn.setElevation(0);
                }
            }
        });

        // Set focus change listener for Cancel button
        cancelButton.setOnFocusChangeListener((v, hasFocus) -> {
            Button btn = (Button) v;
            if (hasFocus) {
                // Focused state
                btn.setBackgroundColor(Color.parseColor("#F44336")); // Red
                btn.setTextColor(Color.WHITE);
                btn.setPadding(35, 18, 35, 18); // Slightly larger when focused

                // Optional: Add elevation for focused state
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    btn.setElevation(10);
                }
            } else {
                // Normal state
                btn.setBackgroundColor(Color.LTGRAY);
                btn.setTextColor(Color.BLACK);
                btn.setPadding(30, 15, 30, 15);

                // Reset elevation
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    btn.setElevation(0);
                }
            }
        });

        // Optional: Add Enter key handling for buttons
        saveButton.setOnKeyListener((v, keyCode, event) -> {
            if (event.getAction() == KeyEvent.ACTION_DOWN) {
                if (keyCode == KeyEvent.KEYCODE_ENTER || keyCode == KeyEvent.KEYCODE_NUMPAD_ENTER) {
                    saveButton.performClick();
                    return true;
                }
            }
            return false;
        });

        cancelButton.setOnKeyListener((v, keyCode, event) -> {
            if (event.getAction() == KeyEvent.ACTION_DOWN) {
                if (keyCode == KeyEvent.KEYCODE_ENTER || keyCode == KeyEvent.KEYCODE_NUMPAD_ENTER) {
                    cancelButton.performClick();
                    return true;
                }
            }
            return false;
        });

        // Set focus navigation between buttons
        saveButton.setNextFocusLeftId(cancelButton.getId());
        saveButton.setNextFocusRightId(cancelButton.getId());
        saveButton.setNextFocusUpId(cancelButton.getId());
        saveButton.setNextFocusDownId(cancelButton.getId());

        cancelButton.setNextFocusLeftId(saveButton.getId());
        cancelButton.setNextFocusRightId(saveButton.getId());
        cancelButton.setNextFocusUpId(saveButton.getId());
        cancelButton.setNextFocusDownId(saveButton.getId());
    }

    private void toggleDFragment() {
        if (currentFragmentState == FragmentState.FRAGMENT_D) {
            hideFragmentAndShowCounter();
            Toast.makeText(this, "Search & Print View Closed", Toast.LENGTH_SHORT).show();
        } else {
            if (visibleFragment != null && visibleFragment != dFragment) {
                getSupportFragmentManager().beginTransaction()
                        .hide(visibleFragment)
                        .commitAllowingStateLoss();

                if (selectedButton != null) {
                    selectedButton.setSelected(false);
                    selectedButton = null;
                }
            }

            if (!dFragment.isAdded()) {
                getSupportFragmentManager().beginTransaction()
                        .add(R.id.fragment_container, dFragment, "FRAGMENT_D")
                        .commitAllowingStateLoss();
            } else {
                getSupportFragmentManager().beginTransaction()
                        .show(dFragment)
                        .commitAllowingStateLoss();
            }

            visibleFragment = dFragment;
            currentFragmentState = FragmentState.FRAGMENT_D;

            updateUIBasedOnState();
            if (txtCounter != null) {
                txtCounter.setTextColor(Color.parseColor("#FF5722"));
            }
            Toast.makeText(this, "Search & Print View Opened (F1)", Toast.LENGTH_SHORT).show();
        }
    }

    private void openMasterDataDialog() {
        View currentFocus = getCurrentFocus();
        if (currentFocus instanceof EditText) {
            return;
        }

        MasterDataDialog dialog = new MasterDataDialog(this);
        dialog.show();
        Toast.makeText(this, "Master Data Settings (F2)", Toast.LENGTH_SHORT).show();
    }

    private View findNextFocusableView(View currentView, int direction) {
        if (currentView == null) return null;

        @SuppressLint("WrongConstant") View nextView = currentView.focusSearch(direction);

        while (nextView != null && !nextView.isFocusable()) {
            @SuppressLint("WrongConstant") View tempView = nextView.focusSearch(direction);
            if (tempView == null || tempView == nextView) break;
            nextView = tempView;
        }

        return nextView;
    }

    private View findNextEditText(View currentView, int direction) {
        if (currentView == null) return null;

        @SuppressLint("WrongConstant") View nextView = currentView.focusSearch(direction);

        while (nextView != null && !(nextView instanceof EditText)) {
            @SuppressLint("WrongConstant") View tempView = nextView.focusSearch(direction);
            if (tempView == null || tempView == nextView) break;
            nextView = tempView;
        }

        return (nextView instanceof EditText) ? nextView : null;
    }

    private EditText findFirstEditText() {
        View rootView = findViewById(android.R.id.content);
        if (rootView != null) {
            return findFirstEditTextInView(rootView);
        }
        return null;
    }

    private EditText findFirstEditTextInView(View view) {
        if (view instanceof EditText) {
            return (EditText) view;
        }

        if (view instanceof ViewGroup) {
            ViewGroup viewGroup = (ViewGroup) view;
            for (int i = 0; i < viewGroup.getChildCount(); i++) {
                View child = viewGroup.getChildAt(i);
                EditText editText = findFirstEditTextInView(child);
                if (editText != null) {
                    return editText;
                }
            }
        }

        return null;
    }

    private View findFirstFocusableView() {
        View rootView = findViewById(android.R.id.content);
        if (rootView != null) {
            return findFirstFocusableInView(rootView);
        }
        return null;
    }

    private View findFirstFocusableInView(View view) {
        if (view.isFocusable()) {
            return view;
        }

        if (view instanceof ViewGroup) {
            ViewGroup viewGroup = (ViewGroup) view;
            for (int i = 0; i < viewGroup.getChildCount(); i++) {
                View child = viewGroup.getChildAt(i);
                View focusable = findFirstFocusableInView(child);
                if (focusable != null) {
                    return focusable;
                }
            }
        }

        return null;
    }

    private void executeInBackground(Runnable task) {
        ensureExecutorRunning();
        try {
            backgroundExecutor.execute(task);
        } catch (Exception e) {
            Log.e("DeviceList", "Failed to execute task", e);
            initializeExecutorService();
            backgroundExecutor.execute(task);
        }
    }

    private void initializeUIAfterBackground() {
        if (mNavigationView != null) {
            mNavigationView.setNavigationItemSelectedListener(this);
        }

        Intent newint = getIntent();
        address = newint.getStringExtra(MainActivity.EXTRA_ADDRESS);
        info_address = newint.getStringExtra(MainActivity.EXTRA_INFO);

        if (address != null) {
            connectToDevice(address, info_address);
        } else {
            updateConnectionStatus("Disconnected", R.drawable.ic_bluetooth_disconnected);
        }
    }

    private void setupClock() {
        stopUnifiedTimer();
        startUnifiedTimer();
    }

    private void startUnifiedTimer() {
        if (unifiedScheduler == null || unifiedScheduler.isShutdown()) {
            unifiedScheduler = Executors.newSingleThreadScheduledExecutor();
        }

        stopUnifiedTimer();

        unifiedTimerTask = unifiedScheduler.scheduleWithFixedDelay(
                this::unifiedTimerUpdate,
                0,
                1000,
                TimeUnit.MILLISECONDS
        );

        isTimerScheduled.set(true);
        Log.d("Timer", "Unified timer started");
    }

    private void stopUnifiedTimer() {
        if (unifiedTimerTask != null) {
            unifiedTimerTask.cancel(true);
            unifiedTimerTask = null;
        }
        isTimerScheduled.set(false);
    }

    private boolean isBluetoothConnected() {
        return bluetoothManager != null && bluetoothManager.isConnected();
    }

    private void unifiedTimerUpdate() {
        long currentTime = System.currentTimeMillis();

        if (currentTime - lastClockUpdate >= 1000) {
            lastClockUpdate = currentTime;
            updateClock(currentTime);
        }
    }

    private void updateClock(long currentTime) {
        runOnUiThread(() -> {
            try {
                TextView clockView = findViewById(R.id.hk_date);
                TextView dayView = findViewById(R.id.hk_day);
                if (dayView != null) {
                    dayView.setVisibility(View.GONE);
                }
                TextView clockTimeView = findViewById(R.id.hk_time);

                if (clockView != null && clockTimeView != null) {
                    String datePattern = "dd MMM yyyy";
                    String timePattern = "hh:mm:ss";

                    SimpleDateFormat dateFormat = new SimpleDateFormat(datePattern, Locale.getDefault());
                    SimpleDateFormat timeFormat = new SimpleDateFormat(timePattern, Locale.getDefault());

                    Date currentDate = new Date(currentTime);

                    clockView.setText(dateFormat.format(currentDate));
                    clockTimeView.setText(timeFormat.format(currentDate));
                }
            } catch (Exception e) {
                Log.e("Timer", "Error updating clock", e);
            }
        });
    }

    private void handleIncomingIntent() {
        Intent intent = getIntent();
        String address = intent.getStringExtra(MainActivity.EXTRA_ADDRESS);
        String info_address = intent.getStringExtra(MainActivity.EXTRA_INFO);

        if (address != null) {
            connectToDevice(address, info_address);
        }
    }

    private void connectToDevice(String address, String info) {
        if (address == null || address.isEmpty()) {
            Log.e("Connect", "Invalid address");
            return;
        }

        if (isBluetoothConnected()) {
            if (address.equals(currentConnectionAddress)) {
                Log.d("Connect", "Already connected to this device");
                runOnUiThread(() -> updateConnectionUI(true));
                return;
            } else {
                cleanupConnection();
            }
        }

        long currentTime = System.currentTimeMillis();
        if (currentTime - lastConnectionAttemptTime < MIN_CONNECTION_INTERVAL_MS) {
            Log.d("Connect", "Connection attempt too frequent, skipping");
            return;
        }
        lastConnectionAttemptTime = currentTime;

        if (isConnecting.get()) {
            Log.d("Connect", "Already connecting, skipping");
            return;
        }

        stopReconnectionAttempts();

        isConnecting.set(true);
        this.currentConnectionAddress = address;
        this.currentConnectionInfo = info;

        if (!checkBluetoothPermissions(this)) {
            requestBluetoothPermissions(this, 1001);
            isConnecting.set(false);
            return;
        }

        uiHandler.post(() -> {
            if (progress == null) {
                progress = new ProgressDialog(this);
                progress.setMessage("Connecting...");
                progress.setCancelable(false);
            }

            if (!isFinishing() && !isDestroyed()) {
                progress.show();
            }
            updateConnectionStatus("Connecting...", R.drawable.ic_bluetooth_connecting);
        });

        executeInBackground(() -> {
            try {
                bluetoothManager.connect(address, info, this);
            } catch (Exception e) {
                Log.e("Connect", "Error during connection: " + e.getMessage());
                runOnUiThread(() -> {
                    if (progress != null && progress.isShowing()) {
                        try {
                            progress.dismiss();
                        } catch (Exception ex) { }
                    }
                    isConnecting.set(false);
                    updateConnectionUI(false);
                    Toast.makeText(MainActivity.this,
                            "Connection error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
            } finally {
                uiHandler.post(() -> {
                    if (progress != null && progress.isShowing()) {
                        try {
                            progress.dismiss();
                        } catch (Exception e) { }
                    }
                });
            }
        });
    }

    @Override
    public void onConnectionResult(int resultCode, String message) {
        uiHandler.post(() -> {
            isConnecting.set(false);

            switch (resultCode) {
                case BluetoothConnectionManager.CONNECTION_SUCCESS:
                    stopReconnectionAttempts();
                    isConnected.set(true);
                    currentConnectionAddress = this.currentConnectionAddress;
                    updateConnectionUI(true);
                    saveDeviceForAutoConnect();
                    Log.d("Connection", "Successfully connected to: " + currentConnectionAddress);
                    break;

                case BluetoothConnectionManager.CONNECTION_FAILED:
                    if (!isBluetoothConnected()) {
                        updateConnectionUI(false);
                        if (isAutoConnectEnabled.get() && !isReconnecting) {
                            startReconnectionAttempts();
                        }
                    }
                    break;
            }
        });
    }

    @Override
    public void onDataReceived(byte[] data) { }

    @Override
    public void onConnectionLost() {
        runOnUiThread(() -> {
            Log.d("Connection", "Connection lost");
            currentConnectionAddress = null;
            updateConnectionUI(false);

            if (isAutoConnectEnabled.get() && !isReconnecting) {
                new Handler(Looper.getMainLooper()).postDelayed(() -> {
                    startReconnectionAttempts();
                }, 2000);
            }
        });
    }

    @Override
    public void onSensorDataUpdated(String cutting) {
        runOnUiThread(() -> {
            if (txtCounter != null) {
                txtCounter.setText(cutting);
            }
        });
    }

    @Override
    public void onSettingsUpdated(int tempSet, int humidSet, int pressureSet) { }

    private void initializeBackgroundComponents() {
        myBluetooth = BluetoothAdapter.getDefaultAdapter();
    }

    private synchronized void initializeExecutorService() {
        if (backgroundExecutor == null || backgroundExecutor.isShutdown() || backgroundExecutor.isTerminated()) {
            backgroundExecutor = Executors.newSingleThreadExecutor();
            isExecutorShutdown = false;
            Log.d("DeviceList", "ExecutorService initialized");
        }
    }

    private synchronized void ensureExecutorRunning() {
        if (backgroundExecutor == null || backgroundExecutor.isShutdown() || backgroundExecutor.isTerminated()) {
            Log.w("DeviceList", "ExecutorService was terminated, recreating...");
            backgroundExecutor = Executors.newSingleThreadExecutor();
            isExecutorShutdown = false;
        }
    }

    private void saveDeviceForAutoConnect() {
        if (currentConnectionAddress != null && currentConnectionInfo != null) {
            lastDevicePrefs.edit()
                    .putString("address", currentConnectionAddress)
                    .putString("info", currentConnectionInfo)
                    .putLong("timestamp", System.currentTimeMillis())
                    .apply();

            Log.d("AutoConnect", "Device saved for auto-connect: " + currentConnectionAddress);
        }
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem menuItem) {
        int id = menuItem.getItemId();

        if (id == R.id.nav_exit) {
            BaseActivity.exitApplication(this);
        } else if (id == R.id.nav_home) {
            hideFragmentAndShowCounter();
        } else if (id == R.id.action_searchList) {
            ScanDevicesList();
        } else if (id == R.id.action_pairedList) {
            pairedDevicesList();
        } else if (id == R.id.action_disconnect) {
            executeInBackground(() -> {
                if (bluetoothManager != null) {
                    bluetoothManager.disconnect();
                }
                runOnUiThread(() -> updateConnectionUI(false));
            });
        } else if (id == R.id.action_toggle_auto_connect) {
            toggleAutoConnect();
        } else if (id == R.id.action_report) {
            toggleReportFragment();
        }

        drawerLayout.closeDrawer(GravityCompat.START);
        return true;
    }

    private void toggleAutoConnect() {
        boolean newState = !isAutoConnectEnabled.get();
        isAutoConnectEnabled.set(newState);

        autoConnectPrefs.edit()
                .putBoolean(PREF_AUTO_CONNECT, newState)
                .apply();

        invalidateOptionsMenu();

        String message = newState ? "Auto-connect enabled" : "Auto-connect disabled";
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();

        if (newState && !isConnected.get()) {
            cleanupConnection();
            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                performDelayedAutoConnect();
            }, 2000);
        } else if (!newState) {
            stopReconnectionAttempts();
            if (isConnected.get()) {
                executeInBackground(() -> {
                    if (bluetoothManager != null) {
                        bluetoothManager.disconnect();
                    }
                });
            }
        }
    }

    private void setupAutoStart() {
        if (checkSelfPermission(android.Manifest.permission.RECEIVE_BOOT_COMPLETED)
                != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{android.Manifest.permission.RECEIVE_BOOT_COMPLETED}, 100);
        }

        if (!AutoStartPermissionHelper.isAutoStartPermissionEnabled(this)) {
            new AlertDialog.Builder(this)
                    .setTitle("Enable Auto Start")
                    .setMessage("Please enable auto-start permission for this app to work after device restart")
                    .setPositiveButton("Open Settings", (dialog, which) -> {
                        AutoStartPermissionHelper.requestAutoStartPermission(this);
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!AutoStartPermissionHelper.isBatteryOptimizationDisabled(this)) {
                AutoStartPermissionHelper.requestDisableBatteryOptimization(this);
            }
        }
    }

    public void exitApplication() {
        final AlertDialog.Builder adb = new AlertDialog.Builder(this);
        adb.setMessage("Are you sure you want to exit application?");
        adb.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                cleanupConnection();
                stopReconnectionAttempts();
                dialog.dismiss();
                finish();
            }
        });
        adb.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });
        adb.setNeutralButton("Rate", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                final String appPackageName = getPackageName();
                try {
                    startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=" + getPackageName())));
                } catch (ActivityNotFoundException anfe) {
                    startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("http://play.google.com/store/apps/details?id=" + getPackageName())));
                }
            }
        });
        AlertDialog alert = adb.create();
        alert.show();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_device_list, menu);

        if (printerMenuHelper != null) {
            printerMenuHelper.createPrinterMenu(menu);
        }

        MenuItem testPrintItem = menu.add(0, 9999, 200, "🔧 Test Printer");
        testPrintItem.setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);

        MenuItem autoConnectItem = menu.findItem(R.id.action_toggle_auto_connect);
        if (autoConnectItem != null) {
            autoConnectItem.setTitle(isAutoConnectEnabled.get() ?
                    "Disable Auto-Connect" : "Enable Auto-Connect");
        }
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        if (printerMenuHelper != null) {
            printerMenuHelper.updateMenuItems(menu);
        }
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (printerMenuHelper != null && printerMenuHelper.handlePrinterMenuItem(item)) {
            return true;
        }

        PrinterHelper helper = new PrinterHelper(this);

        switch (item.getItemId()) {
            case 1001:
                helper.checkUsbStatus();
                return true;
            case 1002:
                if (usbPrinterManager != null) {
                    List<UsbDevice> printers = usbPrinterManager.getAvailablePrinters();
                    if (!printers.isEmpty() && !isUsbConnected.get() && !isUsbConnecting.get()) {
                        usbPrinterManager.connectToPrinter(printers.get(0));
                    } else if (isUsbConnected.get()) {
                        Toast.makeText(this, "Printer already connected", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(this, "No printers found", Toast.LENGTH_SHORT).show();
                    }
                }
                return true;
            case 1003:
                return true;
            case 1004:
                if (helper != null && isPrinterConnected) {
                    helper.printTicket("TEST123", "1000", "500", "500");
                } else {
                    Toast.makeText(this, "Printer not connected", Toast.LENGTH_SHORT).show();
                }
                return true;
            case 1005:
                if (permissionHelper != null && !isUsbPermissionRequested.get()) {
                    isUsbPermissionRequested.set(true);
                    permissionHelper.showPermissionTroubleshooting();
                }
                return true;
            case 1006:
                PrintHelper helper1 = new PrintHelper(this);
                helper1.showAvailablePrinters();
                return true;
        }

        int id = item.getItemId();
        if (id == R.id.action_disconnect) {
            executeInBackground(() -> {
                if (bluetoothManager != null) {
                    bluetoothManager.disconnect();
                }
                runOnUiThread(() -> updateConnectionUI(false));
            });
            return true;
        } else if (id == R.id.action_searchList) {
            ScanDevicesList();
            return true;
        } else if (id == R.id.action_pairedList) {
            pairedDevicesList();
            return true;
        } else if (id == R.id.action_toggle_auto_connect) {
            toggleAutoConnect();
            return true;
        } else if (id == R.id.action_report) {
            toggleReportFragment();
            return true;
        }

        if (item.getItemId() == 9999) {
            checkPrinterConnection();
            if (isPrinterConnected) {
                testPrinterDirectly();
            } else {
                Toast.makeText(this, "Printer not connected", Toast.LENGTH_SHORT).show();
            }
            return true;
        }

        if (actionBarDrawerToggle != null && actionBarDrawerToggle.onOptionsItemSelected(item)) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void ScanDevicesList() {
        Intent intent = new Intent(this, ScanActivity.class);
        pendingScanIntent = intent;
        isScanPending = true;
        checkPermissionsAndStart();
    }

    private void checkPermissionsAndStart() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                        PERMISSION_REQUEST_CODE);
                return;
            }
        }
        startPendingActivity();
    }

    private void checkPrinterConnection() {
        if (printerManager == null) return;

        boolean connected = printerManager.isPrinterConnected();
        String printerName = printerManager.getConnectedPrinterName();
        UsbPrinterHelper.PrinterType type = printerManager.getCurrentPrinterType();

        Log.d("PrinterCheck", "Printer connected: " + connected);
        Log.d("PrinterCheck", "Printer name: " + printerName);
        Log.d("PrinterCheck", "Printer type: " + type);

        Toast.makeText(this,
                "Printer: " + (connected ? "✅ Connected" : "❌ Disconnected") +
                        "\nName: " + printerName +
                        "\nType: " + type,
                Toast.LENGTH_LONG).show();
    }

    private void startPendingActivity() {
        if (pendingScanIntent != null && isScanPending) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                try {
                    ActivityOptions options = ActivityOptions.makeSceneTransitionAnimation(this);
                    startActivityForResult(pendingScanIntent, SCAN_ACTIVITY_REQUEST_CODE, options.toBundle());
                } catch (Exception e) {
                    startActivityForResult(pendingScanIntent, SCAN_ACTIVITY_REQUEST_CODE);
                }
            } else {
                startActivityForResult(pendingScanIntent, SCAN_ACTIVITY_REQUEST_CODE);
            }
            pendingScanIntent = null;
            isScanPending = false;
        }
    }

    public void testPrinterDirectly() {
        if (!isPrinterConnected()) {
            Toast.makeText(this, "Printer not connected", Toast.LENGTH_SHORT).show();
            return;
        }

        Log.d("PrinterTest", "Testing printer directly...");

        String testText = "\n\n";
        testText += "================================\n";
        testText += "     PRINTER TEST\n";
        testText += "================================\n";
        testText += "Date: " + new SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault()).format(new Date()) + "\n";
        testText += "Printer: " + currentPrinterName + "\n";
        testText += "Type: " + (printerManager != null ? printerManager.getCurrentPrinterType() : "Unknown") + "\n";
        testText += "================================\n";
        testText += "This is a test print to verify\n";
        testText += "the USB printer connection.\n";
        testText += "================================\n\n\n\f";

        if (printerManager != null) {
            printerManager.addListener(new PrinterManager.PrinterConnectionAdapter() {
                @Override
                public void onPrintSuccess() {
                    runOnUiThread(() -> {
                        printerManager.removeListener(this);
                        Toast.makeText(MainActivity.this, "✅ Test print successful", Toast.LENGTH_SHORT).show();
                    });
                }

                @Override
                public void onPrintError(String error) {
                    runOnUiThread(() -> {
                        printerManager.removeListener(this);
                        Toast.makeText(MainActivity.this, "❌ Test print failed: " + error, Toast.LENGTH_LONG).show();
                    });
                }

                @Override
                public void onDebugInfo(String info) {
                    Log.d("PrinterTest", "Debug: " + info);
                }
            });

            boolean printed = printerManager.autoDetectAndPrint(testText);
            Log.d("PrinterTest", "autoDetectAndPrint result: " + printed);

            if (!printed) {
                printed = printerManager.printText(testText);
                Log.d("PrinterTest", "printText result: " + printed);
            }

            if (!printed) {
                printed = printerManager.printPlainText(testText);
                Log.d("PrinterTest", "printPlainText result: " + printed);
            }

            if (!printed) {
                Log.e("PrinterTest", "All print methods failed");
                if (printerManager.listeners != null && printerManager.listeners.size() > 0) {
                    printerManager.removeListener(printerManager.listeners.get(printerManager.listeners.size() - 1));
                }
            }
        }
    }

    private void pairedDevicesList() {
        executeInBackground(() -> {
            if (myBluetooth == null) {
                uiHandler.post(() ->
                        Toast.makeText(MainActivity.this, "Bluetooth not supported", Toast.LENGTH_LONG).show());
                return;
            }

            if (!myBluetooth.isEnabled()) {
                uiHandler.post(() -> {
                    Toast.makeText(MainActivity.this, "Please enable Bluetooth", Toast.LENGTH_LONG).show();
                    Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                    startActivity(enableBtIntent);
                });
                return;
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.BLUETOOTH_CONNECT)
                        != PackageManager.PERMISSION_GRANTED) {
                    uiHandler.post(() ->
                            Toast.makeText(MainActivity.this,
                                    "Bluetooth permission required for Android 12+",
                                    Toast.LENGTH_LONG).show());
                    return;
                }
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M &&
                    Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
                if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION)
                        != PackageManager.PERMISSION_GRANTED) {
                    uiHandler.post(() ->
                            Toast.makeText(MainActivity.this,
                                    "Location permission required for Bluetooth on Android 6-10",
                                    Toast.LENGTH_LONG).show());
                    return;
                }
            }

            try {
                Set<BluetoothDevice> pairedDevices = myBluetooth.getBondedDevices();
                ArrayList<String> list = new ArrayList<>();

                if (pairedDevices != null && pairedDevices.size() > 0) {
                    for (BluetoothDevice bt : pairedDevices) {
                        String deviceName = "Unknown Device";
                        String deviceAddress = bt.getAddress();

                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                            if (ActivityCompat.checkSelfPermission(this,
                                    Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED) {
                                deviceName = bt.getName() != null ? bt.getName() : "Unknown Device";
                            }
                        } else {
                            deviceName = bt.getName() != null ? bt.getName() : "Unknown Device";
                        }

                        list.add(deviceName + "\n" + deviceAddress);
                    }
                }

                uiHandler.post(() -> {
                    if (list.isEmpty()) {
                        Toast.makeText(getApplicationContext(),
                                "No Paired Bluetooth Devices Found.", Toast.LENGTH_LONG).show();
                        return;
                    }
                    showPairedDevicesDialog(list);
                });

            } catch (SecurityException e) {
                Log.e("Bluetooth", "Security exception: " + e.getMessage());
                uiHandler.post(() ->
                        Toast.makeText(MainActivity.this,
                                "Permission denied: " + e.getMessage(),
                                Toast.LENGTH_LONG).show());
            } catch (Exception e) {
                Log.e("Bluetooth", "Error getting paired devices: " + e.getMessage());
                uiHandler.post(() ->
                        Toast.makeText(MainActivity.this,
                                "Error: " + e.getMessage(),
                                Toast.LENGTH_LONG).show());
            }
        });
    }

    private void showPairedDevicesDialog(ArrayList<String> deviceList) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Select a paired device for connecting");

        LinearLayout parent = new LinearLayout(MainActivity.this);
        parent.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT));
        parent.setOrientation(LinearLayout.VERTICAL);

        ListView modeList = new ListView(this);

        final ArrayAdapter<String> modeAdapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_list_item_1,
                deviceList);
        modeList.setAdapter(modeAdapter);
        modeList.setOnItemClickListener(myListClickListener);

        builder.setView(modeList);
        AlertDialog dialog = builder.create();
        dialog.show();

        Window window = dialog.getWindow();
        if (window != null) {
            window.setLayout(ViewGroup.LayoutParams.WRAP_CONTENT, 600);
        }
    }

    private AdapterView.OnItemClickListener myListClickListener = new AdapterView.OnItemClickListener() {
        public void onItemClick(AdapterView<?> av, View v, int arg2, long arg3) {
            String info = ((TextView) v).getText().toString();
            String address = info.substring(info.length() - 17);

            if (checkBluetoothPermissions(MainActivity.this)) {
                cleanupConnection();
                new Handler(Looper.getMainLooper()).postDelayed(() -> {
                    connectToDevice(address, info);
                }, 1000);
            } else {
                requestBluetoothPermissions(MainActivity.this, 1001);
            }
        }
    };

    private final BroadcastReceiver bluetoothStatusReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if ("BLUETOOTH_CONNECTION_STATUS".equals(intent.getAction())) {
                int status = intent.getIntExtra("status", -1);
                String message = intent.getStringExtra("message");
                String deviceAddress = intent.getStringExtra("device");

                Log.d("MainActivity", "Received broadcast: " + status + " - " + message);
                runOnUiThread(() -> updateBluetoothUI(status, message));
            }
        }
    };

    @Override
    protected void onResume() {
        super.onResume();

        IntentFilter filter = new IntentFilter("BLUETOOTH_CONNECTION_STATUS");

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(bluetoothStatusReceiver, filter, Context.RECEIVER_NOT_EXPORTED);
        } else {
            registerReceiver(bluetoothStatusReceiver, filter);
        }

        // Check for USB devices with safeguards
        UsbManager usbManager = (UsbManager) getSystemService(Context.USB_SERVICE);
        if (usbManager != null) {
            HashMap<String, UsbDevice> deviceList = usbManager.getDeviceList();

            for (UsbDevice device : deviceList.values()) {
                if (isPrinterDevice(device)) {
                    if (!isUsbConnected.get() && !isUsbConnecting.get() && !isUsbPermissionRequested.get()) {
                        long currentTime = System.currentTimeMillis();
                        if (currentTime - lastUsbConnectionAttempt > USB_CONNECTION_COOLDOWN_MS) {
                            if (!usbManager.hasPermission(device)) {
                                isUsbPermissionRequested.set(true);
                                PendingIntent permissionIntent = PendingIntent.getBroadcast(this, 0,
                                        new Intent("com.googleapi.bluetoothweight.USB_PERMISSION"),
                                        PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
                                usbManager.requestPermission(device, permissionIntent);
                            }
                        }
                    }
                    break;
                }
            }
        }

        if (printerManager != null) {
            printerManager.registerReceiver();
        }

        Log.d("MainActivity", "Broadcast receiver registered successfully");

        if (!isAutoConnectAfterRebootAttempted) {
            isAutoConnectAfterRebootAttempted = true;
            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                performDelayedAutoConnect();
            }, 2000);
        }

        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            safeUsbAutoConnect();
        }, 2500);

        if (printerMenuHelper != null) {
            printerMenuHelper.handlePrinterConnectionAfterReboot();
        }
    }

    private boolean isPrinterDevice(UsbDevice device) {
        if (device == null) return false;
        if (device.getDeviceClass() == 7) return true;

        int[] printerVendors = {1046, 1208, 1193, 1008, 1118, 1305};
        for (int vendor : printerVendors) {
            if (device.getVendorId() == vendor) return true;
        }

        return false;
    }

    @Override
    protected void onPause() {
        super.onPause();

        try {
            unregisterReceiver(bluetoothStatusReceiver);
            Log.d("MainActivity", "Broadcast receiver unregistered");
        } catch (IllegalArgumentException e) {
            Log.w("MainActivity", "Receiver was not registered");
        }

        if (printerManager != null) {
            printerManager.unregisterReceiver();
        }
    }

    private void updateBluetoothUI(int status, String message) {
        Log.d("MainActivity", "updateBluetoothUI called with status: " + status);

        switch (status) {
            case BluetoothConnectionManager.CONNECTION_SUCCESS:
                updateConnectionUI(true);
                break;

            case BluetoothConnectionManager.CONNECTION_FAILED:
                updateConnectionUI(false);
                break;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopUnifiedTimer();
        if (unifiedScheduler != null) {
            unifiedScheduler.shutdown();
        }
        stopReconnectionAttempts();
        reconnectHandler.removeCallbacksAndMessages(null);

        cleanupConnection();

        if (bluetoothManager != null) {
            bluetoothManager.release();
        }
        if (printerMenuHelper != null) {
            printerMenuHelper.shutdown();
        }

        if (usbPermissionResultReceiver != null) {
            try {
                unregisterReceiver(usbPermissionResultReceiver);
            } catch (Exception e) {
                Log.e("MainActivity", "Error unregistering USB receiver: " + e.getMessage());
            }
        }

        if (printerManager != null) {
            printerManager.disconnectPrinter();
            printerManager.unregisterReceiver();
        }
    }
}