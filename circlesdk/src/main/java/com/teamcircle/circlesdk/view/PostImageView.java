package com.teamcircle.circlesdk.view;

import android.content.Context;
import android.graphics.Matrix;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import androidx.appcompat.widget.AppCompatImageView;

import com.pedromassango.doubleclick.DoubleClick;
import com.pedromassango.doubleclick.DoubleClickListener;

public class PostImageView extends AppCompatImageView {
    Matrix matrix;
    float[] m;

    int viewWidth, viewHeight;
    protected float origWidth, origHeight;
    Context context;
    float lastTouchX, lastTouchY;
    boolean onMeasureFinishCalled = false;
    boolean isFullWidth = false;

    ImageCallback imageCallback = null;

    public PostImageView(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
        init(context);
    }

    public void setIsFullWidth(boolean isFullWidth) {
        this.isFullWidth = isFullWidth;
    }

    public interface ImageCallback {
        void onClick(float percentX, float percentY);
        void onDoubleClick();
        void onMeasureFinish(float width, float height);
    }

    public void setImageCallback(ImageCallback callback) {
        imageCallback = callback;
    }

    private void init(Context context) {
        setClickable(true);
        this.context = context;
        matrix = new Matrix();
        m = new float[9];
        setImageMatrix(matrix);
        setScaleType(ScaleType.MATRIX);

        setOnClickListener(new DoubleClick(new DoubleClickListener() {
            @Override
            public void onSingleClick(View view) {
                matrix.getValues(m);
                float transX = m[Matrix.MTRANS_X];
                float transY = m[Matrix.MTRANS_Y];
                float percentX = (lastTouchX - transX) / origWidth;
                float percentY = (lastTouchY - transY) / origHeight;

                if (imageCallback != null && percentX >= 0 && percentX <= 1 && percentY >= 0 && percentY <= 1) {
                    imageCallback.onClick(percentX, percentY);
                }
            }

            @Override
            public void onDoubleClick(View view) {
                if (imageCallback != null) {
                    imageCallback.onDoubleClick();
                }
            }
        }));

        setOnTouchListener(new OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getActionMasked() == MotionEvent.ACTION_DOWN) {
                    lastTouchX = event.getX();
                    lastTouchY = event.getY();
                }
                return false;
            }
        });
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        viewWidth = MeasureSpec.getSize(widthMeasureSpec);
        viewHeight = MeasureSpec.getSize(heightMeasureSpec);

        if (viewWidth == 0 || viewHeight == 0)
            return;

        if (!onMeasureFinishCalled) {
            setOriginalImageMatrix();
        }
    }

    public void setOriginalImageMatrix() {
        if (viewWidth == 0 || viewHeight == 0)
            return;

        Drawable drawable = getDrawable();
        if (drawable == null || drawable.getIntrinsicWidth() == 0
                || drawable.getIntrinsicHeight() == 0)
            return;
        int bmWidth = drawable.getIntrinsicWidth();
        int bmHeight = drawable.getIntrinsicHeight();
        float scaleX = (float) viewWidth / (float) bmWidth;
        float scaleY = (float) viewHeight / (float) bmHeight;
        float scale;
        if (isFullWidth) {
            scale = scaleX;
        } else {
            scale = Math.min(scaleX, scaleY);
        }
        matrix.setScale(scale, scale);

        // Center the image
        float redundantYSpace = (float) viewHeight
                - (scale * (float) bmHeight);
        float redundantXSpace = (float) viewWidth
                - (scale * (float) bmWidth);
        redundantYSpace /= (float) 2;
        redundantXSpace /= (float) 2;

        matrix.postTranslate(redundantXSpace, redundantYSpace);

        origWidth = viewWidth - 2 * redundantXSpace;
        origHeight = viewHeight - 2 * redundantYSpace;
        setImageMatrix(matrix);

        if (imageCallback != null && !onMeasureFinishCalled) {
            onMeasureFinishCalled = true;
            imageCallback.onMeasureFinish(origWidth, origHeight);
        }
    }
}
