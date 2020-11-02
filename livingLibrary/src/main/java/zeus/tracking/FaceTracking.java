package zeus.tracking;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by yujinke on 17/07/2018.
 */


public class FaceTracking {

    static {
        System.loadLibrary("zeuseesTracking-lib");
    }

    public native static void update(byte[] data, int height, int width, long session);

    public native static void initTracking(byte[] data, int height, int width, long session);

    public native static long createSession(String modelPath);

    public native static void releaseSession(long session);

    public native static int getTrackingNum(long session);

    public native static int[] getTrackingLocationByIndex(int index, long session);

    public native static int[] getAttributeByIndex(int index, long session);

    public native static float[] getEulerAngleByIndex(int index, long session);

    public native static int getTrackingIDByIndex(int index, long session);

    private long session;
    private List<Face> faces;

    public FaceTracking(String pathModel) {
        session = createSession(pathModel);
        faces = new ArrayList<>();
    }

    public void releaseSession() {
        releaseSession(session);
    }

    public void FaceTrackingInit(byte[] data, int height, int width) {
        initTracking(data, height, width, session);
    }

    public void Update(byte[] data, int height, int width) {
        try {
            update(data, height, width, session);
            int numsFace = getTrackingNum(session);
            faces.clear();
            for (int i = 0; i < numsFace; i++) {
                int[] faceRect = getTrackingLocationByIndex(i, session);
                int[] attributes = getAttributeByIndex(i, session);
                float[] attitudes = getEulerAngleByIndex(i, session);
                int id = getTrackingIDByIndex(i, session);
                //Face face = new Face(0,0,0,0,0);
                Face face = new Face(faceRect[0], faceRect[1], faceRect[2], faceRect[3], id);

                face.monthState = attributes[0];
                face.eyeState = attributes[1];
                face.shakeState = attributes[2];
                face.riseState = attributes[3];
                face.pitch = attitudes[0];
                face.yaw = attitudes[1];
                face.roll = attitudes[2];
                //            Log.d("debug", "pitch " + face.pitch + "yaw " + face.yaw + "roll " + face.roll);
                faces.add(face);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public List<Face> getTrackingInfo() {
        return faces;

    }


}
