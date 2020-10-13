package com.data.src.main.java.jelly.data.repositories;

import com.data.src.main.java.jelly.data.entities.GetJellyTokenIDResponse;
import io.reactivex.Observable;

/**
 * Created by tiantianfeng on 11/30/17.
 */

public interface JellyTokenRespository {
    Observable<GetJellyTokenIDResponse> getJellyTokenIDResponse(String jellyTokenID);
}
