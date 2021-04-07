package com.renren.faceos.fragment;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.util.Base64;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.lzy.okgo.OkGo;
import com.lzy.okgo.callback.StringCallback;
import com.lzy.okgo.model.Response;
import com.renren.faceos.MainActivity;
import com.renren.faceos.R;
import com.renren.faceos.entity.IdNamePhoto;
import com.renren.faceos.utils.Base64Utils;
import com.renren.faceos.utils.FaceUtils;
import com.renren.faceos.utils.FastYUVtoRGB;
import com.renren.faceos.widget.AuthDialog;
import com.renren.faceos.widget.TimeoutDialog;

public class AuthFragment extends Fragment implements AuthDialog.OnAuthDialogClickListener {

    private AuthDialog authDialog;
    private String url = "https://49.233.242.197:8313/CreditFunc/v2.1/IdNamePhotoCheck";

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_auth, container, false);
    }

    private void initAuthDialog() {
        authDialog = new AuthDialog(getContext());
        authDialog.setDialogListener(this);
        authDialog.setCanceledOnTouchOutside(false);
        authDialog.setCancelable(false);
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        initAuthDialog();
        Bitmap faceData = ((MainActivity) getActivity()).faceData;
        String name = ((MainActivity) getActivity()).name;
        String idCard = ((MainActivity) getActivity()).idCard;
        IdNamePhoto idNamePhoto = new IdNamePhoto();
        idNamePhoto.setLoginName("faceos");
        idNamePhoto.setPwd("faceos");
        idNamePhoto.setServiceName("IdNamePhotoCheck");
        IdNamePhoto.ParamBean paramBean = new IdNamePhoto.ParamBean();
        paramBean.setName(name);
        paramBean.setIdCard(idCard);
        // 图片旋转
        Bitmap rotateBitmap = FaceUtils.bitmapRotation(faceData, 270);
        // 人脸裁剪
        Bitmap cutFace = FaceUtils.faceCut(rotateBitmap, getContext());
        paramBean.setImage(Base64Utils.bitmapToBase64(cutFace));
        idNamePhoto.setParam(paramBean);
        OkGo.<String>post(url)
                .upJson(JSON.toJSONString(idNamePhoto))
                .execute(new StringCallback() {
                    @Override
                    public void onSuccess(Response<String> response) {
                        JSONObject jsonObject = JSON.parseObject(response.body());
                        int result = jsonObject.getIntValue("RESULT");
                        if (result == 1) {
                            authDialog.setAuthDialogText("认证成功");
                            authDialog.setAuthOutText("确定");
                            authDialog.setAuthDialogImg(R.mipmap.ic_auth_success);
                        } else {
                            authDialog.setAuthDialogText("认证失败");
                            authDialog.setAuthOutText("重新认证");
                            authDialog.setAuthDialogImg(R.mipmap.ic_auth_fail);
                        }
                    }

                    @Override
                    public void onError(Response<String> response) {
                        super.onError(response);
                        authDialog.setAuthDialogText("认证失败");
                        authDialog.setAuthOutText("重新认证");
                        authDialog.setAuthDialogImg(R.mipmap.ic_auth_fail);
                    }

                    @Override
                    public void onFinish() {
                        super.onFinish();
                        authDialog.show();
                    }
                });

    }

    @Override
    public void onReturn() {
        String authDialogText = authDialog.getAuthDialogText();
        if (authDialogText.equals("认证成功")) {

        } else {

        }
        ((MainActivity) getActivity()).initIdentityFragment();
        authDialog.dismiss();
    }
}