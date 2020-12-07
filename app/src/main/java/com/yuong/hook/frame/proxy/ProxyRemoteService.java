package com.yuong.hook.frame.proxy;

import android.app.Application;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.Nullable;

import com.yuong.hook.frame.manager.HookManager;
import com.yuong.hook.frame.manager.PluginManager;

import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.HashMap;

/**
 * @author : zhiwen.yang
 * date   : 2020/12/7
 * desc   :
 */
public class ProxyRemoteService extends Service {
    private String path = Environment.getExternalStorageDirectory().getAbsolutePath() + File.separator + "plugin-debug.apk";

    private static final String TAG = "ProxyRemoteService";
    private static HashMap<String, Service> mServices = new HashMap<>();

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
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "ProxyRemoteService onStartCommand...");
        //获取intent中插件servcie的className,构建插件servcie实例，并调用插件servcie的onCreate方法
        if (intent.hasExtra("serviceIntent")) {
            Intent parcelableExtra = intent.getParcelableExtra("serviceIntent");
            String className = parcelableExtra.getComponent().getClassName();
            Log.d(TAG, className);
            Service service = mServices.get(className);
            if (null == service) {
                try {
                    service = (Service) Class.forName(className).newInstance();
                    //目前创建的servcie实例是没有上下文的，需要调用其attach方法，才能让这个service拥有上下文环境
//                public final void attach(
//                        Context context,
//                        ActivityThread thread, String className, IBinder token,
//                        Application application, Object activityManager)

                    Class<?> activityThreadClazz = Class.forName("android.app.ActivityThread");
                    Field sCurrentActivityThreadFiled = activityThreadClazz.getDeclaredField("sCurrentActivityThread");
                    sCurrentActivityThreadFiled.setAccessible(true);
                    Object sCurrentActivityThread = sCurrentActivityThreadFiled.get(null);

                    Field mAppThreadFiled = activityThreadClazz.getDeclaredField("mAppThread");
                    mAppThreadFiled.setAccessible(true);
                    Object mAppThread = mAppThreadFiled.get(sCurrentActivityThread);
                    Class<?> iInterfaceClazz = Class.forName("android.os.IInterface");
                    Method asBinderMethod = iInterfaceClazz.getDeclaredMethod("asBinder");
                    asBinderMethod.setAccessible(true);
                    IBinder token = (IBinder) asBinderMethod.invoke(mAppThread);
                    Object iActivityManager = HookManager.getIActivityManager();
                    Method attachMethod = Service.class.getDeclaredMethod("attach", Context.class, sCurrentActivityThread.getClass(),
                            String.class, IBinder.class, Application.class, Object.class);
                    attachMethod.setAccessible(true);
                    attachMethod.invoke(service, this, sCurrentActivityThread, className, token, getApplication(), iActivityManager);
                    service.onCreate();
                    mServices.put(className, service);
//                    service.onStartCommand(intent, flags, startId);
                    IBinder iBinder = service.onBind(parcelableExtra);
                    Bundle extras = intent.getExtras();
                    if (extras != null) {
                        IBinder serviceConnection = extras.getBinder("connected");
                        Log.d(TAG, serviceConnection + "");
                    }

                } catch (Exception e) {
                    e.printStackTrace();
                    Log.e(TAG, e.toString());
                }
            } else {
//                service.onStartCommand(intent, flags, startId);
            }

        }
        return super.onStartCommand(intent, flags, startId);
    }

    public static int stopPlugService(Intent intent) {
        if (null == intent) {
            return 0;
        }
        if (null == intent.getComponent()) {
            return 0;
        }
        String className = intent.getComponent().getClassName();
        Log.i(TAG, className);
        Service service = mServices.get(className);
        Log.i(TAG, service + "");
        if (null == service) {
            return 0;
        }
        service.onDestroy();
        mServices.remove(className);
        return 1;
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "ProxyLocalService onDestroy...");
        super.onDestroy();
    }
}
