package com.yuong.plugin;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;

import com.yuong.plugin.base.BaseActivity;

public class PluginActivity extends BaseActivity {
    private static final String TAG = "PluginActivity";
    private IBookInterface iBookInterface;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_plugin);
        Log.d(TAG, "PluginActivity onCreate");

        findViewById(R.id.btn_starActivity).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                Log.d(TAG, "启动插件的Activity");
                Intent intent = new Intent(PluginActivity.this, Plugin2Activity.class);
                startActivity(intent);
            }
        });

        findViewById(R.id.btn_startLocalService).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                Log.d(TAG, "启动插件的Service");
                Intent intent = new Intent(PluginActivity.this, PluginLocalService.class);
//                intent.setClassName("com.yuong.plugin", "com.yuong.plugin.PluginLocalService");
                startService(intent);
            }
        });

        findViewById(R.id.btn_stopLocalService).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(TAG, "停止插件的Service");
                Intent intent = new Intent(PluginActivity.this, PluginLocalService.class);
                stopService(intent);
            }
        });
        findViewById(R.id.btn_startRemoteService).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(TAG, "启动插件的远程Service");
                Intent remoteService = new Intent();
                remoteService.setClassName("com.yuong.plugin", "com.yuong.plugin.PluginRemoteService");
                bindService(remoteService, connection, Context.BIND_AUTO_CREATE);
            }
        });
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

    ServiceConnection connection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            Log.d(TAG, "onServiceConnected");
            iBookInterface = IBookInterface.Stub.asInterface(service);
            try {
                iBookInterface.setBookName("yuongzw");
                Log.d(TAG, iBookInterface.getBookName() + "........");
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            iBookInterface = null;
        }
    };
}
