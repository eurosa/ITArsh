package com.googleapi.bluetoothweight;

import static com.googleapi.bluetoothweight.bluetooth.BluetoothConnectionManager.checkBluetoothPermissions;
import static com.googleapi.bluetoothweight.bluetooth.BluetoothConnectionManager.requestBluetoothPermissions;

import android.Manifest;
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

    // Fragment instances
    private AFragment aFragment;
    private BFragment bFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

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

        // Initialize fragments
        aFragment = new AFragment();
        bFragment = new BFragment();

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

        // Make sure counter is visible initially
        showCounterViews();

        Handler handler = new Handler();
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                value += 1;
                handler.postDelayed(this, 1000);
            }
        };
        handler.post(runnable);

        setupClock();

        // Setup button click listeners
        setupButtonClickListener(buttonA, aFragment, "A_FRAGMENT", "#FFA500");
        setupButtonClickListener(buttonB, bFragment, "B_FRAGMENT", "#00FF00");
    }

    /**
     * Show counter and related views
     */
    private void showCounterViews() {
        txtCounter.setVisibility(View.VISIBLE);
        abcLayout.setVisibility(View.VISIBLE);
        uNit.setVisibility(View.VISIBLE);
        txtCounter.setTextColor(Color.parseColor("#FF0000"));
    }

    /**
     * Hide counter and related views
     */
    private void hideCounterViews() {
        txtCounter.setVisibility(View.GONE);
        abcLayout.setVisibility(View.GONE);
        uNit.setVisibility(View.GONE);
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

            // Deselect the button
            if (selectedButton != null) {
                selectedButton.setSelected(false);
                selectedButton = null;
            }

            // Show counter and related views
            showCounterViews();

            // Clear visible fragment reference
            visibleFragment = null;
        }
    }

    /**
     * Show fragment and hide counter
     */
    private void showFragmentAndHideCounter(Button button, Fragment fragment, String tag, String colorHex) {
        // Hide current visible fragment if any
        if (visibleFragment != null) {
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
                    .add(R.id.fragment_container, fragment, tag)
                    .commitAllowingStateLoss();
        } else {
            getSupportFragmentManager().beginTransaction()
                    .show(fragment)
                    .commitAllowingStateLoss();
        }

        // Hide counter and related views
        hideCounterViews();
        txtCounter.setTextColor(Color.parseColor(colorHex));

        visibleFragment = fragment;
        button.setSelected(true);
        selectedButton = button;
    }

    /**
     * Toggle fragment visibility - for both button click and keyboard press
     */
    private void toggleFragment(Button button, Fragment fragment, String tag, String colorHex) {
        // If clicking the same fragment that's visible, hide it
        if (visibleFragment == fragment) {
            hideFragmentAndShowCounter();
        }
        // If clicking a different fragment or no fragment is visible, show it
        else {
            showFragmentAndHideCounter(button, fragment, tag, colorHex);
        }
    }

    @Override
    public void onBackPressed() {
        // If drawer is open, close it first
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START);
        }
        // If a fragment is visible, hide it and show the counter
        else if (visibleFragment != null) {
            hideFragmentAndShowCounter();
        }
        // If no fragment is visible and drawer is closed, exit normally
        else {
            super.onBackPressed();
        }
    }

    private void setupButtonClickListener(Button button, Fragment fragment, String tag, String colorHex) {
        button.setOnClickListener(v -> toggleFragment(button, fragment, tag, colorHex));
    }

    /**
     * Handle key events including keyboard keys
     */
    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        if (event.getAction() == KeyEvent.ACTION_DOWN) {
            int keyCode = event.getKeyCode();
            char unicodeChar = (char) event.getUnicodeChar();

            Log.d("KEY_PRESSED", "KeyCode: " + keyCode);
            Log.d("KEY_PRESSED", "Key Name: " + KeyEvent.keyCodeToString(keyCode));
            Log.d("KEY_PRESSED", "Character: " + unicodeChar);

            // Handle BACK button
            if (keyCode == KeyEvent.KEYCODE_BACK) {
                // If drawer is open, close it
                if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
                    drawerLayout.closeDrawer(GravityCompat.START);
                    return true;
                }
                // If fragment is visible, hide it
                else if (visibleFragment != null) {
                    hideFragmentAndShowCounter();
                    return true;
                }
                return false; // Let system handle if nothing else
            }

            // Handle ESCAPE key (acts as back button on keyboards)
            if (keyCode == KeyEvent.KEYCODE_ESCAPE) {
                // If drawer is open, close it
                if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
                    drawerLayout.closeDrawer(GravityCompat.START);
                    return true;
                }
                // If fragment is visible, hide it
                else if (visibleFragment != null) {
                    hideFragmentAndShowCounter();
                    return true;
                }
                return true;
            }

            // Handle number keys for fragments
            if (keyCode == KeyEvent.KEYCODE_1 || keyCode == KeyEvent.KEYCODE_NUMPAD_1 || unicodeChar == '1') {
                toggleFragment(buttonA, aFragment, "A_FRAGMENT", "#FFA500");
                return true;
            }
            if (keyCode == KeyEvent.KEYCODE_2 || keyCode == KeyEvent.KEYCODE_NUMPAD_2 || unicodeChar == '2') {
                toggleFragment(buttonB, bFragment, "B_FRAGMENT", "#00FF00");
                return true;
            }
        }

        return super.dispatchKeyEvent(event);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        // This is kept for compatibility but dispatchKeyEvent handles most cases
        return super.onKeyDown(keyCode, event);
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
        if (isBluetoothConnected()) {
            Log.d("AutoConnect", "Already connected");
            return;
        }

        String savedAddress = lastDevicePrefs.getString("address", "");
        String savedInfo = lastDevicePrefs.getString("info", "");

        if (!savedAddress.isEmpty()) {
            Log.d("AutoConnect", "Found saved device: " + savedAddress);
            connectToDevice(savedAddress, savedInfo);
        } else {
            Log.d("AutoConnect", "No saved device found");
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
        this.currentConnectionAddress = address;
        this.currentConnectionInfo = info;

        if (!checkBluetoothPermissions(this)) {
            requestBluetoothPermissions(this, 1001);
            return;
        }

        uiHandler.post(() -> {
            if (progress == null) {
                progress = new ProgressDialog(this);
                progress.setMessage("Connecting...");
                progress.setCancelable(false);
            }
            progress.show();
        });

        executeInBackground(() -> {
            bluetoothManager.connect(address, info, this);

            uiHandler.post(() -> {
                if (progress != null && progress.isShowing()) {
                    progress.dismiss();
                }
            });
        });
    }

    @Override
    public void onConnectionResult(int resultCode, String message) {
        uiHandler.post(() -> {
            switch (resultCode) {
                case BluetoothConnectionManager.CONNECTION_SUCCESS:
                    connectionStatusIcon.setImageResource(R.drawable.ic_bluetooth_connected);
                    connectionStatusTxt.setText("Connected");
                    Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
                    saveDeviceForAutoConnect();
                    break;
                case BluetoothConnectionManager.CONNECTION_FAILED:
                    connectionStatusIcon.setImageResource(R.drawable.ic_bluetooth_disconnected);
                    connectionStatusTxt.setText("Disconnected");
                    bluetoothManager.disconnect();
                    Toast.makeText(this, "Bluetooth connection failed " + message, Toast.LENGTH_LONG).show();
                    break;
            }
        });
    }

    @Override
    public void onDataReceived(byte[] data) {
    }

    @Override
    public void onConnectionLost() {
    }

    @Override
    public void onSensorDataUpdated(String actual, String cutting) {
        Log.d("Received_Data 1", " actual " + actual + " " + cutting);
        txtCounter.setText(actual);
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

    private void updateButtonSelection(Button button) {
        if (selectedButton != null) {
            selectedButton.setSelected(false);
        }
        button.setSelected(true);
        selectedButton = button;
    }

    private void replaceFragment(Fragment fragment) {
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .commit();
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem menuItem) {
        int id = menuItem.getItemId();

        if (id == R.id.nav_exit) {
            exitApplication();
        }if (id == R.id.nav_home) {
            hideFragmentAndShowCounter();
        } else if (id == R.id.action_searchList) {
            ScanDevicesList();
        } else if (id == R.id.action_pairedList) {
            pairedDevicesList();
        } else if (id == R.id.action_disconnect) {
            executeInBackground(() -> bluetoothManager.disconnect());
        }

        drawerLayout.closeDrawer(GravityCompat.START);
        return true;
    }

    public void exitApplication() {
        final AlertDialog.Builder adb = new AlertDialog.Builder(this);
        adb.setMessage("Are you sure you want to exit application?");
        adb.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
                finish();
            }
        });
        adb.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                Toast.makeText(MainActivity.this, "Cancel", Toast.LENGTH_SHORT).show();
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

    public void shareApp() {
        try {
            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.setType("text/plain");
            shareIntent.putExtra(Intent.EXTRA_SUBJECT, "My application name");
            String shareMessage = "\nLet me recommend you this application\n\n";
            shareMessage = shareMessage + "https://play.google.com/store/apps/details?id=" + getPackageName() + "\n\n";
            shareIntent.putExtra(Intent.EXTRA_TEXT, shareMessage);
            startActivity(Intent.createChooser(shareIntent, "choose one"));
        } catch (Exception e) {
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_device_list, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.action_disconnect) {
            executeInBackground(() -> {
                if (bluetoothManager != null) {
                    runOnUiThread(() -> {
                        Toast.makeText(MainActivity.this,
                                "Bluetooth disconnected", Toast.LENGTH_SHORT).show();
                        connectionStatusIcon.setImageResource(R.drawable.ic_bluetooth_disconnected);
                        connectionStatusTxt.setText("Disconnected");
                    });
                }
            });
            return true;
        } else if (id == R.id.action_searchList) {
            ScanDevicesList();
            return true;
        } else if (id == R.id.action_pairedList) {
            pairedDevicesList();
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
                connectToDevice(address, info);
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
                connectionStatusIcon.setImageResource(R.drawable.ic_bluetooth_connected);
                connectionStatusTxt.setText("Connected");
                Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
                break;

            case BluetoothConnectionManager.CONNECTION_FAILED:
                connectionStatusIcon.setImageResource(R.drawable.ic_bluetooth_disconnected);
                connectionStatusTxt.setText("Disconnected");
                Toast.makeText(this, "Disconnected: " + message, Toast.LENGTH_LONG).show();
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
    }
}