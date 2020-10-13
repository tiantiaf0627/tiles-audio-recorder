package com.tiles.util;

import android.Manifest;
import android.content.Context;

import com.tiles.constant.Constants;

public final class Permission {

    public static boolean isAllPermissionGranted(Context context) {

        String[] permissionMainName = { Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.RECORD_AUDIO,
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.CAMERA};

        int numberOfGrantedPermissions = 0;
        for(int i = 0; i < 5; i++) {
            if(Utils.retrieveSharedPreference(context, permissionMainName[i]).equals(Integer.toString(Constants.PER_ENABLE))) {
                numberOfGrantedPermissions = numberOfGrantedPermissions + 1;
            }
        }
        if(numberOfGrantedPermissions == 5) {
            Utils.writeSharedPreference(context, Constants.PER_STATUS, Constants.PER_ALL_GRANTED);
        }

        if(Utils.retrieveSharedPreference(context, Constants.PER_STATUS).equals(Constants.PER_ALL_GRANTED)) {
            return true;
        } else {
            return false;
        }
    }

}
