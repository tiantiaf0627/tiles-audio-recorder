package com.tiles.opensmile;

import com.opencsv.CSVWriter;
import com.tiles.constant.Constants;
import com.audeering.opensmile.androidtemplate.SmileJNI;
import com.tiles.util.Utils;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.content.res.AssetManager;
import android.graphics.Color;
import android.icu.text.DateFormat;
import android.media.MediaRecorder;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.SystemClock;
import android.util.Log;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;

/**
 * Created by tiantianfeng on 09/18/19.
 */

public class OpenSmile_ForegroundService extends Service {

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

    private String atomTokenID;

    /**
     * Length of time to allow scanning before automatically shutting off. (2 minutes)
     */
    // private long TIMEOUT = TimeUnit.MILLISECONDS.convert(20, TimeUnit.SECONDS);
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
                .setContentTitle("TILES_OPENSMILE")
                .setContentText("Start OPENSMILE Service")
                .setPriority(NotificationManager.IMPORTANCE_LOW)
                .setSound(null)
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
        if (Utils.retrieveSharedPreference(getApplicationContext(), Constants.OPEN_SMILE_SAMPLE_MODE).equals(Integer.toString(Constants.DISABLE_SAMPLE))) {
            // Make sure that if something wrong operating openSMILE, it will resume to opensmileVAD service
            Utils.writeSharedPreference(this, Constants.OPENSMILE_STATE, Constants.ON);
            mHandler.postDelayed(idleTickExecutor, 60 * 1000);

        } else {
            Utils.writeSharedPreference(this, Constants.OPENSMILE_STATE, Constants.ON);

            int record_duration = Integer.valueOf(Utils.retrieveSharedPreference(getApplicationContext(), Constants.AUDIO_LENGTH));
            Log.d(Constants.DEBUG_OPENSMILE, "record duration: " + Integer.toString(record_duration));

            // Make sure that if something wrong operating openSMILE, it will resume to opensmileVAD service
            mHandler.postDelayed(mTickExecutor, (record_duration + 10) * 1000);
            setupAssets();

            atomTokenID = Utils.retrieveSharedPreference(getApplicationContext(), Constants.MAC_ADDR).replaceAll("[^a-zA-Z0-9]", "");
            // atomTokenID = atomTokenID.substring(atomTokenID.length() - 6);

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
                    dataPath = csvFolder + File.separator + "audio_" + atomTokenID + "_" + timestamp + ".csv";
                    Log.d(Constants.DEBUG_OPENSMILE, dataPath);
                    conf = getApplication().getCacheDir() + "/" + config.saveDataConf;

                    if (Utils.retrieveSharedPreference(this, Constants.OPENSMILE_STATE).equals(Constants.RUNNING)) {

                    } else {
                        Utils.writeSharedPreference(this, Constants.OPENSMILE_STATE, Constants.RUNNING);
                        runOpenSMILEToFile(record_duration);
                    }

                    break;

                default:
                    break;

            }

        }

        return START_STICKY;
    }

    // Save data to csv
    private void saveSummary(String save_folder, String save_path) {

        // Get time string, save file name
        String currentDateTimeString = null;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            currentDateTimeString = DateFormat.getDateTimeInstance().format(new Date());
        }

        SimpleDateFormat format= new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        String myDate = format.format(new Date());

        File summaryFolder = new File(Environment.getExternalStorageDirectory(), Constants.ROOT_FILE + File.separator + "audio_summary");
        if (!summaryFolder.exists()) {
            if (!summaryFolder.mkdirs()) {
                Log.e(Constants.DEBUG_OPENSMILE, "Problem creating folder");
            }
        }

        // Create Root file if not existed
        String csv_file_path = Environment.getExternalStorageDirectory() + File.separator + Constants.ROOT_FILE + File.separator + "audio_summary" + File.separator + myDate + ".csv";
        File tmpFile = new File(csv_file_path);
        boolean exists = tmpFile.exists();

        File file = new File(dataPath);

        // Write the data to csv
        try (CSVWriter writer = new CSVWriter(new FileWriter(csv_file_path, true))) {

            // Write header if i is 0
            if (exists == false) {
                String[] header = new String[3];

                header[0] = "save_path";
                header[1] = "record_length";
                header[2] = "file_size";
                writer.writeNext(header);
            }

            // Write the data part
            String[] data = new String[3];
            data[0] = dataPath;
            data[1] = Utils.retrieveSharedPreference(getApplicationContext(), Constants.AUDIO_LENGTH);


            // Currently disabled
            data[1] = Utils.retrieveSharedPreference(getApplicationContext(), Constants.AUDIO_LENGTH);

            if (file.exists()) {
                data[2] = Long.toString(file.length());
            } else {
                data[2] = "unknown";
            }
            Log.d(Constants.DEBUG_OPENSMILE, dataPath);
            Log.d(Constants.DEBUG_OPENSMILE, data[2]);

            writer.writeNext(data);

            writer.close();

        } catch (IOException e) {
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        Log.d(Constants.DEBUG_OPENSMILE, "onStop: OpenSmile_Services");

        if (Utils.retrieveSharedPreference(getApplicationContext(), Constants.OPEN_SMILE_SAMPLE_MODE).equals(Integer.toString(Constants.DISABLE_SAMPLE))) {
            if (Utils.retrieveSharedPreference(this, Constants.OPENSMILE_STATE).equals(Constants.RUNNING)) {
                Utils.writeSharedPreference(this, Constants.OPENSMILE_STATE, Constants.ON);
            }

            /*
             *   Remove the mTickExecutor only when all permission is allowed
             * */
            if(isAllPermissionAllowed)
                mHandler.removeCallbacks(idleTickExecutor);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                stopForeground(true);
            }

        } else {
            // Just set the status to ON, else do nothing
            if (Utils.retrieveSharedPreference(this, Constants.OPENSMILE_STATE).equals(Constants.RUNNING)) {
                SimpleDateFormat format= new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                String myDate = format.format(new Date());

                String save_folder = Environment.getExternalStorageDirectory() + File.separator + Constants.ROOT_FILE + File.separator + "audio" + File.separator + myDate + File.separator + "csv";
                String timestamp = Long.toString(new Date().getTime());
                String save_path = "audio_" + atomTokenID + "_" + timestamp + ".csv";

                Utils.writeSharedPreference(this, Constants.OPENSMILE_STATE, Constants.ON);
                saveSummary(save_folder, save_path);

                String timeString = DateFormat.getDateTimeInstance().format(new Date());
                Log.d(Constants.DEBUG_OPENSMILE, timeString);
                Utils.writeSharedPreference(getApplicationContext(), Constants.DEBUG_OPENSMILE_FILE_WRITE_TIME, timeString);

            }

            /*
             *   Remove the mTickExecutor only when all permission is allowed
             * */
            if(isAllPermissionAllowed)
                mHandler.removeCallbacks(mTickExecutor);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                stopForeground(true);
            }
        }

        scheduleNextService();
    }

    private void scheduleNextService() {
        alarmManager = (AlarmManager)getSystemService(ALARM_SERVICE);
        Intent opensmileIntent = new Intent(getApplicationContext(), OpenSmile_ForegroundService.class);
        pendingIntent = PendingIntent.getService(getApplicationContext(), 1, opensmileIntent, PendingIntent.FLAG_ONE_SHOT);

        int record_duration = Integer.valueOf(Utils.retrieveSharedPreference(getApplicationContext(), Constants.AUDIO_LENGTH));
        long TIMEOUT = 0;

        Log.d(Constants.DEBUG_OPENSMILE, Integer.toString(record_duration));

        TIMEOUT = TimeUnit.MILLISECONDS.convert(60 - record_duration, TimeUnit.SECONDS);
        Log.d(Constants.DEBUG_OPENSMILE, "Timeout: " + Long.toString(TIMEOUT));

        if(Build.VERSION.SDK_INT >= 23) {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.ELAPSED_REALTIME_WAKEUP, SystemClock.elapsedRealtime() + TIMEOUT, pendingIntent);
            Log.d("Tiles", "Schedule OpenSMILE Alarm Service");
        }
        else if(Build.VERSION.SDK_INT >= 19) {
            alarmManager.setExact(AlarmManager.RTC_WAKEUP, SystemClock.elapsedRealtime() + TIMEOUT, pendingIntent);
        } else {
            alarmManager.set(AlarmManager.RTC_WAKEUP, SystemClock.elapsedRealtime() + TIMEOUT, pendingIntent);
        }

    }

    private Runnable mTickExecutor = new Runnable() {
        @Override
        public void run() {

            // Just set the status to ON, else do nothing
            if (Utils.retrieveSharedPreference(getApplicationContext(), Constants.OPENSMILE_STATE).equals(Constants.RUNNING)) {
                stopOpenSMILE();
            }

            stopSelf();

        }
    };

    private Runnable idleTickExecutor = new Runnable() {
        @Override
        public void run() {

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
        final SmileFeatureThread obj = new SmileFeatureThread();

        final Thread newThread = new Thread(obj);
        newThread.start();

        /*
        Timer t = new Timer();
        t.schedule(new TimerTask() {
            @Override
            public void run() {
                stopOpenSMILE();
                mHandler.removeCallbacks(mTickExecutor);
                Log.d("TILEs", "Stop recording in opensmile");
                stopSelf();
            }
        }, second * 1000); */


        timeoutHandler = new Handler();
        timeoutRunnable = new Runnable() {
            @Override
            public void run() {
                stopOpenSMILE();
                mHandler.removeCallbacks(mTickExecutor);
                Log.d("TILEs", "Stop recording in opensmile");
                stopSelf();
            }
        };
        timeoutHandler.postDelayed(timeoutRunnable, second * 1000);

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

}