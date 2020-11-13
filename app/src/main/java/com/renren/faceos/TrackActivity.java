package com.renren.faceos;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PorterDuff;
import android.graphics.Rect;
import android.graphics.RectF;
import android.hardware.Camera;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.SystemClock;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.TextureView;
import android.widget.TextView;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import com.renren.faceos.utils.FastYUVtoRGB;
import com.renren.faceos.utils.FileUtils;

import zeus.tracking.Face;
import zeus.tracking.FaceTracking;

public class TrackActivity extends AppCompatActivity implements SurfaceHolder.Callback, Camera.PreviewCallback {

    private Camera camera = null;
    private SurfaceView cameraSurface = null;
    private FaceTracking faceTracker;
    private int height = 1080;
    private int width = 1920;
    private int frame;
    private HandlerThread handleThread = null;
    private Handler detectHandler = null;
    private TextView show;
    private int detection_state = 0;
    private List<String> live;
    private int cameraId = 1;
    private byte[] resultData;

    private final int REQUEST_EXTERNAL_STORAGE = 1;
    private final String[] PERMISSIONS_STORAGE = {
            "android.permission.READ_EXTERNAL_STORAGE",
            "android.permission.WRITE_EXTERNAL_STORAGE"};

    private void verifyStoragePermissions(Activity activity) {
        try {
            //检测是否有写的权限
            int permission = ActivityCompat.checkSelfPermission(activity,
                    "android.permission.WRITE_EXTERNAL_STORAGE");
            if (permission != PackageManager.PERMISSION_GRANTED) {
                // 没有写的权限，去申请写的权限，会弹出对话框
                ActivityCompat.requestPermissions(activity, PERMISSIONS_STORAGE, REQUEST_EXTERNAL_STORAGE);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tracking);
        verifyStoragePermissions(this);
        String sdPath = Environment.getExternalStorageDirectory().getAbsolutePath();
        faceTracker = new FaceTracking(sdPath + "/faceos/models");
        handleThread = new HandlerThread("dt");
        handleThread.start();
        detectHandler = new Handler(handleThread.getLooper());
        show = findViewById(R.id.show);
        cameraSurface = findViewById(R.id.preview);
        cameraSurface.getHolder().addCallback(this);
        cameraSurface.setKeepScreenOn(true);

        //选择活体动作
        live = new ArrayList<>();
        live.add("张嘴");
        live.add("摇头");
        live.add("眨眼");
        live.add("向左");
        live.add("向右");
        live.add("拍照");
    }

    TextureView textureView;

    private void opPreviewSize(int width, int height) {

        if (camera != null && width > 0) {
            try {
                Camera.Parameters parameters = camera.getParameters();
                Camera.Size optSize = getOptimalSize(width, height, camera.getParameters().getSupportedPreviewSizes());
                Log.i("wtf", "opPreviewSize-> " + optSize.width + " " + optSize.height);
                parameters.setPreviewSize(optSize.width, optSize.height);
                // parameters.setPreviewFpsRange(10, 15);
                camera.setParameters(parameters);
                camera.startPreview();
            } catch (RuntimeException e) {
                e.printStackTrace();
            }
        }
    }

    private Camera.Size getOptimalSize(int width, int height, List<Camera.Size> sizes) {

        Camera.Size pictureSize = sizes.get(0);

        List<Camera.Size> candidates = new ArrayList<>();

        for (Camera.Size size : sizes) {
            if (size.width >= width && size.height >= height && size.width * height == size.height * width) {
                // 比例相同
                candidates.add(size);
            } else if (size.height >= width && size.width >= height && size.width * width == size.height * height) {
                // 反比例
                candidates.add(size);
            }
        }
        if (!candidates.isEmpty()) {
            return Collections.min(candidates, sizeComparator);
        }

        for (Camera.Size size : sizes) {
            if (size.width >= width && size.height >= height) {
                return size;
            }
        }

        return pictureSize;
    }

    private Comparator<Camera.Size> sizeComparator = new Comparator<Camera.Size>() {
        @Override
        public int compare(Camera.Size lhs, Camera.Size rhs) {
            return Long.signum((long) lhs.width * lhs.height - (long) rhs.width * rhs.height);
        }
    };


    @Override
    protected void onResume() {
        super.onResume();
        Log.i("MainActivity", "Activity On onResume");
        if (camera != null) {
            camera.startPreview();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.i("MainActivity", "Activity On Pasue");
    }

    private void releaseCamera() {
        if (camera != null) {
            camera.setPreviewCallback(null);
            camera.stopPreview();
            camera.release();
        }
    }

    @Override
    public void surfaceCreated(SurfaceHolder surfaceHolder) {
        try {
            Log.e("TAG", "surfaceCreated");
            camera = Camera.open(cameraId);
            camera.setDisplayOrientation(90);
            camera.setPreviewDisplay(surfaceHolder);//把摄像头获得画面显示在SurfaceView控件里面
            opPreviewSize(480, 640);
            Camera.Size size = camera.getParameters().getPreviewSize();
            height = size.height;
            width = size.width;
            Log.e("TAG", "height" + height + " width " + width);
            camera.setPreviewCallback(TrackActivity.this);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void surfaceChanged(SurfaceHolder surfaceHolder, int i, int i1, int i2) {
        Log.e("TAG", "surfaceChanged");
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder surfaceHolder) {
        Log.e("TAG", "surfaceDestroyed");
    }

    String txt;

    @Override
    public void onPreviewFrame(byte[] data, Camera camera) {
        if (detection_state == 0 && faceTracker != null && data != null) {
            try {
                if (frame == 0) {
                    detection_state = 1;
                    faceTracker.faceTrackingInit(data, height, width);
                    SystemClock.sleep(500);
                    detection_state = 0;
                } else {
                    detection_state = 1;
                    faceTracker.update(data, height, width);
                    List<Face> trackingInfo = faceTracker.getTrackingInfo();
                    if (live.size() > 0) {
                        txt = "请" + live.get(0);
                        if (trackingInfo != null && trackingInfo.size() == 1) {
                            Face face = trackingInfo.get(0);
                            takePhoto(face, data);
                            if (live.size() > 0)
                                switch (live.get(0)) {
                                    case "张嘴":
                                        if (face.monthState == 1) {
                                            live.remove("张嘴");
                                        }
                                        break;
                                    case "摇头":
                                        if (face.shakeState == 1) {
                                            live.remove("摇头");
                                        }
                                        break;
                                    case "眨眼":
                                        if (face.eyeState == 1 && face.shakeState == 0) {
                                            live.remove("眨眼");
                                        }
                                        break;
                                    case "向左":
                                        if (face.yaw > 7) {
                                            live.remove("向左");
                                        }
                                        break;
                                    case "向右":
                                        if (face.yaw < -7) {
                                            live.remove("向右");
                                        }
                                        break;
                                    case "拍照":
                                        takePhoto(face, data);
                                        break;
                                }

                        } else {
                            txt = "无人脸";
                        }
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                if (show != null)
                                    show.setText(txt);
                            }
                        });
                        Log.e("TAG", txt);
                    } else {
                        if (resultData != null) {
                            releaseCamera();
                            if (faceTracker != null) {
                                faceTracker.releaseSession();
                                faceTracker = null;
                            }
                            finish();
                        }
                    }

                    detection_state = 0;
                }
                frame = 1;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

    }



    private void takePhoto(Face face, byte[] data) {
        //中心点偏移距离
        double distance = centerDistance(face);
        if (resultData == null && distance < 0.3 && face.pitch > -10 && face.pitch < 3
                && face.yaw > -1 && face.yaw < 3 && face.roll > -1 && face.roll < 3) {
            live.remove("拍照");
            resultData = data;
            FastYUVtoRGB fastYUVtoRGB = new FastYUVtoRGB(this);
            Bitmap bitmap = fastYUVtoRGB.convertYUVtoRGB(data, width, height);
            //保存人脸到SD卡
            try {
                File file = File.createTempFile("face", null, this.getCacheDir());
                FileUtils.saveFile(file, bitmap);
                Intent intent = new Intent();
                intent.putExtra("facePath", file.getAbsolutePath());
                setResult(1, intent);
            } catch (IOException e) {
                e.printStackTrace();
            }

        }
    }


    int prev_cx;
    int prev_cy;

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
}
