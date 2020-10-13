package com.example.tar;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Debug;
import android.os.Environment;
import android.os.Handler;
import android.os.PowerManager;
import android.os.StatFs;
import android.os.SystemClock;
import android.provider.Settings;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import android.Manifest;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.tiles.Init;
import com.tiles.battery.Battery_ForegroundService;
import com.tiles.bluetooth.Bluetooth_Scan_Exp;
import com.tiles.bluetooth.Bluetooth_Scan_ForegroundService;
import com.tiles.bluetooth.Bluetooth_Scan_Service;
import com.tiles.constant.Constants;
import com.tiles.constant.Phone_Id;
import com.tiles.debug.DebugOpenSmile;
import com.tiles.debug.DebugOwl;
import com.tiles.opensmile.OpenSmile_ForegroundService;
import com.tiles.qr_code.QR_Code_Activity;
import com.tiles.util.Permission;
import com.tiles.util.Utils;

import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;


public class MainActivity extends AppCompatActivity {

    private PendingIntent pendingIntent;
    public static final long ALARM_TRIGGER_AT_TIME = SystemClock.elapsedRealtime() + 10000;
    private AlarmManager alarmManager;


    private final int MY_PERMISSIONS_REQUEST    = 100;


    private final String[] permissionName = {Manifest.permission.BLUETOOTH,
                                             Manifest.permission.BATTERY_STATS,
                                             Manifest.permission.BLUETOOTH_ADMIN,
                                             Manifest.permission.BLUETOOTH_PRIVILEGED,
                                             Manifest.permission.READ_EXTERNAL_STORAGE,
                                             Manifest.permission.WRITE_EXTERNAL_STORAGE,
                                             Manifest.permission.SET_ALARM, Manifest.permission.WAKE_LOCK,
                                             Manifest.permission.ACCESS_COARSE_LOCATION,
                                             Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.INTERNET,
                                             Manifest.permission.ACCESS_WIFI_STATE,
                                             Manifest.permission.CHANGE_WIFI_STATE,
                                             Manifest.permission.ACCESS_NETWORK_STATE,
                                             Manifest.permission.FOREGROUND_SERVICE,
                                             Manifest.permission.RECEIVE_BOOT_COMPLETED};

    private final String[] permissionMainName = { Manifest.permission.READ_EXTERNAL_STORAGE,
                                                  Manifest.permission.WRITE_EXTERNAL_STORAGE,
                                                  Manifest.permission.RECORD_AUDIO,
                                                  Manifest.permission.ACCESS_COARSE_LOCATION,
                                                  Manifest.permission.CAMERA};

    private ArrayList<String> ungrantPermission;
    private ArrayList<Integer> ungrantPermissionCode;
    private int ungrantPermissionIndex = 0;


    private boolean isHandlerRun = false;
    private Handler mHandler = new Handler();

    /*
     *   UI Related
     * */
    private TextView Running_Status_Textview;
    private TextView Debug_Textview, Participant_Textview;
    private Button TurnOnOffButton, UploadDataButton;
    private TextView FileSize_Textview;
    private Spinner SampleSpinner;

    // private int MODE = Constants.DEBUG_BATTERY_BT_MODE;
    private int MODE = Constants.NORMAL_MODE;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        /*
         *  1. Grant permissions
         * */
        ungrantPermission = new ArrayList<String>();
        ungrantPermissionCode   = new ArrayList<Integer>();
        for (int i = 0; i < permissionName.length; i++) {

            if(!checkPermission(permissionName[i])) {
                ungrantPermission.add(permissionName[i]);
                ungrantPermissionCode.add(i + 1);
            }
        }

        if(ungrantPermission.size() > 0) {
            Log.d(Constants.DEBUG_MAIN, "Request" + ungrantPermission.get(0));
            requestPermission(ungrantPermission.get(0), ungrantPermissionCode.get(0));
            ungrantPermissionIndex = ungrantPermissionIndex + 1;
        }

        /*
         *  2. Ignore the battery optimization
         * */
        isIgnoreBatteryOption(MainActivity.this);

        /*
         *  3. Initiate Battery service
         * */
        init_services();

        /*
         *  4. Initiate UI
         * */
        initUI();
    }

    private void init_services() {
        /*
         *  1. Enable power options
         * */
        Init.enPower(getApplicationContext());

        /*
         *  2. Initialize shared preference
         * */
        Init.initSharedPreference(getApplicationContext());
        Init.initBeaconSharedPreference(getApplicationContext());

        /*
         *  3. Always enable Battery logger to csv
         * */
        if (Permission.isAllPermissionGranted(getApplicationContext()) == true) {
            if(Utils.retrieveSharedPreference(getApplicationContext(), Constants.QR_STATE).equals(Constants.SCANNED)) {
                startBLEExpScanService();
                startOpenSmileForegroundService();
                startBatteryService();
            }
        }

    }


    private void initUI() {

        setContentView(R.layout.activity_main);
        TurnOnOffButton = (Button) findViewById(R.id.turn_on_off_btn);
        Debug_Textview = (TextView) findViewById(R.id.debug_txt);
        Participant_Textview = (TextView) findViewById(R.id.role_txt);
        SampleSpinner = (Spinner) findViewById(R.id.sample_spinner);

        Toolbar toolbar = (Toolbar)findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        initButton();


        SampleSpinner.setOnItemSelectedListener(new SampleOnItemSelectedListener());

        TurnOnOffButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

            /*
             *  Permission granted
             * */
            if(Permission.isAllPermissionGranted(getApplicationContext())) {

                if(Utils.retrieveSharedPreference(getApplicationContext(), Constants.QR_STATE).equals(Constants.SCANNED)) {
                    /*
                     *  Audio is off, turned on
                     * */
                    if(Utils.retrieveSharedPreference(getApplicationContext(), Constants.OPEN_SMILE_SAMPLE_MODE).equals(Integer.toString(Constants.DISABLE_SAMPLE))) {

                        Utils.writeSharedPreference(getApplicationContext(), Constants.OPEN_SMILE_SAMPLE_MODE, Integer.toString(Constants.NORMAL_SAMPLE));
                        TurnOnOffButton.setText("Audio is On");
                        TurnOnOffButton.setBackgroundColor(getResources().getColor(R.color.colorOn));
                        Debug_Textview.setText("Initializing");

                    }
                    /*
                     *  Audio is on, turned off
                     * */
                    else {
                        Utils.writeSharedPreference(getApplicationContext(), Constants.OPEN_SMILE_SAMPLE_MODE, Integer.toString(Constants.DISABLE_SAMPLE));
                        Utils.writeSharedPreference(getApplicationContext(), Constants.RECORD_DISABLE_COUNTER, "0");
                        TurnOnOffButton.setText("Audio is Off");
                        TurnOnOffButton.setBackgroundColor(getResources().getColor(R.color.colorOff));
                        Debug_Textview.setText("Initializing");
                    }

                } else {
                    if(Permission.isAllPermissionGranted(getApplicationContext())) {
                        Toast.makeText(getApplicationContext(),"SCAN QR CODE", Toast.LENGTH_LONG).show();
                        Intent qr_code_activity = new Intent(getApplicationContext(), QR_Code_Activity.class);
                        startActivityForResult(qr_code_activity, Constants.QR_CODE_SCAN_REQUEST);
                        Toast.makeText(getApplicationContext(),"Please scan the QR code First", Toast.LENGTH_LONG).show();
                    } else {
                        Toast.makeText(getApplicationContext(),"Please Enable All the Permissions First", Toast.LENGTH_LONG).show();
                    }

                }


            }
            /*
             *  Permission not granted
             * */
            else {
                Toast.makeText(getApplicationContext(),"Please Enable the Permission First", Toast.LENGTH_LONG).show();
                RequestMultiplePermission();
            }
            }
        });
    }

    private void updateGUI() {

        // UI Showing time
        String debugFileSize;
        String debugFileWriteTime;

        if(Permission.isAllPermissionGranted(getApplicationContext())) {

            if (Utils.retrieveSharedPreference(getApplicationContext(), Constants.QR_STATE).equals(Constants.SCANNED)) {
                Utils.countDebugFileSize(getApplicationContext(), MODE);

                String opensmile_running_str = "Normal";
                Log.d(Constants.DEBUG_MAIN, opensmile_running_str);

                debugFileSize = Utils.retrieveSharedPreference(getApplicationContext(), Constants.DEBUG_OPENSMILE_FILE_SIZE);
                debugFileWriteTime = Utils.retrieveSharedPreference(getApplicationContext(), Constants.DEBUG_OPENSMILE_FILE_WRITE_TIME);

                // Get environment data
                long availableMb = getApplicationContext().getExternalFilesDir(null).getUsableSpace() / (1024 * 1024);
                float gbAvailable   = availableMb / 1024;

                String SampleLength = Utils.retrieveSharedPreference(getApplicationContext(), Constants.AUDIO_LENGTH);
                Debug_Textview.setText("File Number: " + debugFileSize + "\nSpace left: " + Float.toString(gbAvailable) + " GB\n" + "File Write Time: "
                        + debugFileWriteTime + "\nSample Length: " + SampleLength + " Sec");
                Participant_Textview.setText("Participant ID\n" + Utils.retrieveSharedPreference(getApplicationContext(), Constants.MAC_ADDR));

            } else {
                Debug_Textview.setText("Wait for scan id");
                Participant_Textview.setText("Wait for scan id");
            }
        } else {
            Debug_Textview.setText("Wait for permission");
            Participant_Textview.setText("Wait for permission");
        }
    }

    private void initButton() {
        if(Permission.isAllPermissionGranted(getApplicationContext())) {

            if(Utils.retrieveSharedPreference(getApplicationContext(), Constants.QR_STATE).equals(Constants.SCANNED)) {
                if(Utils.retrieveSharedPreference(getApplicationContext(), Constants.DEBUG_BT_FILE_SIZE).isEmpty()) {
                    Utils.writeSharedPreference(getApplicationContext(), Constants.DEBUG_BT_FILE_SIZE, Integer.toString(0));
                }

                if(Utils.retrieveSharedPreference(getApplicationContext(), Constants.DEBUG_BT_FILE_WRITE_TIME).isEmpty()) {
                    Utils.writeSharedPreference(getApplicationContext(), Constants.DEBUG_BT_FILE_WRITE_TIME, "0");
                }

                if (Utils.retrieveSharedPreference(getApplicationContext(), Constants.OPEN_SMILE_SAMPLE_MODE).equals(Integer.toString(Constants.DISABLE_SAMPLE))) {
                    TurnOnOffButton.setText("Audio is Off");
                    TurnOnOffButton.setBackgroundColor(getResources().getColor(R.color.colorOff));
                    Log.d(Constants.DEBUG_MAIN, "Audio is Off");
                } else {
                    TurnOnOffButton.setText("Audio is On");
                    TurnOnOffButton.setBackgroundColor(getResources().getColor(R.color.colorOn));
                    Log.d(Constants.DEBUG_MAIN, "Audio is On");

                    if(Utils.retrieveSharedPreference(getApplicationContext(), Constants.OPENSMILE_STATE).isEmpty()) {
                        Utils.writeSharedPreference(getApplicationContext(), Constants.OPENSMILE_STATE, Constants.ON);
                    }
                }


            } else {
                TurnOnOffButton.setText("Scan QR Code");
                TurnOnOffButton.setBackgroundColor(getResources().getColor(R.color.colorPrimary));
            }


        } else {
            TurnOnOffButton.setText("Enable Permission");
            TurnOnOffButton.setBackgroundColor(getResources().getColor(R.color.colorPrimary));
        }

    }

    @Override
    protected void onResume() {
        super.onResume();

        if (Permission.isAllPermissionGranted(getApplicationContext()) == true) {
            initButton();
            updateGUI();

        }

        if(!isHandlerRun) {
            mHandler.postDelayed(mTickExecutor, 2500);
            isHandlerRun = true;
        }

    }

    private Runnable mTickExecutor = new Runnable() {
        @Override
        public void run() {
            if (Permission.isAllPermissionGranted(getApplicationContext()) == true) {
                initButton();
                updateGUI();
            }
            mHandler.postDelayed(mTickExecutor, 2500);
            isHandlerRun = true;
        }
    };

    @Override
    protected void onStop() {
        super.onStop();
        if(isHandlerRun) {
            mHandler.removeCallbacks(mTickExecutor);
            isHandlerRun = false;
        }
    }

    private boolean checkPermission(String permission) {
        if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
            return false;
        }
        else {
            Log.d(Constants.DEBUG_MAIN, permission + " granted");
            return true;
        }
    }

    private void requestPermission(String permission, int requestCode) {

        if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {

            // Should we show an explanation?
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, permission)) {
                Toast.makeText (this, permission, Toast.LENGTH_LONG).show ();

            } else {
                ActivityCompat.requestPermissions(this, new String[]{permission}, requestCode);
            }
        } else {

            Log.d(Constants.DEBUG_MAIN, permission + " granted");
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        switch (item.getItemId()) {

            case R.id.enable_permission:
                RequestMultiplePermission();
                return true;

            case R.id.scan_qr_code:

                if(Permission.isAllPermissionGranted(getApplicationContext())) {
                    Toast.makeText(getApplicationContext(),"SCAN QR CODE", Toast.LENGTH_LONG).show();
                    Intent qr_code_activity = new Intent(this, QR_Code_Activity.class);
                    startActivityForResult(qr_code_activity, Constants.QR_CODE_SCAN_REQUEST);
                    return true;
                } else {
                    Toast.makeText(getApplicationContext(),"Please Enable All the Permissions First", Toast.LENGTH_LONG).show();
                }

            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void RequestMultiplePermission() {
        ActivityCompat.requestPermissions(MainActivity.this, permissionMainName, MY_PERMISSIONS_REQUEST);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {

        switch (requestCode) {
            case MY_PERMISSIONS_REQUEST: {
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Log.d(Constants.DEBUG_MAIN, "Granted");

                } else if (grantResults.length > 0) {

                    Log.d(Constants.DEBUG_MAIN, "Not grant");
                }

                for (int i = 0; i < permissions.length; i++) {
                    Utils.writeSharedPreference(this, permissions[i], Integer.toString(grantResults[i]));
                }

                int numberOfGrantedPermissions = 0;
                for(int i = 0; i < 5; i++) {
                    Log.d(Constants.DEBUG_MAIN, Utils.retrieveSharedPreference(this, permissionMainName[i]));
                    if(Utils.retrieveSharedPreference(this, permissionMainName[i]).equals(Integer.toString(Constants.PER_ENABLE))) {
                        numberOfGrantedPermissions = numberOfGrantedPermissions + 1;
                    }
                }

                Log.d(Constants.DEBUG_MAIN, "Main->onRequestPermissionsResult->" + numberOfGrantedPermissions);
                if(numberOfGrantedPermissions == 5) {
                    Utils.writeSharedPreference(this, Constants.PER_STATUS, Constants.PER_ALL_GRANTED);
                }


                Log.d(Constants.DEBUG_MAIN, "Main->onRequestPermissionsResult");

                ungrantPermission       = new ArrayList<String>();
                ungrantPermissionCode   = new ArrayList<Integer>();

                for (int i = 0; i < permissionName.length; i++) {

                    if(!checkPermission(permissionName[i])) {
                        ungrantPermission.add(permissionName[i]);
                        ungrantPermissionCode.add(i + 1);
                    }
                }

                if(ungrantPermission.size() > 0) {
                    Log.d(Constants.DEBUG_MAIN, "Request" + ungrantPermission.get(0));
                    requestPermission(ungrantPermission.get(0), ungrantPermissionCode.get(0));
                    ungrantPermissionIndex = ungrantPermissionIndex + 1;
                }

                return;
            }
        }

    }

    /*
     *   Start BLE Worker
     * */
    private void startOpenSmileForegroundService() {
        Log.d(Constants.DEBUG_MAIN, "OPENSMILE FOREGROUND SERVICE START");
        Intent opensmile_Service = new Intent(getApplicationContext(), OpenSmile_ForegroundService.class);
        opensmile_Service.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
        getApplication().getApplicationContext().startService(opensmile_Service);
    }

    /*
     *   Start BLE Exp Service
     * */
    private void startBLEExpScanService() {
        Log.d(Constants.DEBUG_MAIN, "BT SCAN START");
        Intent ble_scan_Service = new Intent(getApplicationContext(), Bluetooth_Scan_Exp.class);
        ble_scan_Service.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
        getApplication().getApplicationContext().startService(ble_scan_Service);
    }

    private void startBatteryService() {

        /*
         *   Repeat the recording services every 3min (It will vary according to test results)
         */
        alarmManager = (AlarmManager)getSystemService(ALARM_SERVICE);

        Intent battery_Intent = new Intent(this, Battery_ForegroundService.class);
        pendingIntent = PendingIntent.getService(MainActivity.this, 1, battery_Intent, PendingIntent.FLAG_UPDATE_CURRENT);

        /*
         *   Alarm set repeat is not exact and can have significant drift
         * */
        alarmManager.setExact(AlarmManager.ELAPSED_REALTIME_WAKEUP, ALARM_TRIGGER_AT_TIME, pendingIntent);

    }

    public static void isIgnoreBatteryOption(Activity activity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            try {
                Intent intent = new Intent();
                String packageName = activity.getPackageName();
                PowerManager pm = (PowerManager) activity.getSystemService(Context.POWER_SERVICE);
                if (!pm.isIgnoringBatteryOptimizations(packageName)) {
                    intent.setAction(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
                    intent.setData(Uri.parse("package:" + packageName));
                    activity.startActivityForResult(intent, 1);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

    }


    public void onActivityResult(int requestCode, int resultCode, Intent intent) {

        if (requestCode == Constants.QR_CODE_SCAN_REQUEST) {
            if(resultCode == Activity.RESULT_OK && intent != null){
                String atomTokenID = intent.getStringExtra(Constants.QR_CODE_SCAN_EXTRA);
                Log.d(Constants.DEBUG_MAIN, "Scanned id: " + atomTokenID);
                // String[] urlParts = result.split("/");
                // String jellyTokenID = urlParts[urlParts.length - 1];

                Utils.writeSharedPreference(getApplicationContext(), Constants.QR_STATE, Constants.SCANNED);
                Utils.writeSharedPreference(getApplicationContext(), Constants.OPEN_SMILE_SAMPLE_MODE, Integer.toString(Constants.NORMAL_SAMPLE));
                Utils.writeSharedPreference(getApplicationContext(), Constants.MAC_ADDR, atomTokenID);
                Toast.makeText(getApplicationContext(), "Sync Complete", Toast.LENGTH_SHORT).show();

                init_services();

            }
        }

    }

    public class SampleOnItemSelectedListener implements AdapterView.OnItemSelectedListener {

        public void onItemSelected(AdapterView<?> parent, View view, int pos,long id) {
            if (pos == 0) {
                Utils.writeSharedPreference(getApplicationContext(), Constants.AUDIO_LENGTH, Integer.toString(20));
            } else if (pos == 1) {
                Utils.writeSharedPreference(getApplicationContext(), Constants.AUDIO_LENGTH, Integer.toString(30));
            } else {
                Utils.writeSharedPreference(getApplicationContext(), Constants.AUDIO_LENGTH, Integer.toString(40));
            }
        }

        @Override
        public void onNothingSelected(AdapterView<?> arg0) {
            // TODO Auto-generated method stub
        }

    }

}
