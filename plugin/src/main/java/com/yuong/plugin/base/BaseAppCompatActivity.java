package com.yuong.plugin.base;

import android.content.res.Resources;
import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.view.ContextThemeWrapper;

import java.lang.reflect.Field;

public abstract class BaseAppCompatActivity extends AppCompatActivity {

    protected ContextThemeWrapper mContext;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Resources resources = PluginManager.getInstance(getApplication()).getResource();
        mContext = new ContextThemeWrapper(getBaseContext(), 0);
        //替換插件的
        Class<? extends ContextThemeWrapper> clazz = mContext.getClass();
        try {
            Field mResourcesFiled = clazz.getDeclaredField("mResources");
            mResourcesFiled.setAccessible(true);
            mResourcesFiled.set(mContext, resources);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
