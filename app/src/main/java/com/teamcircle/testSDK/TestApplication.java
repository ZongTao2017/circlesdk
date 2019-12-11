package com.teamcircle.testSDK;

import android.app.Application;

import com.teamcircle.circlesdk.CircleApi;

public class TestApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        CircleApi.init(this);
    }
}
