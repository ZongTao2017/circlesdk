package com.teamcircle.testSDK;

import android.app.Application;

import com.facebook.flipper.android.AndroidFlipperClient;
import com.facebook.flipper.android.utils.FlipperUtils;
import com.facebook.flipper.core.FlipperClient;
import com.facebook.flipper.plugins.inspector.DescriptorMapping;
import com.facebook.flipper.plugins.inspector.InspectorFlipperPlugin;
import com.facebook.soloader.SoLoader;
import com.teamcircle.circlesdk.CircleApi;

public class TestApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        CircleApi.init(this);
        SoLoader.init(this, false);

        if (FlipperUtils.shouldEnableFlipper(this)) {
            final FlipperClient client = AndroidFlipperClient.getInstance(this);
            client.addPlugin(new InspectorFlipperPlugin(this, DescriptorMapping.withDefaults()));
            client.start();
        }
    }
}
