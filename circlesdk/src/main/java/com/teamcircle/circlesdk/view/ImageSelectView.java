package com.teamcircle.circlesdk.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.view.View;

import com.teamcircle.circlesdk.R;
import com.teamcircle.circlesdk.helper.AppSocialGlobal;

public class ImageSelectView extends View {
    Paint mRoundPaint, mBgPaint, mPaint, mTextPaint;
    int mNumber = 0;

    static final int TEXT_SIZE = 14;
    static final int RADIUS = 10;

    public ImageSelectView(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);

        mRoundPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mRoundPaint.setStyle(Paint.Style.STROKE);
        mRoundPaint.setStrokeWidth(2);
        mRoundPaint.setColor(Color.WHITE);

        mBgPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mBgPaint.setStyle(Paint.Style.FILL);
        mBgPaint.setColor(Color.WHITE);
        mBgPaint.setAlpha(102);

        mPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mPaint.setStyle(Paint.Style.FILL);
        mPaint.setColor(getContext().getResources().getColor(R.color.lightBlueColor));

        mTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mTextPaint.setStyle(Paint.Style.FILL);
        mTextPaint.setColor(Color.WHITE);
        mTextPaint.setTextSize(AppSocialGlobal.dpToPx(getContext(), TEXT_SIZE));
        if (AppSocialGlobal.textFontRegular != null) {
            mTextPaint.setTypeface(Typeface.createFromAsset(context.getAssets(),
                    AppSocialGlobal.textFontRegular));
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        int radius = AppSocialGlobal.dpToPx(getContext(), RADIUS);
        if (mNumber == 0) {
            canvas.drawCircle(radius, radius, radius, mBgPaint);
            canvas.drawCircle(radius, radius, radius - 1, mRoundPaint);
        } else {
            canvas.drawCircle(radius, radius, radius, mPaint);
            canvas.drawCircle(radius, radius, radius - 1, mRoundPaint);
            float w = mTextPaint.measureText(String.valueOf(mNumber));
            float h = mTextPaint.descent() + mTextPaint.ascent();
            canvas.drawText(String.valueOf(mNumber), radius - w / 2, radius - h / 2, mTextPaint);
        }
    }

    public void setNumber(int number) {
        mNumber = number;
        invalidate();
    }
}
