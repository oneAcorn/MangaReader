package com.truthower.suhang.mangareader.business.threadpooldownload;

import android.content.Context;

import com.truthower.suhang.mangareader.bean.RxDownloadChapterBean;
import com.truthower.suhang.mangareader.bean.RxDownloadPageBean;
import com.truthower.suhang.mangareader.business.blockConcurrent.LogUtil;
import com.truthower.suhang.mangareader.business.blockConcurrent.Producer;
import com.truthower.suhang.mangareader.business.blockConcurrent.Storage;
import com.truthower.suhang.mangareader.listener.JsoupCallBack;
import com.truthower.suhang.mangareader.listener.MangaDownloader;
import com.truthower.suhang.mangareader.utils.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.Vector;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Created by acorn on 2020/4/12.
 */
public class PageProducer extends Producer<RxDownloadPageBean> {
    private final Context mContext;
    private final List<RxDownloadChapterBean> mChapters;
    private final MangaDownloader mDownloader;
    private final String mMangaName;

    public PageProducer(Context context, Storage<RxDownloadPageBean> storage, List<RxDownloadChapterBean> chapters,
                        MangaDownloader mangaDownloader, String mangaName) {
        super(storage);
        mContext = context;
        mChapters = chapters;
        mDownloader = mangaDownloader;
        mMangaName = mangaName;
    }

    @Override
    protected void execute(final Storage<RxDownloadPageBean> storage) throws InterruptedException {
        for (final RxDownloadChapterBean chapter : mChapters) {
            LogUtil.i("Producer chapter "+chapter.getChapterName());
            if (null != chapter.getPages() && chapter.getPages().size() > 0) {
                LogUtil.i("Producer 之前获取过图片地址 "+chapter.getChapterName());
                //之前获取过该章节的图片地址的情况
                for (RxDownloadPageBean page : chapter.getPages()) {
                    if (!page.isDownloaded()) {
                        storage.put(page);
                    }
                }
            } else {
                mDownloader.getMangaChapterPics(mContext, chapter.getChapterUrl(), new JsoupCallBack<ArrayList<String>>() {
                    @Override
                    public void loadSucceed(ArrayList<String> result) {
                        List<RxDownloadPageBean> pages = new ArrayList<>();
                        for (int i = 0; i < result.size(); i++) {
                            RxDownloadPageBean page = new RxDownloadPageBean();
                            page.setPageUrl(result.get(i));
                            page.setPageName(mMangaName + "_" + chapter.getChapterName() + "_" + i + ".png");
                            page.setChapterName(chapter.getChapterName());
                            page.setMangaName(mMangaName);
                            pages.add(page);
                        }
                        chapter.setPages(pages);
                        chapter.setPageCount(result.size());
                        for (RxDownloadPageBean page : pages) {
                            try {
                                storage.put(page);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                        }
                    }

                    @Override
                    public void loadFailed(String error) {
                        LogUtil.i("PageProducer chapter load failed: " + error);
                    }
                });
            }
        }
    }
}
