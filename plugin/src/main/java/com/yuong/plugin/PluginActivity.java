package com.yuong.plugin;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;
import android.view.View;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TextView;

import androidx.annotation.RequiresApi;

import com.yuong.plugin.base.BaseActivity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class PluginActivity extends BaseActivity {
    private static final String TAG = "PluginActivity";
    private IBookInterface iBookInterface;

    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_plugin);
        Log.d(TAG, "PluginActivity onCreate");

//        TextView textView = findViewById(R.id.textView);
//        textView.setTextColor(getColor(R.color.textColor));
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
                Intent remoteService = new Intent(PluginActivity.this, PluginRemoteService.class);
                startService(remoteService);
                bindService(remoteService, connection, Context.BIND_AUTO_CREATE);
            }
        });

        findViewById(R.id.btn_unBindemoteService).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                unbindService(connection);
                Intent remoteService = new Intent(PluginActivity.this, PluginRemoteService.class);
                stopService(remoteService);
            }
        });
        findViewById(R.id.btn_sendReceiver).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent1 = new Intent("yuongzw");
                sendBroadcast(intent1);
            }
        });
        IntentFilter filter = new IntentFilter("yuongzw");
        registerReceiver(receiver, filter);
        ListView listView = findViewById(R.id.listView);
        List<HashMap<String, Object>> m_event_data = new ArrayList<HashMap<String,Object>>();
        HashMap<String, Object> item = new HashMap<String, Object>();
        item.put("time", "时间");
        item.put("event", "事件内容");
        m_event_data.add(item);

        SimpleAdapter adapter = new SimpleAdapter(PluginActivity.this, m_event_data, R.layout.event_list_item,
                new String[]{"time", "event"}, new int[]{R.id.event_time, R.id.event_name});

        listView.setAdapter(adapter);
    }


    private BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals("yuongzw")) {
                Log.d(TAG, "收到广播了。。。");
                int yuongzw = intent.getIntExtra("yuongzw", -1);
                Log.d(TAG, "value=" + yuongzw);
            }
        }
    };

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
        unregisterReceiver(receiver);
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
            Log.d(TAG, "onServiceDisconnected");
            iBookInterface = null;
        }
    };
}
