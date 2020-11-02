package com.renren.faceos.utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PointF;
import android.media.FaceDetector;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class FaceUtils {
    private static BitmapFactory.Options BitmapFactoryOptionsbfo;
    private static ByteArrayOutputStream out;
    private static byte[] data;
    private static FaceDetector.Face[] myFace;
    private static FaceDetector myFaceDetect;
    private static int tx = 0;
    private static int ty = 0;
    private static int bx = 0;
    private static int by = 0;
    private static int width = 0;
    private static int height = 0;
    private static float wuchax = 0;
    private static float wuchay = 0;
    private static FaceDetector.Face face;
    private static PointF myMidPoint;
    private static float myEyesDistance;
    private static List<String> facePaths;
    private static String facePath;

    public static Bitmap faceCut(Bitmap bitmap, Context context) {
        facePaths = null;
        BitmapFactoryOptionsbfo = new BitmapFactory.Options();
        BitmapFactoryOptionsbfo.inPreferredConfig = Bitmap.Config.RGB_565; // 构造位图生成的参数，必须为565。类名+enum
        out = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 50, out);
        data = out.toByteArray();
        bitmap = BitmapFactory.decodeByteArray(data, 0, data.length,
                BitmapFactoryOptionsbfo);
        try {
            out.flush();
            out.close();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        width = bitmap.getWidth();
        height = bitmap.getHeight();
        myFace = new FaceDetector.Face[5]; // 分配人脸数组空间
        myFaceDetect = new FaceDetector(bitmap.getWidth(), bitmap.getHeight(), 5);
        int numberOfFaceDetected = myFaceDetect.findFaces(bitmap, myFace);
        if (numberOfFaceDetected <= 0) {// FaceDetector构造实例并解析人脸
            bitmap.recycle();
            return null;
        }
        facePaths = new ArrayList<String>();
        for (int i = 0; i < numberOfFaceDetected; i++) {
            face = myFace[i];
            myMidPoint = new PointF();
            face.getMidPoint(myMidPoint);
            myEyesDistance = face.eyesDistance();   //得到人脸中心点和眼间距离参数，并对每个人脸进行画框
            wuchax = myEyesDistance / 2 + myEyesDistance;
            wuchay = myEyesDistance * 2 / 3 + myEyesDistance;

            if (myMidPoint.x - wuchax < 0) {//判断左边是否出界
                tx = 0;
            } else {
                tx = (int) (myMidPoint.x - wuchax);
            }
            if (myMidPoint.x + wuchax > width) {//判断右边是否出界
                bx = width;
            } else {
                bx = (int) (myMidPoint.x + wuchax);
            }
            if (myMidPoint.y - wuchay < 0) {//判断上边是否出界
                ty = 0;
            } else {
                ty = (int) (myMidPoint.y - wuchay);
            }
            if (myMidPoint.y + wuchay > height) {//判断下边是否出界
                by = height;
            } else {
                by = (int) (myMidPoint.y + wuchay);
            }

            try {
                return Bitmap.createBitmap(bitmap, tx, ty, bx - tx, by - ty);//这里可以自行调整裁剪宽高
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        bitmap.recycle();
        return bitmap;
    }

    public static Bitmap bitmapRotation(Bitmap bm, final int orientationDegree) {

        Matrix m = new Matrix();
        m.setRotate(orientationDegree, (float) bm.getWidth() / 2,
                (float) bm.getHeight() / 2);
        float targetX, targetY;
        if (orientationDegree == 90) {
            targetX = bm.getHeight();
            targetY = 0;
        } else if (orientationDegree == 270) {
            targetX = 0;
            targetY = bm.getWidth();
        } else {
            targetX = bm.getHeight();
            targetY = bm.getWidth();
        }

        final float[] values = new float[9];
        m.getValues(values);

        float x1 = values[Matrix.MTRANS_X];
        float y1 = values[Matrix.MTRANS_Y];

        m.postTranslate(targetX - x1, targetY - y1);

        Bitmap bm1 = Bitmap.createBitmap(bm.getHeight(), bm.getWidth(),
                Bitmap.Config.ARGB_8888);

        Paint paint = new Paint();
        Canvas canvas = new Canvas(bm1);
        canvas.drawBitmap(bm, m, paint);

        return bm1;
    }


}