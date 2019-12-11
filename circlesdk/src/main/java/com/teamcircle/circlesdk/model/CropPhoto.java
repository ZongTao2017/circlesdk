package com.teamcircle.circlesdk.model;

import android.graphics.Bitmap;

public class CropPhoto {
    public String photoUrl;
    public Bitmap bitmap;
    public float scale;
    public float originalScale;
    public float offsetX, offsetY;
    public float originalOffsetX, originalOffsetY;
    public float startX, startY, width, height, bmWidth, bmHeight;

    public CropPhoto(Bitmap bitmap, float originalScale, float scale, float offsetX, float offsetY, float originalOffsetX, float originalOffsetY, float startX, float startY, float width, float height, float bmWidth, float bmHeight) {
        this.bitmap = bitmap;
        this.originalScale = originalScale;
        this.scale = scale;
        this.offsetX = offsetX;
        this.offsetY = offsetY;
        this.originalOffsetX = originalOffsetX;
        this.originalOffsetY = originalOffsetY;
        this.startX = startX;
        this.startY = startY;
        this.width = width;
        this.height = height;
        this.bmWidth = bmWidth;
        this.bmHeight = bmHeight;
    }
}
