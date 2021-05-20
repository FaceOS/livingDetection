package faceos.tracking;

import android.util.Log;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class FaceTracking {

    static {
        System.loadLibrary("Tracking-lib");
    }

    public native static void Update(byte[] data, int height, int width, long session);

    public native static void initTracking(byte[] data, int height, int width, long session);

    public native static long createSession(String modelPath);

    public native static void releaseSession(long session);

    public native static void setSmooth(long session, float h);

    public native static int getTrackingNum(long session);

    public native static int[] getTrackingRectkByIndex(int index, long session);

    public native static int[] getTrackingFaceActionByIndex(int index, long session);

    public native static float[] getEulerAngleByIndex(int index, long session);

    public native static int getTrackingIDByIndex(int index, long session);


    private long session;
    private List<Face> faces;
    private int tracking_seq = 0;


    public FaceTracking(String pathModel) {
        session = createSession(pathModel);
        faces = new ArrayList<Face>();
    }

    public void release() {
        releaseSession(session);
    }

    public void FaceTrackingInit(byte[] data, int height, int width) {
        initTracking(data, height, width, session);
    }
    public void Update(byte[] data, int height, int width, boolean stablize) {
        Update(data, height, width, session);
        int numsFace = getTrackingNum(session);
        for (int i = 0; i < numsFace; i++) {
            int[] rect = getTrackingRectkByIndex(i, session);
            int id = getTrackingIDByIndex(i, session);
            float[] attitudes = getEulerAngleByIndex(i, session);
            int[] face_action = getTrackingFaceActionByIndex(i, session);
//            Log.d("ACTION" , "blink " + face_action[0] + " shake "  + face_action[1]  +  " mouth_open " + face_action[2] + " head_rise " + face_action[3]);
            Face face = new Face(rect[0], rect[1], rect[2], rect[3]);
            Log.d("ACTION", "Update: .pitch"+ Math.abs(attitudes[0])+ " yaw"+ Math.abs(attitudes[1]) + "" +
                    "roll "+Math.abs(attitudes[2]));
            face.eyeState = face_action[0];
            face.shakeState = face_action[1];
            face.mouthState = face_action[2];
            face.pitch = attitudes[0];
            face.yaw = attitudes[1];
            face.roll = attitudes[2];
//            Log.d("ACTION", "Update: ."+face.width+" "+face.height);
            faces.add(face);
        }

//        faces.clear();
//        faces = _faces;
//        tracking_seq += 1;
    }

    public List<Face> getTrackingInfo() {
        return faces;
    }

    public void setSmoothRatio(float h){
        setSmooth(session,h);
    }

}
