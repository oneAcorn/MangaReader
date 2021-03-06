package com.truthower.suhang.mangareader.business.threadpooldownload;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import com.truthower.suhang.mangareader.bean.RxDownloadPageBean;
import com.truthower.suhang.mangareader.listener.OnResultListener;
import com.truthower.suhang.mangareader.spider.FileSpider;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;

public class PageDownloadRunner implements Runnable {
    private RxDownloadPageBean mPageBean;
    private OnResultListener mOnResultListener;

    public PageDownloadRunner(RxDownloadPageBean pageBean,OnResultListener onResultListener) {
        mPageBean = pageBean;
        mOnResultListener=onResultListener;
    }

    @Override
    public void run() {
        Bitmap bp = downloadUrlBitmap(mPageBean.getPageUrl());
        //把图片保存到本地
        FileSpider.getInstance().saveBitmap(bp, mPageBean.getPageName(), mPageBean.getChapterName(), mPageBean.getMangaName());
        mPageBean.setDownloaded(true);
        if (null != mOnResultListener) {
            mOnResultListener.onFinish();
        }
    }

    private Bitmap downloadUrlBitmap(String urlString) {
        HttpURLConnection urlConnection = null;
        BufferedInputStream in = null;
        Bitmap bitmap = null;
        try {
            final URL url = new URL(urlString);
            urlConnection = (HttpURLConnection) url.openConnection();
            in = new BufferedInputStream(urlConnection.getInputStream(), 8 * 1024);
            bitmap = BitmapFactory.decodeStream(in);
        } catch (final IOException e) {
            e.printStackTrace();
        } finally {
            if (urlConnection != null) {
                urlConnection.disconnect();
            }
            try {
                if (in != null) {
                    in.close();
                }
            } catch (final IOException e) {
                e.printStackTrace();
            }
        }
        return bitmap;
    }

    public void setOnResultListener(OnResultListener onResultListener) {
        mOnResultListener = onResultListener;
    }
}
