package com.tiles;

import android.Manifest;
import android.content.Context;
import android.os.Build;
import android.os.PowerManager;
import android.util.Log;

import com.tiles.constant.Active_Beacon_ID;
import com.tiles.constant.Constants;
import com.tiles.util.Utils;

public final class Init {

    public static void initSharedPreference(Context context) {

        String[] permissionMainName = { Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.RECORD_AUDIO,
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.CAMERA};

        if(Utils.retrieveSharedPreference(context, Constants.QR_CODE_SCANNED_STATE).isEmpty()){
            Utils.writeSharedPreference(context, Constants.QR_CODE_SCANNED_STATE, Constants.UN_SCANNED);
            // Utils.writeSharedPreference(getApplicationContext(), Constants.QR_CODE_SYNC, Constants.QR_CODE_UN_SYNCED);
        }

        if(Utils.retrieveSharedPreference(context, Constants.UPLOAD_STATE).isEmpty()) {
            Utils.writeSharedPreference(context, Constants.UPLOAD_STATE, Constants.OFF);
        } else {
            Utils.writeSharedPreference(context, Constants.UPLOAD_STATE, Constants.OFF);
        }

        if(Utils.retrieveSharedPreference(context, Constants.BT_STATE).isEmpty()) {
            Utils.writeSharedPreference(context, Constants.BT_STATE, Constants.OFF);
        } else {
            Utils.writeSharedPreference(context, Constants.BT_STATE, Constants.OFF);
        }

        if(Utils.retrieveSharedPreference(context, permissionMainName[0]).isEmpty()) {
            for(int i = 0; i < 5; i++) {
                Utils.writeSharedPreference(context, permissionMainName[i], Integer.toString(Constants.PER_DISABLE));
            }
        }

        if(Utils.retrieveSharedPreference(context, Constants.PER_STATUS).isEmpty()) {
            Utils.writeSharedPreference(context, Constants.PER_STATUS, Constants.PER_NOT_ALL_GRANTED);
        }

        if(Utils.retrieveSharedPreference(context, Constants.OPEN_SMILE_SAMPLE_MODE).isEmpty()) {
            Utils.writeSharedPreference(context, Constants.OPEN_SMILE_SAMPLE_MODE, Integer.toString(Constants.NORMAL_SAMPLE));
        } else {
            Utils.writeSharedPreference(context, Constants.OPEN_SMILE_SAMPLE_MODE, Integer.toString(Constants.NORMAL_SAMPLE));
        }

        if(Utils.retrieveSharedPreference(context, Constants.AUDIO_LENGTH).isEmpty()) {
            Utils.writeSharedPreference(context, Constants.AUDIO_LENGTH, "20");
        }

        Utils.writeSharedPreference(context, Constants.BT_STATE, Constants.OFF);
        Utils.writeSharedPreference(context, Constants.OPENSMILE_STATE, Constants.OFF);

    }

    public static void initBeaconSharedPreference(Context context) {

        for (String key : Active_Beacon_ID.ACTIVE_BEACON_ID_LIST) {
            if(Utils.retrieveSharedPreference(context, key).isEmpty()) {
                Utils.writeSharedPreference(context, key, "0");
            } else {
                Utils.writeSharedPreference(context, key, "0");
            }
        }

    }

    public static void enPower(Context context) {

        PowerManager.WakeLock wakeLock;
        PowerManager powerManager = (PowerManager) context.getSystemService(context.POWER_SERVICE);
        wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK | PowerManager.ACQUIRE_CAUSES_WAKEUP, Constants.DEBUG_MAIN);
        wakeLock.acquire();

        Log.d(Constants.DEBUG_MAIN, Integer.toString(Build.VERSION.SDK_INT));

    }

}
