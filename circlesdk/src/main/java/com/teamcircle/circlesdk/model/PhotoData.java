package com.teamcircle.circlesdk.model;

import java.io.Serializable;
import java.util.ArrayList;

public class PhotoData implements Serializable {
    public String photoUrl;
    public int orientation;
    public ArrayList<TagData> tags;
    public int width;
    public int height;
    public int date;

    public PhotoData(String path) {
        this.photoUrl = path;
        this.tags = new ArrayList<>();
    }

    public PhotoData(String path, int width, int height) {
        this(path);
        this.width = width;
        this.height = height;
    }

    public PhotoData(String path, int orientation) {
        this(path);
        this.orientation = orientation;
    }

    public PhotoData(PhotoData photoData) {
        this.photoUrl = photoData.photoUrl;
        this.orientation = photoData.orientation;
        this.tags = new ArrayList<>();
        for (TagData tagData : photoData.tags) {
            this.tags.add(new TagData(tagData));
        }
        this.width = photoData.width;
        this.height = photoData.height;
    }
}