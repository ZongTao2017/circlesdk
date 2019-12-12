package com.teamcircle.circlesdk.view;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.teamcircle.circlesdk.R;
import com.teamcircle.circlesdk.activity.CustomerPhotoActivity;
import com.teamcircle.circlesdk.helper.AppSocialGlobal;
import com.teamcircle.circlesdk.model.CustomerPhotoData;
import com.teamcircle.circlesdk.model.ProductData;

import java.util.ArrayList;

public class CustomerPhotoGallery extends LinearLayout {
    private String mProductId;
    private ArrayList<CustomerPhotoData> mPhotos;
    private PhotoGalleryAdapter mAdapter;
    private RecyclerView mRecyclerView;

    public CustomerPhotoGallery(Context context) {
        super(context);
        init(context);
    }

    public CustomerPhotoGallery(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    private void init(Context context) {
        mRecyclerView = new RecyclerView(context);
        mRecyclerView.setLayoutParams(new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        LinearLayoutManager layoutManager= new LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false);
        mRecyclerView.setLayoutManager(layoutManager);
        addView(mRecyclerView);

        mPhotos = new ArrayList<>();
        mAdapter = new PhotoGalleryAdapter();
        mRecyclerView.setAdapter(mAdapter);
    }

    public void setProductId(String productId) {
        mProductId = productId;
        AppSocialGlobal.getInstance().updateProductById(productId, new AppSocialGlobal.ProductOnUpdateListener() {
            @Override
            public void onUpdate(ProductData productData) {
                if (productData != null) {
                    mRecyclerView.setLayoutParams(new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, AppSocialGlobal.dpToPx(getContext(), 150)));
                    mPhotos = productData.customerPhotos;
                    Activity activity = (Activity) getContext();
                    if (activity != null) {
                        activity.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                mAdapter.notifyDataSetChanged();
                            }
                        });
                    }
                }
            }
        });
    }

    public class PhotoGalleryAdapter extends RecyclerView.Adapter<PostViewHolder> {

        @NonNull
        @Override
        public PostViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            LayoutInflater layoutInflater = LayoutInflater.from(getContext());
            View view = layoutInflater.inflate(R.layout.photo, parent, false);
            int size = parent.getMeasuredHeight();
            RecyclerView.LayoutParams lp = (RecyclerView.LayoutParams) view.getLayoutParams();
            lp.width = size;
            lp.height = size;
            view.setLayoutParams(lp);
            return new PostViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull PostViewHolder holder, final int position) {
            final CustomerPhotoData photoData = mPhotos.get(position);
            AppSocialGlobal.loadImage(photoData.photoUrl, holder.imageView);
            holder.imageView.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent intent = new Intent(getContext(), CustomerPhotoActivity.class);
                    intent.putExtra("productId", mProductId);
                    intent.putExtra("position", position);
                    getContext().startActivity(intent);
                }
            });
        }

        @Override
        public int getItemCount() {
            return mPhotos.size();
        }
    }

    public class PostViewHolder extends RecyclerView.ViewHolder {
        ImageView imageView;

        public PostViewHolder(@NonNull View itemView) {
            super(itemView);
            imageView = itemView.findViewById(R.id.image_view);
        }
    }
}
