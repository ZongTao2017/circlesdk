package com.teamcircle.circlesdk.helper;

import android.content.Context;
import android.database.Cursor;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentActivity;
import androidx.loader.app.LoaderManager;
import androidx.loader.content.CursorLoader;
import androidx.loader.content.Loader;

import com.teamcircle.circlesdk.model.VideoData;

import java.util.ArrayList;
import java.util.Collections;

public class VideoGalleryHelper implements LoaderManager.LoaderCallbacks<Cursor> {
    private static VideoGalleryHelper instance;
    private Context context;
    private VideoGalleryLoaderCallback callback;
    private int loaderId;

    public static VideoGalleryHelper getInstance() {
        if (instance == null) {
            instance = new VideoGalleryHelper();
        }
        return instance;
    }

    @NonNull
    @Override
    public Loader<Cursor> onCreateLoader(int i, @Nullable Bundle bundle) {
        final String video_id = MediaStore.Video.Media._ID;
        final String path = MediaStore.Video.Media.DATA;
        final String date = MediaStore.Video.Media.DATE_ADDED;
        final String duration = MediaStore.Video.Media.DURATION;
        final String resolution = MediaStore.Video.Media.RESOLUTION;
        Uri uri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
        String[] columns = {video_id, path, date, duration, resolution};
        return new CursorLoader(this.context, uri, columns, null, null, null);
    }

    @Override
    public void onLoadFinished(@NonNull Loader<Cursor> loader, Cursor cursor) {
        ArrayList<VideoData> videos = new ArrayList<>();
        MediaMetadataRetriever retriever = new MediaMetadataRetriever();
        if (cursor.getCount() > 0) {
            cursor.moveToFirst();
            do {
                int idColumn = cursor.getColumnIndex(MediaStore.Video.Media._ID);
                int pathColumn = cursor.getColumnIndex(MediaStore.Video.Media.DATA);
                int dateColumn = cursor.getColumnIndex(MediaStore.Video.Media.DATE_ADDED);
                int durationColumn = cursor.getColumnIndex(MediaStore.Video.Media.DURATION);
                int resolutionColumn = cursor.getColumnIndex(MediaStore.Video.Media.RESOLUTION);
                long id = cursor.getLong(idColumn);
                String path = cursor.getString(pathColumn);
                int date = cursor.getInt(dateColumn);
                int duration = cursor.getInt(durationColumn);
                String resolution = cursor.getString(resolutionColumn);
                try {
                    retriever.setDataSource(path);
                    String[] s = resolution.split("x");
                    int w, h;
                    String ss = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION);
                    if (ss.equals("270") || ss.equals("90")) {
                        w = Integer.parseInt(s[1]);
                        h = Integer.parseInt(s[0]);
                    } else {
                        w = Integer.parseInt(s[0]);
                        h = Integer.parseInt(s[1]);
                    }
                    VideoData videoData = new VideoData(path, duration, w, h);
                    videoData.date = date;
                    for (int i = 0; i < videos.size(); i++) {
                        VideoData videoData1 = videos.get(i);
                        if (videoData.date > videoData1.date) {
                            videos.add(i, videoData);
                            break;
                        }
                    }
                    if (!videos.contains(videoData)) {
                        videos.add(videoData);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            } while(cursor.moveToNext());
        }
        if (callback != null) {
            callback.videoGalleryLoaderDone(videos);
        }
        ((FragmentActivity) context).getSupportLoaderManager().destroyLoader(loaderId);
    }

    @Override
    public void onLoaderReset(@NonNull Loader<Cursor> loader) {

    }

    public interface VideoGalleryLoaderCallback {
        void videoGalleryLoaderDone(ArrayList<VideoData> videos);
    }

    public void startVideoLoader(Context context, VideoGalleryLoaderCallback callback) {
        this.context = context;
        this.callback = callback;
        this.loaderId = ++AppSocialGlobal.loaderId;
        ((FragmentActivity) context).getSupportLoaderManager().initLoader(loaderId, null, this);
    }
}
