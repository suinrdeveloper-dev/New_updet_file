package com.adobs.logscope.core;

import android.util.Log;
import java.lang.reflect.Method;

// ✅ FIXED IMPORTS: Changed 'fake.hook' to 'core.hook'
import top.niunaijun.blackbox.BlackBoxCore;
import top.niunaijun.blackbox.core.hook.MethodHook;
import top.niunaijun.blackbox.core.hook.MethodHookParam;

public class LogHook {

    private static final String TAG = "LogScope_Hook";

    public static void startHooking() {
        try {
            Class<?> logClass = android.util.Log.class;
            String[] methods = {"v", "d", "i", "w", "e", "wtf"};

            for (String methodName : methods) {
                // Hook Normal Logs: Log.d(String, String)
                hookSpecificMethod(logClass, methodName, String.class, String.class);

                // Hook Exception Logs: Log.d(String, String, Throwable)
                hookSpecificMethod(logClass, methodName, String.class, String.class, Throwable.class);
            }
            
            LogManager.write("[System] LogHook Engine Attached Successfully.");

        } catch (Exception e) {
            Log.e(TAG, "Failed to initialize LogHook", e);
        }
    }

    private static void hookSpecificMethod(Class<?> targetClass, String methodName, Class<?>... paramTypes) {
        try {
            Method method = targetClass.getMethod(methodName, paramTypes);
            
            BlackBoxCore.get().getHookManager().addMethodHook(method, new MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    processLog(methodName, param);
                }
            });
        } catch (NoSuchMethodException e) {
        } catch (Exception e) {
            Log.e(TAG, "Error hooking method: " + methodName, e);
        }
    }

    private static void processLog(String methodName, MethodHookParam param) {
        try {
            if (param.args == null || param.args.length < 2) return;

            String tag = String.valueOf(param.args[0]);
            String msg = String.valueOf(param.args[1]);
            String level = methodName.toUpperCase();

            StringBuilder exceptionData = new StringBuilder();
            if (param.args.length >= 3 && param.args[2] instanceof Throwable) {
                Throwable tr = (Throwable) param.args[2];
                exceptionData.append("\nStacktrace:\n").append(Log.getStackTraceString(tr));
            }

            String fullLog = String.format("[%s/%s] %s%s", level, tag, msg, exceptionData.toString());
            LogManager.write(fullLog);

        } catch (Exception ex) {
            Log.e(TAG, "Log processing error", ex);
        }
    }
}
