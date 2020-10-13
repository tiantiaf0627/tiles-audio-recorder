package com.tiles.util;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.util.Log;

import com.tiles.constant.Constants;

import java.io.File;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

public final class Utils {


    public static void writeSharedPreference(Context context, String key, String value) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(key, value);
        editor.apply();
    }

    public static String retrieveSharedPreference(Context context, String key) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        return preferences.getString(key, "");
    }

    public static void countDebugFileSize(Context context, int MODE) {
        File[] files;

        List<Date> dateList = new ArrayList<>();
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
        int fileSize = 0;

        File audioFolder = new File(Environment.getExternalStorageDirectory(), Constants.ROOT_FILE + File.separator + "audio");

        switch (MODE) {
            case Constants.NORMAL_MODE:

                // Date list is days data being saved
                createFolder(new File(Environment.getExternalStorageDirectory(), Constants.ROOT_FILE), Constants.DEBUG_MAIN);
                createFolder(new File(Environment.getExternalStorageDirectory(), Constants.ROOT_FILE + File.separator + "audio"), Constants.DEBUG_MAIN);

                if(audioFolder.exists()) {
                    files = audioFolder.listFiles();
                    if (files.length > 0) {
                        for (File file : files) {
                            if (file.getName().contains("-")) {
                                try {
                                    Date tempDate = dateFormat.parse(file.getName());
                                    dateList.add(tempDate);
                                } catch (ParseException e) {
                                }
                            }
                        }

                        Collections.sort(dateList);
                        for(int i = 0; i < dateList.size(); i++) {
                            File csvFile = new File(Environment.getExternalStorageDirectory(), Constants.ROOT_FILE + File.separator + "audio" + File.separator + dateFormat.format(dateList.get(i)) + File.separator + "csv");
                            if(csvFile.exists()) {
                                files = csvFile.listFiles();
                                if (files.length > 0) {
                                    fileSize = fileSize + files.length;
                                }
                            }
                        }
                        Log.d(Constants.DEBUG_MAIN, "Main->countFileSize->Size: "+ fileSize);
                    }
                }

                if (fileSize > 0) {
                    Log.d(Constants.DEBUG_MAIN, "Main->countFileSize->Size: "+ fileSize);
                    Utils.writeSharedPreference(context, Constants.DEBUG_OPENSMILE_FILE_SIZE, Integer.toString(fileSize));
                } else {
                    Utils.writeSharedPreference(context, Constants.DEBUG_OPENSMILE_FILE_SIZE, Integer.toString(0));
                }

                break;

            case Constants.DEBUG_BATTERY_OPENSMILE_MODE:

                // Date list is days data being saved
                createFolder(new File(Environment.getExternalStorageDirectory(), Constants.ROOT_FILE), Constants.DEBUG_MAIN);
                createFolder(new File(Environment.getExternalStorageDirectory(), Constants.ROOT_FILE + File.separator + "audio"), Constants.DEBUG_MAIN);

                if(audioFolder.exists()) {
                    files = audioFolder.listFiles();
                    if (files.length > 0) {
                        for (File file : files) {
                            if (file.getName().contains("-")) {
                                try {
                                    Date tempDate = dateFormat.parse(file.getName());
                                    dateList.add(tempDate);
                                } catch (ParseException e) {
                                }
                            }
                        }

                        Collections.sort(dateList);
                        for(int i = 0; i < dateList.size(); i++) {
                            File csvFile = new File(Environment.getExternalStorageDirectory(), Constants.ROOT_FILE + File.separator + "audio" + File.separator + dateFormat.format(dateList.get(i)) + File.separator + "csv");
                            if(csvFile.exists()) {
                                files = csvFile.listFiles();
                                if (files.length > 0) {
                                    fileSize = fileSize + files.length;
                                }
                            }
                        }
                        Log.d(Constants.DEBUG_MAIN, "Main->countFileSize->Size: "+ fileSize);
                    }
                }

                if (fileSize > 0) {
                    Log.d(Constants.DEBUG_MAIN, "Main->countFileSize->Size: "+ fileSize);
                    Utils.writeSharedPreference(context, Constants.DEBUG_OPENSMILE_FILE_SIZE, Integer.toString(fileSize));
                } else {
                    Utils.writeSharedPreference(context, Constants.DEBUG_OPENSMILE_FILE_SIZE, Integer.toString(0));
                }

                break;

            case Constants.DEBUG_BATTERY_BT_MODE:

                File csvBeaconFolder = new File(Environment.getExternalStorageDirectory().getAbsoluteFile() + File.separator + Constants.ROOT_FILE + File.separator + "s1_5m");
                createFolder(csvBeaconFolder, Constants.DEBUG_MAIN);

                if(csvBeaconFolder.exists()) {
                    files = csvBeaconFolder.listFiles();
                    if (files.length > 0) {
                        Log.d(Constants.DEBUG_MAIN, "Main->countFileSize->Size: "+ files.length);
                        Utils.writeSharedPreference(context, Constants.DEBUG_BT_FILE_SIZE, Integer.toString(files.length));
                    } else {
                        Utils.writeSharedPreference(context, Constants.DEBUG_BT_FILE_SIZE, Integer.toString(0));
                    }
                }
                break;

            default:
                break;

        }
    }

    public static void createFolder(File file, String DEBUG_MSG) {
        if (!file.exists()) {
            if (!file.mkdirs()) {
                Log.e(DEBUG_MSG, "Problem creating folder");
            }
        }
    }

}
