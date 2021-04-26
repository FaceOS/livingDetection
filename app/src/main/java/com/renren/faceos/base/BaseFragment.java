package com.renren.faceos.base;

import android.support.v4.app.Fragment;

import com.renren.faceos.MainActivity;

public class BaseFragment extends Fragment {


    public MainActivity getMainActivity() {
        return ((MainActivity) getActivity());
    }
}