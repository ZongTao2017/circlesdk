package com.teamcircle.testSDK;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentTransaction;

import android.content.Intent;
import android.os.Bundle;

import com.teamcircle.circlesdk.CircleApi;
import com.teamcircle.circlesdk.fragment.CircleFragment;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        CircleApi.reportUserInfo(1, "");
        CircleApi.setBackIcon(R.drawable.back);

        CircleFragment circle = new CircleFragment();
        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
        ft.add(R.id.content_layout, circle).commit();

        CircleApi.setProductTagOnClickListener(new CircleApi.ProductTagOnClickListener() {
            @Override
            public void onClick(String productId) {
                Intent intent = new Intent(MainActivity.this, ProductActivity.class);
                intent.putExtra("productId", productId);
                startActivity(intent);
            }
        });
    }
}
