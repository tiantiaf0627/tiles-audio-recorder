package com.tiles.qr_code;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.ViewGroup;

import androidx.appcompat.app.AppCompatActivity;

import com.example.tar.R;
import com.google.zxing.Result;
import com.tiles.constant.Active_Beacon_ID;
import com.tiles.constant.Constants;

import me.dm7.barcodescanner.zxing.ZXingScannerView;

/**
 * Created by tiantianfeng on 11/30/17.
 */

public class QR_Code_Activity extends AppCompatActivity implements ZXingScannerView.ResultHandler {

    private ZXingScannerView mScannerView;
    private ProgressDialog mProgressDialog;

    @Override
    public void onCreate(Bundle state) {
        super.onCreate(state);
        setContentView(R.layout.qr_code_layout);

        ViewGroup contentFrame = (ViewGroup) findViewById(R.id.content_frame);

        mScannerView = new ZXingScannerView(this);
        contentFrame.addView(mScannerView);

    }

    @Override
    public void onResume() {
        super.onResume();
        mScannerView.setResultHandler(this);
        mScannerView.startCamera();
        mScannerView.setAspectTolerance(0.2f);

    }

    @Override
    public void onPause() {
        super.onPause();
        mScannerView.stopCamera();
    }

    @Override
    public void handleResult(Result rawResult) {
        Log.d(Constants.DEBUG_QR, rawResult.getText());
        Log.d(Constants.DEBUG_QR, rawResult.getBarcodeFormat().toString());

        if(!rawResult.getText().isEmpty()){

            if (rawResult.getText().contains("tar")) {
                Intent returnIntent = new Intent();
                returnIntent.putExtra(Constants.QR_CODE_SCAN_EXTRA, rawResult.getText());
                setResult(Activity.RESULT_OK, returnIntent);
                finish();

                Log.d(Constants.DEBUG_QR, "QR_Code_Activity->handleResult->" + rawResult.getText());
            }

        }

        // If you would like to resume scanning, call this method below:
        Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                mScannerView.resumeCameraPreview(QR_Code_Activity.this);
            }
        }, 2000);
    }
}
