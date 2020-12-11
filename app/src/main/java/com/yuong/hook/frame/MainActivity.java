package com.yuong.hook.frame;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.ProgressDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.yuong.hook.frame.manager.HookManager;
import com.yuong.hook.frame.manager.PluginManager;
import com.yuong.hook.frame.proxy.ProxyRemoteService;

import java.io.File;

public class MainActivity extends AppCompatActivity {
    private ProgressDialog dialog;
    //是否加载完成
    private boolean isLoadSuccess = false;

    private String path = Environment.getExternalStorageDirectory().getAbsolutePath() + File.separator + "plugin-debug.apk";
    @SuppressLint("HandlerLeak")
    private Handler handler = new Handler() {
        @Override
        public void handleMessage(@NonNull Message msg) {
            super.handleMessage(msg);
            if (msg.what == 666) {
                isLoadSuccess = true;
                if (dialog != null && dialog.isShowing()) {
                    dialog.dismiss();
                }
                Toast.makeText(MainActivity.this, "加载插件成功！", Toast.LENGTH_SHORT).show();
            } else if (msg.what == 0) {
                if (dialog != null && dialog.isShowing()) {
                    dialog.dismiss();
                }
                Toast.makeText(MainActivity.this, "加载插件失败，请检查插件是否存在！", Toast.LENGTH_SHORT).show();
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    public void loadPlugin(View view) {
        //判断是否已经赋予权限
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ContextCompat.checkSelfPermission(this,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{
                        Manifest.permission.WRITE_EXTERNAL_STORAGE,
                        Manifest.permission.READ_EXTERNAL_STORAGE}, 666);
            } else {
                showProgress();
                try {
                    if (!MyApplication.isHookSystemApi) {
                        HookManager.getInstance(getApplicationContext()).hookAMSAction();
                        HookManager.getInstance(getApplicationContext()).hookLaunchActivity();
                        MyApplication.isHookSystemApi = true;
                    }
                    PluginManager.getInstance(MyApplication.getContext()).pluginToApp(handler, path);
                } catch (Exception e) {
                    e.printStackTrace();
                    if (dialog != null && dialog.isShowing()) {
                        dialog.dismiss();
                    }
                    Toast.makeText(MainActivity.this, e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            }
        } else {
            showProgress();
            PluginManager.getInstance(MyApplication.getContext()).pluginToApp(handler, path);
        }

    }

    public void jumpPlugin(View view) {
        if (!isLoadSuccess) {
            Toast.makeText(this, "请先加载插件", Toast.LENGTH_SHORT).show();
        } else {
            Intent intent = new Intent();
//            intent.setComponent(new ComponentName("com.dingli.pioneer", "com.dingli.pioneer.Main2Activity"));
            intent.setComponent(new ComponentName("com.yuong.plugin", "com.yuong.plugin.PluginActivity"));
            startActivity(intent);
        }
//        Intent intent = new Intent(this, TestActivity.class);
//        startActivity(intent);
    }

    private void showProgress() {
        if (dialog == null) {
            dialog = new ProgressDialog(this);
        }
        dialog.setTitle("加载插件中，请稍后。。。");
        dialog.setCancelable(false);
        dialog.show();
    }

    public void bindLocalService(View view) {
        Intent intent = new Intent(this, ProxyRemoteService.class);
        boolean b = bindService(intent, new ServiceConnection() {
            @Override
            public void onServiceConnected(ComponentName name, IBinder service) {
                Log.d("yuongzw", "service Connected");
            }

            @Override
            public void onServiceDisconnected(ComponentName name) {
                Log.d("yuongzw", "service Disconnected");
            }
        }, Context.BIND_AUTO_CREATE);
    }
}
