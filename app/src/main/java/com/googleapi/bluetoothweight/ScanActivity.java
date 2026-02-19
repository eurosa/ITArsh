/*
 * MIT License
 * <p>
 * Copyright (c) 2017 Donato Rimenti
 * <p>
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * <p>
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 * <p>
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package com.googleapi.bluetoothweight;

import static com.googleapi.bluetoothweight.bluetooth.BluetoothController.PERMISSION_REQUEST_BLUETOOTH_SCAN_CONNECT;

import android.Manifest;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.googleapi.bluetoothweight.bluetooth.BluetoothController;
import com.googleapi.bluetoothweight.bluetooth.BluetoothDiscoveryDeviceListener;
import com.googleapi.bluetoothweight.view.DeviceRecyclerViewAdapter;
import com.googleapi.bluetoothweight.view.ListInteractionListener;
import com.googleapi.bluetoothweight.view.RecyclerViewProgressEmptySupport;

import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Set;
import java.util.UUID;

import pub.devrel.easypermissions.AfterPermissionGranted;
import pub.devrel.easypermissions.EasyPermissions;

/*******************************************************************************************************
 * Main Activity of this application.
 *
 * @author Donato Rimenti
 *******************************************************************************************************/
public class ScanActivity extends AppCompatActivity implements ListInteractionListener<BluetoothDevice> {

    //==============================To Connect Bluetooth Device==========================================
    private ProgressDialog progress;
    private boolean isBtConnected = false;
    BluetoothSocket btSocket = null;
    OutputStream mmOutputStream;
    InputStream mmInputStream;
    static final UUID myUUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    private String addressBLE = null;
    private String infoBLE = null;
    String address = null;
    private String name = null;
    private Button sendBtn;

    boolean listViewFlag = true;

    ListView devicelist;
    //Bluetooth
    private BluetoothAdapter myBluetooth = null;
    private Set<BluetoothDevice> pairedDevices;
    public static String EXTRA_ADDRESS = "device_address";
    public static String EXTRA_INFO = "device_info";
    //==============================To connect bluetooth devices=============================

    private final int REQUEST_LOCATION_PERMISSION = 1;
    /**
     * Tag string used for logging.
     */
    private static final String TAG = "ScanActivity";

    /**
     * The controller for Bluetooth functionalities.
     */
    private BluetoothController bluetooth;

    /**
     * The Bluetooth discovery button.
     */
    private FloatingActionButton fab;

    /**
     * Progress dialog shown during the pairing process.
     */
    private ProgressDialog bondingProgressDialog;

    /**
     * Adapter for the recycler view.
     */
    private DeviceRecyclerViewAdapter recyclerViewAdapter;

    private RecyclerViewProgressEmptySupport recyclerView;

    private LinearLayout adContainer;

    // ArrayList to store discovered devices
    private ArrayList<BluetoothDevice> discoveredDevices = new ArrayList<>();

    /**
     * {@inheritDoc}
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scan);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);
        getSupportActionBar().setHomeAsUpIndicator(R.drawable.ic_left_arrow);

        requestLocationPermission();

        devicelist = findViewById(R.id.listView);

        // Initialize Bluetooth adapter
        myBluetooth = BluetoothAdapter.getDefaultAdapter();

        // Check if Bluetooth is available
        boolean hasBluetooth = getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH);
        if (!hasBluetooth || myBluetooth == null) {
            AlertDialog dialog = new AlertDialog.Builder(ScanActivity.this).create();
            dialog.setTitle(getString(R.string.bluetooth_not_available_title));
            dialog.setMessage(getString(R.string.bluetooth_not_available_message));
            dialog.setButton(AlertDialog.BUTTON_NEUTRAL, "OK",
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                            ScanActivity.this.finish();
                        }
                    });
            dialog.setCancelable(false);
            dialog.show();
            return;
        }

        // Check if Bluetooth is enabled
        if (!myBluetooth.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(this,
                            new String[]{Manifest.permission.BLUETOOTH_CONNECT},
                            PERMISSION_REQUEST_BLUETOOTH_SCAN_CONNECT);
                    return;
                }
            }
            startActivityForResult(enableBtIntent, 1);
        }

        // Sets up the RecyclerView.
        this.recyclerViewAdapter = new DeviceRecyclerViewAdapter(this);
        this.recyclerView = findViewById(R.id.list);
        this.recyclerView.setLayoutManager(new LinearLayoutManager(this));

        // Sets the view to show when the dataset is empty.
        View emptyView = findViewById(R.id.empty_list);
        this.recyclerView.setEmptyView(emptyView);

        // Sets the view to show during progress.
        ProgressBar progressBar = findViewById(R.id.progressBar);
        this.recyclerView.setProgressView(progressBar);

        this.recyclerView.setAdapter(recyclerViewAdapter);

        // Sets up the bluetooth controller.
        // The DeviceRecyclerViewAdapter implements BluetoothDiscoveryDeviceListener
        this.bluetooth = new BluetoothController(this, myBluetooth, recyclerViewAdapter);

        fab = findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                searchDeviceList();
            }
        });

        // Register the BroadcastReceiver for discovery
        registerDiscoveryReceiver();

        // Show paired devices initially
        pairedDevicesList();
    }

    private void registerDiscoveryReceiver() {
        IntentFilter filter = new IntentFilter();
        filter.addAction(BluetoothDevice.ACTION_FOUND);
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED);
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        registerReceiver(discoveryReceiver, filter);
    }

    // BroadcastReceiver for discovered devices
    private final BroadcastReceiver discoveryReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                // Discovery has found a device
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);

                if (device != null) {
                    // Check if device already exists in the list
                    boolean exists = false;
                    for (BluetoothDevice d : discoveredDevices) {
                        if (d.getAddress().equals(device.getAddress())) {
                            exists = true;
                            break;
                        }
                    }

                    if (!exists) {
                        discoveredDevices.add(device);

                        // Add device to the list through the adapter
                        // The DeviceRecyclerViewAdapter has a method to add devices
                        if (recyclerViewAdapter != null) {
                            // Assuming the adapter has a method to add devices
                            // You might need to check the actual method name in your adapter
                            // For example, if it has addDevice(BluetoothDevice device)
                            // recyclerViewAdapter.addDevice(device);

                            // If the adapter implements BluetoothDiscoveryDeviceListener,
                            // it should have a method like onDeviceDiscovered
                            // For now, let's just update the UI through the BluetoothController
                            if (bluetooth != null) {
                                // The BluetoothController might handle this internally
                                if (ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                                    // TODO: Consider calling
                                    //    ActivityCompat#requestPermissions
                                    // here to request the missing permissions, and then overriding
                                    //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                                    //                                          int[] grantResults)
                                    // to handle the case where the user grants the permission. See the documentation
                                    // for ActivityCompat#requestPermissions for more details.
                                    return;
                                }
                                Log.d(TAG, "Device discovered: " + device.getName() + " [" + device.getAddress() + "]");
                            }
                        }
                    }
                }
            } else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
                // Discovery finished
                Log.d(TAG, "Discovery finished");
                if (fab != null) {
                    fab.setImageResource(R.drawable.ic_bluetooth_white_24dp);
                }

                // Update UI to show discovery finished
                if (recyclerView != null) {
                    recyclerView.endLoading();
                }

                Toast.makeText(ScanActivity.this,
                        "Discovery finished. Found " + discoveredDevices.size() + " devices",
                        Toast.LENGTH_SHORT).show();

            } else if (BluetoothAdapter.ACTION_DISCOVERY_STARTED.equals(action)) {
                // Discovery started
                Log.d(TAG, "Discovery started");
                if (fab != null) {
                    fab.setImageResource(R.drawable.ic_bluetooth_searching_white_24dp);
                }

                // Clear previous discovered devices
                discoveredDevices.clear();

                // Update UI to show loading
                if (recyclerView != null) {
                    recyclerView.startLoading();
                }
            }
        }
    };

    private void pairedDevicesList() {
        if (myBluetooth == null) return;

        ArrayList<String> list = new ArrayList<>();

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                    return;
                }
            }

            pairedDevices = myBluetooth.getBondedDevices();

            if (pairedDevices != null && pairedDevices.size() > 0) {
                for (BluetoothDevice bt : pairedDevices) {
                    String deviceName = bt.getName() != null ? bt.getName() : "Unknown Device";
                    list.add(deviceName + "\n" + bt.getAddress());
                }

                final ArrayAdapter<String> modeAdapter = new ArrayAdapter<>(this,
                        android.R.layout.simple_list_item_1, list);
                devicelist.setAdapter(modeAdapter);
                devicelist.setOnItemClickListener(myListClickListener);
                setListViewHeightBasedOnItems(devicelist);
            } else {
                Toast.makeText(getApplicationContext(),
                        "No Paired Bluetooth Devices Found.", Toast.LENGTH_LONG).show();
            }
        } catch (SecurityException e) {
            Log.e(TAG, "Security exception: " + e.getMessage());
        }
    }

    public static void setListViewHeightBasedOnItems(ListView listView) {
        ListAdapter listAdapter = listView.getAdapter();
        if (listAdapter != null) {
            int numberOfItems = listAdapter.getCount();
            int totalItemsHeight = 0;

            for (int itemPos = 0; itemPos < numberOfItems; itemPos++) {
                View item = listAdapter.getView(itemPos, null, listView);
                item.measure(0, 0);
                totalItemsHeight += item.getMeasuredHeight();
            }

            int totalDividersHeight = listView.getDividerHeight() * (numberOfItems - 1);
            ViewGroup.LayoutParams params = listView.getLayoutParams();
            params.height = totalItemsHeight + totalDividersHeight;
            listView.setLayoutParams(params);
            listView.requestLayout();
        }
    }

    private AdapterView.OnItemClickListener myListClickListener = new AdapterView.OnItemClickListener() {
        public void onItemClick(AdapterView<?> av, View v, int arg2, long arg3) {
            String info = ((TextView) v).getText().toString();
            String address = info.substring(info.length() - 17);

            Intent i = new Intent(getApplicationContext(), MainActivity.class);
            i.putExtra(EXTRA_ADDRESS, address);
            i.putExtra(EXTRA_INFO, info);
            i.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            setResult(RESULT_OK, i);
            startActivity(i);
            finish();
        }
    };

    @Override
    public void onBackPressed() {
        setResult(RESULT_CANCELED);
        super.onBackPressed();
    }

    public void searchDeviceList() {
        // Check permissions first
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            // Android 12+ permissions
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.BLUETOOTH_SCAN},
                        PERMISSION_REQUEST_BLUETOOTH_SCAN_CONNECT);
                return;
            }

            if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.BLUETOOTH_CONNECT},
                        PERMISSION_REQUEST_BLUETOOTH_SCAN_CONNECT);
                return;
            }
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            // Android 6.0-11: Need location permission for Bluetooth discovery
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                        REQUEST_LOCATION_PERMISSION);
                return;
            }
        }

        if (myBluetooth == null) {
            myBluetooth = BluetoothAdapter.getDefaultAdapter();
        }

        if (myBluetooth == null) {
            Toast.makeText(this, "Bluetooth not available", Toast.LENGTH_SHORT).show();
            return;
        }

        // If the bluetooth is not enabled, turns it on
        if (!myBluetooth.isEnabled()) {
            Toast.makeText(getApplicationContext(), R.string.enabling_bluetooth, Toast.LENGTH_SHORT).show();
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT)
                        != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(this,
                            new String[]{Manifest.permission.BLUETOOTH_CONNECT},
                            PERMISSION_REQUEST_BLUETOOTH_SCAN_CONNECT);
                    return;
                }
            }

            startActivityForResult(enableBtIntent, 1);
            return;
        }

        // Check if discovery is already in progress
        try {
            if (myBluetooth.isDiscovering()) {
                myBluetooth.cancelDiscovery();
                Toast.makeText(getApplicationContext(), "Discovery stopped", Toast.LENGTH_SHORT).show();
                fab.setImageResource(R.drawable.ic_bluetooth_white_24dp);
                return;
            }
        } catch (SecurityException e) {
            Log.e(TAG, "Security exception checking discovery", e);
        }

        // Clear previous discovered devices
        discoveredDevices.clear();

        // Clear the adapter through BluetoothController
        if (bluetooth != null) {
            bluetooth.cancelDiscovery();
        }

        // Start discovery
        Toast.makeText(getApplicationContext(), "Searching for devices...", Toast.LENGTH_SHORT).show();
        fab.setImageResource(R.drawable.ic_bluetooth_searching_white_24dp);

        // Start discovery through BluetoothController
        if (bluetooth != null) {
            bluetooth.startDiscovery();
        } else {
            // Fallback: direct discovery
            try {
                boolean started = myBluetooth.startDiscovery();
                if (!started) {
                    Toast.makeText(this, "Failed to start discovery", Toast.LENGTH_SHORT).show();
                    fab.setImageResource(R.drawable.ic_bluetooth_white_24dp);
                }
            } catch (SecurityException e) {
                Log.e(TAG, "Security exception when starting discovery", e);
                Toast.makeText(this, "Permission denied for Bluetooth discovery", Toast.LENGTH_SHORT).show();
                fab.setImageResource(R.drawable.ic_bluetooth_white_24dp);
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == PERMISSION_REQUEST_BLUETOOTH_SCAN_CONNECT) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission granted, try the operation again
                searchDeviceList();
            } else {
                Toast.makeText(this, "Bluetooth scan permission required", Toast.LENGTH_SHORT).show();
            }
        } else if (requestCode == REQUEST_LOCATION_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                searchDeviceList();
            } else {
                Toast.makeText(this, "Location permission required for Bluetooth discovery", Toast.LENGTH_SHORT).show();
            }
        }

        // Forward results to EasyPermissions
        EasyPermissions.onRequestPermissionsResult(requestCode, permissions, grantResults, this);
    }

    @AfterPermissionGranted(REQUEST_LOCATION_PERMISSION)
    public void requestLocationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            // For Android 12+, we need BLUETOOTH_SCAN, BLUETOOTH_CONNECT, and BLUETOOTH_ADMIN
            String[] perms = {
                    Manifest.permission.BLUETOOTH_SCAN,
                    Manifest.permission.BLUETOOTH_CONNECT,
                    Manifest.permission.BLUETOOTH_ADMIN,
                    Manifest.permission.ACCESS_FINE_LOCATION
            };

            if (!EasyPermissions.hasPermissions(this, perms)) {
                EasyPermissions.requestPermissions(this,
                        "Please grant all Bluetooth permissions for pairing",
                        REQUEST_LOCATION_PERMISSION,
                        perms);
            }
        } else {
            // For older Android versions
            String[] perms = {
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.BLUETOOTH_ADMIN
            };

            if (!EasyPermissions.hasPermissions(this, perms)) {
                EasyPermissions.requestPermissions(this,
                        "Please grant location and Bluetooth admin permissions for pairing",
                        REQUEST_LOCATION_PERMISSION,
                        perms);
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        return true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onItemClick(BluetoothDevice device) {
        Log.d(TAG, "Item clicked : " + BluetoothController.deviceToString(device));
        if (bluetooth.isAlreadyPaired(device)) {
            Log.d(TAG, "Device already paired!");
            Toast.makeText(this, R.string.device_already_paired, Toast.LENGTH_SHORT).show();

            // If already paired, connect directly
            String deviceName = BluetoothController.getDeviceName(device);
            String address = device.getAddress();

            Intent i = new Intent(getApplicationContext(), MainActivity.class);
            i.putExtra(EXTRA_ADDRESS, address);
            i.putExtra(EXTRA_INFO, deviceName + "\n" + address);
            i.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            setResult(RESULT_OK, i);
            startActivity(i);
            finish();
        } else {
            Log.d(TAG, "Device not paired. Pairing.");
            boolean outcome = bluetooth.pair(device);

            String deviceName = BluetoothController.getDeviceName(device);
            if (outcome) {
                bondingProgressDialog = ProgressDialog.show(this, "",
                        "Pairing with device " + deviceName + "...", true, false);
            } else {
                Log.d(TAG, "Error while pairing with device " + deviceName + "!");
                Toast.makeText(this, "Error while pairing with device " + deviceName + "!", Toast.LENGTH_SHORT).show();
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void startLoading() {
        if (this.recyclerView != null) {
            this.recyclerView.startLoading();
        }
        if (this.fab != null) {
            this.fab.setImageResource(R.drawable.ic_bluetooth_searching_white_24dp);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void endLoading(boolean partialResults) {
        if (this.recyclerView != null) {
            this.recyclerView.endLoading();
        }
        if (!partialResults && this.fab != null) {
            fab.setImageResource(R.drawable.ic_bluetooth_white_24dp);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void endLoadingWithDialog(boolean error, BluetoothDevice device) {
        if (this.bondingProgressDialog != null) {
            String message;
            String deviceName = BluetoothController.getDeviceName(device);

            if (error) {
                message = "Failed pairing with device " + deviceName + "!";
            } else {
                message = "Successfully paired with device " + deviceName + "!";

                // After successful pairing, connect to the device
                String address = device.getAddress();
                Intent i = new Intent(getApplicationContext(), MainActivity.class);
                i.putExtra(EXTRA_ADDRESS, address);
                i.putExtra(EXTRA_INFO, deviceName + "\n" + address);
                i.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                setResult(RESULT_OK, i);
                startActivity(i);
                finish();
            }

            this.bondingProgressDialog.dismiss();
            Toast.makeText(getApplicationContext(), message, Toast.LENGTH_SHORT).show();
            this.bondingProgressDialog = null;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void onDestroy() {
        super.onDestroy();
        try {
            unregisterReceiver(discoveryReceiver);
        } catch (IllegalArgumentException e) {
            Log.e(TAG, "Receiver not registered", e);
        }

        if (bluetooth != null) {
            bluetooth.close();
        }

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        if (myBluetooth != null && myBluetooth.isDiscovering()) {
            try {
                myBluetooth.cancelDiscovery();
            } catch (SecurityException e) {
                Log.e(TAG, "Security exception canceling discovery", e);
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void onRestart() {
        super.onRestart();
        if (this.bluetooth != null) {
            this.bluetooth.cancelDiscovery();
        }
        pairedDevicesList();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void onStop() {
        super.onStop();
        if (this.bluetooth != null) {
            this.bluetooth.cancelDiscovery();
        }
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
    }
}