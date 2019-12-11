package com.teamcircle.circlesdk.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PointF;
import android.graphics.Typeface;
import android.os.Handler;
import android.util.Log;
import android.widget.FrameLayout;

import com.teamcircle.circlesdk.R;
import com.teamcircle.circlesdk.helper.AppSocialGlobal;
import com.teamcircle.circlesdk.model.TagData;

public class TagNumberView extends FrameLayout {

    Paint mPaint, mTextPaint;
    int mWidth, mHeight;
    float mPercentX, mPercentY;
    float mImageWidth, mImageHeight;
    float mImageStartX, mImageStartY;
    String mNumberString;
    PointF mCenterPoint;
    boolean mIsFocus;
    float mRadius, mTextSize;
    Handler mHandler;

    private static final int RADIUS = 8;
    private static final int TEXT_SIZE = 10;
    private static final int RADIUS_LARGE = 12;
    private static final int TEXT_SIZE_LARGE = 14;

    public TagNumberView(Context context, TagData tagData, int number, float imageWidth, float imageHeight) {
        super(context);

        mNumberString = Integer.toString(number);
        mPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mPaint.setColor(context.getResources().getColor(R.color.whiteColor));
        mTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mTextPaint.setColor(context.getResources().getColor(R.color.lightBlueColor));
        if (AppSocialGlobal.textFontBold != null) {
            mTextPaint.setTypeface(Typeface.createFromAsset(context.getAssets(),
                    AppSocialGlobal.textFontBold));
        } else if (AppSocialGlobal.textFontRegular != null) {
            Typeface tf = Typeface.createFromAsset(context.getAssets(),
                    AppSocialGlobal.textFontRegular);
            mTextPaint.setTypeface(Typeface.create(tf, Typeface.BOLD));
        } else {
            mTextPaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        }
        mImageWidth = imageWidth;
        mImageHeight = imageHeight;
        mPercentX = tagData.percentX;
        mPercentY = tagData.percentY;
        mIsFocus = false;
        mRadius = 0;
        mTextSize = 0;
        mHandler = new Handler();
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        mWidth = right - left;
        mHeight = bottom - top;
        mImageStartX = (mWidth - mImageWidth) / 2;
        mImageStartY = (mHeight - mImageHeight) / 2;
        if (mCenterPoint == null) {
            mCenterPoint = new PointF(mImageStartX + mImageWidth * mPercentX, mImageStartY + mImageHeight * mPercentY);
        }
    }

    @Override
    protected void dispatchDraw(Canvas canvas) {
        canvas.drawCircle(mCenterPoint.x, mCenterPoint.y, AppSocialGlobal.dpToPx(getContext(), mRadius), mPaint);
        mTextPaint.setTextSize(AppSocialGlobal.dpToPx(getContext(), mTextSize));
        float w = mTextPaint.measureText(mNumberString);
        float h = mTextPaint.descent() + mTextPaint.ascent();
        if (!mNumberString.equals("0")) {
            canvas.drawText(mNumberString, mCenterPoint.x - w / 2, mCenterPoint.y - h / 2, mTextPaint);
        }
    }

    public void setFocus(boolean focus) {
        if (mIsFocus && focus) {
            mIsFocus = false;
        } else {
            mIsFocus = focus;
        }
        if (mIsFocus) {
            mRadius = RADIUS_LARGE;
            mTextSize = TEXT_SIZE_LARGE;
        } else {
            mRadius = RADIUS;
            mTextSize = TEXT_SIZE;
        }
        invalidate();
        bringToFront();
    }

    public void animateCircle() {
        for (int i = 1; i <= 150; i++) {
            final int index = i;
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    mRadius = RADIUS_LARGE * index / 150f;
                    mTextSize = TEXT_SIZE_LARGE * index / 150f;
                    invalidate();
                    if (mRadius == RADIUS_LARGE) {
                        for (int j = 1; j <= 150; j++) {
                            final int index2 = j;
                            mHandler.postDelayed(new Runnable() {
                                @Override
                                public void run() {
                                    mRadius = RADIUS_LARGE - (RADIUS_LARGE - RADIUS) * index2 / 150f;
                                    mTextSize = TEXT_SIZE_LARGE - (TEXT_SIZE_LARGE - TEXT_SIZE) * index2 / 150f;
                                    invalidate();
                                }
                            }, j);
                        }
                    }
                }
            }, i);
        }
    }

    public void reset(boolean flag) {
        if (flag) {
            mRadius = RADIUS;
            mTextSize = TEXT_SIZE;
        } else {
            mRadius = 0;
            mTextSize = 0;
        }
        invalidate();

    }
}
