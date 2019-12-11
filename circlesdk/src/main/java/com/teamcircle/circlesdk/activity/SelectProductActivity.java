package com.teamcircle.circlesdk.activity;

import android.app.Activity;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.tabs.TabLayout;
import com.squareup.picasso.Picasso;
import com.teamcircle.circlesdk.R;
import com.teamcircle.circlesdk.helper.ApiHelper;
import com.teamcircle.circlesdk.helper.AppSocialGlobal;
import com.teamcircle.circlesdk.model.CategoryData;
import com.teamcircle.circlesdk.model.MessageEvent;
import com.teamcircle.circlesdk.model.ProductData;
import com.teamcircle.circlesdk.model.ProductVariantData;
import com.teamcircle.circlesdk.model.TagData;

import org.greenrobot.eventbus.EventBus;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;

public class SelectProductActivity extends Activity {
    private ArrayList<ProductData> mProducts;
    private ArrayList<ProductData> mSearchProducts;
    private ProductGridAdapter mAdapter;
    private int mItemSize;
    private int mMargin;
    private CategoryData mCurrentCategory;
    private int mSelectedProductId = -1;
    private TabLayout mTabLayout;
    private RecyclerView mRecyclerView;
    private ListView mListView;
    private ProductListAdapter mListAdapter;
    private EditText mEditText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_select_product);

        int width = AppSocialGlobal.getScreenWidth(SelectProductActivity.this) / 3;
        mMargin = 50;
        mItemSize = width - mMargin * 2;

        ImageView backImage = findViewById(R.id.back_image);
        backImage.setImageResource(AppSocialGlobal.backResourceId);
        FrameLayout back = findViewById(R.id.back);
        back.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
        FrameLayout done = findViewById(R.id.done);
        done.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mSelectedProductId != -1) {
                    TagData tagData = AppSocialGlobal.getInstance().tmp_tag;
                    tagData.productId = mSelectedProductId;
                    tagData.productImage = AppSocialGlobal.getInstance().getProductById(mSelectedProductId).photos.get(0);
                    AppSocialGlobal.getInstance().tmp_tag = tagData;
                    EventBus.getDefault().post(new MessageEvent(MessageEvent.MessageEventType.ADD_TAG));
                }
                finish();
            }
        });

        mProducts = new ArrayList<>();
        mSearchProducts = new ArrayList<>();
        if (AppSocialGlobal.getInstance().allProducts.size() > 0) {
            CategoryData categoryData = AppSocialGlobal.getInstance().allProducts.entrySet().iterator().next().getValue();
            mProducts.addAll(categoryData.products);
            mCurrentCategory = categoryData;
        }

        mTabLayout = findViewById(R.id.tabs);
        mTabLayout.setTabMode(TabLayout.MODE_SCROLLABLE);
        for (int categoryId : AppSocialGlobal.getInstance().allProducts.keySet()) {
            final CategoryData categoryData = AppSocialGlobal.getInstance().allProducts.get(categoryId);
            if (categoryData.products.size() == 0) {
                continue;
            }
            TabLayout.Tab tab = mTabLayout.newTab();
            tab.setText(categoryData.categoryName);
            tab.setTag(categoryId);
            mTabLayout.addTab(tab);
            if (mProducts.size() == 0) {
                mProducts.addAll(categoryData.products);
                mCurrentCategory = categoryData;
            }
        }
        mTabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                mCurrentCategory = AppSocialGlobal.getInstance().allProducts.get(tab.getTag());
                mProducts = mCurrentCategory.products;
                mAdapter.notifyDataSetChanged();
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {

            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {

            }
        });

        mRecyclerView = findViewById(R.id.product_grid);
        mRecyclerView.setLayoutManager(new GridLayoutManager(this, 3, RecyclerView.VERTICAL, false));
        mAdapter = new ProductGridAdapter();
        mRecyclerView.setAdapter(mAdapter);

        mListView = findViewById(R.id.product_list);
        mListAdapter = new ProductListAdapter();
        mListView.setAdapter(mListAdapter);
        mListView.setVisibility(View.GONE);

        mEditText = findViewById(R.id.search_product);
        mEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                String keyword = mEditText.getText().toString();
                if (keyword.isEmpty()) {
                    mListView.setVisibility(View.GONE);
                    mRecyclerView.setVisibility(View.VISIBLE);
                    mTabLayout.setVisibility(View.VISIBLE);
                    mAdapter.notifyDataSetChanged();
                } else {
                    mListView.setVisibility(View.VISIBLE);
                    mRecyclerView.setVisibility(View.GONE);
                    mTabLayout.setVisibility(View.GONE);
                    ApiHelper.getProducts(0, keyword, new ApiHelper.ApiCallback() {
                        @Override
                        public void onSuccess(JSONObject response) {
                            mSearchProducts.clear();
                            JSONArray products = response.optJSONArray("resultData");
                            for (int j = 0; j < products.length(); j++) {
                                JSONObject product = products.optJSONObject(j);
                                int productId = product.optInt("productId");
                                String itemNumber = product.optString("productNo");
                                String productName = product.optString("productName");
                                JSONArray images = product.optJSONArray("images");
                                ArrayList<String> photos = new ArrayList<>();
                                for (int i = 0; i < images.length(); i++) {
                                    photos.add(images.optString(i));
                                }
                                int priceLow = product.optInt("defaultPrice");
                                int priceHigh = product.optInt("defaultPrice");
                                String installTime = product.optString("installTime");
                                String link = product.optString("websiteLink");
                                String description = product.optString("description");
                                String feature = product.optString("productStatus");
                                JSONArray variantArray = product.optJSONArray("variants");
                                ArrayList<ProductVariantData> variants = new ArrayList<>();
                                for (int k = 0; k < variantArray.length(); k++) {
                                    JSONObject variantObj = variantArray.optJSONObject(k);
                                    int variantId = variantObj.optInt("variantId");
                                    String variantName = variantObj.optString("variantName");
                                    int price = variantObj.optInt("unitPrice");
                                    ProductVariantData variantData = new ProductVariantData(variantId,
                                            variantName, price);
                                    variants.add(variantData);
                                }
                                ProductData productData = new ProductData(productId, productName, photos,
                                        priceLow, priceHigh, installTime, link, description, variants, feature);
                                productData.itemNumber = itemNumber;
                                mSearchProducts.add(productData);
                            }
                            mListAdapter.notifyDataSetChanged();
                        }

                        @Override
                        public void onFail(String errorMsg) {

                        }
                    });

                }
            }
        });
    }

    public class ProductGridAdapter extends RecyclerView.Adapter<ProductViewHolder> {

        @NonNull
        @Override
        public ProductViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            LayoutInflater layoutInflater = LayoutInflater.from(parent.getContext());
            View view = layoutInflater.inflate(R.layout.product_grid_item, parent, false);

            ImageView imageView = view.findViewById(R.id.image);
            LinearLayout.LayoutParams layoutParams = (LinearLayout.LayoutParams) imageView.getLayoutParams();

            layoutParams.width = mItemSize;
            layoutParams.height = mItemSize;
            layoutParams.setMargins(mMargin, mMargin, mMargin, 0);
            imageView.setLayoutParams(layoutParams);

            return new ProductViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ProductViewHolder holder, int position) {
            ProductData productData = mProducts.get(position);
            String name = productData.name;
            holder.nameText.setText(name);
            FrameLayout selected = holder.selected;
            if (mProducts.get(position).productId == mSelectedProductId) {
                selected.setVisibility(View.VISIBLE);
            } else {
                selected.setVisibility(View.GONE);
            }
            AppSocialGlobal.loadImage(productData.photos.get(0), holder.imageView);
        }

        @Override
        public int getItemCount() {
            return mProducts.size();
        }
    }

    public class ProductViewHolder extends RecyclerView.ViewHolder {
        ImageView imageView;
        TextView nameText;
        FrameLayout selected;

        public ProductViewHolder(View itemView) {
            super(itemView);
            imageView = itemView.findViewById(R.id.image);
            nameText = itemView.findViewById(R.id.name);
            selected = itemView.findViewById(R.id.selected);

            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    int position = getAdapterPosition();
                    mSelectedProductId = mProducts.get(position).productId;
                    mAdapter.notifyDataSetChanged();
                }
            });
        }
    }

    public class ProductListAdapter extends BaseAdapter {

        @Override
        public int getCount() {
            return mSearchProducts.size();
        }

        @Override
        public Object getItem(int position) {
            return null;
        }

        @Override
        public long getItemId(int position) {
            return 0;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            final ProductData productData = mSearchProducts.get(position);
            View view = convertView;
            if (view == null) {
                LayoutInflater layoutInflater = LayoutInflater.from(SelectProductActivity.this);
                view = layoutInflater.inflate(R.layout.tag_list_item, parent, false);
            }
            ImageView productImage = view.findViewById(R.id.product_image);
            String productImageUrl = productData.photos.get(0);
            AppSocialGlobal.loadImage(productImageUrl, productImage);
            TextView productName = view.findViewById(R.id.product_name);
            productName.setText(productData.name);
            TextView productPrice = view.findViewById(R.id.product_price);
            if (productData.priceLow == productData.priceHigh) {
                productPrice.setText(String.format("$%.2f", productData.priceLow / 100f));
            } else {
                productPrice.setText(String.format("$%.2f - $%.2f", productData.priceLow / 100f, productData.priceHigh / 100f));
            }
            FrameLayout selected = view.findViewById(R.id.selected);
            if (productData.productId == mSelectedProductId) {
                selected.setVisibility(View.VISIBLE);
            } else {
                selected.setVisibility(View.GONE);
            }
            view.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    mSelectedProductId = productData.productId;
                    mListAdapter.notifyDataSetChanged();
                }
            });
            return view;
        }
    }
}
