package com.renren.faceos;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.Point;
import android.graphics.PorterDuff;
import android.graphics.Rect;
import android.graphics.RectF;
import android.hardware.Camera;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.Gravity;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.TextureView;
import android.view.View;
import android.view.WindowManager;
import android.widget.FrameLayout;

import com.renren.faceos.utils.BrightnessUtils;
import com.renren.faceos.utils.CameraPreviewUtils;
import com.renren.faceos.utils.CameraUtils;
import com.renren.faceos.utils.FastYUVtoRGB;
import com.renren.faceos.utils.FileUtils;
import com.renren.faceos.widget.FaceDetectRoundView;
import com.renren.faceos.widget.TimeoutDialog;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import faceos.tracking.Face;
import faceos.tracking.FaceTracking;

public class FaceLivenessActivity extends Activity implements
        SurfaceHolder.Callback,
        Camera.PreviewCallback,
        Camera.ErrorCallback,
        TimeoutDialog.OnTimeoutDialogClickListener {

    public static final String TAG = FaceLivenessActivity.class.getSimpleName();

    // View
    protected View mRootView;
    protected FrameLayout mFrameLayout;
    protected SurfaceView mSurfaceView;
    protected SurfaceHolder mSurfaceHolder;
    protected FaceDetectRoundView mFaceDetectRoundView;

    // 显示Size
    private Rect mPreviewRect = new Rect();
    protected int mDisplayWidth = 0;
    protected int mDisplayHeight = 0;
    protected int mSurfaceWidth = 0;
    protected int mSurfaceHeight = 0;
    protected boolean mIsCreateSurface = false;
    protected volatile boolean mIsCompletion = false;

    int prev_cx;
    int prev_cy;

    // 显示Size

    // 相机
    protected Camera mCamera;
    protected Camera.Parameters mCameraParam;
    protected int mCameraId;
    protected int mPreviewWidth;
    protected int mPreviewHeight;
    protected int mPreviewDegree;
    private FaceTracking faceTracker;
    private List<String> live;
    private byte[] resultData;

    boolean initTrack;
    boolean detectionState;
    String txt;
    long liveStartTime;
    private TimeoutDialog mTimeoutDialog;
    private TextureView textureView;
    private int liveSize;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setScreenBright();
        //屏幕常亮
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.activity_liveness);

        initModel();
        DisplayMetrics dm = new DisplayMetrics();
        Display display = this.getWindowManager().getDefaultDisplay();
        display.getMetrics(dm);
        mDisplayWidth = dm.widthPixels;
        mDisplayHeight = dm.heightPixels;
        mRootView = findViewById(R.id.detect_root_layout);
        mFrameLayout = findViewById(R.id.detect_surface_layout);

        mSurfaceView = new SurfaceView(this);
        mSurfaceHolder = mSurfaceView.getHolder();
        mSurfaceHolder.setSizeFromLayout();
        mSurfaceHolder.addCallback(this);
        mSurfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);

        int w = mDisplayWidth;
        int h = mDisplayHeight;

//        FrameLayout.LayoutParams cameraFL = new FrameLayout.LayoutParams(
//                (int) (w * FaceDetectRoundView.SURFACE_RATIO), (int) (h * FaceDetectRoundView.SURFACE_RATIO),
//                Gravity.CENTER_VERTICAL | Gravity.CENTER_HORIZONTAL);

        Log.e(TAG, w + "  " + h);

        FrameLayout.LayoutParams cameraFL = new FrameLayout.LayoutParams(
                (int) (w), (int) (h),
                Gravity.CENTER_VERTICAL | Gravity.CENTER_HORIZONTAL);
        mSurfaceView.setLayoutParams(cameraFL);
        mFrameLayout.addView(mSurfaceView);

        findViewById(R.id.detect_close).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onBackPressed();
            }
        });

        mFaceDetectRoundView = findViewById(R.id.detect_face_round);
        mFaceDetectRoundView.setIsActiveLive(false);

        textureView = findViewById(R.id.textureView);

    }

    private void initModel() {

        String assetPath = "faceos";
        String sdcardPath = Environment.getExternalStorageDirectory()
                + File.separator + assetPath;
        FileUtils.copyFilesFromAssets(this, assetPath, sdcardPath);

        String sdPath = Environment.getExternalStorageDirectory().getAbsolutePath();
        faceTracker = new FaceTracking(sdPath + "/faceos/models/model.bin");

        //选择活体动作
        live = new ArrayList<>();
        live.add("正脸");
        live.add("张嘴");
        live.add("摇头");
        live.add("眨眼");
//        live.add("向左");
//        live.add("向右");
        liveSize = live.size();
    }

    /**
     * 设置屏幕亮度
     */
    private void setScreenBright() {
        int currentBright = BrightnessUtils.getScreenBrightness(this);
        BrightnessUtils.setBrightness(this, currentBright + 100);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mFaceDetectRoundView != null) {
            mFaceDetectRoundView.setTipTopText("请将脸移入取景框");
        }
        startPreview();
    }

    @Override
    public void onPause() {
        super.onPause();
        stopPreview();
    }

    private Camera open() {
        Camera camera;
        int numCameras = Camera.getNumberOfCameras();
        if (numCameras == 0) {
            return null;
        }

        int index = 0;
        while (index < numCameras) {
            Camera.CameraInfo cameraInfo = new Camera.CameraInfo();
            Camera.getCameraInfo(index, cameraInfo);
            if (cameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
                break;
            }
            index++;
        }

        if (index < numCameras) {
            camera = Camera.open(index);
            mCameraId = index;
        } else {
            camera = Camera.open(0);
            mCameraId = 0;
        }
        return camera;
    }

    private void stopPreview() {
        if (mCamera != null) {
            try {
                mCamera.setErrorCallback(null);
                mCamera.setPreviewCallback(null);
                mCamera.stopPreview();
            } catch (RuntimeException e) {
                e.printStackTrace();
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                CameraUtils.releaseCamera(mCamera);
                mCamera = null;
            }
        }
        if (mSurfaceHolder != null) {
            mSurfaceHolder.removeCallback(this);
        }
    }

    protected void startPreview() {
        if (mSurfaceView != null && mSurfaceView.getHolder() != null) {
            mSurfaceHolder = mSurfaceView.getHolder();
            mSurfaceHolder.addCallback(this);
        }

        if (mCamera == null) {
            try {
                mCamera = open();
            } catch (RuntimeException e) {
                e.printStackTrace();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        if (mCamera == null) {
            return;
        }
        if (mCameraParam == null) {
            mCameraParam = mCamera.getParameters();
        }

        mCameraParam.setPictureFormat(PixelFormat.JPEG);
        int degree = displayOrientation(this);
        mCamera.setDisplayOrientation(degree);
        // 设置后无效，camera.setDisplayOrientation方法有效
        mCameraParam.set("rotation", degree);
        mPreviewDegree = degree;

        Point point = CameraPreviewUtils.getBestPreview(mCameraParam,
                new Point(mDisplayWidth, mDisplayHeight));
        Log.e("TAG", point.x + "===" + point.y);
        mPreviewWidth = point.x;
        mPreviewHeight = point.y;
        // Preview 768,432
        mPreviewRect.set(0, 0, mPreviewHeight, mPreviewWidth);

        mCameraParam.setPreviewSize(mPreviewWidth, mPreviewHeight);
        mCamera.setParameters(mCameraParam);
        Log.e(TAG, mPreviewWidth + " size " + mPreviewHeight);
        try {
            mCamera.setPreviewDisplay(mSurfaceHolder);
            mCamera.stopPreview();
            mCamera.setErrorCallback(this);
            mCamera.setPreviewCallback(this);
            mCamera.startPreview();
//            mCamera.setFaceDetectionListener(new Camera.FaceDetectionListener() {
//                @Override
//                public void onFaceDetection(Camera.Face[] faces, Camera camera) {
//                    showFrame(faces);
//                }
//            });
//            mCamera.startFaceDetection();
        } catch (RuntimeException e) {
            e.printStackTrace();
            CameraUtils.releaseCamera(mCamera);
            mCamera = null;
        } catch (Exception e) {
            e.printStackTrace();
            CameraUtils.releaseCamera(mCamera);
            mCamera = null;
        }

    }

    private int displayOrientation(Context context) {
        WindowManager windowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        int rotation = windowManager.getDefaultDisplay().getRotation();
        int degrees = 0;
        switch (rotation) {
            case Surface.ROTATION_0:
                degrees = 0;
                break;
            case Surface.ROTATION_90:
                degrees = 90;
                break;
            case Surface.ROTATION_180:
                degrees = 180;
                break;
            case Surface.ROTATION_270:
                degrees = 270;
                break;
            default:
                degrees = 0;
                break;
        }
        int result = (0 - degrees + 360) % 360;
        if (Build.VERSION.SDK_INT >= 9) {
            Camera.CameraInfo info = new Camera.CameraInfo();
            Camera.getCameraInfo(mCameraId, info);
            if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
                result = (info.orientation + degrees) % 360;
                result = (360 - result) % 360;
            } else {
                result = (info.orientation - degrees + 360) % 360;
            }
        }
        return result;
    }

    @Override
    public void onError(int i, Camera camera) {

    }

    @Override
    public void onPreviewFrame(byte[] data, Camera camera) {
        if (mIsCompletion) {
            return;
        }

        if (!detectionState && faceTracker != null
                && mFaceDetectRoundView != null && mFaceDetectRoundView.getRound() > 0) {
            detectionState = true;
            if (!initTrack) {
                //初始化人脸追踪
                faceTracker.FaceTrackingInit(data, mPreviewHeight, mPreviewWidth);
                //faceTracker.FaceTrackingInit(data, mPreviewHeight, mPreviewWidth);
                initTrack = true;
            } else {
                //更新人脸数据
                faceTracker.Update(data, mPreviewHeight, mPreviewWidth, true);
                //faceTracker.update(data, mPreviewHeight, mPreviewWidth);
                List<Face> trackingInfo = faceTracker.getTrackingInfo();
                if (trackingInfo.size() > 0) {
//                  人脸位置
                    Face faceRect = trackingInfo.get(0);
//                    float faceCx = faceRect.center_x / faceTracker.ui_width;
//                    float faceCy = faceRect.center_y / faceTracker.ui_height;
//                    float faceWidthRef = faceRect.center_x / faceTracker.ui_width;
//                    float faceHeightRef = faceRect.center_y / faceTracker.ui_height;
//                    float faceCx = 1;
//                    float faceCy = 1;
//                    Log.e("debug", "pitch " + faceRect.pitch + "yaw " + faceRect.yaw + "roll " + faceRect.roll);
                    //Log.e(TAG, "faceDetectRectRect" + faceDetectRectRect.toString());
//                    Log.e("face x", faceCx + "");
//                    Log.e("face y", faceCy + "");
//                    Log.e("faceWidthRef", faceWidthRef + "");
                    //Log.d("faceWidthRef" , faceWidthRef+ "" );
//                    Log.e("CenterJ", (Math.abs(faceCx - 0.5) < 0.1 && Math.abs(faceCy - 0.5) < 0.1 && faceWidthRef > 0.3) + "");


//                    int width = textureView.getWidth();
//                    int height = textureView.getHeight();
//                        if (Math.abs(faceHeight - height) < 20 ||
//                                Math.abs(faceWidth - width) < 20) {
//                            mFaceDetectRoundView.setTipSecondText("");
//                            mFaceDetectRoundView.setTipTopText("请将脸部离远一点");
//                            detectionState = false;
//                            return;
//                        }
                    //判断人脸中心点
                    //判断人脸是否正脸
                    if (live.size() > 0) {
                        txt = "请" + live.get(0);
                        switch (live.get(0)) {
                            case "正脸":
                                takePhoto(faceRect, data, mPreviewWidth, mPreviewHeight);
                                liveStartTime = System.currentTimeMillis();
                                break;
                            case "张嘴":
                                if (faceRect.mouthState == 1) {
                                    live.remove("张嘴");
//
                                }
                                break;
                            case "眨眼":
                                if (faceRect.eyeState == 1 && faceRect.shakeState == 0) {
                                    live.remove("眨眼");
                                }
                                break;
                            case "摇头":
                                if (faceRect.shakeState == 1) {
                                    live.remove("摇头");
                                }
                                break;
//                            case "向左":
//                                if (faceRect.yaw > 7) {
//                                    live.remove("向左");
//                                }
//                                break;
//                            case "向右":
//                                if (faceRect.yaw < -7) {
//                                    live.remove("向右");
//                                }
//                                break;

                        }
                        if (!txt.contains("正脸")) {
                            mFaceDetectRoundView.setTipTopText(txt);
                        }
                    } else {
                        if (resultData != null) {
                            if (faceTracker != null) {
                                faceTracker = null;
                            }
                            Intent intent = new Intent();
                            intent.putExtra("facePath", takePhotoFile.getAbsolutePath());
                            setResult(1, intent);
                            finish();
                        }
                    }
                } else {
//                    mFaceDetectRoundView.setTipTopText("请将脸移入取景框");
                    //无人脸
                 mFaceDetectRoundView.setTipTopText("未检测到人脸");
                    //采集超时
//                    if (liveSize > live.size() && (System.currentTimeMillis() - liveStartTime > 10000)) {
//                        showMessageDialog();
//                    }
                }
                trackingInfo.clear();
            }
//            else{
//                //无人脸
//                mFaceDetectRoundView.setTipTopText("请将脸移入取景框");
//                //采集超时
//                if (liveSize > live.size() && (System.currentTimeMillis() - liveStartTime > 10000)) {
//                    showMessageDialog();
//                }
//            }
            detectionState = false;

        }
    }

    private void showMessageDialog() {
        mTimeoutDialog = new TimeoutDialog(this);
        mTimeoutDialog.setDialogListener(this);
        mTimeoutDialog.setCanceledOnTouchOutside(false);
        mTimeoutDialog.setCancelable(false);
        mTimeoutDialog.show();
        onPause();
    }

    @Override
    public void onRecollect() {
        if (mTimeoutDialog != null) {
            mTimeoutDialog.dismiss();
        }
        onResume();
    }

    @Override
    public void onReturn() {
        if (mTimeoutDialog != null) {
            mTimeoutDialog.dismiss();
        }
        finish();
    }

    private File takePhotoFile;

    private void takePhoto(Face face, byte[] data, int width, int height) {
        try {
            //中心点偏移距离
//            double distance = centerDistance(face);
            Log.d("debug", "pitch " + face.pitch + "yaw " + face.yaw + "roll " + face.roll);

            if (resultData == null && centerDistance(face) <= 3 && Math.abs(face.pitch) >= 0 && Math.abs(face.pitch) <= 3
                    && Math.abs(face.yaw) >= 0 && Math.abs(face.yaw) <= 3 && face.eyeState == 0 && Math.abs(face.roll) >= 0 && Math.abs(face.roll) <= 3) {
                live.remove("正脸");
                resultData = data;
                FastYUVtoRGB fastYUVtoRGB = new FastYUVtoRGB(this);
                Bitmap bitmap = fastYUVtoRGB.convertYUVtoRGB(data, width, height);
                takePhotoFile = File.createTempFile("face", null, this.getCacheDir());
//                //保存人脸到SD卡
                FileUtils.saveFile(takePhotoFile, bitmap);
            } else {
                if (face.roll >= 3) {
                    mFaceDetectRoundView.setTipTopText("请缓慢向左调整，保持正脸");
                } else if (face.roll <= -3) {
                    mFaceDetectRoundView.setTipTopText("请缓慢向右调整，保持正脸");
                } else if (Math.abs(face.pitch) >= 3) {
                    mFaceDetectRoundView.setTipTopText("请缓慢低头");
                } else if (face.yaw >= 0) {
                    mFaceDetectRoundView.setTipTopText("请缓慢向右转头");
                } else if (face.yaw <= 3) {
                    mFaceDetectRoundView.setTipTopText("请缓慢向左转头");
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

    }


    /**
     * 计算人脸中心点坐标
     *
     * @param face
     * @return
     */
    private double centerDistance(Face face) {
        int cx = (face.left + face.right) / 2;
        int cy = (face.top + face.bottom) / 2;
        int diff_x = cx - prev_cx;
        int diff_y = cy - prev_cy;
        //记录上一帧
        prev_cx = cx;
        prev_cy = cy;
        return Math.sqrt(diff_x * diff_x + diff_y * diff_y);
    }

    @Override
    public void surfaceCreated(SurfaceHolder surfaceHolder) {
        mIsCreateSurface = true;
    }

    @Override
    public void surfaceChanged(SurfaceHolder surfaceHolder, int format, int width, int height) {
        mSurfaceWidth = width;
        mSurfaceHeight = height;
        if (surfaceHolder.getSurface() == null) {
            return;
        }
        startPreview();
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder surfaceHolder) {
        mIsCreateSurface = false;
        if (faceTracker != null) {
            faceTracker = null;
        }
    }

    /**
     * 人脸框绘制
     *
     * @param rrFaces
     */

    Paint paint;

    {
        paint = new Paint();
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(9);
        paint.setColor(Color.WHITE);
    }

    Rect sysRect = null;

    private void showFrame(Camera.Face[] faces) {
        Canvas canvas = textureView.lockCanvas();
        if (canvas == null) return;
        canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);
        if (faces.length < 1) {
            // 清空canvas
            sysRect = null;
            mFaceDetectRoundView.setTipTopText("请将脸移入取景框");
//            mSurfaceHolder.unlockCanvasAndPost(canvas);
            return;
        }

        RectF rectF = new RectF(faces[0].rect);
        int viewWidth = textureView.getWidth();
        int viewHeight = textureView.getHeight();
        Matrix matrix = new Matrix();

//        这里使用的是后置摄像头就不用翻转。由于没有进行旋转角度的兼容，这里直接传系统调整的值
        prepareMatrix(matrix, false, 270, viewWidth, viewHeight);
        //坐标调整
        matrix.mapRect(rectF);

        //人脸翻转
        float left = viewWidth - rectF.right;
        float right = left + rectF.width();
        rectF.left = left;
        rectF.right = right;
        canvas.drawRect(rectF, paint);

        float cx = (rectF.left + rectF.right) / 2;
        float cy = (rectF.top + rectF.bottom) / 2;


        sysRect = new Rect((int) rectF.left, (int) rectF.top, (int) rectF.right, (int) rectF.bottom);
        textureView.unlockCanvasAndPost(canvas);

    }

    public void prepareMatrix(Matrix matrix, boolean mirror, int displayOrientation,
                              int viewWidth, int viewHeight) {
        // Need mirror for front camera.
        matrix.setScale(mirror ? -1 : 1, 1);
        // This is the value for android.hardware.Camera.setDisplayOrientation.
        matrix.postRotate(displayOrientation);
        // Camera driver coordinates range from (-1000, -1000) to (1000, 1000).
        // UI coordinates range from (0, 0) to (width, height)
        matrix.postScale(viewWidth / 2000f, viewHeight / 2000f);
        matrix.postTranslate(viewWidth / 2f, viewHeight / 2f);
    }
}