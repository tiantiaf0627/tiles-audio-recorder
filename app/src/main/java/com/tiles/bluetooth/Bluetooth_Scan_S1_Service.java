package com.tiles.bluetooth;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanRecord;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.icu.text.DateFormat;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.ParcelUuid;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import com.opencsv.CSVWriter;
import com.tiles.constant.Constants;
import com.tiles.util.Utils;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Created by tiantianfeng on 8/13/19.
 */

public class Bluetooth_Scan_S1_Service extends Service {
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
    private long TIMEOUT = TimeUnit.MILLISECONDS.convert(10, TimeUnit.SECONDS);
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

    private String[] s1Addr = {"32:34", "AE:65", "AE:A0", "AE:CC", "32:36",
                               "AE:5B", "AE:A8", "AE:C5", "33:EE", "AE:60",
                               "32:39", "32:FD", "33:F3", "AE:8B", "33:E2"};

    String startTimeString = null;
    String endTimeString = null;

    @Override
    public void onCreate() {
        super.onCreate();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startMyOwnForeground();
        }
    }

    private void startMyOwnForeground(){
        String NOTIFICATION_CHANNEL_ID = "com.example.tiles";
        String channelName = "BT Scan Service";
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel chan = new NotificationChannel(NOTIFICATION_CHANNEL_ID, channelName, NotificationManager.IMPORTANCE_HIGH);
            chan.setLightColor(Color.BLUE);
            chan.setLockscreenVisibility(Notification.VISIBILITY_PRIVATE);
            NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            assert manager != null;
            manager.createNotificationChannel(chan);
        }

        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID);
        Notification notification = notificationBuilder.setOngoing(true)
                .setContentTitle("TILES_BT")
                .setContentText("Start BT foreground service")
                .setPriority(NotificationManager.IMPORTANCE_HIGH)
                .setCategory(Notification.CATEGORY_SERVICE)
                .build();
        startForeground(Constants.BT_NOTIFICATION_ID, notification);
    }


    @Override
    public void onStart(Intent intent, int startId) {
        super.onStart(intent, startId);

        DEBUG_STATE = ENABLE_SCAN;

        if (retrieveSharedPreference(Constants.BT_STATE).equals(Constants.RUNNING)) {

        } else {
            writeSharedPreference(Constants.BT_STATE, Constants.RUNNING);

            Log.d(Constants.DEBUG_BT, "Start BT Scan Service");

            // Initialize parameters
            initialize();

            // Start scanning
            startScanning();

            // Timeout for next service
            setTimeout();

            // Get time string, save file name
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                startTimeString = DateFormat.getDateTimeInstance().format(new Date());
            }
            Utils.writeSharedPreference(getApplicationContext(), Constants.DEBUG_BT_FILE_WRITE_TIME, startTimeString);
        }


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
                        Log.d(Constants.DEBUG_S1, "Enable BT function");
                        bluetoothAdapter.enable();
                        mBluetoothLeScanner = bluetoothAdapter.getBluetoothLeScanner();
                        writeSharedPreference(Constants.BT_STATE, Constants.ON);
                    }

                    Log.d(Constants.DEBUG_S1, "GET getBluetoothLeScanner");
                } else {
                    Log.d(Constants.DEBUG_S1, BLE_MANAGER_NULL);
                }

            } else {
                Log.d(Constants.DEBUG_S1, BLE_MANAGER_NULL);
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
                String currentDateTimeString = null;
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    currentDateTimeString = DateFormat.getDateTimeInstance().format(new Date());
                }
                Log.d(Constants.DEBUG_S1, "stop_service: " + currentDateTimeString);
                Log.d(Constants.DEBUG_S1, "ScanService has reached timeout of "+TIMEOUT+" milliseconds, stopping advertising.");
                scheduleNextService();
                stopSelf();
            }
        };
        timeoutHandler.postDelayed(timeoutRunnable, TIMEOUT);
    }


    private void scheduleNextService() {
        alarmManager = (AlarmManager)getSystemService(ALARM_SERVICE);
        Intent bleScanIntent = new Intent(getApplicationContext(), Bluetooth_Scan_S1_Service.class);
        pendingIntent = PendingIntent.getService(getApplicationContext(), 1, bleScanIntent, PendingIntent.FLAG_ONE_SHOT);

        if(Build.VERSION.SDK_INT >= 23) {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.ELAPSED_REALTIME_WAKEUP, SystemClock.elapsedRealtime() + ALARM_INTERVAL, pendingIntent);
            Log.d(Constants.DEBUG_S1, "Set BLE Alarm Service");
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

        Log.d(Constants.DEBUG_S1, "Start BT Scan Service");

        if (mScanCallback == null) {
            Log.d(Constants.DEBUG_S1, "Starting Scanning");

            // Kick off a new scan.
            String currentDateTimeString = null;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                currentDateTimeString = DateFormat.getDateTimeInstance().format(new Date());
            }
            mScanCallback = new LeScanCallback();
            mBluetoothLeScanner.startScan(buildScanFilters(), buildScanSettings(), mScanCallback);
            Log.d(Constants.DEBUG_S1, "start_scan: " + currentDateTimeString);

        } else {
            Log.d(Constants.DEBUG_S1, "Failed Init mBluetoothLeScanner");
        }
    }

    /**
     * Stop scanning for BLE Advertisements.
     */
    public void stopScanning() {
        Log.d(Constants.DEBUG_S1, "Stopping Scanning");

        // Stop the scan, wipe the callback.
        mBluetoothLeScanner.stopScan(mScanCallback);
        mScanCallback = null;

        // Kick off a new scan.
        String currentDateTimeString = null;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            currentDateTimeString = DateFormat.getDateTimeInstance().format(new Date());
        }
        Log.d(Constants.DEBUG_S1, "stop_scan: " + currentDateTimeString);

        // Save CSV data
        saveDataToCSV(saveList);

        // Make sure BT is ON
        writeSharedPreference(Constants.BT_STATE, Constants.ON);

    }

    // Save data to csv
    private void saveDataToCSV(ArrayList<String[]> saveList) {

        // Get time string, save file name
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            endTimeString = DateFormat.getDateTimeInstance().format(new Date());
        }

        // Create Root file if not existed
        Utils.createFolder(new File(Environment.getExternalStorageDirectory().getAbsoluteFile() + File.separator + Constants.ROOT_FILE), Constants.DEBUG_S1);

        // Create beacon folder if not existed
        File csvFolder = new File(Environment.getExternalStorageDirectory().getAbsoluteFile() + File.separator + Constants.ROOT_FILE + File.separator + "s1_5m");
        Utils.createFolder(csvFolder, Constants.DEBUG_S1);
        String csv_file_path = Environment.getExternalStorageDirectory().getPath() + File.separator + Constants.ROOT_FILE + File.separator + "s1_5m" + File.separator + startTimeString + "-" + endTimeString + ".csv";

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
                        header[3] = "mac_addr";
                        writer.writeNext(header);
                    }

                    // Write the data part
                    String[] data = new String[4];
                    data[0] = deviceInfo[1];
                    data[1] = deviceInfo[2];
                    data[2] = deviceInfo[3];
                    data[3] = deviceInfo[0];
                    writer.writeNext(data);

                    for (int j = 0; j < s1Addr.length; j++) {
                        if (data[3].equals(s1Addr[j]) == true) {

                            Log.d(Constants.DEBUG_S1, "Write available S1: Device Name: " + data[1] + ", Device RSSI: " + data[2] + ", macAddr: " + data[3]);
                        }
                    }
                }

                writer.close();

            } catch (IOException e) {
            }
        } else {
            try (CSVWriter writer = new CSVWriter(new FileWriter(csv_file_path, true))) {

                String[] header = new String[4];

                header[0] = "device_addr";
                header[1] = "device_name";
                header[2] = "rssi";
                header[3] = "mac_addr";

                writer.writeNext(header);
                writer.close();

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
                Log.d(Constants.DEBUG_S1, result.toString());
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
            List<ParcelUuid> uuidList = scanRecord != null ? scanRecord.getServiceUuids() : null;

            for (int j = 0; j < s1Addr.length; j++) {
                if (deviceAddr.contains(s1Addr[j]) == true) {

                    Log.d(Constants.DEBUG_S1, result.toString());
                    String[] deviceInfo = new String[4];

                    deviceInfo[0] = s1Addr[j];
                    deviceInfo[1] = deviceAddr;
                    deviceInfo[2] = scanRecord.getDeviceName();
                    deviceInfo[3] = Integer.toString(rssi);
                    saveList.add(deviceInfo);

                }
            }

        }

        @Override
        public void onScanFailed(int errorCode) {
            super.onScanFailed(errorCode);
            Log.d(Constants.DEBUG_S1, "Failed to scan");
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
