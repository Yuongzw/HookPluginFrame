package com.yuong.hook.frame.manager;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ServiceInfo;
import android.content.res.AssetManager;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Build;
import android.os.Handler;
import android.text.TextUtils;
import android.util.ArrayMap;
import android.util.Log;

import com.yuong.hook.frame.utils.Utils;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.ref.WeakReference;
import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import dalvik.system.DexClassLoader;
import dalvik.system.PathClassLoader;

/**
 * @author :
 * date   : 2020/6/1
 * desc   :
 */
public class PluginManager {
    private static final String TAG = PluginManager.class.getSimpleName();
    private static PluginManager instance;
    private Context context;
    private DexClassLoader dexClassLoader;
    private AssetManager assetManager;
    private Resources pluginResource;
    private ArrayMap<String, List<ServiceInfo>> serviceInfoMap = new ArrayMap<>();
    private File mNativeLibDir;

    private PluginManager(Context context) {
        this.context = context;
    }

    public static PluginManager getInstance(Context context) {
        if (instance == null) {
            synchronized (PluginManager.class) {
                if (instance == null) {
                    instance = new PluginManager(context);
                }
            }
        }
        return instance;
    }

    public void pluginToApp(final Handler handler, final String path) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    //加载插件的dex文件
                    loadPluginDex(path);

                    loadPluginSo();

                    File apkFile = new File(path);
                    copyNativeLib(apkFile, mNativeLibDir);

                    //加载插件的资源
                    loadPluginResource(path);

                    //解析插件
                    parsePlugin(path);
                    handler.sendEmptyMessage(666);
                } catch (Exception e) {
                    e.printStackTrace();
                    if (handler != null) {
                        handler.sendEmptyMessage(0);
                    }
                }
            }
        }).start();
    }


    private void copyNativeLib(File apk, File nativeLibDir) throws Exception {
        long startTime = System.currentTimeMillis();
        ZipFile zipfile = new ZipFile(apk.getAbsolutePath());

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                for (String cpuArch : Build.SUPPORTED_ABIS) {
                    if (findAndCopyNativeLib(zipfile, cpuArch, nativeLibDir)) {
                        return;
                    }
                }

            } else {
                if (findAndCopyNativeLib(zipfile, Build.CPU_ABI, nativeLibDir)) {
                    return;
                }
            }

            findAndCopyNativeLib(zipfile, "armeabi", nativeLibDir);

        } finally {
            zipfile.close();
            Log.d(TAG, "Done! +" + (System.currentTimeMillis() - startTime) + "ms");
        }
    }

    private boolean findAndCopyNativeLib(ZipFile zipfile, String cpuArch, File nativeLibDir) throws Exception {
        Log.d(TAG, "Try to copy plugin's cup arch: " + cpuArch);
        boolean findLib = false;
        boolean findSo = false;
        byte buffer[] = null;
        String libPrefix = "lib/" + cpuArch + "/";
        ZipEntry entry;
        Enumeration e = zipfile.entries();

        while (e.hasMoreElements()) {
            entry = (ZipEntry) e.nextElement();
            String entryName = entry.getName();

            if (entryName.charAt(0) < 'l') {
                continue;
            }
            if (entryName.charAt(0) > 'l') {
                break;
            }
            if (!findLib && !entryName.startsWith("lib/")) {
                continue;
            }
            findLib = true;
            if (!entryName.endsWith(".so") || !entryName.startsWith(libPrefix)) {
                continue;
            }

            if (buffer == null) {
                findSo = true;
                Log.d(TAG, "Found plugin's cup arch dir: " + cpuArch);
                buffer = new byte[8192];
            }

            String libName = entryName.substring(entryName.lastIndexOf('/') + 1);
            File libParentFile = new File(nativeLibDir.getAbsolutePath(), cpuArch);
            if (!libParentFile.exists()) {
                libParentFile.mkdirs();
            }
            Log.d(TAG, "verify so " + libName);
            File libFile = new File(libParentFile, libName);
//            String key = packageInfo.packageName + "_" + libName;
//            if (libFile.exists()) {
//                int VersionCode = Settings.getSoVersion(context, key);
//                if (VersionCode == packageInfo.versionCode) {
//                    Log.d(TAG, "skip existing so : " + entry.getName());
//                    continue;
//                }
//            }
            FileOutputStream fos = new FileOutputStream(libFile);
            Log.d(TAG, "copy so " + entry.getName() + " of " + cpuArch);
            copySo(buffer, zipfile.getInputStream(entry), fos);
//            Settings.setSoVersion(context, key, packageInfo.versionCode);
        }

        if (!findLib) {
            Log.d(TAG, "Fast skip all!");
            return true;
        }

        return findSo;
    }

    private static void copySo(byte[] buffer, InputStream input, OutputStream output) throws IOException {
        BufferedInputStream bufferedInput = new BufferedInputStream(input);
        BufferedOutputStream bufferedOutput = new BufferedOutputStream(output);
        int count;

        while ((count = bufferedInput.read(buffer)) > 0) {
            bufferedOutput.write(buffer, 0, count);
        }
        bufferedOutput.flush();
        bufferedOutput.close();
        output.close();
        bufferedInput.close();
        input.close();
    }

    private void loadPluginSo() throws Exception {
        //第一步：找到宿主的 dexElements 对象
        //获取 BaseDexClassLoader 类
        Class<?> mainBaseDexClassLoaderClass = Class.forName("dalvik.system.BaseDexClassLoader");
        //获取 pathList 属性
        Field mainPathListField = mainBaseDexClassLoaderClass.getDeclaredField("pathList");
        mainPathListField.setAccessible(true);
        //获取 PathClassLoader
        PathClassLoader mainClassLoader = (PathClassLoader) context.getClassLoader();
        //获取 pathList 属性值
        Object mainPathList = mainPathListField.get(mainClassLoader);


        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP_MR1) {
            Class<?> pathListClass = mainPathList.getClass();
            Field nativeLibraryDirectoriesFiled = pathListClass.getDeclaredField("nativeLibraryDirectories");
            nativeLibraryDirectoriesFiled.setAccessible(true);
            List<File> nativeLibraryDirectories = (List<File>) nativeLibraryDirectoriesFiled.get(mainPathList);
            nativeLibraryDirectories.add(mNativeLibDir);

            Field nativeLibraryPathElementsFiled = pathListClass.getDeclaredField("nativeLibraryPathElements");
            nativeLibraryPathElementsFiled.setAccessible(true);
            Object baseNativeLibraryPathElements = nativeLibraryPathElementsFiled.get(mainPathList);
            final int baseArrayLength = Array.getLength(baseNativeLibraryPathElements);


            //第二步：找到插件的dexElements 对象
            //获取 BaseDexClassLoader 类
            Class<?> pluginBaseDexClassLoaderClass = Class.forName("dalvik.system.BaseDexClassLoader");
            //获取 pathList 属性
            Field pluginPathListField = pluginBaseDexClassLoaderClass.getDeclaredField("pathList");
            pluginPathListField.setAccessible(true);
            Object newPathList = pluginPathListField.get(dexClassLoader);

            Field newNativeLibraryPathElementsFiled = newPathList.getClass().getDeclaredField("nativeLibraryPathElements");
            newNativeLibraryPathElementsFiled.setAccessible(true);
            Object newNativeLibraryPathElements = newNativeLibraryPathElementsFiled.get(newPathList);

            Class<?> elementClass = newNativeLibraryPathElements.getClass().getComponentType();
            Object allNativeLibraryPathElements = Array.newInstance(elementClass, baseArrayLength + 1);
            System.arraycopy(baseNativeLibraryPathElements, 0, allNativeLibraryPathElements, 0, baseArrayLength);

            Field soPathField;
            if (Build.VERSION.SDK_INT >= 26) {
                soPathField = elementClass.getDeclaredField("path");
            } else {
                soPathField = elementClass.getDeclaredField("dir");
            }
            soPathField.setAccessible(true);
            final int newArrayLength = Array.getLength(newNativeLibraryPathElements);
            for (int i = 0; i < newArrayLength; i++) {
                Object element = Array.get(newNativeLibraryPathElements, i);
                String dir = ((File) soPathField.get(element)).getAbsolutePath();
                if (dir.contains("libs")) {
                    Array.set(allNativeLibraryPathElements, baseArrayLength, element);
                    break;
                }
            }
            nativeLibraryPathElementsFiled.set(mainPathList, allNativeLibraryPathElements);
        } else {
            Class<?> pathListClass = mainPathList.getClass();
            Field nativeLibraryDirectoriesFiled = pathListClass.getDeclaredField("nativeLibraryDirectories");
            nativeLibraryDirectoriesFiled.setAccessible(true);
            File[] nativeLibraryDirectories = (File[]) nativeLibraryDirectoriesFiled.get(mainPathList);

            final int N = nativeLibraryDirectories.length;
            File[] newNativeLibraryDirectories = new File[N + 1];
            System.arraycopy(nativeLibraryDirectories, 0, newNativeLibraryDirectories, 0, N);
            newNativeLibraryDirectories[N] = mNativeLibDir;
            nativeLibraryDirectoriesFiled.set(mainPathList, newNativeLibraryDirectories);
        }
    }

    private void loadPluginDex(String path) throws Exception {
        //第一步：找到宿主的 dexElements 对象
        //获取 BaseDexClassLoader 类
        Class<?> mainBaseDexClassLoaderClass = Class.forName("dalvik.system.BaseDexClassLoader");
        //获取 pathList 属性
        Field mainPathListField = mainBaseDexClassLoaderClass.getDeclaredField("pathList");
        mainPathListField.setAccessible(true);
        //获取 PathClassLoader
        PathClassLoader mainClassLoader = (PathClassLoader) context.getClassLoader();
        //获取 pathList 属性值
        Object mainPathList = mainPathListField.get(mainClassLoader);
        //获取 dexElements
        Field mainDexElementsField = mainPathList.getClass().getDeclaredField("dexElements");
        mainDexElementsField.setAccessible(true);
        Object mainDexElements = mainDexElementsField.get(mainPathList);


        //第二步：找到插件的dexElements 对象
        //获取 BaseDexClassLoader 类
        Class<?> pluginBaseDexClassLoaderClass = Class.forName("dalvik.system.BaseDexClassLoader");
        //获取 pathList 属性
        Field pluginPathListField = pluginBaseDexClassLoaderClass.getDeclaredField("pathList");
        pluginPathListField.setAccessible(true);
        //获取 DexClassLoader
        File file = new File(path);
        if (!file.exists()) {
            Log.e(TAG, "插件不存在");
            return;
        }
        File pluginDir = context.getDir("plugin", Context.MODE_PRIVATE);
        mNativeLibDir = new File(pluginDir, "libs");
        if (!pluginDir.exists()) {
            pluginDir.mkdirs();
        }
        if (!mNativeLibDir.exists()) {
            mNativeLibDir.mkdirs();
        }
        //加载插件的class
        dexClassLoader = new DexClassLoader(path, pluginDir.getAbsolutePath(), mNativeLibDir.getAbsolutePath(), context.getClassLoader());
        //获取 pathList 属性值
        Object pluginPathList = pluginPathListField.get(dexClassLoader);
        //获取 dexElements
        Field pluginDexElementsField = pluginPathList.getClass().getDeclaredField("dexElements");
        pluginDexElementsField.setAccessible(true);
        Object pluginDexElements = pluginDexElementsField.get(pluginPathList);

        //第三步：创建出新的 dexElements 对象
        //获取宿主 dexElements 跟插件 dexElements 的长度
        int mainLength = Array.getLength(mainDexElements);
        int pluginLength = Array.getLength(pluginDexElements);
        int newLength = mainLength + pluginLength;
        //创建新的数组
        Object newDexElements = Array.newInstance(mainDexElements.getClass().getComponentType(), newLength);

        //第四步：新的 dexElements = 宿主的dexElements + 插件的dexElements
        for (int i = 0; i < newLength; i++) {
            if (i < mainLength) {
                Array.set(newDexElements, i, Array.get(mainDexElements, i));
            } else {
                Array.set(newDexElements, i, Array.get(pluginDexElements, i - mainLength));
            }
        }

        //第五步：新的 dexElements设置到宿主当中去
        mainDexElementsField.set(mainPathList, newDexElements);
    }

    public void loadPluginResource(final String path) throws Exception {
        //加载插件的资源文件
        //1、获取插件的AssetManager
        assetManager = AssetManager.class.newInstance();
        Method addAssetPath = AssetManager.class.getMethod("addAssetPath", String.class);
        addAssetPath.setAccessible(true);
        addAssetPath.invoke(assetManager, path);
        //2、获取宿主的Resources
        Resources appResources = context.getResources();
        //实例化插件的Resources
        pluginResource = new Resources(assetManager, appResources.getDisplayMetrics(), appResources.getConfiguration());
    }

    @SuppressLint("PrivateApi")
    public void parsePlugin(String pluginPath) throws Exception {
        File file = new File(pluginPath);
        if (!file.exists()) {
            Log.e(TAG, "插件不存在");
            return;
        }
        //1、解析插件包  public Package parsePackage(File packageFile, int flags)
        Class<?> mPackageParserClass = Class.forName("android.content.pm.PackageParser");
        Object mPackageParser = mPackageParserClass.newInstance();
        Method parsePackageMethod = mPackageParserClass.getMethod("parsePackage", File.class, int.class);
        Object mPackage = parsePackageMethod.invoke(mPackageParser, file, PackageManager.GET_SERVICES);

        //2、获取Package类下的   public final ArrayList<Activity> receivers = new ArrayList<Activity>(0); 广播集合
        Field mReceiversField = mPackage.getClass().getDeclaredField("services");
        ArrayList<Object> services = (ArrayList<Object>) mReceiversField.get(mPackage);

        //3、遍历所有的Service
        //Activity 该Activity 不是四大组件里面的activity，而是一个Java bean对象，用来封装清单文件中的activity和receiver
        for (Object mService : services) {
            Field infoFiled = mService.getClass().getDeclaredField("info");
            infoFiled.setAccessible(true);
            ServiceInfo serviceInfo = (ServiceInfo) infoFiled.get(mService);
            List<ServiceInfo> serviceInfoList = serviceInfoMap.get(serviceInfo.name);
            if (serviceInfoList == null) {
                serviceInfoList = new ArrayList<>();
                serviceInfoList.add(serviceInfo);
                serviceInfoMap.put(serviceInfo.name, serviceInfoList);
            }
            Log.d("yuongzw", serviceInfo.name + ", " + serviceInfo.packageName);
        }

    }

    public List<ServiceInfo> getServiceInfos(String packageName) {
        synchronized (serviceInfoMap) {
            return serviceInfoMap.get(packageName);
        }
    }

    public Resources getResource() {
        return pluginResource;
    }

    public DexClassLoader getClassLoader() {
        return dexClassLoader;
    }

    public AssetManager getAssetManager() {
        return assetManager;
    }
}
