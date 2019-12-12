package com.teamcircle.circlesdk.helper;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.AsyncHttpResponseHandler;
import com.teamcircle.circlesdk.model.PhotoData;
import com.teamcircle.circlesdk.model.PostData;
import com.teamcircle.circlesdk.model.TagData;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import cz.msebera.android.httpclient.Header;
import cz.msebera.android.httpclient.HttpResponse;
import cz.msebera.android.httpclient.NameValuePair;
import cz.msebera.android.httpclient.client.HttpClient;
import cz.msebera.android.httpclient.client.entity.UrlEncodedFormEntity;
import cz.msebera.android.httpclient.client.methods.HttpPost;
import cz.msebera.android.httpclient.entity.StringEntity;
import cz.msebera.android.httpclient.impl.client.DefaultHttpClient;

public class ApiHelper {
    private static final String BASE_URL = "http://api.teamcircle.io/v1/app/";
    private static AsyncHttpClient client = new AsyncHttpClient();

    public interface ApiCallback {
        void onSuccess(JSONObject response);
        void onFail(String errorMsg);
    }

    public static void login(int userId, String username, ApiCallback callback) {
        try {
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("userId", userId);
            jsonObject.put("username", username);
            post("users/login", jsonObject.toString(), callback);
        } catch (JSONException e) {

        }
    }

    public static void getCategories(ApiCallback callback) {
        get("categories", callback);
    }

    public static void getProducts(int categoryId, String keyword, ApiCallback callback) {
        if (AppSocialGlobal.getInstance().me == null) {
            get("products?categoryId=" + categoryId, callback);
        } else {
            get("products?categoryId=" + categoryId + "&userId=" + AppSocialGlobal.getInstance().me.userId + "&keyword=" + keyword, callback);
        }
    }

    public static void getPostsForProduct(int productId, ApiCallback callback) {
        get("products/" + productId + "/posts?userId=" + AppSocialGlobal.getInstance().me.userId, callback);
    }

    public static void getUserInfo(int userId, ApiCallback callback) {
        get("/users/" + userId, callback);
    }

    public static void viewPost(int postId, ApiCallback callback) {
        try {
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("userId", AppSocialGlobal.getInstance().me.userId);
            post("posts/" + postId + "/visit", jsonObject.toString(), callback);
        } catch (JSONException e) {

        }
    }

    public static void getUserPosts(int userId, ApiCallback callback) {
        get("/users/" + userId + "/posts?userId=" + AppSocialGlobal.getInstance().me.userId, callback);
    }

    public static void getUserPostsNext(int userId, ApiCallback callback) {
        get("/users/" + userId + "/posts/next?userId=" + AppSocialGlobal.getInstance().me.userId, callback);
    }

    public static void editProfile(String profileUrl, String username, String bio, ApiCallback callback) {
        try {
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("avatar", profileUrl);
//            jsonObject.put("username", CharHelper.getUnicodeString(username));
//            jsonObject.put("bio", CharHelper.getUnicodeString(bio));
            put("users/" + AppSocialGlobal.getInstance().me.userId, jsonObject.toString(), callback);
        } catch (JSONException e) {

        }
    }

    public static void sendPost(PostData postData, boolean isContest, String email, ApiCallback callback) {
        try {
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("userId", AppSocialGlobal.getInstance().me.userId);
            jsonObject.put("description", CharHelper.getUnicodeString(postData.caption));
            jsonObject.put("email", email);
            if (isContest) {
                jsonObject.put("postType", "CONTEST");
            } else {
                jsonObject.put("postType", "REGULAR");
                jsonObject.put("contestId", null);
            }
            String hashtag = "";
            ArrayList<String> hashtags = new ArrayList<>(new HashSet<>(postData.hashtags));;
            for (int i = 0; i < hashtags.size(); i++) {
                String s = hashtags.get(i);
                hashtag = hashtag.concat(s.replace("#", ""));
                if (i < hashtags.size() - 1) {
                    hashtag = hashtag.concat(";");
                }
            }
            jsonObject.put("hashtag", hashtag);
            JSONArray jsonArray = new JSONArray();
            boolean mute = false;
            if (postData.photos.size() > 0) {
                for (PhotoData photoData : postData.photos) {
                    JSONObject object = new JSONObject();
                    object.put("imageUrl", photoData.photoUrl + "?" + "size=" + photoData.width + "x" + photoData.height);
                    object.put("videoUrl", null);
                    object.put("videoLength", null);
                    JSONArray tags = new JSONArray();
                    for (TagData tagData : photoData.tags) {
                        JSONObject tag = new JSONObject();
                        if (tagData.productId != 0) {
                            tag.put("productId", tagData.productId);
                        }
                        tag.put("productCategory", tagData.productCategory);
                        tag.put("productImage", tagData.productImage);
                        tag.put("productItemNumber", tagData.productItemNumber);
                        tag.put("productName", tagData.productName);
                        tag.put("x", tagData.percentX);
                        tag.put("y", tagData.percentY);
                        tags.put(tag);
                    }
                    object.put("products", tags);
                    jsonArray.put(object);
                }
            } else {
                JSONObject object = new JSONObject();
                object.put("imageUrl", null);
                object.put("videoUrl", postData.video.videoUrl + "?" +
                        "size=" + postData.video.width + "x" + postData.video.height +
                        "x" + String.format("%.9f", postData.video.startPercentX) +
                        "x" + String.format("%.9f", postData.video.endPercentX) +
                        "x" + String.format("%.9f", postData.video.startPercentY) +
                        "x" + String.format("%.9f", postData.video.endPercentY) +
                        "&photoUrl=" + postData.video.photoUrl);
                object.put("videoLength", postData.video.duration);
                JSONArray tags = new JSONArray();
                for (TagData tagData : postData.video.tags) {
                    JSONObject tag = new JSONObject();
                    tag.put("productId", tagData.productId);
                    tag.put("productImage", tagData.productImage);
                    tag.put("x", tagData.percentX);
                    tag.put("y", tagData.percentY);
                    tags.put(tag);
                }
                object.put("products", tags);
                jsonArray.put(object);
                mute = postData.video.isMuted;
            }
            jsonObject.put("photos", jsonArray);
            jsonObject.put("mute", mute);
            post("posts/send", jsonObject.toString(), callback);
        } catch (JSONException e) {

        }
    }

    public static void getAllPosts(ApiCallback callback) {
        get("posts?userId=" + AppSocialGlobal.getInstance().me.userId, callback);
    }

    public static void getAllPostsNext(ApiCallback callback) {
        get("posts/next?userId=" + AppSocialGlobal.getInstance().me.userId, callback);
    }

    public static void getPost(int postId, ApiCallback callback) {
        get("posts/" + postId + "?userId=" + AppSocialGlobal.getInstance().me.userId, callback);
    }

    public static void deletePost(int postId, ApiCallback callback) {
        delete("posts/" + postId + "?userId=" + AppSocialGlobal.getInstance().me.userId, callback);
    }

    public static void follow(int targetId, ApiCallback callback) {
        try {
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("userId", AppSocialGlobal.getInstance().me.userId);
            post("/users/" + targetId + "/follow", jsonObject.toString(), callback);
        } catch (JSONException e) {

        }
    }

    public static void followHashtag(int hashtagId, ApiCallback callback) {
        try {
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("userId", AppSocialGlobal.getInstance().me.userId);
            jsonObject.put("hashtagId", hashtagId);
            post("hashtag/follow", jsonObject.toString(), callback);
        } catch (JSONException e) {

        }
    }

    public static void getFollowingHashtags(int pageNumber, int userId, ApiCallback callback) {
        get("users/" + userId + "/hashtags?pageNo=" + pageNumber + "&pageSize=20", callback);
    }

    public static void getFollowers(int userId, ApiCallback callback) {
        get("users/" + userId + "/followers", callback);
    }

    public static void getFollowersNext(int userId, ApiCallback callback) {
        get("users/" + userId + "/followers/next", callback);
    }

    public static void getFollowings(int userId, ApiCallback callback) {
        get("users/" + userId + "/followings", callback);
    }

    public static void getFollowingsNext(int userId, ApiCallback callback) {
        get("users/" + userId + "/followings/next", callback);
    }

    public static void likePost(int postId, ApiCallback callback) {
        try {
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("userId", AppSocialGlobal.getInstance().me.userId);
            post("posts/" + postId + "/like", jsonObject.toString(), callback);
        } catch (JSONException e) {

        }
    }

    public static void favorPost(int postId, ApiCallback callback) {
        try {
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("userId", AppSocialGlobal.getInstance().me.userId);
            post("posts/" + postId + "/favor", jsonObject.toString(), callback);
        } catch (JSONException e) {

        }
    }

    public static void getFavors(ApiCallback callback) {
        get("users/" + AppSocialGlobal.getInstance().me.userId +"/favors", callback);
    }

    public static void getFavorsNext(ApiCallback callback) {
        get("users/" + AppSocialGlobal.getInstance().me.userId +"/favors/next", callback);
    }

    public static void commentPost(int postId, String content, ApiCallback callback) {
        try {
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("userId", AppSocialGlobal.getInstance().me.userId);
            jsonObject.put("content", CharHelper.getUnicodeString(content));
            post("posts/" + postId + "/comments", jsonObject.toString(), callback);
        } catch (JSONException e) {

        }
    }

    public static void replyComment(int commentId, String content, ApiCallback callback) {
        try {
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("userId", AppSocialGlobal.getInstance().me.userId);
            jsonObject.put("content", CharHelper.getUnicodeString(content));
            post("posts/comments/" + commentId + "/reply", jsonObject.toString(), callback);
        } catch (JSONException e) {

        }
    }

    public static void likeComment(int commentId, ApiCallback callback) {
        try {
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("userId", AppSocialGlobal.getInstance().me.userId);
            post("posts/comments/" + commentId + "/like", jsonObject.toString(), callback);
        } catch (JSONException e) {

        }
    }

    public static void deleteComment(int commentId, ApiCallback callback) {
        delete("posts/comments/" + commentId + "?userId=" + AppSocialGlobal.getInstance().me.userId, callback);
    }

    public static void searchPeople(String keyword, ApiCallback callback) {
        get("users/search?keyword=" + keyword + "&userId=" + AppSocialGlobal.getInstance().me.userId + "&searchType=HOME_PAGE", callback);
    }

    public static void searchPeopleNext(String keyword, ApiCallback callback) {
        get("users/search?keyword=" + keyword + "&userId=" + AppSocialGlobal.getInstance().me.userId + "&searchType=NEXT_PAGE", callback);
    }

    public static void searchHashtag(String keyword, ApiCallback callback) {
        get("hashtag/search?keyword=" + keyword + "&searchType=HOME_PAGE&userId=" + AppSocialGlobal.getInstance().me.userId, callback);
    }

    public static void searchHashtagNext(String keyword, ApiCallback callback) {
        get("hashtag/search?keyword=" + keyword + "&searchType=NEXT_PAGE&userId=" + AppSocialGlobal.getInstance().me.userId, callback);
    }

    public static void getHashtagPosts(String keyword, ApiCallback callback) {
        get("hashtag/posts?hashtag=" + keyword + "&userId=" + AppSocialGlobal.getInstance().me.userId, callback);
    }

    public static void getHashtagPostsNext(String keyword, ApiCallback callback) {
        get("hashtag/posts/next?hashtag=" + keyword + "&userId=" + AppSocialGlobal.getInstance().me.userId, callback);
    }
    public static void get(final String url, final ApiCallback callback) {
        new Handler(Looper.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                client.get(getAbsoluteUrl(url), null, new AsyncHttpResponseHandler() {
                    @Override
                    public void onSuccess(int statusCode, Header[] headers, byte[] responseBody) {
                        if (callback != null) {
                            try {
                                JSONObject response = new JSONObject(new String(responseBody));
                                boolean result = response.optBoolean("resultStatus");
                                if (result) {
                                    callback.onSuccess(response);
                                } else {
                                    callback.onFail(response.optString("errorCode"));
                                }
                            } catch (JSONException e) {
                                callback.onFail(e.getMessage());
                            }
                        }
                    }

                    @Override
                    public void onFailure(int statusCode, Header[] headers, byte[] responseBody, Throwable error) {
                        if (callback != null) {
                            callback.onFail(error.getMessage());
                        }
                    }
                });
            }
        });
    }

    public static void post(final String url, final String body, final ApiCallback callback) {
        new Handler(Looper.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                try {
                    StringEntity entity = new StringEntity(body);
                    client.post(null, getAbsoluteUrl(url), entity, "application/json", new AsyncHttpResponseHandler() {
                        @Override
                        public void onSuccess(int statusCode, Header[] headers, byte[] responseBody) {
                            if (callback != null) {
                                try {
                                    JSONObject response = new JSONObject(new String(responseBody));
                                    boolean result = response.optBoolean("resultStatus");
                                    if (result) {
                                        callback.onSuccess(response);
                                    } else {
                                        callback.onFail(response.optString("errorCode"));
                                    }
                                } catch (JSONException e) {
                                    callback.onFail(e.getMessage());
                                }
                            }
                        }

                        @Override
                        public void onFailure(int statusCode, Header[] headers, byte[] responseBody, Throwable error) {
                            if (callback != null) {
                                callback.onFail(error.getMessage());
                            }
                        }
                    });
                } catch (UnsupportedEncodingException e) {
                    if (callback != null) {
                        callback.onFail(e.getMessage());
                    }
                }
            }
        });
    }

    public static void post(final String url, final List<NameValuePair> nameValuePairs) {
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    HttpClient httpclient = new DefaultHttpClient();
                    HttpPost httppost = new HttpPost(url);
                    UrlEncodedFormEntity entity = new UrlEncodedFormEntity(nameValuePairs);
                    httppost.setEntity(entity);
                    HttpResponse response = httpclient.execute(httppost);
                    Log.d("POST SUCCESS", response.toString());
                } catch (Exception e) {
                    Log.e("POST FAIL", e.getMessage());
                }
            }
        });
        thread.start();
    }

    public static void put(final String url, final String body, final ApiCallback callback) {
        new Handler(Looper.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                try {
                    StringEntity entity = new StringEntity(body);
                    client.put(null, getAbsoluteUrl(url), entity, "application/json", new AsyncHttpResponseHandler() {
                        @Override
                        public void onSuccess(int statusCode, Header[] headers, byte[] responseBody) {
                            if (callback != null) {
                                try {
                                    JSONObject response = new JSONObject(new String(responseBody));
                                    boolean result = response.optBoolean("resultStatus");
                                    if (result) {
                                        callback.onSuccess(response);
                                    } else {
                                        callback.onFail(response.optString("errorCode"));
                                    }
                                } catch (JSONException e) {
                                    callback.onFail(e.getMessage());
                                }
                            }
                        }

                        @Override
                        public void onFailure(int statusCode, Header[] headers, byte[] responseBody, Throwable error) {
                            if (callback != null) {
                                callback.onFail(error.getMessage());
                            }
                        }
                    });
                } catch (UnsupportedEncodingException e) {
                    if (callback != null) {
                        callback.onFail(e.getMessage());
                    }
                }
            }
        });
    }

    public static void delete(final String url, final ApiCallback callback) {
        new Handler(Looper.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                client.delete(null, getAbsoluteUrl(url), null, "application/json", new AsyncHttpResponseHandler() {
                    @Override
                    public void onSuccess(int statusCode, Header[] headers, byte[] responseBody) {
                        if (callback != null) {
                            try {
                                JSONObject response = new JSONObject(new String(responseBody));
                                boolean result = response.optBoolean("resultStatus");
                                if (result) {
                                    callback.onSuccess(response);
                                } else {
                                    callback.onFail(response.optString("errorCode"));
                                }
                            } catch (JSONException e) {
                                callback.onFail(e.getMessage());
                            }
                        }
                    }

                    @Override
                    public void onFailure(int statusCode, Header[] headers, byte[] responseBody, Throwable error) {
                        if (callback != null) {
                            callback.onFail(error.getMessage());
                        }
                    }
                });
            }
        });
    }

    private static String getAbsoluteUrl(String relativeUrl) {
        return BASE_URL + relativeUrl;
    }
}
