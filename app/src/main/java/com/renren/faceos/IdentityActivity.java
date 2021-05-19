package com.renren.faceos;


import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.PixelFormat;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.drawable.ColorDrawable;
import android.hardware.Camera;
import android.os.Build;
import android.os.Bundle;
import android.os.SystemClock;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.TextureView;
import android.view.View;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ImageView;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.lzy.okgo.OkGo;
import com.lzy.okgo.callback.StringCallback;
import com.lzy.okgo.model.Response;
import com.lzy.okgo.request.base.Request;
import com.renren.faceos.entity.API;
import com.renren.faceos.entity.IdNamePhoto;
import com.renren.faceos.utils.Base64Utils;
import com.renren.faceos.utils.BitmapZoomUtils;
import com.renren.faceos.utils.BrightnessUtils;
import com.renren.faceos.utils.CameraPreviewUtils;
import com.renren.faceos.utils.CameraUtils;
import com.renren.faceos.utils.FaceUtils;
import com.renren.faceos.utils.FastYUVtoRGB;
import com.renren.faceos.utils.FileUtils;
import com.renren.faceos.utils.PermissionsUtil;
import com.renren.faceos.widget.FaceDetectRoundView;
import com.renren.faceos.widget.TimeoutDialog;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import faceos.tracking.FaceTracking;
import faceos.tracking.Face;


public class IdentityActivity extends AppCompatActivity implements PermissionsUtil.IPermissionsCallback,
        SurfaceHolder.Callback,
        Camera.PreviewCallback,
        Camera.ErrorCallback,
        TimeoutDialog.OnTimeoutDialogClickListener {

    private PermissionsUtil request;

    public static final String TAG = IdentityActivity.class.getSimpleName();
    // View
    protected View mRootView;
    protected FrameLayout mFrameLayout;
    protected SurfaceView mSurfaceView;
    protected SurfaceHolder mSurfaceHolder;
    protected FaceDetectRoundView mFaceDetectRoundView;
    protected ImageView detect_close;

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
    private String[] action = {"张张嘴", "眨眨眼"};

    public Bitmap faceData;
    public String name;
    public String idCard;
    private Intent resultIntent;

    private int activityResultCode = 20002;
    private String RESULT = "code";
    private String MSG = "msg";
    private int SUCCESSCode = 1;
    private int FAILCode = 0;
    private String successMsg = "认证成功";
    private String LivenessFailMsg = "活体验证失败";
    private String networkFailMsg = "网络连接失败";

    private AlertDialog alertDialog;

    public void showLoadingDialog() {
        alertDialog = new AlertDialog.Builder(this).create();
        alertDialog.getWindow().setBackgroundDrawable(new ColorDrawable());
        alertDialog.setCancelable(false);
        alertDialog.setOnKeyListener(new DialogInterface.OnKeyListener() {
            @Override
            public boolean onKey(DialogInterface dialog, int keyCode, KeyEvent event) {
                if (keyCode == KeyEvent.KEYCODE_SEARCH || keyCode == KeyEvent.KEYCODE_BACK)
                    return true;
                return false;
            }
        });
        alertDialog.show();
        alertDialog.setContentView(R.layout.loading_alert);
        alertDialog.setCanceledOnTouchOutside(false);
    }

    public void dismissLoadingDialog() {
        if (null != alertDialog && alertDialog.isShowing()) {
            alertDialog.dismiss();
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_identity);
        request = PermissionsUtil
                .with(this)
                .requestCode(1)
                .isDebug(true)//开启log
                .permissions(PermissionsUtil.Permission.Camera.CAMERA)
                .request();
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        request.onRequestPermissionsResult(requestCode, permissions, grantResults);
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

    }


    @Override
    public void onPermissionsGranted(int requestCode, String... permission) {
        String assetPath = "faceos";
        String sdcardPath = getFilesDir().getPath() + File.separator + assetPath;
        FileUtils.copyFilesFromAssets(this, assetPath, sdcardPath);

        init();
    }

    private void init() {
        setScreenBright();
        //屏幕常亮
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        initModel();
        DisplayMetrics dm = new DisplayMetrics();
        Display display = getWindowManager().getDefaultDisplay();
        display.getMetrics(dm);
        mDisplayWidth = dm.widthPixels;
        mDisplayHeight = dm.heightPixels;
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

        FrameLayout.LayoutParams cameraFL = new FrameLayout.LayoutParams(
                (int) (w), (int) (h),
                Gravity.CENTER_VERTICAL | Gravity.CENTER_HORIZONTAL);
        mSurfaceView.setLayoutParams(cameraFL);
        mFrameLayout.addView(mSurfaceView);

        mFaceDetectRoundView = findViewById(R.id.detect_face_round);
        mFaceDetectRoundView.setIsActiveLive(true);
        textureView = findViewById(R.id.textureView);
        detect_close = findViewById(R.id.detect_close);
        detect_close.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                resultIntent.putExtra(RESULT, FAILCode);
                resultIntent.putExtra(MSG, "手动关闭活体检测");
                setResult(activityResultCode, resultIntent);
                release();
            }
        });
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
                        runOnUiThread(new Runnable() {
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

        resultIntent = new Intent();
        resultIntent.putExtra("name", name);
        resultIntent.putExtra("idCard", idCard);
    }

    private void initModel() {
        faceTracker = new FaceTracking(getFilesDir().getPath() + "/faceos/models/model.bin");
        //选择活体动作
        live = new ArrayList<>();
        //随机动作
        Random random = new Random();
        List<Integer> indexArray = new ArrayList<>();
        while (indexArray.size() != 2) {
            int index = random.nextInt(2);
            if (!indexArray.contains(index))
                indexArray.add(index);
        }
        for (Integer index : indexArray) {
            live.add(action[index]);
        }
//        live.add(action[2]);
//        for (String s : live) {
//            System.out.println(s + "==================");
//        }
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
    public void onResume() {
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
//        Log.e(TAG, mPreviewWidth + " size " + mPreviewHeight);
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

            if (!initTrack) {
                //初始化人脸追踪
                faceTracker.FaceTrackingInit(data, mPreviewHeight, mPreviewWidth);
                initTrack = true;
            } else {
                //更新人脸数据
                faceTracker.Update(data, mPreviewHeight, mPreviewWidth, true);
                List<Face> trackingInfo = faceTracker.getTrackingInfo();
                if (trackingInfo.size() > 0) {
                    liveStartTime = System.currentTimeMillis();
//                  人脸位置
                    Face faceRect = trackingInfo.get(0);
//                    float faceCx = faceRect.center_x / faceTracker.ui_width;
//                    float faceCy = faceRect.center_y / faceTracker.ui_height;
//                    float faceCx=1;
//                    float faceCy=1;
//                    Log.e("debug", mPreviewWidth + "  " + mPreviewHeight + "width " + faceRect.width + "height " + faceRect.height);
                    //Log.e(TAG, "faceDetectRectRect" + faceDetectRectRect.toString());
//                    Log.e("face x", faceCx + "");
//                    Log.e("face y", faceCy + "");
//                    Log.e("CenterJ", (Math.abs(faceCx - 0.5) < 0.1 && Math.abs(faceCy - 0.5) < 0.1 && faceWidthRef > 0.3) + "");

                    //判断人脸中心点
//                    Log.e(TAG, Math.abs(faceCx - 0.5) + " X " + Math.abs(faceCy - 0.5) + " Y ");
//                    if (Math.abs(faceCx - 0.5) < 0.3 && Math.abs(faceCy - 0.5) < 0.2) {
                    int maxFace = mPreviewWidth / 2 - 100;

                    if (faceRect.width < maxFace && faceRect.height < maxFace) {
                        if (live.size() > 0) {
                            Random random = new Random();
                            txt = "请" + live.get(0);
                            mFaceDetectRoundView.setTipTopText(txt);
                            switch (live.get(0)) {
                                case "张张嘴":
                                    if (faceRect.mouthState == 1 && faceRect.shakeState == 0) {
                                        detectionState = true;
                                        live.remove("张张嘴");
                                        mFaceDetectRoundView.setTipTopText("非常好");
                                        detectionStateSleep();
                                    }

                                    break;
                                case "左右摇摇头":
                                    if (faceRect.shakeState == 1) {
                                        detectionState = true;
                                        live.remove("左右摇摇头");
                                        mFaceDetectRoundView.setTipTopText("非常好");
                                        detectionStateSleep();
                                    }

                                    break;
                                case "眨眨眼":
                                    if (faceRect.eyeState == 1 && faceRect.shakeState == 0) {
                                        detectionState = true;
                                        live.remove("眨眨眼");
                                        mFaceDetectRoundView.setTipTopText("非常好");
                                        detectionStateSleep();
                                    }
                                    break;
                            }
                            mFaceDetectRoundView.setProcessCount(liveSize - live.size(), liveSize);
                            if (live.size() == 0) {
                                takePhoto(faceRect, data, mPreviewWidth, mPreviewHeight);
                            }

                        }
                    } else {
                        mFaceDetectRoundView.setTipTopText("请把手机拿远一点");
                    }
                } else {
                    //无人脸
                    mFaceDetectRoundView.setTipTopText("未检测到人脸");

                }

                trackingInfo.clear();
//                if (resultData != null) {
//                    if (faceTracker != null) {
//                        faceTracker = null;
//                    }
//                    stopPreview();
//                    flag = true;
//                }
            }
//            detectionState = false;

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
            resultIntent.putExtra(RESULT, FAILCode);
            resultIntent.putExtra(MSG, "活体采集超时");
            setResult(activityResultCode, resultIntent);
            release();
        }
    }

    private File takePhotoFile;

    private void takePhoto(Face face, byte[] data, int width, int height) {
        try {
            FastYUVtoRGB fastYUVtoRGB = new FastYUVtoRGB(this);
            faceData = fastYUVtoRGB.convertYUVtoRGB(data, width, height);
            // 图片旋转
            Bitmap rotateBitmap = FaceUtils.bitmapRotation(faceData, 270);
            // 人脸裁剪
            Bitmap cutFace = FaceUtils.faceCut(rotateBitmap, this);
            Intent intent = getIntent();
            //这里图片可能是空的
            if (cutFace != null && intent != null) {
                //图片压缩
                Bitmap bitmap = BitmapZoomUtils.compressScale(cutFace);
                String name = intent.getStringExtra("name");
                String carNo = intent.getStringExtra("carNo");
                String appKey = intent.getStringExtra("appKey");
                String appScrect = intent.getStringExtra("appScrect");

                IdNamePhoto idNamePhoto = new IdNamePhoto();
                idNamePhoto.setName(name);
                idNamePhoto.setCardNo(carNo);
                facelivenessImg(idNamePhoto, appKey, appScrect, bitmap);
            } else {
                resultIntent.putExtra(RESULT, FAILCode);
                resultIntent.putExtra(MSG, LivenessFailMsg);
                setResult(activityResultCode, resultIntent);
                release();
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    private void facelivenessImg(final IdNamePhoto idNamePhoto, final String appKey, final String appScrect, final Bitmap cutFace) {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("imageBase64", Base64Utils.bitmapToBase64(cutFace));
        OkGo.<String>post(API.getInstance().getFacelivenessImg(appKey, appScrect))
                .upJson(jsonObject.toJSONString())
                .execute(new StringCallback() {
                    @Override
                    public void onStart(Request<String, ? extends Request> request) {
                        showLoadingDialog();
                    }

                    @Override
                    public void onSuccess(Response<String> response) {

                        JSONObject jsonObject = JSON.parseObject(response.body());
                        int code = jsonObject.getIntValue("code");
                        String msg = jsonObject.getString("msg");
                        if (code == 0) {
                            JSONObject data = jsonObject.getJSONObject("data");
                            Float score = data.getFloat("score");
                            if (score > 0.8) {
                                //为活体
                                idNamePhoto.setFaceBase64(Base64Utils.bitmapToBase64(cutFace));
                                nameIdCardAuth(idNamePhoto, appKey, appScrect);
                            } else {
                                resultIntent.putExtra(RESULT, FAILCode);
                                resultIntent.putExtra(MSG, LivenessFailMsg);
                                setResult(activityResultCode, resultIntent);
                                release();
                            }
                        } else {
                            resultIntent.putExtra(RESULT, FAILCode);
                            resultIntent.putExtra(MSG, LivenessFailMsg + " " + msg);
                            setResult(activityResultCode, resultIntent);
                            release();
                        }
                    }

                    @Override
                    public void onError(Response<String> response) {
                        super.onError(response);
                        resultIntent.putExtra(RESULT, FAILCode);
                        resultIntent.putExtra(MSG, networkFailMsg);
                        setResult(activityResultCode, resultIntent);
                        release();
                    }

                    @Override
                    public void onFinish() {
                        dismissLoadingDialog();
                    }
                });
    }

    private void nameIdCardAuth(IdNamePhoto idNamePhoto, String appKey, String appScrect) {
        OkGo.<String>post(API.getInstance().getIdNamePhotoCheck(appKey, appScrect))
                .upJson(JSON.toJSONString(idNamePhoto))
                .execute(new StringCallback() {
                    @Override
                    public void onStart(Request<String, ? extends Request> request) {
                        showLoadingDialog();
                    }

                    @Override
                    public void onSuccess(Response<String> response) {
                        JSONObject jsonObject = JSON.parseObject(response.body());
                        int result = jsonObject.getIntValue("RESULT");
                        JSONObject detail = jsonObject.getJSONObject("detail");
                        int resultCode = detail.getIntValue("resultCode");
                        if (result == 1 && resultCode == 1001) {
                            resultIntent.putExtra(RESULT, SUCCESSCode);
                            resultIntent.putExtra(MSG, successMsg);
                        } else {
                            String message = jsonObject.getString("MESSAGE");
                            String resultMsg = detail.getString("resultMsg");
                            resultIntent.putExtra(RESULT, FAILCode);
                            resultIntent.putExtra(MSG, message + " " + resultMsg);
                        }
                        setResult(activityResultCode, resultIntent);
                        release();
                    }

                    @Override
                    public void onError(Response<String> response) {
                        super.onError(response);
                        resultIntent.putExtra(RESULT, FAILCode);
                        resultIntent.putExtra(MSG, networkFailMsg);
                        setResult(activityResultCode, resultIntent);
                        release();
                    }

                    @Override
                    public void onFinish() {
                        dismissLoadingDialog();
                    }
                });
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
//            faceTracker.releaseSession();
            faceTracker = null;
        }
    }

    @Override
    public void onPermissionsDenied(int requestCode, String... permission) {
        finish();
    }


    public void release() {
        if (faceTracker != null) {
            faceTracker.release();
            faceTracker = null;
        }
        stopPreview();
        flag = true;
        finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        release();
    }
}

