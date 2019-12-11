package com.teamcircle.circlesdk.activity;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;

import com.teamcircle.circlesdk.R;
import com.teamcircle.circlesdk.helper.AppSocialGlobal;
import com.teamcircle.circlesdk.model.MessageEvent;
import com.teamcircle.circlesdk.model.PostData;
import com.teamcircle.circlesdk.model.VideoData;
import com.teamcircle.circlesdk.view.PlayVideoLayout;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;

import java.util.Timer;
import java.util.TimerTask;

public class TrimVideoActivity extends Activity {
    private VideoData mVideoData;
    private FrameLayout mVideoLayout;
    private PlayVideoLayout mVideoView;
    private Timer mTimer;
    private PlayVideoTimerTask mTimerTask;
    private ImageView mSoundImage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_trim_video);
        EventBus.getDefault().register(this);

        mVideoData = AppSocialGlobal.getInstance().tmp_video;
        if (mVideoData.end - mVideoData.start > 30000) {
            mVideoData.end = mVideoData.start + 30000;
        }

        ImageView backImage = findViewById(R.id.back_image);
        backImage.setImageResource(AppSocialGlobal.backResourceId);
        FrameLayout back = findViewById(R.id.back);
        back.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        FrameLayout next = findViewById(R.id.next);
        next.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                PostData postData = new PostData();
                postData.video = mVideoData;
                AppSocialGlobal.getInstance().tmp_post = postData;
                startActivity(new Intent(TrimVideoActivity.this, ReadyToPostActivity.class));
            }
        });

        FrameLayout sound = findViewById(R.id.sound);
        mSoundImage = findViewById(R.id.sound_image);
        sound.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mVideoData.isMuted = !mVideoData.isMuted;
                if (mVideoData.isMuted) {
                    mSoundImage.setImageResource(R.drawable.ic_no_sound);
                    mVideoView.mute();
                } else {
                    mSoundImage.setImageResource(R.drawable.ic_sound);
                    mVideoView.unmute();
                }
                mVideoView.showBottomText();
            }
        });

        int size = AppSocialGlobal.getScreenWidth(this);

        mVideoLayout = findViewById(R.id.video_layout);
        mVideoLayout.setLayoutParams(new LinearLayout.LayoutParams(size, size));

        mVideoView = findViewById(R.id.video_view);

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
        mVideoView.setVideo(mVideoData);
    }

    @Override
    protected void onResume() {
        super.onResume();
        playVideo();
    }

    @Override
    protected void onPause() {
        super.onPause();
        stopVideo();
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
        switch (event.type) {
            case TRIM_VIDEO:
                playVideo();
                break;
            case DONE_SEND_POST:
                finish();
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
        int start = AppSocialGlobal.getInstance().tmp_video.start;
        int duration = AppSocialGlobal.getInstance().tmp_video.end - start;

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

    private void stopVideo() {
        if (mTimer != null) {
            mTimer.cancel();
            mTimer = null;
        }
        if (mTimerTask != null) {
            mTimerTask.cancel();
            mTimerTask = null;
        }
        mVideoView.pause();
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN || keyCode == KeyEvent.KEYCODE_VOLUME_UP) {
            AppSocialGlobal.getInstance().isMuted = false;
            mVideoView.unmute();
            mSoundImage.setImageResource(R.drawable.ic_sound);
            mVideoData.isMuted = false;
            EventBus.getDefault().post(new MessageEvent(MessageEvent.MessageEventType.CHANGE_VOLUME));
        }
        return super.onKeyDown(keyCode, event);
    }
}
