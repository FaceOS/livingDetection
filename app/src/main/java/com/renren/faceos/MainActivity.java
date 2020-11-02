package com.renren.faceos;


import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Environment;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.lzy.okgo.OkGo;
import com.lzy.okgo.callback.StringCallback;
import com.lzy.okgo.model.Response;
import com.renren.faceos.utils.Base64Utils;
import com.renren.faceos.utils.FaceUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";

    Button livingCheck;
    ImageView imageView;
    private static final int REQUEST_EXTERNAL_STORAGE = 1;
    private static String[] PERMISSIONS_STORAGE = {
            "android.permission.READ_EXTERNAL_STORAGE",
            "android.permission.WRITE_EXTERNAL_STORAGE",
            "android.permission.CAMERA"};
    EditText idCard;
    EditText name;
    LinearLayout realLayout;
    Bitmap cutFace;
    String url = "https://49.233.242.197:8313/CreditFunc/v2.1/IdNamePhotoCheck";
    TextView result;

    public static void verifyStoragePermissions(Activity activity) {
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


    public void copyFilesFromAssets(Context context, String oldPath, String newPath) {
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
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    void InitModelFiles() {

        String assetPath = "faceos";
        String sdcardPath = Environment.getExternalStorageDirectory()
                + File.separator + assetPath;
        copyFilesFromAssets(this, assetPath, sdcardPath);

    }



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        livingCheck = (Button) findViewById(R.id.living_check);
        final Button realCheck = findViewById(R.id.real_check);
        realLayout = findViewById(R.id.real_layout);
        idCard = findViewById(R.id.id_card);
        name = findViewById(R.id.name);
        imageView = findViewById(R.id.face);
        result = findViewById(R.id.result);
        verifyStoragePermissions(this);
        InitModelFiles();
        realLayout.setVisibility(View.INVISIBLE);

        livingCheck.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, TrackActivity.class);
                startActivityForResult(intent, 0);
            }
        });

        realCheck.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String idCardStr = idCard.getText().toString();
                String nameStr = name.getText().toString();

                if (!TextUtils.isEmpty(idCardStr) && !TextUtils.isEmpty(nameStr)) {

                    IdNamePhoto idNamePhoto = new IdNamePhoto();
                    //替换自己的用户名
                    idNamePhoto.setLoginName("innerTest");
                    //替换自己的密码
                    idNamePhoto.setPwd("innerRenRen");
                    idNamePhoto.setServiceName("IdNamePhotoCheck");
                    IdNamePhoto.ParamBean param = new IdNamePhoto.ParamBean();
                    param.setName(nameStr);
                    param.setIdCard(idCardStr);
                    if (cutFace != null)
                        param.setImage(Base64Utils.bitmapToBase64(cutFace));
                    idNamePhoto.setParam(param);
                    OkGo.<String>post(url)
                            .upJson(JSON.toJSONString(idNamePhoto))
                            .execute(new StringCallback() {
                                @Override
                                public void onSuccess(Response<String> response) {
                                    Log.e("TAG11", response.body().toString());
                                    JSONObject jsonObject = JSON.parseObject(response.body().toString());
                                    String MESSAGE = jsonObject.getString("MESSAGE");
                                    int result1 = jsonObject.getIntValue("RESULT");
                                    result.setText(MESSAGE);


                                }
                            });

                } else {
                    Toast.makeText(MainActivity.this, "必填项不能为空", Toast.LENGTH_SHORT).show();
                }
            }
        });

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 0 && resultCode == 1) {
            try {
                String facePath = data.getStringExtra("facePath");
                FileInputStream fis = new FileInputStream(facePath);
                Bitmap bitmap = BitmapFactory.decodeStream(fis);
                // 图片旋转
                Bitmap rotateBitmap = FaceUtils.bitmapRotation(bitmap, 270);
//                // 人脸裁剪
                cutFace = FaceUtils.faceCut(rotateBitmap, this);
                imageView.setImageBitmap(cutFace);
                realLayout.setVisibility(View.VISIBLE);
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }

        }
    }
}

