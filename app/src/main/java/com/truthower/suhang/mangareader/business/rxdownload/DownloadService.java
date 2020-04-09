package com.truthower.suhang.mangareader.business.rxdownload;

import android.app.Service;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.IBinder;

import com.truthower.suhang.mangareader.bean.RxDownloadBean;
import com.truthower.suhang.mangareader.bean.RxDownloadChapterBean;
import com.truthower.suhang.mangareader.bean.RxDownloadPageBean;
import com.truthower.suhang.mangareader.eventbus.EventBusEvent;
import com.truthower.suhang.mangareader.listener.JsoupCallBack;
import com.truthower.suhang.mangareader.listener.MangaDownloader;
import com.truthower.suhang.mangareader.spider.FileSpider;
import com.truthower.suhang.mangareader.widget.toast.EasyToast;

import org.greenrobot.eventbus.EventBus;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;

import androidx.annotation.Nullable;
import io.reactivex.Observable;
import io.reactivex.ObservableEmitter;
import io.reactivex.ObservableOnSubscribe;
import io.reactivex.ObservableSource;
import io.reactivex.Observer;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Function;
import io.reactivex.schedulers.Schedulers;

public class DownloadService extends Service {
    private RxDownloadBean downloadBean;
    private ArrayList<RxDownloadChapterBean> chapters;
    private Disposable mDisposable;
    private EasyToast mEasyToast;
    private MangaDownloader mDownloader;

    @Override
    public void onCreate() {
        super.onCreate();
        mEasyToast = new EasyToast(this);
        downloadBean = DownloadCaretaker.getDownloadMemoto(this);
        chapters = downloadBean.getChapters();
        mDownloader = downloadBean.getDownloader();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Observable.fromIterable(chapters)
                .subscribeOn(Schedulers.io())
                .observeOn(Schedulers.io())
                //map是应用用同步的转换,这里显然只能用flatmap或concatmap 由于需要尽量按顺序下载 这里使用concatmap
                .concatMap(new Function<RxDownloadChapterBean, ObservableSource<ArrayList<RxDownloadPageBean>>>() {
                    @Override
                    public ObservableSource<ArrayList<RxDownloadPageBean>> apply(final RxDownloadChapterBean bean) throws Exception {
                        return Observable.create(new ObservableOnSubscribe<ArrayList<RxDownloadPageBean>>() {
                            @Override
                            public void subscribe(final ObservableEmitter<ArrayList<RxDownloadPageBean>> e) throws Exception {
                                if (null != bean.getPages() && bean.getPages().size() > 0) {
                                    //之前获取过该章节的图片地址的情况
                                    e.onNext(bean.getPages());
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
                                            e.onNext(pages);
                                        }

                                        @Override
                                        public void loadFailed(String error) {
                                            e.onError(new Exception());
                                        }
                                    });
                                }
                            }
                        });
                    }
                })
                .flatMap(new Function<ArrayList<RxDownloadPageBean>, ObservableSource<RxDownloadPageBean>>() {
                    @Override
                    public ObservableSource<RxDownloadPageBean> apply(ArrayList<RxDownloadPageBean> beans) throws Exception {
                        return Observable.fromIterable(beans);
                    }
                })
                .subscribe(new Observer<RxDownloadPageBean>() {
                    @Override
                    public void onSubscribe(Disposable d) {
                        mDisposable = d;
                    }

                    @Override
                    public void onNext(RxDownloadPageBean value) {
                        Bitmap bp = downloadUrlBitmap(value.getPageUrl());
                        //把图片保存到本地
                        FileSpider.getInstance().saveBitmap(bp, value.getPageName(), value.getChapterName(), downloadBean.getMangaName());
                        for (int i = 0; i < chapters.size(); i++) {
                            if (value.getChapterName().equals(chapters.get(i).getChapterName())) {
                                chapters.get(i).getPages().remove(value);
                                EventBus.getDefault().post(new RxDownloadEvent(EventBusEvent.DOWNLOAD_PAGE_FINISH_EVENT, downloadBean, i));
                                if (chapters.get(i).getPages().size() <= 0) {
                                    EventBus.getDefault().post(new RxDownloadEvent(EventBusEvent.DOWNLOAD_CHAPTER_FINISH_EVENT, downloadBean, i));
                                }
                                return;
                            }
                        }
                    }

                    @Override
                    public void onError(Throwable e) {

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
        mDisposable.dispose();
        DownloadCaretaker.saveDownloadMemoto(this, downloadBean);
    }
}
