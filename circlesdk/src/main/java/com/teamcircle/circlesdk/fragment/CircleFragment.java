package com.teamcircle.circlesdk.fragment;

import android.animation.Animator;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager.widget.ViewPager;

import com.airbnb.lottie.LottieAnimationView;
import com.omadahealth.github.swipyrefreshlayout.library.SwipyRefreshLayout;
import com.omadahealth.github.swipyrefreshlayout.library.SwipyRefreshLayoutDirection;
import com.pedromassango.doubleclick.DoubleClick;
import com.pedromassango.doubleclick.DoubleClickListener;
import com.teamcircle.circlesdk.R;
import com.teamcircle.circlesdk.activity.AccountActivity;
import com.teamcircle.circlesdk.activity.CommentActivity;
import com.teamcircle.circlesdk.activity.PhotoPickerActivity;
import com.teamcircle.circlesdk.activity.SearchActivity;
import com.teamcircle.circlesdk.helper.ApiHelper;
import com.teamcircle.circlesdk.helper.AppSocialGlobal;
import com.teamcircle.circlesdk.helper.PhotoPagerAdapter;
import com.teamcircle.circlesdk.model.CommentData;
import com.teamcircle.circlesdk.model.MessageEvent;
import com.teamcircle.circlesdk.model.PhotoData;
import com.teamcircle.circlesdk.model.PostData;
import com.teamcircle.circlesdk.model.TagData;
import com.teamcircle.circlesdk.text.CustomEditText;
import com.teamcircle.circlesdk.view.ListVideoView;
import com.teamcircle.circlesdk.view.ViewPagerIndicator;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;

public class CircleFragment extends Fragment {
    private SwipyRefreshLayout mRefreshLayout;
    private RecyclerView mRecyclerView;
    private StreamPostAdapter mAdapter;
    private LinearLayoutManager mLayoutManager;
    private ArrayList<PostData> mPosts;
    private LinearLayout mCommentLayout;
    private boolean mIsReadyToPost = false;
    private ImageView mAddPostImage;
    private ImageView mCommentProfile;
    private int mCurrentPlayingVideoViewIndex = -1;
    private CustomEditText mAddCommentText;
    private TextView mPostButtonText;
    private int mAddCommentPostId;
    private FrameLayout search, me;
    private boolean haveMoreToLoad = true;
    private LottieAnimationView mAnimationView;
    private Button mUpdatePostBtn;
    private boolean mIsShowingBtn = false;

    private int previousFirstVisibleItem = -1;
    private long previousEventTime = 0;
    private double speed = 0;
    private boolean isFling = false;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EventBus.getDefault().register(this);
    }

    @Override
    public void onDestroy() {
        EventBus.getDefault().unregister(this);
        super.onDestroy();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.circle, container, false);

        mPosts = new ArrayList<PostData>(AppSocialGlobal.getInstance().mainPosts.values());
        mAdapter = new StreamPostAdapter();
        mLayoutManager = new LinearLayoutManager(getContext());
        mRecyclerView = view.findViewById(R.id.recycler_view);
        mRecyclerView.setLayoutManager(mLayoutManager);
        mRecyclerView.setAdapter(mAdapter);
        mRecyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(@NonNull RecyclerView recyclerView, int newState) {
                if (newState == RecyclerView.SCROLL_STATE_SETTLING) {
                    isFling = true;
                } else {
                    isFling = false;
                }
            }

            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                int firstVisibleItem = mLayoutManager.findFirstVisibleItemPosition();
                int lastVisibleItem = mLayoutManager.findLastVisibleItemPosition();
                int height = mRecyclerView.getHeight();
                int videoIndex = -1;
                float percentMax = 0;
                ListVideoView videoView = null;
                HashMap<String, String> map = new HashMap<>();
                for (int i = firstVisibleItem; i <= lastVisibleItem; i++) {
                    View cellView = mLayoutManager.findViewByPosition(i);
                    if (cellView != null) {
                        int scrollY = cellView.getTop();
                        videoView = cellView.findViewById(R.id.video_view);
                        FrameLayout photoLayout = cellView.findViewById(R.id.photos_view_layout);
                        int contentHeight = 0;
                        if (videoView != null && videoView.getVideoPath() != null) {
                            contentHeight = videoView.getHeight();
                        }
                        if (photoLayout != null && videoView != null && videoView.getVideoPath() == null) {
                            contentHeight = photoLayout.getHeight();
                        }
                        if (contentHeight > 0) {
                            int top = Math.max(0, scrollY + AppSocialGlobal.dpToPx(getContext(), 65));
                            int bottom = Math.min(height, scrollY + AppSocialGlobal.dpToPx(getContext(), 65) + contentHeight);
                            float percent = (float) (bottom - top) / contentHeight;
                            if (videoView != null && videoView.getVideoPath() != null) {
                                if (percent > percentMax) {
                                    videoIndex = i;
                                    percentMax = percent;
                                }
                            }
                        }
                    }
                }
                if (previousFirstVisibleItem != firstVisibleItem){
                    long currTime = System.currentTimeMillis();
                    long timeToScrollOneElement = currTime - previousEventTime;
                    speed = ((double) 1 / timeToScrollOneElement) * 1000;

                    previousFirstVisibleItem = firstVisibleItem;
                    previousEventTime = currTime;
                }
                if (videoIndex != -1) {
                    if (videoIndex != mCurrentPlayingVideoViewIndex) {
                        if (!isFling || speed < 4) {
                            playVideo(videoIndex);
                        }
                    }
                }
                if (videoIndex == -1 && mCurrentPlayingVideoViewIndex != -1) {
                    stopAnyVideo();
                    mCurrentPlayingVideoViewIndex = -1;
                }
                if (dy > 0 && mIsShowingBtn) {
                    mIsShowingBtn = false;
                    mUpdatePostBtn.setVisibility(View.GONE);
                }
            }
        });

        mCommentLayout = view.findViewById(R.id.comment_layout);
        mCommentLayout.setVisibility(View.GONE);
        mCommentProfile = mCommentLayout.findViewById(R.id.comment_profile_image);
        AppSocialGlobal.loadImage(AppSocialGlobal.getInstance().me.photoUrl, mCommentProfile);
        mAddCommentText = mCommentLayout.findViewById(R.id.comment_edit_text);
        mPostButtonText = mCommentLayout.findViewById(R.id.comment_post_button_text);
        mAddCommentText.setCallback(new CustomEditText.Callback() {
            @Override
            public void onBackPressed() {
                endEdit(mAddCommentText, mPostButtonText);
            }
        });
        mAddCommentText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                if (mAddCommentText.getText().toString().isEmpty()) {
                    mPostButtonText.setTextColor(getResources().getColor(R.color.whiteLightBlueColor));
                    mIsReadyToPost = false;
                } else {
                    mPostButtonText.setTextColor(getResources().getColor(R.color.lightBlueColor));
                    mIsReadyToPost = true;
                }
                if (mAddCommentText.getLayout() != null) {
                    if (mAddCommentText.getLayout().getLineCount() > 10)
                        mAddCommentText.getText().delete(mAddCommentText.getText().length() - 1, mAddCommentText.getText().length());
                }
            }
        });
        FrameLayout postButton = mCommentLayout.findViewById(R.id.comment_post_button);
        postButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mIsReadyToPost) {
                    String content = mAddCommentText.getText().toString();
                    AppSocialGlobal.getInstance().commentPost(mAddCommentPostId, content);
                    endEdit(mAddCommentText, mPostButtonText);
                }
            }
        });
        View view1 = mCommentLayout.findViewById(R.id.click_to_quit_edit);
        view1.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getActionMasked() == MotionEvent.ACTION_DOWN) {
                    endEdit(mAddCommentText, mPostButtonText);
                    return true;
                }
                return false;
            }
        });

        mRefreshLayout = view.findViewById(R.id.refresh_layout);
        mRefreshLayout.setOnRefreshListener(new SwipyRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh(SwipyRefreshLayoutDirection direction) {
                if (direction == SwipyRefreshLayoutDirection.TOP) {
                    ApiHelper.getAllPosts(new ApiHelper.ApiCallback() {
                        @Override
                        public void onSuccess(JSONObject response) {
                            Log.d("APIGetAllPosts success", response.toString());
                            AppSocialGlobal.getInstance().mainPosts.clear();
                            AppSocialGlobal.getInstance().updatePosts(response.optJSONArray("resultData"), AppSocialGlobal.getInstance().mainPosts);
                            setupPosts();
                        }

                        @Override
                        public void onFail(String errorMsg) {
                            Log.e("APIGetAllPosts fail", errorMsg);
                            mRefreshLayout.setRefreshing(false);
                        }
                    });
                    haveMoreToLoad = true;
                }
            }
        });

        setupPosts();

        search = view.findViewById(R.id.search);
        search.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(getContext(), SearchActivity.class));
            }
        });

        mAddPostImage = view.findViewById(R.id.add_post);
        mAddPostImage.setImageResource(AppSocialGlobal.newPostResourceId);
        mAddPostImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!AppSocialGlobal.getInstance().checkIfNeedSignIn(getContext())) {
                    AppSocialGlobal.getInstance().photoPickerType = 1;
                    AppSocialGlobal.getInstance().newPostType = 0;
                    startActivity(new Intent(getContext(), PhotoPickerActivity.class));
                }
            }
        });

        me = view.findViewById(R.id.me);
        me.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!AppSocialGlobal.getInstance().checkIfNeedSignIn(getContext())) {
                    startActivity(new Intent(getContext(), AccountActivity.class));
                }
            }
        });

        mAnimationView = view.findViewById(R.id.animation);
        mAnimationView.setVisibility(View.GONE);
        mUpdatePostBtn = view.findViewById(R.id.update_posts_button);
        mUpdatePostBtn.setVisibility(View.GONE);
        mUpdatePostBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ApiHelper.getAllPosts(new ApiHelper.ApiCallback() {
                    @Override
                    public void onSuccess(JSONObject response) {
                        Log.d("APIGetAllPosts success", response.toString());
                        AppSocialGlobal.getInstance().mainPosts.clear();
                        AppSocialGlobal.getInstance().updatePosts(response.optJSONArray("resultData"), AppSocialGlobal.getInstance().mainPosts);
                        setupPosts();
                        mUpdatePostBtn.setVisibility(View.GONE);
                        mIsShowingBtn = false;
                    }

                    @Override
                    public void onFail(String errorMsg) {
                        Log.e("APIGetAllPosts fail", errorMsg);
                    }
                });
                haveMoreToLoad = true;
            }
        });

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        playVideo(mCurrentPlayingVideoViewIndex);
    }

    @Override
    public void onPause() {
        stopAnyVideo();
        super.onPause();
    }

    @Override
    public void onHiddenChanged(boolean hidden) {
        super.onHiddenChanged(hidden);
        if (hidden) {
            stopAnyVideo();
        } else {
            playVideo(mCurrentPlayingVideoViewIndex);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
    }

    @Subscribe(sticky = true)
    public void onEvent(MessageEvent event) {
        switch (event.type) {
            case SIGN_OUT:
            case FINISH_AUTH:
                getAllPosts();
                break;
            case FOLLOW:
            case UPDATE_POST:
            case DONE_CHANGE_PROFILE_IMAGE:
            case DONE_DOWNLOAD:
                setupPosts();
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        AppSocialGlobal.loadImage(AppSocialGlobal.getInstance().me.photoUrl, mCommentProfile);
                    }
                });
                break;
            case DONE_SEND_POST_MAIN:
                showAnimation();
                break;
        }
    }

    public void showTopIcons(boolean show) {
        if (search != null && me != null) {
            if (show) {
                search.setVisibility(View.VISIBLE);
                me.setVisibility(View.VISIBLE);
            } else {
                search.setVisibility(View.GONE);
                me.setVisibility(View.GONE);
            }
        }
    }

    private void scrollTop() {
        mRecyclerView.scrollToPosition(0);
    }

    public class PostRowHolder extends RecyclerView.ViewHolder {
        ImageView profileImage, addCommentProfile, likeImage, tagImage, favImage, commentImage;
        FrameLayout photosLayout, like, fav, tag, comment;
        LinearLayout tagsLayout, myCommentLayout, addComment, commentView;
        ListVideoView videoView;
        TextView likeNumberText, followButton, usernameText, captionText, viewAllComments;
        ViewPager viewPager;
        ViewPagerIndicator viewPagerIndicator;

        PostRowHolder(@NonNull View itemView) {
            super(itemView);
            profileImage = itemView.findViewById(R.id.profile_image);
            addCommentProfile = itemView.findViewById(R.id.add_comment_profile);
            likeImage = itemView.findViewById(R.id.post_like_image);
            tagImage = itemView.findViewById(R.id.post_tag_image);
            favImage = itemView.findViewById(R.id.post_fav_image);
            commentImage = itemView.findViewById(R.id.post_comment_image);
            photosLayout = itemView.findViewById(R.id.photos_view_layout);
            like = itemView.findViewById(R.id.post_like);
            fav = itemView.findViewById(R.id.post_fav);
            tag = itemView.findViewById(R.id.post_tag);
            comment = itemView.findViewById(R.id.post_comment);
            tagsLayout = itemView.findViewById(R.id.tags_layout);
            myCommentLayout = itemView.findViewById(R.id.my_comment_layout);
            addComment = itemView.findViewById(R.id.add_comment);
            commentView = itemView.findViewById(R.id.comment_view);
            videoView = itemView.findViewById(R.id.video_view);
            likeNumberText = itemView.findViewById(R.id.like_number);
            followButton = itemView.findViewById(R.id.follow_button);
            usernameText = itemView.findViewById(R.id.username);
            captionText = itemView.findViewById(R.id.caption);
            viewAllComments = itemView.findViewById(R.id.view_all_comments);
            viewPager = itemView.findViewById(R.id.view_pager);
            viewPagerIndicator = itemView.findViewById(R.id.view_pager_indicator);
        }
    }

    private class StreamPostAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

        @NonNull
        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            LayoutInflater layoutInflater = LayoutInflater.from(getContext());
            View view = layoutInflater.inflate(R.layout.stream_post_row, parent, false);
            return new PostRowHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull RecyclerView.ViewHolder viewHolder, int position) {
            final PostRowHolder holder = (PostRowHolder) viewHolder;
            final PostData postData = mPosts.get(position);
            final int screenWidth = AppSocialGlobal.getScreenWidth(getContext());
            final PhotoPagerAdapter adapter = new PhotoPagerAdapter(getContext(), postData, holder.likeImage, holder.likeNumberText, "feed");

            holder.tagsLayout.removeAllViews();
            if (postData.photos.size() > 0) {
                holder.photosLayout.setVisibility(View.VISIBLE);
                holder.videoView.setVisibility(View.GONE);
                holder.videoView.setVideoPathNull();
                FrameLayout.LayoutParams layoutParams = (FrameLayout.LayoutParams) holder.viewPager.getLayoutParams();
                layoutParams.width = screenWidth;
                int height = 0;
                for (PhotoData photoData : postData.photos) {
                    int h = screenWidth * photoData.height / photoData.width;
                    height = Math.max(height, h);
                }
                layoutParams.height = height;
                holder.viewPager.setLayoutParams(layoutParams);
                holder.viewPager.setAdapter(adapter);
                holder.viewPagerIndicator.setDotsCount(postData.photos.size());
                holder.viewPager.clearOnPageChangeListeners();
                holder.viewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
                    @Override
                    public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

                    }

                    @Override
                    public void onPageSelected(final int pos) {
                        holder.viewPagerIndicator.setCurrent(pos);
                        holder.tagsLayout.removeAllViews();
                        if (adapter.isShowingTags()) {
                            holder.tagsLayout.setVisibility(View.VISIBLE);
                        } else {
                            holder.tagsLayout.setVisibility(View.GONE);
                        }
                        if (postData.photos.get(pos).tags.size() == 0) {
                            holder.tagsLayout.setVisibility(View.GONE);
                        }
                        for (int i = 0; i < postData.photos.get(pos).tags.size(); i++) {
                            final int p = i;
                            TagData tagData = postData.photos.get(pos).tags.get(i);
                            final FrameLayout tagLayout;
                            if (postData.photos.get(pos).tags.size() == 1) {
                                tagLayout = AppSocialGlobal.addTagView(getContext(), holder.tagsLayout, tagData, 0, false);
                            } else {
                                tagLayout = AppSocialGlobal.addTagView(getContext(), holder.tagsLayout, tagData, i + 1, false);
                            }
                            tagLayout.setTag(0);
                            adapter.reset();
                            tagLayout.setOnClickListener(new View.OnClickListener() {
                                @Override
                                public void onClick(View v) {
                                    adapter.setFocus(pos, p);
                                    int n = (int) tagLayout.getTag();
                                    for(int index = 0; index < holder.tagsLayout.getChildCount(); index++) {
                                        View view = holder.tagsLayout.getChildAt(index);
                                        view.setBackgroundColor(getResources().getColor(R.color.transparentWhite));
                                        if (!tagLayout.equals(view)) {
                                            view.setTag(0);
                                        }
                                    }
                                    if (n == 0) {
                                        tagLayout.setTag(1);
                                        tagLayout.setBackgroundColor(getResources().getColor(R.color.transparentWhiteSelected));
                                    } else {
                                        tagLayout.setTag(0);
                                    }
                                }
                            });
                        }
                    }

                    @Override
                    public void onPageScrollStateChanged(int state) {

                    }
                });
                if (postData.photos.size() > 1) {
                    holder.viewPagerIndicator.setVisibility(View.VISIBLE);
                } else {
                    holder.viewPagerIndicator.setVisibility(View.GONE);
                }
                for (int i = 0; i < postData.photos.get(0).tags.size(); i++) {
                    final int pos= i;
                    TagData tagData = postData.photos.get(0).tags.get(i);
                    final FrameLayout tagLayout;
                    if (postData.photos.get(0).tags.size() == 1) {
                        tagLayout = AppSocialGlobal.addTagView(getContext(), holder.tagsLayout, tagData, 0, false);
                    } else {
                        tagLayout = AppSocialGlobal.addTagView(getContext(), holder.tagsLayout, tagData, i + 1, false);
                    }
                    tagLayout.setTag(0);
                    tagLayout.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            adapter.setFocus(0, pos);
                            int n = (int) tagLayout.getTag();
                            for(int index = 0; index < holder.tagsLayout.getChildCount(); index++) {
                                View view = holder.tagsLayout.getChildAt(index);
                                view.setBackgroundColor(getResources().getColor(R.color.transparentWhite));
                                if (!tagLayout.equals(view)) {
                                    view.setTag(0);
                                }
                            }
                            if (n == 0) {
                                tagLayout.setTag(1);
                                tagLayout.setBackgroundColor(getResources().getColor(R.color.transparentWhiteSelected));
                            } else {
                                tagLayout.setTag(0);
                            }
                        }
                    });
                }
            } else if (postData.video != null) {
                holder.photosLayout.setVisibility(View.GONE);
                holder.viewPagerIndicator.setVisibility(View.GONE);
                holder.videoView.setVisibility(View.VISIBLE);
                holder.videoView.setIfNoSound(postData.video.isMuted);
                holder.videoView.setVideo(postData.video);
                for (int i = 0; i < postData.video.tags.size(); i++) {
                    TagData tagData = postData.video.tags.get(i);
                    AppSocialGlobal.addTagView(getContext(), holder.tagsLayout, tagData, 0, false);
                }
                holder.videoView.setOnClickListener(new DoubleClick(new DoubleClickListener() {
                    @Override
                    public void onSingleClick(View view) {
                        holder.videoView.playAudio();
                    }

                    @Override
                    public void onDoubleClick(View view) {
                        if (!AppSocialGlobal.getInstance().checkIfNeedSignIn(getContext())) {
                            if (!postData.isLiked) {
                                AppSocialGlobal.getInstance().likePost(postData.postId);
                                AppSocialGlobal.setLikeNumber(postData, holder.likeNumberText);
                                holder.likeImage.setImageResource(R.drawable.post_like_sel);
                                holder.videoView.animateHeart();
                            }
                        }
                    }
                }));
            } else {
                holder.photosLayout.setVisibility(View.GONE);
                holder.viewPagerIndicator.setVisibility(View.GONE);
                holder.videoView.setVisibility(View.GONE);
            }

            holder.followButton.setVisibility(View.VISIBLE);
            if (postData.user.equals(AppSocialGlobal.getInstance().me)) {
                holder.followButton.setVisibility(View.GONE);
            } else if (postData.user.followed) {
                holder.followButton.setVisibility(View.GONE);
            } else {
                holder.followButton.setText("Follow");
                holder.followButton.setTextColor(getResources().getColor(R.color.lightBlueColor));
                holder.followButton.setEnabled(true);
            }
            holder.followButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (!AppSocialGlobal.getInstance().checkIfNeedSignIn(getContext())) {
                        AppSocialGlobal.getInstance().follow(postData.user);
                    }
                }
            });
            String photoUrl = postData.user.photoUrl;
            String name = postData.user.username;

            AppSocialGlobal.loadImage(photoUrl, holder.profileImage);
            holder.profileImage.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    AppSocialGlobal.getInstance().gotoAccount(postData.user.userId, getContext());
                }
            });
            holder.usernameText.setText(name);
            holder.usernameText.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    AppSocialGlobal.getInstance().gotoAccount(postData.user.userId, getContext());
                }
            });

            AppSocialGlobal.setLikeNumber(postData, holder.likeNumberText);
            ArrayList<String> captions = postData.captions;
            AppSocialGlobal.getInstance().setTextExpand(holder.captionText, name, captions, getContext(), postData, "feed");

            holder.commentView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    AppSocialGlobal.getInstance().tmp_post = postData;
                    Intent intent = new Intent(getContext(), CommentActivity.class);
                    intent.putExtra("input", false);
                    startActivity(intent);
                }
            });

            if (postData.comments.size() == 0) {
                holder.viewAllComments.setVisibility(View.GONE);
            } else {
                holder.viewAllComments.setVisibility(View.VISIBLE);
                int number = postData.comments.size();
                for (CommentData commentData : postData.comments) {
                    number += commentData.replies.size();
                }
                holder.viewAllComments.setText(String.format("View All %d Comments", number));
            }

            if (postData.isLiked) {
                holder.likeImage.setImageResource(AppSocialGlobal.likeSelectedResourceId);
            } else {
                holder.likeImage.setImageResource(AppSocialGlobal.likeUnselectedResourceId);
            }
            holder.like.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (!AppSocialGlobal.getInstance().checkIfNeedSignIn(getContext())) {
                        AppSocialGlobal.getInstance().likePost(postData.postId);
                        AppSocialGlobal.setLikeNumber(postData, holder.likeNumberText);
                        if (postData.isLiked) {
                            holder.likeImage.setImageResource(AppSocialGlobal.likeSelectedResourceId);
                        } else {
                            holder.likeImage.setImageResource(AppSocialGlobal.likeUnselectedResourceId);
                        }
                    }
                }
            });

            holder.commentImage.setImageResource(AppSocialGlobal.commentResourceId);
            holder.comment.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    AppSocialGlobal.getInstance().tmp_post = postData;
                    Intent intent = new Intent(getContext(), CommentActivity.class);
                    intent.putExtra("input", true);
                    startActivity(intent);
                }
            });

            if (postData.isFavored) {
                holder.favImage.setImageResource(AppSocialGlobal.favoriteSelectedResourceId);
            } else {
                holder.favImage.setImageResource(AppSocialGlobal.favoriteUnselectedResourceId);
            }
            holder.fav.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (!AppSocialGlobal.getInstance().checkIfNeedSignIn(getContext())) {
                        AppSocialGlobal.getInstance().favorPost(postData.postId);
                        if (postData.isFavored) {
                            holder.favImage.setImageResource(AppSocialGlobal.favoriteSelectedResourceId);
                        } else {
                            holder.favImage.setImageResource(AppSocialGlobal.favoriteUnselectedResourceId);
                        }
                    }
                }
            });

            boolean hasTag = false;
            for (PhotoData photoData : postData.photos) {
                if (photoData.tags.size() > 0) {
                    hasTag = true;
                    break;
                }
            }
            if (postData.video != null) {
                if (postData.video.tags.size() > 0) {
                    hasTag = true;
                }
            }
            if (hasTag) {
                holder.tag.setVisibility(View.VISIBLE);
            } else {
                holder.tag.setVisibility(View.GONE);
            }
            holder.tagImage.setTag("0");
            holder.tagImage.setImageResource(AppSocialGlobal.tagUnselectedResourceId);
            holder.tagsLayout.setVisibility(View.GONE);
            holder.tag.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (holder.tagImage.getTag().equals("0")) {
                        holder.tagImage.setTag("1");
                        holder.tagImage.setImageResource(AppSocialGlobal.tagSelectedResourceId);
                        holder.tagsLayout.setVisibility(View.VISIBLE);
                        if (postData.photos.size() > 0) {
                            PhotoData photoData = postData.photos.get(holder.viewPager.getCurrentItem());
                            if (photoData.tags.size() == 0) {
                                holder.tagsLayout.setVisibility(View.GONE);
                            }
                        }
                        adapter.showTags(true);
                    } else {
                        holder.tagImage.setTag("0");
                        holder.tagImage.setImageResource(AppSocialGlobal.tagUnselectedResourceId);
                        for(int index = 0; index < holder.tagsLayout.getChildCount(); index++) {
                            View view = holder.tagsLayout.getChildAt(index);
                            view.setBackgroundColor(getResources().getColor(R.color.transparentWhite));
                            view.setTag(0);
                        }
                        holder.tagsLayout.setVisibility(View.GONE);
                        adapter.showTags(false);
                    }
                }
            });

            holder.myCommentLayout.removeAllViews();
            for (int i = postData.comments.size() - 1; i >= 0; i--) {
                CommentData commentData = postData.comments.get(i);
                if (commentData.user.equals(AppSocialGlobal.getInstance().me)) {
                    AppSocialGlobal.getInstance().addCommentRow(getContext(), commentData, holder.myCommentLayout, postData.postId, "feed");
                    break;
                }
                if (commentData.user.equals(postData.user)) {
                    AppSocialGlobal.getInstance().addCommentRow(getContext(), commentData, holder.myCommentLayout, postData.postId, "feed");
                    break;
                }
            }

            String photoUrl2 = AppSocialGlobal.getInstance().me.photoUrl;
            AppSocialGlobal.loadImage(photoUrl2, holder.addCommentProfile);
            holder.addComment.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (!AppSocialGlobal.getInstance().checkIfNeedSignIn(getContext())) {
                        EventBus.getDefault().post(new MessageEvent(MessageEvent.MessageEventType.HIDE_TAB_BUTTONS));
                        mCommentLayout.setVisibility(View.VISIBLE);
                        mAddCommentText.requestFocus();
                        mAddPostImage.setVisibility(View.GONE);
                        mAddCommentPostId = postData.postId;
                        InputMethodManager imm = (InputMethodManager) getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
                        imm.showSoftInput(mAddCommentText, InputMethodManager.SHOW_IMPLICIT);
                    }
                }
            });

            if (position == mPosts.size() - 1 && haveMoreToLoad) {
                ApiHelper.getAllPostsNext(new ApiHelper.ApiCallback() {
                    @Override
                    public void onSuccess(JSONObject response) {
                        Log.d("APIGetAllPostsNext", response.toString());
                        haveMoreToLoad = AppSocialGlobal.getInstance().updatePosts(response.optJSONArray("resultData"), AppSocialGlobal.getInstance().mainPosts);
                        setupPosts();
                    }

                    @Override
                    public void onFail(String errorMsg) {
                        Log.e("APIGetAllPostsNext", errorMsg);
                    }
                });
            }
        }

        @Override
        public int getItemCount() {
            return mPosts.size();
        }
    }

    private void endEdit(EditText editText, TextView textView) {
        mAddPostImage.setVisibility(View.VISIBLE);
        textView.setTextColor(getResources().getColor(R.color.whiteLightBlueColor));
        editText.getText().clear();
        EventBus.getDefault().post(new MessageEvent(MessageEvent.MessageEventType.SHOW_TAB_BUTTONS));
        mCommentLayout.setVisibility(View.GONE);
        InputMethodManager imm = (InputMethodManager) getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(editText.getWindowToken(), 0);
    }

    private void setupPosts() {
        Activity activity = getActivity();
        if (activity != null) {
            activity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    mPosts = new ArrayList<PostData>(AppSocialGlobal.getInstance().mainPosts.values());
                    if (mAdapter != null) {
                        mAdapter.notifyDataSetChanged();
                    }
                    mRefreshLayout.setRefreshing(false);
                }
            });
        }
    }

    private void getAllPosts() {
        ApiHelper.getAllPosts(new ApiHelper.ApiCallback() {
            @Override
            public void onSuccess(JSONObject response) {
                Log.d("APIGetAllPosts success", response.toString());
                AppSocialGlobal.getInstance().mainPosts.clear();
                haveMoreToLoad = AppSocialGlobal.getInstance().updatePosts(response.optJSONArray("resultData"), AppSocialGlobal.getInstance().mainPosts);
                setupPosts();
                Activity activity = getActivity();
                if (activity != null) {
                    activity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            scrollTop();
                            AppSocialGlobal.loadImage(AppSocialGlobal.getInstance().me.photoUrl, mCommentProfile);
                        }
                    });
                }
                mRefreshLayout.setRefreshing(false);
            }

            @Override
            public void onFail(String errorMsg) {
                Log.e("APIGetAllPosts fail", errorMsg);
                mRefreshLayout.setRefreshing(false);
            }
        });
    }

    private void playVideo(int index) {
        stopAnyVideo();
        if (index != -1) {
            View cellView = mLayoutManager.findViewByPosition(index);
            if (cellView != null) {
                ListVideoView videoView = cellView.findViewById(R.id.video_view);
                String videoPath = videoView.getVideoPath();
                if (videoPath != null && !videoPath.startsWith("http")) {
                    Log.d("playvideo", "index:" + index + ", url:" + videoPath);
                    mCurrentPlayingVideoViewIndex = index;
                    videoView.startVideo();
                }
            }
        }
    }

    private void stopAnyVideo() {
        if (mCurrentPlayingVideoViewIndex != -1) {
            View cellView = mLayoutManager.findViewByPosition(mCurrentPlayingVideoViewIndex);
            if (cellView != null) {
                ListVideoView videoView = cellView.findViewById(R.id.video_view);
                String videoPath = videoView.getVideoPath();
                if (videoPath != null) {
                    Log.d("playvideostop", "index:" + mCurrentPlayingVideoViewIndex + ", url:" + videoPath);
                    videoView.stopVideo();
                }
            }
        }
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
                mUpdatePostBtn.setVisibility(View.VISIBLE);
                mIsShowingBtn = true;
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        if (mIsShowingBtn) {
                            mUpdatePostBtn.setVisibility(View.GONE);
                            mIsShowingBtn = false;
                        }
                    }
                }, 5000);
            }

            @Override
            public void onAnimationCancel(Animator animation) {

            }

            @Override
            public void onAnimationRepeat(Animator animation) {

            }
        });
        MediaPlayer mp = MediaPlayer.create(getContext(), R.raw.playsound);
        mp.start();
    }
}
