package com.tiles.upload;

import android.app.AlarmManager;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.SystemClock;
import android.util.Log;

import androidx.annotation.Nullable;

import com.amazonaws.ClientConfiguration;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferListener;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferObserver;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferState;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferUtility;
import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3Client;
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

public class Summary_Upload extends Service {

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
    private TransferListener listener = new UploadListener();

    private String atomTokenID, uploadFileDirectory;

    private SimpleDateFormat dateFormat;
    private List<Date> dateList = new ArrayList<>();

    private int numOfErr = 0;

    private TransferObserver observer;

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if(startId > 0) {
            stopSelfResult(startId);
            Log.d(Constants.DEBUG_UPLOAD, "File_Uploading_Service->onStartCommand->stopSelfResult");
        }
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onCreate() {
        super.onCreate();


        if (Constants.S3_ACCESS_KEY.contains("CHANGE_ME")) {
            Log.d(DEBUG, "No key found");
        } else {
            if (isDeviceOnline() && Permission.isAllPermissionGranted(getApplicationContext())) {

                atomTokenID = Utils.retrieveSharedPreference(getApplicationContext(), Constants.MAC_ADDR).replaceAll("[^a-zA-Z0-9]", "");
                Log.d(Constants.DEBUG_UPLOAD, "File_Uploading_Service->onStart->jellyTokenID:" + atomTokenID);

                ClientConfiguration clientConfiguration = new ClientConfiguration();

                clientConfiguration.setMaxErrorRetry(3);
                clientConfiguration.setMaxConnections(8);
                clientConfiguration.setConnectionTimeout(15 * 1000);

                /*
                 *   Init Amazon Client Object and Transfer object
                 * */
                s3Client = new AmazonS3Client(new BasicAWSCredentials(Constants.S3_ACCESS_KEY, Constants.S3_SECRET));
                s3Client.setRegion(Region.getRegion(Regions.US_WEST_2));
                //s3Client.setRegion(Region.getRegion(Regions.DEFAULT_REGION));

                TransferUtility transferUtility = new TransferUtility(s3Client, getApplicationContext());

                SimpleDateFormat format= new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                String myDate = format.format(new Date());

                String mainPath = "TILEs";
                File mainFile = new File(Environment.getExternalStorageDirectory(), mainPath);
                if (!mainFile.exists()) {
                    if (!mainFile.mkdirs()) {
                        Log.e(DEBUG, "File_Uploading_Service->Problem creating folder");
                    }
                }

                dateFormat = new SimpleDateFormat("yyyy-MM-dd");

                if(mainFile.exists()) {

                    Log.d(DEBUG, "File_Uploading_Service->Transimit folder exist");
                    files = mainFile.listFiles();
                    Log.d(DEBUG, "File_Uploading_Service->Size: "+ files.length);
                    if (files.length > 0) {
                        for (File file : files) {
                            if (file.getName().contains("-")) {
                                Log.d(DEBUG, "File_Uploading_Service->File Name: "+ file.getName());
                                try {
                                    Date tempDate = dateFormat.parse(file.getName());
                                    dateList.add(tempDate);
                                } catch (ParseException e) {
                                }
                            }
                        }

                        Collections.sort(dateList);

                        boolean isAllFolderEmpty = true;

                        for(int i = 0; i < dateList.size(); i++) {
                            Log.d(DEBUG, "File_Uploading_Service->Folder: "+ dateFormat.format(dateList.get(i)).toString());
                            File csvFile = new File(Environment.getExternalStorageDirectory(),
                                    mainPath + "/" + dateFormat.format(dateList.get(i)).toString() + "/" + "csv");
                            if(csvFile.exists()) {
                                files = csvFile.listFiles();
                                if (files.length > 0) {
                                    isAllFolderEmpty = false;
                                    folderIndex = i;
                                    Log.d(DEBUG, "File_Uploading_Service->Size: "+ files.length);
                                } else {
                                    //csvFile.delete();

                                    File deleteFolder = new File(Environment.getExternalStorageDirectory(),
                                            mainPath + "/" + dateFormat.format(dateList.get(i)).toString());

                                    Log.d(DEBUG, "File_Uploading_Service->csvFile.exists()->Delete Folder: "+ deleteFolder.getName());

                                    if(deleteFolder.isDirectory()) {
                                        //deleteFolder.delete();
                                    }
                                }
                            } else {
                                File deleteFolder = new File(Environment.getExternalStorageDirectory(),
                                        mainPath + "/" + dateFormat.format(dateList.get(i)).toString());

                                Log.d(DEBUG, "File_Uploading_Service->Delete Folder: "+ deleteFolder.getName());

                                if(deleteFolder.isDirectory()) {
                                    //deleteFolder.delete();
                                }
                            }
                        }

                        if(isAllFolderEmpty) {
                            stopSelf();
                            Utils.writeSharedPreference(getApplicationContext(), Constants.UPLOAD_STATE, Constants.OFF);
                        } else {
                            /*
                             *   A. Transmit the files in first Folder
                             *   B. Delete the Folder once done
                             * */
                            if(dateList.size() > 0) {
                                File csvFile = new File(Environment.getExternalStorageDirectory(),
                                        mainPath + "/" + dateFormat.format(dateList.get(folderIndex)).toString() + "/" + "csv");

                                if(csvFile.exists()) {
                                    files = csvFile.listFiles();
                                    if (files.length > 0) {

                                        Log.d(DEBUG, "File_Uploading_Service->Uploading Size: "+ files.length);

                                        if(files[currentFileIndex].getName().contains("csv")) {

                                            uploadFileDirectory = atomTokenID + "/" + dateFormat.format(dateList.get(folderIndex)).toString() + "/";
                                            Log.d(DEBUG, "File_Uploading_Service->Start Transmitting File->" + uploadFileDirectory);

                                            /*
                                             *   Start Transfer
                                             * */
                                            observer = transferUtility.upload(
                                                    Constants.S3_BUCKET,
                                                    uploadFileDirectory + files[currentFileIndex].getName(),
                                                    files[currentFileIndex]
                                            );

                                            // observer.setTransferListener(new );
                                            // Sets listeners to in progress transfers
                                            if (TransferState.WAITING.equals(observer.getState())
                                                    || TransferState.WAITING_FOR_NETWORK.equals(observer.getState())
                                                    || TransferState.IN_PROGRESS.equals(observer.getState())) {
                                                observer.setTransferListener(listener);
                                                map.put(observer.getId(), files[currentFileIndex].getName());
                                                Log.d(DEBUG, "observer: " + observer.getId());
                                            }
                                        }


                                    }
                                }
                            }

                        }

                    } else {
                        Utils.writeSharedPreference(getApplicationContext(), Constants.UPLOAD_STATE, Constants.OFF);
                        stopSelf();
                    }

                }

            }
        }


    }

    @Override
    public void onStart(Intent intent, int startId) {
        super.onStart(intent, startId);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }


    private Runnable mTickExecutor = new Runnable() {
        @Override
        public void run() {

            alarmManager = (AlarmManager)getSystemService(ALARM_SERVICE);
            Intent battery_Intent = new Intent(getApplicationContext(), Summary_Upload.class);
            pendingIntent = PendingIntent.getService(getApplicationContext(), 1, battery_Intent, PendingIntent.FLAG_ONE_SHOT);

            if(Build.VERSION.SDK_INT >= 23) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.ELAPSED_REALTIME_WAKEUP, SystemClock.elapsedRealtime() + ALARM_INTERVAL, pendingIntent);
                Log.d("Tiles", "File Loading Set Alarm Service");
            }
            else if(Build.VERSION.SDK_INT >= 19) {
                alarmManager.setExact(AlarmManager.ELAPSED_REALTIME_WAKEUP, SystemClock.elapsedRealtime() + ALARM_INTERVAL, pendingIntent);
            } else {
                alarmManager.set(AlarmManager.ELAPSED_REALTIME_WAKEUP, SystemClock.elapsedRealtime() + ALARM_INTERVAL, pendingIntent);
            }

            stopSelf();
        }
    };

    private Runnable fileTickExecutor = new Runnable() {
        @Override
        public void run() {

            if(isDeviceOnline() && Utils.retrieveSharedPreference(getApplicationContext(), Constants.UPLOAD_STATE).contains(Constants.ON)) {
                currentFileIndex = currentFileIndex + 1;

                if(currentFileIndex >= files.length) {

                } else {
                    TransferUtility transferUtility = new TransferUtility(s3Client, getApplicationContext());
                    if(files[currentFileIndex].getName().contains("csv")) {

                        uploadFileDirectory = atomTokenID + "/" + dateFormat.format(dateList.get(folderIndex)).toString() + "/";

                        Log.d(DEBUG, "File_Uploading_Service->Start Transmitting File->" + uploadFileDirectory);
                        Log.d(DEBUG, "File_Uploading_Service->Start Transmitting File->" + files[currentFileIndex].getName());

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
                            Log.d(DEBUG, "observer: " + observer.getId());
                        }

                        long seconds = files[currentFileIndex].length() / 1024;
                        seconds = seconds / 512;
                        Log.d(Constants.DEBUG_UPLOAD, "File_Uploading_Service->File Size(KB)->" + files[currentFileIndex].length()/1024);
                        Log.d(Constants.DEBUG_UPLOAD, "File_Uploading_Service->Seconds->" + seconds);

                        //fileHandler.postDelayed(fileTickExecutor, 1000 * seconds);
                    }

                }

            } else {
                Utils.writeSharedPreference(getApplicationContext(), Constants.UPLOAD_STATE, Constants.OFF);
                stopSelf();
            }
        }
    };


    public boolean isDeviceOnline() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
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
        alarmManager = (AlarmManager)getSystemService(ALARM_SERVICE);

        Intent upload_Service = new Intent(this, Summary_Upload.class);
        pendingIntent = PendingIntent.getService(getApplicationContext(), 1, upload_Service, PendingIntent.FLAG_UPDATE_CURRENT);

        /*
         *   Alarm set repeat is not exact and can have significant drift
         * */
        alarmManager.setExact(AlarmManager.ELAPSED_REALTIME_WAKEUP, ALARM_TRIGGER_AT_TIME, pendingIntent);

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
            stopSelf();
            Utils.writeSharedPreference(getApplicationContext(), Constants.UPLOAD_STATE, Constants.OFF);
            if(numOfErr > 3) {

            }
        }

        @Override
        public void onProgressChanged(int id, long bytesCurrent, long bytesTotal) {
            Log.d(DEBUG, String.format("onProgressChanged: %d, total: %d, current: %d",
                    id, bytesTotal, bytesCurrent));

        }

        @Override
        public void onStateChanged(int id, TransferState newState) {
            Log.d(DEBUG, "onStateChanged: " + id + ", " + newState);
            if(newState.toString().contains("COMPLETED")) {
                map.containsKey(Integer.toString(id));
                String mainPath = "TILEs";
                for( Map.Entry me : map.entrySet() )
                {

                    if (Integer.parseInt(me.getKey().toString()) == id) {

                        File file = new File(Environment.getExternalStorageDirectory(),
                                mainPath + "/" + dateFormat.format(dateList.get(folderIndex)).toString() + "/" + "csv/"
                                        + me.getValue().toString());

                        if(file.exists()) {
                            Log.d(Constants.DEBUG_UPLOAD, "Transmit complete, Delete file");
                            Log.d(Constants.DEBUG_UPLOAD, "File_Uploading_Service->UploadListener->onStateChanged->" + file.getName());
                            file.delete();
                        }

                    }
                }

                File csvFile = new File(Environment.getExternalStorageDirectory(),
                        mainPath + "/" + dateFormat.format(dateList.get(folderIndex)).toString() + "/" + "csv");

                if(csvFile.exists()) {
                    if (csvFile.listFiles().length < 1) {
                        Log.d(Constants.DEBUG_UPLOAD, "File_Uploading_Service->UploadListener->Start New transmit");
                        startUploadingService();
                        stopSelf();
                    } else {
                        fileHandler.postDelayed(fileTickExecutor, 1000 * 1);
                    }
                }
            } else if (newState.toString().contains("WAITING_FOR_NETWORK")) {
                stopSelf();
                Utils.writeSharedPreference(getApplicationContext(), Constants.UPLOAD_STATE, Constants.OFF);
            }

        }
    }

}
