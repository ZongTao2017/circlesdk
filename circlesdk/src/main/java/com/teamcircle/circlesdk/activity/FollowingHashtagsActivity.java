package com.teamcircle.circlesdk.activity;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.teamcircle.circlesdk.R;
import com.teamcircle.circlesdk.helper.ApiHelper;
import com.teamcircle.circlesdk.helper.AppSocialGlobal;
import com.teamcircle.circlesdk.model.HashtagData;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;

import de.hdodenhof.circleimageview.CircleImageView;

public class FollowingHashtagsActivity extends Activity {
    private ArrayList<HashtagData> mHashtags;
    private RecyclerView mRecyclerView;
    private HashtagAdapter mAdapter;
    private int mPageNumber = 0;
    private int mUserId;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_following_hashtags);

        mUserId = getIntent().getIntExtra("userId", AppSocialGlobal.getInstance().me.userId);
        mHashtags = new ArrayList<>();
        LinearLayoutManager layoutManager = new LinearLayoutManager(FollowingHashtagsActivity.this);
        mRecyclerView = findViewById(R.id.recycler_view);
        mRecyclerView.setLayoutManager(layoutManager);
        mAdapter = new HashtagAdapter();
        mRecyclerView.setAdapter(mAdapter);

        ImageView backImage = findViewById(R.id.back_image);
        backImage.setImageResource(AppSocialGlobal.backResourceId);
        FrameLayout back = findViewById(R.id.back);
        back.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        getHashtags();
    }

    public class HashtagViewHolder extends RecyclerView.ViewHolder {
        View view;
        CircleImageView imageView;
        TextView nameTextView;
        TextView numberTextView;

        public HashtagViewHolder(@NonNull View itemView) {
            super(itemView);
            view = itemView;
            imageView = itemView.findViewById(R.id.image);
            nameTextView = itemView.findViewById(R.id.name);
            numberTextView = itemView.findViewById(R.id.number);
        }
    }

    public class HashtagAdapter extends RecyclerView.Adapter<HashtagViewHolder> {

        @NonNull
        @Override
        public HashtagViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            LayoutInflater layoutInflater = LayoutInflater.from(parent.getContext());
            View view = layoutInflater.inflate(R.layout.search_row, parent, false);
            return new HashtagViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull HashtagViewHolder holder, int position) {
            final HashtagData hashtagData = mHashtags.get(position);
            AppSocialGlobal.loadImage(hashtagData.photoUrl, holder.imageView);
            holder.nameTextView.setText("#" + hashtagData.hashtag);
            holder.numberTextView.setText(hashtagData.number + " posts");
            holder.view.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent intent = new Intent(FollowingHashtagsActivity.this, HashtagActivity.class);
                    intent.putExtra("hashtag", hashtagData.hashtag);
                    startActivity(intent);
                }
            });
            if (position == 20 * mPageNumber - 1) {
                getHashtags();
            }
        }

        @Override
        public int getItemCount() {
            return mHashtags.size();
        }
    }

    private void getHashtags() {
        ApiHelper.getFollowingHashtags(++mPageNumber, mUserId, new ApiHelper.ApiCallback() {
            @Override
            public void onSuccess(JSONObject response) {
                JSONArray jsonArray = response.optJSONArray("resultData");
                for (int i = 0; i < jsonArray.length(); i++) {
                    final String hashtag = jsonArray.optJSONObject(i).optString("hashtag");
                    ApiHelper.getHashtagPosts(hashtag, new ApiHelper.ApiCallback() {
                        @Override
                        public void onSuccess(JSONObject response) {
                            Log.d("APIGetHashtagPosts", response.toString());
                            int hashtagId = response.optJSONObject("resultData").optInt("hashtagId");
                            int postNumber = response.optJSONObject("resultData").optInt("postNumber");
                            JSONObject photo = response.optJSONObject("resultData").optJSONArray("posts").optJSONObject(0).optJSONArray("photos").optJSONObject(0);
                            String photoUrl = null;
                            if (!photo.isNull("imageUrl")) {
                                photoUrl = photo.optString("imageUrl");
                            } else if (!photo.isNull("videoUrl")) {
                                photoUrl = photo.optString("videoUrl").split("&photoUrl=")[1];
                            }
                            HashtagData hashtagData = new HashtagData(hashtagId, hashtag, postNumber, photoUrl);
                            mHashtags.add(hashtagData);
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    mAdapter.notifyDataSetChanged();
                                }
                            });
                        }

                        @Override
                        public void onFail(String errorMsg) {
                            Log.e("APIGetHashtagPosts", errorMsg);
                        }
                    });
                }
            }

            @Override
            public void onFail(String errorMsg) {

            }
        });
    }
}
