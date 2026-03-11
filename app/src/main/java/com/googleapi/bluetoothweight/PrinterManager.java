package com.googleapi.bluetoothweight;

import android.content.Context;
import android.hardware.usb.UsbDevice;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

public class PrinterManager {
    private static final String TAG = "PrinterManager";
    private static PrinterManager instance;

    public UsbPrinterHelper usbPrinterHelper;
    private boolean isPrinterConnected = false;
    private UsbDevice currentPrinterDevice;
    private UsbPrinterHelper.PrinterType currentPrinterType = UsbPrinterHelper.PrinterType.UNKNOWN;
    public List<PrinterConnectionListener> listeners = new ArrayList<>();
    private Context appContext;

    private String connectedPrinterName = "";
    private List<UsbPrinterHelper.PrinterInfo> availablePrinters = new ArrayList<>();

    // Background execution
    private ExecutorService printerExecutor = Executors.newSingleThreadExecutor();
    private Handler mainHandler = new Handler(Looper.getMainLooper());
    private Future<?> currentPrintJob = null;

    public static synchronized PrinterManager getInstance() {
        if (instance == null) {
            instance = new PrinterManager();
        }
        return instance;
    }

    private PrinterManager() {
    }

    public void init(Context context) {
        this.appContext = context.getApplicationContext();

        if (usbPrinterHelper == null) {
            usbPrinterHelper = new UsbPrinterHelper(appContext, new UsbPrinterHelper.PrinterCallback() {
                @Override
                public void onPrinterConnected() {
                    mainHandler.post(() -> {
                        isPrinterConnected = true;
                        currentPrinterType = usbPrinterHelper.getCurrentPrinterType();
                        connectedPrinterName = getPrinterName(currentPrinterDevice);
                        notifyPrinterConnected();
                        Log.d(TAG, "Printer connected: " + currentPrinterType);
                    });
                }

                @Override
                public void onPrinterDisconnected() {
                    mainHandler.post(() -> {
                        isPrinterConnected = false;
                        currentPrinterDevice = null;
                        currentPrinterType = UsbPrinterHelper.PrinterType.UNKNOWN;
                        connectedPrinterName = "";
                        notifyPrinterDisconnected();
                        Log.d(TAG, "Printer disconnected");
                    });
                }

                @Override
                public void onPrintSuccess() {
                    mainHandler.post(() -> {
                        notifyPrintSuccess();
                        Log.d(TAG, "Print successful");
                    });
                }

                @Override
                public void onPrintError(String error) {
                    mainHandler.post(() -> {
                        notifyPrintError(error);
                        Log.e(TAG, "Print error: " + error);
                    });
                }

                @Override
                public void onPermissionDenied() {
                    mainHandler.post(() -> {
                        notifyPermissionDenied();
                        Log.e(TAG, "Permission denied");
                    });
                }

                @Override
                public void onDebugInfo(String info) {
                    Log.d(TAG, "Debug: " + info);
                    mainHandler.post(() -> {
                        notifyDebugInfo(info);
                    });
                }

                @Override
                public void onPrintersFound(List<UsbPrinterHelper.PrinterInfo> printers) {
                    mainHandler.post(() -> {
                        availablePrinters.clear();
                        availablePrinters.addAll(printers);
                        notifyPrintersFound(printers);
                        Log.d(TAG, "Printers found: " + printers.size());
                    });
                }
            });
        }
    }

    private String getPrinterName(UsbDevice device) {
        if (device == null) return "Unknown";

        for (UsbPrinterHelper.PrinterInfo info : availablePrinters) {
            if (info.device.equals(device)) {
                return info.toString();
            }
        }
        return String.format("Printer (0x%04X)", device.getVendorId());
    }

    public void registerReceiver() {
        if (usbPrinterHelper != null) {
            usbPrinterHelper.registerReceiver();
        }
    }

    public void unregisterReceiver() {
        if (usbPrinterHelper != null) {
            usbPrinterHelper.unregisterReceiver();
        }
    }

    public void scanForPrinters() {
        if (usbPrinterHelper != null) {
            availablePrinters.clear();
            usbPrinterHelper.scanForPrinters();
        }
    }

    public void connectToPrinter(UsbDevice device) {
        if (usbPrinterHelper != null) {
            currentPrinterDevice = device;
            usbPrinterHelper.connectToPrinter(device);
        }
    }

    public void disconnectPrinter() {
        cancelCurrentPrint();

        if (usbPrinterHelper != null) {
            usbPrinterHelper.disconnect();
            isPrinterConnected = false;
            currentPrinterDevice = null;
            currentPrinterType = UsbPrinterHelper.PrinterType.UNKNOWN;
            connectedPrinterName = "";
        }
    }

    /**
     * Async print methods - these won't block the UI
     */
    public void printTextAsync(String text, PrinterConnectionListener listener) {
        if (listener != null) {
            addListener(listener);
        }

        // Cancel any ongoing print job
        cancelCurrentPrint();

        currentPrintJob = printerExecutor.submit(() -> {
            boolean result = false;
            String errorMsg = null;

            try {
                Log.d(TAG, "Starting async printText");
                if (usbPrinterHelper != null && isPrinterConnected) {
                    result = usbPrinterHelper.printText(text);
                } else {
                    errorMsg = "Printer not connected";
                }
            } catch (Exception e) {
                Log.e(TAG, "Print error", e);
                errorMsg = e.getMessage();
            }

            final boolean finalResult = result;
            final String finalError = errorMsg;

            mainHandler.post(() -> {
                if (finalError != null) {
                    if (listener != null) {
                        listener.onPrintError(finalError);
                        removeListener(listener);
                    }
                } else if (finalResult) {
                    if (listener != null) {
                        listener.onPrintSuccess();
                        removeListener(listener);
                    }
                } else {
                    if (listener != null) {
                        listener.onPrintError("Print failed");
                        removeListener(listener);
                    }
                }
            });
        });
    }

    public void autoDetectAndPrintAsync(String text, PrinterConnectionListener listener) {
        if (listener != null) {
            addListener(listener);
        }

        // Cancel any ongoing print job
        cancelCurrentPrint();

        currentPrintJob = printerExecutor.submit(() -> {
            boolean result = false;
            String errorMsg = null;

            try {
                Log.d(TAG, "Starting async autoDetectAndPrint");
                if (usbPrinterHelper != null && isPrinterConnected) {
                    result = usbPrinterHelper.autoDetectAndPrint(text);
                } else {
                    errorMsg = "Printer not connected";
                }
            } catch (Exception e) {
                Log.e(TAG, "Auto-print error", e);
                errorMsg = e.getMessage();
            }

            final boolean finalResult = result;
            final String finalError = errorMsg;

            mainHandler.post(() -> {
                if (finalError != null) {
                    if (listener != null) {
                        listener.onPrintError(finalError);
                        removeListener(listener);
                    }
                } else if (finalResult) {
                    if (listener != null) {
                        listener.onPrintSuccess();
                        removeListener(listener);
                    }
                } else {
                    if (listener != null) {
                        listener.onPrintError("Auto-print failed");
                        removeListener(listener);
                    }
                }
            });
        });
    }

    public void printPlainTextAsync(String text, PrinterConnectionListener listener) {
        if (listener != null) {
            addListener(listener);
        }

        // Cancel any ongoing print job
        cancelCurrentPrint();

        currentPrintJob = printerExecutor.submit(() -> {
            boolean result = false;
            String errorMsg = null;

            try {
                Log.d(TAG, "Starting async printPlainText");
                if (usbPrinterHelper != null && isPrinterConnected) {
                    result = usbPrinterHelper.printPlainText(text);
                } else {
                    errorMsg = "Printer not connected";
                }
            } catch (Exception e) {
                Log.e(TAG, "Plain print error", e);
                errorMsg = e.getMessage();
            }

            final boolean finalResult = result;
            final String finalError = errorMsg;

            mainHandler.post(() -> {
                if (finalError != null) {
                    if (listener != null) {
                        listener.onPrintError(finalError);
                        removeListener(listener);
                    }
                } else if (finalResult) {
                    if (listener != null) {
                        listener.onPrintSuccess();
                        removeListener(listener);
                    }
                } else {
                    if (listener != null) {
                        listener.onPrintError("Plain print failed");
                        removeListener(listener);
                    }
                }
            });
        });
    }

    /**
     * Synchronous methods (original) - these will block until complete
     */
    public boolean printText(String text) {
        if (usbPrinterHelper != null && isPrinterConnected) {
            return usbPrinterHelper.printText(text);
        }
        return false;
    }

    public boolean printPlainText(String text) {
        if (usbPrinterHelper != null && isPrinterConnected) {
            return usbPrinterHelper.printPlainText(text);
        }
        return false;
    }

    public boolean autoDetectAndPrint(String text) {
        if (usbPrinterHelper != null && isPrinterConnected) {
            return usbPrinterHelper.autoDetectAndPrint(text);
        }
        return false;
    }

    /**
     * Cancel current print job
     */
    public void cancelCurrentPrint() {
        if (currentPrintJob != null && !currentPrintJob.isDone()) {
            Log.d(TAG, "Cancelling current print job");
            currentPrintJob.cancel(true);
        }
    }

    /**
     * Check if printer is connected
     */
    public boolean isPrinterConnected() {
        return isPrinterConnected && usbPrinterHelper != null && usbPrinterHelper.isConnected();
    }

    /**
     * Get current printer type
     */
    public UsbPrinterHelper.PrinterType getCurrentPrinterType() {
        return currentPrinterType;
    }

    /**
     * Get connected printer name
     */
    public String getConnectedPrinterName() {
        return connectedPrinterName;
    }

    /**
     * Get available printers list
     */
    public List<UsbPrinterHelper.PrinterInfo> getAvailablePrinters() {
        return availablePrinters;
    }

    /**
     * Add connection listener
     */
    public void addListener(PrinterConnectionListener listener) {
        if (!listeners.contains(listener)) {
            listeners.add(listener);
        }
    }

    /**
     * Remove connection listener
     */
    public void removeListener(PrinterConnectionListener listener) {
        listeners.remove(listener);
    }

    /**
     * Diagnose printer connection
     */
    public void diagnosePrinterConnection() {
        Log.d(TAG, "========== PRINTER MANAGER DIAGNOSTICS ==========");
        Log.d(TAG, "isPrinterConnected: " + isPrinterConnected);
        Log.d(TAG, "currentPrinterDevice: " + (currentPrinterDevice != null));
        Log.d(TAG, "currentPrinterType: " + currentPrinterType);
        Log.d(TAG, "connectedPrinterName: " + connectedPrinterName);

        if (usbPrinterHelper != null) {
           // usbPrinterHelper.diagnoseConnection();
        } else {
            Log.e(TAG, "usbPrinterHelper is null");
        }

        Log.d(TAG, "=================================================");
    }

    /**
     * Test direct print
     */
    public boolean testDirectPrint(String text) {
        Log.d(TAG, "========== DIRECT PRINT TEST ==========");
        Log.d(TAG, "isPrinterConnected: " + isPrinterConnected);
        Log.d(TAG, "usbPrinterHelper: " + (usbPrinterHelper != null));

        if (!isPrinterConnected || usbPrinterHelper == null) {
            Log.e(TAG, "Printer not connected or helper null");
            return false;
        }

        boolean result = usbPrinterHelper.printPlainText(text);
        Log.d(TAG, "Direct print result: " + result);
        Log.d(TAG, "========================================");
        return result;
    }

    /**
     * Shutdown executor service
     */
    public void shutdown() {
        Log.d(TAG, "Shutting down PrinterManager");
        cancelCurrentPrint();

        if (printerExecutor != null) {
            printerExecutor.shutdown();
            try {
                if (!printerExecutor.awaitTermination(5000, TimeUnit.MILLISECONDS)) {
                    printerExecutor.shutdownNow();
                }
            } catch (InterruptedException e) {
                printerExecutor.shutdownNow();
            }
        }

        disconnectPrinter();
    }

    // Notification methods
    private void notifyPrinterConnected() {
        for (PrinterConnectionListener listener : listeners) {
            listener.onPrinterConnected(connectedPrinterName);
        }
    }

    private void notifyPrinterDisconnected() {
        for (PrinterConnectionListener listener : listeners) {
            listener.onPrinterDisconnected();
        }
    }

    private void notifyPrintSuccess() {
        for (PrinterConnectionListener listener : listeners) {
            listener.onPrintSuccess();
        }
    }

    private void notifyPrintError(String error) {
        for (PrinterConnectionListener listener : listeners) {
            listener.onPrintError(error);
        }
    }

    private void notifyPermissionDenied() {
        for (PrinterConnectionListener listener : listeners) {
            listener.onPermissionDenied();
        }
    }

    private void notifyDebugInfo(String info) {
        for (PrinterConnectionListener listener : listeners) {
            listener.onDebugInfo(info);
        }
    }

    private void notifyPrintersFound(List<UsbPrinterHelper.PrinterInfo> printers) {
        for (PrinterConnectionListener listener : listeners) {
            listener.onPrintersFound(printers);
        }
    }

    // Listener interfaces
    public interface PrinterConnectionListener {
        void onPrinterConnected(String printerName);
        void onPrinterDisconnected();
        void onPrintSuccess();
        void onPrintError(String error);
        void onPermissionDenied();
        void onDebugInfo(String info);
        void onPrintersFound(List<UsbPrinterHelper.PrinterInfo> printers);
    }

    public static class PrinterConnectionAdapter implements PrinterConnectionListener {
        @Override public void onPrinterConnected(String printerName) {}
        @Override public void onPrinterDisconnected() {}
        @Override public void onPrintSuccess() {}
        @Override public void onPrintError(String error) {}
        @Override public void onPermissionDenied() {}
        @Override public void onDebugInfo(String info) {}
        @Override public void onPrintersFound(List<UsbPrinterHelper.PrinterInfo> printers) {}
    }
}