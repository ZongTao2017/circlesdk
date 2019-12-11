package com.teamcircle.circlesdk.view;

import android.content.Context;
import android.os.Handler;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import com.teamcircle.circlesdk.R;
import com.teamcircle.circlesdk.helper.AppSocialGlobal;
import com.teamcircle.circlesdk.model.VideoData;
import com.teamcircle.circlesdk.text.TextViewRegular;
import com.teamcircle.circlesdk.video.TextureVideoView;

public class ListVideoView extends FrameLayout {
    FrameLayout mVideoLayout;
    TextureVideoView mVideoPlayerView;
    ImageView mPreviewImage;
    ImageView mPlayImageView;
    ImageView mHeartImageView;
    Handler mHandler;
    boolean noSound, isMuted;
    FrameLayout mCenterLayout;
    TextView mCenterText;
    String mVideoPath = null;

    public ListVideoView(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
        init(context);
    }

    public ListVideoView(Context context) {
        super(context);
        init(context);
    }

    private void init(Context context) {
        mHandler = new Handler();
        noSound = false;
        isMuted = false;

        mVideoLayout = new FrameLayout(context);
        addView(mVideoLayout);

        mPreviewImage = new ImageView(getContext());
        mVideoLayout.addView(mPreviewImage);

        mVideoPlayerView = new TextureVideoView(getContext());
        mVideoLayout.addView(mVideoPlayerView);

        mPlayImageView = new ImageView(context);
        int size = AppSocialGlobal.dpToPx(context, 40);
        LayoutParams layoutParams = new LayoutParams(size, size);
        layoutParams.gravity = Gravity.CENTER;
        mPlayImageView.setLayoutParams(layoutParams);
        mPlayImageView.setColorFilter(context.getResources().getColor(R.color.whiteColor));
        addView(mPlayImageView);

        mCenterLayout = new FrameLayout(getContext());
        mCenterLayout.setBackgroundColor(getContext().getResources().getColor(R.color.transparentBlack));
        LayoutParams layoutParams1 = new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, AppSocialGlobal.dpToPx(getContext(), 40));
        layoutParams1.gravity = Gravity.CENTER;
        mCenterLayout.setLayoutParams(layoutParams1);
        mCenterLayout.setAlpha(0f);
        addView(mCenterLayout);
        mCenterText = new TextViewRegular(getContext());
        LayoutParams layoutParams3 = new LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        layoutParams3.gravity = Gravity.CENTER;
        mCenterText.setLayoutParams(layoutParams3);
        mCenterText.setTextColor(getContext().getResources().getColor(R.color.whiteColor));
        mCenterText.setTextSize(16);
        mCenterText.setGravity(Gravity.CENTER);
        mCenterLayout.addView(mCenterText);

        mHeartImageView = new ImageView(getContext());
        mHeartImageView.setImageResource(R.drawable.post_like_sel);
        mHeartImageView.setVisibility(GONE);
        mVideoLayout.addView(mHeartImageView);
    }

    public void setIfNoSound(boolean noSound) {
        this.isMuted = true;
        this.noSound = noSound;
    }

    public void setVideo(VideoData videoData) {
        int videoWidth = (int) (videoData.width * (videoData.endPercentX - videoData.startPercentX));
        int videoHeight = (int) (videoData.height * (videoData.endPercentY - videoData.startPercentY));
        int screenSize = AppSocialGlobal.getScreenWidth(getContext());
        mVideoLayout.setLayoutParams(new LayoutParams(screenSize, screenSize * videoHeight / videoWidth));
        mPreviewImage.setLayoutParams(new LayoutParams(screenSize, screenSize * videoHeight / videoWidth));
        AppSocialGlobal.loadImage(videoData.photoUrl, mPreviewImage);

        mVideoPath = videoData.videoUrl;
        LayoutParams layoutParams = new LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        int width, height;
        if (videoWidth == videoHeight) {
            if (videoData.width > videoData.height) {
                width = screenSize * videoData.width / videoData.height;
                height = screenSize;
            } else {
                width = screenSize;
                height = screenSize * videoData.height / videoData.width;
            }
            layoutParams.topMargin = (int) (height * -videoData.startPercentY);
            layoutParams.leftMargin = (int) (width * -videoData.startPercentX);
        } else {
            width = screenSize;
            height = screenSize * videoData.height / videoData.width;
            if (videoWidth < videoHeight) {
                layoutParams.topMargin = (int) (height * -videoData.startPercentY);
            }
        }

        layoutParams.width = width;
        layoutParams.height = height;
        mVideoPlayerView.setLayoutParams(layoutParams);
        if (!videoData.videoUrl.startsWith("http")) {
            mVideoPlayerView.setVideoPath(videoData.videoUrl);
            mVideoPlayerView.setVisibility(INVISIBLE);
        }
        mVideoPlayerView.mute();

        LayoutParams layoutParams1;
        if (height > AppSocialGlobal.dpToPx(getContext(), 100)) {
            layoutParams1 = new LayoutParams(AppSocialGlobal.dpToPx(getContext(), 70), AppSocialGlobal.dpToPx(getContext(), 70));
        } else {
            layoutParams1 = new LayoutParams(0, 0);
        }
        layoutParams1.gravity = Gravity.CENTER;
        mHeartImageView.setLayoutParams(layoutParams1);
    }

    public void setVideoPathNull() {
        mVideoPath = null;
    }

    public String getVideoPath() {
        return mVideoPath;
    }

    public void startVideo() {
        mVideoPlayerView.setVisibility(VISIBLE);
        mVideoPlayerView.start();
    }

    public void stopVideo() {
        mVideoPlayerView.stop();
        mVideoPlayerView.setVisibility(INVISIBLE);
    }


    public void playAudio() {
        isMuted = !isMuted;
        mHandler.removeCallbacksAndMessages(null);
        if (noSound) {
            mCenterLayout.setAlpha(1f);
            mCenterText.setText("This video has no sound");
            for (int i = 1; i <= 50; i++) {
                final float alpha = (50 - i) / 50f;
                mHandler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        mCenterLayout.setAlpha(alpha);
                    }
                }, 500 + i * 10);
            }
        } else {
            if (!isMuted) {
                if (mVideoPath != null) {
                    mVideoPlayerView.unMute();
                }
                mPlayImageView.setImageResource(R.drawable.ic_sound);
            } else {
                if (mVideoPath != null) {
                    mVideoPlayerView.mute();
                }
                mPlayImageView.setImageResource(R.drawable.ic_no_sound);
            }

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
    }

    public void animateHeart() {
        mHeartImageView.setVisibility(View.VISIBLE);
        for (int i = 1; i <= 20; i++) {
            final int count = i;
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    int size = 70 + count;
                    LayoutParams layoutParams = (LayoutParams) mHeartImageView.getLayoutParams();
                    layoutParams.width = AppSocialGlobal.dpToPx(getContext(), size);
                    layoutParams.height = AppSocialGlobal.dpToPx(getContext(), size);
                    mHeartImageView.setLayoutParams(layoutParams);
                    if (size == 90) {
                        for (int j = 1; j <= 20; j++) {
                            final int count = j;
                            mHandler.postDelayed(new Runnable() {
                                @Override
                                public void run() {
                                    int size = 90 - count;
                                    LayoutParams layoutParams = (LayoutParams) mHeartImageView.getLayoutParams();
                                    layoutParams.width = AppSocialGlobal.dpToPx(getContext(), size);
                                    layoutParams.height = AppSocialGlobal.dpToPx(getContext(), size);
                                    mHeartImageView.setLayoutParams(layoutParams);
                                    if (size == 70) {
                                        mHeartImageView.setVisibility(View.INVISIBLE);
                                    }
                                }
                            }, j * 15);
                        }
                    }
                }
            }, i * 15);
        }
    }
}
