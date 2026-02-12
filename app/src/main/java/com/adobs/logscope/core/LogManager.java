package com.adobs.logscope.core;

import android.os.Environment;
import android.util.Log;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class LogManager {

    private static final String TAG = "LogManager";
    
    // SAFETY 1: Bounded Queue to prevent OOM
    private static final BlockingQueue<String> logQueue = new LinkedBlockingQueue<>(50000);
    
    private static volatile boolean isRunning = false;
    private static Thread writerThread;
    private static BufferedWriter bufferedWriter;

    /**
     * Initialize file and background thread
     */
    public static void init(String packageName) {
        if (isRunning) return;

        try {
            // Saving logs in "Documents/LogScope" (Better for Android 11+)
            File root = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS), "LogScope");
            File appFolder = new File(root, packageName);
            
            if (!appFolder.exists() && !appFolder.mkdirs()) {
                Log.e(TAG, "Critical Error: Failed to create directories at " + appFolder.getAbsolutePath());
                return;
            }

            String timeStamp = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.US).format(new Date());
            File currentLogFile = new File(appFolder, "Log_" + timeStamp + ".txt");
            
            // SAFETY 2: 8KB Buffer for efficient writing
            bufferedWriter = new BufferedWriter(new FileWriter(currentLogFile, true), 8192);
            
            isRunning = true;
            startWriterThread();

            write("--- Session Started: " + packageName + " ---");
            Log.d(TAG, "LogManager Initialized at: " + currentLogFile.getAbsolutePath());

        } catch (IOException e) {
            Log.e(TAG, "Error creating log file: ", e);
        }
    }

    /**
     * Background Writer Thread
     */
    private static void startWriterThread() {
        writerThread = new Thread(() -> {
            int logCount = 0;
            
            while (isRunning || !logQueue.isEmpty()) {
                try {
                    // Blocking call - CPU sleeps until new log arrives
                    String message = logQueue.take(); 
                    
                    if (bufferedWriter != null) {
                        bufferedWriter.write(message);
                        bufferedWriter.newLine();
                        
                        // PERFORMANCE FIX: Flush only every 100 logs
                        logCount++;
                        if (logCount % 100 == 0) {
                            bufferedWriter.flush();
                        }
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                } catch (IOException e) {
                    Log.e(TAG, "Disk Write Failed: ", e);
                }
            }
            closeSilently();
        }, "LogScope-AsyncWriter");
        
        writerThread.setPriority(Thread.MIN_PRIORITY); 
        writerThread.start();
    }

    /**
     * API called by LogHook (Non-blocking)
     */
    public static void write(String message) {
        if (!isRunning) return;

        try {
            String time = new SimpleDateFormat("HH:mm:ss.SSS", Locale.US).format(new Date());
            String formattedLog = time + " : " + message;
            
            // If queue is full, drop log instead of crashing app
            if (!logQueue.offer(formattedLog)) {
                Log.e(TAG, "Log Queue Full! Dropping log.");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error processing log message", e);
        }
    }

    public static void shutdown() {
        isRunning = false;
        if (writerThread != null) {
            writerThread.interrupt();
        }
    }

    private static void closeSilently() {
        if (bufferedWriter != null) {
            try {
                bufferedWriter.flush();
                bufferedWriter.close();
            } catch (IOException ignored) {}
        }
    }
}
