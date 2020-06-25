package com.teamcircle.circlesdk.activity;

import android.animation.Animator;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.text.Layout;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.airbnb.lottie.LottieAnimationView;
import com.flipboard.bottomsheet.BottomSheetLayout;
import com.omadahealth.github.swipyrefreshlayout.library.SwipyRefreshLayout;
import com.omadahealth.github.swipyrefreshlayout.library.SwipyRefreshLayoutDirection;
import com.teamcircle.circlesdk.R;
import com.teamcircle.circlesdk.helper.ApiHelper;
import com.teamcircle.circlesdk.helper.AppSocialGlobal;
import com.teamcircle.circlesdk.model.MessageEvent;
import com.teamcircle.circlesdk.model.PostData;
import com.teamcircle.circlesdk.model.UserData;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.LinkedHashMap;

public class AccountActivity extends Activity {
    private int mUserId;
    private UserData mUserData;
    private TextView mUsername;
    private RecyclerView mRecyclerView;
    private PostGridAdapter mGridAdapter;
    private ArrayList<PostData> mPosts;
    private LinkedHashMap<Integer, PostData> mPostMap;
    private boolean isShowingPosts = true;
    private FrameLayout mActions;
    private BottomSheetLayout mBottomSheetLayout;
    private ImageView mAddPostImage;
    boolean haveMoreToLoad = true;
    private SwipyRefreshLayout mRefreshLayout;
    private LottieAnimationView mAnimationView;

    private static final int CELL_NUMBER_IN_ROW = 3;
    private static final int REQ = 777;

    private static final int ITEM_VIEW_TYPE_HEADER = 0;
    private static final int ITEM_VIEW_TYPE_ITEM = 1;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EventBus.getDefault().register(this);
        setContentView(R.layout.activity_account);

        mUserId = getIntent().getIntExtra("userId", AppSocialGlobal.getInstance().me.userId);
        mUserData = AppSocialGlobal.getInstance().getUserById(mUserId);
        mUsername = findViewById(R.id.username);
        mUsername.setText(mUserData.username);
        mPosts = new ArrayList<>();
        mPostMap = new LinkedHashMap<>();

        getUserPosts();

        ImageView backImage = findViewById(R.id.back_image);
        backImage.setImageResource(AppSocialGlobal.backResourceId);
        FrameLayout back = findViewById(R.id.back);
        back.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        mRefreshLayout = findViewById(R.id.refresh_layout);
        mRefreshLayout.setOnRefreshListener(new SwipyRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh(SwipyRefreshLayoutDirection direction) {
                if (direction == SwipyRefreshLayoutDirection.TOP) {
                    getUserPosts();
                }
            }
        });

        mAddPostImage = findViewById(R.id.add_post);
        mAddPostImage.setImageResource(AppSocialGlobal.newPostResourceId);
        mAddPostImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AppSocialGlobal.getInstance().photoPickerType = 1;
                AppSocialGlobal.getInstance().newPostType = 1;
                startActivity(new Intent(AccountActivity.this, PhotoPickerActivity.class));
            }
        });

        mRecyclerView = findViewById(R.id.recycler_view);
        mGridAdapter = new PostGridAdapter();
        final GridLayoutManager layoutManager = new GridLayoutManager(AccountActivity.this, CELL_NUMBER_IN_ROW, RecyclerView.VERTICAL, false);
        layoutManager.setSpanSizeLookup(new GridLayoutManager.SpanSizeLookup() {
            @Override
            public int getSpanSize(int position) {
                return position == 0 ? CELL_NUMBER_IN_ROW : 1;
            }
        });
        mRecyclerView.setLayoutManager(layoutManager);
        mRecyclerView.setNestedScrollingEnabled(false);
        mRecyclerView.setEnabled(false);
        mRecyclerView.setVerticalScrollBarEnabled(false);

        mRecyclerView.setAdapter(mGridAdapter);
        mRecyclerView.getRecycledViewPool().setMaxRecycledViews(0, 8 * CELL_NUMBER_IN_ROW);

        mBottomSheetLayout = findViewById(R.id.bottomsheet);

        mAnimationView = findViewById(R.id.animation);
        mAnimationView.setVisibility(View.GONE);
    }

    @Override
    protected void onResume() {
        super.onResume();
        setupPosts();
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQ) {
            setupPosts();
        }
    }

    @Subscribe(sticky = true)
    public void onEvent(MessageEvent event) {
        switch (event.type) {
            case CHANGE_PROFILE:
                mUsername.setText(mUserData.username);
            case DONE_CHANGE_PROFILE_IMAGE:
            case FOLLOW:
            case UPDATE_POST:
                setupPosts();
                break;
            case DONE_SEND_POST_ME:
                showAnimation();
                break;
        }
    }

    private void setupPosts() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (isShowingPosts) {
                    mPosts = new ArrayList<PostData>(mUserData.posts.values());
                } else {
                    mPosts = new ArrayList<PostData>(mPostMap.values());
                }
                mGridAdapter.notifyDataSetChanged();
            }
        });
    }

    private class PostGridAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
        @NonNull
        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            LayoutInflater layoutInflater = LayoutInflater.from(AccountActivity.this);
            if (viewType == ITEM_VIEW_TYPE_HEADER) {
                View view = layoutInflater.inflate(R.layout.layout_account_top, parent, false);
                return new PostViewHeaderHolder(view);
            } else {
                View view = layoutInflater.inflate(R.layout.photo_gallery_item, parent, false);
                int width = AppSocialGlobal.getScreenWidth(AccountActivity.this) / CELL_NUMBER_IN_ROW;
                view.setLayoutParams(new ViewGroup.LayoutParams(width, width));
                return new PostViewHolder(view);
            }
        }

        @Override
        public void onBindViewHolder(@NonNull final RecyclerView.ViewHolder viewHolder, final int position) {
            if (position == 0) {
                final PostViewHeaderHolder holder = (PostViewHeaderHolder) viewHolder;
                AppSocialGlobal.loadImage(mUserData.photoUrl, holder.mProfileImage);
                holder.mProfileImage.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (mUserId == AppSocialGlobal.getInstance().me.userId) {
                            AppSocialGlobal.getInstance().photoPickerType = 0;
                            startActivity(new Intent(AccountActivity.this, PhotoPickerActivity.class));
                        }
                    }
                });
                holder.mBio.setVisibility(View.GONE);
                if (mUserData.bio != null) {
                    holder.mBio.setVisibility(View.VISIBLE);
                    holder.mBio.setText(mUserData.bio);
                    ViewTreeObserver viewTreeObserver = holder.mBio.getViewTreeObserver();
                    viewTreeObserver.addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener()
                    {
                        @Override
                        public void onGlobalLayout()
                        {
                            ViewTreeObserver viewTreeObserver = holder.mBio.getViewTreeObserver();
                            viewTreeObserver.removeOnGlobalLayoutListener(this);

                            holder.mBio.setTag(0);
                            Layout l = holder.mBio.getLayout();
                            if (l != null) {
                                int lines = l.getLineCount();
                                if (lines > 0)
                                    if (l.getEllipsisCount(lines - 1) > 0)
                                        holder.mBio.setTag(1);
                            }
                        }
                    });

                    holder.mBio.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            int n = (int) holder.mBio.getTag();
                            if (n == 1) {
                                holder.mBio.setTag(0);
                                holder.mBio.setMaxLines(Integer.MAX_VALUE);
                            }
                        }
                    });
                }
                holder.mFollowerNum.setText(String.format("%d", mUserData.followerNumber));
                holder.mFollowingNum.setText(String.format("%d", mUserData.followingNumber));
                holder.mPostNum.setText(String.format("%d", mUserData.postNumber));
                holder.mFirstPost.setVisibility(View.GONE);
                if (mUserId != AppSocialGlobal.getInstance().me.userId) {
                    holder.mPostStyle.setVisibility(View.GONE);
                } else {
                    if (isShowingPosts && mPosts.size() == 0) {
                        holder.mFirstPost.setVisibility(View.VISIBLE);
                        holder.mFirstPost.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                AppSocialGlobal.getInstance().photoPickerType = 1;
                                startActivity(new Intent(AccountActivity.this, PhotoPickerActivity.class));
                            }
                        });
                        holder.mPostStyle.setVisibility(View.GONE);
                    } else {
                        holder.mFirstPost.setVisibility(View.GONE);
                        holder.mPostStyle.setVisibility(View.VISIBLE);
                    }
                }
                if (isShowingPosts) {
                    holder.mPostStylePostsImage.setColorFilter(getResources().getColor(R.color.lightBlueColor));
                    holder.mPostStyleFavorImage.setColorFilter(getResources().getColor(R.color.darkGrayColor));
                } else {
                    holder.mPostStylePostsImage.setColorFilter(getResources().getColor(R.color.darkGrayColor));
                    holder.mPostStyleFavorImage.setColorFilter(getResources().getColor(R.color.lightBlueColor));
                }
                holder.mPostStylePosts.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        isShowingPosts = true;
                        holder.mPostStylePostsImage.setColorFilter(getResources().getColor(R.color.lightBlueColor));
                        holder.mPostStyleFavorImage.setColorFilter(getResources().getColor(R.color.darkGrayColor));
                        ApiHelper.getUserPosts(mUserId, new ApiHelper.ApiCallback() {
                            @Override
                            public void onSuccess(JSONObject response) {
                                Log.d("APIGetUserPosts success", response.toString());
                                haveMoreToLoad = true;
                                AppSocialGlobal.getInstance().getUserById(mUserId).posts.clear();
                                AppSocialGlobal.getInstance().updateUser(response.optJSONObject("resultData"), null);
                                setupPosts();
                                mRecyclerView.scrollToPosition(0);
                            }

                            @Override
                            public void onFail(String errorMsg) {
                                Log.e("APIGetUserPosts fail", errorMsg);
                            }
                        });
                    }
                });

                holder.mPostStyleFavor.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        isShowingPosts = false;
                        holder.mPostStylePostsImage.setColorFilter(getResources().getColor(R.color.darkGrayColor));
                        holder.mPostStyleFavorImage.setColorFilter(getResources().getColor(R.color.lightBlueColor));
                        ApiHelper.getFavors(new ApiHelper.ApiCallback() {
                            @Override
                            public void onSuccess(JSONObject response) {
                                Log.d("APIGetFavors success", response.toString());
                                haveMoreToLoad = true;
                                mPostMap.clear();
                                AppSocialGlobal.getInstance().updatePosts(response.optJSONArray("resultData"), mPostMap);
                                setupPosts();
                                mRecyclerView.scrollToPosition(0);
                            }

                            @Override
                            public void onFail(String errorMsg) {
                                Log.e("APIGetFavors fail", errorMsg);
                            }
                        });
                    }
                });
                holder.mFollowers.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Intent intent = new Intent(AccountActivity.this, FollowActivity.class);
                        intent.putExtra("userId", mUserData.userId);
                        intent.putExtra("follower", true);
                        startActivity(intent);
                    }
                });
                holder.mFollowing.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Intent intent = new Intent(AccountActivity.this, FollowActivity.class);
                        intent.putExtra("userId", mUserData.userId);
                        intent.putExtra("follower", false);
                        startActivity(intent);
                    }
                });
                if (mUserData.equals(AppSocialGlobal.getInstance().me)) {
                    holder.mFollowButton.setVisibility(View.GONE);
                } else {
                    if (mUserData.followed) {
                        holder.mFollowButton.setBackground(getDrawable(R.drawable.radius_corner_stroke_white));
                        holder.mFollowText.setText("Followed");
                        holder.mFollowText.setTextColor(getResources().getColor(R.color.blackColor));
                        holder.mFollowImage.setVisibility(View.VISIBLE);
                    } else {
                        holder.mFollowButton.setBackground(getDrawable(R.drawable.radius_corner_blue));
                        holder.mFollowText.setTextColor(getResources().getColor(R.color.whiteColor));
                        if (mUserData.followingMe) {
                            holder.mFollowText.setText("Follow back");
                        } else {
                            holder.mFollowText.setText("Follow");
                        }
                        holder.mFollowImage.setVisibility(View.GONE);
                    }
                }
                holder.mFollowButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (!AppSocialGlobal.getInstance().checkIfNeedSignIn(AccountActivity.this)) {
                            if (!mUserData.followed) {
                                holder.mFollowButton.setBackground(getDrawable(R.drawable.radius_corner_stroke_white));
                                holder.mFollowText.setTextColor(getResources().getColor(R.color.blackColor));
                                holder.mFollowText.setText("Followed");
                                holder.mFollowImage.setVisibility(View.VISIBLE);
                                AppSocialGlobal.getInstance().follow(mUserData);
                            } else {
                                showFollowActions();
                            }
                        }
                    }
                });

            } else {
                PostViewHolder holder = (PostViewHolder) viewHolder;
                final PostData postData = mPosts.get(position - 1);
                holder.mMulti.setVisibility(View.GONE);
                if (postData.photos.size() > 0) {
                    holder.mVideoTime.setVisibility(View.GONE);
                    String photoUrl = postData.photos.get(0).photoUrl;
                    AppSocialGlobal.loadImage(photoUrl, holder.mImageView);
                    if (postData.photos.size() > 1) {
                        holder.mMulti.setVisibility(View.VISIBLE);
                    }
                } else if (postData.video != null) {
                    holder.mVideoTime.setVisibility(View.VISIBLE);
                    int minute = postData.video.duration / 1000 / 60;
                    int second = postData.video.duration / 1000 % 60;
                    String text = String.format("%d:%d", minute, second);
                    if (second < 10) {
                        text = String.format("%d:0%d", minute, second);
                    }
                    holder.mVideoTime.setText(text);
                    AppSocialGlobal.loadImage(postData.video.photoUrl, holder.mImageView);
                } else {
                    holder.mVideoTime.setVisibility(View.GONE);
                    holder.mImageView.setImageResource(R.drawable.error);
                }
                holder.mImageView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (isShowingPosts) {
                            Intent intent = new Intent(AccountActivity.this, UserPostsActivity.class);
                            intent.putExtra("userId", mUserId);
                            intent.putExtra("position", position - 1);
                            startActivityForResult(intent, REQ);
                        } else {
                            Intent intent = new Intent(AccountActivity.this, PostActivity.class);
                            intent.putExtra("postId", postData.postId);
                            startActivity(intent);
                        }
                    }
                });
                if (isShowingPosts) {
                    if (position == mPosts.size() && haveMoreToLoad) {
                        ApiHelper.getUserPostsNext(mUserId, new ApiHelper.ApiCallback() {
                            @Override
                            public void onSuccess(JSONObject response) {
                                Log.d("APIGetUserPostsNext", response.toString());
                                AppSocialGlobal.getInstance().updateUser(response.optJSONObject("resultData"), null);
                                if (mUserData.posts.size() == mUserData.postNumber) {
                                    haveMoreToLoad = false;
                                }
                                setupPosts();
                            }

                            @Override
                            public void onFail(String errorMsg) {
                                Log.e("APIGetUserPostsNext", errorMsg);
                            }
                        });
                    }
                }
                else {
                    if (position == mPostMap.size() && haveMoreToLoad) {
                        ApiHelper.getFavorsNext(new ApiHelper.ApiCallback() {
                            @Override
                            public void onSuccess(JSONObject response) {
                                Log.d("APIGetFavorsNext", response.toString());
                                haveMoreToLoad = AppSocialGlobal.getInstance().updatePosts(response.optJSONArray("resultData"), mPostMap);
                                setupPosts();
                            }

                            @Override
                            public void onFail(String errorMsg) {
                                Log.e("APIGetFavorsNext", errorMsg);
                            }
                        });
                    }
                }
            }

        }

        @Override
        public int getItemViewType(int position) {
            return position == 0 ? ITEM_VIEW_TYPE_HEADER : ITEM_VIEW_TYPE_ITEM;
        }

        @Override
        public int getItemCount() {
            return mPosts.size() + 1;
        }
    }

    private class PostViewHolder extends RecyclerView.ViewHolder {
        ImageView mImageView;
        TextView mVideoTime;
        ImageView mMulti;

        public PostViewHolder(View itemView) {
            super(itemView);
            mImageView = itemView.findViewById(R.id.image);
            mVideoTime = itemView.findViewById(R.id.video_time);
            mMulti = itemView.findViewById(R.id.multi);
        }
    }

    private class PostViewHeaderHolder extends RecyclerView.ViewHolder {
        ImageView mProfileImage;
        TextView mBio;
        TextView mPostNum;
        TextView mFollowerNum;
        TextView mFollowingNum;
        LinearLayout mFirstPost;
        LinearLayout mPostStyle;
        LinearLayout mFollowButton;
        TextView mFollowText;
        ImageView mFollowImage;
        FrameLayout mPostStylePosts;
        ImageView mPostStylePostsImage;
        FrameLayout mPostStyleFavor;
        ImageView mPostStyleFavorImage;
        LinearLayout mFollowers;
        LinearLayout mFollowing;

        public PostViewHeaderHolder(View itemView) {
            super(itemView);
            mProfileImage = itemView.findViewById(R.id.profile_image);
            mBio = itemView.findViewById(R.id.bio);
            mPostNum = itemView.findViewById(R.id.post_number);
            mFollowerNum = itemView.findViewById(R.id.follower_num);
            mFollowingNum = itemView.findViewById(R.id.following_num);
            mFirstPost = itemView.findViewById(R.id.first_post);
            mPostStyle = itemView.findViewById(R.id.post_style);
            mFollowButton = itemView.findViewById(R.id.follow_button);
            mFollowText = itemView.findViewById(R.id.follow_text);
            mFollowImage = itemView.findViewById(R.id.follow_img);
            mPostStylePosts = itemView.findViewById(R.id.type_posts);
            mPostStylePostsImage = itemView.findViewById(R.id.type_posts_image);
            mPostStyleFavor = itemView.findViewById(R.id.type_favor);
            mPostStyleFavorImage = itemView.findViewById(R.id.type_favor_image);
            mFollowers = itemView.findViewById(R.id.followers_layout);
            mFollowing = itemView.findViewById(R.id.following_layout);
        }
    }

    private void showFollowActions() {
        View sheetView = LayoutInflater.from(this).inflate(R.layout.bottom_sheet_follow, mBottomSheetLayout, false);
        TextView unfollow = sheetView.findViewById(R.id.unfollow);
        unfollow.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mBottomSheetLayout.isSheetShowing()) {
                    mBottomSheetLayout.dismissSheet();
                    AppSocialGlobal.getInstance().follow(mUserData);
                    mGridAdapter.notifyDataSetChanged();
                }
            }
        });
        mBottomSheetLayout.showWithSheetView(sheetView);
    }

    private void getUserPosts() {
        haveMoreToLoad = true;
        ApiHelper.getUserPosts(mUserId, new ApiHelper.ApiCallback() {
            @Override
            public void onSuccess(JSONObject response) {
                Log.d("APIGetUserPosts success", response.toString());
                AppSocialGlobal.getInstance().getUserById(mUserId).posts.clear();
                AppSocialGlobal.getInstance().updateUser(response.optJSONObject("resultData"), null);
                setupPosts();
                mRefreshLayout.setRefreshing(false);
            }

            @Override
            public void onFail(String errorMsg) {
                Log.e("APIGetUserPosts fail", errorMsg);
                mRefreshLayout.setRefreshing(false);
            }
        });
    }

    private void showAnimation() {
        mAnimationView.setVisibility(View.VISIBLE);
        mAnimationView.setSpeed(2f);
        mAnimationView.playAnimation();
        mAnimationView.addAnimatorListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animation) {

            }

            @Override
            public void onAnimationEnd(Animator animation) {
                mAnimationView.setVisibility(View.GONE);
            }

            @Override
            public void onAnimationCancel(Animator animation) {

            }

            @Override
            public void onAnimationRepeat(Animator animation) {

            }
        });
    }
}
