package com.truthower.suhang.mangareader.bean;

import androidx.annotation.NonNull;

/**
 * Created by Administrator on 2017/7/18.
 */

public class RxDownloadPageBean extends BaseBean {
    private String pageUrl;
    private String pageName;
    private String chapterName;
    private String mangaName;
    private volatile boolean isDownloaded;

    public String getPageUrl() {
        return pageUrl;
    }

    public void setPageUrl(String pageUrl) {
        this.pageUrl = pageUrl;
    }

    public String getPageName() {
        return pageName;
    }

    public void setPageName(String pageName) {
        this.pageName = pageName;
    }

    public String getChapterName() {
        return chapterName;
    }

    public void setChapterName(String chapterName) {
        this.chapterName = chapterName;
    }

    public boolean isDownloaded() {
        return isDownloaded;
    }

    public void setDownloaded(boolean downloaded) {
        isDownloaded = downloaded;
    }

    public String getMangaName() {
        return mangaName;
    }

    public void setMangaName(String mangaName) {
        this.mangaName = mangaName;
    }

    @NonNull
    @Override
    public String toString() {
        return pageName;
    }
}
