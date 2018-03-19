package com.pykj.downloadlibrary;

import android.app.Activity;
import android.support.annotation.NonNull;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.blankj.utilcode.util.AppUtils;

/**
 * Created by L-CE on 2018/1/31.
 * 带进度显示的文件下载
 */

public class DownloadProxy {
    private MaterialDialog progressDialog;
    private boolean showProgress;

    public void downloadAPK(final Activity activity, String url, final String updateMessage,final String authority) {
        progressDialog = null;
        DownloadManager.getInstance(activity)
                .download(url, new DownloadCallBack() {
                    @Override
                    public void success(DownloadInfo downloadInfo) {
                        progressDialog.dismiss();
                        AppUtils.installApp(downloadInfo.getFilePath(), authority);
                    }

                    @Override
                    public void fail(Exception e) {
//                        Toast.makeText(activity, "下载失败", Toast.LENGTH_SHORT).show();
                        progressDialog.dismiss();
                    }

                    @Override
                    public void progress(DownloadInfo downloadInfo) {

                        if (progressDialog == null) {
                            initProgressDialog(downloadInfo, activity,updateMessage);
                        }

                        progressDialog.setMaxProgress((int) downloadInfo.getTotal());
                        progressDialog.setProgress((int) downloadInfo.getProgress());
                        progressDialog.show();

                    }
                });
    }

    private void initProgressDialog(final DownloadInfo downloadInfo, final Activity activity,String updateMessage) {
        progressDialog = new MaterialDialog.Builder(activity)
                .content(updateMessage)
                .progress(false, (int) downloadInfo.getTotal())
                .cancelable(false)
                .positiveText("取消")
                .onPositive(new MaterialDialog.SingleButtonCallback() {
                    @Override
                    public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                        DownloadManager.getInstance(activity).cancel(downloadInfo.getUrl());
                        progressDialog.dismiss();
                    }
                })
                .build();
    }
}
