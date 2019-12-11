package com.teamcircle.circlesdk.model;

import java.io.Serializable;
import java.util.ArrayList;

public class ProductData implements Serializable {
    public int productId;
    public String itemNumber;
    public String name;
    public ArrayList<String> photos;
    public int priceLow;
    public int priceHigh;
    public String installTime;
    public String link;
    public String description;
    public ArrayList<ProductVariantData> variants;
    public String feature;
    public ArrayList<CustomerPhotoData> customerPhotos;

    public ProductData(int productId, String name, ArrayList<String> photos, int priceLow, int priceHigh,
                       String installTime, String link, String description, ArrayList<ProductVariantData> variants, String feature) {
        this.productId = productId;
        this.name = name;
        this.photos = photos;
        this.priceLow = priceLow;
        this.priceHigh = priceHigh;
        this.installTime = installTime.replaceAll("\\s+", "");
        this.link = link;
        this.description = description;
        this.variants = variants;
        switch (feature) {
            case "NEW":
                this.feature = "NEW";
                break;
            case "BEST_SELLER":
                this.feature = "HOT";
                break;
        }
        this.customerPhotos = new ArrayList<>();
    }
}
