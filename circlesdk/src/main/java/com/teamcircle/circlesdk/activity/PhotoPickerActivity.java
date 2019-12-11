package com.teamcircle.circlesdk.activity;

import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;

import com.teamcircle.circlesdk.R;
import com.teamcircle.circlesdk.fragment.PhotoGalleryFragment;
import com.teamcircle.circlesdk.fragment.VideoGalleryFragment;
import com.teamcircle.circlesdk.helper.AppSocialGlobal;
import com.teamcircle.circlesdk.model.MessageEvent;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;

public class PhotoPickerActivity extends FragmentActivity {
    private boolean mIsSelectPhoto = true;
    private PhotoGalleryFragment mPhotoGallery;
    private VideoGalleryFragment mVideoGallery;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_photo_picker);
        EventBus.getDefault().register(this);
        AppSocialGlobal.getInstance().checkIfMuted(this);

        mPhotoGallery = new PhotoGalleryFragment();
        mVideoGallery = new VideoGalleryFragment();
        final FragmentManager manager = getSupportFragmentManager();
        manager.beginTransaction().replace(R.id.content, mPhotoGallery, "photo_gallery").commitAllowingStateLoss();

        LinearLayout selections = findViewById(R.id.selections);
        if (AppSocialGlobal.getInstance().photoPickerType == 0 ||
                AppSocialGlobal.getInstance().photoPickerType == 3) {
            selections.setVisibility(View.GONE);
        }

        FrameLayout photo = findViewById(R.id.photo);
        final ImageView photoImage = findViewById(R.id.photo_image);
        FrameLayout video = findViewById(R.id.video);
        final ImageView videoImage = findViewById(R.id.video_image);
        photo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!mIsSelectPhoto) {
                    mIsSelectPhoto = true;
                    photoImage.setColorFilter(getResources().getColor(R.color.lightBlueColor));
                    videoImage.setColorFilter(getResources().getColor(R.color.lightGrayColor));
                    manager.beginTransaction().replace(R.id.content, mPhotoGallery, "photo_gallery").commitAllowingStateLoss();
                    AppSocialGlobal.getInstance().photoPickerType = 1;
                }
            }
        });
        video.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mIsSelectPhoto) {
                    mIsSelectPhoto = false;
                    photoImage.setColorFilter(getResources().getColor(R.color.lightGrayColor));
                    videoImage.setColorFilter(getResources().getColor(R.color.lightBlueColor));
                    manager.beginTransaction().replace(R.id.content, mVideoGallery, "video_gallery").commitAllowingStateLoss();
                    AppSocialGlobal.getInstance().photoPickerType = 2;
                }
            }
        });
    }

    @Override
    protected void onDestroy() {
        EventBus.getDefault().unregister(this);
        super.onDestroy();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == PhotoGalleryFragment.REQUEST_READ) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                if (mIsSelectPhoto) {
                    mPhotoGallery.startPhotoLoader();
                } else {
                    mVideoGallery.startVideoLoader();
                }
            }
        }
    }

    @Subscribe(sticky = true)
    public void onEvent(MessageEvent event) {
        switch (event.type) {
            case DONE_CHANGE_PROFILE_IMAGE:
            case DONE_SEND_POST:
                finish();
                break;
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN || keyCode == KeyEvent.KEYCODE_VOLUME_UP) {
            EventBus.getDefault().post(new MessageEvent(MessageEvent.MessageEventType.CHANGE_VOLUME));
        }
        return super.onKeyDown(keyCode, event);
    }
}
