package com.renren.faceos.fragment;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.renren.faceos.MainActivity;
import com.renren.faceos.R;

public class IdentityFragment extends Fragment implements View.OnClickListener {
    EditText name;
    EditText idCard;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_identity, container, false);
        Button submit = view.findViewById(R.id.submit);
        name = view.findViewById(R.id.name);
        idCard = view.findViewById(R.id.id_card);
        submit.setOnClickListener(this);
        return view;
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
    }


    @Override
    public void onClick(View view) {
        name.setText("张三");
        idCard.setText("110111111111111111");
        String nameStr = name.getText().toString();
        String idCardStr = idCard.getText().toString();
        if (TextUtils.isEmpty(nameStr)) {
            Toast.makeText(getContext(), "姓名不能为空", Toast.LENGTH_SHORT).show();
            return;
        }
        if (TextUtils.isEmpty(idCardStr)) {
            Toast.makeText(getContext(), "身份证号不能为空", Toast.LENGTH_SHORT).show();
            return;
        }
        if (idCardStr.length() < 18) {
            Toast.makeText(getContext(), "身份证号不正确", Toast.LENGTH_SHORT).show();
            return;
        }
        ((MainActivity) getActivity()).name = nameStr;
        ((MainActivity) getActivity()).idCard = idCardStr;
        ((MainActivity) getActivity()).initDetectFragment();
    }
}