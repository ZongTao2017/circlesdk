package com.teamcircle.testSDK;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentTransaction;

import android.content.Intent;
import android.os.Bundle;

import com.teamcircle.circlesdk.CircleApi;
import com.teamcircle.circlesdk.fragment.CircleFragment;
import com.teamcircle.circlesdk.view.NewPostButton;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

//        CircleApi.reportUserInfo(-1, "");
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

        NewPostButton newPostButton = findViewById(R.id.new_post_btn);
        newPostButton.setProductInfo("a332a1a5-18fd-47c7-a5bd-b7fdfacb1511", "Nike", "Free X Metcon 2 Cool Grey", "https://stockx.imgix.net/Free-Metcon-2-Cool-Grey.jpg?fit=fill&bg=FFFFFF&w=700&h=500&auto=format,compress&q=90&dpr=2&trim=color&updated_at=1562184677", 8900);
    }
}
