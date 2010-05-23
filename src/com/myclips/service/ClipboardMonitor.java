package com.myclips.service;

import com.myclips.MyClips;
import com.myclips.R;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

/**
 * Starts a background thread to monitor the states of clipboard and stores
 * any new clips in the SQLite database.
 */
public class ClipboardMonitor extends Service {

    private NotificationManager nm;
    private MonitorTask task = new MonitorTask();

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        showNotification();
        task.start();
    }

    private void showNotification() {
        Notification notif = new Notification(R.drawable.myclips_icon,
                "MyClips clipboard monitor is started",
                System.currentTimeMillis());
        PendingIntent contentIntent = PendingIntent.getActivity(this, 0,
                new Intent(this, MyClips.class), 0);
        notif.setLatestEventInfo(this, getText(R.string.clip_monitor_service),
                "Tap here to enter MyClips UI", contentIntent);
        // Use layout id because it's unique
        nm.notify(R.string.clip_monitor_service, notif);
    }
    
    @Override
    public void onDestroy() {
        nm.cancel(R.string.clip_monitor_service);
        task.cancel();
    }
    
    @Override
    public void onStart(Intent intent, int startId) {
    }

    private static class MonitorTask extends Thread {

        private volatile boolean runFlag = false;
        int interval_ms;
        
        public MonitorTask() {
            this(3000);
        }

        public MonitorTask(int intervalMillis) {
            super("ClipboardMonitor");
            this.interval_ms = intervalMillis;
        }

        /** Cancel task */
        public void cancel() {
            runFlag = false;
            interrupt();
        }
        
        @Override
        public void run() {
            runFlag = true;
            while (true) {
                doTask();
                try {
                    Thread.sleep(interval_ms);
                } catch (InterruptedException ignored) {
                }
                if (!runFlag) {
                    break;
                }
            }
        }
        
        private void doTask() {
            // TODO monitor task;
        }
    }
}
