package com.renren.faceos;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.PixelFormat;
import android.graphics.Point;
import android.graphics.Rect;
import android.hardware.Camera;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.Gravity;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
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

import zeus.tracking.Face;
import zeus.tracking.FaceTracking;

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
    private int liveSize;
    private byte[] resultData;
    private int round = 50;

    boolean initTrack;
    boolean detectionState;
    String txt;
    long liveStartTime;
    private TimeoutDialog mTimeoutDialog;


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

        FrameLayout.LayoutParams cameraFL = new FrameLayout.LayoutParams(
                (int) (w * FaceDetectRoundView.SURFACE_RATIO), (int) (h * FaceDetectRoundView.SURFACE_RATIO),
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
    }

    private void initModel() {
        String sdPath = Environment.getExternalStorageDirectory().getAbsolutePath();
        faceTracker = new FaceTracking(sdPath + "/faceos/models");

        //选择活体动作
        live = new ArrayList<>();
        live.add("张嘴");
//        live.add("摇头");
//        live.add("眨眼");
        live.add("向左");
        live.add("向右");
        live.add("拍照");
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
        mPreviewWidth = point.x;
        mPreviewHeight = point.y;
        // Preview 768,432
        mPreviewRect.set(0, 0, mPreviewHeight, mPreviewWidth);

        mCameraParam.setPreviewSize(mPreviewWidth, mPreviewHeight);
        mCamera.setParameters(mCameraParam);

        try {
            mCamera.setPreviewDisplay(mSurfaceHolder);
            mCamera.stopPreview();
            mCamera.setErrorCallback(this);
            mCamera.setPreviewCallback(this);
            mCamera.startPreview();
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
            if (!initTrack) {
                detectionState = true;
                //初始化人脸追踪
                faceTracker.faceTrackingInit(data, mPreviewHeight, mPreviewWidth);
                initTrack = true;
                detectionState = false;
            } else {
                detectionState = true;
                //更新人脸数据
                faceTracker.update(data, mPreviewHeight, mPreviewWidth);
                List<Face> trackingInfo = faceTracker.getTrackingInfo();
                if (trackingInfo.size() > 0) {
//                  人脸位置
                    Face faceRect = trackingInfo.get(0);
                    int previewWidth = mPreviewRect.right / 2;
                    int previewHeight = mPreviewRect.bottom / 2;
                    int faceWidth = (faceRect.left + faceRect.right) / 2;
                    int faceHeight = (faceRect.top + faceRect.bottom) / 2;
                    //判断人脸中心点
                    if (Math.abs(faceHeight - previewWidth) < round &&
                            Math.abs(faceWidth - previewHeight) < round) {
                        //判断人脸是否正脸
                        if (faceRect.pitch > -10 && faceRect.pitch < 3 && faceRect.roll > -1 && faceRect.roll < 3) {
                            if (live.size() > 0) {
                                txt = "请" + live.get(0);
                                takePhoto(faceRect, data, mPreviewWidth, mPreviewHeight);
                                switch (live.get(0)) {
                                    case "张嘴":
                                        if (faceRect.monthState == 1) {
                                            live.remove("张嘴");
                                            liveStartTime = System.currentTimeMillis();
                                        }
                                        break;
                                    case "摇头":
                                        if (faceRect.shakeState == 1) {
                                            live.remove("摇头");
                                        }
                                        break;
                                    case "眨眼":
                                        if (faceRect.eyeState == 1 && faceRect.shakeState == 0) {
                                            live.remove("眨眼");
                                        }
                                        break;
                                    case "向左":
                                        if (faceRect.yaw > 7) {
                                            live.remove("向左");
                                        }
                                        break;
                                    case "向右":
                                        if (faceRect.yaw < -7) {
                                            live.remove("向右");
                                        }
                                        break;
                                    case "拍照":
                                        takePhoto(faceRect, data, mPreviewWidth, mPreviewHeight);
                                        break;
                                }
                                mFaceDetectRoundView.setTipTopText(txt);
                            } else {
                                if (resultData != null) {
                                    if (faceTracker != null) {
                                        faceTracker.releaseSession();
                                        faceTracker = null;
                                    }
                                    finish();
                                }
                            }
                        } else {
                            mFaceDetectRoundView.setTipTopText("请保持正脸");
                        }

                    } else {
                        mFaceDetectRoundView.setTipTopText("请将脸移入取景框");
                    }

                } else {
                    //无人脸
                    mFaceDetectRoundView.setTipTopText("请将脸移入取景框");
                    //采集超时
                    if (liveSize > live.size() && (System.currentTimeMillis() - liveStartTime > 10000)) {
                        showMessageDialog();
                    }
                }
                detectionState = false;
            }

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

    private void takePhoto(Face face, byte[] data, int width, int height) {
        try {
            //中心点偏移距离
            double distance = centerDistance(face);
            if (resultData == null && distance < 3 && face.pitch > -10 && face.pitch < 3
                    && face.yaw > -1 && face.yaw < 3 && face.roll > -1 && face.roll < 3) {
                live.remove("拍照");
                resultData = data;
                FastYUVtoRGB fastYUVtoRGB = new FastYUVtoRGB(this);
                Bitmap bitmap = fastYUVtoRGB.convertYUVtoRGB(data, width, height);
                File file = File.createTempFile("face", null, this.getCacheDir());
//                //保存人脸到SD卡
                FileUtils.saveFile(file, bitmap);
                Intent intent = new Intent();
                intent.putExtra("facePath", file.getAbsolutePath());
                setResult(1, intent);
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
            faceTracker.releaseSession();
            faceTracker = null;
        }
    }
}