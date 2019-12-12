package com.teamcircle.circlesdk.activity;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import androidx.viewpager.widget.ViewPager;

import com.pedromassango.doubleclick.DoubleClick;
import com.pedromassango.doubleclick.DoubleClickListener;
import com.teamcircle.circlesdk.R;
import com.teamcircle.circlesdk.helper.ApiHelper;
import com.teamcircle.circlesdk.helper.AppSocialGlobal;
import com.teamcircle.circlesdk.helper.PhotoPagerAdapter;
import com.teamcircle.circlesdk.model.CommentData;
import com.teamcircle.circlesdk.model.MessageEvent;
import com.teamcircle.circlesdk.model.PhotoData;
import com.teamcircle.circlesdk.model.PostData;
import com.teamcircle.circlesdk.model.TagData;
import com.teamcircle.circlesdk.view.CommentRowView;
import com.teamcircle.circlesdk.view.ListVideoView;
import com.teamcircle.circlesdk.view.ViewPagerIndicator;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;

import de.hdodenhof.circleimageview.CircleImageView;

public class PostActivity extends Activity {
    private int mPostId;
    private PostData mPostData;
    private EditText mCommentEditText;
    private TextView mPostButtonText;
    private FrameLayout mReplyIndicator;
    private TextView mReplyIndicatorText;
    private int mReplyToCommentId = -1;
    private boolean mIsReadyToPost = false;
    private ListView mListView;
    private PostAdapter mAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_post);
        EventBus.getDefault().register(this);

        mPostId = getIntent().getIntExtra("postId", -1);
        mPostData = AppSocialGlobal.getInstance().getPostById(mPostId);

        mPostButtonText = findViewById(R.id.post_button_text);
        mCommentEditText = findViewById(R.id.comment_edit_text);
        mReplyIndicator = findViewById(R.id.add_comment_reply_layout);
        mReplyIndicatorText = findViewById(R.id.add_comment_reply_text);
        CircleImageView mCommentProfile = findViewById(R.id.comment_profile_image);
        AppSocialGlobal.loadImage(AppSocialGlobal.getInstance().me.photoUrl, mCommentProfile);
        mCommentEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                if (mCommentEditText.getText().toString().isEmpty()) {
                    mPostButtonText.setTextColor(getResources().getColor(R.color.whiteLightBlueColor));
                    mIsReadyToPost = false;
                } else {
                    mPostButtonText.setTextColor(getResources().getColor(R.color.lightBlueColor));
                    mIsReadyToPost = true;
                }
                if (mCommentEditText.getLayout() != null) {
                    if (mCommentEditText.getLayout().getLineCount() > 10)
                        mCommentEditText.getText().delete(mCommentEditText.getText().length() - 1, mCommentEditText.getText().length());
                }
            }
        });
        mCommentEditText.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (hasFocus) {
                    if (AppSocialGlobal.getInstance().checkIfNeedSignIn(PostActivity.this)) {
                        mCommentEditText.clearFocus();
                    }
                }
            }
        });
        FrameLayout postButton = findViewById(R.id.post_button);
        postButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mIsReadyToPost) {
                    String content = mCommentEditText.getText().toString();
                    if (mReplyToCommentId == -1) {
                        ApiHelper.commentPost(mPostData.postId, content, new ApiHelper.ApiCallback() {
                            @Override
                            public void onSuccess(JSONObject response) {
                                Log.d("APIComment success", response.toString());
                                AppSocialGlobal.getInstance().updatePosts(response.optJSONArray("resultData"), null);
                                mPostData = AppSocialGlobal.getInstance().allPosts.get(mPostData.postId);
                                mAdapter.notifyDataSetChanged();
                            }

                            @Override
                            public void onFail(String errorMsg) {
                                Log.e("APIComment fail", errorMsg);
                            }
                        });
                    } else {
                        ApiHelper.replyComment(mReplyToCommentId, content, new ApiHelper.ApiCallback() {
                            @Override
                            public void onSuccess(JSONObject response) {
                                Log.d("APIReply success", response.toString());
                                AppSocialGlobal.getInstance().updatePosts(response.optJSONArray("resultData"), null);
                                mPostData = AppSocialGlobal.getInstance().allPosts.get(mPostData.postId);
                                mAdapter.notifyDataSetChanged();
                            }

                            @Override
                            public void onFail(String errorMsg) {
                                Log.e("APIReply fail", errorMsg);
                            }
                        });
                    }
                    doneEditing();
                }
            }
        });

        ImageView backImage = findViewById(R.id.back_image);
        backImage.setImageResource(AppSocialGlobal.backResourceId);
        FrameLayout back = findViewById(R.id.back);
        back.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        mListView = findViewById(R.id.list_view);
        mAdapter = new PostAdapter();
        mListView.setAdapter(mAdapter);
    }

    @Override
    protected void onDestroy() {
        EventBus.getDefault().unregister(this);
        super.onDestroy();
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Subscribe(sticky = true)
    public void onEvent(MessageEvent event) {
        switch (event.type) {
            case UPDATE_POST:
            case DONE_DOWNLOAD:
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        mAdapter.notifyDataSetChanged();
                    }
                });
                break;
        }
    }

    public class PostAdapter extends BaseAdapter {

        @Override
        public int getCount() {
            return mPostData.comments.size() + 1;
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
            View view = convertView;
            LayoutInflater layoutInflater = LayoutInflater.from(PostActivity.this);
            if (position == 0) {
                view = layoutInflater.inflate(R.layout.post_top, parent, false);

                final ImageView likeImage = view.findViewById(R.id.post_like_image);
                final TextView likeNumberText = view.findViewById(R.id.like_number);
                FrameLayout photosLayout = view.findViewById(R.id.photos_view_layout);
                final ViewPager viewPager = view.findViewById(R.id.view_pager);
                final PhotoPagerAdapter adapter = new PhotoPagerAdapter(PostActivity.this, mPostData, likeImage, likeNumberText, "post");
                final ViewPagerIndicator viewPagerIndicator = view.findViewById(R.id.view_pager_indicator);
                final ListVideoView videoView = view.findViewById(R.id.video_view);
                final int screenWidth = AppSocialGlobal.getScreenWidth(PostActivity.this);
                final LinearLayout tagsLayout = view.findViewById(R.id.tags_layout);
                tagsLayout.removeAllViews();
                if (mPostData.photos.size() > 0) {
                    photosLayout.setVisibility(View.VISIBLE);
                    videoView.setVisibility(View.GONE);
                    FrameLayout.LayoutParams layoutParams = (FrameLayout.LayoutParams) viewPager.getLayoutParams();
                    layoutParams.width = screenWidth;
                    int height = 0;
                    for (PhotoData photoData : mPostData.photos) {
                        int h = screenWidth * photoData.height / photoData.width;
                        height = Math.max(height, h);
                    }
                    layoutParams.height = height;
                    viewPager.setLayoutParams(layoutParams);
                    viewPager.setAdapter(adapter);
                    viewPagerIndicator.setDotsCount(mPostData.photos.size());
                    viewPager.clearOnPageChangeListeners();
                    viewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
                        @Override
                        public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

                        }

                        @Override
                        public void onPageSelected(final int pos) {
                            viewPagerIndicator.setCurrent(pos);
                            tagsLayout.removeAllViews();
                            if (adapter.isShowingTags()) {
                                tagsLayout.setVisibility(View.VISIBLE);
                            } else {
                                tagsLayout.setVisibility(View.GONE);
                            }
                            if (mPostData.photos.get(pos).tags.size() == 0) {
                                tagsLayout.setVisibility(View.GONE);
                            }
                            for (int i = 0; i < mPostData.photos.get(pos).tags.size(); i++) {
                                final int p = i;
                                TagData tagData = mPostData.photos.get(pos).tags.get(i);
                                final FrameLayout tagLayout;
                                if (mPostData.photos.get(pos).tags.size() == 1) {
                                    tagLayout = AppSocialGlobal.addTagView(PostActivity.this, tagsLayout, tagData, 0, false);
                                } else {
                                    tagLayout = AppSocialGlobal.addTagView(PostActivity.this, tagsLayout, tagData, i + 1, false);
                                }
                                tagLayout.setTag(0);
                                adapter.reset();
                                tagLayout.setOnClickListener(new View.OnClickListener() {
                                    @Override
                                    public void onClick(View v) {
                                        adapter.setFocus(pos, p);
                                        int n = (int) tagLayout.getTag();
                                        for (int index = 0; index < tagsLayout.getChildCount(); index++) {
                                            View view = tagsLayout.getChildAt(index);
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
                    if (mPostData.photos.size() > 1) {
                        viewPagerIndicator.setVisibility(View.VISIBLE);
                    } else {
                        viewPagerIndicator.setVisibility(View.GONE);
                    }
                    for (int i = 0; i < mPostData.photos.get(0).tags.size(); i++) {
                        final int pos = i;
                        TagData tagData = mPostData.photos.get(0).tags.get(i);
                        final FrameLayout tagLayout;
                        if (mPostData.photos.get(0).tags.size() == 1) {
                            tagLayout = AppSocialGlobal.addTagView(PostActivity.this, tagsLayout, tagData, 0, false);
                        } else {
                            tagLayout = AppSocialGlobal.addTagView(PostActivity.this, tagsLayout, tagData, i + 1, false);
                        }
                        tagLayout.setTag(0);
                        tagLayout.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                adapter.setFocus(0, pos);
                                int n = (int) tagLayout.getTag();
                                for (int index = 0; index < tagsLayout.getChildCount(); index++) {
                                    View view = tagsLayout.getChildAt(index);
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
                } else if (mPostData.video != null) {
                    photosLayout.setVisibility(View.GONE);
                    viewPagerIndicator.setVisibility(View.GONE);
                    videoView.setVisibility(View.VISIBLE);
                    videoView.setIfNoSound(mPostData.video.isMuted);
                    videoView.setVideo(mPostData.video);
                    for (int i = 0; i < mPostData.video.tags.size(); i++) {
                        TagData tagData = mPostData.video.tags.get(i);
                        AppSocialGlobal.addTagView(PostActivity.this, tagsLayout, tagData, 0, false);
                    }
                    String videoPath = videoView.getVideoPath();
                    if (videoPath != null && !videoPath.startsWith("http")) {
                        videoView.startVideo();
                    }
                    videoView.setOnClickListener(new DoubleClick(new DoubleClickListener() {
                        @Override
                        public void onSingleClick(View view) {
                            videoView.playAudio();
                        }

                        @Override
                        public void onDoubleClick(View view) {
                            if (!AppSocialGlobal.getInstance().checkIfNeedSignIn(PostActivity.this)) {
                                    if (!mPostData.isLiked) {
                                        AppSocialGlobal.getInstance().likePost(mPostData.postId);
                                        AppSocialGlobal.setLikeNumber(mPostData, likeNumberText);
                                        likeImage.setImageResource(R.drawable.post_like_sel);
                                        videoView.animateHeart();
                                    }
                                }
                            }
                    }));
                } else {
                    photosLayout.setVisibility(View.GONE);
                    viewPagerIndicator.setVisibility(View.GONE);
                    videoView.setVisibility(View.GONE);
                }

                ImageView profileImage = view.findViewById(R.id.profile_image);
                final TextView followButton = view.findViewById(R.id.follow_button);
                followButton.setVisibility(View.VISIBLE);
                if (mPostData.user.equals(AppSocialGlobal.getInstance().me)) {
                    followButton.setVisibility(View.GONE);
                } else if (mPostData.user.followed) {
                    followButton.setVisibility(View.GONE);
                } else {
                    followButton.setText("Follow");
                    followButton.setTextColor(getResources().getColor(R.color.lightBlueColor));
                    followButton.setEnabled(true);
                }
                followButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (!AppSocialGlobal.getInstance().checkIfNeedSignIn(PostActivity.this)) {
                            AppSocialGlobal.getInstance().follow(mPostData.user);
                        }
                    }
                });
                String photoUrl = mPostData.user.photoUrl;
                String name = mPostData.user.username;

                profileImage.setImageResource(R.drawable.profile_placeholder);
                AppSocialGlobal.loadImage(photoUrl, profileImage);
                profileImage.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        AppSocialGlobal.getInstance().gotoAccount(mPostData.user.userId, PostActivity.this);
                    }
                });
                TextView usernameText = view.findViewById(R.id.username);
                usernameText.setText(name);
                usernameText.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        AppSocialGlobal.getInstance().gotoAccount(mPostData.user.userId, PostActivity.this);
                    }
                });

                AppSocialGlobal.setLikeNumber(mPostData, likeNumberText);
                final TextView captionText = view.findViewById(R.id.caption);

                ArrayList<String> captions = mPostData.captions;
                AppSocialGlobal.getInstance().setTextClick(captionText, mPostData.user, captions, mPostId, "post");

                FrameLayout like = view.findViewById(R.id.post_like);
                if (mPostData.isLiked) {
                    likeImage.setImageResource(R.drawable.post_like_sel);
                } else {
                    likeImage.setImageResource(R.drawable.post_like);
                }
                like.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (!AppSocialGlobal.getInstance().checkIfNeedSignIn(PostActivity.this)) {
                            AppSocialGlobal.getInstance().likePost(mPostData.postId);
                            AppSocialGlobal.setLikeNumber(mPostData, likeNumberText);
                            if (mPostData.isLiked) {
                                likeImage.setImageResource(R.drawable.post_like_sel);
                            } else {
                                likeImage.setImageResource(R.drawable.post_like);
                            }
                        }
                    }
                });

                FrameLayout comment = view.findViewById(R.id.post_comment);
                comment.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        AppSocialGlobal.getInstance().tmp_post = mPostData;
                        Intent intent = new Intent(PostActivity.this, CommentActivity.class);
                        intent.putExtra("input", true);
                        startActivity(intent);
                    }
                });

                FrameLayout fav = view.findViewById(R.id.post_fav);
                final ImageView favImage = view.findViewById(R.id.post_fav_image);
                if (mPostData.isFavored) {
                    favImage.setImageResource(R.drawable.post_favor_sel);
                } else {
                    favImage.setImageResource(R.drawable.post_favor);
                }
                fav.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (!AppSocialGlobal.getInstance().checkIfNeedSignIn(PostActivity.this)) {
                            AppSocialGlobal.getInstance().favorPost(mPostData.postId);
                            if (mPostData.isFavored) {
                                favImage.setImageResource(R.drawable.post_favor_sel);
                            } else {
                                favImage.setImageResource(R.drawable.post_favor);
                            }
                        }
                    }
                });

                FrameLayout tag = view.findViewById(R.id.post_tag);
                boolean hasTag = false;
                for (PhotoData photoData : mPostData.photos) {
                    if (photoData.tags.size() > 0) {
                        hasTag = true;
                        break;
                    }
                }
                if (mPostData.video != null) {
                    if (mPostData.video.tags.size() > 0) {
                        hasTag = true;
                    }
                }
                if (hasTag) {
                    tag.setVisibility(View.VISIBLE);
                } else {
                    tag.setVisibility(View.GONE);
                }
                final ImageView tagImage = view.findViewById(R.id.post_tag_image);
                tagImage.setTag("0");
                tagImage.setImageResource(R.drawable.post_tag);
                tagsLayout.setVisibility(View.GONE);
                tag.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (tagImage.getTag().equals("0")) {
                            tagImage.setTag("1");
                            tagImage.setImageResource(R.drawable.post_tag_sel);
                            tagsLayout.setVisibility(View.VISIBLE);
                            if (mPostData.photos.size() > 0) {
                                PhotoData photoData = mPostData.photos.get(viewPager.getCurrentItem());
                                if (photoData.tags.size() == 0) {
                                    tagsLayout.setVisibility(View.GONE);
                                }
                            }
                            adapter.showTags(true);
                        } else {
                            tagImage.setTag("0");
                            tagImage.setImageResource(R.drawable.post_tag);
                            for (int index = 0; index < tagsLayout.getChildCount(); index++) {
                                View view = tagsLayout.getChildAt(index);
                                view.setBackgroundColor(getResources().getColor(R.color.transparentWhite));
                                view.setTag(0);
                            }
                            tagsLayout.setVisibility(View.GONE);
                            adapter.showTags(false);
                        }
                    }
                });
            } else {
                CommentData commentData = mPostData.comments.get(position - 1);
                view = addCommentRow(commentData);
            }
            return view;
        }
    }

    private void startEditing(String s) {
        mCommentEditText.requestFocus();
        mCommentEditText.getText().clear();
        if (s != null) {
            mCommentEditText.append("@" + s + " ");
        }
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.showSoftInput(mCommentEditText, InputMethodManager.SHOW_IMPLICIT);
    }

    private void doneEditing() {
        mReplyToCommentId = -1;
        mReplyIndicator.setVisibility(View.GONE);
        mCommentEditText.getText().clear();
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(mCommentEditText.getWindowToken(), 0);
    }

    private LinearLayout addCommentRow(final CommentData commentData) {
        final boolean deletable = commentData.user.equals(AppSocialGlobal.getInstance().me);
        final LinearLayout commentRow = new CommentRowView(this, AppSocialGlobal.dpToPx(PostActivity.this, 15), deletable);
        ImageView commentImage = commentRow.findViewById(R.id.comment_profile);
        String photoUrl = commentData.user.photoUrl;
        AppSocialGlobal.loadImage(photoUrl, commentImage);
        commentImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AppSocialGlobal.getInstance().gotoAccount(commentData.user.userId, PostActivity.this);
            }
        });
        TextView commentText = commentRow.findViewById(R.id.comment_text);
        AppSocialGlobal.getInstance().setTextClick(commentText, commentData.user, commentData.contents, mPostId, "post");
        final TextView replyButton = commentRow.findViewById(R.id.reply_button);
        replyButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!AppSocialGlobal.getInstance().checkIfNeedSignIn(PostActivity.this)) {
                    mReplyToCommentId = commentData.commentId;
                    mReplyIndicator.setVisibility(View.VISIBLE);
                    mReplyIndicator.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            mReplyToCommentId = -1;
                            mReplyIndicator.setVisibility(View.GONE);
                            mCommentEditText.getText().clear();
                        }
                    });
                    mReplyIndicatorText.setText("Replying to " + commentData.user.username + "...");
                    startEditing(null);
                }
            }
        });
        TextView timeText = commentRow.findViewById(R.id.time);
        long time = commentData.date;
        long timeDiff = new Date().getTime() - time;
        String timeString = "1m";
        if (timeDiff >= 7 * 24 * 60 * 60 * 1000) {
            timeString = timeDiff / (7 * 24 * 60 * 60 * 1000) + "w";
        } else if (timeDiff >= (24 * 60 * 60 * 1000)) {
            timeString = timeDiff / (24 * 60 * 60 * 1000) + "d";
        } else if (timeDiff > 60 * 60 * 1000) {
            timeString = timeDiff / (60 * 60 * 1000) + "h";
        } else if (timeDiff > 60 * 1000) {
            timeString = timeDiff / (60 * 1000) + "m";
        }
        timeText.setText(timeString);
        FrameLayout like = commentRow.findViewById(R.id.like);
        final ImageView likeImage = commentRow.findViewById(R.id.like_image);
        if (commentData.isLiked) {
            likeImage.setImageResource(R.drawable.post_like_sel);
        } else {
            likeImage.setImageResource(R.drawable.post_like);
        }
        final TextView likeNumber = commentRow.findViewById(R.id.like_number);
//        likeNumber.setVisibility(View.VISIBLE);
        if (commentData.likeNumber <= 0) {
            likeNumber.setVisibility(View.GONE);
        } else if (commentData.likeNumber == 1) {
            likeNumber.setText("1 like");
        } else {
            likeNumber.setText(String.format("%d likes", commentData.likeNumber));
        }
        like.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!AppSocialGlobal.getInstance().checkIfNeedSignIn(PostActivity.this)) {
                    if (commentData.isLiked) {
                        likeImage.setImageResource(R.drawable.post_like);
                        commentData.likeNumber--;
                    } else {
                        likeImage.setImageResource(R.drawable.post_like_sel);
                        commentData.likeNumber++;
                    }
                    commentData.isLiked = !commentData.isLiked;
//                    if (commentData.likeNumber <= 0) {
//                        likeNumber.setVisibility(View.GONE);
//                    } else if (commentData.likeNumber == 1) {
//                        likeNumber.setText("1 like");
//                    } else {
//                        likeNumber.setText(String.format("%d likes", commentData.likeNumber));
//                    }
                    ApiHelper.likeComment(commentData.commentId, new ApiHelper.ApiCallback() {
                        @Override
                        public void onSuccess(JSONObject response) {
                            Log.d("APILikeComment success", response.toString());
                        }

                        @Override
                        public void onFail(String errorMsg) {
                            Log.d("APILikeComment fail", errorMsg);
                        }
                    });
                }
            }
        });

        FrameLayout delete = commentRow.findViewById(R.id.delete);
        ImageView deleteImage = commentRow.findViewById(R.id.delete_image);
        if (!deletable) {
            deleteImage.setVisibility(View.GONE);
        }
        delete.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (deletable) {
                    mPostData.comments.remove(commentData);
                    mAdapter.notifyDataSetChanged();
                    ApiHelper.deleteComment(commentData.commentId, new ApiHelper.ApiCallback() {
                        @Override
                        public void onSuccess(JSONObject response) {
                            Log.d("APIDeleteComment", response.toString());
                        }

                        @Override
                        public void onFail(String errorMsg) {
                            Log.e("APIDeleteComment", errorMsg);
                        }
                    });
                }
            }
        });
        for (final CommentData replyData : commentData.replies) {
            addReplyRow(replyData, commentData, commentRow);
        }
        return commentRow;
    }

    private void addReplyRow(final CommentData replyData, final CommentData commentData, final LinearLayout commentRow) {
        final boolean deletable = replyData.user.equals(AppSocialGlobal.getInstance().me);
        final LinearLayout replyRow = new CommentRowView(this, AppSocialGlobal.dpToPx(this, 30), deletable);
        ImageView replyImage = replyRow.findViewById(R.id.comment_profile);
        String photoUrl = replyData.user.photoUrl;
        AppSocialGlobal.loadImage(photoUrl, replyImage);
        replyImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AppSocialGlobal.getInstance().gotoAccount(replyData.user.userId, PostActivity.this);
            }
        });
        TextView replyText = replyRow.findViewById(R.id.comment_text);
        AppSocialGlobal.getInstance().setTextClick(replyText, replyData.user, replyData.contents, mPostId, "post");
        TextView replyButton = replyRow.findViewById(R.id.reply_button);
        replyButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                HashMap<String, String> map = new HashMap<>();
                map.put("post_id", Integer.toString(mPostId));
                map.put("comment_id", Integer.toString(replyData.commentId));
                map.put("method", "reply");
                if (!AppSocialGlobal.getInstance().checkIfNeedSignIn(PostActivity.this)) {
                    mReplyToCommentId = replyData.commentId;
                    mReplyIndicator.setVisibility(View.VISIBLE);
                    mReplyIndicator.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            mReplyToCommentId = -1;
                            mReplyIndicator.setVisibility(View.GONE);
                            mCommentEditText.getText().clear();
                        }
                    });
                    mReplyIndicatorText.setText("Replying to " + replyData.user.username);
                    startEditing(replyData.user.username);
                }

            }
        });
        TextView timeText = replyRow.findViewById(R.id.time);
        long time = replyData.date;
        long timeDiff = new Date().getTime() - time;
        String timeString = "1m";
        if (timeDiff >= 7 * 24 * 60 * 60 * 1000) {
            timeString = timeDiff / (7 * 24 * 60 * 60 * 1000) + "w";
        } else if (timeDiff >= (24 * 60 * 60 * 1000)) {
            timeString = timeDiff / (24 * 60 * 60 * 1000) + "d";
        } else if (timeDiff > 60 * 60 * 1000) {
            timeString = timeDiff / (60 * 60 * 1000) + "h";
        } else if (timeDiff > 60 * 1000) {
            timeString = timeDiff / (60 * 1000) + "m";
        }
        timeText.setText(timeString);
        FrameLayout like = replyRow.findViewById(R.id.like);
        final ImageView likeImage = replyRow.findViewById(R.id.like_image);
        if (replyData.isLiked) {
            likeImage.setImageResource(R.drawable.post_like_sel);
        } else {
            likeImage.setImageResource(R.drawable.post_like);
        }
        final TextView likeNumber = replyRow.findViewById(R.id.like_number);
        likeNumber.setVisibility(View.VISIBLE);
        if (replyData.likeNumber <= 0) {
            likeNumber.setVisibility(View.GONE);
        } else if (replyData.likeNumber == 1) {
            likeNumber.setText("1 like");
        } else {
            likeNumber.setText(String.format("%d likes", replyData.likeNumber));
        }
        like.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!AppSocialGlobal.getInstance().checkIfNeedSignIn(PostActivity.this)) {
                    if (replyData.isLiked) {
                        likeImage.setImageResource(R.drawable.post_like);
                        replyData.likeNumber--;
                    } else {
                        likeImage.setImageResource(R.drawable.post_like_sel);
                        replyData.likeNumber++;
                    }
                    replyData.isLiked = !replyData.isLiked;
                    likeNumber.setVisibility(View.VISIBLE);
                    if (replyData.likeNumber <= 0) {
                        likeNumber.setVisibility(View.GONE);
                    } else if (replyData.likeNumber == 1) {
                        likeNumber.setText("1 like");
                    } else {
                        likeNumber.setText(String.format("%d likes", replyData.likeNumber));
                    }
                    ApiHelper.likeComment(replyData.commentId, new ApiHelper.ApiCallback() {
                        @Override
                        public void onSuccess(JSONObject response) {
                            Log.d("APILikeComment success", response.toString());
                        }

                        @Override
                        public void onFail(String errorMsg) {
                            Log.d("APILikeComment fail", errorMsg);
                        }
                    });
                }
            }
        });
        FrameLayout delete = replyRow.findViewById(R.id.delete);
        ImageView deleteImage = replyRow.findViewById(R.id.delete_image);
        if (!deletable) {
            deleteImage.setVisibility(View.GONE);
        }
        delete.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (deletable) {
                    commentData.replies.remove(replyData);
                    mAdapter.notifyDataSetChanged();
                    ApiHelper.deleteComment(commentData.commentId, new ApiHelper.ApiCallback() {
                        @Override
                        public void onSuccess(JSONObject response) {
                            Log.d("APIDeleteComment", response.toString());
                        }

                        @Override
                        public void onFail(String errorMsg) {
                            Log.e("APIDeleteComment", errorMsg);
                        }
                    });
                }
            }
        });
        commentRow.addView(replyRow);
    }
}
