package com.teamcircle.circlesdk.activity;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.util.Patterns;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.github.hiteshsondhi88.libffmpeg.ExecuteBinaryResponseHandler;
import com.github.hiteshsondhi88.libffmpeg.exceptions.FFmpegCommandAlreadyRunningException;
import com.teamcircle.circlesdk.R;
import com.teamcircle.circlesdk.helper.AmazonS3Helper;
import com.teamcircle.circlesdk.helper.ApiHelper;
import com.teamcircle.circlesdk.helper.AppSocialGlobal;
import com.teamcircle.circlesdk.model.MessageEvent;
import com.teamcircle.circlesdk.model.PhotoData;
import com.teamcircle.circlesdk.model.VideoData;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

public class ReadyToPostActivity extends Activity {
    private TextView mProductNumberText;
    private ArrayList<PhotoData> mPhotos;
    private VideoData mVideoData;
    private EditText mCaptionText;
    private int mUploadCount = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_photo_post);
        EventBus.getDefault().register(this);

        mCaptionText = findViewById(R.id.caption_text);
        mCaptionText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                if (mCaptionText.getLayout() != null) {
                    if (mCaptionText.getLayout().getLineCount() > 10)
                        mCaptionText.getText().delete(mCaptionText.getText().length() - 1, mCaptionText.getText().length());
                }
            }
        });
        ImageView backImage = findViewById(R.id.back_image);
        backImage.setImageResource(AppSocialGlobal.backResourceId);
        FrameLayout back = findViewById(R.id.back);
        back.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
        final ImageView photo = findViewById(R.id.photo);
        ImageView multi = findViewById(R.id.multi);
        ImageView playVideo = findViewById(R.id.video_play);
        FrameLayout tagProduct = findViewById(R.id.tag_product);
        LinearLayout tagInfo = findViewById(R.id.tag_info);

        if (AppSocialGlobal.getInstance().photoPickerType == 2) {
            mVideoData = AppSocialGlobal.getInstance().tmp_post.video;
            playVideo.setVisibility(View.VISIBLE);
            new Thread(new Runnable() {
                @Override
                public void run() {
                    final MediaMetadataRetriever retriever = new MediaMetadataRetriever();
                    retriever.setDataSource(ReadyToPostActivity.this, Uri.parse(mVideoData.videoUrl));
                    Bitmap bitmap = retriever.getFrameAtTime(mVideoData.start * 1000);
                    int width = bitmap.getWidth();
                    int height = bitmap.getHeight();
                    int x = (int) (mVideoData.startPercentX * width);
                    int y = (int) (mVideoData.startPercentY * height);
                    int w = (int) ((mVideoData.endPercentX - mVideoData.startPercentX) * width);
                    int h = (int) ((mVideoData.endPercentY - mVideoData.startPercentY) * height);
                    final Bitmap b = Bitmap.createBitmap(bitmap, x, y, w, h);
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            photo.setImageBitmap(b);
                        }
                    });
                }
            }).start();
        } else {
            mPhotos = AppSocialGlobal.getInstance().tmp_post.photos;
            if (mPhotos.size() > 1) {
                multi.setVisibility(View.VISIBLE);
            }
            String photoUrl = mPhotos.get(0).photoUrl;
            AppSocialGlobal.loadImage(photoUrl, photo);
            photo.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    startActivity(new Intent(ReadyToPostActivity.this, ShowPhotosActivity.class));
                }
            });
        }

        if (AppSocialGlobal.getInstance().newPostType == 2) {
            tagProduct.setVisibility(View.GONE);
            AppSocialGlobal.addTagView(ReadyToPostActivity.this, tagInfo, AppSocialGlobal.getInstance().tmp_tag, 0, false);
            if (AppSocialGlobal.getInstance().photoPickerType == 2) {
                mVideoData.tags.add(AppSocialGlobal.getInstance().tmp_tag);
            } else {
                for (PhotoData photoData : mPhotos) {
                    photoData.tags.add(AppSocialGlobal.getInstance().tmp_tag);
                }
            }
        }

        tagProduct.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (AppSocialGlobal.getInstance().photoPickerType == 2) {
                    startActivity(new Intent(ReadyToPostActivity.this, AddProductTagActivity.class));
                } else {
                    startActivity(new Intent(ReadyToPostActivity.this, TagProductActivity.class));
                }
            }
        });

        mProductNumberText = findViewById(R.id.product_number);

        FrameLayout done = findViewById(R.id.done);
        done.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                switch (AppSocialGlobal.getInstance().photoPickerType) {
                    case 1:
                        savePhotos(false);
                        EventBus.getDefault().post(new MessageEvent(MessageEvent.MessageEventType.DONE_SEND_POST));
                        if (AppSocialGlobal.getInstance().newPostType == 0) {
                            EventBus.getDefault().post(new MessageEvent(MessageEvent.MessageEventType.DONE_SEND_POST_MAIN));
                        } else {
                            EventBus.getDefault().post(new MessageEvent(MessageEvent.MessageEventType.DONE_SEND_POST_ME));
                        }
                        finish();
                        break;
                    case 2:
                        saveVideo();
                        EventBus.getDefault().post(new MessageEvent(MessageEvent.MessageEventType.DONE_SEND_POST));
                        if (AppSocialGlobal.getInstance().newPostType == 0) {
                            EventBus.getDefault().post(new MessageEvent(MessageEvent.MessageEventType.DONE_SEND_POST_MAIN));
                        } else {
                            EventBus.getDefault().post(new MessageEvent(MessageEvent.MessageEventType.DONE_SEND_POST_ME));
                        }
                        finish();
                        break;
                }
            }
        });
    }

    @Override
    protected void onDestroy() {
        EventBus.getDefault().unregister(this);
        super.onDestroy();
    }

    @Subscribe(sticky = true)
    public void onEvent(MessageEvent event) {
        switch (event.type) {
            case DONE_TAG_PRODUCT:
                setProductNumber();
                break;
        }
    }

    private void setProductNumber() {
        int number = 0;
        for (PhotoData photoData : AppSocialGlobal.getInstance().tmp_post.photos) {
            number += photoData.tags.size();
        }
        if (AppSocialGlobal.getInstance().tmp_post.video != null) {
            number += AppSocialGlobal.getInstance().tmp_post.video.tags.size();
        }
        if (number == 0) {
            mProductNumberText.setText("");
        } else if (number == 1) {
            mProductNumberText.setText("1 Item");
        } else {
            mProductNumberText.setText(String.format("%d Items", number));
        }
    }

    private void savePhotos(final boolean isContest) {
        final ArrayList<PhotoData> photos = AppSocialGlobal.getInstance().tmp_post.photos;
        for (final PhotoData photoData : photos) {
            AmazonS3Helper.getInstance().upload(photoData.photoUrl, new AmazonS3Helper.FileUploadCallback() {
                @Override
                public void onComplete(String fileUrl) {
                    Log.d("S3File Upload", "Complete!");
                    photoData.photoUrl = fileUrl;
                    mUploadCount++;
                    if (mUploadCount == photos.size()) {
                        String caption = mCaptionText.getText().toString();
                        AppSocialGlobal.getInstance().tmp_post.setCaption(caption);
                        AppSocialGlobal.getInstance().tmp_post.user = AppSocialGlobal.getInstance().me;
                        ApiHelper.sendPost(AppSocialGlobal.getInstance().tmp_post, isContest, "", new ApiHelper.ApiCallback() {
                            @Override
                            public void onSuccess(JSONObject response) {
                                Log.d("APISendPosts success", response.toString());
                                if (!isContest) {
                                    int postId = response.optInt("resultData");
                                    AppSocialGlobal.getInstance().tmp_post.postId = postId;
                                    AppSocialGlobal.getInstance().addPost();
                                    EventBus.getDefault().post(new MessageEvent(MessageEvent.MessageEventType.UPDATE_POST));
                                }
                            }

                            @Override
                            public void onFail(String errorMsg) {
                                Log.e("APISendPosts fail", errorMsg);
                            }
                        });
                    }
                }
            });
        }
    }

    private void saveVideo() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                String inputFilePath = mVideoData.videoUrl;
                final String dir = getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS).getAbsolutePath();
                String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
                final String trimmedFilePath = dir + "/" + timeStamp + ".mp4";
                final int width = mVideoData.width;
                final int height = mVideoData.height;
                final int duration = mVideoData.end - mVideoData.start;
                String[] cmd = {"-ss", "" + mVideoData.start / 1000, "-t", "" + duration / 1000,
                        "-noaccurate_seek", "-i", inputFilePath, "-y", "-codec", "copy", trimmedFilePath};
//                String[] cmd = {"-ss", "" + mVideoData.start / 1000, "-t", "" + duration / 1000,
//                        "-noaccurate_seek", "-i", inputFilePath, "-y", "-vcodec", "libx264", "-crf", "24", trimmedFilePath};


                try {
                    final long time = new Date().getTime();
                    AppSocialGlobal.getInstance().ffmpeg.execute(cmd, new ExecuteBinaryResponseHandler() {

                        @Override
                        public void onStart() {
                            Log.d("ffmpeg", "start");
                        }

                        @Override
                        public void onProgress(String message) {
                            Log.d("ffmpeg", "progress - " + message);
                        }

                        @Override
                        public void onFailure(String message) {
                            Log.d("ffmpeg", "done fail");
                        }

                        @Override
                        public void onSuccess(String message) {
                            Log.d("ffmpeg", String.format("done success in %d seconds", (new Date().getTime() - time) / 1000));
                            String caption = mCaptionText.getText().toString();
                            AppSocialGlobal.getInstance().tmp_post.setCaption(caption);
                            AppSocialGlobal.getInstance().tmp_post.user = AppSocialGlobal.getInstance().me;
                            mVideoData.width = width;
                            mVideoData.height = height;
                            mVideoData.duration = duration;
                            mVideoData.start = 0;
                            mVideoData.end = duration;
                            mVideoData.videoUrl = trimmedFilePath;
                            uploadVideo();
                        }

                        @Override
                        public void onFinish() {}
                    });
                } catch (FFmpegCommandAlreadyRunningException e) {
                    Log.d("ffmpeg", e.toString());
                }
            }
        }).start();
    }

    private void uploadVideo() {
        final MediaMetadataRetriever retriever = new MediaMetadataRetriever();
        retriever.setDataSource(mVideoData.videoUrl);
        Bitmap b = retriever.getFrameAtTime(1000);
        Bitmap bitmap = Bitmap.createBitmap(b, (int) (mVideoData.startPercentX * mVideoData.width),
                (int) (mVideoData.startPercentY * mVideoData.height),
                (int) ((mVideoData.endPercentX - mVideoData.startPercentX) * mVideoData.width),
                (int) ((mVideoData.endPercentY - mVideoData.startPercentY) * mVideoData.height));
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "JPEG_" + timeStamp;
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS);
        File image = null;
        try {
            image = File.createTempFile(
                    imageFileName,  /* prefix */
                    ".jpg",         /* suffix */
                    storageDir      /* directory */
            );
            FileOutputStream out = new FileOutputStream(image);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, out);
            out.flush();
            out.close();
            String path = image.getAbsolutePath();
            AmazonS3Helper.getInstance().upload(path, new AmazonS3Helper.FileUploadCallback() {
                @Override
                public void onComplete(String fileUrl) {
                    Log.d("S3File Upload", "Complete!");
                    final String photoUrl = fileUrl;
                    AmazonS3Helper.getInstance().upload(mVideoData.videoUrl, new AmazonS3Helper.FileUploadCallback() {
                        @Override
                        public void onComplete(String fileUrl) {
                            Log.d("S3File Upload", "Complete!");
                            mVideoData.videoUrl = fileUrl;
                            mVideoData.photoUrl = photoUrl;
                            AppSocialGlobal.getInstance().tmp_post.video = mVideoData;
                            String caption = mCaptionText.getText().toString();
                            AppSocialGlobal.getInstance().tmp_post.setCaption(caption);
                            AppSocialGlobal.getInstance().tmp_post.user = AppSocialGlobal.getInstance().me;

                            ApiHelper.sendPost(AppSocialGlobal.getInstance().tmp_post, false, "", new ApiHelper.ApiCallback() {
                                @Override
                                public void onSuccess(JSONObject response) {
                                    Log.d("APISendPosts success", response.toString());
                                    int postId = response.optInt("resultData");
                                    AppSocialGlobal.getInstance().tmp_post.postId = postId;
                                    AppSocialGlobal.getInstance().addPost();
                                    EventBus.getDefault().post(new MessageEvent(MessageEvent.MessageEventType.UPDATE_POST));
                                }

                                @Override
                                public void onFail(String errorMsg) {
                                    Log.e("APISendPosts fail", errorMsg);
                                }
                            });
                        }
                    });
                }
            });


        } catch (Exception e) {

        }
    }
}
