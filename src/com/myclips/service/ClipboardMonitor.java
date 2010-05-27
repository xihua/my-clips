package com.myclips.service;

import com.myclips.LogTag;
import com.myclips.MyClips;
import com.myclips.R;
import com.myclips.db.Clip;
import com.myclips.db.ClipboardDbAdapter;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.text.ClipboardManager;
import android.util.Log;

/**
 * Starts a background thread to monitor the states of clipboard and stores
 * any new clips in the SQLite database.
 */
public class ClipboardMonitor extends Service implements LogTag {

    private NotificationManager mNM;
    private MonitorTask mTask = new MonitorTask();
    private ClipboardManager mCM;
    private ClipboardDbAdapter mDbAdapter;

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        mNM = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        showNotification();
        mCM = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
        mDbAdapter = new ClipboardDbAdapter(this);
        mTask.start();
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
        mNM.notify(R.string.clip_monitor_service, notif);
    }
    
    @Override
    public void onDestroy() {
        mNM.cancel(R.string.clip_monitor_service);
        mTask.cancel();
        mDbAdapter.close();
    }
    
    @Override
    public void onStart(Intent intent, int startId) {
    }

    private class MonitorTask extends Thread {

        private volatile boolean mKeepRunning = false;
        private int mIntervalMillis;
        private CharSequence mOldClip = null;

        
        public MonitorTask() {
            this(3000);
        }

        public MonitorTask(int intervalMillis) {
            super("ClipboardMonitor");
            this.mIntervalMillis = intervalMillis;
        }

        /** Cancel task */
        public void cancel() {
            mKeepRunning = false;
            interrupt();
        }
        
        @Override
        public void run() {
            mKeepRunning = true;
            while (true) {
                doTask();
                try {
                    Thread.sleep(mIntervalMillis);
                } catch (InterruptedException ignored) {
                }
                if (!mKeepRunning) {
                    break;
                }
            }
        }
        
        private void doTask() {
            if (mCM.hasText()) {
                CharSequence newClip = mCM.getText();
                if (!newClip.equals(mOldClip)) {
                    mOldClip = newClip;
                    /* TODO: now just insert into default clipboard (id = 1);
                     * in the future, get selected clipboard from Preference
                     * to insert new clips
                     */
                    mDbAdapter.insertClip(Clip.CLIP_TYPE_TEXT,
                            newClip.toString(), 1);
                    Log.i(TAG, "new clip inserted: " + newClip.toString());
                }
            }
        }
    }
}
