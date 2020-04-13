package com.truthower.suhang.mangareader.bean;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class RxDownloadChapterBean extends BaseBean {
    private String chapterUrl;
    private String chapterName;
    private AtomicInteger downloadedCount=new AtomicInteger();
    private int pageCount;
    private List<RxDownloadPageBean> pages;

    public String getChapterUrl() {
        return chapterUrl;
    }

    public void setChapterUrl(String chapterUrl) {
        this.chapterUrl = chapterUrl;
    }

    public String getChapterName() {
        return chapterName;
    }

    public void setChapterName(String chapterName) {
        this.chapterName = chapterName;
    }

    public List<RxDownloadPageBean> getPages() {
        return pages;
    }

    public void setPages(List<RxDownloadPageBean> pages) {
        this.pages = pages;
    }

    public int getPageCount() {
        return pageCount;
    }


    public void setPageCount(int pageCount) {
        this.pageCount = pageCount;
    }

    public AtomicInteger getDownloadedCount() {
        return downloadedCount;
    }

    public void setDownloadedCount(AtomicInteger downloadedCount) {
        this.downloadedCount = downloadedCount;
    }

    public boolean isDownloaded() {
        return downloadedCount.get() >= pageCount;
    }

    public int getDownloadCount() {
        if (null == pages || pages.size() == 0) {
            return 0;
        }
        int count = 0;
        for (RxDownloadPageBean item : pages) {
            if (item.isDownloaded()) {
                count++;
            }
        }
        return count;
    }

}
