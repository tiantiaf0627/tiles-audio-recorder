package com.mvp.presenter;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

import com.data.src.main.java.jelly.data.entities.GetJellyTokenIDResponse;
import com.data.src.main.java.jelly.data.rest.RestDataSource;
import com.domain.src.main.java.com.jelly.domain.GetJellyTokenUseCase;
import com.tiles.constant.Constants;
import com.tiles.util.Utils;

import io.reactivex.Observer;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;

public class JellyTokenPresenter extends Service {

    private GetJellyTokenUseCase mJellyTokenUseCase;

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        this.mJellyTokenUseCase = new GetJellyTokenUseCase(new RestDataSource(),
                Schedulers.newThread(),
                AndroidSchedulers.mainThread());

        Log.d(Constants.DEBUG_QR, "JellyTokenService -> onCreate -> " + Utils.retrieveSharedPreference(getApplicationContext(), Constants.QR_STATE));
        requestJellyToken(Utils.retrieveSharedPreference(getApplicationContext(), Constants.QR_STATE));
    }

    public void requestJellyToken(String jellyToken) {

        mJellyTokenUseCase.passJellyTokenParams(jellyToken);

        mJellyTokenUseCase.execute().subscribe(new Observer<GetJellyTokenIDResponse>() {
            @Override
            public void onSubscribe(Disposable d) {
                Log.d(Constants.DEBUG_QR, "JellyTokenPresenter->GetJellyTokenIDResponse->onSubscribe");
            }

            @Override
            public void onNext(GetJellyTokenIDResponse value) {
                Log.d(Constants.DEBUG_QR, "JellyTokenPresenter->GetJellyTokenIDResponse->onNext:" + value.toString());
                if(value.toString().contains("true")){
                    Utils.writeSharedPreference(getApplicationContext(), Constants.QR_STATE, Constants.SCANNED);
                    Log.d(Constants.DEBUG_QR, "JellyTokenPresenter->GetJellyTokenIDResponse->onNext: SYNCED");
                }
            }

            @Override
            public void onError(Throwable e) {
                e.printStackTrace();
            }

            @Override
            public void onComplete() {
                Log.d(Constants.DEBUG_QR, "JellyTokenPresenter->GetJellyTokenIDResponse->onComplete");
                stopSelf();
            }
        });
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return super.onStartCommand(intent, flags, startId);
    }

}
