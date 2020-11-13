package com.renren.faceos.utils;

import android.hardware.Camera;

/**
 * CameraUtils
 */
public class CameraUtils {

    public static final String TAG = CameraUtils.class.getSimpleName();

    public static void releaseCamera(Camera camera) {
        try {
            camera.release();
        } catch (RuntimeException e2) {
            e2.printStackTrace();
        }
    }
}
