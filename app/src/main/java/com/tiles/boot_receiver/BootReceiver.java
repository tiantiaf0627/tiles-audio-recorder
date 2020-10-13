package com.tiles.boot_receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.tiles.constant.Constants;
import com.example.tar.MainActivity;
import com.tiles.util.Utils;


import java.io.File;

/**
 * Created by tiantianfeng on 8/25/19.
 */

public class BootReceiver extends BroadcastReceiver {

    Context context;

    @Override
    public void onReceive(Context context, Intent intent) {

        this.context = context;

        if ("android.intent.action.BOOT_COMPLETED".equals(intent.getAction())) {

            /*
             * Initialize the preference settings
             * */
            Utils.writeSharedPreference(context, Constants.BT_STATE, Constants.OFF);
            Utils.writeSharedPreference(context, Constants.OPENSMILE_STATE, Constants.OFF);
            Utils.writeSharedPreference(context, Constants.UPLOAD_STATE, Constants.OFF);

            Utils.writeSharedPreference(context, Constants.OPEN_SMILE_SAMPLE_MODE, Integer.toString(Constants.NORMAL_SAMPLE));

            Intent MainActivity_Intent = new Intent(context, MainActivity.class);

            MainActivity_Intent.setAction(Intent.ACTION_MAIN);
            MainActivity_Intent.addCategory(Intent.CATEGORY_LAUNCHER);
            MainActivity_Intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
            context.startActivity(MainActivity_Intent);
        }
    }


}
