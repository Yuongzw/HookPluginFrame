package com.yuong.hook.frame.proxy;

import android.content.Intent;
import android.os.Environment;
import android.os.IBinder;
import android.os.Process;
import android.util.Log;
import androidx.annotation.Nullable;
import com.yuong.hook.frame.manager.PluginManager;

import java.io.File;

/**
 * @author : zhiwen.yang
 * date   : 2020/12/7
 * desc   :
 */
public class ProxyRemoteService extends ProxyLocalService {
    private String path = Environment.getExternalStorageDirectory().getAbsolutePath() + File.separator + "plugin-debug.apk";

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        Log.d(TAG, "ProxyRemoteService onBind...");
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "ProxyRemoteService onCreate...");
        PluginManager.getInstance(this.getApplicationContext()).pluginToApp(null, path);
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "ProxyRemoteService onStartCommand...");
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "ProxyRemoteService onDestroy...");
        super.onDestroy();
        Process.killProcess(android.os.Process.myPid());
    }
}
