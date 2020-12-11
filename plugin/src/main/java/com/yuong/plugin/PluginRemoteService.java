package com.yuong.plugin;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;

import androidx.annotation.Nullable;

/**
 * @author : zhiwen.yang
 * date   : 2020/12/7
 * desc   :
 */
public class PluginRemoteService extends Service {
    private static final String TAG = "PluginRemoteService";
    private MyBinder myBinder = null;

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        Log.d(TAG, "PluginRemoteService onBind....");
        if (myBinder == null) {
            myBinder = new MyBinder();
        }
        return myBinder;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "PluginRemoteService onCreate....");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "PluginRemoteService onStartCommand....");
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "PluginRemoteService onDestroy....");
    }

    @Override
    public void onRebind(Intent intent) {
        super.onRebind(intent);
        Log.d(TAG, "PluginRemoteService onRebind....");
    }

    @Override
    public boolean onUnbind(Intent intent) {
        Log.d(TAG, "PluginRemoteService onUnbind....");
        return super.onUnbind(intent);
    }

    static class MyBinder extends IBookInterface.Stub {

        @Override
        public void setBookName(String name) throws RemoteException {

        }

        @Override
        public String getBookName() throws RemoteException {
            return "yuongzw";
        }
    }
}
