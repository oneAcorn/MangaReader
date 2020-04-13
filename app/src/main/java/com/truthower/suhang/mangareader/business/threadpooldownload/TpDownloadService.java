package com.truthower.suhang.mangareader.business.threadpooldownload;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;
import android.widget.RemoteViews;

import com.truthower.suhang.mangareader.R;
import com.truthower.suhang.mangareader.bean.DownloadBean;
import com.truthower.suhang.mangareader.bean.RxDownloadBean;
import com.truthower.suhang.mangareader.bean.RxDownloadChapterBean;
import com.truthower.suhang.mangareader.bean.RxDownloadPageBean;
import com.truthower.suhang.mangareader.business.blockConcurrent.Consumer;
import com.truthower.suhang.mangareader.business.blockConcurrent.LogUtil;
import com.truthower.suhang.mangareader.business.blockConcurrent.Storage;
import com.truthower.suhang.mangareader.business.rxdownload.DownloadCaretaker;
import com.truthower.suhang.mangareader.business.rxdownload.RxDownloadActivity;
import com.truthower.suhang.mangareader.eventbus.EventBusEvent;
import com.truthower.suhang.mangareader.listener.JsoupCallBack;
import com.truthower.suhang.mangareader.listener.MangaDownloader;
import com.truthower.suhang.mangareader.listener.OnResultListener;
import com.truthower.suhang.mangareader.utils.Logger;
import com.truthower.suhang.mangareader.widget.toast.EasyToast;

import org.greenrobot.eventbus.EventBus;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

public class TpDownloadService extends Service implements Consumer.IDispatcher<RxDownloadPageBean> {
    private String TAG = "TpDownloadService";
    private RxDownloadBean downloadBean;
    private EasyToast mEasyToast;
    private MangaDownloader mDownloader;
    private NotificationCompat.Builder notificationBuilder;
    private RemoteViews remoteViews;
    private NotificationManager notificationManager;

    private ExecutorService mExecutorService = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
    private List<RxDownloadChapterBean> chapters;
    private final List<RxDownloadChapterBean> toRemoveChapters = new ArrayList<>();
    private boolean isFinished = false;

    @Override
    public void onCreate() {
        super.onCreate();
        Logger.setTag("TpDownloadService");
        mEasyToast = new EasyToast(this);
        downloadBean = DownloadCaretaker.getDownloadMemoto(this);
        chapters = downloadBean.getChapters();
        mDownloader = downloadBean.getDownloader();
        createNotification(this);
        startForeground(10, notificationBuilder.build());
    }

    private void createNotification(Context context) {
        try {
            notificationBuilder = new NotificationCompat.Builder(context);
            remoteViews = new RemoteViews(context.getPackageName(), R.layout.notification_download);
            notificationManager = (NotificationManager) context.getSystemService
                    (context.NOTIFICATION_SERVICE);
            notificationBuilder.setSmallIcon(R.drawable.spider_128);
            notificationBuilder.setContent(remoteViews);
            notificationBuilder.setDefaults(Notification.DEFAULT_SOUND);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                NotificationChannel mChannel = new NotificationChannel("manga_channel", "manga", NotificationManager.IMPORTANCE_LOW);
                notificationManager.createNotificationChannel(mChannel);
                notificationBuilder.setChannelId("manga_channel");
            }
//        remoteViews.setImageViewResource(R.id.image, R.mipmap.timg);
            remoteViews.setTextViewText(R.id.notification_title_tv, downloadBean.getMangaName() + "下载中...");
            remoteViews.setProgressBar(R.id.notification_download_progress_bar, 10,
                    0, false);
            notificationManager.notify(10, notificationBuilder.build());

            Intent intent = new Intent(context, TpDownloadActivity.class);
            PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent, 0);
            notificationBuilder.setContentIntent(pendingIntent);
        } catch (Exception e) {
            Logger.d("e" + e);
        }
    }

    private void updateNotification(RxDownloadChapterBean chapter) {
        try {
            remoteViews.setProgressBar(R.id.notification_download_progress_bar, chapter.getPageCount()
                    , chapter.getDownloadedCount().get(), false);
            notificationManager.notify(10, notificationBuilder.build());
        } catch (Exception e) {
            Logger.d("e" + e);
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        getChapterInfo();
        return super.onStartCommand(intent, flags, startId);
    }

    private void getChapterInfo() {
        if (null == chapters || chapters.size() == toRemoveChapters.size()) {
            //下载完成
            mEasyToast.showToast(downloadBean.getMangaName() + "全部下载完成!");
            DownloadCaretaker.clean(this);
            EventBus.getDefault().post(new TpDownloadEvent(EventBusEvent.DOWNLOAD_FINISH_EVENT));
            return;
        }
        Storage<RxDownloadPageBean> storage = new Storage<>(30);
        mExecutorService.execute(new PageProducer(this, storage, chapters, mDownloader, downloadBean.getMangaName()));
        int threadCount = Runtime.getRuntime().availableProcessors();
        for (int i = 0; i < threadCount - 1; i++) {
            mExecutorService.execute(new PageConsumer(storage, this));
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mExecutorService.shutdown();
        DownloadCaretaker.saveDownloadMemoto(this, downloadBean);
    }

    @Override
    public synchronized void finish(RxDownloadPageBean page) {
        for (RxDownloadChapterBean chapter : chapters) {
            int downloadCount = 0;
            if (chapter.getChapterName().equals(page.getChapterName())) {
                //不要remove,而是设置一个下载完成标志,防止ConcurrentModificationException
                page.setDownloaded(true);
                EventBus.getDefault().post(new TpDownloadEvent(EventBusEvent.DOWNLOAD_PAGE_FINISH_EVENT, chapter)); //这个章节下载完成一页
                downloadCount = chapter.getDownloadedCount().incrementAndGet();
                updateNotification(chapter);
                LogUtil.i("下完1页 " + page.toString());
            }
            if (chapter.getPages() != null && chapter.getPageCount() == downloadCount) { //此章节下载完成
                toRemoveChapters.add(chapter);
                List<RxDownloadChapterBean> removedChapters = new ArrayList<>(chapters);
                removedChapters.removeAll(toRemoveChapters);
                downloadBean.setChapters(removedChapters);
                EventBus.getDefault().post(new TpDownloadEvent(EventBusEvent.DOWNLOAD_CHAPTER_FINISH_EVENT, downloadBean));
                DownloadCaretaker.saveDownloadMemoto(TpDownloadService.this, downloadBean);
                LogUtil.i("下完一章 " + chapter.getChapterName());
            }
        }
        if (chapters.size() == toRemoveChapters.size()) {
            isFinished = true;
            mExecutorService.shutdownNow();

            LogUtil.i("全部下完");
            //下载完成
            mEasyToast.showToast(downloadBean.getMangaName() + "全部下载完成!");
            DownloadCaretaker.clean(this);
            EventBus.getDefault().post(new TpDownloadEvent(EventBusEvent.DOWNLOAD_FINISH_EVENT));
        }
    }

    @Override
    public boolean isFinished() {
        return isFinished;
    }
}