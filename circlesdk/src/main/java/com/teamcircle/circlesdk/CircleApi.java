package com.teamcircle.circlesdk;

import android.content.Context;
import android.content.Intent;

import com.amazonaws.mobileconnectors.s3.transferutility.TransferService;
import com.teamcircle.circlesdk.helper.AmazonS3Helper;
import com.teamcircle.circlesdk.helper.ApiHelper;
import com.teamcircle.circlesdk.helper.AppSocialGlobal;

import org.json.JSONObject;

public class CircleApi {
    public interface ProductTagOnClickListener {
        void onClick(String productId);
    }

    public interface OnPopLoginListener {
        void onPopLogin(Context context);
    }

    public static void init(Context appContext) {
        AppSocialGlobal.getInstance().init(appContext);
        appContext.startService(new Intent(appContext, TransferService.class));
        AmazonS3Helper.getInstance().init(appContext);
    }

    public static void reportUserInfo(final int userId, final String username) {
        if (userId == -1) {
            AppSocialGlobal.getInstance().signOut();
        } else {
            ApiHelper.login(userId, username, new ApiHelper.ApiCallback() {
                @Override
                public void onSuccess(JSONObject response) {
                    AppSocialGlobal.getInstance().signIn(userId);
                    AppSocialGlobal.getInstance().me.username = username;
                    ApiHelper.editUsername(username, new ApiHelper.ApiCallback() {
                        @Override
                        public void onSuccess(JSONObject response) {

                        }

                        @Override
                        public void onFail(String errorMsg) {

                        }
                    });
                }

                @Override
                public void onFail(String errorMsg) {

                }
            });
        }
    }

    public static void setProductTagOnClickListener(ProductTagOnClickListener listener) {
        AppSocialGlobal.setProductTagOnClickListener(listener);
    }

    public static void setOnPopLoginListener(OnPopLoginListener listener) {
        AppSocialGlobal.setOnPopLoginListener(listener);
    }

    public static void setTextFontRegular(String textFont) {
        AppSocialGlobal.textFontRegular = textFont;
    }

    public static void setTextFontBold(String textFont) {
        AppSocialGlobal.textFontBold = textFont;
    }

    public static void setTextFontAction(String textFont) {
        AppSocialGlobal.textFontAction = textFont;
    }

    public static void setTextFontProductName(String textFont) {
        AppSocialGlobal.textFontProductName = textFont;
    }

    public static void setTextFontProductPrice(String textFont) {
        AppSocialGlobal.textFontProductPrice = textFont;
    }

    public static void setFavoriteIconUnselected(int resourceId) {
        AppSocialGlobal.favoriteUnselectedResourceId = resourceId;
    }

    public static void setFavoriteIconSelected(int resourceId) {
        AppSocialGlobal.favoriteSelectedResourceId = resourceId;
    }

    public static void setTagIconUnselected(int resourceId) {
        AppSocialGlobal.tagUnselectedResourceId = resourceId;
    }

    public static void setTagIconSelected(int resourceId) {
        AppSocialGlobal.tagSelectedResourceId = resourceId;
    }

    public static void setLikeIconUnSelected(int resourceId) {
        AppSocialGlobal.likeUnselectedResourceId = resourceId;
    }

    public static void setLikeIconSelected(int resourceId) {
        AppSocialGlobal.likeSelectedResourceId = resourceId;
    }

    public static void setCommentIcon(int resourceId) {
        AppSocialGlobal.commentResourceId = resourceId;
    }

    public static void setNewPostIcon(int resourceId) {
        AppSocialGlobal.newPostResourceId = resourceId;
    }

    public static void setBackIcon(int resourceId) {
        AppSocialGlobal.backResourceId = resourceId;
    }

}
