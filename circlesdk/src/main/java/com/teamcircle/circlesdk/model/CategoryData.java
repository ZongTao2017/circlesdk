package com.teamcircle.circlesdk.model;

import java.util.ArrayList;

public class CategoryData {
    public int categoryId;
    public String categoryName;
    public ArrayList<ProductData> products;

    public CategoryData(int id, String name) {
        this.categoryId = id;
        this.categoryName = name;
        this.products = new ArrayList<>();
    }
}
