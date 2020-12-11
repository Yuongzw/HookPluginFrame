package com.yuong.hook.frame;

import android.Manifest;
import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.content.res.Resources;
import android.os.Build;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;

import com.yuong.hook.frame.manager.HookManager;
import com.yuong.hook.frame.manager.PluginManager;
import com.yuong.hook.frame.proxy.ProxyActivity;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.List;

import me.weishu.reflection.Reflection;

/**
 * @author :
 * date   : 2020/6/1
 * desc   :
 */
public class MyApplication extends Application {
    public static boolean isHookSystemApi = false;
    private static Context context;
    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
        context = base;
        Reflection.unseal(base);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d("yuongzw", "MyApplication oncreate...");
        int selfPermission = ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE);
        if (selfPermission == PackageManager.PERMISSION_GRANTED) {
            try {
                //先hook AMS检查
                HookManager.getInstance(getApplicationContext()).hookAMSAction();
                //hook ActivityThread
                HookManager.getInstance(getApplicationContext()).hookLaunchActivity();
                isHookSystemApi = true;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public static Context getContext() {
        return context;
    }

    @Override
    public Resources getResources() {
        return PluginManager.getInstance(context).getResource() == null ? super.getResources() : PluginManager.getInstance(this).getResource();
    }

    @Override
    public AssetManager getAssets() {
        return PluginManager.getInstance(context).getAssetManager() == null ? super.getAssets() : PluginManager.getInstance(this).getAssetManager();
    }
}
