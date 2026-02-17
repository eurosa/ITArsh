package com.googleapi.bluetoothweight;

// ConnectionStatusViewModel.java
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

public class ConnectionStatusViewModel extends ViewModel {
    private final MutableLiveData<ConnectionStatus> connectionStatus = new MutableLiveData<>();

    public LiveData<ConnectionStatus> getConnectionStatus() {
        return connectionStatus;
    }

    public void updateStatus(int code, String message) {
        connectionStatus.postValue(new ConnectionStatus(code, message));
    }

    public static class ConnectionStatus {
        public final int statusCode;
        public final String message;

        public ConnectionStatus(int statusCode, String message) {
            this.statusCode = statusCode;
            this.message = message;
        }
    }
}