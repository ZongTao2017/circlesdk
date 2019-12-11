package com.teamcircle.circlesdk.activity;

import android.app.Activity;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.viewpager.widget.PagerAdapter;
import androidx.viewpager.widget.ViewPager;

import com.teamcircle.circlesdk.R;
import com.teamcircle.circlesdk.helper.AppSocialGlobal;
import com.teamcircle.circlesdk.model.PhotoData;
import com.teamcircle.circlesdk.view.ViewPagerIndicator;

import java.util.ArrayList;

public class ShowPhotosActivity extends Activity {
    private ArrayList<PhotoData> mPhotos;
    private ViewPager mViewPager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_show_photos);

        mPhotos = AppSocialGlobal.getInstance().tmp_post.photos;
        final FrameLayout photosLayout = findViewById(R.id.photos);
        mViewPager = findViewById(R.id.photos_view_pager);
        FrameLayout.LayoutParams layoutParams = (FrameLayout.LayoutParams) mViewPager.getLayoutParams();
        layoutParams.height = AppSocialGlobal.getScreenWidth(this);
        mViewPager.setLayoutParams(layoutParams);
        PhotoPagerAdapter adapter = new PhotoPagerAdapter();
        mViewPager.setAdapter(adapter);
        final TextView textView = photosLayout.findViewById(R.id.text);

        photosLayout.setVisibility(View.VISIBLE);
        photosLayout.post(new Runnable() {
            @Override
            public void run() {
                int width = photosLayout.getWidth();
                int height = photosLayout.getHeight();
                FrameLayout.LayoutParams layoutParams = (FrameLayout.LayoutParams) textView.getLayoutParams();
                layoutParams.topMargin = height / 2 + width / 2 + AppSocialGlobal.dpToPx(ShowPhotosActivity.this, 30);
                textView.setLayoutParams(layoutParams);
                if (mPhotos.size() > 1) {
                    final ViewPagerIndicator viewPagerIndicator = new ViewPagerIndicator(ShowPhotosActivity.this, mPhotos.size());
                    FrameLayout.LayoutParams layoutParams2 = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                    layoutParams2.topMargin = height / 2 + width / 2 + AppSocialGlobal.dpToPx(ShowPhotosActivity.this, 10);
                    viewPagerIndicator.setLayoutParams(layoutParams2);
                    photosLayout.addView(viewPagerIndicator);
                    mViewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
                        @Override
                        public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

                        }

                        @Override
                        public void onPageSelected(int position) {
                            viewPagerIndicator.setCurrent(position);
                        }

                        @Override
                        public void onPageScrollStateChanged(int state) {

                        }
                    });
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
    }

    public class PhotoPagerAdapter extends PagerAdapter {
        @NonNull
        @Override
        public Object instantiateItem(@NonNull ViewGroup container, final int position) {
            LayoutInflater inflater = LayoutInflater.from(ShowPhotosActivity.this);
            int size = AppSocialGlobal.getScreenWidth(ShowPhotosActivity.this);
            final FrameLayout layout = (FrameLayout) inflater.inflate(R.layout.photo, container, false);
            layout.setLayoutParams(new ViewGroup.LayoutParams(size, size));
            final ImageView imageView = layout.findViewById(R.id.image_view);
            imageView.setLayoutParams(new FrameLayout.LayoutParams(size, size));
            String photoUrl = mPhotos.get(position).photoUrl;
            AppSocialGlobal.loadImage(photoUrl, imageView);
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
            return mPhotos.size();
        }
    }
}
