package com.renren.faceos.fragment;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.lzy.okgo.OkGo;
import com.lzy.okgo.callback.StringCallback;
import com.lzy.okgo.model.Response;
import com.renren.faceos.R;
import com.renren.faceos.base.BaseFragment;
import com.renren.faceos.entity.IdName;
import com.renren.faceos.entity.IdNamePhoto;
import com.renren.faceos.utils.Base64Utils;
import com.renren.faceos.utils.BitmapZoomUtils;
import com.renren.faceos.utils.FaceUtils;
import com.renren.faceos.widget.AuthDialog;

public class AuthFragment extends BaseFragment implements AuthDialog.OnAuthDialogClickListener {

    private AuthDialog authDialog;
    private String url = "http://api.faceos.com:8181/openapi/IdNamePhotoCheck?appKey=yn29zKj7YZ&appScrect=a5633c63300146c8d3b87410a2ef2ced";
    private String faceImgUrl = "http://api.faceos.com:8181/openapi/facelivenessImg?appKey=yn29zKj7YZ&appScrect=a5633c63300146c8d3b87410a2ef2ced";

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
        Bitmap faceData = getMainActivity().faceData;
        String name = getMainActivity().name;
        String idCard = getMainActivity().idCard;
        IdName idName = new IdName();
        idName.setName(name);
        idName.setCardNo(idCard);
//        IdNamePhoto idNamePhoto = new IdNamePhoto();
//        idNamePhoto.setLoginName("faceos");
//        idNamePhoto.setPwd("faceos");
//        idNamePhoto.setServiceName("IdNamePhotoCheck");
//        IdNamePhoto.ParamBean paramBean = new IdNamePhoto.ParamBean();
//        paramBean.setName(name);
//        paramBean.setIdCard(idCard);
        // 图片旋转
        Bitmap rotateBitmap = FaceUtils.bitmapRotation(faceData, 270);
        // 人脸裁剪
//        Bitmap cutFace = FaceUtils.faceCut(rotateBitmap, getContext());
        //这里图片可能是空的
        if (rotateBitmap != null) {
            //图片压缩
            Bitmap bitmap = BitmapZoomUtils.compressScale(rotateBitmap);
//            paramBean.setImage(Base64Utils.bitmapToBase64(bitmap));
//            Log.e("TAG",Base64Utils.bitmapToBase64(bitmap).length()+"===");
//            idNamePhoto.setParam(paramBean);
            idName.setFaceBase64(Base64Utils.bitmapToBase64(bitmap));
            facelivenessImg(idName, Base64Utils.bitmapToBase64(bitmap));
        } else {
            authDialog.setAuthDialogText("认证失败");
            authDialog.setAuthOutText("重新认证");
            authDialog.setAuthDialogImg(R.mipmap.ic_auth_fail);
            authDialog.show();
            toast("人脸采集异常");
        }
    }

    public void toast(String text) {
        Toast.makeText(getContext(), text, Toast.LENGTH_LONG).show();
    }

    private void facelivenessImg(final IdName idName, String imageBase64) {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("imageBase64", imageBase64);
        OkGo.<String>post(faceImgUrl)
                .upJson(jsonObject.toJSONString())
                .execute(new StringCallback() {
                    @Override
                    public void onSuccess(Response<String> response) {

                        JSONObject jsonObject = JSON.parseObject(response.body());
                        int code = jsonObject.getIntValue("code");
                        if (code == 0) {
                            JSONObject data = jsonObject.getJSONObject("data");
                            Float score = data.getFloat("score");
                            if (score > 0.8) {
                                //为活体
                                nameIdCardAuth(idName);
                            } else {
                                authDialog.setAuthDialogText("认证失败");
                                authDialog.setAuthOutText("重新认证");
                                authDialog.setAuthDialogImg(R.mipmap.ic_auth_fail);
                                authDialog.show();
                                toast("活体检测异常");
                            }
                        } else {
                            authDialog.setAuthDialogText("认证失败");
                            authDialog.setAuthOutText("重新认证");
                            authDialog.setAuthDialogImg(R.mipmap.ic_auth_fail);
                            authDialog.show();
                            toast("活体检测异常");
                        }
                    }

                    @Override
                    public void onError(Response<String> response) {
                        super.onError(response);
                        authDialog.setAuthDialogText("认证失败");
                        authDialog.setAuthOutText("重新认证");
                        authDialog.setAuthDialogImg(R.mipmap.ic_auth_fail);
                        authDialog.show();
                        toast("网络错误");
                    }
                });
    }

    private void nameIdCardAuth(IdName idName) {
        OkGo.<String>post(url)
                .upJson(JSON.toJSONString(idName))
                .execute(new StringCallback() {
                    @Override
                    public void onSuccess(Response<String> response) {
                        JSONObject jsonObject = JSON.parseObject(response.body());
                        int result = jsonObject.getIntValue("RESULT");
                        String message = jsonObject.getString("MESSAGE");
                        JSONObject detail = jsonObject.getJSONObject("detail");
                        int resultCode = detail.getIntValue("resultCode");
                        String resultMsg = detail.getString("resultMsg");
                        if (result == 1 && resultCode == 1001) {
                            authDialog.setAuthDialogText("认证成功");
                            authDialog.setAuthOutText("完成");
                            authDialog.setAuthDialogImg(R.mipmap.ic_auth_success);
                        } else {
                            authDialog.setAuthDialogText("认证失败");
                            authDialog.setAuthOutText("重新认证");
                            authDialog.setAuthDialogImg(R.mipmap.ic_auth_fail);
                            toast("认证失败" + message + " " + resultMsg);
                        }
                    }

                    @Override
                    public void onError(Response<String> response) {
                        super.onError(response);
                        authDialog.setAuthDialogText("认证失败");
                        authDialog.setAuthOutText("重新认证");
                        authDialog.setAuthDialogImg(R.mipmap.ic_auth_fail);
                        toast("网络错误");
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
        getMainActivity().initIdentityFragment();
        authDialog.dismiss();
    }
}