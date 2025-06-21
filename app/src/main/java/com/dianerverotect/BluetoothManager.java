package com.dianerverotect;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
// Removed BluetoothManager import to avoid naming conflict
import android.bluetooth.BluetoothProfile;
import android.bluetooth.BluetoothSocket;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Handler;
import android.os.ParcelUuid;
import android.util.Log;
import android.widget.Toast;

import androidx.core.app.ActivityCompat;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import com.dianerverotect.alerts.NotificationHelper;
import com.dianerverotect.alerts.ReminderPreferences;

public class BluetoothManager {
    private static final String TAG = "BluetoothManager";
    
    // BLE UUIDs for DianervProtect device
    private static final String SERVICE_UUID = "6E400001-B5A3-F393-E0A9-E50E24DCCA9E";
    private static final String CHARACTERISTIC_UUID_RX = "6E400002-B5A3-F393-E0A9-E50E24DCCA9E";
    private static final String CHARACTERISTIC_UUID_TX = "6E400003-B5A3-F393-E0A9-E50E24DCCA9E";
    
    // Device name
    private static final String DEVICE_NAME = "DianervProtect";
    
    // Legacy Bluetooth UUID (for fallback)
    private static final UUID SERIAL_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    private Context context;
    private android.bluetooth.BluetoothManager bluetoothManagerSystem;
    private BluetoothAdapter mBluetoothAdapter;
    private BluetoothLeScanner mBluetoothLeScanner;
    private BluetoothGatt mBluetoothGatt;
    private BluetoothSocket mBluetoothSocket; // For legacy connection
    private InputStream mInputStream; // For legacy connection
    private NotificationHelper notificationHelper;
    private ReminderPreferences reminderPreferences;
    private boolean isConnected = false;
    private boolean isScanning = false;
    private int batteryLevel = 100;
    private static final int LOW_BATTERY_THRESHOLD = 20;
    
    // Sensor data values
    private float emgValue = 0.0f;
    private float temperatureValue = 0.0f;
    private float pressureValue = 0.0f;
    private Handler mHandler = new Handler();
    
    /**
     * Get the current EMG value
     * @return The current EMG value in microvolts
     */
    public float getEmgValue() {
        return emgValue;
    }
    
    /**
     * Get the current temperature value
     * @return The current temperature value in degrees Celsius
     */
    public float getTemperatureValue() {
        return temperatureValue;
    }
    
    /**
     * Get the current pressure value
     * @return The current pressure value in kilopascals
     */
    public float getPressureValue() {
        return pressureValue;
    }
    
    /**
     * Get the current battery level
     * @return The current battery level as a percentage (0-100)
     */
    public int getBatteryLevel() {
        return batteryLevel;
    }
    
    /**
     * Set the battery level
     * @param level The battery level as a percentage (0-100)
     */
    public void setBatteryLevel(int level) {
        if (level >= 0 && level <= 100) {
            this.batteryLevel = level;
        }
    }

    public interface DataListener {
        void onDataReceived(String str);
        void onEmgDataReceived(float value);
        void onTemperatureDataReceived(float value);
        void onPressureDataReceived(float value);
    }

    public interface ConnectionListener {
        void onConnectionStateChanged(boolean isConnected);
    }

    private ConnectionListener connectionListener;

    public BluetoothManager(Context context) {
        this.context = context;
        
        // Initialize notification helpers
        this.notificationHelper = new NotificationHelper(context);
        this.reminderPreferences = new ReminderPreferences(context);
        
        // First check if Bluetooth is supported by the device hardware
        if (!context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH)) {
            Toast.makeText(context, "Bluetooth is not supported on this device", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // Get Bluetooth manager and adapter - using version-specific approach
        initializeBluetoothAdapter(context);
        
        // Only show toast if we've confirmed hardware support but adapter initialization failed
        if (this.mBluetoothAdapter == null) {
            Log.e(TAG, "Failed to initialize Bluetooth adapter despite hardware support");
        }
    }
    
    /**
     * Initialize Bluetooth adapter using version-specific approach
     * This method handles different Android versions appropriately
     */
    private void initializeBluetoothAdapter(Context context) {
        try {
            // First try to get the BluetoothManager service
            this.bluetoothManagerSystem = (android.bluetooth.BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE);
            
            // For Android 12+ (API 31+)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                // Even without permission, we can still initialize the adapter
                // We'll just get a SecurityException later if we try to use it without permission
                if (bluetoothManagerSystem != null) {
                    this.mBluetoothAdapter = bluetoothManagerSystem.getAdapter();
                    // Only try to get the scanner if we have permission
                    if (ActivityCompat.checkSelfPermission(context, android.Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED) {
                        this.mBluetoothLeScanner = mBluetoothAdapter != null ? mBluetoothAdapter.getBluetoothLeScanner() : null;
                    }
                }
            }
            // For Android 6.0 to Android 11 (API 23-30)
            else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                // We can initialize the adapter without permissions
                if (bluetoothManagerSystem != null) {
                    this.mBluetoothAdapter = bluetoothManagerSystem.getAdapter();
                    this.mBluetoothLeScanner = mBluetoothAdapter != null ? mBluetoothAdapter.getBluetoothLeScanner() : null;
                }
            }
            // For older Android versions (below API 23)
            else {
                if (bluetoothManagerSystem != null) {
                    this.mBluetoothAdapter = bluetoothManagerSystem.getAdapter();
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP && mBluetoothAdapter != null) {
                        this.mBluetoothLeScanner = mBluetoothAdapter.getBluetoothLeScanner();
                    }
                }
            }
            
            // Samsung-specific workaround - some Samsung devices need this fallback
            if (this.mBluetoothAdapter == null) {
                Log.d(TAG, "Trying fallback method for Samsung devices");
                this.mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
            }
            
            // Log the result
            if (this.mBluetoothAdapter != null) {
                Log.d(TAG, "Successfully initialized Bluetooth adapter");
            } else {
                Log.e(TAG, "Failed to initialize Bluetooth adapter");
            }
            
        } catch (Exception e) {
            Log.e(TAG, "Error initializing Bluetooth adapter: " + e.getMessage(), e);
            // Fallback to direct adapter access
            try {
                this.mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
                Log.d(TAG, "Fallback initialization " + (this.mBluetoothAdapter != null ? "succeeded" : "failed"));
            } catch (Exception ex) {
                Log.e(TAG, "Fallback Bluetooth initialization failed: " + ex.getMessage(), ex);
            }
        }
    }

    public void connectToDevice(final String deviceName, final Runnable onSuccess, final Runnable onFail) {
        new Thread(new Runnable() { // from class: com.example.dodi2.-$$Lambda$BluetoothManager$SMg6BST17T-zRtgKEQooZ8JoxTU
            @Override // java.lang.Runnable
            public final void run() {
                BluetoothManager.this.connectToDeviceBluetoothManager(deviceName, onSuccess, onFail);
            }
        }).start();
    }

    public void connectToDeviceBluetoothManager(String deviceName, Runnable onSuccess, Runnable onFail) {
        BluetoothDevice device = findDeviceByName(deviceName);
        if (device != null) {
            try {
                // Check for appropriate permissions based on Android version
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    // Android 12+ requires BLUETOOTH_CONNECT permission
                    if (ActivityCompat.checkSelfPermission(context, android.Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                        Log.e(TAG, "Missing BLUETOOTH_CONNECT permission");
                        handleConnectionStateChange(false);
                        ((MainActivity) this.context).runOnUiThread(onFail);
                        return;
                    }
                } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    // Android 6-11 requires BLUETOOTH permission
                    if (ActivityCompat.checkSelfPermission(context, android.Manifest.permission.BLUETOOTH) != PackageManager.PERMISSION_GRANTED) {
                        Log.e(TAG, "Missing BLUETOOTH permission");
                        handleConnectionStateChange(false);
                        ((MainActivity) this.context).runOnUiThread(onFail);
                        return;
                    }
                }
                
                // Cancel discovery before connecting
                if (this.mBluetoothAdapter != null && this.mBluetoothAdapter.isDiscovering()) {
                    this.mBluetoothAdapter.cancelDiscovery();
                }
                
                // Create socket and connect
                this.mBluetoothSocket = device.createRfcommSocketToServiceRecord(SERIAL_UUID);
                this.mBluetoothSocket.connect();
                InputStream inputStream = this.mBluetoothSocket.getInputStream();
                this.mInputStream = inputStream;
                
                if (inputStream != null) {
                    handleConnectionStateChange(true);
                    ((MainActivity) this.context).runOnUiThread(onSuccess);
                } else {
                    handleConnectionStateChange(false);
                    ((MainActivity) this.context).runOnUiThread(onFail);
                }
            } catch (SecurityException se) {
                Log.e(TAG, "Security exception during Bluetooth connection", se);
                closeSocket();
                handleConnectionStateChange(false);
                ((MainActivity) this.context).runOnUiThread(onFail);
            } catch (IOException e) {
                Log.e(TAG, "Connection failed", e);
                closeSocket();
                handleConnectionStateChange(false);
                ((MainActivity) this.context).runOnUiThread(onFail);
            } catch (Exception e) {
                Log.e(TAG, "Unexpected error during connection", e);
                closeSocket();
                handleConnectionStateChange(false);
                ((MainActivity) this.context).runOnUiThread(onFail);
            }
        } else {
            Log.e(TAG, "Device not found: " + deviceName);
            handleConnectionStateChange(false);
            ((MainActivity) this.context).runOnUiThread(onFail);
        }
    }

    public void sendMessage(final String message) {
        if (this.mBluetoothSocket != null) {
            new Thread(new Runnable() { // from class: com.example.dodi2.-$$Lambda$BluetoothManager$E48jVOP131st-pQtlEWtp7EuDWk
                @Override // java.lang.Runnable
                public final void run() {
                    BluetoothManager.this.lambda$sendMessage$1$BluetoothManager(message);
                }
            }).start();
        }
    }

    public /* synthetic */ void lambda$sendMessage$1$BluetoothManager(String message) {
        try {
            this.mBluetoothSocket.getOutputStream().write(message.getBytes());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void closeSocket() {
        try {
            BluetoothSocket bluetoothSocket = this.mBluetoothSocket;
            if (bluetoothSocket != null) {
                bluetoothSocket.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        handleConnectionStateChange(false);
    }

    public void startListening(final Handler handler, final DataListener dataListener) {
        new Thread(new Runnable() { // from class: com.example.dodi2.-$$Lambda$BluetoothManager$r1BOHaFawgs1vVWKRcOzRSKnilo
            @Override // java.lang.Runnable
            public final void run() {
                BluetoothManager.this.lambda$startListening$3$BluetoothManager(handler, dataListener);
            }
        }).start();
    }

    public /* synthetic */ void lambda$startListening$3$BluetoothManager(Handler handler, final DataListener dataListener) {
        byte[] buffer = new byte[1024];
        while (!Thread.currentThread().isInterrupted()) {
            try {
                if (this.mInputStream != null && this.mInputStream.available() > 0) {
                    int bytes = this.mInputStream.read(buffer);
                    final String data = new String(buffer, 0, bytes).trim();
                    handler.post(new Runnable() {
                        @Override
                        public final void run() {
                            dataListener.onDataReceived(data);
                            // Process the data to extract sensor values
                            processGloveData(data, dataListener);
                        }
                    });
                }
            } catch (IOException e) {
                Log.e(TAG, "Error reading data", e);
                return;
            }
        }
    }

    private BluetoothDevice findDeviceByName(String deviceName) {
        try {
            // Check for appropriate permissions based on Android version
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                // Android 12+ requires BLUETOOTH_CONNECT permission
                if (ActivityCompat.checkSelfPermission(context, android.Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                    Log.e(TAG, "Missing BLUETOOTH_CONNECT permission for device discovery");
                    return null;
                }
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                // Android 6-11 requires BLUETOOTH permission
                if (ActivityCompat.checkSelfPermission(context, android.Manifest.permission.BLUETOOTH) != PackageManager.PERMISSION_GRANTED) {
                    Log.e(TAG, "Missing BLUETOOTH permission for device discovery");
                    return null;
                }
            }
            
            // Proceed with getting paired devices if we have the adapter and permissions
            if (this.mBluetoothAdapter != null) {
                Set<BluetoothDevice> pairedDevices = this.mBluetoothAdapter.getBondedDevices();
                if (pairedDevices != null) {
                    for (BluetoothDevice device : pairedDevices) {
                        String name = device.getName();
                        if (name != null && name.equals(deviceName)) {
                            return device;
                        }
                    }
                }
            }
        } catch (SecurityException se) {
            Log.e(TAG, "Security exception during device discovery", se);
        } catch (Exception e) {
            Log.e(TAG, "Error finding device by name", e);
        }
        return null;
    }

    /**
     * Handle device connection state change
     */
    private void handleConnectionStateChange(boolean isConnected) {
        // Update the connection state
        this.isConnected = isConnected;
        
        // Notify listeners
        if (connectionListener != null) {
            connectionListener.onConnectionStateChanged(isConnected);
        }
        
        // Show notification if enabled in preferences
        if (reminderPreferences.isGloveConnectionAlertsEnabled()) {
            notificationHelper.showGloveConnectionNotification(isConnected);
        }
        
        // Log connection state
        Log.d(TAG, "Glove connection state: " + (isConnected ? "Connected" : "Disconnected"));
    }

    public void connect() {
        // Try to connect to the last known device or the default device
        if (mBluetoothAdapter != null) {
            try {
                // Check for appropriate permissions based on Android version
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    // Android 12+ requires BLUETOOTH_CONNECT permission
                    if (ActivityCompat.checkSelfPermission(context, android.Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                        Log.e(TAG, "Missing BLUETOOTH_CONNECT permission for connection");
                        return;
                    }
                } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    // Android 6-11 requires BLUETOOTH permission
                    if (ActivityCompat.checkSelfPermission(context, android.Manifest.permission.BLUETOOTH) != PackageManager.PERMISSION_GRANTED) {
                        Log.e(TAG, "Missing BLUETOOTH permission for connection");
                        return;
                    }
                }
                
                // Try to find and connect to the DianervProtect device
                BluetoothDevice device = findDeviceByName(DEVICE_NAME);
                if (device != null) {
                    connectToDevice(DEVICE_NAME, 
                        () -> Log.d(TAG, "Connected to DianervProtect device"), 
                        () -> Log.e(TAG, "Failed to connect to DianervProtect device")
                    );
                } else {
                    Log.e(TAG, "DianervProtect device not found in paired devices");
                }
            } catch (Exception e) {
                Log.e(TAG, "Error during connect", e);
            }
        } else {
            Log.e(TAG, "Bluetooth adapter is not initialized");
        }
    }
    
    public void disconnect() {
        try {
            // Check for appropriate permissions based on Android version
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (ActivityCompat.checkSelfPermission(context, android.Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                    Log.e(TAG, "Missing BLUETOOTH_CONNECT permission for disconnection");
                    return;
                }
            }
            
            // Close Bluetooth GATT if it exists
            if (mBluetoothGatt != null) {
                mBluetoothGatt.close();
                mBluetoothGatt = null;
            }
            
            // Close socket
            closeSocket();
            
            // When disconnection is complete:
            handleConnectionStateChange(false);
            Log.d(TAG, "Disconnected from Bluetooth device");
        } catch (Exception e) {
            Log.e(TAG, "Error during disconnect", e);
        }
    }

    /**
     * Handle battery level update
     * @param level Battery level percentage (0-100)
     */
    public void handleBatteryLevelUpdate(int level) {
        // Update the stored battery level
        this.batteryLevel = level;
        
        // Check if battery is low and show notification if needed
        if (level <= LOW_BATTERY_THRESHOLD) {
            // Show low battery notification if technical alerts are enabled
            if (reminderPreferences.isTechnicalAlertsEnabled()) {
                notificationHelper.showLowBatteryNotification(level);
            }
            
            // Log battery level
            Log.d(TAG, "Glove battery level low: " + level + "%");
        }
    }
    
    /**
     * Process data received from the DianervProtect device
     * Different data types are identified by specific prefixes
     */
    public void processGloveData(String data, DataListener dataListener) {
        if (data.startsWith("BAT:")) {
            // Battery level update
            try {
                int level = Integer.parseInt(data.substring(4).trim());
                handleBatteryLevelUpdate(level);
            } catch (NumberFormatException e) {
                Log.e(TAG, "Error parsing battery level: " + e.getMessage());
            }
        } else if (data.startsWith("EMG:")) {
            // EMG data
            try {
                float value = Float.parseFloat(data.substring(4).trim());
                this.emgValue = value;
                if (dataListener != null) {
                    dataListener.onEmgDataReceived(value);
                }
                Log.d(TAG, "EMG value received: " + value);
            } catch (NumberFormatException e) {
                Log.e(TAG, "Error parsing EMG data: " + e.getMessage());
            }
        } else if (data.startsWith("TEMP:")) {
            // Temperature data
            try {
                float value = Float.parseFloat(data.substring(5).trim());
                this.temperatureValue = value;
                if (dataListener != null) {
                    dataListener.onTemperatureDataReceived(value);
                }
                Log.d(TAG, "Temperature value received: " + value);
            } catch (NumberFormatException e) {
                Log.e(TAG, "Error parsing temperature data: " + e.getMessage());
            }
        } else if (data.startsWith("PRESS:")) {
            // Pressure data
            try {
                float value = Float.parseFloat(data.substring(6).trim());
                this.pressureValue = value;
                if (dataListener != null) {
                    dataListener.onPressureDataReceived(value);
                }
                Log.d(TAG, "Pressure value received: " + value);
            } catch (NumberFormatException e) {
                Log.e(TAG, "Error parsing pressure data: " + e.getMessage());
            }
        } else if (data.startsWith("SYNC:ERROR")) {
            // Sync error detected
            if (reminderPreferences.isTechnicalAlertsEnabled()) {
                notificationHelper.showSyncIssueNotification("Bluetooth");
            }
        }
    }
    
    /**
     * Set the connection listener to receive connection state updates
     * @param listener The listener to receive updates
     */
    public void setConnectionListener(ConnectionListener listener) {
        this.connectionListener = listener;
    }
    
    /**
     * Clean up Bluetooth resources when the app is being destroyed
     * This method should be called from the Activity's onDestroy method
     */
    public void cleanup() {
        try {
            // Stop any ongoing scanning
            if (mBluetoothAdapter != null && mBluetoothAdapter.isDiscovering()) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    if (ActivityCompat.checkSelfPermission(context, android.Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED) {
                        mBluetoothAdapter.cancelDiscovery();
                    }
                } else {
                    mBluetoothAdapter.cancelDiscovery();
                }
            }
            
            // Close the input stream
            if (mInputStream != null) {
                try {
                    mInputStream.close();
                } catch (IOException e) {
                    Log.e(TAG, "Error closing input stream", e);
                }
                mInputStream = null;
            }
            
            // Close the Bluetooth socket
            if (mBluetoothSocket != null) {
                try {
                    mBluetoothSocket.close();
                } catch (IOException e) {
                    Log.e(TAG, "Error closing Bluetooth socket", e);
                }
                mBluetoothSocket = null;
            }
            
            // Close the GATT connection
            if (mBluetoothGatt != null) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    if (ActivityCompat.checkSelfPermission(context, android.Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED) {
                        mBluetoothGatt.close();
                    }
                } else {
                    mBluetoothGatt.close();
                }
                mBluetoothGatt = null;
            }
            
            // Update connection state
            isConnected = false;
            if (connectionListener != null) {
                connectionListener.onConnectionStateChanged(false);
            }
            
            Log.d(TAG, "Bluetooth resources cleaned up successfully");
        } catch (Exception e) {
            Log.e(TAG, "Error cleaning up Bluetooth resources", e);
        }
    }
}
