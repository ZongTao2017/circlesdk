package com.teamcircle.circlesdk.fragment;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.PointF;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.util.Log;
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

import com.squareup.picasso.Picasso;
import com.teamcircle.circlesdk.R;
import com.teamcircle.circlesdk.activity.CameraActivity;
import com.teamcircle.circlesdk.activity.ReadyToPostActivity;
import com.teamcircle.circlesdk.helper.AmazonS3Helper;
import com.teamcircle.circlesdk.helper.ApiHelper;
import com.teamcircle.circlesdk.helper.AppSocialGlobal;
import com.teamcircle.circlesdk.helper.PhotoGalleryHelper;
import com.teamcircle.circlesdk.model.CropPhoto;
import com.teamcircle.circlesdk.model.MessageEvent;
import com.teamcircle.circlesdk.model.PhotoData;
import com.teamcircle.circlesdk.model.PostData;
import com.teamcircle.circlesdk.view.ImageSelectView;
import com.teamcircle.circlesdk.view.TouchImageView;

import org.greenrobot.eventbus.EventBus;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

public class PhotoGalleryFragment extends Fragment {
    private ArrayList<PhotoData> mPhotos;
    private RecyclerView mRecyclerView;
    private PhotoGalleryAdapter mAdapter;
    private ArrayList<Integer> mSelected;
    private ArrayList<CropPhoto> mSelectedCropPhotoList;
    private int mCurrent = 0;
    private TouchImageView mImageView;
    private FrameLayout mToggleLayout;
    private ImageView mToggleImageView;
    private boolean mIsToggled;
    private Handler mHandler;
    private FrameLayout mNext;
    private PointF mTouchPoint;
    private boolean mIsGoingUp;
    private boolean mIsClick;
    private boolean mTouchable;
    private FrameLayout mResizeLayout;

    public static final int REQUEST_READ = 110;
    private static final int CELL_NUMBER_IN_ROW = 4;
    private static final int MAX = 9;

    @SuppressLint("ClickableViewAccessibility")
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        final View view = inflater.inflate(R.layout.fragment_photo_gallery, container, false);

        mImageView = view.findViewById(R.id.image_view);
        mToggleLayout = view.findViewById(R.id.toggle_photos);
        mToggleImageView = view.findViewById(R.id.toggle_photos_image);
        mIsToggled = false;
        mRecyclerView = view.findViewById(R.id.photo_gallery);
        mRecyclerView.setLayoutManager(new GridLayoutManager(getContext(), CELL_NUMBER_IN_ROW, RecyclerView.VERTICAL, false));
        mRecyclerView.getRecycledViewPool().setMaxRecycledViews(0, 8 * CELL_NUMBER_IN_ROW);

        mPhotos = new ArrayList<>();
        mSelected = new ArrayList<>();
        mSelectedCropPhotoList = new ArrayList<>();
        mHandler = new Handler();
        mTouchPoint = new PointF();
        mIsGoingUp = true;
        mIsClick = true;
        mTouchable = true;

        if (ContextCompat.checkSelfPermission(getContext(), Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            String permission = Manifest.permission.READ_EXTERNAL_STORAGE;
            ActivityCompat.requestPermissions(getActivity(), new String[]{permission}, REQUEST_READ);
        }
        if (AppSocialGlobal.getInstance().galleryPhotos != null) {
            mPhotos.addAll(AppSocialGlobal.getInstance().galleryPhotos);
        } else {
            startPhotoLoader();
        }

        final int size = AppSocialGlobal.getScreenWidth(getContext());
        ViewGroup.LayoutParams layoutParams = mImageView.getLayoutParams();
        layoutParams.height = size;
        mImageView.setLayoutParams(layoutParams);

        mResizeLayout = view.findViewById(R.id.resize);
        FrameLayout.LayoutParams layoutParams3 = (FrameLayout.LayoutParams) mResizeLayout.getLayoutParams();
        layoutParams3.topMargin = size + AppSocialGlobal.dpToPx(getContext(), 10);
        mResizeLayout.setLayoutParams(layoutParams3);
        mResizeLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mImageView.resize();
            }
        });

        if (AppSocialGlobal.getInstance().photoPickerType == 0) {
            mResizeLayout.setVisibility(View.GONE);
            mImageView.setMinScaleLimit(true);
        }

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
        TextView nextText = view.findViewById(R.id.next_text);
        if (AppSocialGlobal.getInstance().photoPickerType == 0) {
            nextText.setText("Done");
        } else {
            nextText.setText("Next");
        }
        mNext.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (AppSocialGlobal.getInstance().photoPickerType == 0) {
                    saveProfileImage();
                    getActivity().finish();
                } else  {
                    ArrayList<PhotoData> photos = saveSelectCropBitmap();
                    if (photos.size() > 0) {
                        PostData postData = new PostData();
                        postData.photos = photos;
                        AppSocialGlobal.getInstance().tmp_post = postData;
                        startActivity(new Intent(getActivity(), ReadyToPostActivity.class));
                    }
                }
            }
        });

        if (mPhotos.size() > 0) {
            setImage(mPhotos.get(0), false);
        }
        mImageView.setImageCallback(new TouchImageView.ImageCallback() {
            @Override
            public void onMoveFinish() {
                String url = mPhotos.get(mCurrent).photoUrl;
                for (int i = 0; i < mSelectedCropPhotoList.size(); i++) {
                    CropPhoto cropPhoto = mSelectedCropPhotoList.get(i);
                    if (cropPhoto.photoUrl.equals(url)) {
                        CropPhoto newCropPhoto = mImageView.getCropPhoto();
                        newCropPhoto.photoUrl = cropPhoto.photoUrl;
                        mSelectedCropPhotoList.set(i, newCropPhoto);
                        return;
                    }
                }
            }
        });

        mAdapter = new PhotoGalleryAdapter();
        mRecyclerView.setAdapter(mAdapter);

        return view;
    }

    public void startPhotoLoader() {
        if (ContextCompat.checkSelfPermission(getContext(), Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
            PhotoGalleryHelper.getInstance().startPhotoLoader(getContext(), new PhotoGalleryHelper.PhotoGalleryLoaderCallback() {
                @Override
                public void photoGalleryLoaderDone(ArrayList<PhotoData> photos) {
                    AppSocialGlobal.getInstance().galleryPhotos = photos;
                    mPhotos.addAll(photos);
                    if (mPhotos.size() > 0) {
                        setImage(mPhotos.get(0), false);
                    }
                    mAdapter.notifyDataSetChanged();
                }
            });
        }
    }

    private class PhotoGalleryAdapter extends RecyclerView.Adapter<PhotoViewHolder> {
        @NonNull
        @Override
        public PhotoViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            LayoutInflater layoutInflater = LayoutInflater.from(parent.getContext());
            View view = layoutInflater.inflate(R.layout.photo_gallery_item, parent, false);

            int width = AppSocialGlobal.getScreenWidth(getContext()) / CELL_NUMBER_IN_ROW;
            view.setLayoutParams(new ViewGroup.LayoutParams(width, width));
            return new PhotoViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull final PhotoViewHolder holder, final int position) {
            final int pos = position - 1;
            if (position == 0) {
                holder.mCurrentView.setVisibility(View.GONE);
                holder.mSelectedView.setVisibility(View.GONE);
                holder.mImageView.setVisibility(View.GONE);
                holder.mCameraImage.setVisibility(View.VISIBLE);
                holder.mCameraImage.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        startActivity(new Intent(getContext(), CameraActivity.class));
                    }
                });
            } else {
                holder.mImageView.setVisibility(View.VISIBLE);
                holder.mCameraImage.setVisibility(View.GONE);
                holder.mUnselectableView.setVisibility(View.GONE);
                holder.mSelectedView.setVisibility(View.VISIBLE);
                String path = mPhotos.get(pos).photoUrl;
                if (!holder.mPhotoUrl.equals(path)) {
                    holder.mPhotoUrl = path;
                    int width = AppSocialGlobal.getScreenWidth(getContext()) / CELL_NUMBER_IN_ROW;
                    Picasso.get().load(new File(path)).centerCrop().resize(width, width)
                            .error(R.drawable.error)
                            .into(holder.mImageView);
                }

                if (pos == mCurrent) {
                    holder.mCurrentView.setVisibility(View.VISIBLE);
                } else {
                    holder.mCurrentView.setVisibility(View.GONE);
                }
                if (AppSocialGlobal.getInstance().photoPickerType == 0) {
                    holder.mSelectedView.setVisibility(View.GONE);
                } else {
                    holder.mSelectedView.setVisibility(View.VISIBLE);
                    if (mSelected.contains(pos)) {
                        holder.mSelectedView.setNumber(mSelected.indexOf(pos) + 1);
                    } else {
                        holder.mSelectedView.setNumber(0);
                        if (mSelected.size() == MAX) {
                            holder.mSelectedView.setVisibility(View.GONE);
                            holder.mUnselectableView.setVisibility(View.VISIBLE);
                        }
                    }
                }

            }
            holder.mImageView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (AppSocialGlobal.getInstance().photoPickerType == 0) {
                        if (mCurrent != pos) {
                            PhotoData photoData = mPhotos.get(pos);
                            setImage(photoData, false);
                            mCurrent = pos;
                        }
                    } else {
                        if (mCurrent != pos) {
                            mCurrent = pos;
                            PhotoData photoData = mPhotos.get(pos);
                            if (!mSelected.contains(pos) && mSelected.size() < MAX) {
                                mSelected.add(pos);
                                setImage(photoData, true);
                            } else {
                                setImage(photoData, false);
                            }
                        } else {
                            if (mSelected.contains(pos)) {
                                int index = mSelected.indexOf(pos);
                                mSelected.remove(index);
                                mSelectedCropPhotoList.remove(index);
                            } else if (mSelected.size() < MAX) {
                                String url = mPhotos.get(pos).photoUrl;
                                mSelected.add(pos);
                                CropPhoto cropPhoto = mImageView.getCropPhoto();
                                cropPhoto.photoUrl = url;
                                mSelectedCropPhotoList.add(cropPhoto);
                            }
                        }
                        resetAll();
                    }

                    mAdapter.notifyDataSetChanged();
                }
            });

        }

        @Override
        public int getItemCount() {
            return mPhotos.size() + 1;
        }
    }

    private class PhotoViewHolder extends RecyclerView.ViewHolder {
        ImageView mImageView;
        View mCurrentView;
        ImageSelectView mSelectedView;
        String mPhotoUrl;
        FrameLayout mCameraImage;
        View mUnselectableView;

        public PhotoViewHolder(View itemView) {
            super(itemView);
            mImageView = itemView.findViewById(R.id.image);
            mCurrentView = itemView.findViewById(R.id.current);
            mSelectedView = itemView.findViewById(R.id.selected);
            mPhotoUrl = "";
            mCameraImage = itemView.findViewById(R.id.camera);
            mUnselectableView = itemView.findViewById(R.id.unselectable);
        }
    }

    private void setImage(PhotoData photoData, final boolean add) {
        final String path = photoData.photoUrl;
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inPreferredConfig = Bitmap.Config.ARGB_8888;
        options.inScaled = false;
        Bitmap bitmap = BitmapFactory.decodeFile(path, options);
        if (bitmap == null) return;
        Matrix matrix = new Matrix();
        matrix.postRotate(photoData.orientation);
        Bitmap rotatedBitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
        mImageView.setImageBitmap(rotatedBitmap);
        for (CropPhoto cropPhoto : mSelectedCropPhotoList) {
            if (cropPhoto.photoUrl.equals(path)) {
                mImageView.setOriginalImageMatrix(1, cropPhoto.scale, cropPhoto.offsetX, cropPhoto.offsetY);
                return;
            }
        }
        mImageView.setOriginalImageMatrix(0, 0, 0, 0);
        if (add) {
            CropPhoto cropPhoto = mImageView.getCropPhoto();
            cropPhoto.photoUrl = path;
            mSelectedCropPhotoList.add(cropPhoto);
        }
    }

    private ArrayList<PhotoData> saveSelectCropBitmap() {
        ArrayList<PhotoData> photos = new ArrayList<>();
        if (mSelectedCropPhotoList.size() == 0) {
            CropPhoto cropPhoto = mImageView.getCropPhoto();
            cropPhoto.photoUrl = mPhotos.get(mCurrent).photoUrl;
            mSelectedCropPhotoList.add(cropPhoto);
            mSelected.add(mCurrent);
            mAdapter.notifyDataSetChanged();
        }
        for (int i = 0; i < mSelectedCropPhotoList.size(); i++) {
            CropPhoto cropPhoto = mSelectedCropPhotoList.get(i);
            Bitmap bitmap = Bitmap.createBitmap(cropPhoto.bitmap, (int) cropPhoto.startX, (int) cropPhoto.startY,
                    (int) cropPhoto.width, (int) cropPhoto.height);
            bitmap = AppSocialGlobal.getResizedBitmapWithFixedWidth(bitmap, 640);
            String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
            String imageFileName = "JPEG_" + String.format("%d", i) + "_" + timeStamp;
            File storageDir = getContext().getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS);
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
                String url = image.getAbsolutePath();
                PhotoData photoData = new PhotoData(url, bitmap.getWidth(), bitmap.getHeight());
                photos.add(photoData);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return photos;
    }

    private void saveProfileImage() {
        try {
            CropPhoto cropPhoto = mImageView.getCropPhoto();
            Bitmap bitmap = Bitmap.createBitmap(cropPhoto.bitmap, (int) cropPhoto.startX, (int) cropPhoto.startY,
                    (int) cropPhoto.width, (int) cropPhoto.height);
            bitmap = AppSocialGlobal.getResizedBitmapWithFixedWidth(bitmap, AppSocialGlobal.dpToPx(getContext(), 90));
            String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
            String imageFileName = "JPEG_" + timeStamp;
            File storageDir = getContext().getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS);
            File image = File.createTempFile(
                    imageFileName,  /* prefix */
                    ".jpg",         /* suffix */
                    storageDir      /* directory */
            );
            FileOutputStream out = new FileOutputStream(image);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 50, out);
            out.flush();
            out.close();
            AmazonS3Helper.getInstance().upload(image.getAbsolutePath(), new AmazonS3Helper.FileUploadCallback() {
                @Override
                public void onComplete(final String fileUrl) {
                    Log.d("S3File Upload", "Complete!");
                    ApiHelper.editProfile(fileUrl, AppSocialGlobal.getInstance().me.username, null, new ApiHelper.ApiCallback() {
                        @Override
                        public void onSuccess(JSONObject response) {
                            Log.d("APIEditProfile success", response.toString());
                            AppSocialGlobal.getInstance().me.photoUrl = fileUrl;
                            EventBus.getDefault().post(new MessageEvent(MessageEvent.MessageEventType.DONE_CHANGE_PROFILE_IMAGE));
                        }

                        @Override
                        public void onFail(String errorMsg) {
                            Log.e("APIEditProfile fail", errorMsg);
                        }
                    });
                }
            });
        } catch (IOException e) {
            e.printStackTrace();
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

    private void resetAll() {
        if (AppSocialGlobal.getInstance().photoPickerType != 0) {
            if (mSelectedCropPhotoList.size() > 1) {
                for (int i = 0; i < mSelectedCropPhotoList.size(); i++) {
                    CropPhoto cropPhoto = mSelectedCropPhotoList.get(i);
                    if (cropPhoto.scale < cropPhoto.originalScale) {
                        cropPhoto.scale = cropPhoto.originalScale;
                        cropPhoto.offsetX = cropPhoto.originalOffsetX;
                        cropPhoto.offsetY = cropPhoto.originalOffsetY;
                        if (cropPhoto.bmWidth > cropPhoto.bmHeight) {
                            cropPhoto.startX = (cropPhoto.bmWidth - cropPhoto.bmHeight) / 2;
                            cropPhoto.startY = 0;
                            cropPhoto.width = cropPhoto.bmHeight;
                            cropPhoto.height = cropPhoto.bmHeight;
                        } else {
                            cropPhoto.startX = 0;
                            cropPhoto.startY = (cropPhoto.bmHeight - cropPhoto.width) / 2;
                            cropPhoto.width = cropPhoto.bmWidth;
                            cropPhoto.height = cropPhoto.bmWidth;
                        }
                        mSelectedCropPhotoList.set(i, cropPhoto);
                        if (mSelected.contains(mCurrent)) {
                            if (mSelected.indexOf(mCurrent) == i) {
                                mImageView.setOriginalImageMatrix(1, cropPhoto.scale, cropPhoto.offsetX, cropPhoto.offsetY);
                            }
                        }
                    }
                }
                mResizeLayout.setVisibility(View.GONE);
                mImageView.setMinScaleLimit(true);
            } else {
                mResizeLayout.setVisibility(View.VISIBLE);
                mImageView.setMinScaleLimit(false);
            }
        }
    }
}
