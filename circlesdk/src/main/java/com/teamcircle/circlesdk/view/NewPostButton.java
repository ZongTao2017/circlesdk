package com.teamcircle.circlesdk.view;

import android.content.Context;
import android.content.Intent;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatImageView;

import com.teamcircle.circlesdk.CircleApi;
import com.teamcircle.circlesdk.activity.PhotoPickerActivity;
import com.teamcircle.circlesdk.helper.AppSocialGlobal;
import com.teamcircle.circlesdk.model.TagData;

public class NewPostButton extends AppCompatImageView {
    private String productId;
    private String productName;
    private String productImageUrl;
    private String productCategory;
    private int productPrice;
    private CircleApi.OnPopLoginListener listener;

    public NewPostButton(@NonNull final Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        setImageResource(AppSocialGlobal.newPostResourceId);
        setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (AppSocialGlobal.getInstance().checkIfSignIn()) {
                    AppSocialGlobal.getInstance().photoPickerType = 1;
                    AppSocialGlobal.getInstance().newPostType = 2;
                    AppSocialGlobal.getInstance().tmp_tag = new TagData(productId, productCategory, productName, productImageUrl, productPrice);
                    context.startActivity(new Intent(getContext(), PhotoPickerActivity.class));
                } else {
                    if (listener != null) {
                        listener.onPopLogin(context);
                    } else if (AppSocialGlobal.onPopLoginListener != null) {
                        AppSocialGlobal.onPopLoginListener.onPopLogin(context);
                    }
                }
            }
        });
    }

    public void setOnPopLoginListener(CircleApi.OnPopLoginListener listener) {
        this.listener = listener;
    }

    public void setProductInfo(String productId, String category, String productName, String productImageUrl, int productPrice) {
        this.productId = productId;
        this.productCategory = category;
        this.productName = productName;
        this.productImageUrl = productImageUrl;
        this.productPrice = productPrice;
    }
}
