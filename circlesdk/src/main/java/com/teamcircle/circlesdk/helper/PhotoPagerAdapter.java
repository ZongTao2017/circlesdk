package com.teamcircle.circlesdk.helper;

import android.content.Context;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.viewpager.widget.PagerAdapter;

import com.teamcircle.circlesdk.R;
import com.teamcircle.circlesdk.model.PhotoData;
import com.teamcircle.circlesdk.model.PostData;
import com.teamcircle.circlesdk.model.TagData;
import com.teamcircle.circlesdk.view.PostImageView;
import com.teamcircle.circlesdk.view.TagNumberView;

import java.util.ArrayList;
import java.util.HashMap;

public class PhotoPagerAdapter extends PagerAdapter {
    private Context mContext;
    private PostData mPostData;
    private ImageView mLikeImage;
    private TextView mLikeNumText;
    private HashMap<Integer, ArrayList<TagNumberView>> mTagsMap;
    private Handler mHandler;
    private boolean mIsShowing;
    private String mEventName;

    public PhotoPagerAdapter(Context context, PostData postData, ImageView likeImage, TextView likeNumText, final String eventName) {
        mContext = context;
        mPostData = postData;
        mLikeImage = likeImage;
        mLikeNumText = likeNumText;
        mTagsMap = new HashMap<>();
        mHandler = new Handler();
        mIsShowing = false;
        mEventName = eventName;
    }

    public void showTags(boolean isShowing) {
        for (int position : mTagsMap.keySet()) {
            ArrayList<TagNumberView> tags = mTagsMap.get(position);
            for (TagNumberView tagNumberView : tags) {
                if (isShowing) {
                    tagNumberView.setVisibility(View.VISIBLE);
                    tagNumberView.animateCircle();
                } else {
                    tagNumberView.reset(false);
                    tagNumberView.setVisibility(View.GONE);
                }
            }
        }
        mIsShowing = isShowing;
    }

    public void setFocus(int positionInViewPager, int positionInTags) {
        ArrayList<TagNumberView> tags = mTagsMap.get(positionInViewPager);
        for (int i = 0; i < tags.size(); i++) {
            TagNumberView tagNumberView = tags.get(i);
            tagNumberView.setFocus(i == positionInTags);
        }
    }

    public boolean isShowingTags() {
        return mIsShowing;
    }

    @NonNull
    @Override
    public Object instantiateItem(@NonNull ViewGroup container, final int position) {
        LayoutInflater inflater = LayoutInflater.from(mContext);
        int screenWidth = AppSocialGlobal.getScreenWidth(mContext);
        int height = 0;
        for (PhotoData photoData : mPostData.photos) {
            int h = screenWidth * photoData.height / photoData.width;
            height = Math.max(height, h);
        }
        FrameLayout layout = (FrameLayout) inflater.inflate(R.layout.tag_product_photo, container, false);
        layout.setLayoutParams(new ViewGroup.LayoutParams(screenWidth, height));
        final PostImageView imageView = layout.findViewById(R.id.image_view);
        imageView.setIsFullWidth(true);
        imageView.setLayoutParams(new FrameLayout.LayoutParams(screenWidth, height));
        final FrameLayout tagsLayout = layout.findViewById(R.id.tags_framelayout);
        tagsLayout.setLayoutParams(new FrameLayout.LayoutParams(screenWidth, height));
        final ImageView heartImage = layout.findViewById(R.id.heart_image);
        final PhotoData photoData = mPostData.photos.get(position);
        String photoUrl = photoData.photoUrl;
        AppSocialGlobal.loadImage(photoUrl, imageView);
        ArrayList<TagNumberView> tags = new ArrayList<>();
        if (photoData.tags.size() == 1) {
            TagData tagData = photoData.tags.get(0);
            TagNumberView tagNumberView = new TagNumberView(mContext, tagData, 0, screenWidth, height);
            tagsLayout.addView(tagNumberView);
            tags.add(tagNumberView);
            if (mIsShowing)
                tagNumberView.setVisibility(View.VISIBLE);
            else
                tagNumberView.setVisibility(View.GONE);
            mTagsMap.put(position, tags);
        } else if (photoData.tags.size() > 1) {
            for (int i = 0; i < photoData.tags.size(); i++) {
                TagData tagData = photoData.tags.get(i);
                TagNumberView tagNumberView = new TagNumberView(mContext, tagData, i + 1, screenWidth, height);
                tagsLayout.addView(tagNumberView);
                tags.add(tagNumberView);
                if (mIsShowing) {
                    tagNumberView.setVisibility(View.VISIBLE);
                    tagNumberView.reset(true);
                }
                else
                    tagNumberView.setVisibility(View.GONE);
            }
            mTagsMap.put(position, tags);
        }

        imageView.setImageCallback(new PostImageView.ImageCallback() {
            @Override
            public void onClick(float percentX, float percentY) {

            }

            @Override
            public void onDoubleClick() {
                if (!AppSocialGlobal.getInstance().checkIfNeedSignIn(mContext)) {
                    if (!mPostData.isLiked) {
                        mPostData.isLiked = true;
                        mPostData.likeNumber++;
                        AppSocialGlobal.setLikeNumber(mPostData, mLikeNumText);
                        mLikeImage.setImageResource(R.drawable.post_like_sel);
                        heartImage.setVisibility(View.VISIBLE);
                        for (int i = 1; i <= 20; i++) {
                            final int count = i;
                            mHandler.postDelayed(new Runnable() {
                                @Override
                                public void run() {
                                    int size = 70 + count;
                                    FrameLayout.LayoutParams layoutParams = (FrameLayout.LayoutParams) heartImage.getLayoutParams();
                                    layoutParams.width = AppSocialGlobal.dpToPx(mContext, size);
                                    layoutParams.height = AppSocialGlobal.dpToPx(mContext, size);
                                    heartImage.setLayoutParams(layoutParams);
                                    if (size == 90) {
                                        for (int j = 1; j <= 20; j++) {
                                            final int count = j;
                                            mHandler.postDelayed(new Runnable() {
                                                @Override
                                                public void run() {
                                                    int size = 90 - count;
                                                    FrameLayout.LayoutParams layoutParams = (FrameLayout.LayoutParams) heartImage.getLayoutParams();
                                                    layoutParams.width = AppSocialGlobal.dpToPx(mContext, size);
                                                    layoutParams.height = AppSocialGlobal.dpToPx(mContext, size);
                                                    heartImage.setLayoutParams(layoutParams);
                                                    if (size == 70) {
                                                        heartImage.setVisibility(View.INVISIBLE);
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
            }

            @Override
            public void onMeasureFinish(float width, float height) {

            }
        });
        container.addView(layout);
        return layout;
    }

    @Override
    public void destroyItem(@NonNull ViewGroup container, int position, @NonNull Object object) {
        container.removeView((View) object);
    }

    @Override
    public boolean isViewFromObject(@NonNull View view, @NonNull Object object) {
        return view == object;
    }

    @Override
    public int getCount() {
        return mPostData.photos.size();
    }

    public void reset() {
        for (int position : mTagsMap.keySet()) {
            ArrayList<TagNumberView> tags = mTagsMap.get(position);
            for (TagNumberView tagNumberView : tags) {
                tagNumberView.reset(true);
                tagNumberView.setFocus(false);
            }
        }
    }
}
