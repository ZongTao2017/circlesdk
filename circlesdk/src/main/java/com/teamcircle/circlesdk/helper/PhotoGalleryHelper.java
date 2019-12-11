package com.teamcircle.circlesdk.helper;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentActivity;
import androidx.loader.app.LoaderManager;
import androidx.loader.content.CursorLoader;
import androidx.loader.content.Loader;

import com.teamcircle.circlesdk.model.PhotoData;
import com.teamcircle.circlesdk.model.VideoData;

import java.util.ArrayList;
import java.util.Collections;

public class PhotoGalleryHelper implements LoaderManager.LoaderCallbacks<Cursor> {
    private static PhotoGalleryHelper instance;
    private Context context;
    private PhotoGalleryLoaderCallback callback;
    private int loaderId;

    public static PhotoGalleryHelper getInstance() {
        if (instance == null) {
            instance = new PhotoGalleryHelper();
        }
        return instance;
    }

    @NonNull
    @Override
    public Loader<Cursor> onCreateLoader(int i, @Nullable Bundle bundle) {
        final String photo_id = MediaStore.Images.Media._ID;
        final String path = MediaStore.Images.Media.DATA;
        final String photo_date = MediaStore.Images.Media.DATE_ADDED;
        final String orientation = MediaStore.Images.Media.ORIENTATION;
        Uri uri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
        String[] columns = {photo_id, path, photo_date, orientation};
        return new CursorLoader(this.context, uri, columns, null, null, null);
    }

    @Override
    public void onLoadFinished(@NonNull Loader<Cursor> loader, Cursor cursor) {
        ArrayList<PhotoData> photos = new ArrayList<>();
        if (cursor.getCount() > 0) {
            cursor.moveToFirst();
            do {
                int idColumn = cursor.getColumnIndex(MediaStore.Images.Media._ID);
                int pathColumn = cursor.getColumnIndex(MediaStore.Images.Media.DATA);
                int dateColumn = cursor.getColumnIndex(MediaStore.Images.Media.DATE_ADDED);
                int orientationColumn = cursor.getColumnIndex(MediaStore.Images.Media.ORIENTATION);
                long id = cursor.getLong(idColumn);
                String path = cursor.getString(pathColumn);
                int date = cursor.getInt(dateColumn);
                int orientation = cursor.getInt(orientationColumn);
                PhotoData photoData = new PhotoData(path, orientation);
                photoData.date = date;
                for (int i = 0; i < photos.size(); i++) {
                    PhotoData photoData1 = photos.get(i);
                    if (photoData.date > photoData1.date) {
                        photos.add(i, photoData);
                        break;
                    }
                }
                if (!photos.contains(photoData)) {
                    photos.add(photoData);
                }
            } while (cursor.moveToNext());
        }
        if (callback != null) {
            callback.photoGalleryLoaderDone(photos);
        }
        ((FragmentActivity) context).getSupportLoaderManager().destroyLoader(loaderId);
    }

    @Override
    public void onLoaderReset(@NonNull Loader<Cursor> loader) {

    }

    public interface PhotoGalleryLoaderCallback {
        void photoGalleryLoaderDone(ArrayList<PhotoData> photos);
    }

    public void startPhotoLoader(Context context, PhotoGalleryLoaderCallback callback) {
        this.context = context;
        this.callback = callback;
        this.loaderId = ++AppSocialGlobal.loaderId;
        ((FragmentActivity) context).getSupportLoaderManager().initLoader(loaderId, null, this);
    }
}
