package faceos.tracking;

import android.graphics.Rect;

public class Face {


    public int ID;
    public int left;
    public int top;
    public int right;
    public int bottom;
    public int height;
    public int width;
    public float[] landmarks;
    public int mouthState;
    public int eyeState;
    public int shakeState;
    public int riseState;
    public float pitch;
    public float yaw;
    public float roll;
    public float mCenterX;
    public float mCenterY;

    public boolean isStable;


    static public int[] mapping = {16, 53, 101, 73, 91, 6, 5, 4, 3, 2, 1, 0, 57, 32, 30, 31, 28, 29, 26, 33, 61, 43, 45, 44, 38, 99, 88, 58, 37, 35, 92, 82, 93, 89, 72, 78, 98, 85, 87, 86, 97, 75, 100, 76, 68, 84, 49, 62, 71, 24, 90, 63, 77, 54, 69, 74, 25, 12, 66, 55, 67, 96, 64, 103, 94, 95, 8, 56, 27, 46, 40, 70, 104, 39, 42, 41, 15, 14, 13, 36, 11, 10, 9, 65, 34, 60, 51, 50, 47, 48, 80, 79, 81, 83, 52, 18, 19, 17, 22, 23, 20, 21, 7, 102, 59, 105};


    Face(int x1, int y1, int x2, int y2) {
        left =x2;
        top = y1;
        right =  x1;
        bottom = y2;
        height = y2 - y1;
        width = x2 - x1;
        landmarks = new float[106 * 2];
        mouthState = 0;
        eyeState = 0;
        mCenterX = (left + right) / 2;
        mCenterY = (top + bottom) / 2;
    }


    Face(int x1, int y1, int _width, int _height, float[] landmark, int id) {
        left = x1;
        top = y1;
        right = x1 + _width;
        bottom = y1 + _height;
        width = _width;
        height = _height;
        landmarks = landmark;
        ID = id;
        landmarks = new float[106 * 2];
        for (int i = 0; i < 106; i++) {
            landmarks[i * 2] = landmark[mapping[i] * 2];
            landmarks[i * 2 + 1] = landmark[mapping[i] * 2 + 1];
        }
        mouthState = 0;
        eyeState = 0;
    }

    public Rect getFaceRect(float ratioX, float ratioY, float surfaceRatio) {
        float x = this.mCenterX * ratioX;
        float y = this.mCenterY * ratioY;
        Rect rect = new Rect((int)(x - this.width / 2.0F * ratioX * surfaceRatio),
                (int)(y - this.width / 2.0F * ratioY * surfaceRatio),
                (int)(x + this.width / 2.0F * ratioX * surfaceRatio),
                (int)(y + this.width / 2.0F * ratioY * surfaceRatio));
        return rect;
    }
}
