package com.tiles.battery;

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
import com.tiles.bluetooth.Bluetooth_Scan_Exp;
import com.tiles.constant.Constants;
import com.tiles.opensmile.OpenSmile_ForegroundService;
import com.tiles.upload.Summary_Upload_Service;
import com.tiles.util.Permission;
import com.tiles.util.Utils;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Calendar;
import java.util.Date;


public class Battery_ForegroundService extends Service {

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

    private void startMyOwnForeground(){
        String NOTIFICATION_CHANNEL_ID = "com.example.tiles";
        String channelName = "Battery Service";
        NotificationChannel chan = null;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            chan = new NotificationChannel(NOTIFICATION_CHANNEL_ID, channelName, NotificationManager.IMPORTANCE_LOW);
            chan.setLightColor(Color.BLUE);
            chan.setLockscreenVisibility(Notification.VISIBILITY_PRIVATE);
            chan.setSound(null, null);
            NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            assert manager != null;
            manager.createNotificationChannel(chan);
        }

        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID);
        Notification notification = notificationBuilder.setOngoing(true)
                .setContentTitle("TILES_BATTERY")
                .setContentText("Start Battery Foreground Service")
                .setSound(null)
                .setPriority(NotificationManager.IMPORTANCE_LOW)
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
        int    percent = (level * 100)/scale;
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

        Log.d(Constants.DEBUG_BATTERY, "onCreate:Battery_Services");
        batteryLevel = getBatteryLevel();

        if (Permission.isAllPermissionGranted(getApplicationContext()) == true) {
            if(Utils.retrieveSharedPreference(getApplicationContext(), Constants.QR_STATE).equals(Constants.SCANNED)) {
                /*
                 *   Start BLE service
                 * */
                boolean con1 = Utils.retrieveSharedPreference(getApplicationContext(), Constants.BT_STATE).contains(Constants.OFF);
                boolean con2 = Utils.retrieveSharedPreference(getApplicationContext(), Constants.BT_STATE).contains(Constants.RUNNING);
                if(con1) {
                    Log.d(Constants.DEBUG_BATTERY, "Restart BT Service");
                    startBLEScanService();
                } else {
                    Log.d(Constants.DEBUG_BATTERY, "BT Service Runs Fine");
                    if (con2) {

                    } else {
                        Utils.writeSharedPreference(getApplicationContext(), Constants.BT_STATE, Constants.OFF);
                    }
                }

                /*
                 *   Make sure we have turned it back on
                 * */
                if(Utils.retrieveSharedPreference(getApplicationContext(), Constants.OPEN_SMILE_SAMPLE_MODE).equals(Integer.toString(Constants.DISABLE_SAMPLE))){
                    Log.d(Constants.DEBUG_BATTERY, "Battery_Service->onCreate->" + Integer.parseInt(Utils.retrieveSharedPreference(getApplicationContext(), Constants.RECORD_DISABLE_COUNTER)));
                    if(Integer.parseInt(Utils.retrieveSharedPreference(getApplicationContext(), Constants.RECORD_DISABLE_COUNTER)) >= 5) {
                        Utils.writeSharedPreference(getApplicationContext(), Constants.RECORD_DISABLE_COUNTER, "0");
                        Utils.writeSharedPreference(getApplicationContext(), Constants.OPEN_SMILE_SAMPLE_MODE, Integer.toString(Constants.NORMAL_SAMPLE));
                    } else {
                        int timeOff = Integer.parseInt(Utils.retrieveSharedPreference(getApplicationContext(), Constants.RECORD_DISABLE_COUNTER)) + 1;
                        Utils.writeSharedPreference(getApplicationContext(), Constants.RECORD_DISABLE_COUNTER, Integer.toString(timeOff));
                    }
                } else {
                    Date date = new Date();
                    Calendar cal = Calendar.getInstance();
                    cal.setTime(date);
                    int hours = cal.get(Calendar.HOUR_OF_DAY);
                    float minutes = cal.get(Calendar.MINUTE);

                    float off = minutes / 60;
                    double time = hours + off;

                    Log.d(Constants.DEBUG_BATTERY, "Time of the day: " + Double.toString(time));

                }

                /*
                 *   Start BLE service
                 * */
                boolean con3 = Utils.retrieveSharedPreference(getApplicationContext(), Constants.OPENSMILE_STATE).contains(Constants.OFF);
                boolean con4 = Utils.retrieveSharedPreference(getApplicationContext(), Constants.OPENSMILE_STATE).contains(Constants.RUNNING);
                if(con3) {
                    Log.d(Constants.DEBUG_BATTERY, "Restart OpenSMILE Service");
                    startOpenSmileService();
                } else {
                    Log.d(Constants.DEBUG_BATTERY, "OpenSMILE Service Runs Fine");
                    if (con4) {

                    } else {
                        Utils.writeSharedPreference(getApplicationContext(), Constants.OPENSMILE_STATE, Constants.OFF);
                    }
                }

                startUploadService();
            }
        }




        /*
         *   Start Audio service
         * */
        /*
        boolean con1 = Utils.retrieveSharedPreference(getApplicationContext(), Constants.OPENSMILE_STATE).contains(Constants.OFF);
        boolean con2 = Utils.retrieveSharedPreference(getApplicationContext(), Constants.OPENSMILE_STATE).contains(Constants.RUNNING);
        if(con1) {
            Log.d(Constants.DEBUG_BATTERY, "Restart opensmile Service");
            startOpenSmileService();
        } else {
            Log.d(Constants.DEBUG_BATTERY, "OpenSMILE Service Runs Fine");
            if (con2) {

            } else {
                Utils.writeSharedPreference(getApplicationContext(), Constants.OPENSMILE_STATE, Constants.OFF);
            }
        } */

        /*
         *   Start Wifi Service
         * */
        // WifiManager mainWifi = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);

        // if (mainWifi.isWifiEnabled() == true && Utils.retrieveSharedPreference(getApplicationContext(), Constants.UPLOAD_STATE).contains(Constants.OFF)) {
        //    mainWifi.setWifiEnabled(false);
        // }

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

            if(isAllPermissionGranted()) {
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
     * Start BLE scan service
     * */
    private void startBLEScanService() {
        Log.d(Constants.DEBUG_BATTERY, "START BLE SCAN ");
        Intent ble_scan_Service = new Intent(getApplicationContext(), Bluetooth_Scan_Exp.class);
        ble_scan_Service.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
        getApplication().getApplicationContext().startService(ble_scan_Service);
    }

    /*
     * Start upload service
     * */
    private void startUploadService() {
        Log.d(Constants.DEBUG_BATTERY, "START Upload");
        Intent upload_Service = new Intent(getApplicationContext(), Summary_Upload_Service.class);
        upload_Service.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
        getApplication().getApplicationContext().startService(upload_Service);
    }

    /*
     * Start opensmile service
     * */
    private void startOpenSmileService() {
        Log.d(Constants.DEBUG_BATTERY, "START OPENSMILE ");
        Intent opensmile_Service = new Intent(getApplicationContext(), OpenSmile_ForegroundService.class);
        opensmile_Service.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
        getApplication().getApplicationContext().startService(opensmile_Service);
    }

    private boolean isAllPermissionGranted() {
        if(Utils.retrieveSharedPreference(getApplicationContext(), Constants.PER_STATUS).equals(Constants.PER_ALL_GRANTED)) {
            return true;
        } else {
            return false;
        }
    }

}
