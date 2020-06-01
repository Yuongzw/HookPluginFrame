package com.yuong.hook.frame.manager;

import android.content.Context;
import android.content.res.AssetManager;
import android.content.res.Resources;
import android.os.Handler;
import android.util.Log;

import java.io.File;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

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
            if (dexClassLoader != null && pluginResource != null) {
                handler.sendEmptyMessage(666);
            } else {
                handler.sendEmptyMessage(0);
            }
        } catch (Exception e) {
            e.printStackTrace();
            handler.sendEmptyMessage(0);
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
