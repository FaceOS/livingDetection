package com.renren.faceos;


import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.renren.faceos.fragment.AuthFragment;
import com.renren.faceos.fragment.DetectFragment;
import com.renren.faceos.fragment.IdentityFragment;
import com.renren.faceos.utils.FileUtils;
import com.renren.faceos.utils.PermissionsUtil;

import java.io.File;

public class MainActivity extends AppCompatActivity implements PermissionsUtil.IPermissionsCallback, View.OnClickListener {
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
    public int currentBright;
    public ImageView commonTitleLeftButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        line1 = findViewById(R.id.line_1);
        step2 = findViewById(R.id.step_2);
        line2 = findViewById(R.id.line_2);
        step3 = findViewById(R.id.step_3);
        commonTitleLeftButton = findViewById(R.id.common_title_left);
        commonTitleLeftButton.setOnClickListener(this);
        request = PermissionsUtil
                .with(this)
                .requestCode(1)
                .isDebug(true)//开启log
                .permissions(PermissionsUtil.Permission.Camera.CAMERA)
                .request();

        initIdentityFragment();

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

    @Override
    public void onClick(View v) {
        if (detectFragment != null && detectFragment.isVisible()) {
            detectFragment.returnIdentityFragment();
        }
    }

    //声明一个long类型变量：用于存放上一点击“返回键”的时刻
    private long mExitTime;

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        //判断用户是否点击了“返回键”
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            if (detectFragment != null && detectFragment.isVisible()) {
                detectFragment.returnIdentityFragment();
                return false;
            }
            //与上次点击返回键时刻作差
            if ((System.currentTimeMillis() - mExitTime) > 2000) {
                //大于2000ms则认为是误操作，使用Toast进行提示
                Toast.makeText(this, "再按一次退出程序", Toast.LENGTH_SHORT).show();
                //并记录下本次点击“返回键”的时刻，以便下次进行判断
                mExitTime = System.currentTimeMillis();
            } else {
                //小于2000ms则认为是用户确实希望退出程序-调用System.exit()方法进行退出
                System.exit(0);
            }
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }
}

