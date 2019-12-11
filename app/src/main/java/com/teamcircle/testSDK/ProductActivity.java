package com.teamcircle.testSDK;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;

import androidx.annotation.Nullable;

import com.teamcircle.circlesdk.helper.AppSocialGlobal;
import com.teamcircle.circlesdk.view.CustomerPhotoGallery;

public class ProductActivity extends Activity {
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_product);
        String productId = getIntent().getStringExtra("productId");
        CustomerPhotoGallery gallery = findViewById(R.id.customer_photos);
        gallery.setProductId(productId);

        ImageView backImage = findViewById(com.teamcircle.circlesdk.R.id.back_image);
        backImage.setImageResource(AppSocialGlobal.backResourceId);
        FrameLayout back = findViewById(R.id.back);
        back.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
    }
}
