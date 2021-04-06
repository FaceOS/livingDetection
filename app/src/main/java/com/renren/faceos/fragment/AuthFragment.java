package com.renren.faceos.fragment;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
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
import com.renren.faceos.widget.AuthDialog;
import com.renren.faceos.widget.TimeoutDialog;

public class AuthFragment extends Fragment implements AuthDialog.OnAuthDialogClickListener {

    private AuthDialog authDialog;
    private String url = "http://49.233.242.197:8313/CreditFunc/v2.1/IdNamePhotoCheck";

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

        OkGo.<String>post(url)
//                .upJson(JSON.toJSONString(jsonObject))
                .execute(new StringCallback() {
                    @Override
                    public void onSuccess(Response<String> response) {
                        Log.e("TAG11", response.body().toString());
//                        JSONObject jsonObject = JSON.parseObject(response.body().toString());
//                        String MESSAGE = jsonObject.getString("MESSAGE");
//                        int result1 = jsonObject.getIntValue("RESULT");
//                        JSONObject detail = jsonObject.getJSONObject("detail");
//                        String resultMsg = detail.getString("resultMsg");
//                        result.setText(MESSAGE + " " + resultMsg);

//                        authDialog.setAuthDialogText("认证成功");
//                        authDialog.setAuthDialogImg(R.mipmap.ic_auth_success);


                        authDialog.setAuthDialogText("认证失败");
                        authDialog.setAuthOutText("重新认证");
                        authDialog.setAuthDialogImg(R.mipmap.ic_auth_fail);
                    }

                    @Override
                    public void onError(Response<String> response) {
                        super.onError(response);
                        authDialog.setAuthDialogText("认证失败");
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
        authDialog.dismiss();
        ((MainActivity) getActivity()).initIdentityFragment();
    }
}