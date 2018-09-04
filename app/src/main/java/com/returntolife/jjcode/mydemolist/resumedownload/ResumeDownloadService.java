package com.returntolife.jjcode.mydemolist.resumedownload;

import android.app.Service;
import android.content.Intent;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.text.TextUtils;
import android.util.Log;

import com.tools.jj.tools.utils.LogUtil;

import java.io.File;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Created by 455 on 2017/8/7.
 */

public class ResumeDownloadService extends Service {

    public   final  static String ACTION_START="ACTION_START";
    public   final  static String ACTION_STOP="ACTION_STOP";
    public   final  static String ACTION_UPDATE="ACTION_UPDATE";
    public static final String ACTION_FINISHED = "ACTION_FINISHED";

    public static final String INTENT_DATA_FILEINFO="intent_data_fileinfo";

    private  final int MSG_FILEINFO=1;

    private DownloadTask task;

    public static final String DownloadPath = Environment.getExternalStorageDirectory()
            + "/mydemolist/";

    private Map<Integer, DownloadTask> mTasks = new LinkedHashMap<Integer, DownloadTask>();

    Handler handler=new Handler(new Handler.Callback() {
        @Override
        public boolean handleMessage(Message msg) {

            switch (msg.what){
                case MSG_FILEINFO:
                    FileInfo fileInfo=(FileInfo) msg.obj;
                    LogUtil.d(fileInfo.toString()+"\n"+"文件初始化成功");

                    DownloadTask task = new DownloadTask(ResumeDownloadService.this, fileInfo);
                    task.download();
                    // 把下载任务添加到集合中
                    mTasks.put(fileInfo.getId(), task);
                    // 发送启动下载的通知
//                    Intent intent = new Intent(ACTION_START);
//                    intent.putExtra("fileInfo", fileInfo);
//                    sendBroadcast(intent);
                    break;
            }
            return false;
        }
    });



    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        FileInfo fileInfo=(FileInfo) intent.getSerializableExtra(INTENT_DATA_FILEINFO);
        String action=intent.getAction();
        if(!TextUtils.isEmpty(action)){
            switch (action){
                case ACTION_START:
                    DownloadTask.sExecutorService.execute( new Thread(new FileInfoRunnable(fileInfo)));
                    break;
                case ACTION_STOP:
                    DownloadTask task = mTasks.get(fileInfo.getId());
                    if (task != null) {
                        // 停止下载任务
                        task.isPause = true;
                    }
                    break;
                case ACTION_UPDATE:
                    break;
            }
        }




        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }



    private  class  FileInfoRunnable implements Runnable {

        private FileInfo fileInfo;

        FileInfoRunnable(FileInfo fileInfo){
            this.fileInfo=fileInfo;
        }

        @Override
        public void run() {
            try {
                URL url=new URL(fileInfo.getUrl());

                HttpURLConnection conn=(HttpURLConnection)url.openConnection();
                conn.setRequestMethod("GET");
                conn.setReadTimeout(3000);
                conn.connect();

                int length=-1;

                if (conn.getResponseCode()== HttpURLConnection.HTTP_OK){

                  length=conn.getContentLength();

                }

                if(length<=0){
                    return;
                }
                LogUtil.d("fileinfo length="+length);

                File dir=new File(DownloadPath);
                if(!dir.exists()){
                    dir.mkdir();
                }

                LogUtil.d("fileinfo DownloadPath="+DownloadPath);
                fileInfo.setLength(length);

                Message msg= Message.obtain();
                msg.obj=fileInfo;
                msg.what=MSG_FILEINFO;
                handler.sendMessage(msg);



            } catch (MalformedURLException e) {
                LogUtil.e("URL输入不正确 e="+e.toString());
            } catch (IOException e) {
                LogUtil.e("建立http连接对象失败 e="+e.toString());
            }

        }
    }



}
