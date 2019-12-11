package com.teamcircle.circlesdk.activity;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.hardware.Camera;
import android.media.CamcorderProfile;
import android.media.MediaMetadataRetriever;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.Gravity;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.teamcircle.circlesdk.R;
import com.teamcircle.circlesdk.helper.AmazonS3Helper;
import com.teamcircle.circlesdk.helper.ApiHelper;
import com.teamcircle.circlesdk.helper.AppSocialGlobal;
import com.teamcircle.circlesdk.model.MessageEvent;
import com.teamcircle.circlesdk.model.PhotoData;
import com.teamcircle.circlesdk.model.PostData;
import com.teamcircle.circlesdk.model.VideoData;

import org.greenrobot.eventbus.EventBus;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

public class CameraActivity extends Activity {
    private FrameLayout mPreviewLayout;
    private ImageView mTakePhoto;
    private boolean mIsShapeSquare = true;
    private Timer mTimer;
    private RecordTimerTask mTimerTask;
    private int mRecordTime = 0;
    private Camera mCamera;
    private CameraPreview mCameraPreview;
    private int mCameraId;
    private MediaRecorder mRecorder;
    private boolean mIsRecording = false;
    private ProgressBar mVideoTimeProgress;
    private ImageView mSwitchCamera;
    private TextView mReshape;
    private int width, height;
    private Camera.Size previewSize, pictureSize;

    private static final int REQUEST_CAMERA = 120;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_photo_camera);
        mCameraId = Camera.CameraInfo.CAMERA_FACING_BACK;
        final FrameLayout layout = findViewById(R.id.layout);
        final LinearLayout topLayout = findViewById(R.id.top);
        final LinearLayout bottomLayout = findViewById(R.id.bottom);
        final ImageView topCoverImage = findViewById(R.id.top_cover_image);
        final ImageView bottomCoverImage = findViewById(R.id.bottom_cover_image);
        final View topCover = findViewById(R.id.top_cover);
        final View bottomCover = findViewById(R.id.bottom_cover);

        layout.post(new Runnable() {
            @Override
            public void run() {
                width = layout.getWidth();
                height = layout.getHeight();

                FrameLayout.LayoutParams layoutParams1 = (FrameLayout.LayoutParams) topLayout.getLayoutParams();
                layoutParams1.height = (height - width) / 2;
                topLayout.setLayoutParams(layoutParams1);

                FrameLayout.LayoutParams layoutParams2 = (FrameLayout.LayoutParams) bottomLayout.getLayoutParams();
                layoutParams2.height = (height - width) / 2;
                bottomLayout.setLayoutParams(layoutParams2);

                LinearLayout.LayoutParams layoutParams3 = (LinearLayout.LayoutParams) topCover.getLayoutParams();
                layoutParams3.height = width / 6;
                topCover.setLayoutParams(layoutParams3);

                LinearLayout.LayoutParams layoutParams4 = (LinearLayout.LayoutParams) bottomCover.getLayoutParams();
                layoutParams4.height = width / 6;
                bottomCover.setLayoutParams(layoutParams4);

                if (mCameraPreview != null) {
                    FrameLayout.LayoutParams layoutParams = (FrameLayout.LayoutParams) mCameraPreview.getLayoutParams();
                    layoutParams.width = width;
                    layoutParams.height = width * previewSize.width / previewSize.height;
                    layoutParams.gravity = Gravity.CENTER;
                    mCameraPreview.setLayoutParams(layoutParams);
                }
            }
        });

        mPreviewLayout = findViewById(R.id.camera_preview);
        mTakePhoto = findViewById(R.id.start);
        mVideoTimeProgress = findViewById(R.id.video_time_progress);

        mSwitchCamera = findViewById(R.id.switch_camera);
        mSwitchCamera.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mCamera != null) {
                    if(mCameraId == Camera.CameraInfo.CAMERA_FACING_BACK){
                        mCameraId = Camera.CameraInfo.CAMERA_FACING_FRONT;
                    }
                    else {
                        mCameraId = Camera.CameraInfo.CAMERA_FACING_BACK;
                    }
                    releaseCamera();
                    startCamera();
                }
            }
        });

        mReshape = findViewById(R.id.reshape);
        mReshape.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mIsShapeSquare = !mIsShapeSquare;
                if (mIsShapeSquare) {
                    mReshape.setText("1:1");
                    topCoverImage.setVisibility(View.VISIBLE);
                    bottomCoverImage.setVisibility(View.VISIBLE);
                } else {
                    mReshape.setText("3:4");
                    topCoverImage.setVisibility(View.GONE);
                    bottomCoverImage.setVisibility(View.GONE);
                }
            }
        });

        if (AppSocialGlobal.getInstance().photoPickerType == 2) {
            mTakePhoto.setImageResource(R.drawable.take_video);
            mVideoTimeProgress.setVisibility(View.VISIBLE);
        } else if (AppSocialGlobal.getInstance().photoPickerType == 0) {
            mReshape.setVisibility(View.GONE);
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
    }

    @Override
    public void onResume() {
        super.onResume();

        if (checkCameraHardware(this)) {
            ArrayList<String> permissions = new ArrayList<>();
            String permission1 = Manifest.permission.CAMERA;
            String permission2 = Manifest.permission.RECORD_AUDIO;
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED &&
                    ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
                startCamera();
            } else {
                if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                    permissions.add(permission1);
                }
                if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                    permissions.add(permission2);
                }
                if (permissions.size() > 0) {
                    ActivityCompat.requestPermissions(this, permissions.toArray(new String[permissions.size()]), REQUEST_CAMERA);
                }
            }
        }
    }

    @Override
    public void onPause() {
        releaseRecorder();
        releaseCamera();
        stopRecordTimer();
        super.onPause();
    }

    @SuppressLint("ClickableViewAccessibility")
    public void startCamera() {
        mPreviewLayout.removeAllViews();
        mCamera = getCameraInstance();
        if (mCamera != null) {
            Camera.Parameters cameraParameters = mCamera.getParameters();
            List<Camera.Size> list = cameraParameters.getSupportedPreviewSizes();
            previewSize = list.get(0);

            List<Camera.Size> list2 = cameraParameters.getSupportedPictureSizes();
            pictureSize = list2.get(0);
            for (Camera.Size size : list2) {
                if (size.width == previewSize.width && size.height == previewSize.height) {
                    pictureSize = size;
                    break;
                }

            }
            cameraParameters.setPictureSize(pictureSize.width, pictureSize.height);
            cameraParameters.setPreviewSize(previewSize.width, previewSize.height);

            List<String> focusModes = cameraParameters.getSupportedFocusModes();
            if (focusModes.contains(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE)) {
                cameraParameters.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);
            } else if (focusModes.contains(Camera.Parameters.FOCUS_MODE_AUTO)) {
                cameraParameters.setFocusMode(Camera.Parameters.FOCUS_MODE_AUTO);
            }

            mCamera.setParameters(cameraParameters);
            setDisplayOrientation();

            mCameraPreview = new CameraPreview(this);
            FrameLayout.LayoutParams layoutParams = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            layoutParams.width = width;
            layoutParams.height = width * previewSize.width / previewSize.height;
            layoutParams.gravity = Gravity.CENTER;
            mCameraPreview.setLayoutParams(layoutParams);
            mPreviewLayout.addView(mCameraPreview);

            if (AppSocialGlobal.getInstance().photoPickerType == 2) {
                mTakePhoto.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (!mIsRecording) {
                            mIsRecording = true;
                            mTakePhoto.setImageResource(R.drawable.take_video_press);
                            mSwitchCamera.setVisibility(View.GONE);
                            mReshape.setVisibility(View.GONE);
                            if (prepareRecorder()) {
                                mRecorder.start();
                                startRecordTimer();
                            } else {
                                releaseRecorder();
                            }
                        } else {
                            if (mRecordTime > 1000) {
                                mIsRecording = false;
                                mTakePhoto.setImageResource(R.drawable.take_video);
                                stopRecordTimer();
                                mRecorder.stop();
                                releaseRecorder();
                                saveVideo();
                            }
                        }

                    }
                });
            } else {
                mTakePhoto.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        mCamera.takePicture(null, null, mPictureCallback);
                    }
                });
            }
        }
    }

    private void releaseCamera() {
        if (mCamera != null) {
            mCamera.stopPreview();
            mCamera.setPreviewCallback(null);
            mCamera.release();
            mCamera = null;
        }
    }

    private boolean prepareRecorder() {
        mRecorder = new MediaRecorder();
        setDisplayOrientation();

        mCamera.unlock();
        mRecorder.setCamera(mCamera);

        mRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        mRecorder.setVideoSource(MediaRecorder.VideoSource.DEFAULT);
        CamcorderProfile profile = CamcorderProfile.get(CamcorderProfile.QUALITY_720P);
        mRecorder.setProfile(profile);
        String filePath = getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS).getAbsolutePath() + "/tmp.mp4";
        mRecorder.setOutputFile(filePath);
        mRecorder.setVideoSize(previewSize.width, previewSize.height);
        mRecorder.setMaxDuration(30000);
        mRecorder.setMaxFileSize(100000000);
        mRecorder.setPreviewDisplay(mCameraPreview.getHolder().getSurface());
        mRecorder.setOnInfoListener(new MediaRecorder.OnInfoListener() {
            @Override
            public void onInfo(MediaRecorder mr, int what, int extra) {
                if(what == MediaRecorder.MEDIA_RECORDER_INFO_MAX_FILESIZE_REACHED ||
                        what == MediaRecorder.MEDIA_RECORDER_INFO_MAX_DURATION_REACHED) {
                    mIsRecording = false;
                    stopRecordTimer();
                    releaseRecorder();
                    saveVideo();
                }
            }
        });
        try {
            mRecorder.prepare();
        } catch (IOException e) {
            e.printStackTrace();
            releaseRecorder();
            return false;
        }
        return true;
    }

    private void releaseRecorder() {
        if (mRecorder != null) {
            mRecorder.reset();
            mRecorder.release();
            mRecorder = null;
            mCamera.lock();
        }
    }

    public class CameraPreview extends SurfaceView implements SurfaceHolder.Callback {
        private SurfaceHolder mHolder;

        public CameraPreview(Context context) {
            super(context);
            mHolder = getHolder();
            mHolder.addCallback(this);
            mHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
        }

        public void surfaceCreated(SurfaceHolder holder) {

        }

        public void surfaceDestroyed(SurfaceHolder holder) {
            mHolder.removeCallback(this);
        }

        public void surfaceChanged(SurfaceHolder holder, int format, int w, int h) {
            if (mHolder.getSurface() == null) {
                return;
            }
            try {
                mCamera.setPreviewDisplay(mHolder);
                mCamera.startPreview();
            } catch (Exception e) {
                Log.e("start camera preview", e.getMessage());
            }
        }
    }

    private boolean checkCameraHardware(Context context) {
        return context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA);
    }

    private Camera getCameraInstance(){
        Camera c = null;
        try {
            c = Camera.open(mCameraId);
        }
        catch (Exception e){

        }
        return c;
    }

    private void setDisplayOrientation() {
        final Camera.CameraInfo info = new Camera.CameraInfo();
        Camera.getCameraInfo(mCameraId, info);
        int degrees = 0;
        final int currentRotation = ((WindowManager)
                getSystemService(Activity.WINDOW_SERVICE)).getDefaultDisplay()
                .getRotation();
        switch (currentRotation) {
            case Surface.ROTATION_0:
                degrees = 0;
                break;
            case Surface.ROTATION_90:
                degrees = 90;
                break;
            case Surface.ROTATION_180:
                degrees = 180;
                break;
            case Surface.ROTATION_270:
                degrees = 270;
                break;
        }

        int displayOrientation = 0;
        if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
            displayOrientation = (info.orientation + degrees) % 360;
            displayOrientation = (360 - displayOrientation) % 360;
            if (mRecorder != null) {
                mRecorder.setOrientationHint(displayOrientation + 180);
            }
        } else {
            displayOrientation = (info.orientation - degrees + 360) % 360;
            if (mRecorder != null) {
                mRecorder.setOrientationHint(displayOrientation);
            }
        }

        mCamera.setDisplayOrientation(displayOrientation);

    }

    private Camera.PictureCallback mPictureCallback = new Camera.PictureCallback() {

        @Override
        public void onPictureTaken(byte[] data, Camera camera) {
            savePhoto(data, camera);
        }
    };

    private void savePhoto(byte[] data, Camera camera) {
        final Camera.CameraInfo info = new Camera.CameraInfo();
        Camera.getCameraInfo(mCameraId, info);
        Bitmap b = BitmapFactory.decodeByteArray(data, 0, data.length);
        Matrix mat = new Matrix();
        int rotation = info.orientation;
        mat.postRotate(rotation);
//        if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
//            float cx = b.getWidth() / 2f;
//            float cy = b.getHeight() / 2f;
//            mat.postScale(-1, 1, cx, cy);
//        }
        int x, y, width, height;

        if (b.getWidth() > b.getHeight()) {
            y = 0;
            height = b.getHeight();
            if (mIsShapeSquare) {
                x = (b.getWidth() - b.getHeight()) / 2;
                width = b.getHeight();
            } else {
                x = (b.getWidth() - b.getHeight() * 4 / 3) / 2;
                width = b.getHeight() * 4 / 3;
            }

        } else {
            x = 0;
            width = b.getWidth();
            if (mIsShapeSquare) {
                y = (b.getHeight() - b.getWidth()) / 2;
                height = b.getWidth();
            } else {
                y = (b.getHeight() - b.getWidth() * 4 / 3) / 2;
                height = b.getWidth() * 4 / 3;
            }

        }
        Bitmap bitmap = Bitmap.createBitmap(b, x, y, width, height, mat, true);
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "JPEG_" + timeStamp;
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS);
        File image = null;
        try {
            image = File.createTempFile(
                    imageFileName,  /* prefix */
                    ".jpg",         /* suffix */
                    storageDir      /* directory */
            );
            FileOutputStream out = new FileOutputStream(image);
            if (AppSocialGlobal.getInstance().photoPickerType == 0) {
                bitmap = AppSocialGlobal.getResizedBitmapWithFixedWidth(bitmap, AppSocialGlobal.dpToPx(this, 90));
            } else {
                bitmap = AppSocialGlobal.getResizedBitmapWithFixedWidth(bitmap, AppSocialGlobal.getScreenWidth(this));
            }

            bitmap.compress(Bitmap.CompressFormat.JPEG, 30, out);
            out.flush();
            out.close();
            String path = image.getAbsolutePath();

            if (AppSocialGlobal.getInstance().photoPickerType == 0) {
                AmazonS3Helper.getInstance().upload(path, new AmazonS3Helper.FileUploadCallback() {
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
            } else {
                ArrayList<PhotoData> photos = new ArrayList<>();
                PhotoData photoData = new PhotoData(path, bitmap.getWidth(), bitmap.getHeight());
                photos.add(photoData);
                PostData postData = new PostData();
                postData.photos = photos;
                AppSocialGlobal.getInstance().tmp_post = postData;
                startActivity(new Intent(this, ReadyToPostActivity.class));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        finish();
    }

    private void startRecordTimer() {
        mRecordTime = 0;

        if (mTimer == null) {
            mTimer = new Timer();
        }
        if (mTimerTask == null) {
            mTimerTask = new RecordTimerTask();
        }
        mTimer.schedule(mTimerTask, 0, 200);
    }

    private void stopRecordTimer() {
        if (mTimer != null) {
            mTimer.cancel();
            mTimer = null;
        }
        if (mTimerTask != null) {
            mTimerTask.cancel();
            mTimerTask = null;
        }
    }

    private class RecordTimerTask extends TimerTask {

        @Override
        public void run() {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    mVideoTimeProgress.setProgress(100 * mRecordTime / 30000);
                }
            });
            mRecordTime += 200;
        }
    }

    private void saveVideo() {
        String path = getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS).getAbsolutePath() + "/tmp.mp4";
        final MediaMetadataRetriever retriever = new MediaMetadataRetriever();
        retriever.setDataSource(path);
        String s = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
        int duration = Integer.parseInt(s);

        String ss = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION);
        int w = previewSize.width;
        int h = previewSize.height;
        if (ss.equals("270") || ss.equals("90")) {
            w = previewSize.height;
            h = previewSize.width;
        }
        final VideoData videoData = new VideoData(path, duration, w, h);
        float percentY = (h - w) / 2f / h;
        if (!mIsShapeSquare) {
            percentY = (h - w * 4 / 3f) / 2f / h;
        }
        videoData.startPercentX = 0f;
        videoData.endPercentX = 1.0f;
        videoData.startPercentY = percentY;
        videoData.endPercentY = (h + w) / 2f / h;
        if (!mIsShapeSquare) {
            videoData.endPercentY = (h + w * 4 / 3f) / 2f / h;
        }
        AppSocialGlobal.getInstance().tmp_video = new VideoData(videoData);
        startActivity(new Intent(this, TrimVideoActivity.class));
        finish();
    }
}
