package com.teamcircle.circlesdk.view;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.graphics.PointF;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;

import androidx.appcompat.widget.AppCompatImageView;

import com.teamcircle.circlesdk.model.CropPhoto;

public class TouchImageView extends AppCompatImageView {
    Context context;
    Matrix matrix;

    // We can be in one of these 3 states
    static final int NONE = 0;
    static final int DRAG = 1;
    static final int ZOOM = 2;
    int mode = NONE;

    // Remember some things for zooming
    PointF last = new PointF();
    PointF start = new PointF();
    float minScale = 0f;
    float maxScale = 3f;
    float[] m;

    int viewWidth, viewHeight;
    int bmWidth, bmHeight;
    static final int CLICK = 8;
    float saveScale = 1f;
    float origMatrixScale = 1f;
    protected float origWidth, origHeight;
    int oldMeasuredWidth, oldMeasuredHeight;
    boolean isResized = true;

    ScaleGestureDetector mScaleDetector;
    ImageCallback imageCallback;


    public TouchImageView(Context context) {
        super(context);
        init(context);
    }

    public TouchImageView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public interface ImageCallback {
        void onMoveFinish();
    }

    public void setImageCallback(ImageCallback callback) {
        imageCallback = callback;
    }

    public CropPhoto getCropPhoto() {
        matrix.getValues(m);
        float scale = m[Matrix.MSCALE_X];
        float transX = m[Matrix.MTRANS_X];
        float transY = m[Matrix.MTRANS_Y];
        float startX = -transX;
        float startY = -transY;
        float endX = startX + viewWidth;
        float endY = startY + viewHeight;
        startX = Math.max(0, startX / scale);
        startY = Math.max(0, startY / scale);
        endX = Math.min(endX / scale, origWidth / origMatrixScale);
        endY = Math.min(endY / scale, origHeight / origMatrixScale);
        Bitmap b = ((BitmapDrawable) getDrawable()).getBitmap();
        float originalOffsetY = ((float) viewHeight
                - (origMatrixScale * (float) bmHeight)) / 2f;
        float originalOffsetX = ((float) viewWidth
                - (origMatrixScale * (float) bmWidth)) / 2f;
//        Bitmap bitmap = Bitmap.createBitmap(b, (int) startX, (int) startY, (int) (endX - startX), (int) (endY - startY));
        return new CropPhoto(b, origMatrixScale, scale, transX, transY, originalOffsetX, originalOffsetY,
                startX, startY, (int) (endX - startX), (int) (endY - startY), bmWidth, bmHeight);
    }

    private void init(Context context) {
        super.setClickable(true);
        this.context = context;
        mScaleDetector = new ScaleGestureDetector(context, new ScaleListener());
        matrix = new Matrix();
        m = new float[9];
        setImageMatrix(matrix);
        setScaleType(ScaleType.MATRIX);

        setOnTouchListener(new OnTouchListener() {

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                mScaleDetector.onTouchEvent(event);
                PointF curr = new PointF(event.getX(), event.getY());

                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        last.set(curr);
                        start.set(last);
                        mode = DRAG;
                        break;

                    case MotionEvent.ACTION_MOVE:
                        if (mode == DRAG) {
                            float deltaX = curr.x - last.x;
                            float deltaY = curr.y - last.y;
                            if (origWidth * saveScale <= viewWidth) {
                                deltaX = 0;
                            }
                            if (origHeight * saveScale <= viewHeight) {
                                deltaY = 0;
                            }
                            matrix.postTranslate(deltaX, deltaY);
                            fixTrans();
                            last.set(curr.x, curr.y);
                        }
                        break;

                    case MotionEvent.ACTION_UP:
                        mode = NONE;
                        int xDiff = (int) Math.abs(curr.x - start.x);
                        int yDiff = (int) Math.abs(curr.y - start.y);
                        if (xDiff < CLICK && yDiff < CLICK) {
                            performClick();
                        }
                        if (imageCallback != null) {
                            imageCallback.onMoveFinish();
                        }
                        break;

                    case MotionEvent.ACTION_POINTER_UP:
                        mode = NONE;
                        break;
                }

                setImageMatrix(matrix);
                invalidate();
                return true; // indicate event was handled
            }

        });
    }

    private class ScaleListener extends
            ScaleGestureDetector.SimpleOnScaleGestureListener {
        @Override
        public boolean onScaleBegin(ScaleGestureDetector detector) {
            mode = ZOOM;
            return true;
        }

        @Override
        public boolean onScale(ScaleGestureDetector detector) {
            float mScaleFactor = detector.getScaleFactor();
            float origScale = saveScale;
            saveScale *= mScaleFactor;
            if (saveScale > maxScale) {
                saveScale = maxScale;
                mScaleFactor = maxScale / origScale;
            } else if (saveScale < minScale) {
                saveScale = minScale;
                mScaleFactor = minScale / origScale;
            }

            if (origWidth * saveScale <= viewWidth
                    || origHeight * saveScale <= viewHeight)
                matrix.postScale(mScaleFactor, mScaleFactor, viewWidth / 2,
                        viewHeight / 2);
            else
                matrix.postScale(mScaleFactor, mScaleFactor,
                        detector.getFocusX(), detector.getFocusY());

            fixTrans();
            return true;
        }
    }

    private void fixTrans() {
        matrix.getValues(m);
        float transX = m[Matrix.MTRANS_X];
        float transY = m[Matrix.MTRANS_Y];

        float fixTransX = getFixTrans(transX, viewWidth, origWidth * saveScale);
        float fixTransY = getFixTrans(transY, viewHeight, origHeight * saveScale);

        if (fixTransX != 0 || fixTransY != 0)
            matrix.postTranslate(fixTransX, fixTransY);
    }

    private float getFixTrans(float trans, float viewSize, float contentSize) {
        float minTrans, maxTrans;

        if (contentSize <= viewSize) {
            minTrans = 0;
            maxTrans = viewSize - contentSize;
        } else {
            minTrans = viewSize - contentSize;
            maxTrans = 0;
        }

        if (trans < minTrans)
            return -trans + minTrans;
        if (trans > maxTrans)
            return -trans + maxTrans;
        return 0;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        viewWidth = MeasureSpec.getSize(widthMeasureSpec);
        viewHeight = MeasureSpec.getSize(heightMeasureSpec);

        //
        // Rescales image on rotation
        //
        if (oldMeasuredHeight == viewWidth && oldMeasuredHeight == viewHeight
                || viewWidth == 0 || viewHeight == 0)
            return;
        oldMeasuredHeight = viewHeight;
        oldMeasuredWidth = viewWidth;

        setOriginalImageMatrix(0, 0, 0, 0);
    }

    public void setOriginalImageMatrix(int type, float setScale, float setOffsetX, float setOffsetY) {
        Drawable drawable = getDrawable();
        if (drawable == null || drawable.getIntrinsicWidth() == 0
                || drawable.getIntrinsicHeight() == 0 || viewWidth == 0 || viewHeight == 0)
            return;
        bmWidth = drawable.getIntrinsicWidth();
        bmHeight = drawable.getIntrinsicHeight();
        float scaleX = (float) viewWidth / (float) bmWidth;
        float scaleY = (float) viewHeight / (float) bmHeight;
        origMatrixScale = Math.max(scaleX, scaleY);
        if (minScale == 0) {
            float ratio = (float) bmWidth / bmHeight;
            if (ratio >= 4 / 3f) {
                minScale = (float) viewHeight * 3 / 4 / bmHeight / origMatrixScale;
            } else if (ratio >= 1) {
                minScale = (float) viewWidth / bmWidth / origMatrixScale;
            } else if (ratio > 3 / 4f) {
                minScale = (float) viewHeight / bmHeight / origMatrixScale;
            } else {
                minScale = (float) viewWidth * 3 / 4 / bmWidth / origMatrixScale;
            }
        }

        float redundantYSpace = ((float) viewHeight
                - (origMatrixScale * (float) bmHeight)) / 2f;
        float redundantXSpace = ((float) viewWidth
                - (origMatrixScale * (float) bmWidth)) / 2f;
        origWidth = viewWidth - 2 * redundantXSpace;
        origHeight = viewHeight - 2 * redundantYSpace;

        if (type == 0) {
            isResized = true;
            saveScale = 1;
            matrix.setScale(origMatrixScale, origMatrixScale);
            matrix.postTranslate(redundantXSpace, redundantYSpace);
            setImageMatrix(matrix);
        } else if (type == 1) {
            saveScale = setScale / origMatrixScale;
            matrix.setScale(setScale, setScale);
            matrix.postTranslate(setOffsetX, setOffsetY);
            setImageMatrix(matrix);
        } else if (type == 2) {
            saveScale = setScale;
            float scale = setScale * origMatrixScale;
            redundantYSpace = ((float) viewHeight
                    - (scale * (float) bmHeight)) / 2f;
            redundantXSpace = ((float) viewWidth
                    - (scale * (float) bmWidth)) / 2f;
            matrix.setScale(scale, scale);
            matrix.postTranslate(redundantXSpace, redundantYSpace);
            setImageMatrix(matrix);
        }
        fixTrans();
    }

    public void resize() {
        isResized = !isResized;
        if (isResized) {
            setOriginalImageMatrix(0, 0, 0, 0);
        } else {
            setOriginalImageMatrix(2, minScale, 0, 0);
        }
        if (imageCallback != null) {
            imageCallback.onMoveFinish();
        }
    }

    public void setMinScaleLimit(boolean flag) {
        if (flag) {
            minScale = 1;
        } else {
            float ratio = (float) bmWidth / bmHeight;
            if (ratio >= 4 / 3f) {
                minScale = (float) viewHeight * 3 / 4 / bmHeight / origMatrixScale;
            } else if (ratio >= 1) {
                minScale = (float) viewWidth / bmWidth / origMatrixScale;
            } else if (ratio > 3 / 4f) {
                minScale = (float) viewHeight / bmHeight / origMatrixScale;
            } else {
                minScale = (float) viewWidth * 3 / 4 / bmWidth / origMatrixScale;
            }
        }
        if (imageCallback != null) {
            imageCallback.onMoveFinish();
        }

    }
}