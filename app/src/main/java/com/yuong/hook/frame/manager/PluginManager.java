package com.yuong.hook.frame.manager;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.pm.ServiceInfo;
import android.content.res.AssetManager;
import android.content.res.Resources;
import android.os.Handler;
import android.util.ArrayMap;
import android.util.Log;

import java.io.File;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import dalvik.system.DexClassLoader;
import dalvik.system.PathClassLoader;

/**
 * @author :
 * date   : 2020/6/1
 * desc   :
 */
public class PluginManager {
    private static final String TAG = PluginManager.class.getSimpleName();
    private static PluginManager instance;
    private Context context;
    private DexClassLoader dexClassLoader;
    private AssetManager assetManager;
    private Resources pluginResource;
    private ArrayMap<String, List<ServiceInfo>> serviceInfoMap = new ArrayMap<>();

    private PluginManager(Context context) {
        this.context = context;
    }

    public static PluginManager getInstance(Context context) {
        if (instance == null) {
            synchronized (PluginManager.class) {
                if (instance == null) {
                    instance = new PluginManager(context);
                }
            }
        }
        return instance;
    }

    public void pluginToApp(final Handler handler, final String path) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    //第一步：找到宿主的 dexElements 对象
                    //获取 BaseDexClassLoader 类
                    Class<?> mainBaseDexClassLoaderClass = Class.forName("dalvik.system.BaseDexClassLoader");
                    //获取 pathList 属性
                    Field mainPathListField = mainBaseDexClassLoaderClass.getDeclaredField("pathList");
                    mainPathListField.setAccessible(true);
                    //获取 PathClassLoader
                    PathClassLoader mainClassLoader = (PathClassLoader) context.getClassLoader();
                    //获取 pathList 属性值
                    Object mainPathList = mainPathListField.get(mainClassLoader);
                    //获取 dexElements
                    Field mainDexElementsField = mainPathList.getClass().getDeclaredField("dexElements");
                    mainDexElementsField.setAccessible(true);
                    Object mainDexElements = mainDexElementsField.get(mainPathList);


                    //第二步：找到插件的dexElements 对象
                    //获取 BaseDexClassLoader 类
                    Class<?> pluginBaseDexClassLoaderClass = Class.forName("dalvik.system.BaseDexClassLoader");
                    //获取 pathList 属性
                    Field pluginPathListField = pluginBaseDexClassLoaderClass.getDeclaredField("pathList");
                    pluginPathListField.setAccessible(true);
                    //获取 DexClassLoader
                    File file = new File(path);
                    if (!file.exists()) {
                        Log.e(TAG, "插件不存在");
                        return;
                    }
                    File pluginDir = context.getDir("plugin", Context.MODE_PRIVATE);
                    //加载插件的class
                    dexClassLoader = new DexClassLoader(path, pluginDir.getAbsolutePath(), null, context.getClassLoader());
                    //获取 pathList 属性值
                    Object pluginPathList = pluginPathListField.get(dexClassLoader);
                    //获取 dexElements
                    Field pluginDexElementsField = pluginPathList.getClass().getDeclaredField("dexElements");
                    pluginDexElementsField.setAccessible(true);
                    Object pluginDexElements = pluginDexElementsField.get(pluginPathList);

                    //第三步：创建出新的 dexElements 对象
                    //获取宿主 dexElements 跟插件 dexElements 的长度
                    int mainLength = Array.getLength(mainDexElements);
                    int pluginLength = Array.getLength(pluginDexElements);
                    int newLength = mainLength + pluginLength;
                    //创建新的数组
                    Object newDexElements = Array.newInstance(mainDexElements.getClass().getComponentType(), newLength);

                    //第四步：新的 dexElements = 宿主的dexElements + 插件的dexElements
                    for (int i = 0; i < newLength; i++) {
                        if (i < mainLength) {
                            Array.set(newDexElements, i, Array.get(mainDexElements, i));
                        } else {
                            Array.set(newDexElements, i, Array.get(pluginDexElements, i - mainLength));
                        }
                    }

                    //第五步：新的 dexElements设置到宿主当中去
                    mainDexElementsField.set(mainPathList, newDexElements);

                    //加载插件的资源
                    loadPluginResource(handler, path);
                    //解析插件
                    parsePlugin(path);


                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    public void loadPluginResource(final Handler handler, final String path) {
        try {
            //加载插件的资源文件
            //1、获取插件的AssetManager
            assetManager = AssetManager.class.newInstance();
            Method addAssetPath = AssetManager.class.getMethod("addAssetPath", String.class);
            addAssetPath.setAccessible(true);
            addAssetPath.invoke(assetManager, path);
            //2、获取宿主的Resources
            Resources appResources = context.getResources();
            //实例化插件的Resources
            pluginResource = new Resources(assetManager, appResources.getDisplayMetrics(), appResources.getConfiguration());
            if (dexClassLoader != null && pluginResource != null && handler != null) {
                handler.sendEmptyMessage(666);
            } else {
                if (handler != null) {
                    handler.sendEmptyMessage(0);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            if (handler != null) {
                handler.sendEmptyMessage(0);
            }
        }
    }

    @SuppressLint("PrivateApi")
    public void parsePlugin(String pluginPath) {
        try {
            File file = new File(pluginPath);
            if (!file.exists()) {
                Log.e(TAG, "插件不存在");
                return;
            }
            //1、解析插件包  public Package parsePackage(File packageFile, int flags)
            Class<?> mPackageParserClass = Class.forName("android.content.pm.PackageParser");
            Object mPackageParser = mPackageParserClass.newInstance();
            Method parsePackageMethod = mPackageParserClass.getMethod("parsePackage", File.class, int.class);
            Object mPackage = parsePackageMethod.invoke(mPackageParser, file, PackageManager.GET_SERVICES);

            //2、获取Package类下的   public final ArrayList<Activity> receivers = new ArrayList<Activity>(0); 广播集合
            Field mReceiversField = mPackage.getClass().getDeclaredField("services");
            ArrayList<Object> services = (ArrayList<Object>) mReceiversField.get(mPackage);

            //3、遍历所有的Service
            //Activity 该Activity 不是四大组件里面的activity，而是一个Java bean对象，用来封装清单文件中的activity和receiver
            for (Object mService : services) {
                Field infoFiled = mService.getClass().getDeclaredField("info");
                infoFiled.setAccessible(true);
                ServiceInfo serviceInfo = (ServiceInfo) infoFiled.get(mService);
                List<ServiceInfo> serviceInfoList = serviceInfoMap.get(serviceInfo.packageName);
                if (serviceInfoList == null) {
                    serviceInfoList = new ArrayList<>();
                    serviceInfoList.add(serviceInfo);
                    serviceInfoMap.put(serviceInfo.packageName, serviceInfoList);
                }
                Log.d("yuongzw", serviceInfo.processName + ", " + serviceInfo.packageName);

                //4、获取该广播的全类名 即 <service android:name=".PluginLocalService"/> android:name属性后面的值
                //  /**
                //     * Public name of this item. From the "android:name" attribute.
                //     */
                //    public String name;

                // public static final ActivityInfo generateActivityInfo(Activity a, int flags,
                //            PackageUserState state, int userId)

//                public static final ServiceInfo generateServiceInfo(Service s, int flags,
//                PackageUserState state, int userId)
                //先获取到 ServiceInfo 类
//                Class<?> mPackageUserStateClass = Class.forName("android.content.pm.PackageUserState");
//                Object mPackageUserState = mPackageUserStateClass.newInstance();
//
//                Method generateActivityInfoMethod = mPackageParserClass.getMethod("generateActivityInfo", mActivity.getClass(),
//                        int.class, mPackageUserStateClass, int.class);
//                //获取userId
//                Class<?> mUserHandleClass = Class.forName("android.os.UserHandle");
//                //public static @UserIdInt int getCallingUserId()
//                int userId = (int) mUserHandleClass.getMethod("getCallingUserId").invoke(null);
//
//                //执行此方法 由于是静态方法 所以不用传对象
//                ServiceInfo serviceInfo = (ServiceInfo) generateActivityInfoMethod.invoke(null, mActivity, 0, mPackageUserState, userId);
//                String receiverClassName = activityInfo.name;
//                Class<?> receiverClass = getClassLoader().loadClass(receiverClassName);
//                BroadcastReceiver receiver = (BroadcastReceiver) receiverClass.newInstance();
//
//                //5、获取 intent-filter  public final ArrayList<II> intents;这个是intent-filter的集合
//                //静态内部类反射要用 $+类名
//                //getField(String name)只能获取public的字段，包括父类的；
//                //而getDeclaredField(String name)只能获取自己声明的各种字段，包括public，protected，private。
//                Class<?> mComponentClass = Class.forName("android.content.pm.PackageParser$Component");
//                Field intentsField = mActivity.getClass().getField("intents");
//                ArrayList<IntentFilter> intents = (ArrayList<IntentFilter>) intentsField.get(mActivity);
//                for (IntentFilter intentFilter : intents) {
//                    //6、注册广播
//                    context.registerReceiver(receiver, intentFilter);
//                }

            }

        }catch (Exception e) {
            e.printStackTrace();
        }

    }

    public Resources getResource() {
        return pluginResource;
    }

    public DexClassLoader getClassLoader() {
        return dexClassLoader;
    }

    public AssetManager getAssetManager() {
        return assetManager;
    }
}
