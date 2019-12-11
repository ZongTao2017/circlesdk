package com.teamcircle.circlesdk.view;

import android.content.Context;
import android.content.res.TypedArray;
import android.media.MediaPlayer;
import android.os.Handler;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.VideoView;

import com.teamcircle.circlesdk.R;
import com.teamcircle.circlesdk.helper.AppSocialGlobal;
import com.teamcircle.circlesdk.model.VideoData;
import com.teamcircle.circlesdk.text.TextViewRegular;

public class PlayVideoLayout extends FrameLayout {
    FrameLayout mVideoLayout;
    PlayVideoView mVideoView;
    ImageView mPlayImageView;
    Handler mHandler;
    boolean isMuted;
    boolean isLoop;
    boolean isPrepared;
    boolean isPlaying;
    boolean noSound;
    FrameLayout mBottomLayout;
    TextView mBottomText;
    int startMillisec = 1;


    public PlayVideoLayout(Context context, AttributeSet attrs) {
        super(context, attrs);

        mVideoView = new PlayVideoView(getContext());

        mVideoLayout = new FrameLayout(context);
        mVideoLayout.setLayoutParams(new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        addView(mVideoLayout);

        mPlayImageView = new ImageView(context);
        int size = AppSocialGlobal.dpToPx(context, 40);
        LayoutParams layoutParams = new LayoutParams(size, size);
        layoutParams.gravity = Gravity.CENTER;
        mPlayImageView.setLayoutParams(layoutParams);
        mPlayImageView.setColorFilter(context.getResources().getColor(R.color.whiteColor));
        addView(mPlayImageView);

        mHandler = new Handler();
        isMuted = true;
        isLoop = false;
        isPrepared = false;
        isPlaying = false;
        noSound = false;
        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.PlayVideoLayout);
        boolean clickable = a.getBoolean(R.styleable.PlayVideoLayout_ifClickable, true);
        if (clickable) {
            setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (noSound) {
                        showBottomText();
                    } else {
                        isMuted = !isMuted;
                        playAudio();
                    }
                }
            });
        }

        mBottomLayout = new FrameLayout(getContext());
        mBottomLayout.setBackgroundColor(getContext().getResources().getColor(R.color.transparentBlack));
        LayoutParams layoutParams1 = new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, AppSocialGlobal.dpToPx(getContext(), 40));
        layoutParams1.gravity = Gravity.BOTTOM;
        mBottomLayout.setLayoutParams(layoutParams1);
        mBottomLayout.setAlpha(0f);
        addView(mBottomLayout);
        mBottomText = new TextViewRegular(getContext());
        LayoutParams layoutParams3 = new LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        layoutParams3.gravity = Gravity.CENTER;
        mBottomText.setLayoutParams(layoutParams3);
        mBottomText.setTextColor(getContext().getResources().getColor(R.color.whiteColor));
        mBottomText.setTextSize(16);
        mBottomText.setGravity(Gravity.CENTER);
        mBottomLayout.addView(mBottomText);
    }

    public void start() {
        mVideoView.start();
    }

    public void seekTo(int msec) {
        mVideoView.seekTo(msec);
        startMillisec = msec;
    }

    public void pause() {
        mVideoView.pause();
    }

    public void setVideo(VideoData videoData) {
        mVideoLayout.removeAllViews();
        LayoutParams layoutParams = new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        mVideoView.setLayoutParams(layoutParams);
        mVideoLayout.addView(mVideoView);
        if (!videoData.videoUrl.startsWith("http")) {
            mVideoView.setVideoPath(videoData.videoUrl);
        }
    }

    public void startPlay() {
        if (!isPlaying) {
            isPlaying = true;
            if (isPrepared) {
                mVideoView.start();
            }
        }
    }

    public void stopPlay() {
        if (isPlaying) {
            isPlaying = false;
            if (isPrepared) {
                mVideoView.pause();
            }
        }
    }

    public void mute() {
        isMuted = true;
        mVideoView.setVolume(0);
    }

    public void unmute() {
        isMuted = false;
        mVideoView.setVolume(100);
    }

    public void setLoop(boolean isLoop) {
        this.isLoop = isLoop;
    }

    public void setMute(boolean isMuted, boolean muteWhenStart) {
        this.isMuted = isMuted;
        if (muteWhenStart) {
            this.isMuted = true;
        }
        noSound = isMuted;
    }

    public void showBottomText() {
        mHandler.removeCallbacksAndMessages(null);
        mBottomLayout.setAlpha(1f);
        if (noSound) {
            mBottomText.setText("This video has no sound");
        } else {
            if (isMuted) {
                mBottomText.setText("Sound off");
            } else {
                mBottomText.setText("Sound on");
            }

        }
        for (int i = 1; i <= 50; i++) {
            final float alpha = (50 - i) / 50f;
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    mBottomLayout.setAlpha(alpha);
                }
            }, 500 + i * 10);
        }
    }

    private void playAudio() {
        if (!isMuted) {
            mVideoView.setVolume(100);
            mPlayImageView.setImageResource(R.drawable.ic_sound);
        } else {
            mVideoView.setVolume(0);
            mPlayImageView.setImageResource(R.drawable.ic_no_sound);
        }
        mHandler.removeCallbacksAndMessages(null);
        mPlayImageView.setAlpha(1f);
        for (int i = 1; i <= 50; i++) {
            final float alpha = (50 - i) / 50f;
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    mPlayImageView.setAlpha(alpha);
                }
            }, 500 + i * 20);
        }
    }

    public class PlayVideoView extends VideoView implements MediaPlayer.OnPreparedListener {
        MediaPlayer mediaPlayer;

        public PlayVideoView(Context context) {
            super(context);
            this.setOnPreparedListener(this);
        }

        @Override
        public void onPrepared(MediaPlayer mp) {
            mediaPlayer = mp;
            if (!isMuted) {
                setVolume(100);
            } else {
                setVolume(0);
            }
            mp.setLooping(isLoop);
            isPrepared = true;
            seekTo(startMillisec);
            if (isPlaying) {
                start();
            }
        }

        private void setVolume(int amount) {
            final int max = 100;
            final double numerator = max - amount > 0 ? Math.log(max - amount) : 0;
            final float volume = (float) (1 - (numerator / Math.log(max)));
            if (mediaPlayer != null) {
                mediaPlayer.setVolume(volume, volume);
            }
        }
    }
}
