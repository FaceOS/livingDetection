package com.renren.faceos.utils;

import android.content.Context;
import android.os.Build;
import android.text.TextUtils;

public class DensityUtils {

    private static final float DOT_FIVE = 0.5F;
    private static final int PORTRAIT_DEGREE_90 = 90;
    private static final int PORTRAIT_DEGREE_270 = 270;
    private static final String[] BUILD_MODELS = new String[]{"i700v", "A862W", "V8526"};

    private DensityUtils() {
    }

    public static int sp2px(Context context, float spValue) {
        float fontScale = context.getResources().getDisplayMetrics().scaledDensity;
        return (int)(spValue * fontScale + 0.5F);
    }

    public static int dip2px(Context context, float dip) {
        float density = getDensity(context);
        return (int)(dip * density + 0.5F);
    }

    public static int px2dip(Context context, float px) {
        float density = getDensity(context);
        return (int)(px / density + 0.5F);
    }

    public static int getDisplayWidth(Context context) {
        return context.getResources().getDisplayMetrics().widthPixels;
    }

    public static int getDisplayHeight(Context context) {
        return context.getResources().getDisplayMetrics().heightPixels;
    }

    public static float getDensity(Context context) {
        return context.getResources().getDisplayMetrics().density;
    }

    public static int getDensityDpi(Context context) {
        return context.getResources().getDisplayMetrics().densityDpi;
    }

    public static int getPortraitDegree() {
        int degree = 90;
        String[] var1 = BUILD_MODELS;
        int var2 = var1.length;

        for(int var3 = 0; var3 < var2; ++var3) {
            String model = var1[var3];
            if (TextUtils.equals(model, Build.MODEL)) {
                degree = 270;
                break;
            }
        }

        return degree;
    }
}
