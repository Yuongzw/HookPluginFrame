package com.yuong.hook.frame.manager;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.res.AssetManager;
import android.content.res.Resources;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;

import androidx.annotation.NonNull;

import com.yuong.hook.frame.Constans;
import com.yuong.hook.frame.proxy.ProxyActivity;
import com.yuong.hook.frame.proxy.ProxyLocalService;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.List;

/**
 * @author :
 * date   : 2020/6/1
 * desc   :
 */
public class HookManager {
    private Context context;
    private static HookManager instance;

    private HookManager(Context context) {
        this.context = context;
    }

    public static HookManager getInstance(Context context) {
        if (instance == null) {
            synchronized (HookManager.class) {
                if (instance == null) {
                    instance = new HookManager(context);
                }
            }
        }
        return instance;
    }


    public void hookAMSAction() throws Exception {
        //动态代理
        Class<?> mIActivityManagerClass;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            mIActivityManagerClass = Class.forName("android.app.IActivityTaskManager");
        } else {
            mIActivityManagerClass = Class.forName("android.app.IActivityManager");
        }
        //获取 ActivityManager 或 ActivityManagerNative 或 ActivityTaskManager
        Class<?> mActivityManagerClass;
        Method getActivityManagerMethod;
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.N_MR1) {
            mActivityManagerClass = Class.forName("android.app.ActivityManagerNative");
            getActivityManagerMethod = mActivityManagerClass.getDeclaredMethod("getDefault");
        } else if (Build.VERSION.SDK_INT > Build.VERSION_CODES.N_MR1 && Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            mActivityManagerClass = Class.forName("android.app.ActivityManager");
            getActivityManagerMethod = mActivityManagerClass.getDeclaredMethod("getService");
        } else {
            mActivityManagerClass = Class.forName("android.app.ActivityTaskManager");
            getActivityManagerMethod = mActivityManagerClass.getDeclaredMethod("getService");
        }
        getActivityManagerMethod.setAccessible(true);
        //这个实例本质是 IActivityManager或者IActivityTaskManager
        final Object IActivityManager = getActivityManagerMethod.invoke(null);

        //创建动态代理
        Object mActivityManagerProxy = Proxy.newProxyInstance(
                context.getClassLoader(),
                new Class[]{mIActivityManagerClass},//要监听的回调接口
                new InvocationHandler() {
                    @Override
                    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {

                        if ("startActivity".equals(method.getName())) {
                            //做自己的业务逻辑
                            //换成可以通过AMS检测的Activity
                            Intent intent = new Intent(context, ProxyActivity.class);
                            intent.putExtra("actonIntent", (Intent) args[3]);
                            args[3] = intent;
                        }
                        //让程序继续能够执行下去
                        return method.invoke(IActivityManager, args);
                    }
                }
        );

        //获取 IActivityTaskManagerSingleton 或者 IActivityManagerSingleton 或者 gDefault 属性
        Field mSingletonField;
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.N_MR1) {
            mSingletonField = mActivityManagerClass.getDeclaredField("gDefault");
        } else if (Build.VERSION.SDK_INT > Build.VERSION_CODES.N_MR1 && Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            mSingletonField = mActivityManagerClass.getDeclaredField("IActivityManagerSingleton");
        } else {
            mSingletonField = mActivityManagerClass.getDeclaredField("IActivityTaskManagerSingleton");
        }
        mSingletonField.setAccessible(true);
        Object mSingleton = mSingletonField.get(null);

        //替换点
        Class<?> mSingletonClass = Class.forName(Constans.SINGLETON);
        Field mInstanceField = mSingletonClass.getDeclaredField("mInstance");
        mInstanceField.setAccessible(true);
        //将我们创建的动态代理设置到 mInstance 属性当中
        mInstanceField.set(mSingleton, mActivityManagerProxy);
        hookAMSAction2();
    }


    public void hookAMSAction2() throws Exception {
        //动态代理
        Class<?> mIActivityManagerClass;

        mIActivityManagerClass = Class.forName("android.app.IActivityManager");
        //获取 ActivityManager 或 ActivityManagerNative 或 ActivityTaskManager
        Class<?> mActivityManagerClass;
        Method getActivityManagerMethod;
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.N_MR1) {
            mActivityManagerClass = Class.forName("android.app.ActivityManagerNative");
            getActivityManagerMethod = mActivityManagerClass.getDeclaredMethod("getDefault");
        } else {
            mActivityManagerClass = Class.forName("android.app.ActivityManager");
            getActivityManagerMethod = mActivityManagerClass.getDeclaredMethod("getService");
        }
        getActivityManagerMethod.setAccessible(true);
        //这个实例本质是 IActivityManager或者IActivityTaskManager
        final Object IActivityManager = getActivityManagerMethod.invoke(null);

        //创建动态代理
        Object mActivityManagerProxy = Proxy.newProxyInstance(
                context.getClassLoader(),
                new Class[]{mIActivityManagerClass},//要监听的回调接口
                new InvocationHandler() {
                    @Override
                    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {

                        if ("startService".equals(method.getName())) {
                            //startService
                            int index = -1;
                            String packageName = "com.yuong.hook.frame";
                            String proxyServiceClassName = packageName + ".proxy.ProxyLocalService";
                            Intent proxyIntent = null;
                            for (int i = 0; i < args.length; i++) {
                                if (args[i] instanceof Intent) {
                                    index = i;
                                    break;
                                }
                            }

                            if (-1 != index) {
                                Intent plugIntent = (Intent) args[index];
                                if (!plugIntent.getBooleanExtra("isBind", false)) {
                                    if (null != plugIntent.getComponent()) {
                                        proxyIntent = new Intent();
                                        proxyIntent.setClassName(packageName, proxyServiceClassName);
                                        proxyIntent.putExtra("serviceIntent", plugIntent);
                                    }
                                    //这里添加一个判断，防止类名写错时，导致args中的intent这个参数是null,导致崩溃
                                    args[index] = proxyIntent == null ? plugIntent : proxyIntent;
                                }
                            }
                        } else if ("bindIsolatedService".equals(method.getName())) {

                            Bundle bundle = new Bundle();
                            bundle.putBinder("connected", (IBinder) args[4]);

                            // 找到参数里面的第一个Intent 对象
                            int index = -1;
                            for (int i = 0; i < args.length; i++) {
                                if (args[i] instanceof Intent) {
                                    index = i;
                                    break;
                                }
                            }

                            String packageName = "com.yuong.hook.frame";
                            String proxyServiceClassName = packageName + ".proxy.ProxyRemoteService";
                            Intent proxyIntent = null;

                            if (-1 != index) {
                                Intent plugIntent = (Intent) args[index];
                                if (null != plugIntent.getComponent()) {
                                    proxyIntent = new Intent();
                                    proxyIntent.setClassName(packageName, proxyServiceClassName);
                                    proxyIntent.putExtra("serviceIntent", plugIntent);
                                    proxyIntent.putExtra("isBind", true);
                                    proxyIntent.putExtras(bundle);
                                }
                                //这里添加一个判断，防止类名写错时，导致args中的intent这个参数是null,导致崩溃
//                                args[index] = proxyIntent == null ? plugIntent : proxyIntent;
                                return context.startService(proxyIntent);
                            }

                        }
                        else if ("stopService".equals(method.getName())) {
                            int index = -1;
                            for (int i = 0; i < args.length; i++) {
                                if (args[i] instanceof Intent) {
                                    index = i;
                                    break;
                                }
                            }
                            if (index != -1) {
                                Intent rawIntent = (Intent) args[index];
                                return ProxyLocalService.stopPlugService(rawIntent);
                            }
                        }
                        //让程序继续能够执行下去
                        return method.invoke(IActivityManager, args);
                    }
                }
        );

        //获取 IActivityTaskManagerSingleton 或者 IActivityManagerSingleton 或者 gDefault 属性
        Field mSingletonField;
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.N_MR1) {
            mSingletonField = mActivityManagerClass.getDeclaredField("gDefault");
        } else {
            mSingletonField = mActivityManagerClass.getDeclaredField("IActivityManagerSingleton");
        }
        mSingletonField.setAccessible(true);
        Object mSingleton = mSingletonField.get(null);

        //替换点
        Class<?> mSingletonClass = Class.forName(Constans.SINGLETON);
        Field mInstanceField = mSingletonClass.getDeclaredField("mInstance");
        mInstanceField.setAccessible(true);
        //将我们创建的动态代理设置到 mInstance 属性当中
        mInstanceField.set(mSingleton, mActivityManagerProxy);
    }


    public static Object getIActivityManager() throws Exception {
        Object iActivityManager = null;
        Method getActivityManagerMethod = null;
        if (Build.VERSION.SDK_INT >= 26) {
            Class<?> activityManagerClazz = Class.forName("android.app.ActivityManager");
            getActivityManagerMethod = activityManagerClazz.getDeclaredMethod("getService");
            getActivityManagerMethod.setAccessible(true);
        } else {
            Class<?> actitivytManagerNatvieClazz = Class.forName("android.app.ActivityManagerNative");
            getActivityManagerMethod = actitivytManagerNatvieClazz.getDeclaredMethod("getDefault");
            getActivityManagerMethod.setAccessible(true);
        }
        if (null != getActivityManagerMethod) {
            iActivityManager = getActivityManagerMethod.invoke(null);
            return iActivityManager;
        }
        return iActivityManager;
    }

    public void hookLaunchActivity() throws Exception {
        //获取 ActivityThread 类
        Class<?> mActivityThreadClass = Class.forName("android.app.ActivityThread");

        //获取 ActivityThread 的 currentActivityThread() 方法
        Method currentActivityThread = mActivityThreadClass.getDeclaredMethod("currentActivityThread");
        currentActivityThread.setAccessible(true);
        //获取 ActivityThread 实例
        Object mActivityThread = currentActivityThread.invoke(null);

        //获取 ActivityThread 的 mH 属性
        Field mHField = mActivityThreadClass.getDeclaredField("mH");
        mHField.setAccessible(true);
        Handler mH = (Handler) mHField.get(mActivityThread);

        //获取 Handler 的 mCallback 属性
        Field mCallbackField = Handler.class.getDeclaredField("mCallback");
        mCallbackField.setAccessible(true);
        //设置我们自定义的 CallBack
        mCallbackField.set(mH, new MyCallBack());
    }

    class MyCallBack implements Handler.Callback {

        @Override
        public boolean handleMessage(@NonNull Message msg) {
            if (msg.what == Constans.EXECUTE_TRANSACTION) {
                try {
                    Field mActivityCallbacksField = msg.obj.getClass().getDeclaredField("mActivityCallbacks");
                    mActivityCallbacksField.setAccessible(true);
                    List<Object> mActivityCallbacks = (List<Object>) mActivityCallbacksField.get(msg.obj);
                    if (mActivityCallbacks != null && mActivityCallbacks.size() > 0) {
                        Object mClientTransactionItem = mActivityCallbacks.get(0);
                        Class<?> mLaunchActivityItemClass = Class.forName("android.app.servertransaction.LaunchActivityItem");
                        if (mLaunchActivityItemClass.isInstance(mClientTransactionItem)) {
                            //获取 LaunchActivityItem 的 mIntent 属性
                            Field mIntentField = mClientTransactionItem.getClass().getDeclaredField("mIntent");
                            mIntentField.setAccessible(true);
                            Intent intent = (Intent) mIntentField.get(mClientTransactionItem);
                            //取出我们传递的值
                            Intent actonIntent = intent.getParcelableExtra("actonIntent");
                            if (actonIntent != null) {
                                //替换掉原来的intent属性的值
                                mIntentField.set(mClientTransactionItem, actonIntent);
                            }
                        }
                    }

                } catch (Exception e) {
                    e.printStackTrace();
                }

            } else if (msg.what == Constans.LAUNCH_ACTIVITY) {
                /*
                    7.0以下代码
                     case LAUNCH_ACTIVITY: {
                    Trace.traceBegin(Trace.TRACE_TAG_ACTIVITY_MANAGER, "activityStart");
                    final ActivityClientRecord r = (ActivityClientRecord) msg.obj;

                    r.packageInfo = getPackageInfoNoCheck(
                            r.activityInfo.applicationInfo, r.compatInfo);
                    handleLaunchActivity(r, null, "LAUNCH_ACTIVITY");
                    Trace.traceEnd(Trace.TRACE_TAG_ACTIVITY_MANAGER);
                } break;

                 */
                try {
                    //获取 ActivityClientRecord 的 intent 属性
                    Field intentField = msg.obj.getClass().getDeclaredField("intent");
                    intentField.setAccessible(true);
                    Intent intent = (Intent) intentField.get(msg.obj);
                    //取出我们传递的值
                    Intent actonIntent = intent.getParcelableExtra("actonIntent");
                    if (actonIntent != null) {
                        //替换掉原来的intent属性的值
                        intentField.set(msg.obj, actonIntent);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            return false;
        }
    }
}
