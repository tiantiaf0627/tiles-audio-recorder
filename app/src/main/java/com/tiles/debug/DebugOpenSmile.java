package com.tiles.debug;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.AssetManager;
import android.graphics.Color;
import android.icu.text.DateFormat;
import android.media.MediaRecorder;
import android.os.BatteryManager;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.SystemClock;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import com.audeering.opensmile.androidtemplate.SmileJNI;
import com.tiles.constant.Constants;
import com.tiles.opensmile.Config;
import com.tiles.opensmile.OpenSmile_ForegroundService;
import com.tiles.util.Utils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;

public class DebugOpenSmile extends Service {

    private Handler mHandler = new Handler();
    private int i = 0;
    /*
     *   Alarm Manager
     * */
    private Config config;
    private String conf;

    private final int ENABLE_FILE_SAVING   = 3;
    private String dataPath;
    private boolean isAllPermissionAllowed = true;

    private String jellyTokenID;

    /**
     * Length of time to allow scanning before automatically shutting off. (2 minutes)
     */
    private long TIMEOUT = TimeUnit.MILLISECONDS.convert(60, TimeUnit.SECONDS);
    public static final long ALARM_INTERVAL = 1000 * 60;

    /*
     *   Alarm Manager
     * */
    private AlarmManager alarmManager;
    private PendingIntent pendingIntent;

    private Handler timeoutHandler = new Handler();
    private Runnable timeoutRunnable;


    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startMyOwnForeground();
        }

    }

    private void startMyOwnForeground(){
        String NOTIFICATION_CHANNEL_ID = "com.example.tiles";
        String channelName = "OPENSMILE Service";
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
                .setContentTitle("TILES_OPENSMILE")
                .setContentText("Start OPENSMILE Service")
                .setPriority(NotificationManager.IMPORTANCE_HIGH)
                .setCategory(Notification.CATEGORY_SERVICE)
                .build();
        startForeground(Constants.OPENSMILE_NOTIFICATION_ID, notification);
    }


    @Override
    public void onStart(Intent intent, int startId) {
        super.onStart(intent, startId);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        Log.d(Constants.DEBUG_OPENSMILE, "onStart: OpenSmile_Services");

        // Audio service runs fine
        Utils.writeSharedPreference(this, Constants.OPENSMILE_STATE, Constants.ON);
        Utils.writeSharedPreference(this, Constants.VAD_STATE, Constants.ON);

        // Make sure that if something wrong operating openSMILE, it will resume to opensmileVAD service
        mHandler.postDelayed(mTickExecutor, (Constants.OPENSMILE_DURATION + 10) * 1000);
        setupAssets();

        // jellyTokenID = retrieveSharedPreference(Constants.QR_CODE_ID).substring(0, 4);
        jellyTokenID = "1111";

        MediaRecorder mRecorder = new MediaRecorder();
        mRecorder.reset();

        final int debug = ENABLE_FILE_SAVING;
        switch (debug) {

            case ENABLE_FILE_SAVING:

                SimpleDateFormat format= new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                String myDate = format.format(new Date());

                File rootFolder = new File(Environment.getExternalStorageDirectory(), Constants.ROOT_FILE);
                if (!rootFolder.exists()) {
                    if (!rootFolder.mkdirs()) {
                        Log.e(Constants.DEBUG_OPENSMILE, "Problem creating folder");
                    }
                }

                File audioFolder = new File(Environment.getExternalStorageDirectory(), Constants.ROOT_FILE + File.separator + "audio");
                if (!audioFolder.exists()) {
                    if (!audioFolder.mkdirs()) {
                        Log.e(Constants.DEBUG_OPENSMILE, "Problem creating folder");
                    }
                }

                File dateFolder = new File(Environment.getExternalStorageDirectory(), Constants.ROOT_FILE + File.separator + "audio" + File.separator + myDate);
                if (!dateFolder.exists()) {
                    if (!dateFolder.mkdirs()) {
                        Log.e(Constants.DEBUG_OPENSMILE, "Problem creating folder");
                    }
                }

                File csvFolder = new File(Environment.getExternalStorageDirectory(), Constants.ROOT_FILE + File.separator + "audio" + File.separator + myDate + File.separator + "csv");
                if (!csvFolder.exists()) {
                    if (!csvFolder.mkdirs()) {
                        Log.e(Constants.DEBUG_OPENSMILE, "Problem creating folder");
                    }
                }
                String timestamp = Long.toString(new Date().getTime());
                dataPath = csvFolder + File.separator + "audio_" + jellyTokenID + "_" + timestamp + ".csv";
                Log.d(Constants.DEBUG_OPENSMILE, dataPath);
                conf = getApplication().getCacheDir() + "/" + config.saveDataConf;

                if (Utils.retrieveSharedPreference(this, Constants.OPENSMILE_STATE).equals(Constants.RUNNING)) {

                } else {
                    Utils.writeSharedPreference(this, Constants.OPENSMILE_STATE, Constants.RUNNING);
                    runOpenSMILEToFile(Constants.OPENSMILE_DURATION);
                }


                break;

            default:
                break;

        }

        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        Log.d(Constants.DEBUG_OPENSMILE, "onStop: OpenSmile_Services");

        // Just set the status to ON, else do nothing
        if (Utils.retrieveSharedPreference(this, Constants.OPENSMILE_STATE).equals(Constants.RUNNING)) {
            Utils.writeSharedPreference(this, Constants.OPENSMILE_STATE, Constants.ON);
        }


        /*
         *   Remove the mTickExecutor only when all permission is allowed
         * */
        if(isAllPermissionAllowed)
            mHandler.removeCallbacks(mTickExecutor);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            stopForeground(true);
        }

        scheduleNextService();
    }

    private void scheduleNextService() {
        alarmManager = (AlarmManager)getSystemService(ALARM_SERVICE);
        Intent opensmileIntent = new Intent(getApplicationContext(), OpenSmile_ForegroundService.class);
        pendingIntent = PendingIntent.getService(getApplicationContext(), 1, opensmileIntent, PendingIntent.FLAG_ONE_SHOT);

        if(Build.VERSION.SDK_INT >= 23) {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.ELAPSED_REALTIME_WAKEUP, SystemClock.elapsedRealtime() + ALARM_INTERVAL, pendingIntent);
            Log.d("Tiles", "Schedule OpenSMILE Alarm Service");
        }
        else if(Build.VERSION.SDK_INT >= 19) {
            alarmManager.setExact(AlarmManager.RTC_WAKEUP, SystemClock.elapsedRealtime() + ALARM_INTERVAL, pendingIntent);
        } else {
            alarmManager.set(AlarmManager.RTC_WAKEUP, SystemClock.elapsedRealtime() + ALARM_INTERVAL, pendingIntent);
        }

    }

    private Runnable mTickExecutor = new Runnable() {
        @Override
        public void run() {

            File testFile = new File(dataPath);
            if(testFile.exists()) {
                Log.d(Constants.DEBUG_OPENSMILE, "test File exist!");
            }
            stopSelf();

        }
    };

    class SmileFeatureThread implements Runnable {
        @Override
        public void run() {
            SmileJNI.SMILExtractOutputJNI(conf, 1, dataPath);
        }
    }


    public void runOpenSMILEToFile(long second) {

        SmileJNI.prepareOpenSMILE(getApplicationContext());
        final DebugOpenSmile.SmileFeatureThread obj = new DebugOpenSmile.SmileFeatureThread();

        final Thread newThread = new Thread(obj);
        newThread.start();
        Timer t = new Timer();
        t.schedule(new TimerTask() {
            @Override
            public void run() {
                stopOpenSMILE();
                mHandler.removeCallbacks(mTickExecutor);
                Log.d("TILEs", "Stop recording in opensmile");

                stopSelf();
            }
        }, second * 1000);
    }


    public void stopOpenSMILE() {
        SmileJNI.SMILEndJNI();
    }

    void setupAssets() {
        config = new Config();
        String[] assets = config.assets;
        ContextWrapper cw = new ContextWrapper(getApplicationContext());
        String confcach = cw.getCacheDir() + "/";
        AssetManager assetManager = getAssets();
        for (String filename : assets) {
            try {
                InputStream in = assetManager.open(filename);
                String out = confcach + filename;
                File outFile = new File(out);
                FileOutputStream outst = new FileOutputStream(outFile);
                copyFile(in, outst);
                in.close();
                outst.flush();
                outst.close();
            } catch (IOException e) {
                Log.e(Constants.DEBUG_OPENSMILE, "Failed to copy asset file: " + filename, e);
            }
        }
    }

    private void copyFile(InputStream in, OutputStream out) throws IOException {
        byte[] buffer = new byte[1024];
        int read;
        while((read = in.read(buffer)) != -1){
            out.write(buffer, 0, read);
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
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
                    currentDateTimeString = DateFormat.getDateTimeInstance().format(new Date());
                }
                Log.d(Constants.DEBUG_OPENSMILE, "stop_service: " + currentDateTimeString);
                Log.d(Constants.DEBUG_OPENSMILE, "OpenSMILE has reached timeout of "+TIMEOUT+" milliseconds, stopping.");
                scheduleNextService();
                stopSelf();
            }
        };
        timeoutHandler.postDelayed(timeoutRunnable, TIMEOUT);
    }

}
