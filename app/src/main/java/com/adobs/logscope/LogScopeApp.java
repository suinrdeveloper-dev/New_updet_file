package com.adobs.logscope;

import android.app.Application;
import android.content.Context;
import android.util.Log;

import com.adobs.logscope.core.LogHook;
import com.adobs.logscope.core.LogManager;

import top.niunaijun.blackbox.BlackBoxCore;

public class LogScopeApp extends Application {

    private static final String TAG = "LogScopeApp";

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
        try {
            // इंजन को अटैच करना
            BlackBoxCore.get().doAttachBaseContext(base);
        } catch (Exception e) {
            Log.e(TAG, "Fatal Error: BlackBoxCore failed to attach base context", e);
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();

        try {
            // ✅ FIX: doOnCreate() को बदलकर doCreate() कर दिया गया है
            BlackBoxCore.get().doCreate();

            if (BlackBoxCore.get().isVirtualProcess()) {
                String processName = resolveProcessName();
                
                // इंजन के अंदर लॉगिंग सिस्टम शुरू करना
                LogManager.init(processName);
                LogHook.startHooking();
                
                return;
            }
        } catch (Exception e) {
            Log.e(TAG, "Fatal Error: BlackBoxCore initialization failed in onCreate", e);
        }
    }

    /**
     * वर्चुअल प्रोसेस का नाम सुरक्षित रूप से निकालता है
     */
    private String resolveProcessName() {
        try {
            String processName = top.niunaijun.blackbox.app.BActivityThread.currentProcessName();
            if (processName != null && !processName.isEmpty()) {
                return processName;
            }
        } catch (Exception | Error e) {
            Log.w(TAG, "BActivityThread.currentProcessName() failed. Falling back.", e);
        }
        return "Unknown_Virtual_Process";
    }
}
