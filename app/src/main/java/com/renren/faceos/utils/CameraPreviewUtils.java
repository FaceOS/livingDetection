package com.renren.faceos.utils;

import android.graphics.Point;
import android.hardware.Camera;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

public class CameraPreviewUtils {
    private static final String TAG = CameraPreviewUtils.class.getSimpleName();
    private static final int MIN_PREVIEW_PIXELS = 307200;
    private static final int MAX_PREVIEW_PIXELS = 921600;

    public CameraPreviewUtils() {
    }

    public static Point getBestPreview(Camera.Parameters parameters, Point screenResolution) {
        List<Camera.Size> rawSupportedSizes = parameters.getSupportedPreviewSizes();
        if (rawSupportedSizes == null) {
            Camera.Size defaultSize = parameters.getPreviewSize();
            return new Point(640, 480);
        } else {
            List<Camera.Size> supportedPictureSizes = new ArrayList(rawSupportedSizes);
            Collections.sort(supportedPictureSizes, new Comparator<Camera.Size>() {
                public int compare(Camera.Size a, Camera.Size b) {
                    int aPixels = a.height * a.width;
                    int bPixels = b.height * b.width;
                    if (bPixels < aPixels) {
                        return -1;
                    } else {
                        return bPixels > aPixels ? 1 : 0;
                    }
                }
            });
            double screenAspectRatio = screenResolution.x > screenResolution.y ? (double) screenResolution.x / (double) screenResolution.y : (double) screenResolution.y / (double) screenResolution.x;
            Camera.Size selectedSize = null;
            double selectedMinus = -1.0D;
            double selectedPreviewSize = 0.0D;
            Iterator it = supportedPictureSizes.iterator();

            while (true) {
                Camera.Size supportedPreviewSize;
                while (it.hasNext()) {
                    supportedPreviewSize = (Camera.Size) it.next();
                    int realWidth = supportedPreviewSize.width;
                    int realHeight = supportedPreviewSize.height;
                    if (realWidth * realHeight < 307200) {
                        it.remove();
                    } else if (realWidth * realHeight > 921600) {
                        it.remove();
                    } else if (realHeight % 16 == 0 && realWidth % 16 == 0) {
                        double aRatio = supportedPreviewSize.width > supportedPreviewSize.height ? (double) supportedPreviewSize.width / (double) supportedPreviewSize.height : (double) supportedPreviewSize.height / (double) supportedPreviewSize.width;
                        double minus = Math.abs(aRatio - screenAspectRatio);
                        boolean selectedFlag = false;
                        if (selectedMinus == -1.0D && minus <= 0.25D || selectedMinus >= minus && minus <= 0.25D) {
                            selectedFlag = true;
                        }

                        if (selectedFlag) {
                            selectedMinus = minus;
                            selectedSize = supportedPreviewSize;
                            selectedPreviewSize = (double) (realWidth * realHeight);
                        }
                    } else {
                        it.remove();
                    }
                }

                if (selectedSize != null) {
                    return new Point(selectedSize.width, selectedSize.height);
                }

                supportedPreviewSize = parameters.getPreviewSize();
                return new Point(640, 480);
            }
        }
    }
}
