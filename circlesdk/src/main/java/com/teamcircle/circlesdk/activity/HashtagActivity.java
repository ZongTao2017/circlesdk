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
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.flipboard.bottomsheet.BottomSheetLayout;
import com.teamcircle.circlesdk.R;
import com.teamcircle.circlesdk.helper.ApiHelper;
import com.teamcircle.circlesdk.helper.AppSocialGlobal;
import com.teamcircle.circlesdk.model.PostData;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.LinkedHashMap;

import de.hdodenhof.circleimageview.CircleImageView;

public class HashtagActivity extends Activity {
    private LinkedHashMap<Integer, PostData> mPosts;
    private PostViewAdapter mAdapter;
    private boolean haveMoreToLoad = true;
    private String hashtag;
    private CircleImageView mImageView;
    private TextView mPostNumberText;
    private LinearLayout mFollowButton;
    private TextView mFollowText;
    private ImageView mFollowImage;
    private BottomSheetLayout mBottomSheetLayout;
    private int mHashtagId;
    private boolean mIsFollowed;

    private static final int CELL_NUMBER_IN_ROW = 3;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_hashtag);

        hashtag = getIntent().getStringExtra("hashtag").toLowerCase();
        mPosts = new LinkedHashMap<>();

        ImageView backImage = findViewById(R.id.back_image);
        backImage.setImageResource(AppSocialGlobal.backResourceId);
        FrameLayout back = findViewById(R.id.back);
        back.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
        TextView textView = findViewById(R.id.tag_name);
        textView.setText("#" + hashtag);
        RecyclerView recyclerView = findViewById(R.id.recycler_view);
        recyclerView.setLayoutManager(new GridLayoutManager(this, CELL_NUMBER_IN_ROW, RecyclerView.VERTICAL, false));
        mAdapter = new PostViewAdapter();
        recyclerView.setAdapter(mAdapter);

        mImageView = findViewById(R.id.image_view);
        mPostNumberText = findViewById(R.id.post_number);
        mFollowButton = findViewById(R.id.follow_button);
        mFollowText = findViewById(R.id.follow_text);
        mFollowImage = findViewById(R.id.follow_img);
        mBottomSheetLayout = findViewById(R.id.bottomsheet);

        getPosts(hashtag);
    }

    private class PostViewAdapter extends RecyclerView.Adapter<PostViewHolder> {
        @NonNull
        @Override
        public PostViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            LayoutInflater layoutInflater = LayoutInflater.from(parent.getContext());
            View view = layoutInflater.inflate(R.layout.photo_gallery_item, parent, false);
            int width = AppSocialGlobal.getScreenWidth(HashtagActivity.this) / CELL_NUMBER_IN_ROW;
            view.setLayoutParams(new ViewGroup.LayoutParams(width, width));
            return new PostViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull PostViewHolder holder, int position) {
            final PostData postData = new ArrayList<>(mPosts.values()).get(position);
            String photoUrl;
            holder.mMultiImage.setVisibility(View.GONE);
            holder.mVideoTime.setVisibility(View.GONE);
            if (postData.photos.size() > 0) {
                photoUrl = postData.photos.get(0).photoUrl;
                if (postData.photos.size() > 1) {
                    holder.mMultiImage.setVisibility(View.VISIBLE);
                }
            } else {
                photoUrl = postData.video.photoUrl;
                holder.mVideoTime.setVisibility(View.VISIBLE);
                int minute = postData.video.duration / 1000 / 60;
                int second = postData.video.duration / 1000 % 60;
                String text = String.format("%d:%d", minute, second);
                if (second < 10) {
                    text = String.format("%d:0%d", minute, second);
                }
                holder.mVideoTime.setText(text);
            }

            AppSocialGlobal.loadImage(photoUrl, holder.mImageView);
            holder.mImageView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent intent = new Intent(HashtagActivity.this, PostActivity.class);
                    intent.putExtra("postId", postData.postId);
                    startActivity(intent);
                }
            });

            if (position == mPosts.size() - 1 && haveMoreToLoad) {
                getPostsNext(hashtag);
            }
        }

        @Override
        public int getItemCount() {
            return mPosts.size();
        }
    }

    private class PostViewHolder extends RecyclerView.ViewHolder {
        ImageView mImageView;
        TextView mVideoTime;
        ImageView mMultiImage;

        public PostViewHolder(View itemView) {
            super(itemView);
            mImageView = itemView.findViewById(R.id.image);
            mVideoTime = itemView.findViewById(R.id.video_time);
            mMultiImage = itemView.findViewById(R.id.multi);
        }
    }

    private void getPosts(final String hashtag) {
        ApiHelper.getHashtagPosts(hashtag, new ApiHelper.ApiCallback() {
            @Override
            public void onSuccess(JSONObject response) {
                Log.d("APIGetHashtagPosts", response.toString());
                mHashtagId = response.optJSONObject("resultData").optInt("hashtagId");
                int postNumber = response.optJSONObject("resultData").optInt("postNumber");
                mPostNumberText.setText(postNumber + " posts");
                mIsFollowed = response.optJSONObject("resultData").optBoolean("followStatus");
                if (mIsFollowed) {
                    mFollowButton.setBackground(getDrawable(R.drawable.radius_corner_stroke_white));
                    mFollowText.setText("Followed");
                    mFollowText.setTextColor(getResources().getColor(R.color.blackColor));
                    mFollowImage.setVisibility(View.VISIBLE);
                } else {
                    mFollowButton.setBackground(getDrawable(R.drawable.radius_corner_blue));
                    mFollowText.setTextColor(getResources().getColor(R.color.whiteColor));
                    mFollowText.setText("Follow");
                    mFollowImage.setVisibility(View.GONE);
                }
                mFollowButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (!mIsFollowed) {
                            mIsFollowed = true;
                            mFollowButton.setBackground(getDrawable(R.drawable.radius_corner_stroke_white));
                            mFollowText.setText("Followed");
                            mFollowText.setTextColor(getResources().getColor(R.color.blackColor));
                            mFollowImage.setVisibility(View.VISIBLE);
                            ApiHelper.followHashtag(mHashtagId, new ApiHelper.ApiCallback() {
                                @Override
                                public void onSuccess(JSONObject response) {
                                    Log.d("APIFollowHashtag", response.toString());
                                }

                                @Override
                                public void onFail(String errorMsg) {
                                    Log.e("APIFollowHashtag", errorMsg);
                                }
                            });
                        } else {
                            showFollowActions();
                        }
                    }
                });
                haveMoreToLoad = AppSocialGlobal.getInstance().updatePosts(response.optJSONObject("resultData").optJSONArray("posts"), mPosts);
                JSONObject photo = response.optJSONObject("resultData").optJSONArray("posts").optJSONObject(0).optJSONArray("photos").optJSONObject(0);
                String photoUrl = null;
                if (!photo.isNull("imageUrl")) {
                    photoUrl = photo.optString("imageUrl");
                } else if (!photo.isNull("videoUrl")) {
                    photoUrl = photo.optString("videoUrl").split("&photoUrl=")[1];
                }
                AppSocialGlobal.loadImage(photoUrl, mImageView);
                updatePosts();
            }

            @Override
            public void onFail(String errorMsg) {
                Log.e("APIGetHashtagPosts", errorMsg);
            }
        });
    }

    private void getPostsNext(String hashtag) {
        ApiHelper.getHashtagPostsNext(hashtag, new ApiHelper.ApiCallback() {
            @Override
            public void onSuccess(JSONObject response) {
                Log.d("APIGetHashtagPosts", response.toString());
                haveMoreToLoad = AppSocialGlobal.getInstance().updatePosts(response.optJSONArray("resultData"), mPosts);
                updatePosts();
            }

            @Override
            public void onFail(String errorMsg) {
                Log.d("APIGetHashtagPosts fail", errorMsg);
            }
        });
    }

    private void updatePosts() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mAdapter.notifyDataSetChanged();
            }
        });
    }

    private void showFollowActions() {
        View sheetView = LayoutInflater.from(this).inflate(R.layout.bottom_sheet_follow, mBottomSheetLayout, false);
        TextView unfollow = sheetView.findViewById(R.id.unfollow);
        unfollow.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mBottomSheetLayout.isSheetShowing()) {
                    mBottomSheetLayout.dismissSheet();
                    mFollowButton.setBackground(getDrawable(R.drawable.radius_corner_blue));
                    mFollowText.setText("Follow");
                    mFollowText.setTextColor(getResources().getColor(R.color.whiteColor));
                    mFollowImage.setVisibility(View.GONE);
                    mIsFollowed = false;
                    ApiHelper.followHashtag(mHashtagId, new ApiHelper.ApiCallback() {
                        @Override
                        public void onSuccess(JSONObject response) {
                            Log.d("APIFollowHashtag", response.toString());
                        }

                        @Override
                        public void onFail(String errorMsg) {
                            Log.e("APIFollowHashtag", errorMsg);
                        }
                    });
                }
            }
        });
        mBottomSheetLayout.showWithSheetView(sheetView);
    }
}
