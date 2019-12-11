package com.teamcircle.circlesdk.fragment;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.PointF;
import android.media.MediaMetadataRetriever;
import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.teamcircle.circlesdk.R;
import com.teamcircle.circlesdk.activity.CameraActivity;
import com.teamcircle.circlesdk.activity.TrimVideoActivity;
import com.teamcircle.circlesdk.helper.AppSocialGlobal;
import com.teamcircle.circlesdk.model.MessageEvent;
import com.teamcircle.circlesdk.model.VideoData;
import com.teamcircle.circlesdk.view.PlayVideoLayout;
import com.teamcircle.circlesdk.helper.VideoGalleryHelper;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;

import java.util.ArrayList;

public class VideoGalleryFragment extends Fragment {
    private ArrayList<VideoData> mVideos;
    private Bitmap[] mVideoCovers;
    private RecyclerView mRecyclerView;
    private VideoGalleryAdapter mAdapter;
    private int mCurrent = 0;
    private FrameLayout mVideoLayout;
    private PlayVideoLayout mVideoView;
    private FrameLayout mToggleLayout;
    private ImageView mToggleImageView;
    private boolean mIsToggled;
    private Handler mHandler;
    private FrameLayout mNext;
    private VideoData mCurrentVideo;
    private Bitmap[] mCurrentVideoFrames;
    private PointF mTouchPoint;
    private boolean mIsGoingUp;
    private boolean mIsClick;
    private boolean mTouchable;
    private boolean mIsSquare = true;
    private FrameLayout mResizeLayout;
    private TextView mTimeWarning;
    private TextView mNextText;
    private boolean mEnableNext = true;

    private static final int REQUEST_READ = 110;
    private static final int CELL_NUMBER_IN_ROW = 4;

    @SuppressLint("ClickableViewAccessibility")
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        EventBus.getDefault().register(this);

        final View view = inflater.inflate(R.layout.fragment_video_gallery, container, false);

        mResizeLayout = view.findViewById(R.id.resize);
        mVideoLayout = view.findViewById(R.id.video_layout);
        mVideoView = view.findViewById(R.id.video_view);
        mVideoView.setLoop(true);
        mToggleLayout = view.findViewById(R.id.toggle_videos);
        mToggleImageView = view.findViewById(R.id.toggle_videos_image);
        mIsToggled = false;
        mRecyclerView = view.findViewById(R.id.video_gallery);
        mRecyclerView.setLayoutManager(new GridLayoutManager(getContext(), CELL_NUMBER_IN_ROW, RecyclerView.VERTICAL, false));
        mRecyclerView.getRecycledViewPool().setMaxRecycledViews(0, 8 * CELL_NUMBER_IN_ROW);

        mTimeWarning = view.findViewById(R.id.max_time_warning);
        mNextText = view.findViewById(R.id.next_text);

        mVideos = new ArrayList<>();
        mHandler = new Handler();
        mTouchPoint = new PointF();

        if (ContextCompat.checkSelfPermission(getContext(), Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            String permission = Manifest.permission.READ_EXTERNAL_STORAGE;
            ActivityCompat.requestPermissions(getActivity(), new String[]{permission}, REQUEST_READ);
        }
        if (AppSocialGlobal.getInstance().galleryVideos != null) {
            mVideos.addAll(AppSocialGlobal.getInstance().galleryVideos);
        } else {
            startVideoLoader();
        }
        if (mVideos.size() > 0) {
            setVideo(mVideos.get(0));
            mVideoCovers = new Bitmap[mVideos.size()];
        }

        final int size = AppSocialGlobal.getScreenWidth(getContext());

        FrameLayout.LayoutParams layoutParams3 = (FrameLayout.LayoutParams) mResizeLayout.getLayoutParams();
        layoutParams3.topMargin = size - AppSocialGlobal.dpToPx(getContext(), 50);
        mResizeLayout.setLayoutParams(layoutParams3);
        mResizeLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mCurrentVideo != null) {
                    mIsSquare = !mIsSquare;
                    setVideo(mCurrentVideo);
                }
            }
        });

        ViewGroup.LayoutParams layoutParams = mVideoLayout.getLayoutParams();
        layoutParams.height = size;
        mVideoLayout.setLayoutParams(layoutParams);
        mVideoLayout.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (mCurrentVideo == null || !mIsSquare) {
                    return false;
                }
                int action = event.getActionMasked();
                FrameLayout.LayoutParams layoutParams = (FrameLayout.LayoutParams) mVideoView.getLayoutParams();
                int width, height;
                if (mCurrentVideo.width > mCurrentVideo.height) {
                    height = size;
                    width = mCurrentVideo.width * size / mCurrentVideo.height;
                } else {
                    width = size;
                    height = mCurrentVideo.height * size / mCurrentVideo.width;
                }
                switch (action) {
                    case MotionEvent.ACTION_DOWN:
                        mTouchPoint.set(event.getX(), event.getY());
                        break;
                    case MotionEvent.ACTION_MOVE:
                        float dx = event.getX() - mTouchPoint.x;
                        float dy = event.getY() - mTouchPoint.y;
                        if (mCurrentVideo.width > mCurrentVideo.height) {
                            int marginLeft = (int) (layoutParams.leftMargin + dx);
                            if (marginLeft >= height - width && marginLeft <= 0) {
                                layoutParams.leftMargin = marginLeft;
                                mVideoView.setLayoutParams(layoutParams);
                            }
                        } else {
                            int marginTop = (int) (layoutParams.topMargin + dy);
                            if (marginTop >= width - height && marginTop <= 0) {
                                layoutParams.topMargin = marginTop;
                                mVideoView.setLayoutParams(layoutParams);
                            }
                        }
                        mTouchPoint.set(event.getX(), event.getY());
                        break;
                    case MotionEvent.ACTION_CANCEL:
                    case MotionEvent.ACTION_UP:
                        mCurrentVideo.startPercentX = -layoutParams.leftMargin / (float) width;
                        mCurrentVideo.startPercentY = -layoutParams.topMargin / (float) height;
                        mCurrentVideo.endPercentX = (size - layoutParams.leftMargin) / (float) width;
                        mCurrentVideo.endPercentY = (size - layoutParams.topMargin) / (float) height;
                        break;
                }
                return true;
            }
        });

        final int topMargin1 = size + AppSocialGlobal.dpToPx(getContext(), 80);
        final int topMargin2 = AppSocialGlobal.dpToPx(getContext(), 120);
        final FrameLayout.LayoutParams layoutParams1 = (FrameLayout.LayoutParams) mRecyclerView.getLayoutParams();
        layoutParams1.topMargin = topMargin1;
        mRecyclerView.setLayoutParams(layoutParams1);
        final FrameLayout.LayoutParams layoutParams2 = (FrameLayout.LayoutParams) mToggleLayout.getLayoutParams();
        layoutParams2.topMargin = topMargin1 - AppSocialGlobal.dpToPx(getContext(), 30);
        mToggleLayout.setLayoutParams(layoutParams2);
        mToggleLayout.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                int action = event.getActionMasked();
                switch (action) {
                    case MotionEvent.ACTION_DOWN:
                        if (mTouchable) {
                            mIsClick = true;
                            mTouchPoint.set(event.getRawX(), event.getRawY());
                            new Handler().postDelayed(new Runnable() {
                                @Override
                                public void run() {
                                    mIsClick = false;
                                }
                            }, 150);
                        }
                        break;
                    case MotionEvent.ACTION_MOVE:
                        float dy = event.getRawY() - mTouchPoint.y;
                        mIsGoingUp = dy < 0;
                        int topMargin = (int) (layoutParams1.topMargin + dy);
                        if (topMargin >= topMargin2 && topMargin <= topMargin1) {
                            layoutParams1.topMargin = topMargin;
                            mRecyclerView.setLayoutParams(layoutParams1);
                            layoutParams2.topMargin = topMargin - AppSocialGlobal.dpToPx(getContext(), 30);
                            mToggleLayout.setLayoutParams(layoutParams2);
                        }
                        mTouchPoint.set(event.getRawX(), event.getRawY());
                        break;
                    case MotionEvent.ACTION_UP:
                        if (mIsClick) {
                            mIsToggled = !mIsToggled;
                            if (mIsToggled) {
                                moveToTop();
                            } else {
                                moveToBottom();
                            }

                        } else {
                            if (mIsGoingUp) {
                                mIsToggled = true;
                                moveToTop();
                            } else {
                                mIsToggled = false;
                                moveToBottom();
                            }
                        }
                        break;
                }
                return true;
            }
        });

        ImageView backImage = view.findViewById(R.id.back_image);
        backImage.setImageResource(AppSocialGlobal.backResourceId);
        FrameLayout back = view.findViewById(R.id.back);
        back.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getActivity().finish();
            }
        });
        mNext = view.findViewById(R.id.next);
        mNext.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mEnableNext) {
                    mVideoView.pause();
                    AppSocialGlobal.getInstance().tmp_video = new VideoData(mCurrentVideo);
                    AppSocialGlobal.getInstance().tmp_frames = mCurrentVideoFrames;
                    startActivity(new Intent(getContext(), TrimVideoActivity.class));
                }
            }
        });

        mAdapter = new VideoGalleryAdapter();
        mRecyclerView.setAdapter(mAdapter);
        getVideoCovers();



        return view;
    }

    @Override
    public void onDestroy() {
        EventBus.getDefault().unregister(this);
        super.onDestroy();
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    public void startVideoLoader() {
        if (ContextCompat.checkSelfPermission(getContext(), Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
            VideoGalleryHelper.getInstance().startVideoLoader(getContext(), new VideoGalleryHelper.VideoGalleryLoaderCallback() {
                @Override
                public void videoGalleryLoaderDone(ArrayList<VideoData> videos) {
                    AppSocialGlobal.getInstance().galleryVideos = videos;
                    mVideos.addAll(videos);
                    if (mVideos.size() > 0) {
                        setVideo(mVideos.get(0));
                        mVideoCovers = new Bitmap[mVideos.size()];
                    }
                    mAdapter.notifyDataSetChanged();
                    getVideoCovers();
                }
            });
        }
    }

    private class VideoGalleryAdapter extends RecyclerView.Adapter<VideoViewHolder> {
        @NonNull
        @Override
        public VideoViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            LayoutInflater layoutInflater = LayoutInflater.from(parent.getContext());
            View view = layoutInflater.inflate(R.layout.video_gallery_item, parent, false);

            int width = AppSocialGlobal.getScreenWidth(getContext()) / CELL_NUMBER_IN_ROW;
            view.setLayoutParams(new ViewGroup.LayoutParams(width, width));
            return new VideoViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull final VideoViewHolder holder, final int position) {
            final int pos = position - 1;
            if (position == 0) {
                holder.mSelectedView.setVisibility(View.GONE);
                holder.mVideoTimeTextView.setVisibility(View.GONE);
                holder.mImageView.setVisibility(View.GONE);
                holder.mVideoImage.setVisibility(View.VISIBLE);
                holder.mVideoImage.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        startActivity(new Intent(getContext(), CameraActivity.class));
                    }
                });
            } else {
                holder.mVideoTimeTextView.setVisibility(View.VISIBLE);
                holder.mVideoImage.setVisibility(View.GONE);
                holder.mImageView.setVisibility(View.VISIBLE);

                if (mVideoCovers != null && mVideoCovers[pos] != null) {
                    holder.mImageView.setImageBitmap(mVideoCovers[pos]);
                }
                if (mCurrent == pos) {
                    holder.mSelectedView.setVisibility(View.VISIBLE);
                } else {
                    holder.mSelectedView.setVisibility(View.GONE);
                }
                int minute = mVideos.get(pos).duration / 1000 / 60;
                int second = mVideos.get(pos).duration / 1000 % 60;
                String text = String.format("%d:%d", minute, second);
                if (second < 10) {
                    text = String.format("%d:0%d", minute, second);
                }
                holder.mVideoTimeTextView.setText(text);
            }


            holder.mImageView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (mCurrent != pos) {
                        mCurrent = pos;
                        setVideo(mVideos.get(pos));
                        mAdapter.notifyDataSetChanged();
                    }
                }
            });
        }

        @Override
        public int getItemCount() {
            return mVideos.size() + 1;
        }
    }

    private class VideoViewHolder extends RecyclerView.ViewHolder {
        ImageView mImageView;
        View mSelectedView;
        TextView mVideoTimeTextView;
        String mVideoUrl;
        FrameLayout mVideoImage;

        public VideoViewHolder(View itemView) {
            super(itemView);
            mImageView = itemView.findViewById(R.id.image);
            mSelectedView = itemView.findViewById(R.id.selected);
            mVideoTimeTextView = itemView.findViewById(R.id.video_time);
            mVideoUrl = "";
            mVideoImage = itemView.findViewById(R.id.video);
        }
    }

    private void setVideo(VideoData videoData) {
        if (mVideoView == null)
            return;
        mResizeLayout.setVisibility(View.VISIBLE);
        if (videoData.width == videoData.height) {
            mResizeLayout.setVisibility(View.GONE);
        }
        int width, height;
        int size = AppSocialGlobal.getScreenWidth(getContext());
        FrameLayout.LayoutParams layoutParams = (FrameLayout.LayoutParams) mVideoView.getLayoutParams();
        if (mIsSquare) {
            if (videoData.width > videoData.height) {
                videoData.startPercentX = (videoData.width - videoData.height) / 2f / videoData.width;
                videoData.endPercentX = (videoData.width + videoData.height) / 2f / videoData.width;
                videoData.startPercentY = 0f;
                videoData.endPercentY = 1f;
                height = size;
                width = videoData.width * size / videoData.height;
                layoutParams.leftMargin = (height - width) / 2;
                layoutParams.topMargin = 0;
            } else {
                videoData.startPercentY = (videoData.height - videoData.width) / 2f / videoData.height;
                videoData.endPercentY = (videoData.height + videoData.width) / 2f / videoData.height;
                videoData.startPercentX = 0f;
                videoData.endPercentX = 1f;
                width = size;
                height = videoData.height * size / videoData.width;
                layoutParams.topMargin = (width - height) / 2;
                layoutParams.leftMargin = 0;
            }
        } else {
            float ratio = (float) videoData.width / videoData.height;
            if (ratio < 3f / 4) {
                width = size * 3 / 4;
                height = (int) (width / ratio);
                layoutParams.topMargin = (size - height) / 2;
                layoutParams.leftMargin = (size - width) / 2;
                videoData.startPercentY = (height - size) / 2f / height;
                videoData.endPercentY = (height + size) / 2f / height;
                videoData.startPercentX = 0f;
                videoData.endPercentX = 1f;
            } else if (ratio < 1) {
                height = size;
                width = (int) (height * ratio);
                layoutParams.topMargin = 0;
                layoutParams.leftMargin = (size - width) / 2;
                videoData.startPercentY = 0f;
                videoData.endPercentY = 1f;
                videoData.startPercentX = 0f;
                videoData.endPercentX = 1f;
            } else {
                width = size;
                height = (int) (width / ratio);
                layoutParams.topMargin = (size - height) / 2;
                layoutParams.leftMargin = 0;
                videoData.startPercentY = 0f;
                videoData.endPercentY = 1f;
                videoData.startPercentX = 0f;
                videoData.endPercentX = 1f;
            }
        }
        layoutParams.width = width;
        layoutParams.height = height;
        mVideoView.setLayoutParams(layoutParams);
        mCurrentVideo = videoData;
        mVideoView.pause();
        if (AppSocialGlobal.getInstance().isMuted) {
            mVideoView.mute();
        } else {
            mVideoView.unmute();
        }

        mVideoView.setVideo(videoData);
        mVideoView.startPlay();

        if (videoData.duration > 3 * 60 * 1000) {
            mNextText.setTextColor(getResources().getColor(R.color.grayColor));
            mTimeWarning.setVisibility(View.VISIBLE);
            mEnableNext = false;
        } else {
            mNextText.setTextColor(getResources().getColor(R.color.lightBlueColor));
            mTimeWarning.setVisibility(View.GONE);
            mEnableNext = true;
        }
    }

    private void getVideoCovers() {
        if (mVideos.size() > 0) {
            for (int i = 0; i < mVideos.size(); i++) {
                final VideoData videoData = mVideos.get(i);
                final MediaMetadataRetriever retriever = new MediaMetadataRetriever();
                retriever.setDataSource(videoData.videoUrl);
                final int index = i;
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        Bitmap bitmap = retriever.getFrameAtTime(videoData.start * 1000);
                        mVideoCovers[index] = bitmap;
                        Activity activity = getActivity();
                        if (activity != null) {
                            activity.runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    mAdapter.notifyDataSetChanged();
                                }
                            });
                        }
                    }
                }).start();
            }
        }
    }

    @Subscribe(sticky = true)
    public void onEvent(MessageEvent event) {
        switch (event.type) {
            case CHANGE_VOLUME:
                AppSocialGlobal.getInstance().isMuted = false;
                mVideoView.unmute();
                break;
        }
    }

    private void moveToBottom() {
        mTouchable = false;
        final FrameLayout.LayoutParams layoutParams1 = (FrameLayout.LayoutParams) mRecyclerView.getLayoutParams();
        final FrameLayout.LayoutParams layoutParams2 = (FrameLayout.LayoutParams) mToggleLayout.getLayoutParams();
        final int size = AppSocialGlobal.getScreenWidth(getContext());
        final int topMargin1 = size + AppSocialGlobal.dpToPx(getContext(), 80);
        final int topMargin = layoutParams1.topMargin;
        for (int i = 1; i <= 50; i++) {
            final int count = i;
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    mToggleImageView.setRotation((float) 0);
                    int margin = topMargin + (topMargin1 - topMargin) * count / 50;
                    layoutParams1.topMargin = margin;
                    mRecyclerView.setLayoutParams(layoutParams1);
                    layoutParams2.topMargin = margin - AppSocialGlobal.dpToPx(getContext(), 30);
                    mToggleLayout.setLayoutParams(layoutParams2);
                    if (count == 50) {
                        mTouchable = true;
                    }
                }
            }, i * 5);
        }
    }

    private void moveToTop() {
        mTouchable = false;
        final FrameLayout.LayoutParams layoutParams1 = (FrameLayout.LayoutParams) mRecyclerView.getLayoutParams();
        final FrameLayout.LayoutParams layoutParams2 = (FrameLayout.LayoutParams) mToggleLayout.getLayoutParams();
        final int topMargin2 = AppSocialGlobal.dpToPx(getContext(), 120);
        final int topMargin = layoutParams1.topMargin;
        for (int i = 1; i <= 50; i++) {
            final int count = i;
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    mToggleImageView.setRotation((float) 180);
                    int margin = topMargin + (topMargin2 - topMargin) * count / 50;
                    layoutParams1.topMargin = margin;
                    mRecyclerView.setLayoutParams(layoutParams1);
                    layoutParams2.topMargin = margin - AppSocialGlobal.dpToPx(getContext(), 30);
                    mToggleLayout.setLayoutParams(layoutParams2);
                    if (count == 50) {
                        mTouchable = true;
                    }
                }
            }, i * 5);
        }
    }
}

