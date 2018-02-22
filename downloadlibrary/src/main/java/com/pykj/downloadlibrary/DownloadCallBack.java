package com.pykj.downloadlibrary;

/**
 * Created by L-CE on 2017/6/23.
 */

public interface DownloadCallBack {
    void success(DownloadInfo downloadInfo);

    void fail(Exception e);

    void progress(DownloadInfo downloadInfo);

}
