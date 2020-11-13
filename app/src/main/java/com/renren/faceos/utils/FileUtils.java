package com.renren.faceos.utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.Environment;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.InputStream;


public class FileUtils {

    public static String sdCardPath;

    public static String getSdCardPath() {
        return Environment.getExternalStorageDirectory().getAbsolutePath();
    }

    /**
     * 保存人脸到sd卡
     */
    public static void saveFace2Sdcard(String fileName, Bitmap bitmap) {
        File sdRootFile = getSDRootFile();
        File file = null;
        if (sdRootFile != null && sdRootFile.exists()) {
            file = new File(sdRootFile, "faces" + File.separator + fileName);
            if (!file.exists()) {
                file.mkdirs();
            }
            saveFile(file, bitmap);
        }
    }


    /**
     * Checks if is sd card available.
     *
     * @return true, if is sd card available
     */
    public static boolean isSdCardAvailable() {
        return Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED);
    }

    /**
     * Gets the SD root file.
     *
     * @return the SD root file
     */
    public static File getSDRootFile() {
        if (isSdCardAvailable()) {
            return Environment.getExternalStorageDirectory();
        } else {
            return null;
        }
    }

    public static File getFaceDirectory() {
        File sdRootFile = getSDRootFile();
        File file = null;
        if (sdRootFile != null && sdRootFile.exists()) {
            file = new File(sdRootFile, "faces");
            if (!file.exists()) {
                boolean success = file.mkdirs();
            }
        }

        return file;
    }

    public static File getDirectory(String directory) {
        File sdRootFile = getSDRootFile();
        File file = null;
        if (sdRootFile != null && sdRootFile.exists()) {
            file = new File(sdRootFile, directory);
            if (!file.exists()) {
                boolean success = file.mkdirs();
            }
        }

        return file;
    }

    public static File getFaceErrorDirectory(String errRoot) {
        File sdRootFile = getSDRootFile();
        File file = null;
        if (sdRootFile != null && sdRootFile.exists()) {
            file = new File(sdRootFile, "/faceErrorImg/" + errRoot);
            if (!file.exists()) {
                file.mkdirs();
            }
        }

        return file;
    }

    public static boolean saveFile(File file, Bitmap bitmap) {
        FileOutputStream out = null;
        try {
            out = new FileOutputStream(file);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, out);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                if (out != null) {
                    out.close();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return false;
    }


    /**
     * 删除单个文件
     *
     * @param fileName 要删除的文件的文件名
     * @return 单个文件删除成功返回true，否则返回false
     */
    public static boolean deleteFile(String fileName) {
        File file = new File(fileName);
        // 如果文件路径所对应的文件存在，并且是一个文件，则直接删除
        if (file.exists() && file.isFile()) {
            if (file.delete()) {
                return true;
            } else {
                return false;
            }
        } else {
            return false;
        }
    }


    public static void writeTxtToFile(String strContent, String filePath, String fileName) {
        File sdRootFile = Environment.getExternalStorageDirectory();
        makeFilePath(sdRootFile + filePath, fileName);
        String path = sdRootFile + filePath + fileName;
        try {
            FileWriter fw = new FileWriter(sdRootFile + filePath + fileName, true);//SD卡中的路径
            fw.flush();
            fw.write("\r\n" + strContent);
            fw.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    //生成文件

    private static File makeFilePath(String filePath, String fileName) {
        File file = null;
        makeRootDirectory(filePath);
        try {
            file = new File(filePath + fileName);
            if (!file.exists()) {
                file.createNewFile();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return file;
    }

//生成文件夹

    private static void makeRootDirectory(String filePath) {
        File file = null;
        try {
            file = new File(filePath);
            if (!file.exists()) {
                file.mkdir();
            }
        } catch (Exception e) {
            Log.i("error:", e + "");
        }
    }

    public static void copyFilesFromAssets(Context context, String oldPath, String newPath) {
        try {
            String[] fileNames = context.getAssets().list(oldPath);
            if (fileNames.length > 0) {
                // directory
                File file = new File(newPath);
                if (!file.mkdir()) {
                    Log.d("mkdir", "can't make folder");

                }
//                    return false;                // copy recursively
                for (String fileName : fileNames) {
                    copyFilesFromAssets(context, oldPath + "/" + fileName,
                            newPath + "/" + fileName);
                }
            } else {
                // file
                InputStream is = context.getAssets().open(oldPath);
                FileOutputStream fos = new FileOutputStream(new File(newPath));
                byte[] buffer = new byte[1024];
                int byteCount;
                while ((byteCount = is.read(buffer)) != -1) {
                    fos.write(buffer, 0, byteCount);
                }
                fos.flush();
                is.close();
                fos.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
