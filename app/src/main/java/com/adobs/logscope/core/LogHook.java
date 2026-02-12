package com.adobs.logscope.core;

import android.util.Log;
import java.lang.reflect.Method;
import top.niunaijun.blackbox.BlackBoxCore;
import top.niunaijun.blackbox.fake.hook.MethodHook;
import top.niunaijun.blackbox.fake.hook.MethodHookParam;

public class LogHook {

    private static final String TAG = "LogScope_Hook";

    public static void startHooking() {
        try {
            Class<?> logClass = android.util.Log.class;
            String[] methods = {"v", "d", "i", "w", "e", "wtf"};

            for (String methodName : methods) {
                // 1. Hook Normal Logs: Log.d(String, String)
                hookSpecificMethod(logClass, methodName, String.class, String.class);

                // 2. Hook Exception Logs: Log.d(String, String, Throwable)
                hookSpecificMethod(logClass, methodName, String.class, String.class, Throwable.class);
            }
            
            LogManager.write("[System] LogHook Engine Attached Successfully.");

        } catch (Exception e) {
            Log.e(TAG, "Failed to initialize LogHook", e);
        }
    }

    /**
     * Helper method to hook specific method signatures safely
     */
    private static void hookSpecificMethod(Class<?> targetClass, String methodName, Class<?>... paramTypes) {
        try {
            // Reflection से सही Method ढूँढना
            Method method = targetClass.getMethod(methodName, paramTypes);
            
            BlackBoxCore.get().getHookManager().addMethodHook(method, new MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    processLog(methodName, param);
                }
            });
        } catch (NoSuchMethodException e) {
            // कुछ Android वर्जन्स में 'wtf' जैसे मेथड्स अलग हो सकते हैं, इसे इग्नोर करें
        } catch (Exception e) {
            Log.e(TAG, "Error hooking method: " + methodName, e);
        }
    }

    /**
     * लॉग्स को प्रोसेस करके LogManager को भेजना
     */
    private static void processLog(String methodName, MethodHookParam param) {
        try {
            if (param.args == null || param.args.length < 2) return;

            // Arguments निकालना
            String tag = String.valueOf(param.args[0]);
            String msg = String.valueOf(param.args[1]);
            String level = methodName.toUpperCase();

            // Exception Data (अगर मौजूद हो)
            StringBuilder exceptionData = new StringBuilder();
            if (param.args.length >= 3 && param.args[2] instanceof Throwable) {
                Throwable tr = (Throwable) param.args[2];
                exceptionData.append("\nStacktrace:\n").append(Log.getStackTraceString(tr));
            }

            // फाइनल लॉग बनाना
            // Format: [D/Tag] Message
            String fullLog = String.format("[%s/%s] %s%s", level, tag, msg, exceptionData.toString());

            // डिस्क पर लिखना (Non-blocking call)
            LogManager.write(fullLog);

        } catch (Exception ex) {
            // साइलेंट फेलियर - ताकि टारगेट ऐप क्रैश न हो
            Log.e(TAG, "Log processing error", ex);
        }
    }
}
