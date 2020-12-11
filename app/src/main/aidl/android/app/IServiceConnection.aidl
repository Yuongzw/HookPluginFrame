// IServiceConnection.aidl
package android.app;
import android.content.ComponentName;
// Declare any non-default types here with import statements

oneway interface IServiceConnection {
    void connected(in ComponentName name, IBinder service, boolean dead);
}