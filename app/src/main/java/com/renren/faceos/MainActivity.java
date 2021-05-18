package com.renren.faceos;


import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.v4.app.FragmentTransaction;
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
import com.renren.faceos.fragment.AuthFragment;
import com.renren.faceos.fragment.DetectFragment;
import com.renren.faceos.fragment.IdentityFragment;
import com.renren.faceos.utils.Base64Utils;
import com.renren.faceos.utils.FaceUtils;
import com.renren.faceos.utils.FileUtils;
import com.renren.faceos.utils.PermissionsUtil;

import java.io.File;
import java.io.FileInputStream;

public class MainActivity extends AppCompatActivity implements PermissionsUtil.IPermissionsCallback {
    private static final String TAG = MainActivity.class.getSimpleName();

    private PermissionsUtil request;
    private IdentityFragment identityFragment;
    private DetectFragment detectFragment;
    private AuthFragment authFragment;
    private View line1;
    private TextView step2;
    private View line2;
    private TextView step3;
    public Bitmap faceData;
    public String name;
    public String idCard;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        line1 = findViewById(R.id.line_1);
        step2 = findViewById(R.id.step_2);
        line2 = findViewById(R.id.line_2);
        step3 = findViewById(R.id.step_3);
        request = PermissionsUtil
                .with(this)
                .requestCode(1)
                .isDebug(true)//开启log
                .permissions(PermissionsUtil.Permission.Camera.CAMERA)
                .request();

        initIdentityFragment();
//        initAuthFragment();
//        initDetectFragment();

    }

    public void initIdentityFragment() {
        line1.setBackgroundResource(R.color.line_gray);
        step2.setBackgroundResource(R.mipmap.ic_round_gray);
        line2.setBackgroundResource(R.color.line_gray);
        step3.setBackgroundResource(R.mipmap.ic_round_gray);

        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
        if (identityFragment == null) {
            identityFragment = new IdentityFragment();
            ft.add(R.id.fragment, identityFragment);
        }
        if (detectFragment != null) {
            ft.remove(detectFragment);
            detectFragment = null;
        }
        if (authFragment != null) {
            ft.remove(authFragment);
            authFragment = null;
        }
        ft.show(identityFragment);
        ft.commit();
    }

    public void initDetectFragment() {
        line1.setBackgroundResource(R.color.stepBlue);
        step2.setBackgroundResource(R.mipmap.ic_round_blue);
        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
        if (detectFragment == null) {
            detectFragment = new DetectFragment();
            ft.add(R.id.fragment, detectFragment);
        }
        if (identityFragment != null) {
            ft.hide(identityFragment);
        }
        ft.show(detectFragment);
        ft.commit();
    }

    public void initAuthFragment() {
        line2.setBackgroundResource(R.color.stepBlue);
        step3.setBackgroundResource(R.mipmap.ic_round_blue);
        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
        if (authFragment == null) {
            authFragment = new AuthFragment();
            ft.add(R.id.fragment, authFragment);
        }
        if (detectFragment != null) {
            ft.remove(detectFragment);
            detectFragment = null;
        }
        ft.show(authFragment);
        ft.commit();
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        request.onRequestPermissionsResult(requestCode, permissions, grantResults);
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

    }


    @Override
    public void onPermissionsGranted(int requestCode, String... permission) {
        String assetPath = "faceos";
        String sdcardPath = getFilesDir().getPath() + File.separator + assetPath;
        FileUtils.copyFilesFromAssets(this, assetPath, sdcardPath);
    }

    @Override
    public void onPermissionsDenied(int requestCode, String... permission) {
        finish();
    }

}

