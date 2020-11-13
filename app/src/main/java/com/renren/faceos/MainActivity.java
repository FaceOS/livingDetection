package com.renren.faceos;


import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
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
import com.renren.faceos.entity.IdNamePhoto;
import com.renren.faceos.utils.Base64Utils;
import com.renren.faceos.utils.FaceUtils;
import com.renren.faceos.utils.FileUtils;
import com.renren.faceos.utils.PermissionsUtil;

import java.io.File;
import java.io.FileInputStream;

public class MainActivity extends AppCompatActivity implements PermissionsUtil.IPermissionsCallback {
    private static final String TAG = MainActivity.class.getSimpleName();

    Button livingCheck;
    ImageView imageView;
    EditText idCard;
    EditText name;
    LinearLayout realLayout;
    Bitmap cutFace;
    String url = "https://49.233.242.197:8313/CreditFunc/v2.1/IdNamePhotoCheck";
    TextView result;
    PermissionsUtil request;

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
        realLayout.setVisibility(View.INVISIBLE);

        request = PermissionsUtil
                .with(this)
                .requestCode(1)
                .isDebug(true)//开启log
                .permissions(PermissionsUtil.Permission.Storage.WRITE_EXTERNAL_STORAGE,
                        PermissionsUtil.Permission.Camera.CAMERA)
                .request();

        livingCheck.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, FaceLivenessActivity.class);
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
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        request.onRequestPermissionsResult(requestCode, permissions, grantResults);
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 0 && resultCode == 1) {
            Log.e(TAG, "resultCode" + resultCode);
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
            } catch (Exception e) {
                e.printStackTrace();
            }

        }
    }

    @Override
    public void onPermissionsGranted(int requestCode, String... permission) {
        String assetPath = "faceos";
        String sdcardPath = Environment.getExternalStorageDirectory()
                + File.separator + assetPath;
        FileUtils.copyFilesFromAssets(this, assetPath, sdcardPath);
    }

    @Override
    public void onPermissionsDenied(int requestCode, String... permission) {
        finish();
    }


}

