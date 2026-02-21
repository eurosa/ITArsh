package com.googleapi.bluetoothweight;

import static com.googleapi.bluetoothweight.bluetooth.BluetoothConnectionManager.checkBluetoothPermissions;
import static com.googleapi.bluetoothweight.bluetooth.BluetoothConnectionManager.requestBluetoothPermissions;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.ActivityOptions;
import android.app.AlertDialog;
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
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class MainActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener, BluetoothConnectionManager.ConnectionCallback {
    private Button buttonT, buttonG;

// In your onCreate method, after finding buttonA and buttonB

    private Fragment visibleFragment = null;
    private long lastClockUpdate = 0;
    private ScheduledFuture<?> unifiedTimerTask;
    private ScheduledExecutorService unifiedScheduler;

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

    // Key event handling flags
    private AtomicBoolean isProcessingKeyEvent = new AtomicBoolean(false);
    private long lastKeyEventTime = 0;
    private static final long KEY_EVENT_DEBOUNCE_MS = 500;

    // Auto-reconnect variables
    private static final int AUTO_RECONNECT_DELAY_MS = 8000; // Increased to 8 seconds
    private static final int MAX_RECONNECT_ATTEMPTS = 5; // Increased attempts
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
    private static final long MIN_CONNECTION_INTERVAL_MS = 15000; // Increased to 15 seconds

    // Track current state
    private enum FragmentState {
        NONE, FRAGMENT_A, FRAGMENT_B
    }
    private FragmentState currentFragmentState = FragmentState.NONE;

    // Add this to track if we're in the process of cleaning up
    private AtomicBoolean isCleaningUp = new AtomicBoolean(false);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        buttonT = findViewById(R.id.buttonT); // Make sure this ID exists in your layout
        buttonG = findViewById(R.id.buttonG); // Make sure this ID exists in your layout
        Drawable drawable = toolbar.getOverflowIcon();
        if (drawable != null) {
            drawable = DrawableCompat.wrap(drawable);
            DrawableCompat.setTint(drawable.mutate(), getResources().getColor(R.color.white));
            toolbar.setOverflowIcon(drawable);
        }
        connectionStatusIcon = findViewById(R.id.connectionStatus);
        connectionStatusTxt = findViewById(R.id.connectionStatusTxt);

        /***************************************************************************************
         * Navigation Drawer Layout
         *
         ***************************************************************************************/
        drawerLayout = findViewById(R.id.draw_layout);
        actionBarDrawerToggle = new ActionBarDrawerToggle(this, drawerLayout, R.string.nav_open, R.string.nav_close) {
            public void onDrawerClosed(View view) {
                super.onDrawerClosed(view);
            }

            public void onDrawerOpened(View drawerView) {
                super.onDrawerOpened(drawerView);
            }

            public void onDrawerStateChanged(int i) {
            }

            public void onDrawerSlide(View view, float v) {
            }
        };

        drawerLayout.addDrawerListener(actionBarDrawerToggle);
        actionBarDrawerToggle.syncState();
        actionBarDrawerToggle.getDrawerArrowDrawable().setColor(getResources().getColor(R.color.white));

        mNavigationView = findViewById(R.id.nav_view);
        mNavigationView.setNavigationItemSelectedListener(MainActivity.this);
        mNavigationView.setItemIconTintList(null);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setHomeButtonEnabled(true);

        bluetoothManager = new BluetoothConnectionManager(this);
        lastDevicePrefs = getSharedPreferences("last_device", MODE_PRIVATE);

        // Initialize auto-connect preferences
        autoConnectPrefs = getSharedPreferences("auto_connect_settings", MODE_PRIVATE);
        isAutoConnectEnabled.set(autoConnectPrefs.getBoolean(PREF_AUTO_CONNECT, true));

        // Initialize fragments
        aFragment = new AFragment();
        bFragment = new BFragment();

        // Initialize reconnect runnable
        initializeReconnectRunnable();

        executeInBackground(() -> {
            initializeBackgroundComponents();
            uiHandler.post(this::initializeUIAfterBackground);
        });

        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("");
        }

        buttonA = findViewById(R.id.buttonA);
        buttonB = findViewById(R.id.buttonB);
        txtCounter = findViewById(R.id.txtCounter);
        uNit = findViewById(R.id.uNit);
        abcLayout = findViewById(R.id.abc);
        fragmentContainer = findViewById(R.id.fragment_container);

        // Create a container for counter views to manage visibility together
        counterContainer = findViewById(R.id.counter_container);
        if (counterContainer == null) {
            // If no container exists, we'll manage individual views
            counterContainer = new View(this);
        }

        // Make sure counter is visible initially
        showCounterViews();

        // Setup button click listeners
        setupButtonClickListener(buttonA, aFragment, FragmentState.FRAGMENT_A, "#FFA500");
        setupButtonClickListener(buttonB, bFragment, FragmentState.FRAGMENT_B, "#00FF00");

        // Initial UI state
        updateUIBasedOnState();
        setupButtonFocusListeners();
        setupClock();
    }

    /**
     * Initialize the reconnection runnable with improved error handling
     */
    private void initializeReconnectRunnable() {
        reconnectRunnable = new Runnable() {
            @Override
            public void run() {
                if (isReconnecting && isAutoConnectEnabled.get() && !isConnected.get() && !isConnecting.get()) {
                    if (reconnectAttempts < MAX_RECONNECT_ATTEMPTS) {
                        Log.d("Reconnect", "Attempting to reconnect... Attempt " + (reconnectAttempts + 1));

                        // Get last connected device
                        String savedAddress = lastDevicePrefs.getString("address", "");
                        String savedInfo = lastDevicePrefs.getString("info", "");

                        if (!savedAddress.isEmpty()) {
                            // First, ensure any existing connection is properly cleaned up
                            cleanupConnection();

                            runOnUiThread(() -> {
                                Toast.makeText(MainActivity.this,
                                        "Attempting to reconnect... (" + (reconnectAttempts + 1) + "/" + MAX_RECONNECT_ATTEMPTS + ")",
                                        Toast.LENGTH_SHORT).show();
                                updateConnectionStatus("Reconnecting...", R.drawable.ic_bluetooth_connecting);
                            });

                            // Add a small delay before reconnecting to ensure cleanup
                            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                                // Attempt to reconnect
                                connectToDevice(savedAddress, savedInfo);
                            }, 1000);

                            reconnectAttempts++;
                        } else {
                            // No saved device, stop reconnecting
                            stopReconnectionAttempts();
                            runOnUiThread(() -> {
                                Toast.makeText(MainActivity.this,
                                        "No previously connected device found.",
                                        Toast.LENGTH_LONG).show();
                                updateConnectionStatus("Disconnected", R.drawable.ic_bluetooth_disconnected);
                            });
                        }
                    } else {
                        // Max attempts reached
                        stopReconnectionAttempts();
                        runOnUiThread(() -> {
                            Toast.makeText(MainActivity.this,
                                    "Max reconnection attempts reached. Please connect manually.",
                                    Toast.LENGTH_LONG).show();
                            updateConnectionStatus("Disconnected", R.drawable.ic_bluetooth_disconnected);
                        });
                    }
                }
            }
        };
    }

    /**
     * Clean up connection resources properly
     */
    private void cleanupConnection() {
        if (isCleaningUp.get()) return;

        isCleaningUp.set(true);
        try {
            if (bluetoothManager != null) {
                // First disconnect
                bluetoothManager.disconnect();

                // Small delay to ensure disconnect completes
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }

                // Force close any remaining connections
                bluetoothManager.forceCloseConnection();
            }
        } catch (Exception e) {
            Log.e("Cleanup", "Error during cleanup: " + e.getMessage());
        } finally {
            isCleaningUp.set(false);
        }
    }

    /**
     * Start reconnection attempts
     */
    private void startReconnectionAttempts() {
        if (!isReconnecting && isAutoConnectEnabled.get() && !isConnected.get()) {
            isReconnecting = true;
            reconnectAttempts = 0;
            Log.d("Reconnect", "Starting reconnection attempts");
            scheduleNextReconnectAttempt();
        }
    }

    /**
     * Stop reconnection attempts
     */
    private void stopReconnectionAttempts() {
        isReconnecting = false;
        reconnectAttempts = 0;
        reconnectHandler.removeCallbacks(reconnectRunnable);
        Log.d("Reconnect", "Stopped reconnection attempts");
    }

    /**
     * Schedule the next reconnect attempt
     */
    private void scheduleNextReconnectAttempt() {
        reconnectHandler.removeCallbacks(reconnectRunnable);
        reconnectHandler.postDelayed(reconnectRunnable, AUTO_RECONNECT_DELAY_MS);
    }

    /**
     * Update connection status UI
     */
    private void updateConnectionStatus(String status, int iconResId) {
        runOnUiThread(() -> {
            connectionStatusTxt.setText(status);
            connectionStatusIcon.setImageResource(iconResId);
        });
    }

    /**
     * Update connection UI based on connection state with improved handling
     */
    private void updateConnectionUI(boolean connected) {
        isConnected.set(connected);

        if (connected) {
            isConnecting.set(false);
            stopReconnectionAttempts();
            updateConnectionStatus("Connected", R.drawable.ic_bluetooth_connected);
        } else {
            updateConnectionStatus("Disconnected", R.drawable.ic_bluetooth_disconnected);

            // Clean up resources before attempting reconnection
            cleanupConnection();

            // Start reconnection attempts if auto-connect is enabled
            if (isAutoConnectEnabled.get()) {
                // Add a delay before starting reconnection attempts
                new Handler(Looper.getMainLooper()).postDelayed(() -> {
                    startReconnectionAttempts();
                }, 2000);
            }
        }
    }

    /**
     * Show counter and related views
     */
    private void showCounterViews() {
        txtCounter.setVisibility(View.VISIBLE);
        abcLayout.setVisibility(View.VISIBLE);
        uNit.setVisibility(View.VISIBLE);
        fragmentContainer.setVisibility(View.GONE);
        txtCounter.setTextColor(Color.parseColor("#FFFFFF"));
    }

    /**
     * Hide counter and related views
     */
    private void hideCounterViews() {
        txtCounter.setVisibility(View.GONE);
        abcLayout.setVisibility(View.GONE);
        uNit.setVisibility(View.GONE);
        fragmentContainer.setVisibility(View.VISIBLE);
    }

    /**
     * Update UI based on current fragment state
     */
    private void updateUIBasedOnState() {
        if (currentFragmentState == FragmentState.NONE) {
            showCounterViews();
            if (selectedButton != null) {
                selectedButton.setSelected(false);
                selectedButton = null;
            }
        } else {
            hideCounterViews();
            String colorHex = (currentFragmentState == FragmentState.FRAGMENT_A) ? "#FFA500" : "#00FF00";
            txtCounter.setTextColor(Color.parseColor(colorHex));
        }
    }

    /**
     * Hide fragment and show counter
     */
    private void hideFragmentAndShowCounter() {
        if (visibleFragment != null) {
            // Hide the current fragment
            getSupportFragmentManager().beginTransaction()
                    .hide(visibleFragment)
                    .commitAllowingStateLoss();

            visibleFragment = null;
            currentFragmentState = FragmentState.NONE;

            // Deselect the button
            if (selectedButton != null) {
                selectedButton.setSelected(false);
                selectedButton = null;
            }

            // Update UI
            updateUIBasedOnState();
        }
    }

    /**
     * Show fragment and hide counter
     */
    private void showFragmentAndHideCounter(Button button, Fragment fragment, FragmentState state, String colorHex) {
        // Hide current visible fragment if any
        if (visibleFragment != null && visibleFragment != fragment) {
            getSupportFragmentManager().beginTransaction()
                    .hide(visibleFragment)
                    .commitAllowingStateLoss();

            // Deselect previous button
            if (selectedButton != null) {
                selectedButton.setSelected(false);
            }
        }

        // Show new fragment
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

        // Update UI
        updateUIBasedOnState();

        button.setSelected(true);
        selectedButton = button;
    }

    /**
     * Toggle fragment visibility - for both button click and keyboard press
     */
    private void toggleFragment(Button button, Fragment fragment, FragmentState state, String colorHex) {
        // Debounce rapid key presses
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastKeyEventTime < KEY_EVENT_DEBOUNCE_MS) {
            return;
        }
        lastKeyEventTime = currentTime;

        // If clicking the same fragment that's visible, hide it
        if (currentFragmentState == state) {
            hideFragmentAndShowCounter();
        }
        // If clicking a different fragment or no fragment is visible, show it
        else {
            showFragmentAndHideCounter(button, fragment, state, colorHex);
        }
    }

    @Override
    public void onBackPressed() {
        // If drawer is open, close it first
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START);
        }
        // If a fragment is visible, hide it and show the counter
        else if (currentFragmentState != FragmentState.NONE) {
            hideFragmentAndShowCounter();
        }
        // If no fragment is visible and drawer is closed, exit normally
        else {
            super.onBackPressed();
        }
    }

    private void setupButtonClickListener(Button button, Fragment fragment, FragmentState state, String colorHex) {
        button.setOnClickListener(v -> toggleFragment(button, fragment, state, colorHex));
    }

    /**
     * Handle key events including keyboard keys
     */
    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        if (event.getAction() == KeyEvent.ACTION_DOWN) {
            int keyCode = event.getKeyCode();

            // Handle BACK button
            if (keyCode == KeyEvent.KEYCODE_BACK) {
                // If drawer is open, close it
                if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
                    drawerLayout.closeDrawer(GravityCompat.START);
                    return true;
                }
                // If fragment is visible, hide it
                else if (currentFragmentState != FragmentState.NONE) {
                    hideFragmentAndShowCounter();
                    return true;
                }
                return false; // Let system handle if nothing else
            }

            // Handle ESCAPE key (acts as back button on keyboards)
            if (keyCode == KeyEvent.KEYCODE_ESCAPE) {
                aFragment.performGButtonActionClear();
                // If drawer is open, close it
                if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
                    drawerLayout.closeDrawer(GravityCompat.START);
                    return true;
                }
                // If fragment is visible, hide it
                else if (currentFragmentState != FragmentState.NONE) {
                    hideFragmentAndShowCounter();
                    return true;
                }
                return true;
            }

            // Get current focused view
            View currentFocus = getCurrentFocus();

            // Check if current focus is an EditText
            boolean isEditTextFocused = currentFocus instanceof EditText;
            // Check if current focus is a Button (including AppCompatButton)
            boolean isButtonFocused = currentFocus instanceof Button ||
                    currentFocus instanceof androidx.appcompat.widget.AppCompatButton;

            // Handle TAB key - only for EditText navigation
            if (keyCode == KeyEvent.KEYCODE_TAB) {
                // Only process TAB if an EditText is focused
                if (isEditTextFocused) {
                    // Determine direction based on Shift key
                    int direction = event.isShiftPressed() ? View.FOCUS_BACKWARD : View.FOCUS_FORWARD;

                    // Try to find next EditText
                    View nextView = findNextEditText(currentFocus, direction);

                    if (nextView != null) {
                        nextView.requestFocus();
                        return true;
                    }
                }
                return false; // Let TAB perform its default behavior when no EditText is focused
            }

            // Handle ENTER key - for EditText and Button navigation
            if (keyCode == KeyEvent.KEYCODE_ENTER || keyCode == KeyEvent.KEYCODE_NUMPAD_ENTER) {
                // If EditText is focused
                if (isEditTextFocused) {
                    EditText currentEditText = (EditText) currentFocus;

                    // Check if this is a number-only EditText
                    int inputType = currentEditText.getInputType();
                    boolean isNumberEditText = (inputType & InputType.TYPE_CLASS_NUMBER) != 0;

                    if (isNumberEditText) {
                        // For number EditTexts, we want to prevent cursor hiding
                        // Try to find next focusable view
                        View nextView = findNextFocusableView(currentFocus, View.FOCUS_DOWN);

                        if (nextView != null) {
                            nextView.requestFocus();
                            return true;
                        } else {
                            // Try right
                            nextView = findNextFocusableView(currentFocus, View.FOCUS_RIGHT);
                            if (nextView != null) {
                                nextView.requestFocus();
                                return true;
                            }
                        }

                        // If no next view, consume the event to keep cursor
                        return true;
                    } else {
                        // For regular EditTexts, try to navigate
                        View nextView = findNextFocusableView(currentFocus, View.FOCUS_DOWN);

                        if (nextView != null) {
                            nextView.requestFocus();
                            return true;
                        } else {
                            nextView = findNextFocusableView(currentFocus, View.FOCUS_RIGHT);
                            if (nextView != null) {
                                nextView.requestFocus();
                                return true;
                            }
                        }

                        // If no next view, let default behavior happen
                        return false;
                    }
                }
                // If Button is focused
                else if (isButtonFocused) {
                    // Check which button is focused and call appropriate action
                    if (currentFocus.getId() == R.id.button4a) {
                        // Save button is focused
                        if (visibleFragment instanceof AFragment && visibleFragment.isVisible()) {
                            ((AFragment) visibleFragment).performSaveAction();
                            return true;
                        }
                    } else if (currentFocus.getId() == R.id.button5a) {
                        // Print button is focused
                        if (visibleFragment instanceof AFragment && visibleFragment.isVisible()) {
                            ((AFragment) visibleFragment).performPrintAction();
                            return true;
                        }
                    } else {
                        // For other buttons, try to find next focusable view
                        View nextView = findNextFocusableView(currentFocus, View.FOCUS_DOWN);

                        if (nextView != null) {
                            nextView.requestFocus();
                            return true;
                        } else {
                            // Try right
                            nextView = findNextFocusableView(currentFocus, View.FOCUS_RIGHT);
                            if (nextView != null) {
                                nextView.requestFocus();
                                return true;
                            }
                        }

                        // If no next view, perform button click
                        if (currentFocus instanceof Button) {
                            ((Button) currentFocus).performClick();
                            return true;
                        } else if (currentFocus instanceof androidx.appcompat.widget.AppCompatButton) {
                            ((androidx.appcompat.widget.AppCompatButton) currentFocus).performClick();
                            return true;
                        }
                    }
                    return true;
                }

                // Let ENTER perform its default action for other views
                return false;
            }

            // Handle T key for tare button (only if no EditText is focused)
            if ((keyCode == KeyEvent.KEYCODE_T) && !isEditTextFocused) {
                // Check if Fragment A is visible
                if (visibleFragment instanceof AFragment && visibleFragment.isVisible()) {
                    AFragment aFragment = (AFragment) visibleFragment;
                    aFragment.performTButtonAction();
                    return true;
                } else {
                    // If Fragment A is not visible, just click the button if it exists
                    if (buttonT != null) {
                        buttonT.performClick();
                        return true;
                    }
                }
                return true;
            }

            // Handle G key for gross button (only if no EditText is focused)
            if ((keyCode == KeyEvent.KEYCODE_G) && !isEditTextFocused) {
                // Check if Fragment A is visible
                if (visibleFragment instanceof AFragment && visibleFragment.isVisible()) {
                    AFragment aFragment = (AFragment) visibleFragment;
                    aFragment.performGButtonAction();
                    return true;
                } else {
                    // If Fragment A is not visible, just click the button if it exists
                    if (buttonG != null) {
                        buttonG.performClick();
                        return true;
                    }
                }
                return true;
            }

            // Handle M key for manual tare button (only if no EditText is focused)
            if ((keyCode == KeyEvent.KEYCODE_M) && !isEditTextFocused) {
                // Check if Fragment A is visible
                if (visibleFragment instanceof AFragment && visibleFragment.isVisible()) {
                    AFragment aFragment = (AFragment) visibleFragment;
                    aFragment.performMButtonAction();
                    return true;
                }
                return true;
            }

            // Handle number keys for fragments - but only if an EditText is not focused
            if ((keyCode == KeyEvent.KEYCODE_1 || keyCode == KeyEvent.KEYCODE_NUMPAD_1) && !isEditTextFocused) {
                toggleFragment(buttonA, aFragment, FragmentState.FRAGMENT_A, "#FFA500");
                return true;
            }

            if ((keyCode == KeyEvent.KEYCODE_2 || keyCode == KeyEvent.KEYCODE_NUMPAD_2) && !isEditTextFocused) {
                toggleFragment(buttonB, bFragment, FragmentState.FRAGMENT_B, "#00FF00");
                return true;
            }
        }

        return super.dispatchKeyEvent(event);
    }
    // Add this method to setup focus change listeners for buttons
        private void setupButtonFocusListeners() {
        // Setup focus change listener for button T
        if (buttonT != null) {
            buttonT.setOnFocusChangeListener(new View.OnFocusChangeListener() {
                @Override
                public void onFocusChange(View v, boolean hasFocus) {
                    if (hasFocus) {
                        // Button gained focus
                        buttonT.setBackgroundColor(Color.parseColor("#FFA500")); // Orange color
                        buttonT.setTextColor(Color.WHITE);
                    } else {
                        // Button lost focus
                        buttonT.setBackgroundColor(Color.parseColor("#808080")); // Gray color
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
                        // Button gained focus
                        buttonG.setBackgroundColor(Color.parseColor("#00FF00")); // Green color
                        buttonG.setTextColor(Color.WHITE);
                    } else {
                        // Button lost focus
                        buttonG.setBackgroundColor(Color.parseColor("#808080")); // Gray color
                        buttonG.setTextColor(Color.BLACK);
                    }
                }
            });
        }

        // Setup focus change listener for button A (if exists)
        if (buttonA != null) {
            buttonA.setOnFocusChangeListener(new View.OnFocusChangeListener() {
                @Override
                public void onFocusChange(View v, boolean hasFocus) {
                    if (hasFocus) {
                        buttonA.setBackgroundColor(Color.parseColor("#FFA500")); // Orange color
                        buttonA.setTextColor(Color.WHITE);
                    } else {
                        buttonA.setBackgroundColor(Color.parseColor("#808080")); // Gray color
                        buttonA.setTextColor(Color.BLACK);
                    }
                }
            });
        }

        // Setup focus change listener for button B (if exists)
        if (buttonB != null) {
            buttonB.setOnFocusChangeListener(new View.OnFocusChangeListener() {
                @Override
                public void onFocusChange(View v, boolean hasFocus) {
                    if (hasFocus) {
                        buttonB.setBackgroundColor(Color.parseColor("#00FF00")); // Green color
                        buttonB.setTextColor(Color.WHITE);
                    } else {
                        buttonB.setBackgroundColor(Color.parseColor("#808080")); // Gray color
                        buttonB.setTextColor(Color.BLACK);
                    }
                }
            });
        }
    }

    /**
     * Helper method to find the next focusable view in a specific direction
     */
    private View findNextFocusableView(View currentView, int direction) {
        if (currentView == null) return null;

        @SuppressLint("WrongConstant") View nextView = currentView.focusSearch(direction);

        // Keep searching until we find a focusable view or run out of candidates
        while (nextView != null && !nextView.isFocusable()) {
            @SuppressLint("WrongConstant") View tempView = nextView.focusSearch(direction);
            if (tempView == null || tempView == nextView) break; // Prevent infinite loop
            nextView = tempView;
        }

        return nextView;
    }
    /**
     * Helper method to find the next EditText in a specific direction
     */
    private View findNextEditText(View currentView, int direction) {
        if (currentView == null) return null;

        @SuppressLint("WrongConstant") View nextView = currentView.focusSearch(direction);

        // Keep searching until we find an EditText or run out of candidates
        while (nextView != null && !(nextView instanceof EditText)) {
            @SuppressLint("WrongConstant") View tempView = nextView.focusSearch(direction);
            if (tempView == null || tempView == nextView) break; // Prevent infinite loop
            nextView = tempView;
        }

        return (nextView instanceof EditText) ? nextView : null;
    }

    /**
     * Helper method to find the first EditText in the layout
     */
    private EditText findFirstEditText() {
        View rootView = findViewById(android.R.id.content);
        if (rootView != null) {
            return findFirstEditTextInView(rootView);
        }
        return null;
    }

    /**
     * Recursively search for the first EditText
     */
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

    /**
     * Helper method to find the first focusable view in the layout
     */
    private View findFirstFocusableView() {
        // Try to find the view with the next focus forward ID in the root view
        View rootView = findViewById(android.R.id.content);
        if (rootView != null) {
            return findFirstFocusableInView(rootView);
        }
        return null;
    }

    /**
     * Recursively search for the first focusable view
     */
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
        mNavigationView.setNavigationItemSelectedListener(this);

        Intent newint = getIntent();
        address = newint.getStringExtra(MainActivity.EXTRA_ADDRESS);
        info_address = newint.getStringExtra(MainActivity.EXTRA_INFO);

        if (address != null) {
            connectToDevice(address, info_address);
        } else {
            // Try to auto-connect to last used device
            tryAutoConnect();
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

    private void tryAutoConnect() {
        if (!isAutoConnectEnabled.get()) {
            Log.d("AutoConnect", "Auto-connect is disabled");
            return;
        }

        if (isBluetoothConnected()) {
            Log.d("AutoConnect", "Already connected");
            updateConnectionUI(true);
            return;
        }

        String savedAddress = lastDevicePrefs.getString("address", "");
        String savedInfo = lastDevicePrefs.getString("info", "");

        if (!savedAddress.isEmpty()) {
            Log.d("AutoConnect", "Found saved device: " + savedAddress);
            updateConnectionStatus("Auto-connecting...", R.drawable.ic_bluetooth_connecting);
            connectToDevice(savedAddress, savedInfo);
        } else {
            Log.d("AutoConnect", "No saved device found");
            updateConnectionStatus("Disconnected", R.drawable.ic_bluetooth_disconnected);
        }
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
                dayView.setVisibility(View.GONE);
                TextView clockTimeView = findViewById(R.id.hk_time);

                if (clockView != null && dayView != null && clockTimeView != null) {
                    String datePattern = "dd MMM yyyy";
                    String dayPattern = "EEEE";
                    String timePattern = "hh:mm:ss";

                    SimpleDateFormat dateFormat = new SimpleDateFormat(datePattern, Locale.getDefault());
                    SimpleDateFormat dayFormat = new SimpleDateFormat(dayPattern, Locale.getDefault());
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
        // Prevent too frequent connection attempts
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

        // Ensure any old connection is cleaned up
        if (!isCleaningUp.get()) {
            cleanupConnection();
        }

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
            progress.show();
            updateConnectionStatus("Connecting...", R.drawable.ic_bluetooth_connecting);
        });

        executeInBackground(() -> {
            try {
                bluetoothManager.connect(address, info, this);
            } catch (Exception e) {
                Log.e("Connect", "Error during connection: " + e.getMessage());
                runOnUiThread(() -> {
                    if (progress != null && progress.isShowing()) {
                        progress.dismiss();
                    }
                    updateConnectionUI(false);
                    Toast.makeText(MainActivity.this,
                            "Connection error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
            } finally {
                uiHandler.post(() -> {
                    if (progress != null && progress.isShowing()) {
                        progress.dismiss();
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
                    updateConnectionUI(true);
                    saveDeviceForAutoConnect();
                  //  Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
                    break;

                case BluetoothConnectionManager.CONNECTION_FAILED:
                    updateConnectionUI(false);
                   // Toast.makeText(this, "Bluetooth connection failed: " + message, Toast.LENGTH_LONG).show();
                    break;
            }
        });
    }

    @Override
    public void onDataReceived(byte[] data) {
        // Handle received data
    }

    @Override
    public void onConnectionLost() {
        runOnUiThread(() -> {
            Log.d("Connection", "Connection lost");
            updateConnectionUI(false);
           // Toast.makeText(this, "Bluetooth connection lost", Toast.LENGTH_SHORT).show();
        });
    }

    @Override
    public void onSensorDataUpdated(String cutting) {
        Log.d("Received_Data 1", cutting);
        txtCounter.setText(cutting);
    }

    @Override
    public void onSettingsUpdated(int tempSet, int humidSet, int pressureSet) {
    }

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
            exitApplication();
        } else if (id == R.id.nav_home) {
            hideFragmentAndShowCounter();
        } else if (id == R.id.action_searchList) {
            ScanDevicesList();
        } else if (id == R.id.action_pairedList) {
            pairedDevicesList();
        } else if (id == R.id.action_disconnect) {
            executeInBackground(() -> {
                bluetoothManager.disconnect();
                runOnUiThread(() -> updateConnectionUI(false));
            });
        } else if (id == R.id.action_toggle_auto_connect) {
            toggleAutoConnect();
        }

        drawerLayout.closeDrawer(GravityCompat.START);
        return true;
    }

    /**
     * Toggle auto-connect setting
     */
    private void toggleAutoConnect() {
        boolean newState = !isAutoConnectEnabled.get();
        isAutoConnectEnabled.set(newState);

        // Save preference
        autoConnectPrefs.edit()
                .putBoolean(PREF_AUTO_CONNECT, newState)
                .apply();

        // Update menu item if needed
        invalidateOptionsMenu();

        String message = newState ? "Auto-connect enabled" : "Auto-connect disabled";
       // Toast.makeText(this, message, Toast.LENGTH_SHORT).show();

        // If enabling and not connected, try to auto-connect
        if (newState && !isConnected.get()) {
            // Clean up first
            cleanupConnection();
            // Add delay before auto-connect
            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                tryAutoConnect();
            }, 2000);
        } else if (!newState) {
            // Stop any ongoing reconnection attempts
            stopReconnectionAttempts();
            // Disconnect if connected
            if (isConnected.get()) {
                executeInBackground(() -> {
                    bluetoothManager.disconnect();
                });
            }
        }
    }

    public void exitApplication() {
        final AlertDialog.Builder adb = new AlertDialog.Builder(this);
        adb.setMessage("Are you sure you want to exit application?");
        adb.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                // Clean up before exiting
                cleanupConnection();
                stopReconnectionAttempts();
                dialog.dismiss();
                finish();
            }
        });
        adb.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
               // Toast.makeText(MainActivity.this, "Cancel", Toast.LENGTH_SHORT).show();
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

        // Update auto-connect menu item if exists
        MenuItem autoConnectItem = menu.findItem(R.id.action_toggle_auto_connect);
        if (autoConnectItem != null) {
            autoConnectItem.setTitle(isAutoConnectEnabled.get() ?
                    "Disable Auto-Connect" : "Enable Auto-Connect");
        }

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.action_disconnect) {
            executeInBackground(() -> {
                bluetoothManager.disconnect();
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
                // Clean up before connecting to new device
                cleanupConnection();
                // Add delay before connecting
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

        Log.d("MainActivity", "Broadcast receiver registered successfully");

        // Check connection status and try auto-connect if needed
        if (!isBluetoothConnected() && isAutoConnectEnabled.get()) {
            // Add delay before auto-connect to ensure everything is ready
            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                tryAutoConnect();
            }, 2000);
        }
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
    }

    private void updateBluetoothUI(int status, String message) {
        Log.d("MainActivity", "updateBluetoothUI called with status: " + status);

        switch (status) {
            case BluetoothConnectionManager.CONNECTION_SUCCESS:
                updateConnectionUI(true);
              //  Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
                break;

            case BluetoothConnectionManager.CONNECTION_FAILED:
                updateConnectionUI(false);
               // Toast.makeText(this, "Disconnected: " + message, Toast.LENGTH_LONG).show();
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

        // Final cleanup
        cleanupConnection();

        if (bluetoothManager != null) {
            bluetoothManager.release(); // Make sure to release all resources
        }
    }
}