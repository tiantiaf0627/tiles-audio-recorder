package com.tiles.bluetooth;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.AdvertiseCallback;
import android.bluetooth.le.AdvertiseData;
import android.bluetooth.le.AdvertiseSettings;
import android.bluetooth.le.BluetoothLeAdvertiser;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanRecord;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.icu.text.DateFormat;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.ParcelUuid;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.util.Log;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import androidx.annotation.Nullable;


import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import com.opencsv.CSVWriter;
import com.tiles.constant.Constants;
import com.tiles.util.Utils;

/**
 * Created by tiantianfeng on 8/13/19.
 */

public class Bluetooth_Scan_Service extends Service{
    private BluetoothAdapter bluetoothAdapter;

    private String BLE_MANAGER_NULL     = "No BLE Manager";

    private Handler timeoutHandler = new Handler();
    private Runnable timeoutRunnable;

    private ScanCallback mScanCallback;
    private BluetoothLeScanner mBluetoothLeScanner;

    private final int ENABLE_SCAN       = 1;

    /**
     * Length of time to allow scanning before automatically shutting off. (2 minutes)
     */
    private long TIMEOUT = TimeUnit.MILLISECONDS.convert(15, TimeUnit.SECONDS);
    public static final long ALARM_INTERVAL = 1000 * 15;

    // Scan time: should be changeable
    private static final long SCAN_PERIOD = 10000;

    /*
     *   Alarm Manager
     * */
    private AlarmManager alarmManager;
    private PendingIntent pendingIntent;

    private int DEBUG_STATE;
    private Handler scanHandler = new Handler();
    private ArrayList<String[]> saveList = new ArrayList<>();

    @Override
    public void onCreate() {
        super.onCreate();

    }

    @Override
    public void onStart(Intent intent, int startId) {
        super.onStart(intent, startId);

        DEBUG_STATE = ENABLE_SCAN;

        writeSharedPreference(Constants.BT_STATE, Constants.ON);

        Log.d(Constants.DEBUG_BT, "Start BT Scan Service");

        // Initialize parameters
        initialize();

        // Start scanning
        startScanning();

        // Timeout for next service
        setTimeout();

        // Get time string, save file name
        String currentDateTimeString = null;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
            currentDateTimeString = DateFormat.getDateTimeInstance().format(new Date());
        }
        Utils.writeSharedPreference(getApplicationContext(), Constants.DEBUG_BT_FILE_WRITE_TIME, currentDateTimeString);

    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private boolean isAllPermissionGranted() {
        if(retrieveSharedPreference(Constants.PER_STATUS).equals(Constants.PER_ALL_GRANTED)) {
            return true;
        } else {
            return false;
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        /**
         * Note that onDestroy is not guaranteed to be called quickly or at all. Services exist at
         * the whim of the system, and onDestroy can be delayed or skipped entirely if memory need
         * is critical.
         * */
        if (isAllPermissionGranted()) {
            stopScanning();
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            stopForeground(true);
        }
    }

    /**
     * Get references to system Bluetooth objects if we don't have them already.
     */
    private void initialize() {
        if (mBluetoothLeScanner == null) {
            BluetoothManager mBluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);

            if (mBluetoothManager != null) {
                bluetoothAdapter = mBluetoothManager.getAdapter();

                if (bluetoothAdapter != null) {
                    if(bluetoothAdapter.isEnabled()) {
                        mBluetoothLeScanner = bluetoothAdapter.getBluetoothLeScanner();
                    } else {
                        Log.d(Constants.DEBUG_BT, "Enable BT function");
                        bluetoothAdapter.enable();
                        mBluetoothLeScanner = bluetoothAdapter.getBluetoothLeScanner();
                        writeSharedPreference(Constants.BT_STATE, Constants.ON);
                    }

                    Log.d(Constants.DEBUG_BT, "GET getBluetoothLeScanner");
                } else {
                    Log.d(Constants.DEBUG_BT, BLE_MANAGER_NULL);
                }

            } else {
                Log.d(Constants.DEBUG_BT, BLE_MANAGER_NULL);
            }
        }

    }

    /**
     * Starts a delayed Runnable that will cause the BLE Advertising to timeout and stop after a
     * set amount of time.
     */
    private void setTimeout(){
        timeoutHandler = new Handler();
        timeoutRunnable = new Runnable() {
            @Override
            public void run() {
                String currentDateTimeString = DateFormat.getDateTimeInstance().format(new Date());
                Log.d(Constants.DEBUG_BT, "stop_service: " + currentDateTimeString);
                Log.d(Constants.DEBUG_BT, "ScanService has reached timeout of "+TIMEOUT+" milliseconds, stopping advertising.");
                scheduleNextService();
                stopSelf();
            }
        };
        timeoutHandler.postDelayed(timeoutRunnable, TIMEOUT);
    }


    private void scheduleNextService() {
        alarmManager = (AlarmManager)getSystemService(ALARM_SERVICE);
        Intent bleScanIntent = new Intent(getApplicationContext(), Bluetooth_Scan_Service.class);
        pendingIntent = PendingIntent.getService(getApplicationContext(), 1, bleScanIntent, PendingIntent.FLAG_ONE_SHOT);

        if(Build.VERSION.SDK_INT >= 23) {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.ELAPSED_REALTIME_WAKEUP, SystemClock.elapsedRealtime() + ALARM_INTERVAL, pendingIntent);
            Log.d("Tiles", "Set BLE Advertise Alarm Service");
        }
        else if(Build.VERSION.SDK_INT >= 19) {
            alarmManager.setExact(AlarmManager.RTC_WAKEUP, SystemClock.elapsedRealtime() + ALARM_INTERVAL, pendingIntent);
        } else {
            alarmManager.set(AlarmManager.RTC_WAKEUP, SystemClock.elapsedRealtime() + ALARM_INTERVAL, pendingIntent);
        }

    }

    private void writeSharedPreference(String key, String value) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(key, value);
        editor.apply();
    }

    private String retrieveSharedPreference(String key) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        return preferences.getString(key, "");
    }

    /**
     * Start scanning for BLE Advertisements (& set it up to stop after a set period of time).
     */
    public void startScanning() {

        Log.d(Constants.DEBUG_MAIN, "Start BT Scan Service");

        if (mScanCallback == null) {
            Log.d(Constants.DEBUG_BT, "Starting Scanning");

            /*
            // scanHandler = new Handler();
            // Will stop the scanning after a set time.
            scanHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    stopScanning();
                }
            }, SCAN_PERIOD); */

            // Kick off a new scan.
            String currentDateTimeString = null;
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
                currentDateTimeString = DateFormat.getDateTimeInstance().format(new Date());
            }
            mScanCallback = new LeScanCallback();
            mBluetoothLeScanner.startScan(buildScanFilters(), buildScanSettings(), mScanCallback);
            Log.d(Constants.DEBUG_BT, "start_scan: " + currentDateTimeString);

        } else {
            Log.d(Constants.DEBUG_BT, "Failed Init mBluetoothLeScanner");
        }
    }

    /**
     * Stop scanning for BLE Advertisements.
     */
    public void stopScanning() {
        Log.d(Constants.DEBUG_BT, "Stopping Scanning");

        // Stop the scan, wipe the callback.
        mBluetoothLeScanner.stopScan(mScanCallback);
        mScanCallback = null;

        // Kick off a new scan.
        String currentDateTimeString = null;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
            currentDateTimeString = DateFormat.getDateTimeInstance().format(new Date());
        }
        Log.d(Constants.DEBUG_BT, "stop_scan: " + currentDateTimeString);

        // Save CSV data
        saveDataToCSV(saveList);

    }

    // Save data to csv
    private void saveDataToCSV(ArrayList<String[]> saveList) {

        // Get time string, save file name
        String currentDateTimeString = null;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
            currentDateTimeString = DateFormat.getDateTimeInstance().format(new Date());
        }

        // Create Root file if not existed
        File file = new File(Environment.getExternalStorageDirectory().getAbsoluteFile() + File.separator + Constants.ROOT_FILE);
        if (!file.exists()) {
            if (!file.mkdirs()) {
                Log.e(Constants.DEBUG_BT, "Problem creating folder");
            }
        }

        // Create beacon folder if not existed
        File csvFolder = new File(Environment.getExternalStorageDirectory().getAbsoluteFile() + File.separator + Constants.ROOT_FILE + File.separator + "beacon");
        if (!csvFolder.exists()) {
            if (!csvFolder.mkdirs()) {
                Log.e(Constants.DEBUG_BT, "Problem creating folder");
            }
        }

        String csv_file_path = Environment.getExternalStorageDirectory().getPath() + File.separator + Constants.ROOT_FILE + File.separator + "beacon" + File.separator + currentDateTimeString + ".csv";

        // Write the data to csv
        if (saveList.size() != 0) {
            try (CSVWriter writer = new CSVWriter(new FileWriter(csv_file_path, true))) {

                for (int i = 0; i < saveList.size(); i++) {
                    String[] deviceInfo = saveList.get(i);

                    // Write header if i is 0
                    if (i == 0) {
                        String[] header = new String[deviceInfo.length];

                        header[0] = "device_addr";
                        header[1] = "device_name";
                        header[2] = "rssi";
                        header[3] = "frame_type";
                        header[4] = "product_mode";
                        header[5] = "battery_level";
                        header[6] = "mac_addr";
                        writer.writeNext(header);
                    }

                    // Write the data part
                    String[] data = new String[deviceInfo.length];
                    data[0] = deviceInfo[13];
                    data[1] = deviceInfo[14];
                    data[2] = deviceInfo[15];
                    data[0] = deviceInfo[0];
                    data[1] = deviceInfo[1];
                    data[2] = deviceInfo[2];
                    data[3] = deviceInfo[7] + deviceInfo[8] + deviceInfo[9] + deviceInfo[10] + deviceInfo[11] + deviceInfo[12];

                    writer.writeNext(data);
                }

                writer.close();
                // Utils.writeSharedPreference(getApplicationContext(), Constants.DEBUG_FILE_WRITE_TIME, currentDateTimeString);

            } catch (IOException e) {
            }
        }
    }


    /**
     * Custom ScanCallback object - adds to adapter on success, displays error on failure.
     */
    private class LeScanCallback extends ScanCallback {

        @Override
        public void onBatchScanResults(List<ScanResult> results) {
            super.onBatchScanResults(results);

            for (ScanResult result : results) {
                Log.d(Constants.DEBUG_BT, result.toString());
                // Log.d(Constants.DEBUG_BT, "Device Name: " + result.getDevice() + ", Device RSSI: " + result.getRssi());
                Log.d(Constants.DEBUG_BT, "Device Name: " + result.getDevice() + ", Device RSSI: " + result.getRssi());
            }
        }

        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            super.onScanResult(callbackType, result);
            // Get scanRecord
            ScanRecord scanRecord = result.getScanRecord();
            int rssi = result.getRssi();
            String deviceAddr = result.getDevice().getAddress();

            // Parse scanRecord
            if (scanRecord.getDeviceName() != null && !scanRecord.getDeviceName().isEmpty()){
                if (scanRecord.getDeviceName().equals("S1") || scanRecord.getDeviceName().equals("reelyActive")){

                    List<ParcelUuid> uuidList = scanRecord != null ? scanRecord.getServiceUuids() : null;

                    if (uuidList != null) {
                        for (ParcelUuid uuid : uuidList) {
                            byte[] advByteData = scanRecord.getServiceData(uuid);
                            if (advByteData != null) {
                                if (advByteData.length == 13) {
                                    String[] deviceInfo = new String[advByteData.length + 3];

                                    for (int i = 0; i < advByteData.length; i++) {
                                        deviceInfo[i] = String.format("%02x", advByteData[i]);
                                    }

                                    // Get extra information
                                    deviceInfo[13] = deviceAddr;
                                    deviceInfo[14] = scanRecord.getDeviceName();
                                    deviceInfo[15] = Integer.toString(rssi);

                                    String frameType = deviceInfo[0];
                                    String productMode = deviceInfo[1];
                                    String batteryLevel = deviceInfo[2];
                                    String macAddr = deviceInfo[7] + deviceInfo[8] + deviceInfo[9] + deviceInfo[10] + deviceInfo[11] + deviceInfo[12];

                                    Log.d(Constants.DEBUG_BT, "Device Name: " + result.getDevice() + ", Device RSSI: " + result.getRssi() + ", MF Data: " + result.getScanRecord().getDeviceName());
                                    Log.d(Constants.DEBUG_BT, "frameType: " + frameType + ", productMode: " + productMode + ", batteryLevel: " + batteryLevel + ", macAddr: " + macAddr);

                                    saveList.add(deviceInfo);
                                }
                            }

                        }

                    }
                }
            }

        }

        @Override
        public void onScanFailed(int errorCode) {
            super.onScanFailed(errorCode);
            Log.d(Constants.DEBUG_BT, "Failed to scan");
        }

    }


    /**
     * Return a {@link ScanSettings} object set to use low power (to preserve battery life).
     */
    private ScanSettings buildScanSettings() {
        ScanSettings.Builder builder = new ScanSettings.Builder();
        builder.setScanMode(ScanSettings.SCAN_MODE_LOW_POWER);
        return builder.build();
    }

    /**
     * Return a List of {@link ScanFilter} objects to filter by Service UUID.
     */
    private List<ScanFilter> buildScanFilters() {
        List<ScanFilter> scanFilters = new ArrayList<>();

        ScanFilter.Builder builder = new ScanFilter.Builder();
        // Comment out the below line to see all BLE devices around you
        scanFilters.add(builder.build());

        return scanFilters;
    }


}
