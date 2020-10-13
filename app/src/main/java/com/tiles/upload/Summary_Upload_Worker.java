package com.tiles.upload;

import android.app.AlarmManager;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.os.Environment;
import android.os.Handler;
import android.os.SystemClock;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.amazonaws.ClientConfiguration;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferListener;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferObserver;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferState;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferUtility;
import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3Client;
import com.google.android.gms.common.api.Result;
import com.tiles.constant.Constants;
import com.tiles.util.Permission;
import com.tiles.util.Utils;

import java.io.File;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class Summary_Upload_Worker extends Worker {

    private final String DEBUG = "TILEs";

    public static final String UPLOAD_STATE_CHANGED_ACTION = "UPLOAD_STATE_CHANGED_ACTION";
    public static final String UPLOAD_CANCELLED_ACTION = "UPLOAD_CANCELLED_ACTION";


    private AmazonS3Client s3Client;
    private NotificationManager nm;

    private Handler mHandler = new Handler();
    private Handler fileHandler = new Handler();

    private int folderIndex = 0;

    /*
     *   Alarm Manager
     * */
    private AlarmManager alarmManager;
    private PendingIntent pendingIntent;

    public static final long ALARM_INTERVAL = 1000 * 60 * 10;
    public static final long ALARM_TRIGGER_AT_TIME = SystemClock.elapsedRealtime() + 10000;

    private TransferUtility transferUtility;

    Map<Integer, String> map = new HashMap<>();
    private int currentFileIndex = 0;
    private File[] files;
    private TransferListener listener = new Summary_Upload_Worker.UploadListener();

    private String jellyTokenID, uploadFileDirectory;

    private SimpleDateFormat dateFormat;
    private List<Date> dateList = new ArrayList<>();

    private int numOfErr = 0;

    private TransferObserver observer;

    public Summary_Upload_Worker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Worker.Result doWork() {
        if (isDeviceOnline() && Permission.isAllPermissionGranted(getApplicationContext())) {

            jellyTokenID = Utils.retrieveSharedPreference(getApplicationContext(), Constants.MAC_ADDR).substring(0, 4);
            Log.d(Constants.DEBUG_UPLOAD, "File_Uploading_Service->onStart->jellyTokenID:" + jellyTokenID);

            ClientConfiguration clientConfiguration = new ClientConfiguration();

            clientConfiguration.setMaxErrorRetry(3);
            clientConfiguration.setMaxConnections(8);
            clientConfiguration.setConnectionTimeout(15 * 1000);

            /*
             *   Init Amazon Client Object and Transfer object
             * */
            s3Client = new AmazonS3Client(new BasicAWSCredentials(Constants.S3_ACCESS_KEY, Constants.S3_SECRET));
            s3Client.setRegion(Region.getRegion(Regions.US_WEST_2));

            TransferUtility transferUtility = new TransferUtility(s3Client, getApplicationContext());

            // If main path exist
            File summaryFolder = new File(Environment.getExternalStorageDirectory() + File.separator + Constants.ROOT_FILE + File.separator + "audio_summary");
            if(summaryFolder.exists()) {

                Log.d(Constants.DEBUG_UPLOAD, "File_Uploading_Service->Transimit folder exist");
                files = summaryFolder.listFiles();
                Log.d(Constants.DEBUG_UPLOAD, "File_Uploading_Service->Size: "+ files.length);

                if (files.length > 0) {

                    Log.d(DEBUG, "File_Uploading_Service->Uploading Size: "+ files.length);

                    for (int i = 0; i < files.length; i++) {
                        if(files[i].getName().contains("csv")) {
                            Log.d(Constants.DEBUG_UPLOAD, "File_Uploading_Service->Start Transmitting File->" + uploadFileDirectory);
                            /*
                            uploadFileDirectory = jellyTokenID + File.separator + "audio_summary" + File.separator;


                            // Start Transfer
                            observer = transferUtility.upload(Constants.S3_BUCKET,
                                    uploadFileDirectory + files[i].getName(),
                                    files[i]
                            );

                            // observer.setTransferListener(new );
                            // Sets listeners to in progress transfers
                            if (TransferState.WAITING.equals(observer.getState())
                                    || TransferState.WAITING_FOR_NETWORK.equals(observer.getState())
                                    || TransferState.IN_PROGRESS.equals(observer.getState())) {
                                observer.setTransferListener(listener);
                                map.put(observer.getId(), files[i].getName());
                                Log.d(Constants.DEBUG_UPLOAD, "observer: " + observer.getId());
                            } */
                        }

                    }
                } else {
                    Utils.writeSharedPreference(getApplicationContext(), Constants.UPLOAD_STATE, Constants.OFF);
                }
            }

        }

        return null;
    }

    /*
     * A TransferListener class that can listen to a upload task and be notified
     * when the status changes.
     */
    private class UploadListener implements TransferListener {

        // Simply updates the UI list when notified.
        @Override
        public void onError(int id, Exception e) {
            Log.e(DEBUG, "Error during upload: " + id, e);
            numOfErr = numOfErr + 1;
            Utils.writeSharedPreference(getApplicationContext(), Constants.UPLOAD_STATE, Constants.OFF);
            if(numOfErr > 3) {

            }
        }

        @Override
        public void onProgressChanged(int id, long bytesCurrent, long bytesTotal) {
            Log.d(DEBUG, String.format("onProgressChanged: %d, total: %d, current: %d", id, bytesTotal, bytesCurrent));

        }

        @Override
        public void onStateChanged(int id, TransferState newState) {
            Log.d(DEBUG, "onStateChanged: " + id + ", " + newState);
            if(newState.toString().contains("COMPLETED")) {
                map.containsKey(Integer.toString(id));
                String mainPath = "TILEs";

                File csvFile = new File(Environment.getExternalStorageDirectory(),
                        mainPath + "/" + dateFormat.format(dateList.get(folderIndex)).toString() + "/" + "csv");

                if(csvFile.exists()) {
                    if (csvFile.listFiles().length < 1) {
                        Log.d(Constants.DEBUG_UPLOAD, "File_Uploading_Service->UploadListener->Start New transmit");
                        startUploadingService();
                    } else {
                        fileHandler.postDelayed(fileTickExecutor, 1000 * 1);
                    }
                }
            } else if (newState.toString().contains("WAITING_FOR_NETWORK")) {
                Utils.writeSharedPreference(getApplicationContext(), Constants.UPLOAD_STATE, Constants.OFF);
            }

        }
    }

    public boolean isDeviceOnline() {
        ConnectivityManager cm = (ConnectivityManager) getApplicationContext().getSystemService(Context.CONNECTIVITY_SERVICE);
        return cm.getActiveNetworkInfo() != null && cm.getActiveNetworkInfo().isConnectedOrConnecting();
    }


    private BroadcastReceiver uploadCancelReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
        }
    };

    private void startUploadingService() {

        /*
         *   Repeat the recording services every 3min (It will vary according to test results)
         */
        alarmManager = (AlarmManager)getApplicationContext().getSystemService(Context.ALARM_SERVICE);

        Intent upload_Service = new Intent(getApplicationContext(), Summary_Upload.class);
        pendingIntent = PendingIntent.getService(getApplicationContext(), 1, upload_Service, PendingIntent.FLAG_UPDATE_CURRENT);

        /*
         *   Alarm set repeat is not exact and can have significant drift
         * */
        alarmManager.setExact(AlarmManager.ELAPSED_REALTIME_WAKEUP, ALARM_TRIGGER_AT_TIME, pendingIntent);

    }

    private Runnable fileTickExecutor = new Runnable() {
        @Override
        public void run() {

            if(isDeviceOnline() && Utils.retrieveSharedPreference(getApplicationContext(), Constants.UPLOAD_STATE).contains(Constants.ON)) {
                currentFileIndex = currentFileIndex + 1;

                if(currentFileIndex >= files.length) {

                } else {
                    TransferUtility transferUtility = new TransferUtility(s3Client, getApplicationContext());
                    if(files[currentFileIndex].getName().contains("csv")) {

                        uploadFileDirectory = jellyTokenID.substring(0, 4) + "/" + dateFormat.format(dateList.get(folderIndex)).toString() + "/";

                        Log.d(Constants.DEBUG_UPLOAD, "File_Uploading_Service->Start Transmitting File->" + uploadFileDirectory);
                        Log.d(Constants.DEBUG_UPLOAD, "File_Uploading_Service->Start Transmitting File->" + files[currentFileIndex].getName());

                        observer.cleanTransferListener();
                        observer = transferUtility.upload(
                                Constants.S3_BUCKET,
                                uploadFileDirectory + files[currentFileIndex].getName(),
                                files[currentFileIndex]
                        );

                        // Sets listeners to in progress transfers
                        if (TransferState.WAITING.equals(observer.getState())
                                || TransferState.WAITING_FOR_NETWORK.equals(observer.getState())
                                || TransferState.IN_PROGRESS.equals(observer.getState())) {
                            observer.setTransferListener(listener);
                            map.put(observer.getId(), files[currentFileIndex].getName());
                            Log.d(Constants.DEBUG_UPLOAD, "observer: " + observer.getId());
                        }

                        long seconds = files[currentFileIndex].length() / 1024;
                        seconds = seconds / 512;
                        Log.d(Constants.DEBUG_UPLOAD, "File_Uploading_Service->File Size(KB)->" + files[currentFileIndex].length()/1024);
                        Log.d(Constants.DEBUG_UPLOAD, "File_Uploading_Service->Seconds->" + seconds);
                    }

                }

            } else {
                Utils.writeSharedPreference(getApplicationContext(), Constants.DEBUG_UPLOAD, Constants.OFF);
            }
        }
    };
}
