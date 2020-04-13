package com.truthower.suhang.mangareader.business.threadpooldownload;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import com.truthower.suhang.mangareader.bean.RxDownloadPageBean;
import com.truthower.suhang.mangareader.business.blockConcurrent.Consumer;
import com.truthower.suhang.mangareader.business.blockConcurrent.LogUtil;
import com.truthower.suhang.mangareader.business.blockConcurrent.Storage;
import com.truthower.suhang.mangareader.spider.FileSpider;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * Created by acorn on 2020/4/12.
 */
public class PageConsumer extends Consumer<RxDownloadPageBean> {
    public PageConsumer(Storage<RxDownloadPageBean> storage, IDispatcher<RxDownloadPageBean> dispatcher) {
        super(storage, dispatcher);
    }

    @Override
    protected void execute(RxDownloadPageBean page, IDispatcher<RxDownloadPageBean> dispatcher) throws InterruptedException {
        LogUtil.i("Consumer downloading " + page.toString());
        Bitmap bp = downloadUrlBitmap(page);
        //把图片保存到本地
        FileSpider.getInstance().saveBitmap(bp, page.getPageName(), page.getChapterName(), page.getMangaName());
        page.setDownloaded(true);
        dispatcher.finish(page);
    }

    private Bitmap downloadUrlBitmap(RxDownloadPageBean page) {
        HttpURLConnection urlConnection = null;
        BufferedInputStream in = null;
        Bitmap bitmap = null;
        try {
            final URL url = new URL(page.getPageUrl());
            urlConnection = (HttpURLConnection) url.openConnection();
            in = new BufferedInputStream(urlConnection.getInputStream(), 8 * 1024);
            bitmap = BitmapFactory.decodeStream(in);
        } catch (final IOException e) {
            e.printStackTrace();
            LogUtil.i("Consumer IOException" + page.toString());
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
}
