package com.yuong.plugin.base;

import android.app.Activity;
import android.content.res.AssetManager;
import android.content.res.Resources;
import androidx.appcompat.app.AppCompatActivity;

public abstract class BaseActivity extends Activity {

    @Override
    public Resources getResources() {
        if (getApplication() != null && getApplication().getResources() != null) {
            //如果不为空，就说明已经被添加到了宿主当中
            return getApplication().getResources();
        }
        return super.getResources();
    }

    @Override
    public AssetManager getAssets() {
        if (getApplication() != null && getApplication().getAssets() !=  null) {
            //如果不为空，就说明已经被添加到了宿主当中
            return getApplication().getAssets();
        }
        return super.getAssets();
    }

    @Override
    public Resources.Theme getTheme() {
        if (getApplication() != null && getApplication().getTheme() !=  null) {
            //如果不为空，就说明已经被添加到了宿主当中
            return getApplication().getTheme();
        }
        return super.getTheme();
    }
}
