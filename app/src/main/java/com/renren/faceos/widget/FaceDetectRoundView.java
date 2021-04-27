package com.renren.faceos.widget;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;

import com.renren.faceos.R;
import com.renren.faceos.utils.DensityUtils;


/**
 * 人脸检测区域View
 */
public class FaceDetectRoundView extends View {

    private static final String TAG = FaceDetectRoundView.class.getSimpleName();

    public static final float SURFACE_HEIGHT = 1000f;
    public static final float SURFACE_RATIO = 0.75f;
    public static final float WIDTH_SPACE_RATIO = 0.25f;
    public static final float HEIGHT_RATIO = 0.1f;
    public static final float HEIGHT_EXT_RATIO = 0.2f;
    // public static final int CIRCLE_SPACE = 5;
    public static final int PATH_SPACE = 16;
    public static final int PATH_SMALL_SPACE = 12;
    public static final int CIRCLE_LINE_WIDTH = 3;

    public static final int COLOR_BG = Color.parseColor("#f2f2f2");
    // public static final int COLOR_RECT = Color.parseColor("#FFFFFF");
    public static final int COLOR_ROUND = Color.parseColor("#FFA800");
    public static final int COLOR_CIRCLE_LINE = Color.parseColor("#CCCCCC");
    public static final int COLOR_CIRCLE_SELECT_LINE = Color.parseColor("#00BAF2");

    private Paint mBGPaint;
    private Paint mFaceRoundPaint;
    private Paint mCircleLinePaint;
    private Paint mCircleLineSelectPaint;
    private Rect mFaceRect;
    private Rect mFaceDetectRect;
    private Paint mTextSecondPaint;
    private Paint mTextTopPaint;

    private float mX;
    private float mY;
    private float mR;
    private int mTotalActiveCount;
    private int mSuccessActiveCount;
    private boolean mIsActiveLive;
    private String mTipSecondText;
    private String mTipTopText;
    private Bitmap liveSuccessBitmap;

    public FaceDetectRoundView(Context context, AttributeSet attrs) {
        super(context, attrs);

        setLayerType(View.LAYER_TYPE_SOFTWARE, null);

        // DisplayMetrics dm = context.getResources().getDisplayMetrics();
        // float pathSpace = DensityUtils.dip2px(context, PATH_SPACE);
        // float pathSmallSpace = DensityUtils.dip2px(context, PATH_SMALL_SPACE);
        float circleLineWidth = DensityUtils.dip2px(context, CIRCLE_LINE_WIDTH);

        mBGPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mBGPaint.setColor(COLOR_BG);
        mBGPaint.setStyle(Paint.Style.FILL);
        mBGPaint.setAntiAlias(true);
        mBGPaint.setDither(true);

        mFaceRoundPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mFaceRoundPaint.setColor(COLOR_ROUND);
        mFaceRoundPaint.setStyle(Paint.Style.FILL);
        mFaceRoundPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.CLEAR));
        mFaceRoundPaint.setAntiAlias(true);
        mFaceRoundPaint.setDither(true);

        mCircleLinePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mCircleLinePaint.setColor(COLOR_CIRCLE_LINE);
        mCircleLinePaint.setStrokeWidth(circleLineWidth);
        mCircleLinePaint.setStyle(Paint.Style.STROKE);
        mCircleLinePaint.setAntiAlias(true);
        mCircleLinePaint.setDither(true);

        mCircleLineSelectPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mCircleLineSelectPaint.setColor(COLOR_CIRCLE_SELECT_LINE);
        mCircleLineSelectPaint.setStrokeWidth(circleLineWidth);
        mCircleLineSelectPaint.setStyle(Paint.Style.STROKE);
        mCircleLineSelectPaint.setAntiAlias(true);
        mCircleLineSelectPaint.setDither(true);

        mTextSecondPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mTextSecondPaint.setColor(Color.parseColor("#666666"));
        mTextSecondPaint.setTextSize(DensityUtils.dip2px(getContext(), 16));
        mTextSecondPaint.setTextAlign(Paint.Align.CENTER);
        mTextSecondPaint.setAntiAlias(true);
        mTextSecondPaint.setDither(true);

        mTextTopPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mTextTopPaint.setColor(Color.parseColor("#316ffe"));
        mTextTopPaint.setTextSize(DensityUtils.dip2px(getContext(), 18));
        mTextTopPaint.setTextAlign(Paint.Align.CENTER);
        mTextTopPaint.setTypeface(Typeface.DEFAULT_BOLD);
        mTextTopPaint.setAntiAlias(true);
        mTextTopPaint.setDither(true);

        liveSuccessBitmap = BitmapFactory.decodeResource(getResources(), R.mipmap.ic_face_live_finished);
    }

    public void setProcessCount(int successActiveCount, int totalActiveCount) {
        mSuccessActiveCount = successActiveCount;
        mTotalActiveCount = totalActiveCount;
        postInvalidate();
    }

    public void setIsActiveLive(boolean isActiveLive) {
        mIsActiveLive = isActiveLive;
    }

    public void setTipTopText(String tipTopText) {
        mTipTopText = tipTopText;
        if (!TextUtils.isEmpty(tipTopText)) {
            invalidate();
        }
    }

    public void setTipSecondText(String tipSecondText) {
        mTipSecondText = tipSecondText;
        if (!TextUtils.isEmpty(tipSecondText)) {
            invalidate();
        }
    }

    public float getRound() {
        return mR;
    }

    public Rect getFaceRoundRect() {
        if (mFaceRect != null) {
            Log.e(TAG, mFaceRect.toString());
        }
        return mFaceRect;
    }

    public Rect getFaceDetectRectRect() {
        if (mFaceDetectRect != null) {
            Log.e(TAG, mFaceDetectRect.toString());
        }
        return mFaceDetectRect;
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        float canvasWidth = right - left;
        float canvasHeight = bottom - top;

        float x = canvasWidth / 2;
//        float y = (canvasHeight / 2) - ((canvasHeight / 2) * HEIGHT_RATIO);
//        float r = (canvasWidth / 2) - ((canvasWidth / 2) * WIDTH_SPACE_RATIO);


        float y = (canvasHeight / 2);
        float r = (canvasWidth / 2) - ((canvasWidth / 2) * WIDTH_SPACE_RATIO);
        if (mFaceRect == null) {
            //left top right bottom
            mFaceRect = new Rect((int) (x - r),
                    (int) (y - r),
                    (int) (x + r),
                    (int) (y + r));
        }
//        if (mFaceDetectRect == null) {
//            float hr = r + (r * HEIGHT_EXT_RATIO);
//            mFaceDetectRect = new Rect((int) (x - r),
//                    (int) (y - hr),
//                    (int) (x + r),
//                    (int) (y + hr));
        if (mFaceDetectRect == null) {
            float hr = r + (r * HEIGHT_EXT_RATIO);
            mFaceDetectRect = new Rect((int) (x - r),
                    (int) (y - hr),
                    (int) (x + r),
                    (int) (y + hr));
        }
        mX = x;
        mY = y;
        mR = r;
        Log.e(TAG, "mx " + x + " my " + y + " mr " + r);
    }

    @Override
    public void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        canvas.drawColor(Color.TRANSPARENT);
        canvas.drawPaint(mBGPaint);
        canvas.drawCircle(mX, mY, mR, mFaceRoundPaint);
        // 画文字
        if (!TextUtils.isEmpty(mTipSecondText)) {
            canvas.drawText(mTipSecondText, mX, mY - mR - 40 - 25 - 59, mTextSecondPaint);
        }
        if (!TextUtils.isEmpty(mTipTopText)) {
            canvas.drawText(mTipTopText, mX, mY - mR - 40 - 25 - 59 - 40, mTextTopPaint);
        }
        if (mTipTopText.equals("非常好")) {
            canvas.drawBitmap(liveSuccessBitmap, mX - 59 - 28, mY - mR - 40 - 25 - 59, mTextSecondPaint);
        }

        if (mIsActiveLive) {
            canvas.translate(mX, mY);
            // 画默认进度
            drawCircleLine(canvas);
            // 画成功进度
            drawSuccessCircleLine(canvas);
        }
    }

    // 画默认刻度线
    private void drawCircleLine(Canvas canvas) {
        canvas.save();
        canvas.rotate(-90);
        for (int j = 0; j < 360; j += 6) {
            canvas.drawLine(mR + 40, 0, mR + 40 + 25,
                    0, mCircleLinePaint);
            canvas.rotate(6);
        }
        canvas.restore();
    }

    // 画成功刻度线
    private void drawSuccessCircleLine(Canvas canvas) {
        int degree = (int) ((float) mSuccessActiveCount / (float) mTotalActiveCount * 360.0f);
//         Log.e(TAG, "selectDegree = " + degree);
        canvas.save();
        canvas.rotate(-90);
        for (int j = 0; j < degree; j += 6) {
            canvas.drawLine(mR + 40, 0, mR + 40 + 25,
                    0, mCircleLineSelectPaint);
            canvas.rotate(6);
        }
        canvas.restore();
    }

    public static Rect getPreviewDetectRect(int w, int pw, int ph) {
        float round = (w / 2) - ((w / 2) * WIDTH_SPACE_RATIO);
        float x = pw / 2;
        float y = (ph / 2) - ((ph / 2) * HEIGHT_RATIO);
        float r = (pw / 2) > round ? round : (pw / 2);
        float hr = r + (r * HEIGHT_EXT_RATIO);
        Rect rect = new Rect((int) (x - r),
                (int) (y - hr),
                (int) (x + r),
                (int) (y + hr));
        // Log.e(TAG, "FaceRoundView getPreviewDetectRect " + pw + "-" + ph + "-" + rect.toString());
        return rect;
    }

}