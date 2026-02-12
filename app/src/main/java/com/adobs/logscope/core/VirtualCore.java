package com.adobs.logscope.core;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import java.io.File;

import top.niunaijun.blackbox.BlackBoxCore;
import top.niunaijun.blackbox.entity.pm.InstallResult;

public class VirtualCore {

    private static final String TAG = "VirtualCore";
    
    // SAFETY 1: Thread Safety
    private static volatile VirtualCore instance;
    
    // SAFETY 2: Prevent Memory Leaks
    private final Context appContext;
    
    // SAFETY 3: UI Updates from Background
    private final Handler mainHandler;

    private VirtualCore(Context context) {
        this.appContext = context.getApplicationContext();
        this.mainHandler = new Handler(Looper.getMainLooper());
    }

    public static VirtualCore get(Context context) {
        if (instance == null) {
            synchronized (VirtualCore.class) {
                if (instance == null) {
                    instance = new VirtualCore(context);
                }
            }
        }
        return instance;
    }

    /**
     * असली ऐप को BlackBox Engine में इंस्टॉल और लॉन्च करना।
     */
    public void installAndLaunch(String packageName) {
        try {
            // 1. APK फाइल का पता लगाना
            PackageManager pm = appContext.getPackageManager();
            ApplicationInfo appInfo = pm.getApplicationInfo(packageName, 0);
            
            if (appInfo.sourceDir == null) {
                showToast("Error: APK path not found for " + packageName);
                return;
            }

            File apkFile = new File(appInfo.sourceDir);
            if (!apkFile.exists() || !apkFile.canRead()) {
                showToast("Error: Cannot read APK file.");
                return;
            }

            // 2. क्या ऐप पहले से इंस्टॉल है? (UserId 0 = Default Virtual User)
            if (BlackBoxCore.get().isInstalled(packageName, 0)) {
                Log.i(TAG, "App already installed. Launching directly.");
                showToast("Launching " + packageName + "...");
                launchApp(packageName);
                return;
            }

            // 3. इंस्टॉल करें
            showToast("Installing inside LogScope...");
            Log.d(TAG, "Installing APK from: " + apkFile.getAbsolutePath());

            // FIX: 'installPackageAsUser' API का उपयोग करें (Path + UserId)
            InstallResult result = BlackBoxCore.get().installPackageAsUser(apkFile.getAbsolutePath(), 0);
            
            if (result.success) {
                showToast("Install Success! Launching...");
                launchApp(packageName);
            } else {
                String errorMsg = "Install Failed: " + result.msg;
                Log.e(TAG, errorMsg);
                showToast(errorMsg);
            }

        } catch (PackageManager.NameNotFoundException e) {
            showToast("Target app is not installed on this device.");
        } catch (Exception e) {
            Log.e(TAG, "Critical Virtualization Error", e);
            showToast("System Error: " + e.getMessage());
        }
    }

    /**
     * ऐप लॉन्च करना
     */
    private void launchApp(String packageName) {
        try {
            // FIX: Launch API में UserId (0) पास करना ज़रूरी है
            boolean launched = BlackBoxCore.get().launchApk(packageName, 0);
            
            if (!launched) {
                Log.e(TAG, "Engine failed to launch: " + packageName);
                showToast("Failed to launch app.");
            }
        } catch (Exception e) {
            Log.e(TAG, "Launch Exception", e);
            showToast("Launch Failed: " + e.getMessage());
        }
    }

    private void showToast(String message) {
        mainHandler.post(() -> Toast.makeText(appContext, message, Toast.LENGTH_SHORT).show());
    }
}

