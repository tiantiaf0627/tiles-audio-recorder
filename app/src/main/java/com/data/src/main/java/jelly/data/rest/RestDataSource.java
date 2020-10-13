package com.data.src.main.java.jelly.data.rest;

import com.data.src.main.java.jelly.data.entities.GetJellyTokenIDResponse;
import com.data.src.main.java.jelly.data.repositories.JellyTokenRespository;
import com.data.src.main.java.jelly.data.utils.URLMapper;
import com.jakewharton.retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory;

import io.reactivex.Observable;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

/**
 * Created by Tiantian on 05/11/17.
 */

public class RestDataSource implements JellyTokenRespository {

    private EndPoints mEndPoints;

    public RestDataSource() {
        Retrofit retrofit = new Retrofit.Builder()
                                    .baseUrl(URLMapper.BASE_URL)
                                    .addConverterFactory(GsonConverterFactory.create())
                                    .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
                                    .build();
        mEndPoints = retrofit.create(EndPoints.class);
    }
    @Override
    public Observable<GetJellyTokenIDResponse> getJellyTokenIDResponse(String jellyTokenID) {
        return mEndPoints.getJellyTokenIDResponse(jellyTokenID);
    }

}
