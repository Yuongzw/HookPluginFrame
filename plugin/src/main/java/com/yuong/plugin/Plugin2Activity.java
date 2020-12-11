package com.yuong.plugin;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ListView;
import android.widget.SimpleAdapter;

import com.yuong.plugin.base.BaseAppCompatActivity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class Plugin2Activity extends BaseAppCompatActivity {
    private static final String TAG = "PluginActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        View view = LayoutInflater.from(mContext).inflate(R.layout.activity_plugin2, null);
        setContentView(view);
        Log.d(TAG, "Plugin2Activity onCreate");
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, "Plugin2Activity onResume");
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.d(TAG, "Plugin2Activity onPause");
    }

    @Override
    protected void onStop() {
        super.onStop();
        Log.d(TAG, "Plugin2Activity onStop");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "Plugin2Activity onDestroy");
    }
}
