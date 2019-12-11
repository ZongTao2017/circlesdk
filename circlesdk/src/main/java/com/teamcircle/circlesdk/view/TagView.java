package com.teamcircle.circlesdk.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PointF;
import android.graphics.Typeface;
import android.os.Handler;
import android.util.Log;
import android.view.MotionEvent;
import android.widget.FrameLayout;
import android.widget.ImageView;

import androidx.viewpager.widget.ViewPager;

import com.squareup.picasso.Picasso;
import com.teamcircle.circlesdk.R;
import com.teamcircle.circlesdk.helper.AppSocialGlobal;
import com.teamcircle.circlesdk.model.ProductData;
import com.teamcircle.circlesdk.model.TagData;

import java.util.ArrayList;

public class TagView extends FrameLayout {
    ViewPager mViewPager;
    Paint mPaint;
    Path mPath;
    int mWidth, mHeight;
    float mPercentX, mPercentY;
    boolean mTouchable;
    TagOnTouchCallback mCallback;
    boolean mIsClick;
    PointF mTouchPoint;
    PointF mTopPoint;
    Handler mHandler;
    float mImageWidth, mImageHeight;
    float mImageStartX, mImageStartY;
    Paint mTextPaint;
    Paint mPricePaint;
    String mText;
    String mPrice;
    ImageView mImageView;

    int mPadding, mImageSize, mTagWidth, mTagHeight, mTextSize;

    static final int PADDING = 30;
    static final int IMAGE_SIZE = 180;
    static final int TAG_WIDTH = 700;
    static final int TAG_HEIGHT = 240;
    static final int TEXT_SIZE = 48;
    static final int CLICK = 8;

    public TagView(Context context, ViewPager viewPager, TagData tagData, boolean touchable, float imageWidth, float imageHeight) {
        super(context);
        mViewPager = viewPager;
        mTouchable = touchable;
        mPercentX = tagData.percentX;
        mPercentY = tagData.percentY;
        mImageWidth = imageWidth;
        mImageHeight = imageHeight;

        mPadding = PADDING;
        mImageSize = IMAGE_SIZE;
        mTagWidth = TAG_WIDTH;
        mTagHeight = TAG_HEIGHT;
        mTextSize = TEXT_SIZE;

        mPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mPaint.setColor(Color.BLACK);
        mPaint.setAlpha(200);
        mPath = new Path();
        mTouchPoint = new PointF();
        mHandler = new Handler();
        mTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mTextPaint.setColor(Color.WHITE);
        if (AppSocialGlobal.textFontProductName != null) {
            mTextPaint.setTypeface(Typeface.createFromAsset(context.getAssets(),
                    AppSocialGlobal.textFontProductName));
        } else if (AppSocialGlobal.textFontRegular != null) {
            mTextPaint.setTypeface(Typeface.createFromAsset(context.getAssets(),
                    AppSocialGlobal.textFontRegular));
        }
        mPricePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mPricePaint.setColor(Color.GRAY);
        mTextPaint.setTextSize(mTextSize);
        mPricePaint.setTextSize(mTextSize);
        if (AppSocialGlobal.textFontProductPrice != null) {
            mPricePaint.setTypeface(Typeface.createFromAsset(context.getAssets(),
                    AppSocialGlobal.textFontProductPrice));
        } else if (AppSocialGlobal.textFontRegular != null) {
            mPricePaint.setTypeface(Typeface.createFromAsset(context.getAssets(),
                    AppSocialGlobal.textFontRegular));
        }

        mImageView = new ImageView(context);
        mImageView.setLayoutParams(new LayoutParams(mImageSize, mImageSize));
        mImageView.setBackgroundColor(getResources().getColor(R.color.whiteColor));
        addView(mImageView);

        ProductData productData = AppSocialGlobal.getInstance().getProductById(tagData.productId);
        if (productData != null) {
            mText = productData.name;
            if (productData.priceLow == productData.priceHigh) {
                mPrice = String.format("$%d", productData.priceLow / 100);
            } else {
                mPrice = String.format("$%d - $%d", productData.priceLow / 100, productData.priceHigh / 100);
            }
            String productImageUrl = productData.photos.get(0);
            AppSocialGlobal.loadImage(productImageUrl, mImageView);
        } else {

            mText = "";
            mPrice = "";
        }
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        mWidth = right - left;
        mHeight = bottom - top;
        mImageStartX = (mWidth - mImageWidth) / 2;
        mImageStartY = (mHeight - mImageHeight) / 2;
        if (mTopPoint == null) {
            mTopPoint = new PointF(mImageStartX + mImageWidth * mPercentX, mImageStartY + mImageHeight * mPercentY);
        }
    }

    @Override
    protected void dispatchDraw(Canvas canvas) {
        float x = mTopPoint.x;
        float y = mTopPoint.y;
        mPath.reset();
        mPath.moveTo(x, y);
        mPath.lineTo(x - mPadding, y + mPadding);
        mPath.lineTo(x - mTagWidth / 2 + mPadding, y + mPadding);
        mPath.arcTo(x - mTagWidth / 2, y + mPadding, x - mTagWidth / 2 + mPadding * 2, y + mPadding * 3, 270, -90, false);
        mPath.lineTo(x - mTagWidth / 2, y + mTagHeight);
        mPath.arcTo(x - mTagWidth / 2, y + mTagHeight - mPadding, x - mTagWidth / 2 + mPadding * 2, y + mTagHeight + mPadding, 180, -90, false);
        mPath.lineTo(x + mTagWidth / 2 - mPadding, y + mTagHeight + mPadding);
        mPath.arcTo(x + mTagWidth / 2 - mPadding * 2, y + mTagHeight - mPadding, x + mTagWidth / 2, y + mTagHeight + mPadding, 90, -90, false);
        mPath.lineTo(x + mTagWidth / 2, y + mPadding * 2);
        mPath.arcTo(x + mTagWidth / 2 - mPadding * 2, y + mPadding, x + mTagWidth / 2, y + mPadding * 3, 0, -90, false);
        mPath.lineTo(x + mPadding, y + mPadding);
        mPath.lineTo(x, y);
        canvas.drawPath(mPath, mPaint);

        ArrayList<String> textList = split(mText);
        float priceX = x - mTagWidth / 2 + mPadding * 2 + mImageSize;
        float priceY;
        if (textList.size() == 1) {
            priceY = y + mPadding * 1.5f + mTagHeight / 2;
        } else if (textList.size() == 2) {
            priceY = y + mPadding * 1.5f + 3 * mTagHeight / 4;
        } else {
            String text0 = textList.get(0);
            String text1 = textList.get(1);
            text1 = text1.substring(0, text1.length() - 3) + "...";
            textList.clear();
            textList.add(text0);
            textList.add(text1);
            priceX = x - mTagWidth / 2 + mPadding * 2 + mImageSize;
            priceY = y + mPadding * 1.5f + 3 * mTagHeight / 4;
        }

        for (int i = 0; i < textList.size(); i++) {
            canvas.drawText(textList.get(i), x - mTagWidth / 2 + mPadding * 2 + mImageSize, y + mPadding * 1.5f + (i + 1) * mTagHeight / 4, mTextPaint);
        }
        canvas.drawText(mPrice, priceX, priceY, mPricePaint);

        super.dispatchDraw(canvas);

        LayoutParams layoutParams = (LayoutParams) mImageView.getLayoutParams();
        layoutParams.topMargin = (int) (y + mPadding * 2);
        layoutParams.leftMargin = (int) (x - mTagWidth / 2 + mPadding);
        mImageView.setLayoutParams(layoutParams);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        int action = event.getActionMasked();
        switch (action) {
            case MotionEvent.ACTION_DOWN:
                mTouchPoint.set(event.getX(), event.getY());
                if (mTouchPoint.x > mTopPoint.x - mTagWidth / 2 &&
                        mTouchPoint.x < mTopPoint.x + mTagWidth / 2 &&
                        mTouchPoint.y > mTopPoint.y + mPadding &&
                        mTouchPoint.y < mTopPoint.y + mPadding + mTagHeight) {
                    bringToFront();
                    if (mTouchable) {
                        for (int i = 1; i <= 15; i++) {
                            final int count = i;
                            mHandler.postDelayed(new Runnable() {
                                @Override
                                public void run() {
                                    mPadding = (int) ((PADDING * 0.1) * count / 15 + PADDING);
                                    mImageSize = (int) ((IMAGE_SIZE * 0.1) * count / 15 + IMAGE_SIZE);
                                    mTagWidth = (int) ((TAG_WIDTH * 0.1) * count / 15 + TAG_WIDTH);
                                    mTagHeight = (int) ((TAG_HEIGHT * 0.1) * count / 15 + TAG_HEIGHT);
                                    invalidate();
                                }
                            }, i * 10);
                        }
                    }
                    mIsClick = true;
                    mHandler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            mIsClick = false;
                        }
                    }, 300);
                } else {
                    return false;
                }
                break;
            case MotionEvent.ACTION_MOVE:
                mViewPager.requestDisallowInterceptTouchEvent(true);
                float x = event.getX();
                float y = event.getY();
                float dx = x - mTouchPoint.x;
                float dy = y - mTouchPoint.y;
                if (Math.sqrt(dx * dx + dy * dy) > CLICK) {
                    mIsClick = false;
                }
                float topX = mTopPoint.x + dx;
                float topY = mTopPoint.y + dy;
                float startX = (mWidth - mImageWidth) / 2;
                float endX = (mWidth + mImageWidth) / 2;
                float startY = (mHeight - mImageHeight) / 2;
                float endY = (mHeight + mImageHeight) / 2;
                if (mTouchable && topX >= startX && topX <= endX && topY >= startY && topY <= endY) {
                    mTouchPoint.set(x, y);
                    mTopPoint.set(topX, topY);
                    invalidate();
                    Log.d("touchpoint", String.format("%f, %f",x,y));
                    Log.d("toppoint", String.format("%f, %f",topX,topY));
                }
                break;
            case MotionEvent.ACTION_UP:
                mViewPager.requestDisallowInterceptTouchEvent(false);
                if (mIsClick) {
                    if (mCallback != null) {
                        mCallback.onClick();
                    }
                } else {
                    mPercentX = (mTopPoint.x - mImageStartX) / mImageWidth;
                    mPercentY = (mTopPoint.y - mImageStartY) / mImageHeight;
                    if (mTouchable) {
                        for (int i = 1; i <= 15; i++) {
                            final int count = i;
                            mHandler.postDelayed(new Runnable() {
                                @Override
                                public void run() {
                                    mPadding = (int) (PADDING * 1.1 - (PADDING * 0.1) * count / 15);
                                    mImageSize = (int) (IMAGE_SIZE * 1.1 - (IMAGE_SIZE * 0.1) * count / 15);
                                    mTagWidth = (int) (TAG_WIDTH * 1.1 - (TAG_WIDTH * 0.1) * count / 15);
                                    mTagHeight = (int) (TAG_HEIGHT * 1.1 - (TAG_HEIGHT * 0.1) * count / 15);
                                    invalidate();
                                }
                            }, i * 10);
                        }
                    }
                    if (mCallback != null) {
                        mCallback.onMoveTo(mPercentX, mPercentY);
                    }
                }
                break;
        }
        return true;
    }

    public void setTagOnTouchCallback(TagOnTouchCallback callback) {
        mCallback = callback;
    }

    public interface TagOnTouchCallback {
        void onClick();
        void onMoveTo(float percentX, float percentY);
    }

    private ArrayList<String> split(String text) {
        ArrayList<String> result = new ArrayList<>();
        String[] ss = text.split(" ");
        String tmp = "";
        int index = 0;
        int textWidth = TAG_WIDTH - PADDING * 3 - IMAGE_SIZE;
        boolean flag = true;
        for (String s : ss) {
            tmp = tmp + s + " ";
            if (mTextPaint.measureText(tmp) <= textWidth) {
                index += s.length() + 1;
            } else {
                result.add(text.substring(0, index));
                result.addAll(split(text.substring(index)));
                flag = false;
                break;
            }
        }
        if (flag) {
            result.add(text.substring(0, index - 1));
        }
        return result;
    }
}
