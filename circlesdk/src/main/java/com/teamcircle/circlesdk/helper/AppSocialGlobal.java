package com.teamcircle.circlesdk.helper;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Typeface;
import android.media.AudioManager;
import android.text.Layout;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.TextPaint;
import android.text.method.LinkMovementMethod;
import android.text.style.AbsoluteSizeSpan;
import android.text.style.ClickableSpan;
import android.text.style.ForegroundColorSpan;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.github.hiteshsondhi88.libffmpeg.FFmpeg;
import com.github.hiteshsondhi88.libffmpeg.LoadBinaryResponseHandler;
import com.github.hiteshsondhi88.libffmpeg.exceptions.FFmpegNotSupportedException;
import com.squareup.picasso.Picasso;
import com.teamcircle.circlesdk.CircleApi;
import com.teamcircle.circlesdk.R;
import com.teamcircle.circlesdk.activity.AccountActivity;
import com.teamcircle.circlesdk.activity.CommentActivity;
import com.teamcircle.circlesdk.activity.HashtagActivity;
import com.teamcircle.circlesdk.model.CategoryData;
import com.teamcircle.circlesdk.model.CommentData;
import com.teamcircle.circlesdk.model.CustomerPhotoData;
import com.teamcircle.circlesdk.model.MessageEvent;
import com.teamcircle.circlesdk.model.PhotoData;
import com.teamcircle.circlesdk.model.PostData;
import com.teamcircle.circlesdk.model.ProductData;
import com.teamcircle.circlesdk.model.ProductVariantData;
import com.teamcircle.circlesdk.model.TagData;
import com.teamcircle.circlesdk.model.UserData;
import com.teamcircle.circlesdk.model.VideoData;
import com.teamcircle.circlesdk.service.FileDownloadService;
import com.teamcircle.circlesdk.text.CustomTypefaceSpan;
import com.teamcircle.circlesdk.view.CommentRowView;

import org.greenrobot.eventbus.EventBus;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.regex.Pattern;

import de.hdodenhof.circleimageview.CircleImageView;

public class AppSocialGlobal {
    private static AppSocialGlobal instance;
    private Context context;
    public FFmpeg ffmpeg;
    public UserData me;
    public LinkedHashMap<Integer, UserData> allUsers;
    public PostData tmp_post;
    public TagData tmp_tag;
    public VideoData tmp_video;
    public Bitmap[] tmp_frames;
    public ArrayList<PhotoData> galleryPhotos;
    public ArrayList<VideoData> galleryVideos;
    public LinkedHashMap<Integer, CategoryData> allProducts;
    public LinkedHashMap<Integer, PostData> allPosts;
    public LinkedHashMap<Integer, PostData> mainPosts;
    public int photoPickerType; // 0:change profile image; 1:send photo post; 2:send video post; 3:send photo contest
    public int newPostType; // 0:main; 1:me
    public boolean isMuted = true;
    private int categoryNumber = 0;
    public static CircleApi.OnPopLoginListener onPopLoginListener;
    public static CircleApi.ProductTagOnClickListener productTagOnClickListener;

    public ArrayList<String> downloadList;

    public static int loaderId = 1;

    public static final String AtNamePattern = "(?:@)([A-Za-z0-9_](?:(?:[A-Za-z0-9_]|(?:\\.(?!\\.))){0,28}(?:[A-Za-z0-9_]))?)";
    public static final String HashtagPattern = "(?:#)([A-Za-z0-9_](?:(?:[A-Za-z0-9_]|(?:\\.(?!\\.))){0,28}(?:[A-Za-z0-9_]))?)";
    public static final String NamePattern = "([A-Za-z0-9_](?:(?:[A-Za-z0-9_]|(?:\\.(?!\\.))){0,28}(?:[A-Za-z0-9_]))?)";
    public static final String S3_BUCKET = "https://xkchromesocial090426e0035a4ae88195402d1b113d2b-dev.s3.amazonaws.com/public/";

    public static String textFontRegular;
    public static String textFontBold;
    public static String textFontAction;
    public static String textFontProductName;
    public static String textFontProductPrice;

    public static int favoriteUnselectedResourceId = R.drawable.post_favor;
    public static int favoriteSelectedResourceId = R.drawable.post_favor_sel;
    public static int tagUnselectedResourceId = R.drawable.post_tag;
    public static int tagSelectedResourceId = R.drawable.post_tag_sel;
    public static int likeUnselectedResourceId = R.drawable.post_like;
    public static int likeSelectedResourceId = R.drawable.post_like_sel;
    public static int commentResourceId = R.drawable.post_comment;
    public static int newPostResourceId = R.drawable.add_post;
    public static int backResourceId = R.drawable.left_arrow;

    public interface ProductOnUpdateListener {
        void onUpdate(ProductData productData);
    }

    public static AppSocialGlobal getInstance() {
        if (instance == null) {
            instance = new AppSocialGlobal();
        }
        return instance;
    }

    public void init(Context appContext) {
        context = appContext;
        allUsers = new LinkedHashMap<>();
        allPosts = new LinkedHashMap<>();
        mainPosts = new LinkedHashMap<>();
        downloadList = new ArrayList<>();

        me = new UserData(-1);

        getAllProductsAndPosts();

        ffmpeg = FFmpeg.getInstance(context);
        try {
            ffmpeg.loadBinary(new LoadBinaryResponseHandler() {

                @Override
                public void onStart() {

                }

                @Override
                public void onFailure() {
                    Log.d("ffmpeg load", "fail");
                }

                @Override
                public void onSuccess() {
                    Log.d("ffmpeg load", "success");
                }

                @Override
                public void onFinish() {}
            });
        } catch (FFmpegNotSupportedException e) {
            // Handle if FFmpeg is not supported by device
        }
    }

    public static void setOnPopLoginListener(CircleApi.OnPopLoginListener listener) {
        onPopLoginListener = listener;
    }

    public static void setProductTagOnClickListener(CircleApi.ProductTagOnClickListener listener) {
        productTagOnClickListener = listener;
    }

    public boolean checkIfNeedSignIn(Context context) {
        if (me.userId == -1) {
            if (onPopLoginListener != null) {
                onPopLoginListener.onPopLogin(context);
            }
            return true;
        }
        return false;
    }

    public void signIn(final int userId) {
        me = new UserData(userId);
        ApiHelper.getUserPosts(userId, new ApiHelper.ApiCallback() {
            @Override
            public void onSuccess(JSONObject response) {
                Log.d("APIGetUserPosts success", response.toString());
                getUserById(userId).posts.clear();
                updateUser(response.optJSONObject("resultData"), null);
                EventBus.getDefault().post(new MessageEvent(MessageEvent.MessageEventType.FINISH_AUTH));
            }

            @Override
            public void onFail(String errorMsg) {
                Log.e("APIGetUserPosts fail", errorMsg);
            }
        });
    }

    public void signOut() {
        me = new UserData(-1);
        EventBus.getDefault().post(new MessageEvent(MessageEvent.MessageEventType.SIGN_OUT));
    }

    public void follow(final UserData userData) {
        userData.followed = !userData.followed;
        if (userData.followed) {
            ++me.followingNumber;
            ++userData.followerNumber;
        } else {
            --me.followingNumber;
            --userData.followerNumber;
        }
        EventBus.getDefault().post(new MessageEvent(MessageEvent.MessageEventType.FOLLOW));
        ApiHelper.follow(userData.userId, new ApiHelper.ApiCallback() {
            @Override
            public void onSuccess(JSONObject response) {
                Log.d("APIFollow success", response.toString());
            }

            @Override
            public void onFail(String errorMsg) {
                Log.e("APIFollow fail", errorMsg);
            }
        });
    }

    public void follow(final int userId, final String username, final String avatar, final boolean followStatus) {
        UserData userData;
        if (!allUsers.containsKey(userId)) {
            userData = new UserData(userId);
            userData.username = username;
            userData.photoUrl = avatar;
        } else {
            userData = allUsers.get(userId);
        }
        userData.followed = !followStatus;
        ++me.followingNumber;
        allUsers.put(userId, userData);
        EventBus.getDefault().post(new MessageEvent(MessageEvent.MessageEventType.FOLLOW));
        ApiHelper.follow(userId, new ApiHelper.ApiCallback() {
            @Override
            public void onSuccess(JSONObject response) {
                Log.d("APIFollow success", response.toString());
            }

            @Override
            public void onFail(String errorMsg) {
                Log.e("APIFollow fail", errorMsg);
            }
        });
    }

    public void setText(Context context, TextView textView, String name, String content) {
        Typeface typeface;
        if (AppSocialGlobal.textFontBold != null) {
            typeface = Typeface.createFromAsset(context.getAssets(), AppSocialGlobal.textFontBold);
        } else if (AppSocialGlobal.textFontRegular != null) {
            Typeface tf = Typeface.createFromAsset(context.getAssets(), AppSocialGlobal.textFontRegular);
            typeface = Typeface.create(tf, Typeface.BOLD);
        } else {
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD);
        }
        Typeface typeface2;
        if (AppSocialGlobal.textFontRegular != null) {
            typeface2 = Typeface.createFromAsset(context.getAssets(), AppSocialGlobal.textFontRegular);
        } else {
            typeface2 = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL);
        }

        SpannableStringBuilder builder = new SpannableStringBuilder();
        SpannableString ss1 = new SpannableString(name);
        int textSize1 = context.getResources().getDimensionPixelSize(R.dimen.text_size_1);
        ss1.setSpan(new AbsoluteSizeSpan(textSize1), 0, name.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        ss1.setSpan(new ForegroundColorSpan(context.getResources().getColor(R.color.blackColor)), 0, name.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

        ss1.setSpan(new CustomTypefaceSpan("", typeface), 0, name.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        builder.append(ss1);
        builder.append("  ");

        SpannableString ss2 = new SpannableString(content);
        ss2.setSpan(new AbsoluteSizeSpan(textSize1), 0, content.length(), 0);
        ss2.setSpan(new ForegroundColorSpan(context.getResources().getColor(R.color.blackColor)), 0, content.length(), 0);
        ss2.setSpan(new CustomTypefaceSpan("", typeface2), 0, content.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        builder.append(ss2);

        textView.setText(builder, TextView.BufferType.SPANNABLE);
    }

    public void setTextClick(final TextView textView, final UserData userData, ArrayList<String> contents, final int postId, final String eventName) {
        final Typeface typeface;
        if (AppSocialGlobal.textFontBold != null) {
            typeface = Typeface.createFromAsset(context.getAssets(), AppSocialGlobal.textFontBold);
        } else if (AppSocialGlobal.textFontRegular != null) {
            Typeface tf = Typeface.createFromAsset(context.getAssets(), AppSocialGlobal.textFontRegular);
            typeface = Typeface.create(tf, Typeface.BOLD);
        } else {
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD);
        }
        final Typeface typeface2;
        if (AppSocialGlobal.textFontRegular != null) {
            typeface2 = Typeface.createFromAsset(context.getAssets(), AppSocialGlobal.textFontRegular);
        } else {
            typeface2 = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL);
        }
        SpannableStringBuilder builder = new SpannableStringBuilder();

        SpannableString ss1 = new SpannableString(userData.username);
        int textSize1 = context.getResources().getDimensionPixelSize(R.dimen.text_size_1);
        ss1.setSpan(new AbsoluteSizeSpan(textSize1), 0, userData.username.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        ss1.setSpan(new ClickableSpan() {
            @Override
            public void onClick(View widget) {
                gotoAccount(userData.userId, textView.getContext());
            }

            @Override
            public void updateDrawState(TextPaint ds) {
                ds.setColor(context.getResources().getColor(R.color.blackColor));
                ds.setTypeface(typeface);
                ds.setUnderlineText(false);
            }
        }, 0, userData.username.length(), 0);
        builder.append(ss1);
        builder.append("  ");

        for (int i = 0; i < contents.size(); i++) {
            final String caption = contents.get(i);
            SpannableString ss2 = new SpannableString(caption);
            ss2.setSpan(new AbsoluteSizeSpan(textSize1), 0, caption.length(), 0);
            if (i % 2 == 0) {
                ss2.setSpan(new ForegroundColorSpan(context.getResources().getColor(R.color.blackColor)), 0, caption.length(), 0);
            } else {
                ss2.setSpan(new ClickableSpan() {
                    @Override
                    public void onClick(View widget) {
                        Intent intent = new Intent(context, HashtagActivity.class);
                        intent.putExtra("hashtag", caption.replace("#",""));
                        context.startActivity(intent);
                    }

                    @Override
                    public void updateDrawState(TextPaint ds) {
                        ds.setColor(context.getResources().getColor(R.color.lightBlueColor));
                        ds.setTypeface(typeface2);
                        ds.setUnderlineText(false);
                    }
                }, 0, caption.length(), 0);
            }
            builder.append(ss2);
        }

        String s = " ";
        SpannableString ss2 = new SpannableString(s);
        ss2.setSpan(new AbsoluteSizeSpan(textSize1), 0, s.length(), 0);
        ss2.setSpan(new ForegroundColorSpan(context.getResources().getColor(R.color.blackColor)), 0, s.length(), 0);
        ss2.setSpan(new CustomTypefaceSpan("", typeface2), 0, s.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

        builder.append(ss2);

        textView.setText(builder, TextView.BufferType.NORMAL);
        textView.setMovementMethod(LinkMovementMethod.getInstance());
    }

    public void setTextExpand(final TextView textView, String name, ArrayList<String> contents, final Context context, final PostData postData, final String eventName) {
        Typeface typeface;
        if (AppSocialGlobal.textFontBold != null) {
            typeface = Typeface.createFromAsset(context.getAssets(), AppSocialGlobal.textFontBold);
        } else if (AppSocialGlobal.textFontRegular != null) {
            Typeface tf = Typeface.createFromAsset(context.getAssets(), AppSocialGlobal.textFontRegular);
            typeface = Typeface.create(tf, Typeface.BOLD);
        } else {
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD);
        }
        final Typeface typeface2;
        if (AppSocialGlobal.textFontRegular != null) {
            typeface2 = Typeface.createFromAsset(context.getAssets(), AppSocialGlobal.textFontRegular);
        } else {
            typeface2 = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL);
        }

        SpannableStringBuilder builder = new SpannableStringBuilder();

        SpannableString ss1 = new SpannableString(name);
        int textSize1 = context.getResources().getDimensionPixelSize(R.dimen.text_size_1);
        ss1.setSpan(new AbsoluteSizeSpan(textSize1), 0, name.length(), 0);
        ss1.setSpan(new ForegroundColorSpan(context.getResources().getColor(R.color.blackColor)), 0, name.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        ss1.setSpan(new CustomTypefaceSpan("", typeface), 0, name.length(), 0);
        builder.append(ss1);
        builder.append("  ");

        for (int i = 0; i < contents.size(); i++) {
            final String caption = contents.get(i);
            SpannableString ss2 = new SpannableString(caption);
            ss2.setSpan(new AbsoluteSizeSpan(textSize1), 0, caption.length(), 0);
            if (i % 2 == 0) {
                ss2.setSpan(new ForegroundColorSpan(context.getResources().getColor(R.color.blackColor)), 0, caption.length(), 0);
            } else {
                ss2.setSpan(new ClickableSpan() {
                    @Override
                    public void onClick(View widget) {
                        Intent intent = new Intent(context, HashtagActivity.class);
                        intent.putExtra("hashtag", caption.replace("#",""));
                        context.startActivity(intent);
                    }

                    @Override
                    public void updateDrawState(TextPaint ds) {
                        ds.setColor(context.getResources().getColor(R.color.lightBlueColor));
                        ds.setTypeface(typeface2);
                        ds.setUnderlineText(false);
                    }
                }, 0, caption.length(), 0);
            }
            builder.append(ss2);
        }

        String s = " ";
        SpannableString ss2 = new SpannableString(s);
        ss2.setSpan(new AbsoluteSizeSpan(textSize1), 0, s.length(), 0);
        ss2.setSpan(new ForegroundColorSpan(context.getResources().getColor(R.color.blackColor)), 0, s.length(), 0);
        ss2.setSpan(new CustomTypefaceSpan("", typeface2), 0, s.length(), 0);
        builder.append(ss2);

        textView.setText(builder, TextView.BufferType.NORMAL);

        ViewTreeObserver viewTreeObserver = textView.getViewTreeObserver();
        viewTreeObserver.addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener()
        {
            @Override
            public void onGlobalLayout()
            {
                ViewTreeObserver viewTreeObserver = textView.getViewTreeObserver();
                viewTreeObserver.removeOnGlobalLayoutListener(this);

                textView.setTag(0);
                Layout l = textView.getLayout();
                if (l != null) {
                    int lines = l.getLineCount();
                    if (lines > 0)
                        if (l.getEllipsisCount(lines - 1) > 0)
                            textView.setTag(1);
                }
                int n = (int) textView.getTag();
                if (n == 0) {
                    textView.setMovementMethod(LinkMovementMethod.getInstance());
                }
            }
        });

        textView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int n = (int) textView.getTag();
                if (n == 0) {
                    tmp_post = postData;
                    Intent intent = new Intent(context, CommentActivity.class);
                    intent.putExtra("input", false);
                    context.startActivity(intent);
                } else {
                    textView.setTag(0);
                    textView.setMaxLines(Integer.MAX_VALUE);
                    textView.setMovementMethod(LinkMovementMethod.getInstance());
                }
            }
        });
    }

    public void addCommentRow(final Context context, final CommentData commentData, LinearLayout commentLayout, final int postId, final String eventName) {
        final LinearLayout commentRow = new CommentRowView(context, 0, false);
        ImageView commentImage = commentRow.findViewById(R.id.comment_profile);
        commentImage.setVisibility(View.GONE);
        TextView commentText = commentRow.findViewById(R.id.comment_text);
        setText(context, commentText, commentData.user.username, commentData.content);
        commentLayout.addView(commentRow);
        LinearLayout layout = commentRow.findViewById(R.id.bottom_layout);
        layout.setVisibility(View.GONE);
        FrameLayout like = commentRow.findViewById(R.id.like);
        final ImageView likeImage = commentRow.findViewById(R.id.like_image);
        if (commentData.isLiked) {
            likeImage.setImageResource(R.drawable.post_like_sel);
        } else {
            likeImage.setImageResource(R.drawable.post_like);
        }
        like.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(!checkIfNeedSignIn(context)) {
                    if (commentData.isLiked) {
                        likeImage.setImageResource(R.drawable.post_like);
                        commentData.likeNumber--;
                    } else {
                        likeImage.setImageResource(R.drawable.post_like_sel);
                        commentData.likeNumber++;
                    }
                    commentData.isLiked = !commentData.isLiked;
                    ApiHelper.likeComment(commentData.commentId, new ApiHelper.ApiCallback() {
                        @Override
                        public void onSuccess(JSONObject response) {
                            Log.d("APILikeComment success", response.toString());
                        }

                        @Override
                        public void onFail(String errorMsg) {
                            Log.d("APILikeComment fail", errorMsg);
                        }
                    });
                }

            }
        });
    }

    public static int dpToPx(Context context, float dp) {
        DisplayMetrics displayMetrics = context.getResources().getDisplayMetrics();
        return (int)(dp * displayMetrics.density);
    }

    public static int pxToDp(Context context, float px) {
        DisplayMetrics displayMetrics = context.getResources().getDisplayMetrics();
        return (int)(px / displayMetrics.density);
    }

    public static int getScreenWidth(Context context) {
        DisplayMetrics displayMetrics = context.getResources().getDisplayMetrics();
        return displayMetrics.widthPixels;
    }

    public static int getScreenHeight(Context context) {
        DisplayMetrics displayMetrics = context.getResources().getDisplayMetrics();
        return displayMetrics.heightPixels;
    }

    public static Bitmap getResizedBitmapWithFixedWidth(Bitmap image, int fixedWidth) {
        int width = image.getWidth();
        int height = image.getHeight();

        float ratio = (float) fixedWidth / (float) width;
        height = (int) (height * ratio);
        return Bitmap.createScaledBitmap(image, fixedWidth, height, true);
    }

    public static Bitmap getResizedBitmap(Bitmap image, int maxSize) {
        int width = image.getWidth();
        int height = image.getHeight();

        float bitmapRatio = (float) width / (float) height;
        if (bitmapRatio > 1) {
            width = maxSize;
            height = (int) (width / bitmapRatio);
        } else {
            height = maxSize;
            width = (int) (height * bitmapRatio);
        }

        return Bitmap.createScaledBitmap(image, width, height, true);
    }

    public void updateFileUrl(String oldUrl, String newUrl) {
        if (oldUrl.equals(me.photoUrl)) {
            me.photoUrl = newUrl;
        }
        for (PostData postData : me.posts.values()) {
            if (postData.video != null) {
                if (oldUrl.equals(postData.video.videoUrl)) {
                    postData.video.videoUrl = newUrl;
                }
                if (oldUrl.equals(postData.video.photoUrl)) {
                    postData.video.photoUrl = newUrl;
                }
            }
            if (postData.photos != null) {
                for (PhotoData photoData : postData.photos) {
                    if (oldUrl.equals(photoData.photoUrl)) {
                        photoData.photoUrl = newUrl;
                    }
                }
            }
        }
        for (UserData userData : allUsers.values()) {
           if (oldUrl.equals(userData.photoUrl)) {
               userData.photoUrl = newUrl;
           }
            for (PostData postData : userData.posts.values()) {
                if (postData.video != null) {
                    if (oldUrl.equals(postData.video.videoUrl)) {
                        postData.video.videoUrl = newUrl;
                    }
                    if (oldUrl.equals(postData.video.photoUrl)) {
                        postData.video.photoUrl = newUrl;
                    }
                }
                if (postData.photos != null) {
                    for (PhotoData photoData : postData.photos) {
                        if (oldUrl.equals(photoData.photoUrl)) {
                            photoData.photoUrl = newUrl;
                        }
                    }
                }
            }
        }
        for (PostData postData : allPosts.values()) {
            if (postData.video != null) {
                if (oldUrl.equals(postData.video.videoUrl)) {
                    postData.video.videoUrl = newUrl;
                }
                if (oldUrl.equals(postData.video.photoUrl)) {
                    postData.video.photoUrl = newUrl;
                }
            }
            if (postData.photos != null) {
                for (PhotoData photoData : postData.photos) {
                    if (oldUrl.equals(photoData.photoUrl)) {
                        photoData.photoUrl = newUrl;
                    }
                }
            }
        }
    }

    public boolean updatePosts(JSONArray postJsonArray, LinkedHashMap<Integer, PostData> posts) {
        if (postJsonArray == null) return false;
        for (int i = 0; i < postJsonArray.length(); i++) {
            JSONObject post = postJsonArray.optJSONObject(i);
            int postId = post.optInt("postId");
            PostData postData = new PostData(postId);
            postData.date = post.optLong("createTime");
            if (allPosts.containsKey(postId)) {
                postData = allPosts.get(postId);
            }
            if (postData != null) {
                postData.photos.clear();
                postData.likeFollowNames.clear();
                JSONObject user = post.optJSONObject("user");
                postData.user = updateUser(user, null);
                postData.setCaption(CharHelper.getString(post.optString("description")));
                postData.likeNumber = post.optInt("likeNumber");
                JSONArray names = post.optJSONArray("followedUserNames");
                for (int j = 0; j < names.length(); j++) {
                    String name = names.optString(j);
                    postData.likeFollowNames.add(name);
                }
                postData.isLiked = post.optBoolean("likeStatus");
                postData.isFavored = post.optBoolean("favorStatus");
                JSONArray photos = post.optJSONArray("photos");
                if (photos != null) {
                    for (int j = 0; j < photos.length(); j++) {
                        JSONObject photo = photos.optJSONObject(j);
                        if (!photo.isNull("videoUrl")) {
                            String[] ss = photo.optString("videoUrl").split("\\?");
                            if (ss.length == 2) {
                                String videoUrl = ss[0];
                                String[] ss2 = ss[1].split("&photoUrl=");
                                String s = ss2[0].replace("size=", "");
                                String[] sss = s.split("x");
                                VideoData videoData;
                                int width = Integer.parseInt(sss[0]);
                                int height = Integer.parseInt(sss[1]);
                                videoData = new VideoData(videoUrl, width, height);
                                videoData.isMuted = post.optBoolean("mute");
                                videoData.startPercentX = Float.parseFloat(sss[2]);
                                videoData.endPercentX = Float.parseFloat(sss[3]);
                                videoData.startPercentY = Float.parseFloat(sss[4]);
                                videoData.endPercentY = Float.parseFloat(sss[5]);
                                videoData.duration = Integer.parseInt(photo.optString("videoLength"));
                                if (!photo.isNull("products")) {
                                    JSONArray tags = photo.optJSONArray("products");
                                    for (int k = 0; k < tags.length(); k++) {
                                        JSONObject tag = tags.optJSONObject(k);
                                        int productId = tag.optInt("productId");
                                        String productImage = tag.optString("productImage");
                                        float x = (float) tag.optDouble("x");
                                        float y = (float) tag.optDouble("y");
                                        if (getProductById(productId) != null) {
                                            TagData tagData = new TagData(productId, productImage, x, y);
                                            videoData.tags.add(tagData);
                                        }
                                    }
                                }
                                postData.video = videoData;
                                if (ss2.length == 2) {
                                    String photoUrl = ss2[1];
                                    videoData.photoUrl = photoUrl;
                                    AppSocialGlobal.getInstance().cachePhoto(photoUrl);
                                }
                                startDownload(postData.video.videoUrl);
                            }
                        }
                        if (!photo.isNull("imageUrl")) {
                            String[] ss = photo.optString("imageUrl").split("\\?");
                            if (ss.length == 2) {
                                String photoUrl = ss[0];
                                String s = ss[1].replace("size=", "");
                                String[] sss = s.split("x");
                                if (sss.length == 2) {
                                    int width = Integer.parseInt(sss[0]);
                                    int height = Integer.parseInt(sss[1]);
                                    PhotoData photoData = new PhotoData(photoUrl, width, height);
                                    if (!photo.isNull("products")) {
                                        JSONArray tags = photo.optJSONArray("products");
                                        for (int k = 0; k < tags.length(); k++) {
                                            JSONObject tag = tags.optJSONObject(k);
                                            int productId = tag.optInt("productId");
                                            String productImage = tag.optString("productImage");
                                            float x = (float) tag.optDouble("x");
                                            float y = (float) tag.optDouble("y");
                                            if (getProductById(productId) != null) {
                                                TagData tagData = new TagData(productId, productImage, x, y);
                                                photoData.tags.add(tagData);
                                            }
                                        }
                                    }
                                    postData.photos.add(photoData);
                                }
                                AppSocialGlobal.getInstance().cachePhoto(photoUrl);
                            }
                        }
                    }
                    postData.comments.clear();
                    if (!post.isNull("comments")) {
                        JSONArray comments = post.optJSONArray("comments");
                        for (int j = 0; j < comments.length(); j++) {
                            JSONObject comment = comments.optJSONObject(j);
                            postData.comments.add(generateComment(comment));
                        }
                    }
                }

                allPosts.put(postId, postData);
                if (posts != null) {
                    posts.put(postId, postData);
                }
            }
        }
        if (postJsonArray.length() == 0) return false;
        return true;
    }

    public PostData getPostById(int postId) {
        if (me.posts.containsKey(postId)) {
            return me.posts.get(postId);
        } else if (allPosts.containsKey(postId)) {
            return allPosts.get(postId);
        } else {
            return null;
        }
    }

    public UserData getUserById(int userId) {
        if (me.userId == userId) {
            return me;
        } else if (allUsers.containsKey(userId)) {
            return allUsers.get(userId);
        } else {
            UserData userData = new UserData(userId);
            allUsers.put(userId, userData);
            return userData;
        }
    }

    public boolean updateUsers(JSONArray jsonArray, LinkedHashMap<Integer, UserData> users) {
        if (jsonArray != null) {
            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject jsonObject = jsonArray.optJSONObject(i);
                updateUser(jsonObject, users);
            }
        }
        if (jsonArray.length() == 0) return false;
        return true;
    }

    public UserData updateUser(JSONObject jsonObject, LinkedHashMap<Integer, UserData> users) {
        if (jsonObject != null) {
            UserData userData;
            int userId = jsonObject.optInt("userId");
            if (me.userId == userId) {
                userData = me;
            } else if (allUsers.containsKey(userId)) {
                userData = allUsers.get(userId);
            } else {
                userData = new UserData(userId);
            }
            if (userData != null) {
                if (!jsonObject.isNull("username")) {
                    userData.username = CharHelper.getString(jsonObject.optString("username"));
                }
                if (!jsonObject.isNull("email")) {
                    userData.email = jsonObject.optString("email");
                }
                if (!jsonObject.isNull("bio")) {
                    userData.bio = CharHelper.getString(jsonObject.optString("bio"));
                }
                if (!jsonObject.isNull("avatar")) {
                    userData.photoUrl = jsonObject.optString("avatar").split("\\?")[0];
                    AppSocialGlobal.getInstance().cachePhoto(userData.photoUrl);
                }
                if (!jsonObject.isNull("followStatus")) {
                    userData.followed = jsonObject.optBoolean("followStatus");
                }
                if (!jsonObject.isNull("followedStatus")) {
                    userData.followingMe = jsonObject.optBoolean("followedStatus");
                }
                if (!jsonObject.isNull("followerNumber")) {
                    userData.followerNumber = jsonObject.optInt("followerNumber");
                }
                if (!jsonObject.isNull("followersNum")) {
                    userData.followerNumber = jsonObject.optInt("followersNum");
                }
                if (!jsonObject.isNull("followingNumber")) {
                    userData.followingNumber = jsonObject.optInt("followingNumber");
                }
                if (!jsonObject.isNull("favoriteNumber")) {
                    userData.favoriteNumber = jsonObject.optInt("favoriteNumber");
                }
                if (!jsonObject.isNull("postTotalNum")) {
                    userData.postNumber = jsonObject.optInt("postTotalNum");
                }
                if (!jsonObject.isNull("posts")) {
                    JSONArray postJsonArray = jsonObject.optJSONArray("posts");
                    updatePosts(postJsonArray, userData.posts);
                }
                allUsers.put(userId, userData);
                if (users != null) {
                    users.put(userId, userData);
                }
            }
            return userData;
        }
        return null;
    }

    public void startDownload(final String url) {
        if (!downloadList.contains(url)) {
            downloadList.add(url);
            new Thread(new Runnable() {
                @Override
                public void run() {
                    Intent intent = new Intent();
                    intent.putExtra("url", url);
                    FileDownloadService.enqueueWork(context, intent);
                }
            }).start();

        }
    }

    public void likePost(final int postId) {
        PostData postData = allPosts.get(postId);
        if (postData != null) {
            if (postData.isLiked) {
                postData.likeNumber--;
            } else {
                postData.likeNumber++;
            }
            postData.isLiked = !postData.isLiked;
        }
        ApiHelper.likePost(postId, new ApiHelper.ApiCallback() {
            @Override
            public void onSuccess(JSONObject response) {
                Log.d("APILikePost success", response.toString());
            }

            @Override
            public void onFail(String errorMsg) {
                Log.e("APILikePost fail", errorMsg);
            }
        });
    }

    public void favorPost(final int postId) {
        PostData postData = allPosts.get(postId);
        if (postData != null) {
            postData.isFavored = !postData.isFavored;
        }
        ApiHelper.favorPost(postId, new ApiHelper.ApiCallback() {
            @Override
            public void onSuccess(JSONObject response) {
                Log.d("APIFavorPost success", response.toString());
            }

            @Override
            public void onFail(String errorMsg) {
                Log.e("APIFavorPost fail", errorMsg);
            }
        });
    }

    public void commentPost(final int postId, String content) {
        ApiHelper.commentPost(postId, content, new ApiHelper.ApiCallback() {
            @Override
            public void onSuccess(JSONObject response) {
                Log.d("APICommentPost success", response.toString());
                ApiHelper.getPost(postId, new ApiHelper.ApiCallback() {
                    @Override
                    public void onSuccess(JSONObject response) {
                        Log.d("APIGetPost success", response.toString());
                        updatePosts(response.optJSONArray("resultData"), null);
                        EventBus.getDefault().post(new MessageEvent(MessageEvent.MessageEventType.UPDATE_POST));
                    }

                    @Override
                    public void onFail(String errorMsg) {
                        Log.d("APIGetPost fail", errorMsg);
                    }
                });

            }

            @Override
            public void onFail(String errorMsg) {
                Log.e("APICommentPost fail", errorMsg);
            }
        });
    }

    public CommentData generateComment(JSONObject comment) {
        int commentId = comment.optInt("commentId");
        int commentUserId = comment.optInt("userId");
        boolean commentLike = comment.optBoolean("commentLikeStatus");
        int likeNumber = comment.optInt("likeNumber");
        String content = CharHelper.getString(comment.optString("content"));
        JSONArray replies = comment.optJSONArray("replies");
        UserData userData = new UserData(commentUserId);
        if (me.userId == commentUserId) {
            userData = me;
        } else {
            if (allUsers.containsKey(commentUserId)) {
                userData = allUsers.get(commentUserId);
            }
            if (userData != null) {
                userData.username = comment.optString("username");
                if (!comment.isNull("avatar")) {
                    userData.photoUrl = comment.optString("avatar").split("\\?")[0];
                }
            }
            allUsers.put(commentUserId, userData);
        }
        CommentData commentData = new CommentData(commentId, userData);
        commentData.isLiked = commentLike;
        commentData.date = comment.optLong("createTime");
        commentData.likeNumber = likeNumber;
        commentData.setContent(content);
        if (replies != null) {
            for (int i = 0; i < replies.length(); i++) {
                JSONObject reply = replies.optJSONObject(i);
                commentData.replies.add(generateComment(reply));
            }
        }
        return commentData;
    }

    public void addPost() {
        LinkedHashMap<Integer, PostData> map = (LinkedHashMap<Integer, PostData>) me.posts.clone();
        me.posts.clear();
        me.posts.put(tmp_post.postId, tmp_post);
        me.posts.putAll(map);
    }

    public ProductData getProductById(int productId) {
        for (int categoryId : allProducts.keySet()) {
            CategoryData categoryData = allProducts.get(categoryId);
            for (ProductData productData : categoryData.products) {
                if (productData.productId == productId) {
                    return productData;
                }
            }
        }
        return null;
    }

    public static FrameLayout addTagView(final Context context, final LinearLayout tagsLayout, final TagData tagData, int number, boolean deletable, final int postId, final String eventName) {
        LayoutInflater inflater = LayoutInflater.from(context);
        final FrameLayout layout = (FrameLayout) inflater.inflate(R.layout.tag_list_item, null, false);
        final ProductData productData = AppSocialGlobal.getInstance().getProductById(tagData.productId);
        ImageView productImage = layout.findViewById(R.id.product_image);
        TextView productName = layout.findViewById(R.id.product_name);
        TextView productPrice = layout.findViewById(R.id.product_price);
        FrameLayout delete = layout.findViewById(R.id.delete);
        FrameLayout showProduct = layout.findViewById(R.id.show_product);
        LinearLayout layout1 = layout.findViewById(R.id.product_content);
        if (deletable) {
            showProduct.setVisibility(View.GONE);
            delete.setVisibility(View.VISIBLE);
        } else {
            showProduct.setVisibility(View.VISIBLE);
            delete.setVisibility(View.GONE);
        }
        delete.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                tagsLayout.removeView(layout);
                AppSocialGlobal.getInstance().tmp_tag = tagData;
                EventBus.getDefault().post(new MessageEvent(MessageEvent.MessageEventType.DELETE_TAG));
            }
        });
        if (productData != null) {
            String productImageUrl = productData.photos.get(0);
            AppSocialGlobal.loadImage(productImageUrl, productImage);
            if (number == 0) {
                productName.setText(productData.name);
            } else {
                productName.setText(number + "- " + productData.name);
            }
            if (productData.priceLow == productData.priceHigh) {
                productPrice.setText(String.format("$%d", productData.priceLow / 100));
            } else {
                productPrice.setText(String.format("$%d - $%d", productData.priceLow / 100, productData.priceHigh / 100));
            }

            if (deletable) {
                layout.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        productTagOnClickListener.onClick(productData.itemNumber);
                    }
                });
            } else {
                layout1.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        productTagOnClickListener.onClick(productData.itemNumber);
                    }
                });
                showProduct.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        productTagOnClickListener.onClick(productData.itemNumber);
                    }
                });
            }
        }
        tagsLayout.addView(layout);
        return layout;
    }

    public void getAllProductsAndPosts() {
        allProducts = new LinkedHashMap<>();
        CategoryData categoryData = new CategoryData(0, "History");
        allProducts.put(0, categoryData);

        ApiHelper.getCategories(new ApiHelper.ApiCallback() {
            @Override
            public void onSuccess(JSONObject response) {
                Log.d("APIGetCategory success", response.toString());
                JSONArray categories = response.optJSONArray("resultData");
                for (int i = 0; i < categories.length(); i++) {
                    JSONObject category = categories.optJSONObject(i);
                    int categoryId = category.optInt("categoryId");
                    final String categoryName = category.optString("categoryName");
                    final CategoryData categoryData = new CategoryData(categoryId, categoryName);
                    allProducts.put(categoryId, categoryData);
                    ApiHelper.getProducts(categoryId, "", new ApiHelper.ApiCallback() {
                        @Override
                        public void onSuccess(JSONObject response) {
                            Log.d("APIGetProducts success", response.toString());
                            JSONArray products = response.optJSONArray("resultData");
                            for (int j = 0; j < products.length(); j++) {
                                JSONObject product = products.optJSONObject(j);
                                int productId = product.optInt("productId");
                                String itemNumber = product.optString("productNo");
                                String productName = product.optString("productName");
                                JSONArray images = product.optJSONArray("images");
                                ArrayList<String> photos = new ArrayList<>();
                                for (int i = 0; i < images.length(); i++) {
                                    photos.add(images.optString(i));
                                    AppSocialGlobal.getInstance().cachePhoto(images.optString(i));
                                }
                                int priceLow = product.optInt("defaultPrice");
                                int priceHigh = product.optInt("defaultPrice");
                                String installTime = product.optString("installTime");
                                String link = product.optString("websiteLink");
                                String description = product.optString("description");
                                String feature = product.optString("productStatus");
                                JSONArray variantArray = product.optJSONArray("variants");
                                ArrayList<ProductVariantData> variants = new ArrayList<>();
                                for (int k = 0; k < variantArray.length(); k++) {
                                    JSONObject variantObj = variantArray.optJSONObject(k);
                                    int variantId = variantObj.optInt("variantId");
                                    String variantName = variantObj.optString("variantName");
                                    int price = variantObj.optInt("unitPrice");
                                    ProductVariantData variantData = new ProductVariantData(variantId,
                                            variantName, price);
                                    variants.add(variantData);
                                }
                                ProductData productData = new ProductData(productId, productName, photos,
                                        priceLow, priceHigh, installTime, link, description, variants, feature);
                                productData.itemNumber = itemNumber;
                                JSONArray customerPhotoArray = product.optJSONArray("customerPhotos");
                                for (int k = 0; k < customerPhotoArray.length(); k++) {
                                    JSONObject photoObj = customerPhotoArray.optJSONObject(k);
                                    if (photoObj.isNull("postId")) continue;
                                    int postId = photoObj.optInt("postId");
                                    String photoUrl = photoObj.optString("photoUrl");
                                    CustomerPhotoData customerPhotoData = new CustomerPhotoData(postId, photoUrl);
                                    boolean duplicate = false;
                                    for (CustomerPhotoData customerPhotoData1 : productData.customerPhotos) {
                                        if (customerPhotoData1.postId == customerPhotoData.postId) {
                                            duplicate = true;
                                        }
                                    }
                                    if (!duplicate) {
                                        productData.customerPhotos.add(customerPhotoData);
                                    }
                                }
                                categoryData.products.add(productData);
                            }
                            categoryNumber++;
                            if (categoryNumber == allProducts.keySet().size()) {
                                ApiHelper.getAllPosts(new ApiHelper.ApiCallback() {
                                    @Override
                                    public void onSuccess(JSONObject response) {
                                        Log.d("APIGetAllPosts success", response.toString());
                                        mainPosts.clear();
                                        updatePosts(response.optJSONArray("resultData"), mainPosts);
                                        EventBus.getDefault().post(new MessageEvent(MessageEvent.MessageEventType.UPDATE_POST));
                                    }

                                    @Override
                                    public void onFail(String errorMsg) {
                                        Log.e("APIGetAllPosts fail", errorMsg);
                                    }
                                });
                                ApiHelper.getUserPosts(me.userId, new ApiHelper.ApiCallback() {
                                    @Override
                                    public void onSuccess(JSONObject response) {
                                        Log.d("APIGetUserPosts success", response.toString());
                                        getUserById(me.userId).posts.clear();
                                        updateUser(response.optJSONObject("resultData"), null);
                                    }

                                    @Override
                                    public void onFail(String errorMsg) {
                                        Log.e("APIGetUserInfo fail", errorMsg);
                                    }
                                });
                            }
                        }

                        @Override
                        public void onFail(String errorMsg) {
                            Log.e("APIGetProducts fail", errorMsg);
                        }
                    });
                }
            }

            @Override
            public void onFail(String errorMsg) {
                Log.e("APIGetCategory fail", errorMsg);
            }
        });
    }

    public static void loadImage(String photoPath, ImageView imageView) {
        if (photoPath == null && imageView instanceof CircleImageView) {
            Picasso.get().load(R.drawable.profile_placeholder).into(imageView);
        }
        if (photoPath != null) {
            if (photoPath.startsWith("http")) {
                Picasso.get().load(photoPath)
                        .noFade()
                        .into(imageView);
            } else {
                File file = new File(photoPath);
                if (file.exists()) {
                    Picasso.get().load(file)
                            .noFade()
                            .into(imageView);
                }
            }
        }
    }

    public static void setLikeNumber(PostData postData, TextView textView) {
        textView.setVisibility(View.VISIBLE);
        if (postData.likeNumber == 0 && postData.likeFollowNames.size() == 0) {
            textView.setVisibility(View.GONE);
        } else {
            if (postData.likeFollowNames.size() == 0) {
                if (postData.likeNumber == 1) {
                    textView.setText("1 like");
                } else {
                    textView.setText(String.format("%d likes", postData.likeNumber));
                }
            } else {
                String s = "";
                for (int i = 0; i < postData.likeFollowNames.size(); i++) {
                    String ss = postData.likeFollowNames.get(i);
                    s += ss;
                    if (i < postData.likeFollowNames.size() - 1) {
                        s += ", ";
                    }
                }
                if (postData.likeNumber == 0) {
                    textView.setText("Liked by " + s);
                } else {
                    textView.setText(String.format("Liked by %s and %d others", s, postData.likeNumber));
                }
            }
        }
    }

    public void checkIfMuted(Context context) {
        isMuted = false;
        AudioManager audio = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        switch( audio.getRingerMode() ){
            case AudioManager.RINGER_MODE_SILENT:
            case AudioManager.RINGER_MODE_VIBRATE:
                isMuted = true;
        }
    }

    public ProductData findProductById(String productId) {
        for (int categoryId : allProducts.keySet()) {
            CategoryData categoryData = allProducts.get(categoryId);
            for (ProductData productData : categoryData.products) {
                if (productData.itemNumber.equals(productId)) {
                    return productData;
                }
            }
        }
        return null;
    }

    public void updateProductById(final String productIdString, final ProductOnUpdateListener listener) {
        for (int categoryId : allProducts.keySet()) {
            final CategoryData categoryData = allProducts.get(categoryId);
            ApiHelper.getProducts(categoryId, "", new ApiHelper.ApiCallback() {
                @Override
                public void onSuccess(JSONObject response) {
                    Log.d("APIGetProducts success", response.toString());
                    JSONArray products = response.optJSONArray("resultData");
                    for (int j = 0; j < products.length(); j++) {
                        JSONObject product = products.optJSONObject(j);
                        int productId = product.optInt("productId");
                        String itemNumber = product.optString("productNo");
                        String productName = product.optString("productName");
                        JSONArray images = product.optJSONArray("images");
                        ArrayList<String> photos = new ArrayList<>();
                        for (int i = 0; i < images.length(); i++) {
                            photos.add(images.optString(i));
                            AppSocialGlobal.getInstance().cachePhoto(images.optString(i));
                        }
                        int priceLow = product.optInt("defaultPrice");
                        int priceHigh = product.optInt("defaultPrice");
                        String installTime = product.optString("installTime");
                        String link = product.optString("websiteLink");
                        String description = product.optString("description");
                        String feature = product.optString("productStatus");
                        JSONArray variantArray = product.optJSONArray("variants");
                        ArrayList<ProductVariantData> variants = new ArrayList<>();
                        for (int k = 0; k < variantArray.length(); k++) {
                            JSONObject variantObj = variantArray.optJSONObject(k);
                            int variantId = variantObj.optInt("variantId");
                            String variantName = variantObj.optString("variantName");
                            int price = variantObj.optInt("unitPrice");
                            ProductVariantData variantData = new ProductVariantData(variantId,
                                    variantName, price);
                            variants.add(variantData);
                        }

                        for (ProductData productData : categoryData.products) {
                            if (productData.itemNumber.equals(itemNumber) &&
                                    itemNumber.equals(productIdString)) {
                                productData.customerPhotos.clear();
                                JSONArray customerPhotoArray = product.optJSONArray("customerPhotos");
                                for (int k = 0; k < customerPhotoArray.length(); k++) {
                                    JSONObject photoObj = customerPhotoArray.optJSONObject(k);
                                    if (photoObj.isNull("postId")) continue;
                                    int postId = photoObj.optInt("postId");
                                    String photoUrl = photoObj.optString("photoUrl");
                                    CustomerPhotoData customerPhotoData = new CustomerPhotoData(postId, photoUrl);
                                    boolean duplicate = false;
                                    for (CustomerPhotoData customerPhotoData1 : productData.customerPhotos) {
                                        if (customerPhotoData1.postId == customerPhotoData.postId) {
                                            duplicate = true;
                                        }
                                    }
                                    if (!duplicate) {
                                        productData.customerPhotos.add(customerPhotoData);
                                    }
                                }
                                listener.onUpdate(productData);
                            }
                        }
                    }
                }

                @Override
                public void onFail(String errorMsg) {
                    Log.e("APIGetProducts fail", errorMsg);
                }
            });
        }
    }

    public void gotoAccount(int userId, Context context) {
        Intent intent = new Intent(context, AccountActivity.class);
        if (userId != me.userId) {
            intent.putExtra("userId", userId);
        }
        context.startActivity(intent);
    }

    public static boolean isPasswordValid(String s) {
        return s.length() >= 7 &&
                Pattern.compile("[0-9]").matcher(s).find() &&
                Pattern.compile("[a-zA-Z]").matcher(s).find();
    }

    public void cachePhoto(String url) {
        Picasso.get().load(url).fetch();
    }
}
