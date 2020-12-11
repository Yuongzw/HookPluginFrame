package com.yuong.hook.frame.proxy;

import android.app.Application;
import android.app.IServiceConnection;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.IBinder;
import android.util.ArrayMap;
import android.util.Log;
import androidx.annotation.Nullable;
import com.yuong.hook.frame.manager.HookManager;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * @author : zhiwen.yang
 * date   : 2020/12/7
 * desc   :
 */
public class ProxyLocalService extends Service {
    protected static String TAG;
    public static final int ACTION_START_SERVICE = 1;
    public static final int ACTION_BIND_SERVICE = 2;
    public static final int ACTION_STOP_SERVICE = 3;
    public static final int ACTION_UNBIND_SERVICE = 4;

    private static ArrayMap<String, Service> mStartServices = new ArrayMap<>();

    private static ArrayMap<String, Service> mBindServices = new ArrayMap<>();

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        TAG = this.getClass().getSimpleName();
        Log.d(TAG, "ProxyLocalService onCreate...");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "ProxyRemoteService onStartCommand...");
        //获取intent中插件servcie的className,构建插件servcie实例，并调用插件servcie的onCreate方法
        int actionMode = intent.getIntExtra("actionMode", 0);
        Intent target = intent.getParcelableExtra("pluginIntent");
        if (null == target && actionMode <= 0) {
            return START_STICKY;
        }
        Log.d(TAG, "actionMode=" + actionMode);
        try {
            Service service = null;
            String className = null;
            switch (actionMode) {
                case ACTION_START_SERVICE:
                    className = target.getComponent().getClassName();
                    Log.d(TAG, className + "  startService");
                    service = getService(className, false, 1);
                    if (service == null) {
                        service = createServiceAndAttach(className, actionMode);
                    } else {
                        if (!mStartServices.containsValue(service)) {
                            mStartServices.put(className, service);
                        }
                    }
                    service.onStartCommand(target, flags, startId);
                    break;
                case ACTION_BIND_SERVICE:
                    className = target.getComponent().getClassName();
                    Log.d(TAG, className + "  bindService");
                    service = getService(className, true, 1);
                    if (service == null) {
                        service = createServiceAndAttach(className, actionMode);
                        IBinder iBinder = service.onBind(target);
                        Bundle extras = intent.getExtras();
                        if (extras != null) {
                            IBinder serviceConnection = extras.getBinder("connected");
                            IServiceConnection iServiceConnection = IServiceConnection.Stub.asInterface(serviceConnection);
                            iServiceConnection.connected(target.getComponent(), iBinder, false);
                            Log.d(TAG, serviceConnection + "");
                        }
                    } else {
                        if (!mBindServices.containsValue(service)) {
                            mBindServices.put(className, service);
                            IBinder iBinder = service.onBind(target);
                            Bundle extras = intent.getExtras();
                            if (extras != null) {
                                IBinder serviceConnection = extras.getBinder("connected");
                                IServiceConnection iServiceConnection = IServiceConnection.Stub.asInterface(serviceConnection);
                                iServiceConnection.connected(target.getComponent(), iBinder, false);
                                Log.d(TAG, serviceConnection + "");
                            }
                        }
                    }
                    break;
                case ACTION_STOP_SERVICE:
                    stopPlugService(target);
                    break;
                case ACTION_UNBIND_SERVICE:
                    unbindPlugService(target);
                    break;
                default:
                    break;
            }
        } catch (Exception e) {
            Log.e(TAG, e + "");
        }
        return super.onStartCommand(intent, flags, startId);
    }

    private Service getService(String className, boolean isBind, int count) {
        Service service = null;
        if (!isBind) {
            service = mStartServices.get(className);
        }else {
            service = mBindServices.get(className);
        }
        count++;
        if (count <= 2 && service == null) {
            service = getService(className, !isBind, count);
        }
        return service;
    }

    private Service createServiceAndAttach(String className, int actionMode) throws Exception {
        Service service = (Service) Class.forName(className).newInstance();
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
        if (actionMode == 1) {
            mStartServices.put(className, service);
        } else if (actionMode == 2) {
            mBindServices.put(className, service);
        }
        return service;
    }

    private void stopPlugService(Intent intent) {
        if (null == intent) {
            return;
        }
        if (null == intent.getComponent()) {
            return;
        }
        String className = intent.getComponent().getClassName();
        Log.i(TAG, className);
        Service service = mStartServices.remove(className);
        Log.i(TAG, service + "");
        if (null == service) {
            return;
        }
        if (!mBindServices.containsKey(className)) {
            service.onDestroy();
        }
        if (mStartServices.isEmpty() && mBindServices.isEmpty()) {
            stopSelf();
        }
    }

    private void unbindPlugService(Intent intent) {
        if (null == intent) {
            return;
        }
        if (null == intent.getComponent()) {
            return;
        }
        String className = intent.getComponent().getClassName();
        Log.i(TAG, className);
        Service service = mBindServices.remove(className);
        Log.i(TAG, service + "");
        if (null == service) {
            return;
        }
        service.onUnbind(intent);
        if (!mStartServices.containsKey(className)) {
            service.onDestroy();
        }
        if (mStartServices.isEmpty() && mBindServices.isEmpty()) {
            stopSelf();
        }
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "ProxyLocalService onDestroy...");
        super.onDestroy();
    }
}
