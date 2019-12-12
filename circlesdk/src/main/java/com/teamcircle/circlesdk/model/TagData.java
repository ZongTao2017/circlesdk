package com.teamcircle.circlesdk.model;

import androidx.annotation.Nullable;

import java.io.Serializable;

public class TagData implements Serializable {
    public float percentX;
    public float percentY;
    public int productId;
    public String productImage;
    public String productItemNumber;
    public String productName;
    public String productCategory;
    public int price;

    public TagData() {

    }

    public TagData(float percentX, float percentY) {
        this.percentX = percentX;
        this.percentY = percentY;
    }

    public TagData(String productItemNumber, String productCategory, String productName, String productImage, int price) {
        this.productId = 0;
        this.productImage = productImage;
        this.productCategory = productCategory;
        this.productItemNumber = productItemNumber;
        this.productName = productName;
        this.price = price;
        this.percentX = 0;
        this.percentY = 0;
    }

    public TagData(int productId, String productImage, float percentX, float percentY) {
        this.productId = productId;
        this.productImage = productImage;
        this.percentX = percentX;
        this.percentY = percentY;
    }

    public TagData(TagData tagData) {
        this.percentX = tagData.percentX;
        this.percentY = tagData.percentY;
        this.productId = tagData.productId;
        this.productImage = tagData.productImage;
    }

    @Override
    public boolean equals(@Nullable Object obj) {
        if (obj instanceof TagData) {
            return this.productId == ((TagData) obj).productId;
        }
        return false;
    }
}
