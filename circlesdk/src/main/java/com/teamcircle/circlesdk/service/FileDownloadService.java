package com.teamcircle.circlesdk.service;

import android.content.Context;
import android.content.Intent;
import android.os.Environment;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.JobIntentService;

import com.teamcircle.circlesdk.helper.AmazonS3Helper;
import com.teamcircle.circlesdk.helper.AppSocialGlobal;
import com.teamcircle.circlesdk.model.MessageEvent;

import org.greenrobot.eventbus.EventBus;

import java.io.File;

public class FileDownloadService extends JobIntentService {
    public static final int JOB_ID = 1;

    public static void enqueueWork(Context context, Intent work) {
        enqueueWork(context, FileDownloadService.class, JOB_ID, work);
    }

    @Override
    protected void onHandleWork(@NonNull Intent intent) {
        final String filePath = intent.getStringExtra("url");
        if (filePath != null && filePath.startsWith("http")) {
            final String dir2 = getApplicationContext().getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS).getAbsolutePath();
            String dir = intent.getStringExtra("dir");
            if (dir == null) {
                dir = dir2;
            }
            final String fileName = filePath.split("public/")[1];
            final String outputFilePath = dir + "/" + fileName;
            File output = new File(dir, fileName);
            if (!output.exists()) {
                Log.d("S3 Download Start", fileName);
                AmazonS3Helper.getInstance().download(dir, fileName, new AmazonS3Helper.FileDownloadCallback() {
                    @Override
                    public void onComplete(String fileUrl) {
                        Log.d("S3 Download Complete", fileUrl);
                        Log.d("FileUrl Update", fileName);
                        AppSocialGlobal.getInstance().downloadList.remove(filePath);
                        AppSocialGlobal.getInstance().updateFileUrl(filePath, outputFilePath);
                        EventBus.getDefault().post(new MessageEvent(MessageEvent.MessageEventType.DONE_DOWNLOAD));
                    }
                });
            } else {
                Log.d("FileUrl Update", fileName);
                AppSocialGlobal.getInstance().downloadList.remove(filePath);
                AppSocialGlobal.getInstance().updateFileUrl(filePath, outputFilePath);
                EventBus.getDefault().post(new MessageEvent(MessageEvent.MessageEventType.DONE_DOWNLOAD));
            }
        } else {
            AppSocialGlobal.getInstance().downloadList.remove(filePath);
        }
    }
}
