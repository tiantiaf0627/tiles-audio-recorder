package com.tiles.view;

import com.tiles.view.base.InternetView;
import com.tiles.view.base.ProgressableView;
import com.tiles.view.base.View;
import com.tiles.view.base.BaseView;

/**
 * Created by Tiantian on 27/11/17.
 */

public interface QRView extends BaseView, InternetView, ProgressableView, View {
    void handleCameraPermission();
    void permissionGranted();
    void permissionRejected();
    void showToast(String message);
}
