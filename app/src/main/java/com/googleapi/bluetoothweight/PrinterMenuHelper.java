package com.googleapi.bluetoothweight;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class PrinterMenuHelper {

    private AppCompatActivity activity;
    private PrinterManager printerManager;
    private PrinterDialogCallback callback;
    private ProgressDialog progressDialog;
    private Handler mainHandler = new Handler(Looper.getMainLooper());
    private ExecutorService executor = Executors.newSingleThreadExecutor();

    public static final int MENU_PRINTER_SETTINGS = 2001;
    public static final int MENU_TEST_PRINT = 2002;
    public static final int MENU_DISCONNECT = 2003;

    public interface PrinterDialogCallback {
        void onPrinterSelected(UsbDevice printer);
        void onPrinterDisconnected();
        void onTestPrint();
    }

    public PrinterMenuHelper(AppCompatActivity activity, PrinterDialogCallback callback) {
        this.activity = activity;
        this.callback = callback;
        this.printerManager = PrinterManager.getInstance();
    }

    public void createPrinterMenu(Menu menu) {
        MenuItem printerItem = menu.add(0, MENU_PRINTER_SETTINGS, 100, "🖨️ Printer Settings");
        printerItem.setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);

        MenuItem testPrintItem = menu.add(0, MENU_TEST_PRINT, 101, "🖨️ Test Print");
        testPrintItem.setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);

        MenuItem disconnectItem = menu.add(0, MENU_DISCONNECT, 102, "🔌 Disconnect Printer");
        disconnectItem.setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);
    }

    public void updateMenuItems(Menu menu) {
        MenuItem printerItem = menu.findItem(MENU_PRINTER_SETTINGS);
        MenuItem disconnectItem = menu.findItem(MENU_DISCONNECT);
        MenuItem testPrintItem = menu.findItem(MENU_TEST_PRINT);

        if (printerManager.isPrinterConnected()) {
            if (printerItem != null) {
                printerItem.setTitle("🖨️ " + printerManager.getConnectedPrinterName());
            }
            if (disconnectItem != null) {
                disconnectItem.setVisible(true);
            }
            if (testPrintItem != null) {
                testPrintItem.setVisible(true);
            }
        } else {
            if (printerItem != null) {
                printerItem.setTitle("🖨️ Printer Settings");
            }
            if (disconnectItem != null) {
                disconnectItem.setVisible(false);
            }
            if (testPrintItem != null) {
                testPrintItem.setVisible(false);
            }
        }
    }

    public boolean handlePrinterMenuItem(MenuItem item) {
        int itemId = item.getItemId();

        if (itemId == MENU_PRINTER_SETTINGS) {
            showPrinterSettingsDialog();
            return true;
        } else if (itemId == MENU_TEST_PRINT) {
            showTestPrintDialog();
            return true;
        } else if (itemId == MENU_DISCONNECT) {
            confirmDisconnectPrinter();
            return true;
        }

        return false;
    }

    // ============= REPLACE YOUR EXISTING showPrinterSettingsDialog WITH THIS =============
    private void showPrinterSettingsDialog() {
        String[] options;
        if (printerManager.isPrinterConnected()) {
            options = new String[]{
                    "Scan for Printers",                // 0
                    "Disconnect Current Printer",       // 1
                    "Printer Status",                    // 2
                    "Diagnose Connection",                // 3
                    "Canon Style Print",                  // 4
                    "PCL Print",                          // 5
                    "ASCII Print",                        // 6
                    "Try Paper Commands",                  // 7
                    "Send ESC Commands",                   // 8
                    "Send PCL Commands",                   // 9
                    "Try All Formats",                     // 10
                    "Ultra Basic Print",                   // 11
                    "Try Every Endpoint",                  // 12
                    "Hard Reset",                          // 13
                    "Comprehensive Diagnostics",           // 14
                    "List USB Devices"                     // 15
            };
        } else {
            options = new String[]{
                    "Scan for Printers",                 // 0
                    "Diagnose Connection",               // 1
                    "Comprehensive Diagnostics",         // 2
                    "List USB Devices"                    // 3
            };
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        builder.setTitle("Printer Settings")
                .setItems(options, (dialog, which) -> {
                    if (!printerManager.isPrinterConnected() && which > 3) {
                        // If not connected and trying to access connected-only options, show message
                        Toast.makeText(activity, "Printer not connected", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    switch (which) {
                        case 0:
                            scanForPrinters();
                            break;
                        case 1:
                            if (printerManager.isPrinterConnected()) {
                                confirmDisconnectPrinter();
                            } else {
                                diagnosePrinter();
                            }
                            break;
                        case 2:
                            if (printerManager.isPrinterConnected()) {
                                showPrinterStatus();
                            } else {
                                diagnosePrinter();
                            }
                            break;
                        case 3:
                            diagnosePrinter();
                            break;
                        case 4:
                            runDirectUsbWrite();
                            break;
                        case 5:
                            runPCLPrint();
                            break;
                        case 6:
                            runASCIIPrint();
                            break;
                        case 7:
                            runPaperCommands();
                            break;
                        case 8:
                            runESCCommands();
                            break;
                        case 9:
                            runPCLCommandsTest();
                            break;
                        case 10:
                            runAllFormats();
                            break;
                        case 11:
                            runUltraBasicPrint();
                            break;
                        case 12:
                            runTryEveryEndpoint();
                            break;
                        case 13:
                            runHardReset();
                            break;
                        case 14:
                            runComprehensiveDiagnostics();
                            break;
                        case 15:
                            checkUsbDevices();
                            break;
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }


    /**
     * Run all possible formats test
     */
    private void runAllFormats() {
        if (!printerManager.isPrinterConnected()) {
            Toast.makeText(activity, "Printer not connected", Toast.LENGTH_SHORT).show();
            return;
        }

        showProgressDialog("Trying all formats...\n(This will take a minute)");

        executor.submit(() -> {
            boolean result = false;
            if (printerManager.usbPrinterHelper != null) {
                result = printerManager.usbPrinterHelper.tryAllPossibleFormats();
            }

            final boolean finalResult = result;
            mainHandler.post(() -> {
                dismissProgressDialog();
                if (finalResult) {
                    Toast.makeText(activity, "✅ Found working format! Check printer.", Toast.LENGTH_LONG).show();
                } else {
                    Toast.makeText(activity, "❌ No working format found", Toast.LENGTH_LONG).show();
                }
            });
        });
    }

    /**
     * Run ultra basic print
     */
    private void runUltraBasicPrint() {
        if (!printerManager.isPrinterConnected()) {
            Toast.makeText(activity, "Printer not connected", Toast.LENGTH_SHORT).show();
            return;
        }

        showProgressDialog("Trying ultra basic print...");

        executor.submit(() -> {
            boolean result = false;
            if (printerManager.usbPrinterHelper != null) {
                String testText = "TEST PRINT\n" +
                        new SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault()).format(new Date()) + "\n" +
                        "Ultra Basic Test";
                result = printerManager.usbPrinterHelper.ultraBasicPrint(testText);
            }

            final boolean finalResult = result;
            mainHandler.post(() -> {
                dismissProgressDialog();
                if (finalResult) {
                    Toast.makeText(activity, "✅ Ultra basic print sent", Toast.LENGTH_LONG).show();
                } else {
                    Toast.makeText(activity, "❌ Ultra basic print failed", Toast.LENGTH_LONG).show();
                }
            });
        });
    }

    /**
     * Try every endpoint
     */
    private void runTryEveryEndpoint() {
        if (!printerManager.isPrinterConnected()) {
            Toast.makeText(activity, "Printer not connected", Toast.LENGTH_SHORT).show();
            return;
        }

        showProgressDialog("Trying every endpoint...\nCheck Logcat for details");

        executor.submit(() -> {
            if (printerManager.usbPrinterHelper != null) {
                printerManager.usbPrinterHelper.tryEveryEndpoint();
            }

            mainHandler.post(() -> {
                dismissProgressDialog();
                Toast.makeText(activity, "Endpoint test complete. Check Logcat.", Toast.LENGTH_LONG).show();
            });
        });
    }

    /**
     * Hard reset USB connection
     */
    private void runHardReset() {
        showProgressDialog("Hard resetting USB connection...");

        executor.submit(() -> {
            if (printerManager.usbPrinterHelper != null) {
                printerManager.usbPrinterHelper.hardReset();
            }

            mainHandler.post(() -> {
                dismissProgressDialog();
                Toast.makeText(activity, "Hard reset complete", Toast.LENGTH_SHORT).show();
            });
        });
    }

    /**
     * Run paper commands test
     */
    private void runPaperCommands() {
        if (!printerManager.isPrinterConnected()) {
            Toast.makeText(activity, "Printer not connected", Toast.LENGTH_SHORT).show();
            return;
        }

        showProgressDialog("Trying paper commands...");

        executor.submit(() -> {
            if (printerManager.usbPrinterHelper != null) {
                String testText = "TEST PRINT\n" +
                        new SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault()).format(new Date()) + "\n" +
                        "Paper Commands Test";
                printerManager.usbPrinterHelper.tryAllPaperCommands(testText);
            }

            mainHandler.post(() -> {
                dismissProgressDialog();
                Toast.makeText(activity, "Paper commands test complete. Check printer.", Toast.LENGTH_LONG).show();
            });
        });
    }

    /**
     * Run ESC commands test
     */
    private void runESCCommands() {
        if (!printerManager.isPrinterConnected()) {
            Toast.makeText(activity, "Printer not connected", Toast.LENGTH_SHORT).show();
            return;
        }

        showProgressDialog("Sending ESC commands...");

        executor.submit(() -> {
            boolean result = false;
            if (printerManager.usbPrinterHelper != null) {
                result = printerManager.usbPrinterHelper.sendESCCommands();
            }

            final boolean finalResult = result;
            mainHandler.post(() -> {
                dismissProgressDialog();
                if (finalResult) {
                    Toast.makeText(activity, "✅ ESC commands sent", Toast.LENGTH_LONG).show();
                } else {
                    Toast.makeText(activity, "❌ ESC commands failed", Toast.LENGTH_LONG).show();
                }
            });
        });
    }

    /**
     * Run PCL commands test
     */
    private void runCanonDirectPrint() {
        if (!printerManager.isPrinterConnected()) {
            Toast.makeText(activity, "Printer not connected", Toast.LENGTH_SHORT).show();
            return;
        }

        showProgressDialog("Canon direct print...");

        executor.submit(() -> {
            boolean result = false;
            if (printerManager.usbPrinterHelper != null) {
                String testText = "\n\n" +
                        "================================\n" +
                        "     CANON DIRECT PRINT TEST\n" +
                        "================================\n" +
                        "Date: " + new SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault()).format(new Date()) + "\n" +
                        "Printer: Canon USB\n" +
                        "================================\n" +
                        "This is a test page\n" +
                        "If you can read this,\n" +
                        "the printer is working!\n" +
                        "================================\n\n\n";

                result = printerManager.usbPrinterHelper.canonDirectPrint(testText);
            }

            final boolean finalResult = result;
            mainHandler.post(() -> {
                dismissProgressDialog();
                if (finalResult) {
                    Toast.makeText(activity, "✅ Canon direct print sent", Toast.LENGTH_LONG).show();
                } else {
                    Toast.makeText(activity, "❌ Canon direct print failed", Toast.LENGTH_LONG).show();
                }
            });
        });
    }

    /**
     * Direct USB Write Test
     */
    private void runDirectUsbWrite() {
        if (!printerManager.isPrinterConnected()) {
            Toast.makeText(activity, "Printer not connected", Toast.LENGTH_SHORT).show();
            return;
        }

        showProgressDialog("Writing directly to USB...");

        executor.submit(() -> {
            boolean result = false;
            if (printerManager.usbPrinterHelper != null) {
                // Create test data
                String testString = "\n\n" +
                        "================================\n" +
                        "     DIRECT USB WRITE TEST\n" +
                        "================================\n" +
                        "Time: " + new SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(new Date()) + "\n" +
                        "Date: " + new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(new Date()) + "\n" +
                        "================================\n" +
                        "This is a direct USB write test.\n" +
                        "If you can read this, USB writing works!\n" +
                        "================================\n\n\n";

                try {
                    byte[] data = testString.getBytes("UTF-8");
                    result = printerManager.usbPrinterHelper.directUsbWrite(data);
                } catch (Exception e) {
                    Log.e("USB", "Error: " + e.getMessage());
                }
            }

            final boolean finalResult = result;
            mainHandler.post(() -> {
                dismissProgressDialog();
                if (finalResult) {
                    Toast.makeText(activity, "✅ USB write successful", Toast.LENGTH_LONG).show();
                } else {
                    Toast.makeText(activity, "❌ USB write failed", Toast.LENGTH_LONG).show();
                }
            });
        });
    }
    private void runPCLCommandsTest() {
        if (!printerManager.isPrinterConnected()) {
            Toast.makeText(activity, "Printer not connected", Toast.LENGTH_SHORT).show();
            return;
        }

        showProgressDialog("Sending PCL commands...");

        executor.submit(() -> {
            boolean result = false;
            if (printerManager.usbPrinterHelper != null) {
                result = printerManager.usbPrinterHelper.sendPCLCommands();
            }

            final boolean finalResult = result;
            mainHandler.post(() -> {
                dismissProgressDialog();
                if (finalResult) {
                    Toast.makeText(activity, "✅ PCL commands sent", Toast.LENGTH_LONG).show();
                } else {
                    Toast.makeText(activity, "❌ PCL commands failed", Toast.LENGTH_LONG).show();
                }
            });
        });
    }




    // ============= ADD ALL THESE NEW METHODS =============

    /**
     * Run Canon style print test
     */
    private void runCanonStylePrint() {
        if (!printerManager.isPrinterConnected()) {
            Toast.makeText(activity, "Printer not connected", Toast.LENGTH_SHORT).show();
            return;
        }

        showProgressDialog("Trying Canon style print...");

        executor.submit(() -> {
            boolean result = false;
            if (printerManager.usbPrinterHelper != null) {
                String testText = "TEST PRINT\n" +
                        "Weighment System\n" +
                        new SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault()).format(new Date()) + "\n" +
                        "This is a Canon style test";

                result = printerManager.usbPrinterHelper.printCanonStyle(testText);
            }

            final boolean finalResult = result;
            mainHandler.post(() -> {
                dismissProgressDialog();
                if (finalResult) {
                    Toast.makeText(activity, "✅ Canon style print successful", Toast.LENGTH_LONG).show();
                } else {
                    Toast.makeText(activity, "❌ Canon style print failed", Toast.LENGTH_LONG).show();
                }
            });
        });
    }

    /**
     * Run PCL print test
     */
    private void runPCLPrint() {
        if (!printerManager.isPrinterConnected()) {
            Toast.makeText(activity, "Printer not connected", Toast.LENGTH_SHORT).show();
            return;
        }

        showProgressDialog("Trying PCL print...");

        executor.submit(() -> {
            boolean result = false;
            if (printerManager.usbPrinterHelper != null) {
                String testText = "TEST PRINT\nPCL Mode Test\n" +
                        new SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault()).format(new Date());
                result = printerManager.usbPrinterHelper.printPCL(testText);
            }

            final boolean finalResult = result;
            mainHandler.post(() -> {
                dismissProgressDialog();
                if (finalResult) {
                    Toast.makeText(activity, "✅ PCL print successful", Toast.LENGTH_LONG).show();
                } else {
                    Toast.makeText(activity, "❌ PCL print failed", Toast.LENGTH_LONG).show();
                }
            });
        });
    }

    /**
     * Run ASCII print test
     */
    private void runASCIIPrint() {
        if (!printerManager.isPrinterConnected()) {
            Toast.makeText(activity, "Printer not connected", Toast.LENGTH_SHORT).show();
            return;
        }

        showProgressDialog("Trying ASCII print...");

        executor.submit(() -> {
            boolean result = false;
            if (printerManager.usbPrinterHelper != null) {
                String testText = "TEST PRINT\nASCII Mode Test\nSimple text only\n" +
                        new SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault()).format(new Date());
                result = printerManager.usbPrinterHelper.printASCII(testText);
            }

            final boolean finalResult = result;
            mainHandler.post(() -> {
                dismissProgressDialog();
                if (finalResult) {
                    Toast.makeText(activity, "✅ ASCII print successful", Toast.LENGTH_LONG).show();
                } else {
                    Toast.makeText(activity, "❌ ASCII print failed", Toast.LENGTH_LONG).show();
                }
            });
        });
    }

    /**
     * Run comprehensive diagnostics
     */
    private void runComprehensiveDiagnostics() {
        showProgressDialog("Running comprehensive diagnostics...");

        new Thread(() -> {
            if (printerManager != null && printerManager.usbPrinterHelper != null) {
                printerManager.usbPrinterHelper.comprehensiveDiagnostics();
            }

            mainHandler.post(() -> {
                dismissProgressDialog();
                Toast.makeText(activity, "Diagnostics complete. Check Logcat for details.", Toast.LENGTH_LONG).show();
            });
        }).start();
    }

    /**
     * Check all USB devices connected to the system
     */
    private void checkUsbDevices() {
        UsbManager usbManager = (UsbManager) activity.getSystemService(Context.USB_SERVICE);
        HashMap<String, UsbDevice> deviceList = usbManager.getDeviceList();

        StringBuilder sb = new StringBuilder();
        sb.append("USB Devices found: ").append(deviceList.size()).append("\n\n");

        if (deviceList.isEmpty()) {
            sb.append("❌ NO USB DEVICES FOUND!\n");
            sb.append("Please check:\n");
            sb.append("1. Printer is powered on\n");
            sb.append("2. USB cable is connected\n");
            sb.append("3. Cable supports data transfer\n");
        } else {
            for (UsbDevice device : deviceList.values()) {
                sb.append("Device: ").append(device.getDeviceName()).append("\n");
                sb.append("  VID: 0x").append(Integer.toHexString(device.getVendorId())).append("\n");
                sb.append("  PID: 0x").append(Integer.toHexString(device.getProductId())).append("\n");
                sb.append("  Class: ").append(device.getDeviceClass()).append("\n");

                // Check if it might be a printer
                boolean isPrinter = false;
                if (device.getDeviceClass() == 7) {
                    isPrinter = true;
                    sb.append("  ✅ This is a printer device (Class 7)\n");
                }

                // Check vendor IDs for common printer manufacturers
                int vid = device.getVendorId();
                if (vid == 0x04A9 || vid == 0x04B8 || vid == 0x03F0 || vid == 0x0416) {
                    isPrinter = true;
                    sb.append("  ✅ This is from a known printer manufacturer\n");
                }

                boolean hasPermission = usbManager.hasPermission(device);
                sb.append("  Permission: ").append(hasPermission ? "✅ Granted" : "❌ Denied").append("\n");

                if (!isPrinter) {
                    sb.append("  ⚠️ This may not be a printer device\n");
                }

                sb.append("\n");
            }
        }

        // Log the full details
        Log.d("USB_CHECK", sb.toString());

        // Show summary in Toast
        String summary = "Found " + deviceList.size() + " USB devices. Check Logcat for details.";
        Toast.makeText(activity, summary, Toast.LENGTH_LONG).show();

        // Show dialog with basic info
        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        builder.setTitle("USB Devices")
                .setMessage(sb.toString())
                .setPositiveButton("OK", null)
                .setNeutralButton("Copy to Log", (dialog, which) -> {
                    Log.d("USB_DEVICES", sb.toString());
                    Toast.makeText(activity, "Copied to Logcat", Toast.LENGTH_SHORT).show();
                })
                .show();
    }

    /**
     * Diagnose printer connection
     */
    private void diagnosePrinter() {
        showProgressDialog("Diagnosing printer...");

        new Thread(() -> {
            if (printerManager != null) {
                printerManager.diagnosePrinterConnection();
            }

            mainHandler.post(() -> {
                dismissProgressDialog();
                Toast.makeText(activity, "Diagnosis complete. Check Logcat for details.", Toast.LENGTH_LONG).show();
            });
        }).start();
    }

    /**
     * Run simple test
     */
    private void runSimpleTest() {
        if (!printerManager.isPrinterConnected()) {
            Toast.makeText(activity, "Printer not connected", Toast.LENGTH_SHORT).show();
            return;
        }

        showProgressDialog("Running simple USB test...");

        executor.submit(() -> {
            boolean result = false;
            if (printerManager.usbPrinterHelper != null) {
                result = printerManager.usbPrinterHelper.simpleTestPrint();
            }

            final boolean finalResult = result;
            mainHandler.post(() -> {
                dismissProgressDialog();
                if (finalResult) {
                    Toast.makeText(activity, "✅ Simple test passed - printer responding", Toast.LENGTH_LONG).show();
                } else {
                    Toast.makeText(activity, "❌ Simple test failed - printer not responding", Toast.LENGTH_LONG).show();
                }
            });
        });
    }

    /**
     * Force reconnect printer
     */
    private void forceReconnect() {
        showProgressDialog("Force reconnecting...");

        new Thread(() -> {
            if (printerManager != null && printerManager.usbPrinterHelper != null) {
                printerManager.usbPrinterHelper.forceReconnect();
            }

            mainHandler.post(() -> {
                dismissProgressDialog();
                Toast.makeText(activity, "Reconnect attempt complete", Toast.LENGTH_SHORT).show();
            });
        }).start();
    }

    /**
     * Scan for printers
     */
    private void scanForPrinters() {
        Toast.makeText(activity, "Scanning for printers...", Toast.LENGTH_SHORT).show();

        printerManager.addListener(new PrinterManager.PrinterConnectionAdapter() {
            @Override
            public void onPrintersFound(List<UsbPrinterHelper.PrinterInfo> printers) {
                activity.runOnUiThread(() -> {
                    printerManager.removeListener(this);
                    showPrinterSelectionDialog(printers);
                });
            }

            @Override
            public void onDebugInfo(String info) {
                activity.runOnUiThread(() -> {
                    Toast.makeText(activity, info, Toast.LENGTH_SHORT).show();
                });
            }
        });

        printerManager.scanForPrinters();
    }

    /**
     * Show printer selection dialog
     */
    private void showPrinterSelectionDialog(List<UsbPrinterHelper.PrinterInfo> printers) {
        if (printers.isEmpty()) {
            Toast.makeText(activity, "No printers found", Toast.LENGTH_SHORT).show();
            return;
        }

        String[] printerNames = new String[printers.size()];
        for (int i = 0; i < printers.size(); i++) {
            printerNames[i] = printers.get(i).toString();
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        builder.setTitle("Select Printer")
                .setItems(printerNames, (dialog, which) -> {
                    UsbPrinterHelper.PrinterInfo selected = printers.get(which);
                    connectToPrinter(selected.device);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    /**
     * Connect to selected printer
     */
    private void connectToPrinter(UsbDevice printer) {
        showProgressDialog("Connecting to printer...");

        printerManager.addListener(new PrinterManager.PrinterConnectionAdapter() {
            @Override
            public void onPrinterConnected(String printerName) {
                activity.runOnUiThread(() -> {
                    dismissProgressDialog();
                    printerManager.removeListener(this);
                    Toast.makeText(activity, "✅ Connected to: " + printerName, Toast.LENGTH_LONG).show();
                    if (callback != null) {
                        callback.onPrinterSelected(printer);
                    }
                });
            }

            @Override
            public void onPrintError(String error) {
                activity.runOnUiThread(() -> {
                    dismissProgressDialog();
                    printerManager.removeListener(this);
                    Toast.makeText(activity, "❌ Connection failed: " + error, Toast.LENGTH_LONG).show();
                });
            }

            @Override
            public void onPermissionDenied() {
                activity.runOnUiThread(() -> {
                    dismissProgressDialog();
                    printerManager.removeListener(this);
                    Toast.makeText(activity, "❌ USB permission denied", Toast.LENGTH_LONG).show();
                });
            }
        });

        printerManager.connectToPrinter(printer);
    }

    /**
     * Confirm disconnect printer
     */
    private void confirmDisconnectPrinter() {
        if (!printerManager.isPrinterConnected()) {
            Toast.makeText(activity, "No printer connected", Toast.LENGTH_SHORT).show();
            return;
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        builder.setTitle("Disconnect Printer")
                .setMessage("Are you sure you want to disconnect the current printer?")
                .setPositiveButton("Disconnect", (dialog, which) -> {
                    printerManager.disconnectPrinter();
                    Toast.makeText(activity, "Printer disconnected", Toast.LENGTH_SHORT).show();
                    if (callback != null) {
                        callback.onPrinterDisconnected();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    /**
     * Show printer status
     */
    private void showPrinterStatus() {
        if (!printerManager.isPrinterConnected()) {
            Toast.makeText(activity, "No printer connected", Toast.LENGTH_SHORT).show();
            return;
        }

        String status = "Printer: " + printerManager.getConnectedPrinterName() + "\n" +
                "Type: " + printerManager.getCurrentPrinterType() + "\n" +
                "Status: Connected";

        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        builder.setTitle("Printer Status")
                .setMessage(status)
                .setPositiveButton("OK", null)
                .show();
    }

    /**
     * Show test print dialog
     */
    private void showTestPrintDialog() {
        if (!printerManager.isPrinterConnected()) {
            Toast.makeText(activity, "No printer connected", Toast.LENGTH_SHORT).show();
            return;
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        builder.setTitle("Test Print");

        final EditText input = new EditText(activity);
        String defaultText = "Test Print\nWeighment System\n" +
                new SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault()).format(new Date());
        input.setText(defaultText);
        input.setSelection(input.getText().length());

        builder.setView(input);
        builder.setPositiveButton("Print", (dialog, which) -> {
            String text = input.getText().toString();
            performTestPrint(text);
        });
        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    /**
     * Perform test print
     */
    private void performTestPrint(String text) {
        if (!printerManager.isPrinterConnected()) {
            Toast.makeText(activity, "Printer not connected", Toast.LENGTH_SHORT).show();
            return;
        }

        ProgressDialog progressDialog = new ProgressDialog(activity);
        progressDialog.setMessage("Sending to printer...\n(This may take a few seconds)");
        progressDialog.setCancelable(true);
        progressDialog.setButton(DialogInterface.BUTTON_NEGATIVE, "Cancel",
                (dialog, which) -> {
                    printerManager.cancelCurrentPrint();
                    dialog.dismiss();
                    Toast.makeText(activity, "Print cancelled", Toast.LENGTH_SHORT).show();
                });
        progressDialog.show();

        Handler timeoutHandler = new Handler(Looper.getMainLooper());
        Runnable timeoutRunnable = () -> {
            if (progressDialog.isShowing()) {
                progressDialog.dismiss();
                printerManager.cancelCurrentPrint();
                Toast.makeText(activity, "❌ Print timeout - printer not responding", Toast.LENGTH_LONG).show();
            }
        };
        timeoutHandler.postDelayed(timeoutRunnable, 30000);

        PrinterManager.PrinterConnectionListener listener = new PrinterManager.PrinterConnectionAdapter() {
            @Override
            public void onPrintSuccess() {
                timeoutHandler.removeCallbacks(timeoutRunnable);
                if (progressDialog.isShowing()) {
                    progressDialog.dismiss();
                }
                Toast.makeText(activity, "✅ Test print successful", Toast.LENGTH_LONG).show();
                if (callback != null) {
                    callback.onTestPrint();
                }
            }

            @Override
            public void onPrintError(String error) {
                timeoutHandler.removeCallbacks(timeoutRunnable);
                if (progressDialog.isShowing()) {
                    progressDialog.dismiss();
                }
                Toast.makeText(activity, "❌ Test print failed: " + error, Toast.LENGTH_LONG).show();
            }

            @Override
            public void onDebugInfo(String info) {
                Log.d("PrinterTest", "Debug: " + info);
            }
        };

        printerManager.autoDetectAndPrintAsync(text, listener);
    }

    /**
     * Show progress dialog
     */
    private void showProgressDialog(String message) {
        if (progressDialog == null) {
            progressDialog = new ProgressDialog(activity);
            progressDialog.setCancelable(false);
        }
        progressDialog.setMessage(message);
        progressDialog.show();
    }

    /**
     * Dismiss progress dialog
     */
    private void dismissProgressDialog() {
        if (progressDialog != null && progressDialog.isShowing()) {
            progressDialog.dismiss();
        }
    }

    /**
     * Clean up resources
     */
    public void shutdown() {
        if (executor != null) {
            executor.shutdownNow();
        }
    }
}