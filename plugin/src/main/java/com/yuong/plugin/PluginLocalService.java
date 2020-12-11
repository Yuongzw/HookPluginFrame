package com.yuong.plugin;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;
import androidx.annotation.Nullable;

/**
 * @author : zhiwen.yang
 * date   : 2020/12/7
 * desc   :
 */
public class PluginLocalService extends Service {
    private static final String TAG = "PluginLocalService";

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "PluginLocalService onCreate...");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "PluginLocalService onStartCommand...");
        Intent intent1 = new Intent("yuongzw");
        sendBroadcast(intent1);
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Log.d(TAG, "延迟3s 结束Service");
                    Thread.sleep(3000);
                    Log.d(TAG, "结束Service");
                    stopSelf();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }).start();
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "PluginLocalService onDestroy...");
    }
}
