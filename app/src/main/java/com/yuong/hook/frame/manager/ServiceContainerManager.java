package com.yuong.hook.frame.manager;

import android.content.Intent;
import android.os.IBinder;
import android.util.ArrayMap;

/**
 * @author : zhiwen.yang
 * date   : 2020/12/8
 * desc   : Service容器管理类
 */
public class ServiceContainerManager {
    private static ServiceContainerManager instance;

    private ArrayMap<IBinder, Intent> connections = new ArrayMap<>();


    private ServiceContainerManager() {
    }

    public static ServiceContainerManager getInstance() {
        if (instance == null) {
            synchronized (ServiceContainerManager.class) {
                if (instance == null) {
                    instance = new ServiceContainerManager();
                }
            }
        }
        return instance;
    }

    public void putIntentByConnection(IBinder connection, Intent intent) {
        synchronized (connections) {
            connections.put(connection, intent);
        }
    }

    public Intent getIntentByConnection(IBinder connection) {
        synchronized (connections) {
            return connections.remove(connection);
        }
    }
}
