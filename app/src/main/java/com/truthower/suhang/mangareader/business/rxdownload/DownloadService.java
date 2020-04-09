package com.truthower.suhang.mangareader.business.rxdownload;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.os.IBinder;
import android.widget.RemoteViews;

import com.truthower.suhang.mangareader.R;
import com.truthower.suhang.mangareader.bean.RxDownloadBean;
import com.truthower.suhang.mangareader.bean.RxDownloadChapterBean;
import com.truthower.suhang.mangareader.bean.RxDownloadPageBean;
import com.truthower.suhang.mangareader.business.download.DownloadActivity;
import com.truthower.suhang.mangareader.eventbus.EventBusEvent;
import com.truthower.suhang.mangareader.listener.JsoupCallBack;
import com.truthower.suhang.mangareader.listener.MangaDownloader;
import com.truthower.suhang.mangareader.spider.FileSpider;
import com.truthower.suhang.mangareader.utils.Logger;
import com.truthower.suhang.mangareader.widget.toast.EasyToast;

import org.greenrobot.eventbus.EventBus;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import io.reactivex.Flowable;
import io.reactivex.Observable;
import io.reactivex.ObservableEmitter;
import io.reactivex.ObservableOnSubscribe;
import io.reactivex.ObservableSource;
import io.reactivex.Observer;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Consumer;
import io.reactivex.functions.Function;
import io.reactivex.schedulers.Schedulers;

public class DownloadService extends Service {
    private RxDownloadBean downloadBean;
    private ArrayList<RxDownloadChapterBean> chapters;
    private Subscription mDisposable;
    private EasyToast mEasyToast;
    private MangaDownloader mDownloader;
    private NotificationCompat.Builder notificationBuilder;
    private RemoteViews remoteViews;
    private NotificationManager notificationManager;

    @Override
    public void onCreate() {
        super.onCreate();
        createNotification(this);
        startForeground(10, notificationBuilder.build());
        mEasyToast = new EasyToast(this);
        downloadBean = DownloadCaretaker.getDownloadMemoto(this);
        chapters = downloadBean.getChapters();
        mDownloader = downloadBean.getDownloader();
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

            Intent intent = new Intent(context, RxDownloadActivity.class);
            PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent, 0);
            notificationBuilder.setContentIntent(pendingIntent);
        } catch (Exception e) {
            Logger.d("e" + e);
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Flowable.fromIterable(chapters)
                .onBackpressureBuffer()
                .subscribeOn(Schedulers.computation())
                .observeOn(Schedulers.computation())
                .concatMap(new Function<RxDownloadChapterBean, Publisher<ArrayList<RxDownloadPageBean>>>() {
                    @Override
                    public Publisher<ArrayList<RxDownloadPageBean>> apply(final RxDownloadChapterBean bean) throws Exception {
                        return new Publisher<ArrayList<RxDownloadPageBean>>() {
                            @Override
                            public void subscribe(final Subscriber<? super ArrayList<RxDownloadPageBean>> s) {
                                if (null != bean.getPages() && bean.getPages().size() > 0) {
                                    //之前获取过该章节的图片地址的情况
                                    s.onNext(bean.getPages());
                                } else {
                                    mDownloader.getMangaChapterPics(DownloadService.this, bean.getChapterUrl(), new JsoupCallBack<ArrayList<String>>() {
                                        @Override
                                        public void loadSucceed(ArrayList<String> result) {
                                            ArrayList<RxDownloadPageBean> pages = new ArrayList<>();
                                            for (int i = 0; i < result.size(); i++) {
                                                RxDownloadPageBean item = new RxDownloadPageBean();
                                                item.setPageUrl(result.get(i));
                                                item.setPageName(downloadBean.getMangaName() + "_" + bean.getChapterName() + "_" + i + ".png");
                                                item.setChapterName(bean.getChapterName());
                                                pages.add(item);
                                            }
                                            bean.setPages(pages);
                                            bean.setPageCount(result.size());
                                            s.onNext(pages);
                                        }

                                        @Override
                                        public void loadFailed(String error) {
                                            s.onError(new Exception());
                                        }
                                    });
                                }
                            }
                        };
                    }
                })
                .flatMap(new Function<ArrayList<RxDownloadPageBean>, Publisher<RxDownloadPageBean>>() {
                    @Override
                    public Publisher<RxDownloadPageBean> apply(final ArrayList<RxDownloadPageBean> beans) throws Exception {
                        return new Publisher<RxDownloadPageBean>() {
                            @Override
                            public void subscribe(Subscriber<? super RxDownloadPageBean> s) {
                                for (RxDownloadPageBean item : beans) {
                                    s.onNext(item);
                                }
                            }
                        };
                    }
                })
                .subscribe(new Subscriber<RxDownloadPageBean>() {
                    @Override
                    public void onSubscribe(Subscription s) {
                        mDisposable = s;
                    }

                    @Override
                    public void onNext(RxDownloadPageBean bean) {
                        Bitmap bp = downloadUrlBitmap(bean.getPageUrl());
                        //把图片保存到本地
                        FileSpider.getInstance().saveBitmap(bp, bean.getPageName(), bean.getChapterName(), downloadBean.getMangaName());
//                        for (int i = 0; i < chapters.size(); i++) {
//                            if (value.getChapterName().equals(chapters.get(i).getChapterName())) {
//                                chapters.get(i).getPages().remove(value);
//                                EventBus.getDefault().post(new RxDownloadEvent(EventBusEvent.DOWNLOAD_PAGE_FINISH_EVENT, downloadBean, i));
//                                if (chapters.get(i).getPages().size() <= 0) {
//                                    EventBus.getDefault().post(new RxDownloadEvent(EventBusEvent.DOWNLOAD_CHAPTER_FINISH_EVENT, downloadBean, i));
//                                }
//                                return;
//                            }
//                        }
                    }

                    @Override
                    public void onError(Throwable t) {

                    }

                    @Override
                    public void onComplete() {
                        mEasyToast.showToast("全部下载完成!");
                        EventBus.getDefault().post(new RxDownloadEvent(EventBusEvent.DOWNLOAD_FINISH_EVENT));
                    }
                });
        return super.onStartCommand(intent, flags, startId);
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

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mDisposable.cancel();
        DownloadCaretaker.saveDownloadMemoto(this, downloadBean);
    }
}
