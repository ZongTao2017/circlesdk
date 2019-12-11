package com.teamcircle.circlesdk.view;

import android.content.Context;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;

import androidx.core.content.ContextCompat;

import com.teamcircle.circlesdk.R;
import com.teamcircle.circlesdk.helper.AppSocialGlobal;

import java.util.ArrayList;

public class ViewPagerIndicator extends LinearLayout {
    private ArrayList<View> mDots;
    private int mCurrent = 0;

    public ViewPagerIndicator(Context context, int count) {
        super(context);
        mDots = new ArrayList<>();
        setOrientation(HORIZONTAL);
        setGravity(Gravity.CENTER);
        setDotsCount(count);
    }

    public ViewPagerIndicator(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
        mDots = new ArrayList<>();
        setOrientation(HORIZONTAL);
        setGravity(Gravity.CENTER);
    }

    public void setDotsCount(int count) {
        removeAllViews();
        mDots.clear();
        mCurrent = 0;
        for (int i = 0; i < count; i++) {
            View dot = new View(getContext());
            int size = AppSocialGlobal.dpToPx(getContext(), 5);
            LayoutParams layoutParams = new LayoutParams(size, size);
            if (i > 0) {
                layoutParams.leftMargin = size;
            }
            dot.setLayoutParams(layoutParams);
            addView(dot);
            mDots.add(dot);
        }
        setDotsColor();
    }

    public void setCurrent(int current) {
        mCurrent = current;
        setDotsColor();
    }

    private void setDotsColor() {
        if (mDots != null) {
            for (int i = 0; i < mDots.size(); i++) {
                View dot = mDots.get(i);
                LayoutParams layoutParams = (LayoutParams) dot.getLayoutParams();
                if (i == mCurrent) {
                    layoutParams.width = AppSocialGlobal.dpToPx(getContext(), 10);
                    dot.setBackground(ContextCompat.getDrawable(getContext(), R.drawable.dot_blue));
                } else {
                    layoutParams.width = AppSocialGlobal.dpToPx(getContext(), 5);
                    dot.setBackground(ContextCompat.getDrawable(getContext(), R.drawable.dot_white));
                }
                dot.setLayoutParams(layoutParams);
            }
        }
    }
}
