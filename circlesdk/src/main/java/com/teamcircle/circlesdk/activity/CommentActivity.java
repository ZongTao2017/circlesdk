package com.teamcircle.circlesdk.activity;

import android.app.Activity;
import android.content.Context;
import android.graphics.PointF;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
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

import com.teamcircle.circlesdk.R;
import com.teamcircle.circlesdk.helper.ApiHelper;
import com.teamcircle.circlesdk.helper.AppSocialGlobal;
import com.teamcircle.circlesdk.model.CommentData;
import com.teamcircle.circlesdk.model.MessageEvent;
import com.teamcircle.circlesdk.model.PostData;
import com.teamcircle.circlesdk.view.CommentRowView;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.json.JSONObject;

import java.util.Date;

public class CommentActivity extends Activity {
    private PostData mPostData;
    private boolean mIsReadyToPost = false;
    private ListView mListView;
    private CommentAdapter mAdapter;
    private EditText mCommentEditText;
    private TextView mPostButtonText;
    private FrameLayout mReplyIndicator;
    private TextView mReplyIndicatorText;
    private PointF mTouchPoint;
    private View mTopView;
    private CommentRowView mCurrentEditingCommentRowView;
    private int mReplyToCommentId = -1;
    private ImageView mProfileImage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_comment);
        EventBus.getDefault().register(this);
        mPostData = AppSocialGlobal.getInstance().tmp_post;
        mListView = findViewById(R.id.list_view);
        mAdapter = new CommentAdapter();
        mListView.setAdapter(mAdapter);
        mPostButtonText = findViewById(R.id.post_button_text);
        mCommentEditText = findViewById(R.id.comment_edit_text);
        mReplyIndicator = findViewById(R.id.add_comment_reply_layout);
        mReplyIndicatorText = findViewById(R.id.add_comment_reply_text);
        mTopView = findViewById(R.id.top_view);
        mTopView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getActionMasked() == MotionEvent.ACTION_DOWN &&
                        mCurrentEditingCommentRowView != null) {
                    mCurrentEditingCommentRowView.reset();
                    mCurrentEditingCommentRowView = null;
                    return true;
                }
                return false;
            }
        });

        mTouchPoint = new PointF();

        ImageView backImage = findViewById(R.id.back_image);
        backImage.setImageResource(AppSocialGlobal.backResourceId);
        final FrameLayout back = findViewById(R.id.back);
        back.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        mProfileImage = findViewById(R.id.profile_image);
        String photoUrl = AppSocialGlobal.getInstance().me.photoUrl;
        AppSocialGlobal.loadImage(photoUrl, mProfileImage);

        mCommentEditText.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (hasFocus) {
                    if (AppSocialGlobal.getInstance().checkIfNeedSignIn(CommentActivity.this)) {
                        mCommentEditText.clearFocus();
                    }
                }
            }
        });
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
    }

    @Override
    protected void onDestroy() {
        EventBus.getDefault().unregister(this);
        super.onDestroy();
    }

    @Subscribe(sticky = true)
    public void onEvent(MessageEvent event) {
        switch (event.type) {
            case EDIT_COMMENT:
                mCurrentEditingCommentRowView = (CommentRowView) event.data;
                break;
            case FINISH_AUTH:
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        String photoUrl = AppSocialGlobal.getInstance().me.photoUrl;
                        AppSocialGlobal.loadImage(photoUrl, mProfileImage);
                        mAdapter.notifyDataSetChanged();
                    }
                });
                break;
        }
    }

    private class CommentAdapter extends BaseAdapter {

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
            if (position == 0) {
                LayoutInflater layoutInflater = LayoutInflater.from(CommentActivity.this);
                view = layoutInflater.inflate(R.layout.comment_top_layout, parent, false);
                ImageView profile = view.findViewById(R.id.profile);
                String photoUrl = mPostData.user.photoUrl;
                AppSocialGlobal.loadImage(photoUrl, profile);
                TextView captionText = view.findViewById(R.id.caption);
                AppSocialGlobal.getInstance().setTextClick(captionText, mPostData.user, mPostData.captions, mPostData.postId, "comment");
                TextView timeText = view.findViewById(R.id.time);
                long time = mPostData.date;
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
        final LinearLayout commentRow = new CommentRowView(this, AppSocialGlobal.dpToPx(CommentActivity.this, 15), deletable);
        ImageView commentImage = commentRow.findViewById(R.id.comment_profile);
        String photoUrl = commentData.user.photoUrl;
        AppSocialGlobal.loadImage(photoUrl, commentImage);
        commentImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AppSocialGlobal.getInstance().gotoAccount(commentData.user.userId, CommentActivity.this);
            }
        });
        TextView commentText = commentRow.findViewById(R.id.comment_text);
        AppSocialGlobal.getInstance().setTextClick(commentText, commentData.user, commentData.contents, mPostData.postId, "comment");
        final TextView replyButton = commentRow.findViewById(R.id.reply_button);
        replyButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!AppSocialGlobal.getInstance().checkIfNeedSignIn(CommentActivity.this)) {
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
        likeNumber.setVisibility(View.VISIBLE);
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
                if (!AppSocialGlobal.getInstance().checkIfNeedSignIn(CommentActivity.this)) {
                    if (commentData.isLiked) {
                        likeImage.setImageResource(R.drawable.post_like);
                        commentData.likeNumber--;
                    } else {
                        likeImage.setImageResource(R.drawable.post_like_sel);
                        commentData.likeNumber++;
                    }
                    commentData.isLiked = !commentData.isLiked;
                    likeNumber.setVisibility(View.VISIBLE);
                    if (commentData.likeNumber <= 0) {
                        likeNumber.setVisibility(View.GONE);
                    } else if (commentData.likeNumber == 1) {
                        likeNumber.setText("1 like");
                    } else {
                        likeNumber.setText(String.format("%d likes", commentData.likeNumber));
                    }
                    EventBus.getDefault().post(new MessageEvent(MessageEvent.MessageEventType.UPDATE_POST));
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
        final LinearLayout replyRow = new CommentRowView(this, AppSocialGlobal.dpToPx(this, 70), deletable);
        ImageView replyImage = replyRow.findViewById(R.id.comment_profile);
        String photoUrl = replyData.user.photoUrl;
        AppSocialGlobal.loadImage(photoUrl, replyImage);
        replyImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AppSocialGlobal.getInstance().gotoAccount(replyData.user.userId, CommentActivity.this);
            }
        });
        TextView replyText = replyRow.findViewById(R.id.comment_text);
        AppSocialGlobal.getInstance().setTextClick(replyText, replyData.user, replyData.contents, mPostData.postId, "comment");
        TextView replyButton = replyRow.findViewById(R.id.reply_button);
        replyButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!AppSocialGlobal.getInstance().checkIfNeedSignIn(CommentActivity.this)) {
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
        if (replyData.likeNumber == 0) {
            likeNumber.setVisibility(View.GONE);
        } else if (replyData.likeNumber <= 1) {
            likeNumber.setText("1 like");
        } else {
            likeNumber.setText(String.format("%d likes", replyData.likeNumber));
        }
        like.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!AppSocialGlobal.getInstance().checkIfNeedSignIn(CommentActivity.this)) {
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
                    EventBus.getDefault().post(new MessageEvent(MessageEvent.MessageEventType.UPDATE_POST));
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
