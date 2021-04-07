package com.renren.faceos.widget;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.renren.faceos.R;
import com.renren.faceos.utils.DensityUtils;


/**
 * 超时弹窗
 * Created by v_liujialu01 on 2020/4/7.
 */

public class AuthDialog extends Dialog implements View.OnClickListener {
    private Context mContext;
    private OnAuthDialogClickListener mOnTimeoutDialogClickListener;
    private TextView authDialogText;
    private ImageView authDialogImg;
    private Button authOut;
    private String text;
    private String authOutText;
    private int resId;

    public AuthDialog(@NonNull Context context) {
        super(context, R.style.DefaultDialog);
        mContext = context;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        initView();
    }

    private void initView() {
        LayoutInflater inflater = LayoutInflater.from(mContext);
        View view = inflater.inflate(R.layout.auth_dialog, null);
        setContentView(view);
        Window dialogWindow = getWindow();
        dialogWindow.setGravity(Gravity.CENTER_HORIZONTAL);
        WindowManager.LayoutParams lp = dialogWindow.getAttributes();
        int widthPx = DensityUtils.getDisplayWidth(getContext());
        int dp = DensityUtils.px2dip(getContext(), widthPx) - 100;
        lp.width = DensityUtils.dip2px(getContext(), dp);
        lp.height = WindowManager.LayoutParams.WRAP_CONTENT;
        dialogWindow.setAttributes(lp);
        authOut = view.findViewById(R.id.auth_out);
        authDialogText = view.findViewById(R.id.auth_dialog_text);
        authDialogText.setText(text);
        authDialogImg = view.findViewById(R.id.auth_dialog_img);
        authDialogImg.setImageResource(resId);
        authOut.setText(authOutText);
        authOut.setOnClickListener(this);
    }

    public void setDialogListener(OnAuthDialogClickListener listener) {
        mOnTimeoutDialogClickListener = listener;
    }

    public void setAuthDialogText(String text) {
        this.text = text;
    }

    public String getAuthDialogText() {
        return authDialogText.getText().toString();
    }

    public void setAuthDialogImg(int resId) {
        this.resId = resId;
    }

    public void setAuthOutText(String authOutText) {
        this.authOutText = authOutText;
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.auth_out:
                if (mOnTimeoutDialogClickListener != null) {
                    mOnTimeoutDialogClickListener.onReturn();
                }
                break;
        }
    }

    public interface OnAuthDialogClickListener {
        void onReturn();
    }
}
