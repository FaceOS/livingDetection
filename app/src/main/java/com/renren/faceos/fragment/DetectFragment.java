package com.renren.faceos.fragment;

import android.content.Context;
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
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.SystemClock;
import android.support.annotation.Nullable;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.FrameLayout;

import com.renren.faceos.MainActivity;
import com.renren.faceos.R;
import com.renren.faceos.base.BaseFragment;
import com.renren.faceos.utils.BrightnessUtils;
import com.renren.faceos.utils.CameraPreviewUtils;
import com.renren.faceos.utils.CameraUtils;
import com.renren.faceos.utils.FastYUVtoRGB;
import com.renren.faceos.utils.FileUtils;
import com.renren.faceos.utils.RotateNV21Utils;
import com.renren.faceos.utils.SensorManagerHelper;
import com.renren.faceos.widget.FaceDetectRoundView;
import com.renren.faceos.widget.TimeoutDialog;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import faceos.tracking.Face;
import faceos.tracking.FaceTracking;


public class DetectFragment extends BaseFragment implements
        SurfaceHolder.Callback,
        Camera.PreviewCallback,
        Camera.ErrorCallback,
        TimeoutDialog.OnTimeoutDialogClickListener {

    public static final String TAG = DetectFragment.class.getSimpleName();
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
    long liveStartTime = System.currentTimeMillis();
    private TimeoutDialog mTimeoutDialog;
    private TextureView textureView;
    private int liveSize;
    private boolean flag;
    private String[] actionEnd = {"眨眨眼"};
    private String[] action = {"缓慢摇头", "缓慢抬头", "张张嘴"};
    private SensorManagerHelper sensorHelper;
    private long sensorTime;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_detect, container, false);
        setScreenBright();
        //屏幕常亮
        getActivity().getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        initModel();
        DisplayMetrics dm = new DisplayMetrics();
        Display display = getActivity().getWindowManager().getDefaultDisplay();
        display.getMetrics(dm);
        mDisplayWidth = dm.widthPixels;
        mDisplayHeight = dm.heightPixels;
        mRootView = view.findViewById(R.id.detect_root_layout);
        mFrameLayout = view.findViewById(R.id.detect_surface_layout);

        mSurfaceView = new SurfaceView(getContext());
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

        mFaceDetectRoundView = view.findViewById(R.id.detect_face_round);
        mFaceDetectRoundView.setIsActiveLive(true);

        textureView = view.findViewById(R.id.textureView);
        //加速度检测，防止手机摇晃通过动作
        sensorHelper = new SensorManagerHelper(getContext());
        sensorHelper.setOnShakeListener(new SensorManagerHelper.OnShakeListener() {
            @Override
            public void onShake() {
                sensorTime = System.currentTimeMillis();
            }
        });

        return view;
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mFaceDetectRoundView.setProcessCount(0, live.size());
        //计算活体采集超时
        new Thread(new Runnable() {
            @Override
            public void run() {
                while (true) {
                    if (flag)
                        break;
                    SystemClock.sleep(1000);
                    //采集超时
                    if (System.currentTimeMillis() - liveStartTime > 30000) {
                        if (getActivity() != null)
                            getActivity().runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    showMessageDialog();
                                }
                            });
                        break;
                    }
                }
            }
        }).start();
    }

    private void initModel() {
        String sdPath = getMainActivity().getFilesDir().getPath();
        faceTracker = new FaceTracking(sdPath + "/faceos/models/model.bin");

        //选择活体动作
        live = new ArrayList<>();
        //随机动作
        Random random = new Random();
        List<Integer> indexArray = new ArrayList<>();
        while (indexArray.size() != 3) {
            int index = random.nextInt(3);
            if (!indexArray.contains(index))
                indexArray.add(index);
        }
        for (Integer index : indexArray) {
            live.add(action[index]);
        }
        live.remove(2);
        live.add(2, actionEnd[0]);
//        live.add(0, action[0]);
        liveSize = live.size();

    }


    /**
     * 设置屏幕亮度
     */
    private void setScreenBright() {
        getMainActivity().currentBright = BrightnessUtils.getScreenBrightness(getActivity());
        BrightnessUtils.setBrightness(getActivity(), getMainActivity().currentBright + 100);
    }

    @Override
    public void onResume() {
        super.onResume();
        if (mFaceDetectRoundView != null) {
            mFaceDetectRoundView.setTipTopText("把脸移动到框内");
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
        int degree = displayOrientation(getContext());
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
        Log.e(TAG, mPreviewWidth + " mPreviewRect " + mPreviewHeight);
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

    float tmp;
    boolean faceCheck;
    long haveFaceTime;

    @Override
    public void onPreviewFrame(byte[] data, Camera camera) {
        if (mIsCompletion) {
            return;
        }
        if (!detectionState && faceTracker != null
                && mFaceDetectRoundView != null && mFaceDetectRoundView.getRound() > 0) {

            if (!initTrack) {
                //初始化人脸追踪
                faceTracker.FaceTrackingInit(data, mPreviewHeight, mPreviewWidth);
                initTrack = true;
            } else {
                //更新人脸数据
                data = RotateNV21Utils.rotationNV21(data, 270, mPreviewWidth, mPreviewHeight);
                faceTracker.Update(data, mPreviewWidth, mPreviewHeight, true);
                List<Face> trackingInfo = faceTracker.getTrackingInfo();
                if (trackingInfo.size() > 0) {
                    liveStartTime = System.currentTimeMillis();
//                  人脸位置
                    Face faceRect = trackingInfo.get(0);
//                    Log.e("android", faceRect.left + " " + faceRect.right + " " + faceRect.bottom + " " + faceRect.top);
                    float pitch = faceRect.pitch;
                    float yaw = faceRect.yaw;
                    float faceCx = faceRect.mCenterX / mPreviewHeight;
                    float faceCy = faceRect.mCenterY / mPreviewWidth;
//                    float faceCx=1;
//                    float faceCy=1;
//                    Log.e("debug", mPreviewWidth + "  " + mPreviewHeight + "width " + faceRect.width + "height " + faceRect.height);

//                    Rect previewDetectRect = FaceDetectRoundView.getPreviewDetectRect(1440, 3044, 1188, 540);
//                    int mCenterX = (previewDetectRect.left + previewDetectRect.right) >> 1;
//                    int mCenterY = (previewDetectRect.top + previewDetectRect.bottom) >> 1;
                    //previewDetectRect720 1369
//                    Log.e(TAG, "previewDetectRect" + previewDetectRect.left + " "
//                            + previewDetectRect.right + " " + previewDetectRect.top + " " + previewDetectRect.bottom);

//                    Log.e(TAG, "faceDetectRectRect" + faceRect.mCenterX + " " + faceRect.mCenterY);
//                    Log.e("face x", faceCx + "");
//                    Log.e("face y", faceCy + "");
//                    Log.e("CenterJ", (Math.abs(faceCx - 0.5) < 0.1 && Math.abs(faceCy - 0.5) < 0.1 && faceWidthRef > 0.3) + "");
//                    Log.e("CenterJ", mCenterX+ " XXX " + mCenterY  +" YYY ");
                    //判断人脸中心点
                    Log.e("CenterJ", faceCx + " X " + faceCy + " Y ");

                    //防止闪一下就通过验证
                    if (System.currentTimeMillis() - haveFaceTime < 1000) {
                        //动作活体
                        check(faceRect, yaw, pitch, data);
                        //控制正脸显示
                        faceCheck = true;
                    } else {
                        faceCheck = false;
                    }
                    int maxFace = mPreviewWidth / 2 - 100;
                    if (faceRect.width < maxFace && faceRect.height < maxFace) {
                        if ((faceCx > 0.2 && faceCx < 0.55) && (faceCy > 0.4 && faceCy < 0.6)) {
                            if (Math.abs(pitch) > 0) {
                                if (Math.abs(pitch) < 8) {
                                    if (Math.abs(yaw) < 8) {
                                        //记录正脸的时间
                                        haveFaceTime = System.currentTimeMillis();
                                    } else {
                                        if (!faceCheck) {
                                            mFaceDetectRoundView.setTipTopText("请正对手机");
                                        }
                                    }

                                } else {
                                    if (!faceCheck) {
                                        mFaceDetectRoundView.setTipTopText("请正对手机");
                                    }
                                }
                            } else {
                                if (!faceCheck) {
                                    mFaceDetectRoundView.setTipTopText("请正对手机");
                                }
                            }
                        } else {
                            mFaceDetectRoundView.setTipTopText("把脸移动到框内");
                        }
                    } else {
                        mFaceDetectRoundView.setTipTopText("请把手机拿远一点");
                    }


                } else {
                    //无人脸
                    mFaceDetectRoundView.setTipTopText("未检测到人脸");
                }

                trackingInfo.clear();
                if (resultData != null) {
                    if (faceTracker != null) {
                        faceTracker.releaseSession();
                        faceTracker = null;
                    }
                    getMainActivity().initAuthFragment();
                    stopPreview();
                    flag = true;
                }
            }
//            detectionState = false;

        }
    }

    private void check(Face faceRect, float yaw, float pitch, byte[] data) {

        if (live.size() > 0) {
            txt = "请" + live.get(0);
            mFaceDetectRoundView.setTipTopText(txt);
            //检测手机摇晃
//            if (System.currentTimeMillis() - sensorTime < 1000) {
//                return;
//            }
            switch (live.get(0)) {
                case "张张嘴":
                    if (faceRect.mouthState == 1 && faceRect.shakeState == 0) {
                        detectionState = true;
                        live.remove("张张嘴");
                        mFaceDetectRoundView.setTipTopText("非常好");
                        detectionStateSleep();
                    }

                    break;
                case "缓慢摇头":
                    if (System.currentTimeMillis() - sensorTime < 500) {
                        return;
                    }
                    if (Math.abs(yaw) >= 12) {
                        detectionState = true;
                        live.remove("缓慢摇头");
                        mFaceDetectRoundView.setTipTopText("非常好");
                        detectionStateSleep();
                    }
                    break;
                case "缓慢抬头":
                    if (System.currentTimeMillis() - sensorTime < 500) {
                        return;
                    }
                    if (Math.abs(pitch) >= 12) {
                        detectionState = true;
                        live.remove("缓慢抬头");
                        mFaceDetectRoundView.setTipTopText("非常好");
                        detectionStateSleep();
                    }
                    break;
                case "眨眨眼":
                    if (faceRect.eyeState == 1) {
                        detectionState = true;
                        live.remove("眨眨眼");
                        mFaceDetectRoundView.setTipTopText("非常好");
                        detectionStateSleep();
                    }
                    break;
            }
            mFaceDetectRoundView.setProcessCount(liveSize - live.size(), liveSize);
            if (live.size() == 0) {
                takePhoto(faceRect, data, mPreviewHeight, mPreviewWidth);
            }

        }
    }

    private void detectionStateSleep() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                SystemClock.sleep(1000);
                detectionState = false;
            }
        }).start();
    }

    private void showMessageDialog() {
        mTimeoutDialog = new TimeoutDialog(getContext());
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
        getMainActivity().initIdentityFragment();
    }


    private void takePhoto(Face face, byte[] data, int width, int height) {
        try {
            resultData = data;
            FastYUVtoRGB fastYUVtoRGB = new FastYUVtoRGB(getContext());
//            Log.e("TAG", face.left + " " + face.right + " " + face.bottom + " " + face.top);
            ((MainActivity) getActivity()).faceData = fastYUVtoRGB.convertYUVtoRGB(data, width, height);
//            takePhotoFile = File.createTempFile("face", null, getActivity().getCacheDir());
//                //保存人脸到SD卡
//            FileUtils.saveFile(takePhotoFile, ((MainActivity) getActivity()).faceData);
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


    private void showFrame(Camera.Face[] faces) {
        Canvas canvas = textureView.lockCanvas();
        if (canvas == null) return;
        canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);
        if (faces.length < 1) {
            // 清空canvas
//            mFaceDetectRoundView.setTipTopText("请将脸移入取景框");
            textureView.unlockCanvasAndPost(canvas);
            return;
        }
//        Log.e("android", "aaaaa" +
//                faces[0].rect.left + " " + faces[0].rect.right + " " + faces[0].rect.bottom + " " + faces[0].rect.top);
        RectF rectF = new RectF(faces[0].rect);
        int viewWidth = textureView.getWidth();
        int viewHeight = textureView.getHeight();
        Matrix matrix = new Matrix();
//        Log.e("viewWidth", viewHeight + "  " + viewWidth);
//
//        这里使用的是后置摄像头就不用翻转。由于没有进行旋转角度的兼容，这里直接传系统调整的值
        prepareMatrix(matrix, false, 270, viewWidth, viewHeight);
        //坐标调整
        matrix.mapRect(rectF);

        //人脸翻转
        float left = viewWidth - rectF.right;
        float right = left + rectF.width();
        rectF.left = left;
        rectF.right = right;
        //left right top bottom
        //previewDetectRectRect(180, 721 - 1260, 2017)
//        Log.e("TAG", rectF.left + " " + rectF.right + " " + rectF.top + " " + rectF.bottom);
        canvas.drawRect(rectF, paint);

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


    @Override
    public void onDestroy() {
        super.onDestroy();
        sensorHelper.stop();
    }

    public void returnIdentityFragment() {
        if (faceTracker != null) {
            faceTracker.releaseSession();
            faceTracker = null;
        }
        getMainActivity().initIdentityFragment();
        stopPreview();
        flag = true;
    }
}