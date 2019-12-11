package com.teamcircle.circlesdk.activity;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.util.Size;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.viewpager.widget.PagerAdapter;
import androidx.viewpager.widget.ViewPager;

import com.teamcircle.circlesdk.R;
import com.teamcircle.circlesdk.helper.AppSocialGlobal;
import com.teamcircle.circlesdk.model.MessageEvent;
import com.teamcircle.circlesdk.model.PhotoData;
import com.teamcircle.circlesdk.model.TagData;
import com.teamcircle.circlesdk.view.PostImageView;
import com.teamcircle.circlesdk.view.TagView;
import com.teamcircle.circlesdk.view.ViewPagerIndicator;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;

import java.util.ArrayList;
import java.util.HashMap;

public class TagProductActivity extends Activity {
    private ArrayList<PhotoData> mPhotos;
    private ViewPager mViewPager;
    private PhotoPagerAdapter mAdapter;
    private HashMap<Integer, FrameLayout> mTagLayoutMap;
    private HashMap<Integer, Size> mSizeMap;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tag_product);
        EventBus.getDefault().register(this);
        HashMap<String, String> map = new HashMap<>();
        mPhotos = new ArrayList<>();
        for (PhotoData photoData : AppSocialGlobal.getInstance().tmp_post.photos) {
            mPhotos.add(new PhotoData(photoData));
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

        FrameLayout done = findViewById(R.id.done);
        done.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AppSocialGlobal.getInstance().tmp_post.photos = mPhotos;
                EventBus.getDefault().post(new MessageEvent(MessageEvent.MessageEventType.DONE_TAG_PRODUCT));
                finish();
            }
        });

        mViewPager = findViewById(R.id.view_pager);
        final int size = AppSocialGlobal.getScreenWidth(TagProductActivity.this);
        LinearLayout.LayoutParams layoutParams = (LinearLayout.LayoutParams) mViewPager.getLayoutParams();
        layoutParams.width = size;
        layoutParams.height = size;
        mViewPager.setLayoutParams(layoutParams);

        final ViewPagerIndicator viewPagerIndicator = findViewById(R.id.view_pager_indicator);
        if (mPhotos.size() == 1) {
            viewPagerIndicator.setVisibility(View.GONE);
        }
        viewPagerIndicator.setDotsCount(mPhotos.size());
        mViewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int i, float v, int i1) {

            }

            @Override
            public void onPageSelected(int i) {
                viewPagerIndicator.setCurrent(i);
            }

            @Override
            public void onPageScrollStateChanged(int i) {

            }
        });

        mTagLayoutMap = new HashMap<>();
        mSizeMap = new HashMap<>();

        mAdapter = new PhotoPagerAdapter();
        mViewPager.setAdapter(mAdapter);
    }

    @Subscribe(sticky = true)
    public void onEvent(MessageEvent event) {
        switch (event.type) {
            case ADD_TAG:
                TagData tagData = AppSocialGlobal.getInstance().tmp_tag;
                int index = mViewPager.getCurrentItem();
                PhotoData photoData = mPhotos.get(index);
                if (photoData.tags.contains(tagData)) {
                    new AlertDialog.Builder(TagProductActivity.this)
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
                    photoData.tags.add(tagData);
                    FrameLayout tagsLayout = mTagLayoutMap.get(index);
                    Size size = mSizeMap.get(index);
                    addTagView(tagsLayout, photoData, tagData, size);
                }

                break;
        }
    }

    @Override
    protected void onDestroy() {
        EventBus.getDefault().unregister(this);
        super.onDestroy();
    }

    public class PhotoPagerAdapter extends PagerAdapter {
        @NonNull
        @Override
        public Object instantiateItem(@NonNull ViewGroup container, final int position) {
            LayoutInflater inflater = LayoutInflater.from(TagProductActivity.this);
            final int size = AppSocialGlobal.getScreenWidth(TagProductActivity.this);
            FrameLayout layout = (FrameLayout) inflater.inflate(R.layout.tag_product_photo, container, false);
            layout.setLayoutParams(new ViewGroup.LayoutParams(size, size));
            final PostImageView imageView = layout.findViewById(R.id.image_view);
            imageView.setLayoutParams(new FrameLayout.LayoutParams(size, size));
            final FrameLayout tagsLayout = layout.findViewById(R.id.tags_framelayout);
            tagsLayout.setLayoutParams(new FrameLayout.LayoutParams(size, size));
            final PhotoData photoData = mPhotos.get(position);
            String photoUrl = photoData.photoUrl;
            AppSocialGlobal.loadImage(photoUrl, imageView);
            imageView.setImageCallback(new PostImageView.ImageCallback() {
                @Override
                public void onClick(float percentX, float percentY) {
                    AppSocialGlobal.getInstance().tmp_tag = new TagData(percentX, percentY);
                    startActivity(new Intent(TagProductActivity.this, SelectProductActivity.class));
                }

                @Override
                public void onDoubleClick() {

                }

                @Override
                public void onMeasureFinish(float width, float height) {
                    Size size = new Size((int)width, (int)height);
                    mSizeMap.put(position, size);
                    for (TagData tagData : photoData.tags) {
                        addTagView(tagsLayout, photoData, tagData, size);
                    }
                }
            });
            container.addView(layout);

            mTagLayoutMap.put(position, tagsLayout);
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
            return mPhotos.size();
        }
    }

    private void addTagView(final FrameLayout tagsLayout, final PhotoData photoData, final TagData tagData, final Size size) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                final TagView tagView = new TagView(TagProductActivity.this, mViewPager, tagData, true, size.getWidth(), size.getHeight());
                tagView.setLayoutParams(new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
                tagView.setTagOnTouchCallback(new TagView.TagOnTouchCallback() {
                    @Override
                    public void onClick() {
                        tagsLayout.removeView(tagView);
                        photoData.tags.remove(tagData);
                    }

                    @Override
                    public void onMoveTo(float percentX, float percentY) {
                        tagData.percentX = percentX;
                        tagData.percentY = percentY;
                    }
                });
                tagsLayout.addView(tagView);
            }
        });
    }
}
