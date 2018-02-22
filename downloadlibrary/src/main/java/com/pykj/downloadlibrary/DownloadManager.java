package com.pykj.downloadlibrary;

import android.content.Context;
import android.os.Handler;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;

import okhttp3.Call;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;


/**
 * 1.只要服务器支持断点下载,这个就支持.如果服务器不支持,则重新下载完整文件.
 * 2.默认以最后一个/后的字符串为下载文件名；比如"http://192.168.31.169:8080/out/code.apk"，则下载的文件名字默认为code.apk;
 * 3.
 */
public class DownloadManager {

    private HashMap<String, Call> downCalls = new HashMap<>();//用来存放各个下载的请求
    private OkHttpClient mClient;
    private static DownloadManager downloadManager;
    public static String savePath;

    private HashMap<String, DownloadCallBack> mCallBacks = new HashMap<>();//用来存放各个下载的回调
    private static Handler mHandler;
    private String TAG = "DownloadManager";

    private DownloadManager() {
        this.mClient = new OkHttpClient.Builder().build();
    }

    public static DownloadManager getInstance(Context context) {
        if (downloadManager == null) {
            downloadManager = new DownloadManager();
            mHandler = new Handler();
            savePath = context.getExternalFilesDir("apk").getAbsolutePath();
//            savePath = Environment.getExternalStorageDirectory().getAbsolutePath();
        }

        if (savePath == null) {
            throw new NullPointerException("savePath = null");
        }
        return downloadManager;
    }


    public void download(final String url, DownloadCallBack callBack) {
        if (callBack == null) throw new NullPointerException(" DownloadCallBack == null ");

        mCallBacks.put(url, callBack);
        //1.判断是否正在下载，是则无视这次请求
        if (downCalls.containsKey(url)) return;

        new Thread(new Runnable() {
            @Override
            public void run() {
                //2.创建下载信息实体类,获取文件长度
                final DownloadInfo downInfo = createDownInfo(url);
                Log.d(TAG, "remote field size:" + downInfo.getTotal());
                //3.检测本地文件夹,生成新的文件名
                getRealFileName(downInfo);

                //4.开始下载
                long downloadLength = downInfo.getProgress();//已经下载好的长度
                long contentLength = downInfo.getTotal();//文件的总长度

                //4.1初始化下载信息到Ui
                mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        mCallBacks.get(url).progress(downInfo);
                    }
                });

                Request request = new Request.Builder()
                        //确定下载的范围,添加此头,则服务器就可以跳过已经下载好的部分
                        .addHeader("RANGE", "bytes=" + downloadLength + "-" + contentLength)
                        .url(url)
                        .build();

                Call call = mClient.newCall(request);
                downCalls.put(url, call);//把这个添加到call里,方便取消

                InputStream is = null;
                FileOutputStream fileOutputStream = null;

                try {
                    Response response = call.execute();
                    File file = new File(savePath, downInfo.getFileName());//文件保存全路径
                    downInfo.setFilePath(file.getAbsolutePath());
                    Log.d(TAG, "file final save path" + file.getAbsolutePath());
                    is = response.body().byteStream();
                    fileOutputStream = new FileOutputStream(file, true);
                    byte[] buffer = new byte[2048];//缓冲数组2kB
                    int len;
                    while ((len = is.read(buffer)) != -1) {
                        fileOutputStream.write(buffer, 0, len);
                        downloadLength += len;
                        downInfo.setProgress(downloadLength);

                        mHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                mCallBacks.get(url).progress(downInfo);
                            }
                        });
                    }

                    mHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            mCallBacks.get(url).success(downInfo);
                            Log.e(TAG, "文件下载成功");
                        }
                    });


                    fileOutputStream.flush();

                    downCalls.remove(url);

                } catch (Exception e) {
                    e.printStackTrace();
                    mCallBacks.get(url).fail(e);
                } finally {
                    try {
                        is.close();
                        fileOutputStream.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                        mCallBacks.get(url).fail(e);
                    }
                }

            }//run end
        }).start();
    }

    /**
     * 创建DownInfo
     *
     * @param url 请求网址
     * @return DownInfo
     */
    private DownloadInfo createDownInfo(String url) {

        DownloadInfo downloadInfo = new DownloadInfo(url);

        long contentLength = getContentLength(url);//获得文件大小

        downloadInfo.setTotal(contentLength);

        String fileName = url.substring(url.lastIndexOf("/"));
        Log.d(TAG, "field initial name:" + fileName);
        downloadInfo.setFileName(fileName);

        return downloadInfo;
    }

    private DownloadInfo getRealFileName(DownloadInfo downloadInfo) {

        String fileName = downloadInfo.getFileName();

        long downloadLength = 0, contentLength = downloadInfo.getTotal();

        File file = new File(savePath, fileName);

        //文件已存在，且文件不支持断点下载。
        if (file.exists() && contentLength == DownloadInfo.TOTAL_ERROR) {
            Log.d(TAG, "文件已存在且不支持断点下载");
            boolean deleteFileFlag = file.delete();
            if (deleteFileFlag) {
                Log.e(TAG, "文件删除成功");
            } else {
                Log.e(TAG, "文件删除失败");
            }
        }

        if (file.exists() && contentLength != DownloadInfo.TOTAL_ERROR) {
            //找到了文件,代表已经下载过,则获取其长度
            Log.e(TAG, "文件已存在，且支持断点续传，当前断点：" + file.length());
            downloadLength = file.length();
        }

        //之前下载过,需要重新来一个文件
        int i = 1;
        while (contentLength != DownloadInfo.TOTAL_ERROR && downloadLength >= contentLength) {
            int dotIndex = fileName.lastIndexOf(".");
            String fileNameOther;
            if (dotIndex == -1) {
                fileNameOther = fileName + "(" + i + ")";
            } else {
                fileNameOther = fileName.substring(0, dotIndex)
                        + "(" + i + ")" + fileName.substring(dotIndex);
            }
            File newFile = new File(savePath, fileNameOther);
            file = newFile;
            downloadLength = newFile.length();
            i++;
        }
        //设置改变过的文件名/大小
        downloadInfo.setProgress(downloadLength);
        downloadInfo.setFileName(file.getName());
        Log.d(TAG, "file final name:" + file.getName());
        return downloadInfo;
    }

    /**
     * 获取下载长度
     * 同步方法
     *
     * @param downloadUrl
     * @return
     */
    private long getContentLength(String downloadUrl) {
        Request request = new Request.Builder()
                .url(downloadUrl)
                .build();
        try {
            Response response = mClient.newCall(request).execute();
            if (response != null && response.isSuccessful()) {
                long contentLength = response.body().contentLength();
                response.close();
                return contentLength == 0 ? DownloadInfo.TOTAL_ERROR : contentLength;
            }
        } catch (IOException e) {
            e.printStackTrace();
            mCallBacks.get(downloadUrl).fail(e);
        }
        return DownloadInfo.TOTAL_ERROR;
    }

    public void cancel(String url) {
        Call call = downCalls.get(url);
        if (call != null) {
            call.cancel();
        }
        downCalls.remove(url);
    }

}
