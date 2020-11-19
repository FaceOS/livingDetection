package zeus.tracking;

/**
 * Created by yujinke on 17/07/2018.
 */


public class Face {


    public int ID;
    public int left;
    public int top;
    public int right;
    public int bottom;
    public int height;
    public int width;
    public int center_x;
    public int center_y;

    public int[] landmarks;
    public int monthState;
    public int eyeState;
    public int shakeState;
    public int riseState;
    public float pitch;
    public float yaw;
    public float roll;



    Face(int x1, int y1, int x2, int y2) {
        left = x1;
        top = y1;
        right = x2;
        bottom = y2;
        height = y2 - y1;
        width = x2 - x1;
        center_x = x1 + width/2;
        center_y = y1 + height/2;
        landmarks = new int[106 * 2];
        monthState = 0;
        eyeState = 0;

    }


    Face(int x1, int y1, int _width, int _height, int[] landmark, int id) {
        left = x1;
        top = y1;
        right = x1 + _width;
        bottom = y1 + _height;
        center_x = x1 + _width/2;
        center_y = y1 + _height/2;
        width = _width;
        height = _height;
        landmarks = landmark;
        ID = id;
        monthState = 0;
        eyeState = 0;

    }


    Face(int x1, int y1, int _width, int _height, int id) {
        left = x1;
        top = y1;
        right = x1 + _width;
        bottom = y1 + _height;
        center_x = x1 + _width/2;
        center_y = y1 + _height/2;
        width = _width;
        height = _height;
        ID = id;
        monthState = 0;
        eyeState = 0;
    }


}
