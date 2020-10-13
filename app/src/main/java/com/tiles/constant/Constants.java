package com.tiles.constant;

import android.os.ParcelUuid;

/**
 * Created by tiantianfeng on 8/22/19.
 */

public class Constants {

    /*
     *   Debug related parameters
     * */
    public static String DEBUG_MAIN      = "DEBUG_MAIN";
    public static String DEBUG_BT        = "DEBUG_BLE_BT";
    public static String DEBUG_BATTERY   = "DEBUG_BATTERY";
    public static String DEBUG_OPENSMILE = "DEBUG_OPENSMILE";
    public static String DEBUG_OWL       = "DEBUG_OWL";
    public static String DEBUG_S1        = "DEBUG_S1";
    public static String DEBUG_UPLOAD    = "DEBUG_UPLOAD";
    public static String DEBUG_QR        = "DEBUG_QR";

    /*
     *   Generic parameters
     * */
    public static String ON  = "ON";
    public static String OFF = "OFF";
    public static String RUNNING     = "RUNNING";
    public static String SCANNED     = "SCANNED";
    public static String UN_SCANNED  = "UN_SCANNED";
    public static String ROOT_FILE   = "TILES";
    public static String SAVE_FORMAT = "WAV";
    public static String MAC_ADDR    = "MAC_ADDR";

    /*
     *   STATE related parameters
     * */
    public static String BT_STATE                 = "BT_STATE";
    public static String UPLOAD_STATE             = "UPLOAD_STATE";
    public static String QR_CODE_SCANNED_STATE    = "QR_STATE";
    public static String VAD_STATE                = "VAD_STAE";
    public static String OPENSMILE_STATE          = "OPENSMILE_STATE";
    public static String QR_STATE                 = "QR_STATE";

    /*
     *   Sample related parameters
     * */
    public static int OPENSMILE_DURATION = 20;

    /*
     *   Permission
     * */
    public static String PER_STATUS             = "PER_STATUS";
    public static String PER_ALL_GRANTED        = "GRANTED";
    public static String PER_NOT_ALL_GRANTED    = "UNGRANTED";
    public static int PER_DISABLE               = -1;
    public static int PER_ENABLE                = 0;

    /*
     *   Sample Interval
     * */
    public static String VAD_INTERVAL    = "VAD_INTERVAL";
    public static String BT_INTERVAL     = "BT_INTERVAL";
    public static String AUDIO_INTERVAL  = "AUDIO_INTERVAL";
    public static String AUDIO_LENGTH    = "AUDIO_LENGTH";

    /*
     *   Files
     * */
    public static String DEBUG_BT_FILE_SIZE              = "DEBUG_BT_FILE_SIZE";
    public static String DEBUG_OPENSMILE_FILE_SIZE       = "DEBUG_OPENSMILE_FILE_SIZE";
    public static String DEBUG_BT_FILE_WRITE_TIME        = "DEBUG_BT_FILE_WRITE_TIME";
    public static String DEBUG_OPENSMILE_FILE_WRITE_TIME = "DEBUG_OPENSMILE_FILE_WRITE_TIME";

    /*
     *   Notification ID
     * */
    public static int BT_NOTIFICATION_ID          = 1338;
    public static int BATTERY_NOTIFICATION_ID     = 1339;
    public static int OPENSMILE_NOTIFICATION_ID   = 1340;

    /*
     *  MODE
     * */
    public static final int NORMAL_MODE                     = 0;
    public static final int DEBUG_BATTERY_OPENSMILE_MODE    = 1;
    public static final int DEBUG_BATTERY_BT_MODE           = 2;

    /*
     *  OPENSMILE MODE
     * */
    public static final int DISABLE_SAMPLE                  = 0;
    public static final int NORMAL_SAMPLE                   = 2;
    public static String OPEN_SMILE_SAMPLE_MODE             = "OPENSMILE_SAMPLE_MODE";
    public static String RECORD_DISABLE_COUNTER             = "RECORD_DISABLE_COUNTER";

    /*
     *  Upload service
     * */
    public static final String S3_ACCESS_KEY = "CHANGE_ME";
    public static final String S3_SECRET = "CHANGE_ME";
    public static final String S3_BUCKET = "CHANGE_ME";

    /*
     *  QR code
     * */
    public static String QR_CODE_SCAN_EXTRA = "";
    public static int QR_CODE_SCAN_REQUEST       = 10;

    /*
    * Save Folder
    * */
    public static String INACTIVE            = "INACTIVE";
    public static String ACTIVE              = "ACTIVE";
    public static String OWL_IN_ONE          = "OWL_IN_ONE";


}
