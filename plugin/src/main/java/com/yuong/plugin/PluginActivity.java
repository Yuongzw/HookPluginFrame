package com.yuong.plugin;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;

import com.yuong.plugin.base.BaseActivity;

public class PluginActivity extends BaseActivity {
    private static final String TAG = "PluginActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_plugin);
        findViewById(R.id.btn_jump).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(PluginActivity.this, Plugin2Activity.class));
            }
        });
        Log.d(TAG, "PluginActivity onCreate");
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, "PluginActivity onResume");
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.d(TAG, "PluginActivity onPause");
    }

    @Override
    protected void onStop() {
        super.onStop();
        Log.d(TAG, "PluginActivity onStop");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "PluginActivity onDestroy");
    }
}
