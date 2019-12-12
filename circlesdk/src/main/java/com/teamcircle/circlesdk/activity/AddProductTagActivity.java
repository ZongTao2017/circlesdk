package com.teamcircle.circlesdk.activity;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;

import com.teamcircle.circlesdk.R;
import com.teamcircle.circlesdk.helper.AppSocialGlobal;
import com.teamcircle.circlesdk.model.MessageEvent;
import com.teamcircle.circlesdk.model.TagData;
import com.teamcircle.circlesdk.model.VideoData;
import com.teamcircle.circlesdk.view.PlayVideoLayout;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;

import java.util.Timer;
import java.util.TimerTask;

public class AddProductTagActivity extends Activity {
    private PlayVideoLayout mVideoView;
    private LinearLayout mTagsLayout;
    private VideoData mVideoData;
    private Timer mTimer;
    private PlayVideoTimerTask mTimerTask;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_product_tag);
        EventBus.getDefault().register(this);

        mVideoData = AppSocialGlobal.getInstance().tmp_video;

        ImageView backImage = findViewById(R.id.back_image);
        backImage.setImageResource(AppSocialGlobal.backResourceId);
        FrameLayout back = findViewById(R.id.back);
        back.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        FrameLayout done = findViewById(R.id.done);
        done.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AppSocialGlobal.getInstance().tmp_post.video = mVideoData;
                EventBus.getDefault().post(new MessageEvent(MessageEvent.MessageEventType.DONE_TAG_PRODUCT));
                finish();
            }
        });

        int size = AppSocialGlobal.getScreenWidth(this);
        mVideoView = findViewById(R.id.video_view);

        FrameLayout videoLayout = findViewById(R.id.video_view_layout);
        LinearLayout.LayoutParams layoutParams2 = (LinearLayout.LayoutParams) videoLayout.getLayoutParams();
        layoutParams2.height = size;
        videoLayout.setLayoutParams(layoutParams2);

        FrameLayout.LayoutParams layoutParams = (FrameLayout.LayoutParams) mVideoView.getLayoutParams();
        int w = (int) (mVideoData.width * (mVideoData.endPercentX - mVideoData.startPercentX));
        int h = (int) (mVideoData.height * (mVideoData.endPercentY - mVideoData.startPercentY));
        int width, height;
        if (w == h) {
            if (mVideoData.width > mVideoData.height) {
                height = size;
                width = mVideoData.width * size / mVideoData.height;
                layoutParams.leftMargin = (int) (-width * mVideoData.startPercentX);
            } else {
                width = size;
                height = mVideoData.height * size / mVideoData.width;
                layoutParams.topMargin = (int) (-height * mVideoData.startPercentY);
            }
        } else if (w > h) {
            width = size;
            height = size * h / w;
            layoutParams.topMargin = (size - height) / 2;
        } else {
            if (mVideoData.startPercentY == 0) {
                height = size;
                width = height * w / h;
                layoutParams.leftMargin = (size - width) / 2;
            } else {
                width = size * 3 /4;
                height = width * mVideoData.height / mVideoData.width;
                layoutParams.leftMargin = (size - width) / 2;
                layoutParams.topMargin = (int) (-height * mVideoData.startPercentY);
            }
        }
        layoutParams.width = width;
        layoutParams.height = height;
        mVideoView.setLayoutParams(layoutParams);

        if (AppSocialGlobal.getInstance().isMuted) {
            mVideoView.mute();
        } else {
            mVideoView.unmute();
        }
//        mVideoView.setMute(mVideoData.isMuted, false);
        mVideoView.setVideo(mVideoData);
        mTagsLayout = findViewById(R.id.tags_layout);
        FrameLayout addTag = findViewById(R.id.add_tag);
        addTag.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AppSocialGlobal.getInstance().tmp_tag = new TagData();
                startActivity(new Intent(AddProductTagActivity.this, SelectProductActivity.class));
            }
        });

        for (TagData tagData : mVideoData.tags) {
            AppSocialGlobal.addTagView(AddProductTagActivity.this, mTagsLayout, tagData, 0, true);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        playVideo();
    }

    @Override
    protected void onPause() {
        if (mTimer != null) {
            mTimer.cancel();
            mTimer = null;
        }
        if (mTimerTask != null) {
            mTimerTask.cancel();
            mTimerTask = null;
        }
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        EventBus.getDefault().unregister(this);
        if (mTimer != null) {
            mTimer.cancel();
            mTimer = null;
        }
        if (mTimerTask != null) {
            mTimerTask.cancel();
            mTimerTask = null;
        }
        super.onDestroy();
    }

    @Subscribe(sticky = true)
    public void onEvent(MessageEvent event) {
        TagData tagData = AppSocialGlobal.getInstance().tmp_tag;
        switch (event.type) {
            case ADD_TAG:
                if (mVideoData.tags.contains(tagData)) {
                    new AlertDialog.Builder(AddProductTagActivity.this)
                            .setCancelable(true)
                            .setMessage("The product has been tagged.")
                            .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    dialog.dismiss();
                                }
                            })
                            .create()
                            .show();
                } else {
                    mVideoData.tags.add(tagData);
                    AppSocialGlobal.addTagView(AddProductTagActivity.this, mTagsLayout, tagData, 0, true);
                }
                break;
            case DELETE_TAG:
                mVideoData.tags.remove(tagData);
                break;

        }
    }

    private class PlayVideoTimerTask extends TimerTask {
        int start;

        public PlayVideoTimerTask(int start) {
            super();
            this.start = start;
        }

        @Override
        public void run() {
            mVideoView.pause();
            mVideoView.seekTo(start);
            mVideoView.start();
        }
    }

    private void playVideo() {
        int start = mVideoData.start;
        int duration = mVideoData.end - start;

        if (mTimer != null) {
            mTimer.cancel();
            mTimer = null;
        }
        if (mTimerTask != null) {
            mTimerTask.cancel();
            mTimerTask = null;
        }
        mTimer = new Timer();
        mTimerTask = new PlayVideoTimerTask(start);
        mTimer.schedule(mTimerTask, 0, duration);
    }
}
