package com.truthower.suhang.mangareader.widget.dialog;/**
 * Created by Administrator on 2016/11/4.
 */

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.view.Display;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;

import com.truthower.suhang.mangareader.R;
import com.truthower.suhang.mangareader.utils.DisplayUtil;
import com.truthower.suhang.mangareader.widget.dragview.DragView;
import com.truthower.suhang.mangareader.widget.shotview.ScreenShot;
import com.truthower.suhang.mangareader.widget.shotview.ShotView;
import com.truthower.suhang.mangareader.widget.toast.EasyToast;


/**
 * 作者：苏航 on 2016/11/4 11:08
 * 邮箱：772192594@qq.com
 */
public class TailorImgDialog extends MangaImgDialog implements View.OnClickListener {
    private ShotView shotView;
    private DragView screenshootDv;
    private EasyToast mEasyToast;
    private String word;

    public TailorImgDialog(Context context) {
        super(context);
        mEasyToast = new EasyToast(context);
    }

    @Override
    protected int getLayoutId() {
        return R.layout.dialog_image_tailor;
    }

    @Override
    protected void init() {
        super.init();
        shotView = (ShotView) findViewById(R.id.shot_view);
        shotView.setL(new ShotView.FinishShotListener() {
            @Override
            public void finishShot(Bitmap bp) {
                showTailoredDialog(bp);
            }
        });
        shotView.setIsRunning(true);
        screenshootDv = (DragView) findViewById(R.id.screenshoot_dv);
        screenshootDv.setScreenWidth((int) (DisplayUtil.getScreenWidth(context) * 0.9));
        screenshootDv.setOnClickListener(this);
    }

    private void showTailoredDialog(Bitmap bp) {
        MangaImgConfirmDialog imgDialog = new MangaImgConfirmDialog(context);
        imgDialog.show();
        imgDialog.setImgRes(bp);
        imgDialog.setSaveName(word);
        dismiss();
    }

    @Override
    public void setImgRes(Bitmap bp) {
        super.setImgRes(bp);
        resizeDialog();
    }

    private void resizeDialog() {
        Window window = this.getWindow();
        WindowManager.LayoutParams lp = window.getAttributes();
        lp.height =(int)((Double.valueOf(lp.width )/ Double.valueOf(bpRescource.getWidth())) * bpRescource.getHeight());
        window.setAttributes(lp);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.screenshoot_dv:
                try {
                    mEasyToast.showToast("手指划过区域然后松手截屏");

                    Bitmap bgBitmap = shotView.getBitmap();
                    if (bgBitmap != null) {
                        bgBitmap.recycle();
                    }
                    bgBitmap = ScreenShot.takeScreenShot(this, imgIv.getWidth(), imgIv.getHeight());
                    shotView.setBitmap(bgBitmap);
                    shotView.setVisibility(View.VISIBLE);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                break;
        }
    }

    public void setWord(String word) {
        this.word = word;
    }
}
