package com.teamcircle.circlesdk.view;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PointF;
import android.graphics.Typeface;
import android.media.MediaMetadataRetriever;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.teamcircle.circlesdk.R;
import com.teamcircle.circlesdk.helper.AppSocialGlobal;
import com.teamcircle.circlesdk.model.MessageEvent;
import com.teamcircle.circlesdk.model.VideoData;

import org.greenrobot.eventbus.EventBus;

import java.util.ArrayList;

public class TrimVideoView extends FrameLayout {
    private VideoData videoData;
    private ArrayList<ImageView> frames;
    private int duration;
    private LinearLayout layout;
    private ScaleView scaleView;
    private DragView dragView;
    private static int margin = 20;
    private Bitmap[] mVideoFrames;
    private int frameNumber;
    private int scrollPositionX = 0;
    private int unitWidth;
    private float leftCursorX, rightCursorX;

    @SuppressLint("ClickableViewAccessibility")
    public TrimVideoView(@NonNull final Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);

        videoData = AppSocialGlobal.getInstance().tmp_video;
        duration = videoData.duration;

        LayoutParams layoutParams = new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);

        ObservableScrollView scrollView = new ObservableScrollView(context);
        scrollView.setLayoutParams(layoutParams);
        scrollView.setHorizontalScrollBarEnabled(false);
        addView(scrollView);

        scrollView.setOnScrollListener(new ObservableScrollView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(ObservableScrollView view, int scrollState) {
                if (scrollState == ObservableScrollView.OnScrollListener.SCROLL_STATE_IDLE) {
                    Log.d("scrollview", String.format("%d", scrollPositionX / unitWidth));
                    updateVideoTime();
                }
            }

            @Override
            public void onScroll(ObservableScrollView view, boolean isTouchScroll, int l, int t, int oldl, int oldt) {
                scrollPositionX = l;
            }
        });

        scrollView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch( View v, MotionEvent event ) {
                switch ( event.getAction( ) ) {
                    case MotionEvent.ACTION_SCROLL:
                    case MotionEvent.ACTION_MOVE:
                    case MotionEvent.ACTION_DOWN:
                        break;
                    case MotionEvent.ACTION_CANCEL:
                    case MotionEvent.ACTION_UP:
                        Log.d("scrollview", String.format("%d", scrollPositionX / unitWidth));
                        break;
                }
                return false;
            }
        });

        LinearLayout linearLayout = new LinearLayout(context);
        linearLayout.setLayoutParams(new HorizontalScrollView.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.MATCH_PARENT));
        linearLayout.setOrientation(LinearLayout.VERTICAL);
        scrollView.addView(linearLayout);

        layout = new LinearLayout(context);
        linearLayout.addView(layout);

        scaleView = new ScaleView(context);
        linearLayout.addView(scaleView);

        dragView = new DragView(context);
        dragView.setLayoutParams(new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        addView(dragView);

        this.post(new Runnable() {
            @Override
            public void run() {
                int width = AppSocialGlobal.getScreenWidth(context);
                int height = getHeight();
                LinearLayout.LayoutParams layoutParams = (LinearLayout.LayoutParams) layout.getLayoutParams();
                layoutParams.height = height / 3;
                int padding = AppSocialGlobal.dpToPx(getContext(), margin);
                if (duration <= 30 * 1000) {
                    layoutParams.width = width;
                } else {
                    layoutParams.width = (width - padding * 2) * duration / (30 * 1000) + padding * 2;
                }
                layout.setPadding(padding, 0, padding, 0);
                layout.setLayoutParams(layoutParams);
                layout.setBackgroundColor(getResources().getColor(R.color.grayColor));

                LinearLayout.LayoutParams layoutParams2 = (LinearLayout.LayoutParams) scaleView.getLayoutParams();
                layoutParams2.height = height * 2 / 3;
                if (duration <= 30 * 1000) {
                    layoutParams2.width = width;
                } else {
                    layoutParams2.width = (width - padding * 2) * duration / (30 * 1000) + padding * 2;
                }
                scaleView.setLayoutParams(layoutParams2);

                getVideoFrames(layoutParams.width - padding * 2, layoutParams.height);

                unitWidth = (int) ((layoutParams.width - padding * 2) / (duration / 1000f) + 0.5);
                dragView.setUnitWidth(unitWidth);
                scaleView.setUnitWidth(unitWidth);
            }
        });
    }

    private void updateVideoTime() {
        int start = (int) ((leftCursorX - AppSocialGlobal.dpToPx(getContext(), margin)) / unitWidth) + scrollPositionX / unitWidth;
        int end = (int) ((rightCursorX - leftCursorX) / unitWidth) + start;
        Log.d("videotimestart", String.format("%d", start));
        Log.d("videotimeend", String.format("%d", end));
        AppSocialGlobal.getInstance().tmp_video.start = start * 1000;
        AppSocialGlobal.getInstance().tmp_video.end = end * 1000;
        EventBus.getDefault().post(new MessageEvent(MessageEvent.MessageEventType.TRIM_VIDEO));
    }

    private class DragView extends View {
        private PointF mTouchPoint;
        private Paint cursorPaint, cursorCirclePaint, bgPaint;
        private float cursorWidth = 60, cursorHeight, cursorMin, cursorMax;
        private int unitWidth;
        private int moveType; // 1:left 2:right 3:other

        public DragView(Context context) {
            super(context);
            mTouchPoint = new PointF();
            cursorPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            cursorPaint.setColor(getResources().getColor(R.color.whiteColor));
            cursorPaint.setStyle(Paint.Style.STROKE);
            cursorPaint.setStrokeWidth(6);
            cursorCirclePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            cursorCirclePaint.setColor(getResources().getColor(R.color.whiteColor));
            cursorCirclePaint.setStyle(Paint.Style.FILL);
            bgPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            bgPaint.setColor(getResources().getColor(R.color.blackColor));
            bgPaint.setAlpha(205);
            bgPaint.setStyle(Paint.Style.FILL);
        }

        public void setUnitWidth(int unitWidth) {
            this.unitWidth = unitWidth;
        }

        @Override
        protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
            super.onLayout(changed, left, top, right, bottom);
            cursorHeight = (bottom - top) / 3;
            cursorMin = left + AppSocialGlobal.dpToPx(getContext(), margin);
            cursorMax = right - AppSocialGlobal.dpToPx(getContext(), margin);
            leftCursorX = cursorMin;
            rightCursorX = cursorMax;
        }

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            canvas.drawRect(cursorMin - AppSocialGlobal.dpToPx(getContext(), margin), 0, leftCursorX, cursorHeight, bgPaint);
            canvas.drawRect(rightCursorX, 0, cursorMax + AppSocialGlobal.dpToPx(getContext(), margin), cursorHeight, bgPaint);
            canvas.drawLine(leftCursorX, 0, leftCursorX, cursorHeight, cursorPaint);
            canvas.drawCircle(leftCursorX, cursorHeight / 2, cursorWidth / 2, cursorCirclePaint);
            canvas.drawLine(rightCursorX, 0, rightCursorX, cursorHeight, cursorPaint);
            canvas.drawCircle(rightCursorX, cursorHeight / 2, cursorWidth / 2, cursorCirclePaint);
        }

        @Override
        public boolean onTouchEvent(MotionEvent event) {
            int action = event.getAction();
            switch (action) {
                case MotionEvent.ACTION_DOWN:
                    mTouchPoint.set(event.getX(), event.getY());
                    return checkTouchType();
                case MotionEvent.ACTION_MOVE:
                    switch (moveType) {
                        case 1:
                            leftCursorX += event.getX() - mTouchPoint.x;
                            leftCursorX = Math.min(Math.max(leftCursorX, cursorMin), rightCursorX - unitWidth);
                            break;
                        case 2:
                            rightCursorX += event.getX() - mTouchPoint.x;
                            rightCursorX = Math.max(Math.min(rightCursorX, cursorMax), leftCursorX + unitWidth);
                            break;
                    }
                    mTouchPoint.set(event.getX(), event.getY());
                    invalidate();
                    break;
                case MotionEvent.ACTION_CANCEL:
                case MotionEvent.ACTION_UP:
                    updateVideoTime();
                    break;
            }

            return true;
        }

        private boolean checkTouchType() {
            moveType = 3;
            if (mTouchPoint.x >= leftCursorX - cursorWidth / 2f
                    && mTouchPoint.x <= leftCursorX + cursorWidth / 2f
                    && mTouchPoint.y <= cursorHeight) {
                moveType = 1;
                return true;
            }
            if (mTouchPoint.x >= rightCursorX - cursorWidth / 2f
                    && mTouchPoint.x <= rightCursorX + cursorWidth / 2f
                    && mTouchPoint.y <= cursorHeight) {
                moveType = 2;
                return true;
            }
            return false;
        }
    }

    private class ScaleView extends View {
        private int width, height, unitWidth;
        private Paint paint, textPaint;

        public ScaleView(Context context) {
            super(context);
            paint = new Paint(Paint.ANTI_ALIAS_FLAG);
            paint.setColor(getResources().getColor(R.color.grayColor));
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(2);

            textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            textPaint.setTextSize(40);
            textPaint.setColor(getResources().getColor(R.color.grayColor));
            if (AppSocialGlobal.textFontRegular != null) {
                textPaint.setTypeface(Typeface.createFromAsset(context.getAssets(),
                        AppSocialGlobal.textFontRegular));
            }
        }

        public void setUnitWidth(int unitWidth) {
            this.unitWidth = unitWidth;
            invalidate();
        }

        @Override
        protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
            width = right - left;
            height = bottom - top;
        }

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            for (int i = 0; i <= videoData.duration / 1000 + 1; i++) {
                int x = AppSocialGlobal.dpToPx(getContext(), margin) + i * unitWidth;
                if (videoData.duration < 20 * 1000) {
                    if (i % 5 == 0) {
                        int minute = i / 60;
                        int second = i % 60;
                        String text;
                        if (minute == 0) {
                            if (second < 10) {
                                text = String.format(":0%d", second);
                            } else {
                                text = String.format(":%d", second);
                            }
                        } else {
                            if (second < 10) {
                                text = String.format("%d:0%d", minute, second);
                            } else {
                                text = String.format("%d:%d", minute, second);
                            }
                        }
                        float w = textPaint.measureText(text);
                        float h = textPaint.descent() + textPaint.ascent();
                        canvas.drawText(text, x - w / 2, height / 2f + h * 2, textPaint);
                        canvas.drawLine(x, height / 2, x, height, paint);
                    } else {
                        canvas.drawLine(x, height * 3 / 4, x, height, paint);
                    }
                } else {
                    if (i % 10 == 0) {
                        int minute = i / 60;
                        int second = i % 60;
                        String text;
                        if (minute == 0) {
                            if (second < 10) {
                                text = String.format(":0%d", second);
                            } else {
                                text = String.format(":%d", second);
                            }
                        } else {
                            if (second < 10) {
                                text = String.format("%d:0%d", minute, second);
                            } else {
                                text = String.format("%d:%d", minute, second);
                            }
                        }
                        float w = textPaint.measureText(text);
                        float h = textPaint.descent() + textPaint.ascent();
                        canvas.drawText(text, x - w / 2, height / 2f + h * 2, textPaint);
                        canvas.drawLine(x, height / 2, x, height, paint);
                    } else {
                        canvas.drawLine(x, height * 3 / 4, x, height, paint);
                    }
                }
            }
        }
    }

    private void getVideoFrames(int width, int height) {
        frames = new ArrayList<>();
        frameNumber = (int) ((width / (height / 2f)));
        final float frameHeight = height;
        final float frameWidth = (float) width / frameNumber;
        mVideoFrames = new Bitmap[frameNumber];
        frames = new ArrayList<>();
        for (int i = 0; i < frameNumber; i++) {
            ImageView imageView = new ImageView(getContext());
            LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams((int) frameWidth + 1, (int) frameHeight);
            imageView.setLayoutParams(layoutParams);
            layout.addView(imageView);
            frames.add(imageView);
        }
        final MediaMetadataRetriever retriever = new MediaMetadataRetriever();
        if (videoData == null) return;
        retriever.setDataSource(videoData.videoUrl);

        for (int i = 0; i < frameNumber; i++) {
            final int index = i;
            new Thread(new Runnable() {
                @Override
                public void run() {
                    int timeInSec = index * videoData.duration / 1000 / frameNumber;
                    Bitmap bitmap = retriever.getFrameAtTime(timeInSec * 1000000);
                    float w = bitmap.getWidth();
                    float h = bitmap.getHeight();
                    float x = w * videoData.startPercentX;
                    float y = h * videoData.startPercentY;
                    w = w * (videoData.endPercentX - videoData.startPercentX);
                    h = h * (videoData.endPercentY - videoData.startPercentY);
                    if (w / h > frameWidth / frameHeight) {
                        x = x + (w - h * frameWidth / frameHeight) / 2;
                        w = h * frameWidth / frameHeight;
                    } else {
                        y = y + (h - w * frameHeight / frameWidth) / 2;
                        h = w * frameHeight / frameWidth;
                    }
                    bitmap = Bitmap.createBitmap(bitmap, (int)x, (int)y, (int)w, (int)h);
                    Context context = getContext();
                    if (context != null) {
                        final Bitmap b = Bitmap.createScaledBitmap(bitmap, (int) frameWidth + 1, (int) frameHeight, false);
                        mVideoFrames[index] = b;
                    }
                    bitmap.recycle();
                    Activity activity = (Activity) getContext();
                    if (activity != null) {
                        activity.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                ImageView imageView = frames.get(index);
                                if (imageView.getDrawable() == null && mVideoFrames[index] != null) {
                                    imageView.setImageBitmap(mVideoFrames[index]);
                                }
                            }
                        });
                    }
                }
            }).start();
        }
    }
}
