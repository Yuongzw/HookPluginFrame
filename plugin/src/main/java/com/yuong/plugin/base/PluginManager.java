package com.yuong.plugin.base;

import android.content.Context;
import android.content.res.AssetManager;
import android.content.res.Resources;
import android.os.Environment;
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
    private String pluginPath = Environment.getExternalStorageDirectory().getAbsolutePath() + File.separator + "plugin-debug.apk";

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

    public Resources getResource() {
        if (pluginResource == null) {
            try {
                //加载插件的资源文件
                //1、获取插件的AssetManager
                assetManager = AssetManager.class.newInstance();
                Method addAssetPath = AssetManager.class.getMethod("addAssetPath", String.class);
                addAssetPath.setAccessible(true);
                addAssetPath.invoke(assetManager, pluginPath);
                //2、获取宿主的Resources
                Resources appResources = context.getResources();
                //实例化插件的Resources
                pluginResource = new Resources(assetManager, appResources.getDisplayMetrics(), appResources.getConfiguration());
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return pluginResource;
    }

    public DexClassLoader getClassLoader() {
        return dexClassLoader;
    }

    public AssetManager getAssetManager() {
        getResource();
        return assetManager;
    }
}
