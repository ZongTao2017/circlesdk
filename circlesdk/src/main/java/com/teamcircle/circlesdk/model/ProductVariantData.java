package com.teamcircle.circlesdk.model;

import java.io.Serializable;

public class ProductVariantData implements Serializable {
    public int variantId;
    public String name;
    public int price;

    public ProductVariantData(int variantId, String name, int price) {
        this.variantId = variantId;
        this.name = name.replaceAll("\\[([^)]+)\\)", "")
                .replaceAll("\\(([^)]+)\\)", "")
                .replaceAll("\\[([^]]+)\\]", "");
        this.price = price;
    }
}
