package com.tiles.debug;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.icu.text.DateFormat;
import android.os.BatteryManager;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.SystemClock;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import com.opencsv.CSVWriter;
import com.tiles.battery.Battery_ForegroundService;
import com.tiles.bluetooth.Bluetooth_Scan_Owl_Service;
import com.tiles.bluetooth.Bluetooth_Scan_S1_Service;
import com.tiles.bluetooth.Bluetooth_Scan_Service;
import com.tiles.constant.Constants;
import com.tiles.opensmile.OpenSmile_ForegroundService;
import com.tiles.util.Permission;
import com.tiles.util.Utils;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Date;

public class DebugOwl extends Service {

    private final int CSV_DATA_ENTRY_SIZE = 2;

    private String batteryLevel;
    private Handler mHandler = new Handler();

    /*
     *   Alarm Manager
     * */
    private AlarmManager alarmManager;
    private PendingIntent pendingIntent;

    public static final long ALARM_INTERVAL = 1000 * 60 * 2;

    private void startMyOwnForeground(){
        String NOTIFICATION_CHANNEL_ID = "com.example.tiles";
        String channelName = "DEBUG OWL Service";
        NotificationChannel chan = null;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            chan = new NotificationChannel(NOTIFICATION_CHANNEL_ID, channelName, NotificationManager.IMPORTANCE_HIGH);
            chan.setLightColor(Color.BLUE);
            chan.setLockscreenVisibility(Notification.VISIBILITY_PRIVATE);
            NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            assert manager != null;
            manager.createNotificationChannel(chan);
        }

        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID);
        Notification notification = notificationBuilder.setOngoing(true)
                .setContentTitle("TILES_DEBUG_OWL")
                .setContentText("Start Debug Owl Service")
                .setPriority(NotificationManager.IMPORTANCE_HIGH)
                .setCategory(Notification.CATEGORY_SERVICE)
                .build();
        startForeground(Constants.BATTERY_NOTIFICATION_ID, notification);
    }


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

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startMyOwnForeground();
        }

        Log.d(Constants.DEBUG_OWL, "onCreate:Debug_Owl_Services");
        batteryLevel = getBatteryLevel();

        if (Permission.isAllPermissionGranted(getApplicationContext()) == false) {
            return;
        }

        /*
         *   Start BLE service
         * */
        boolean con1 = Utils.retrieveSharedPreference(getApplicationContext(), Constants.BT_STATE).contains(Constants.OFF);
        boolean con2 = Utils.retrieveSharedPreference(getApplicationContext(), Constants.BT_STATE).contains(Constants.RUNNING);
        if(con1) {
            Log.d(Constants.DEBUG_OWL, "Restart OWL Service");
            // startOWLScanService();
            startS1ScanService();
        } else {
            Log.d(Constants.DEBUG_OWL, "OWL Service Runs Fine");
            if (con2) {

            } else {
                Utils.writeSharedPreference(getApplicationContext(), Constants.BT_STATE, Constants.OFF);
            }
        }

        mHandler.postDelayed(mTickExecutor, 10000);

    }

    private void saveDataToCSV() {
        File file = new File(Environment.getExternalStorageDirectory().getAbsoluteFile() + File.separator + Constants.ROOT_FILE);

        if (!file.exists()) {
            if (!file.mkdirs()) {
                Log.e(Constants.DEBUG_OWL, "Problem creating folder");
            }
        }

        String csv_file_path = Environment.getExternalStorageDirectory().getPath() + "/" + Constants.ROOT_FILE +"/Battery_Log.csv";
        String currentDateTimeString = null;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
            currentDateTimeString = DateFormat.getDateTimeInstance().format(new Date());
        }

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

            if(Permission.isAllPermissionGranted(getApplicationContext())) {
                saveDataToCSV();
            }

            alarmManager = (AlarmManager)getSystemService(ALARM_SERVICE);
            Intent battery_Intent = new Intent(getApplicationContext(), Battery_ForegroundService.class);
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
     * Start OWL scan service
     * */
    private void startOWLScanService() {
        Log.d(Constants.DEBUG_OWL, "START OWL SCAN");
        Intent ble_scan_owl_Service = new Intent(getApplicationContext(), Bluetooth_Scan_Owl_Service.class);
        ble_scan_owl_Service.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
        getApplication().getApplicationContext().startService(ble_scan_owl_Service);
    }

    /*
     * Start s1 scan service
     * */
    private void startS1ScanService() {
        Log.d(Constants.DEBUG_OWL, "START S1 SCAN");
        Intent ble_scan_s1_Service = new Intent(getApplicationContext(), Bluetooth_Scan_S1_Service.class);
        ble_scan_s1_Service.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
        getApplication().getApplicationContext().startService(ble_scan_s1_Service);
    }

}
