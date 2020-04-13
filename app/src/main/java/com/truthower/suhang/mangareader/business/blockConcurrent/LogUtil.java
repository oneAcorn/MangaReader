package com.truthower.suhang.mangareader.business.blockConcurrent;

import android.util.Log;

import java.util.Date;

/**
 * Created by acorn on 2020/4/11.
 */
public class LogUtil {
    private static final String TAG = "beaver";

    public static void i(String msg) {
        Log.i(TAG, msg);
    }

    public static void e(String msg) {
        Log.e(TAG, msg);
    }
}
