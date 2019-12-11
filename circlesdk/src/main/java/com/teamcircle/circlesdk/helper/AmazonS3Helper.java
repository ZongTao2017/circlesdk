package com.teamcircle.circlesdk.helper;

import android.content.Context;
import android.util.Log;

import com.amazonaws.mobile.client.AWSMobileClient;
import com.amazonaws.mobile.client.Callback;
import com.amazonaws.mobile.client.UserStateDetails;
import com.amazonaws.mobile.config.AWSConfiguration;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferListener;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferObserver;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferState;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferUtility;
import com.amazonaws.regions.Region;
import com.amazonaws.services.s3.AmazonS3Client;

import org.json.JSONObject;

import java.io.File;
import java.util.UUID;

public class AmazonS3Helper {
    private static AmazonS3Helper instance;
    private Context context;
    private TransferUtility transferUtility;
    private String bucketName;
    private String region;

    private static final String TAG = "S3File";

    public static AmazonS3Helper getInstance() {
        if (instance == null) {
            instance = new AmazonS3Helper();
        }
        return instance;
    }

    public void init(Context appContext) {
        context = appContext;
        AWSMobileClient.getInstance().initialize(context, new Callback<UserStateDetails>() {
            @Override
            public void onResult(UserStateDetails userStateDetails) {
                try {
                    Log.e(TAG, "AWSMobileClient initialized. User State is " + userStateDetails.getUserState());
                    AWSConfiguration configuration = AWSMobileClient.getInstance().getConfiguration();
                    JSONObject object = configuration.optJsonObject("S3TransferUtility");
                    region = object.optString("Region");
                    bucketName = object.optString("Bucket");
                    transferUtility = TransferUtility.builder()
                            .context(context)
                            .awsConfiguration(configuration)
                            .defaultBucket(bucketName)
                            .s3Client(new AmazonS3Client(AWSMobileClient.getInstance(), Region.getRegion(region)))
                            .build();
                } catch (Exception e) {

                }
            }

            @Override
            public void onError(Exception e) {
                Log.e(TAG, "Initialization error.", e);
            }
        });
    }

    public void upload(final String filePath, final FileUploadCallback callback) {
        if (transferUtility != null) {
            final String fileName = UUID.randomUUID().toString().replaceAll("-","");
            TransferObserver uploadObserver = transferUtility.upload("public/" + fileName, new File(filePath));
            uploadObserver.setTransferListener(new TransferListener() {

                @Override
                public void onStateChanged(int id, TransferState state) {
                    if (state == TransferState.COMPLETED) {
                        new File(filePath).delete();
                        callback.onComplete("https://" + bucketName + ".s3.amazonaws.com/public/" + fileName);
                    }
                }

                @Override
                public void onProgressChanged(int id, long bytesCurrent, long bytesTotal) {

                }

                @Override
                public void onError(int id, Exception ex) {
                    Log.e(TAG, "Error:" + ex.getMessage());
                }
            });
        }
    }

    public interface FileUploadCallback {
        void onComplete(String fileUrl);
    }

    public void download(final String dir, final String fileName, final FileDownloadCallback callback) {
        if (transferUtility != null) {
            TransferObserver downloadObserver = transferUtility.download("public/" + fileName, new File(dir + "/" + fileName));

            downloadObserver.setTransferListener(new TransferListener() {

                @Override
                public void onStateChanged(int id, TransferState state) {
                    if (state == TransferState.COMPLETED) {
                        callback.onComplete(fileName);
                    }
                }

                @Override
                public void onProgressChanged(int id, long bytesCurrent, long bytesTotal) {

                }

                @Override
                public void onError(int id, Exception ex) {
                    Log.e(TAG, "Error:" + ex.getMessage());
                }
            });
        }
    }

    public interface FileDownloadCallback {
        void onComplete(String fileUrl);
    }
}
