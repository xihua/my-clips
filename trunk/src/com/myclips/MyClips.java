package com.myclips;

import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;

import com.myclips.db.Clip;
import com.myclips.db.ClipboardDbAdapter;
import com.myclips.prefs.AppPrefs;
import com.myclips.service.ClipboardMonitor;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.ContextMenu;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.View.OnTouchListener;
import android.view.animation.AnimationUtils;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;
import android.widget.ViewFlipper;

public class MyClips extends Activity implements OnTouchListener, LogTag {

    private ViewFlipper vf = null;
    private SharedPreferences mPrefs;
    private static final int OPT_NEW_CLIPBOARD = Menu.FIRST;
    private static final int OPT_EMAIL_CLIPBOARD = Menu.FIRST + 1;
    private static final int OPT_LIST_ALL_CLIPBOARDS = Menu.FIRST + 2;
    private static final int OPT_DELETE_CLIPBOARD = Menu.FIRST + 3;
    private static final int CNTX_INFO = Menu.FIRST + 4;
    private static final int CNTX_DELETE_CLIP = Menu.FIRST + 5;
    private static final int CNTX_EMAIL_CLIP = Menu.FIRST + 6;
    private ClipboardDbAdapter mDbHelper;
    private SensorManager mSM;
    private Sensor mSensor;
    private MotionDetector mMD;;
    private float downXValue;
    private float downYValue;
    private String selectedClipTitle;
    private int clipIdInContext;

    /** Records of all clipboards id in increasing order */
    private int[] cbIdList;
    /** The index of current clipboard with respect to <tt>cbIdList</tt> */
    private int cbIndex;
    private ListView[] cpList;
    private TextView[] cpListCap;
    /** Clips cursors to iterate all clips in cached clipboards */
    private Cursor[] cpCursor;
    /** Clipboard cursor to iterate all clipboards */
    private Cursor cbCursor;
    private Cursor clipCursor;
    /**
     * Clipboard cache size
     * <p>
     * If number of clipboards is greater than this value, only
     * <tt>CB_CACHE_SIZE</tt> of clipboards will be loaded.
     */
    private static final int CB_CACHE_SIZE = 3; // should be odd number
    private Handler uiHandler;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        mDbHelper = new ClipboardDbAdapter(this);
        mPrefs = getSharedPreferences(AppPrefs.NAME, 0);
        uiHandler = new Handler(new UiHandler());
        mSM = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        mSensor = mSM.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        mMD = new MotionDetector();
        mSM.registerListener(mMD, mSensor, SensorManager.SENSOR_DELAY_UI);

        AppPrefs.operatingClipboardId = mPrefs.getInt(
                AppPrefs.KEY_OPERATING_CLIPBOARD,
                AppPrefs.DEF_OPERATING_CLIPBOARD);
        Log.i(TAG, "MyClips OnCreate(): set operatingClipboardId = "
                + AppPrefs.operatingClipboardId);

        vf = (ViewFlipper) findViewById(R.id.details);
        showClipboards();

        ((ClipboardFlipper) vf).setInterceptTouchListener(this);
        vf.setOnTouchListener(this);

        startClipboardMonitor();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mDbHelper.close();
        mSM.unregisterListener(mMD, mSensor);
    }

    @Override
    protected void onStart() {
        super.onStart();
        Log.i(TAG, "MyClips onStart(): executing...");
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.i(TAG, "MyClips onPause(): write back operatingClipboardId = "
                + AppPrefs.operatingClipboardId);
        SharedPreferences.Editor editor = mPrefs.edit();
        editor.putInt(AppPrefs.KEY_OPERATING_CLIPBOARD,
                AppPrefs.operatingClipboardId);
        editor.commit();
    }

    /** Used for first showing clipboards */
    private void showClipboards() {
        cbCursor = mDbHelper.queryAllClipboards();
        startManagingCursor(cbCursor);

        cbIdList = new int[cbCursor.getCount()];
        for (int i = 0; cbCursor.moveToNext(); ++i) {
            cbIdList[i] = cbCursor.getInt(0);
        }
        Log.i(TAG, "cbIdList: " + Arrays.toString(cbIdList));
        Log.i(TAG, "operatingClipboardId = " + AppPrefs.operatingClipboardId);

        cbIndex = Arrays.binarySearch(cbIdList, AppPrefs.operatingClipboardId);
        if (cbIndex < 0) {
            // can't find operating clipboard by id; this shouldn't happen
            Log.w(TAG, "can't find operatingClipboardId = "
                    + AppPrefs.operatingClipboardId + ", use first clipboard");
            cbIndex = 0;
            AppPrefs.operatingClipboardId = cbIdList[0];
        }
        Log.i(TAG, "cbIndex = " + cbIndex);

        /*
         * if number of clipboards <= cache size, just load all clipboards
         */
        if (cbIdList.length <= CB_CACHE_SIZE) {
            cpList = new ListView[cbIdList.length];
            cpListCap = new TextView[cbIdList.length];
            cpCursor = new Cursor[cbIdList.length];

            for (int i = 0; i < cbIdList.length; ++i) {
                int j = (cbIndex + i) % cbIdList.length;
                cbCursor.moveToPosition(j);

                cpList[i] = new ListView(this);
                cpListCap[i] = new TextView(this);
                cpListCap[i].setGravity(Gravity.CENTER_VERTICAL
                        | Gravity.CENTER_HORIZONTAL);
                cpListCap[i].setText(cbCursor.getString(1));
                cpList[i].addHeaderView(cpListCap[i], null, false);
                cpCursor[i] = mDbHelper.queryAllClips(cbIdList[j]);
                startManagingCursor(cpCursor[i]);
                cpList[i].setAdapter(new SimpleCursorAdapter(this,
                        R.layout.clip_entry, cpCursor[i],
                        new String[] { Clip.COL_DATA },
                        new int[] { R.id.clipEntryText }));

                registerForContextMenu(cpList[i]);

                vf.addView(cpList[i], new ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.FILL_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT));
            }
        } else { // number of clipboards > cache size
            cpList = new ListView[CB_CACHE_SIZE];
            cpListCap = new TextView[CB_CACHE_SIZE];
            cpCursor = new Cursor[CB_CACHE_SIZE];

            for (int i = 0; i < CB_CACHE_SIZE; ++i) {
                int j;
                if (i <= CB_CACHE_SIZE / 2) {
                    j = (cbIndex + i) % cbIdList.length;
                } else {
                    j = (cbIndex + i - CB_CACHE_SIZE + cbIdList.length)
                            % cbIdList.length;
                }
                //Log.i(TAG, "cbIndex: j = " + j);
                cbCursor.moveToPosition(j);

                cpList[i] = new ListView(this);
                cpListCap[i] = new TextView(this);
                cpListCap[i].setGravity(Gravity.CENTER_VERTICAL
                        | Gravity.CENTER_HORIZONTAL);
                cpListCap[i].setText(cbCursor.getString(1));
                cpList[i].addHeaderView(cpListCap[i], null, false);
                cpCursor[i] = mDbHelper.queryAllClips(cbIdList[j]);
                startManagingCursor(cpCursor[i]);
                cpList[i].setAdapter(new SimpleCursorAdapter(this,
                        R.layout.clip_entry, cpCursor[i],
                        new String[] { Clip.COL_DATA },
                        new int[] { R.id.clipEntryText }));

                registerForContextMenu(cpList[i]);

                vf.addView(cpList[i], new ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.FILL_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT));
            }
        }
    }
    
    /**
     * When clipboards change, like create or delete a clipboard, you should
     * call this method. If you want to change to a specified clipboard, also
     * call this method.
     */
    private void updateClipboards() {
        /*
         * At below, we just use naive way: discard all old things and then
         * recreate. Maybe make it smarter in the future.
         */
        cbCursor.requery();

        //int oldCount = cbIdList.length;
        cbIdList = new int[cbCursor.getCount()];
        for (int i = 0; cbCursor.moveToNext(); ++i) {
            cbIdList[i] = cbCursor.getInt(0);
        }
        Log.i(TAG, "cbIdList: " + Arrays.toString(cbIdList));
        Log.i(TAG, "operatingClipboardId = " + AppPrefs.operatingClipboardId);

        cbIndex = Arrays.binarySearch(cbIdList, AppPrefs.operatingClipboardId);
        if (cbIndex < 0) {
            // can't find operating clipboard by id; this shouldn't happen
            Log.w(TAG, "can't find operatingClipboardId = "
                    + AppPrefs.operatingClipboardId + ", use first clipboard");
            cbIndex = 0;
            AppPrefs.operatingClipboardId = cbIdList[0];
        }
        Log.i(TAG, "cbIndex = " + cbIndex);

        vf.removeAllViews();
        for (int i = 0; i < cpCursor.length; ++i) {
            stopManagingCursor(cpCursor[i]);
        }
        
        /*
         * if number of clipboards <= cache size, just load all clipboards
         */
        if (cbIdList.length <= CB_CACHE_SIZE) {
            cpList = new ListView[cbIdList.length];
            cpListCap = new TextView[cbIdList.length];
            cpCursor = new Cursor[cbIdList.length];

            for (int i = 0; i < cbIdList.length; ++i) {
                int j = (cbIndex + i) % cbIdList.length;
                cbCursor.moveToPosition(j);

                cpList[i] = new ListView(this);
                cpListCap[i] = new TextView(this);
                cpListCap[i].setGravity(Gravity.CENTER_VERTICAL
                        | Gravity.CENTER_HORIZONTAL);
                cpListCap[i].setText(cbCursor.getString(1));
                cpList[i].addHeaderView(cpListCap[i], null, false);
                cpCursor[i] = mDbHelper.queryAllClips(cbIdList[j]);
                startManagingCursor(cpCursor[i]);
                cpList[i].setAdapter(new SimpleCursorAdapter(this,
                        R.layout.clip_entry, cpCursor[i],
                        new String[] { Clip.COL_DATA },
                        new int[] { R.id.clipEntryText }));

                registerForContextMenu(cpList[i]);

                vf.addView(cpList[i], new ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.FILL_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT));
            }
        } else { // number of clipboards > cache size
            cpList = new ListView[CB_CACHE_SIZE];
            cpListCap = new TextView[CB_CACHE_SIZE];
            cpCursor = new Cursor[CB_CACHE_SIZE];

            for (int i = 0; i < CB_CACHE_SIZE; ++i) {
                int j;
                if (i <= CB_CACHE_SIZE / 2) {
                    j = (cbIndex + i) % cbIdList.length;
                } else {
                    j = (cbIndex + i - CB_CACHE_SIZE + cbIdList.length)
                            % cbIdList.length;
                }
                //Log.i(TAG, "cbIndex: j = " + j);
                cbCursor.moveToPosition(j);

                cpList[i] = new ListView(this);
                cpListCap[i] = new TextView(this);
                cpListCap[i].setGravity(Gravity.CENTER_VERTICAL
                        | Gravity.CENTER_HORIZONTAL);
                cpListCap[i].setText(cbCursor.getString(1));
                cpList[i].addHeaderView(cpListCap[i], null, false);
                cpCursor[i] = mDbHelper.queryAllClips(cbIdList[j]);
                startManagingCursor(cpCursor[i]);
                cpList[i].setAdapter(new SimpleCursorAdapter(this,
                        R.layout.clip_entry, cpCursor[i],
                        new String[] { Clip.COL_DATA },
                        new int[] { R.id.clipEntryText }));

                registerForContextMenu(cpList[i]);

                vf.addView(cpList[i], new ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.FILL_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT));
            }
        }
    }
    
    /**
     * Flip clipboards rightward.
     * <p>
     * AppPrefs.operatingClipboardId is modified to currently displayed
     * clipboard. Update clipboard cache if needed.
     */
    private void clipboardFlipRight() {
        vf.showPrevious();
        Log.i(TAG, "in clipboardFlipRight(): vf.getDisplayedChild() = "
                + vf.getDisplayedChild());

        cbIndex = (cbIndex - 1 + cbIdList.length) % cbIdList.length;
        AppPrefs.operatingClipboardId = cbIdList[cbIndex];

        // if number of clipboards <= cache size, no need to cache
        if (cbIdList.length <= CB_CACHE_SIZE) {
            return;
        }

        int fp_i = (vf.getDisplayedChild() + CB_CACHE_SIZE / 2 + 1)
                % CB_CACHE_SIZE;
        int cb_i = (cbIndex - CB_CACHE_SIZE / 2 + cbIdList.length)
                % cbIdList.length;
        stopManagingCursor(cpCursor[fp_i]);
        cpCursor[fp_i].close();
        cpCursor[fp_i] = mDbHelper.queryAllClips(cbIdList[cb_i]);
        startManagingCursor(cpCursor[fp_i]);
        cbCursor.moveToPosition(cb_i);
        cpListCap[fp_i].setText(cbCursor.getString(1));
        cpList[fp_i].setAdapter(new SimpleCursorAdapter(this,
                R.layout.clip_entry, cpCursor[fp_i],
                new String[] { Clip.COL_DATA },
                new int[] { R.id.clipEntryText }));
    }

    /**
     * Flip clipboards leftward.
     * <p>
     * AppPrefs.operatingClipboardId is modified to currently displayed
     * clipboard. Update clipboard cache if needed.
     */
    private void clipboardFlipLeft() {
        vf.showNext();
        Log.i(TAG, "in clipboardFlipLeft(): vf.getDisplayedChild() = "
                + vf.getDisplayedChild());

        cbIndex = (cbIndex + 1) % cbIdList.length;
        AppPrefs.operatingClipboardId = cbIdList[cbIndex];

        // if number of clipboards <= cache size, no need to cache
        if (cbIdList.length <= CB_CACHE_SIZE) {
            return;
        }

        int fp_i = (vf.getDisplayedChild() + CB_CACHE_SIZE / 2) % CB_CACHE_SIZE;
        int cb_i = (cbIndex + CB_CACHE_SIZE / 2) % cbIdList.length;
        stopManagingCursor(cpCursor[fp_i]);
        cpCursor[fp_i].close();
        cpCursor[fp_i] = mDbHelper.queryAllClips(cbIdList[cb_i]);
        startManagingCursor(cpCursor[fp_i]);
        cbCursor.moveToPosition(cb_i);
        cpListCap[fp_i].setText(cbCursor.getString(1));
        cpList[fp_i].setAdapter(new SimpleCursorAdapter(this,
                R.layout.clip_entry, cpCursor[fp_i],
                new String[] { Clip.COL_DATA },
                new int[] { R.id.clipEntryText }));
    }

    /*
     * When ClipboardMonitor doesn't start on boot due to the reason like we
     * install new app after android phone boots, causing it won't receive boot
     * broadcast, this method makes sure ClipboardMonitor starts when MyClips
     * activity created.
     */
    private void startClipboardMonitor() {
        ComponentName service = startService(new Intent(this,
                ClipboardMonitor.class));
        if (service == null) {
            Log.e(TAG, "Can't start service "
                    + ClipboardMonitor.class.getName());
        }
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo i) {
        AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) i;
        selectedClipTitle = ((TextView) info.targetView).getText().toString();
        Log.i(TAG, "selectedClipTitle: " + selectedClipTitle);
        menu.setHeaderTitle(selectedClipTitle);
        menu.add(0, CNTX_INFO, 0, R.string.context_info);
        menu.add(0, CNTX_DELETE_CLIP, 0, R.string.context_delete_clip);
        menu.add(0, CNTX_EMAIL_CLIP, 0, R.string.context_email_clip);
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) item
                .getMenuInfo();
        Log.i(TAG, "listItemId: " + (info.position - 1));
        cpCursor[vf.getDisplayedChild()].moveToPosition(info.position - 1);
        clipCursor = cpCursor[vf.getDisplayedChild()];

        switch (item.getItemId()) {
            case CNTX_INFO:
                displayClipInfo(selectedClipTitle, clipCursor);
                return true;
            case CNTX_DELETE_CLIP:
                deleteClip(selectedClipTitle, clipCursor);
                return true;
            case CNTX_EMAIL_CLIP:
                emailClip(selectedClipTitle, clipCursor);
                return true;
        }
        return true;
    }

    public void displayClipInfo(String clipTitle, Cursor clipCursor) {
        AlertDialog.Builder alert = new AlertDialog.Builder(this);
        alert.setTitle("Info for \"" + clipTitle + "\"");

        LinearLayout ll = new LinearLayout(this);
        ll.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.FILL_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        layoutParams.setMargins(30, 20, 30, 0);
        final TextView tv = new TextView(this);
        ll.addView(tv, layoutParams);

        int type = clipCursor.getInt(1);
        if (type == 1) {
            tv.append("Type: " + getString(R.string.clip_text_type) + "\n");
        } else if (type == 2) {
            tv.append("Type: " + getString(R.string.clip_image_type) + "\n");
        }

        long time = clipCursor.getLong(3);
        SimpleDateFormat sdf = new SimpleDateFormat("EEE, d MMM yyyy HH:mm:ss");
        Date d = new Date(time);
        d.setTime(time);
        tv.append("Saved: " + sdf.format(d) + "\n");

        tv.append("In Clipboard: " + cpListCap[vf.getDisplayedChild()].getText() + "\n");

        String data = clipCursor.getString(2);
        tv.append("Content: \n" + data + "\n");

        alert.setView(ll);
        AlertDialog ad = alert.create();
        ad.show();
    }

    public void deleteClip(String clipTitle, final Cursor clipCursor) {
        AlertDialog.Builder alert = new AlertDialog.Builder(this);
        alert.setTitle("Delete \"" + clipTitle + "\"?");
        alert.setMessage("Are you sure you want to delete this clip?");

        DialogInterface.OnClickListener OKListener = new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                int clipID = clipCursor.getInt(0);
                mDbHelper.deleteClip(clipID);
                clipCursor.requery();
            }
        };

        DialogInterface.OnClickListener CancelListener = new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                // Do nothing; cancel...
            }
        };

        alert.setPositiveButton("Delete", OKListener);
        alert.setNegativeButton("Cancel", CancelListener);
        alert.show();
    }

    public void emailClip(String clipTitle, final Cursor clipCursor) {
        AlertDialog.Builder alert = new AlertDialog.Builder(this);
        alert.setTitle("Email \"" + clipTitle + "\"");
        alert.setMessage("Enter an email address");

        final EditText in = new EditText(this);
        alert.setView(in);

        DialogInterface.OnClickListener OKListener = new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                String allData = clipCursor.getString(2);

                String emailAddr = in.getText().toString();
                // Log.e(TAG, "emailAddr:: " + emailAddr);
                final Intent emailIntent = new Intent(
                        android.content.Intent.ACTION_SEND);
                emailIntent.setType("plain/text");
                emailIntent.putExtra(android.content.Intent.EXTRA_EMAIL,
                        new String[] { emailAddr });
                emailIntent.putExtra(android.content.Intent.EXTRA_SUBJECT,
                        "I want to share something with you");
                emailIntent
                        .putExtra(android.content.Intent.EXTRA_TEXT, allData);
                startActivity(Intent.createChooser(emailIntent, "Sending..."));
            }
        };

        DialogInterface.OnClickListener CancelListener = new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                // Do nothing; canceled...
            }
        };

        alert.setPositiveButton("Send", OKListener);
        alert.setNegativeButton("Cancel", CancelListener);
        alert.show();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        menu.add(0, OPT_NEW_CLIPBOARD, 0, R.string.create_new_clipboard);
        menu.add(0, OPT_EMAIL_CLIPBOARD, 0, R.string.email_clipboard);
        menu.add(0, OPT_LIST_ALL_CLIPBOARDS, 0, R.string.list_all_clipboards);
        menu.add(0, OPT_DELETE_CLIPBOARD, 0, R.string.delete_clipboard);
        return true;
    }

    @Override
    public boolean onMenuItemSelected(int featureId, MenuItem item) {
        switch (item.getItemId()) {
            case OPT_NEW_CLIPBOARD:
                createNewClipboard();
                return true;
            case OPT_EMAIL_CLIPBOARD:
                emailClipboard();
                return true;
            case OPT_LIST_ALL_CLIPBOARDS:
                listAllClipboards();
                return true;
            case OPT_DELETE_CLIPBOARD:
                deleteClipboard();
                return true;
        }
        return super.onMenuItemSelected(featureId, item);
    }

    public void createNewClipboard() {
        AlertDialog.Builder alert = new AlertDialog.Builder(this);
        alert.setMessage("Enter a name for new clipboard");

        final EditText in = new EditText(this);
        alert.setView(in);

        DialogInterface.OnClickListener OKListener = new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                String name = in.getText().toString();
                mDbHelper.insertClipboard(name);
                Cursor cursor = mDbHelper.queryClipboard(name);
                cursor.moveToNext();
                //AppPrefs.operatingClipboardId = cursor.getInt(0);
                updateClipboards();
            }
        };

        DialogInterface.OnClickListener CancelListener = new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                // Do nothing; cancel...
            }
        };

        alert.setPositiveButton("OK", OKListener);
        alert.setNegativeButton("Cancel", CancelListener);
        alert.show();
    }

    public void emailClipboard() {
        AlertDialog.Builder alert = new AlertDialog.Builder(this);
        alert.setMessage("Enter an email address");

        final EditText in = new EditText(this);
        alert.setView(in);

        DialogInterface.OnClickListener OKListener = new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                String allData = "";
                Cursor clipsCursor = mDbHelper.queryAllClips(new String[] {
                        Clip._ID, Clip.COL_DATA },
                        AppPrefs.operatingClipboardId);
                // Log.i(TAG, "clip count: " + clipsCursor.getCount());
                while (clipsCursor.moveToNext()) {
                    String clipData = clipsCursor.getString(1);
                    // Log.e(TAG, "clipData:: " + clipData);
                    allData += clipData + "\n";
                }
                // Log.e(TAG, "allData:: " + allData);

                String emailAddr = in.getText().toString();
                // Log.e(TAG, "emailAddr:: " + emailAddr);
                final Intent emailIntent = new Intent(
                        android.content.Intent.ACTION_SEND);
                emailIntent.setType("plain/text");
                emailIntent.putExtra(android.content.Intent.EXTRA_EMAIL,
                        new String[] { emailAddr });
                emailIntent.putExtra(android.content.Intent.EXTRA_SUBJECT,
                        "I want to share my clips with you");
                emailIntent
                        .putExtra(android.content.Intent.EXTRA_TEXT, allData);
                startActivity(Intent.createChooser(emailIntent, "Sending..."));

            }
        };

        DialogInterface.OnClickListener CancelListener = new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                // Do nothing; canceled...
            }
        };

        alert.setPositiveButton("Send", OKListener);
        alert.setNegativeButton("Cancel", CancelListener);
        alert.show();
    }

    public void listAllClipboards() {
        // TODO list all clilpboards in a ListView
    }

    public void deleteClipboard() {
        AlertDialog.Builder alert = new AlertDialog.Builder(this);
        alert.setTitle("Delete clipboard?");
        alert.setMessage("Are you sure you want to delete this clipboard with all clips in?");

        DialogInterface.OnClickListener OKListener = new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                mDbHelper.deleteClipboard(AppPrefs.operatingClipboardId);
                cbIndex = (cbIndex + 1) % cbIdList.length;
                AppPrefs.operatingClipboardId = cbIdList[cbIndex];
                updateClipboards();
            }
        };

        DialogInterface.OnClickListener CancelListener = new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                // Do nothing; cancel...
            }
        };

        alert.setPositiveButton("Delete", OKListener);
        alert.setNegativeButton("Cancel", CancelListener);
        alert.show();
    }

    @Override
    public boolean onTouch(View v, MotionEvent me) {
        // upXValue: X coordinate when the user releases the finger
        float upXValue = 0;
        float upYValue = 0;
        float ratio;
        double angle;

        switch (me.getAction()) {
            case MotionEvent.ACTION_DOWN: {
                downXValue = me.getX();
                downYValue = me.getY();
                // Log.i(TAG, "downX: " + downXValue);
                break;
            }
            case MotionEvent.ACTION_UP: {
                upXValue = me.getX();
                ratio = Math.abs(upXValue - downXValue)
                        / Math.abs(upYValue - downYValue);
                angle = Math.atan(ratio);
                // Log.i(TAG, "alpha: " + angle);
                // finger moving toward right
                if ((downXValue < upXValue) && (angle >= 0.6)) {
                    vf.setAnimation(AnimationUtils.loadAnimation(this,
                            R.anim.slide_right));
                    uiHandler.sendEmptyMessage(UiHandler.CLIPBOARD_FLIP_RIGHT);
                }
                // finger moving toward left
                if ((downXValue > upXValue) && (angle >= 0.6)) {
                    vf.setAnimation(AnimationUtils.loadAnimation(this,
                            R.anim.slide_left));
                    uiHandler.sendEmptyMessage(UiHandler.CLIPBOARD_FLIP_LEFT);
                }
                break;
            }
        }

        // if return false, these actions won't be recorded
        return true;
    }

    private class UiHandler implements Handler.Callback {
        public static final int CLIPBOARD_FLIP_RIGHT = 1;
        public static final int CLIPBOARD_FLIP_LEFT = 2;

        @Override
        public boolean handleMessage(Message msg) {
            switch (msg.what) {
                case CLIPBOARD_FLIP_RIGHT:
                    clipboardFlipRight();
                    break;
                case CLIPBOARD_FLIP_LEFT:
                    clipboardFlipLeft();
                    break;
                default:
                    Log.i(TAG, "unknown message");
            }
            return true;
        }
    }

    private class MotionDetector implements SensorEventListener {
        private static final float MIN_THRESHOLD = 100.0f;
        private static final float MAX_THRESHOLD = 200.0f;
        private static final long TIME_INTERVAL_NANO = 1000000000;
        private long startTime = -1;

        private int count = 0;
        private int mode = 0;

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {
            // Nothing to do
        }

        protected void doActionForShaking() {
            createNewClipboard();
        }

        @Override
        public void onSensorChanged(SensorEvent event) {
            if (startTime >= 0) {
                if ((event.timestamp - startTime) >= TIME_INTERVAL_NANO) {
                    startTime = -1;
                    mode = 0;
                }
            }
            float aX = event.values[0];
            float aY = event.values[1];
            float aZ = event.values[2];
            float a = aX * aX + aY * aY + aZ * aZ;
            // Log.i(TAG, "a = " + a);
            switch (mode) {
                case 0: // normal mode
                    if (a > MAX_THRESHOLD) {
                        startTime = event.timestamp;
                        mode = 1;
                        count = 0;
                        Log.i(TAG, "in MotionDetector: go to mode 1");
                    }
                    break;
                case 1:
                    if (a < MIN_THRESHOLD) {
                        ++count;
                        if (count == 2) {
                            mode = 0;
                            Log.i(TAG, "in MotionDetector: go back to mode 0");
                            Log.i(TAG, "detect shaking 2 times: eta = "
                                    + ((event.timestamp - startTime) / 1000000)
                                    + "ms");
                            doActionForShaking();
                        } else {
                            mode = 2;
                            Log.i(TAG, "in MotionDetector: go to mode 2");
                        }
                    }
                    break;
                case 2:
                    if (a > MAX_THRESHOLD) {
                        mode = 1;
                        Log.i(TAG, "in MotionDetector: go to mode 1");
                    }
                    break;
                default:
                    throw new RuntimeException(getClass().getName()
                            + ": unexpected mode");
            }

        }
    }
}
