package com.teamcircle.circlesdk.view;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.PointF;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;

import com.teamcircle.circlesdk.R;
import com.teamcircle.circlesdk.helper.AppSocialGlobal;
import com.teamcircle.circlesdk.model.MessageEvent;

import org.greenrobot.eventbus.EventBus;

public class CommentRowView extends LinearLayout {
    boolean mIsEditing;
    PointF mTouchPoint;
    LinearLayout mDraggable;

    @SuppressLint("ClickableViewAccessibility")
    public CommentRowView(final Context context, final int leftMargin, final boolean deletable) {
        super(context);
        setOrientation(VERTICAL);

        LayoutInflater layoutInflater = LayoutInflater.from(context);
        final LinearLayout row = (LinearLayout) layoutInflater.inflate(R.layout.comment_row, null, false);
        LinearLayout layout = row.findViewById(R.id.layout);
        LayoutParams layoutParams1 = (LayoutParams) layout.getLayoutParams();
        if (leftMargin == 0) {
            layoutParams1.bottomMargin = AppSocialGlobal.dpToPx(context, 5);
        } else {
            layoutParams1.bottomMargin = AppSocialGlobal.dpToPx(context, 20);
        }
        layout.setLayoutParams(layoutParams1);
        ImageView imageView = row.findViewById(R.id.comment_profile);
        LayoutParams layoutParams2 = (LayoutParams) imageView.getLayoutParams();
        layoutParams2.leftMargin = leftMargin;
        if (leftMargin > AppSocialGlobal.dpToPx(context, 15)) {
            layoutParams2.width = AppSocialGlobal.dpToPx(context, 30);
            layoutParams2.height = AppSocialGlobal.dpToPx(context, 30);
        } else {
            layoutParams2.width = AppSocialGlobal.dpToPx(context, 40);
            layoutParams2.height = AppSocialGlobal.dpToPx(context, 40);
        }
        imageView.setLayoutParams(layoutParams2);
        FrameLayout like = row.findViewById(R.id.like);
        LayoutParams layoutParams3 = (LayoutParams) like.getLayoutParams();
        if (leftMargin > 0) {
            layoutParams3.rightMargin = AppSocialGlobal.dpToPx(context, 15);
        } else {
            layoutParams3.rightMargin = 0;
        }
        like.setLayoutParams(layoutParams3);
        addView(row);

        mIsEditing = false;
        mTouchPoint = new PointF();
        mDraggable = row.findViewById(R.id.draggable);
        mDraggable.setOnTouchListener(new OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (!deletable) {
                    return false;
                }
                LayoutParams layoutParams1 = (LayoutParams) mDraggable.getLayoutParams();
                layoutParams1.width = AppSocialGlobal.getScreenWidth(context);
                int leftMargin = layoutParams1.leftMargin;
                switch (event.getActionMasked()) {
                    case MotionEvent.ACTION_DOWN:
                        if (!mIsEditing) {
                            mTouchPoint.set(event.getX(), event.getY());
                        } else {
                            return false;
                        }
                        break;
                    case MotionEvent.ACTION_MOVE:
                        int dx = (int) (event.getX() - mTouchPoint.x);
                        int newLeftMargin = leftMargin + dx;
                        if (newLeftMargin <= 0 && newLeftMargin >= AppSocialGlobal.dpToPx(context, -50)) {
                            layoutParams1.leftMargin = newLeftMargin;
                            mDraggable.setLayoutParams(layoutParams1);
                        }
                        mTouchPoint.set(event.getX(), event.getY());
                        break;
                    case MotionEvent.ACTION_UP:
                    case MotionEvent.ACTION_CANCEL:
                        if (leftMargin <= AppSocialGlobal.dpToPx(context, -25)) {
                            layoutParams1.leftMargin = AppSocialGlobal.dpToPx(context, -50);
                            mIsEditing = true;
                            EventBus.getDefault().post(new MessageEvent(MessageEvent.MessageEventType.EDIT_COMMENT, CommentRowView.this));
                        } else {
                            layoutParams1.leftMargin = 0;
                            mIsEditing = false;
                        }
                        mDraggable.setLayoutParams(layoutParams1);
                        break;
                }
                return true;
            }
        });
    }

    public void reset() {
        LayoutParams layoutParams1 = (LayoutParams) mDraggable.getLayoutParams();
        layoutParams1.width = AppSocialGlobal.getScreenWidth(getContext());
        layoutParams1.leftMargin = 0;
        mDraggable.setLayoutParams(layoutParams1);
        mIsEditing = false;
    }
}
