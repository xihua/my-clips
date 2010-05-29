package com.myclips.service;

import java.util.HashSet;
import java.util.Set;

import com.myclips.LogTag;
import com.myclips.MyClips;
import com.myclips.R;
import com.myclips.db.Clip;
import com.myclips.db.ClipboardDbAdapter;
import com.myclips.prefs.AppPrefs;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.FileObserver;
import android.os.IBinder;
import android.text.ClipboardManager;
import android.util.Log;

/**
 * Starts a background thread to monitor the states of clipboard and stores
 * any new clips into the SQLite database.
 * <p>
 * <i>Note:</i> the current android clipboard system service only supports
 * text clips, so in browser, we can just save images to external storage
 * (SD card). This service also monitors the downloads of browser, if any
 * image is detected, it will be stored into SQLite database, too.   
 */
public class ClipboardMonitor extends Service implements LogTag {
    
    /** Image type to be monitored */
    private static final String[] IMAGE_SUFFIXS = new String[] {
        "jpg", "jpeg", "gif", "png"
    };
    /** Path to browser downloads */
    private static final String BROWSER_DOWNLOAD_PATH = "/sdcard/download";
    
    private NotificationManager mNM;
    private MonitorTask mTask = new MonitorTask();
    private ClipboardManager mCM;
    private ClipboardDbAdapter mDbAdapter;
    private SharedPreferences mPrefs;

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
        mPrefs = getSharedPreferences(AppPrefs.NAME, MODE_PRIVATE);
        mTask.start();
    }

    private void showNotification() {
        Notification notif = new Notification(R.drawable.myclips_icon,
                "MyClips clipboard monitor is started",
                System.currentTimeMillis());
        notif.flags |= (Notification.FLAG_ONGOING_EVENT | Notification.FLAG_NO_CLEAR);
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

    /**
     * Monitor task: monitor new text clips in global system clipboard and
     * new image clips in browser download directory
     */
    private class MonitorTask extends Thread {

        private volatile boolean mKeepRunning = false;
        private CharSequence mOldClip = null;
        private BrowserDownloadMonitor mBDM = new BrowserDownloadMonitor();
        
        public MonitorTask() {
            super("ClipboardMonitor");
        }

        /** Cancel task */
        public void cancel() {
            mKeepRunning = false;
            interrupt();
        }
        
        @Override
        public void run() {
            mKeepRunning = true;
            mBDM.startWatching();
            while (true) {
                doTask();
                try {
                    Thread.sleep(mPrefs.getInt(AppPrefs.KEY_MONITOR_INTERVAL,
                            AppPrefs.DEF_MONITOR_INTERVAL));
                } catch (InterruptedException ignored) {
                }
                if (!mKeepRunning) {
                    break;
                }
            }
            mBDM.stopWatching();
        }
        
        private void doTask() {
            if (mCM.hasText()) {
                CharSequence newClip = mCM.getText();
                if (!newClip.equals(mOldClip)) {
                    Log.i(TAG, "detect new text clip: " + newClip.toString());
                    mOldClip = newClip;
                    mDbAdapter.insertClip(Clip.CLIP_TYPE_TEXT,
                            newClip.toString(),
                            mPrefs.getInt(AppPrefs.KEY_OPERATING_CLIPBOARD,
                                    AppPrefs.DEF_OPERATING_CLIPBOARD));
                    Log.i(TAG, "new text clip inserted: " + newClip.toString());
                }
            }
        }
        
        /**
         * Monitor change of download directory of browser. It listens two
         * events: <tt>CREATE</tt> and <tt>CLOSE_WRITE</tt>. <tt>CREATE</tt>
         * event occurs when new file created in download directory. If this
         * file is image, new image clip will be inserted into database when 
         * receiving <tt>CLOSE_WRITE</tt> event, meaning file is sucessfully
         * downloaded.
         */
        private class BrowserDownloadMonitor extends FileObserver {

            private Set<String> mFiles = new HashSet<String>();
            
            public BrowserDownloadMonitor() {
                super(BROWSER_DOWNLOAD_PATH, CREATE | CLOSE_WRITE);
            }
            
            private void doDownloadCompleteAction(String path) {
                mDbAdapter.insertClip(Clip.CLIP_TYPE_IMAGE,
                        BROWSER_DOWNLOAD_PATH + path,
                        mPrefs.getInt(AppPrefs.KEY_OPERATING_CLIPBOARD,
                                AppPrefs.DEF_OPERATING_CLIPBOARD));
                Log.i(TAG, "new image clip inserted: " + path);
            }
            
            @Override
            public void onEvent(int event, String path) {
                switch (event) {
                    case CREATE:
                        for (String s : IMAGE_SUFFIXS) {
                            if (path.endsWith(s)) {
                                Log.i(TAG, "detect new image: " + path);
                                mFiles.add(path);
                                break;
                            }
                        }
                        break;
                    case CLOSE_WRITE:
                        if (mFiles.remove(path)) { // File download completes
                            doDownloadCompleteAction(path);
                        }
                        break;
                    default:
                        throw new RuntimeException("BrowserDownloadMonitor" +
                        		" got unexpected event");
                }
            }
        }
    }
}
