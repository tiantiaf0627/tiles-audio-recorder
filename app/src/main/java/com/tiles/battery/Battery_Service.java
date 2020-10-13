package com.tiles.battery;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.icu.text.DateFormat;
import android.net.wifi.WifiManager;
import android.os.BatteryManager;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.SystemClock;

import android.util.Log;
import androidx.annotation.Nullable;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Date;

import com.opencsv.CSVWriter;
import com.tiles.constant.Constants;
import com.tiles.util.Utils;
import com.tiles.bluetooth.Bluetooth_Scan_Service;
import com.opencsv.CSVReader;


public class Battery_Service extends Service {

    private final int CSV_DATA_ENTRY_SIZE = 2;

    private String batteryLevel;
    private String batteryTimestamp;
    private Handler mHandler = new Handler();

    private String vad_gap, vad_run;

    /*
     *   Alarm Manager
     * */
    private AlarmManager alarmManager;
    private PendingIntent pendingIntent;

    public static final long ALARM_INTERVAL = 1000 * 60 * 2;

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    public String getBatteryLevel() {
        Intent intent  = getApplicationContext().registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        int    level   = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, 0);
        int    scale   = intent.getIntExtra(BatteryManager.EXTRA_SCALE, 100);
        int    percent = (level*100)/scale;
        Log.d(Constants.DEBUG_BATTERY, String.valueOf(percent) + "%");

        return String.valueOf(percent) + "%";
    }

    @Override
    public void onStart(Intent intent, int startId) {
        super.onStart(intent, startId);
    }

    /*
     * Start BLE scan service
     * */
    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(Constants.DEBUG_BATTERY, "onCreate:Battery_Services");
        batteryLevel = getBatteryLevel();

        /*
         *   Start BLE service
         * */
        if(Utils.retrieveSharedPreference(getApplicationContext(), Constants.BT_STATE).contains(Constants.OFF)) {
            startBLEScanService();
        } else {
            Utils.writeSharedPreference(getApplicationContext(), Constants.BT_STATE, Constants.OFF);
        }

        /*
         *   Start Wifi Service
         * */
        WifiManager mainWifi = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);

        if (mainWifi.isWifiEnabled() == true && Utils.retrieveSharedPreference(getApplicationContext(), Constants.UPLOAD_STATE).contains(Constants.OFF)) {
            mainWifi.setWifiEnabled(false);
        }

        mHandler.postDelayed(mTickExecutor, 10000);

    }

    private void saveDataToCSV() {
        File file = new File(Environment.getExternalStorageDirectory().getAbsoluteFile() + File.separator + Constants.ROOT_FILE);

        if (!file.exists()) {
            if (!file.mkdirs()) {
                Log.e(Constants.DEBUG_BATTERY, "Problem creating folder");
            }
        }

        String csv_file_path = Environment.getExternalStorageDirectory().getPath() + "/" + Constants.ROOT_FILE +"/Battery_Log.csv";
        String currentDateTimeString = DateFormat.getDateTimeInstance().format(new Date());

        try (CSVWriter writer = new CSVWriter(new FileWriter(csv_file_path, true))) {

            String[] data = new String[CSV_DATA_ENTRY_SIZE];

            data[0] = currentDateTimeString;
            data[1] = batteryLevel;

            writer.writeNext(data);
            writer.close();

        } catch (IOException e) {
        }

    }

    private Runnable mTickExecutor = new Runnable() {
        @Override
        public void run() {

            if(isAllPermissionGranted()) {
                saveDataToCSV();
            }

            alarmManager = (AlarmManager)getSystemService(ALARM_SERVICE);
            Intent battery_Intent = new Intent(getApplicationContext(), Battery_Service.class);
            pendingIntent = PendingIntent.getService(getApplicationContext(), 1, battery_Intent, PendingIntent.FLAG_ONE_SHOT);

            if(Build.VERSION.SDK_INT >= 23) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.ELAPSED_REALTIME_WAKEUP, SystemClock.elapsedRealtime() + ALARM_INTERVAL, pendingIntent);
                Log.d("Tiles", "Battery Set Alarm Service");
            }
            else if(Build.VERSION.SDK_INT >= 19) {
                alarmManager.setExact(AlarmManager.ELAPSED_REALTIME_WAKEUP, SystemClock.elapsedRealtime() + ALARM_INTERVAL, pendingIntent);
            } else {
                alarmManager.set(AlarmManager.ELAPSED_REALTIME_WAKEUP, SystemClock.elapsedRealtime() + ALARM_INTERVAL, pendingIntent);
            }

            stopSelf();

        }
    };


    /*
     * Start BLE scan service
     * */
    private void startBLEScanService() {
        Log.d(Constants.DEBUG_BATTERY, "START BLE SCAN ");
        Intent ble_scan_Service = new Intent(getApplicationContext(), Bluetooth_Scan_Service.class);
        ble_scan_Service.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
        // getApplication().getApplicationContext().startService(ble_scan_Service);

        getApplication().getApplicationContext().startForegroundService(ble_scan_Service);
    }

    private boolean isAllPermissionGranted() {
        if(Utils.retrieveSharedPreference(getApplicationContext(), Constants.PER_STATUS).equals(Constants.PER_ALL_GRANTED)) {
            return true;
        } else {
            return false;
        }
    }

}
