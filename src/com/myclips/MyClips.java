package com.myclips;

import java.util.Arrays;

import com.myclips.db.Clip;
import com.myclips.db.ClipboardDbAdapter;
import com.myclips.prefs.AppPrefs;
import com.myclips.service.ClipboardMonitor;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
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
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;
import android.widget.ViewFlipper;

public class MyClips extends Activity implements OnTouchListener, LogTag {

	ViewFlipper vf = null;
	SharedPreferences mPrefs;
	private static final int OPT_NEW_CLIPBOARD = 0;
	private static final int OPT_EMAIL_CLIPBOARD = 1;
	private static final int CNTX_INFO = 2;
	private ClipboardDbAdapter mDbHelper;
	private float downXValue;
	private float downYValue;
	//private int operatingClipboardID = 1;
	String selectedClip;
	
	private int[] cbIdList;
	private int cbIndex;
	private int fpIndex;
	private ListView[] cpList;
	private TextView[] cpListCap;
	private Cursor[] cpCursor;
	/** Clipboard cursor iterating all clipboards */
	private Cursor cbCursor;
	private static final int CB_CACHE_SIZE = 3; // should be odd number
	private Handler uiHandler;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);
		
		mDbHelper = new ClipboardDbAdapter(this);
		mPrefs = getSharedPreferences(AppPrefs.NAME, 0);
		uiHandler = new Handler(new UiHandler());
		
		/* This method also initialize operatingClilpboardId */
        AppPrefs.operatingClipboardId = mPrefs.getInt(
                AppPrefs.KEY_OPERATING_CLIPBOARD,
                AppPrefs.DEF_OPERATING_CLIPBOARD);
        Log.i(TAG, "MyClips OnCreate(): set operatingClipboardId = "
                + AppPrefs.operatingClipboardId);
		
		vf = (ViewFlipper) findViewById(R.id.details);
		showClipboards();
		//getClipboards();
		
		ClipboardFlipper vf = (ClipboardFlipper) findViewById(R.id.details);
		vf.setInterceptTouchListener(this);
		vf.setOnTouchListener(this);
		
		startClipboardMonitor();
	}
	
	@Override
	protected void onDestroy() {
		super.onDestroy();
		mDbHelper.close();
	}
	
    @Override
    protected void onStart() {
        super.onStart();

    }

    @Override
	protected void onStop() {
		super.onStop();
		Log.i(TAG, "MyClips onStop(): write back operatingClipboardId = "
		        + AppPrefs.operatingClipboardId);
		SharedPreferences.Editor editor = mPrefs.edit();
		editor.putInt(AppPrefs.KEY_OPERATING_CLIPBOARD, AppPrefs.operatingClipboardId);
		editor.commit();
	}
	
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
	    Log.i(TAG, "cbIndex = " + cbIndex);
	    fpIndex = 0;
	    cpList = new ListView[CB_CACHE_SIZE];
	    cpListCap = new TextView[CB_CACHE_SIZE];
	    cpCursor = new Cursor[CB_CACHE_SIZE];
	    
	    for (int i = 0; i < CB_CACHE_SIZE; ++i) {
	        int j;
	        if (i <= CB_CACHE_SIZE / 2) {
	            j = (cbIndex + i) % cbIdList.length;;
	        } else {
	            j = (cbIndex + i - CB_CACHE_SIZE + cbIdList.length) % cbIdList.length;
	        }
	        Log.i(TAG, "cbIndex: j = " + j);
	        cbCursor.moveToPosition(j);
	        
	        cpList[i] = new ListView(this);
	        cpListCap[i] = new TextView(this);
	        cpListCap[i].setGravity(Gravity.CENTER_VERTICAL | Gravity.CENTER_HORIZONTAL);
	        cpListCap[i].setText(cbCursor.getString(1));
	        cpList[i].addHeaderView(cpListCap[i], null, false);
	        cpCursor[i] = mDbHelper.queryAllClips(cbIdList[j]);
	        startManagingCursor(cpCursor[i]);
            cpList[i].setAdapter(new SimpleCursorAdapter(this,
                    R.layout.clip_entry,
                    cpCursor[i],
                    new String[] { Clip.COL_DATA },
                    new int[] { R.id.clipEntryText }));
            
            vf.addView(cpList[i]);
	    }
	}
	
	// move right load left
	private void loadLeftClipboard() {
	    cbIndex = (cbIndex - 1 + cbIdList.length) % cbIdList.length;
	    fpIndex = (fpIndex - 1 + CB_CACHE_SIZE) % CB_CACHE_SIZE;
	    int fp_i = (fpIndex + CB_CACHE_SIZE / 2 + 1) % CB_CACHE_SIZE;
	    int cb_i = (cbIndex - CB_CACHE_SIZE / 2 + cbIdList.length) % cbIdList.length;
	    stopManagingCursor(cpCursor[fp_i]);
	    cpCursor[fp_i].close();
	    cpCursor[fp_i] = mDbHelper.queryAllClips(cbIdList[cb_i]);
	    startManagingCursor(cpCursor[fp_i]);
	    cbCursor.moveToPosition(cb_i);
	    cpListCap[fp_i].setText(cbCursor.getString(1));
	    cpList[fp_i].setAdapter(new SimpleCursorAdapter(this,
	            R.layout.clip_entry,
	            cpCursor[fp_i],
	            new String[] { Clip.COL_DATA },
	            new int[] { R.id.clipEntryText}));
	}
	
	// move left load right
	private void loadRightClipboard() {
	    cbIndex = (cbIndex + 1) % cbIdList.length;
	    fpIndex = (fpIndex + 1) % CB_CACHE_SIZE;
	    int fp_i = (fpIndex + CB_CACHE_SIZE / 2) % CB_CACHE_SIZE;
	    int cb_i = (cbIndex + CB_CACHE_SIZE / 2) % cbIdList.length;
	    stopManagingCursor(cpCursor[fp_i]);
        cpCursor[fp_i].close();
        cpCursor[fp_i] = mDbHelper.queryAllClips(cbIdList[cb_i]);
        startManagingCursor(cpCursor[fp_i]);
        cbCursor.moveToPosition(cb_i);
        cpListCap[fp_i].setText(cbCursor.getString(1));
        cpList[fp_i].setAdapter(new SimpleCursorAdapter(this,
                R.layout.clip_entry,
                cpCursor[fp_i],
                new String[] { Clip.COL_DATA },
                new int[] { R.id.clipEntryText}));
	}
	
	private void getClipboards() {
		String[] from = new String[] { Clip.COL_DATA };
		int[] to = new int[] { R.id.clipEntryText };
		
		vf = (ViewFlipper) findViewById(R.id.details);
		
		Cursor clipboardsCursor = mDbHelper.queryAllClipboards();
		
		while (clipboardsCursor.moveToNext()) {
		    int clipboardID = clipboardsCursor.getInt(0);
			
			Log.i(TAG, "operating clipboard: " + AppPrefs.DEF_OPERATING_CLIPBOARD);
		    Log.i(TAG, "clipboard name: " + clipboardsCursor.getString(1));
		    
		    Cursor clipsCursor = mDbHelper.queryAllClips(
		            new String[] { Clip._ID, Clip.COL_DATA }, clipboardID);
	        startManagingCursor(clipsCursor);
	        
		    ListView lv = new ListView(this);
		    TextView tv = new TextView(this);
		    tv.setGravity(Gravity.CENTER_VERTICAL | Gravity.CENTER_HORIZONTAL);
		    tv.setText(clipboardsCursor.getString(1));
		    lv.addHeaderView(tv, null, false);
		    
		    SimpleCursorAdapter adapter = new SimpleCursorAdapter(
		            this, R.layout.clip_entry,
		            mDbHelper.queryAllClips(clipboardID), from, to);
		    lv.setAdapter(adapter);
		    
			registerForContextMenu(lv);
			
	        vf.addView(lv, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.FILL_PARENT,
	                ViewGroup.LayoutParams.WRAP_CONTENT));
		}
	}

	/* When ClipboardMonitor doesn't start on boot due to the reason like we
	 * install new app after android phone boots, causing it won't receive
	 * boot broadcast, this method makes sure ClipboardMonitor starts when
	 * MyClips activity created. 
	 */
	private void startClipboardMonitor() {
        ComponentName service = startService(
                new Intent(this, ClipboardMonitor.class));
        if (service == null) {
            Log.e(TAG, "Can't start service "
                    + ClipboardMonitor.class.getName());
        }
	}

	@Override
	public void onCreateContextMenu(ContextMenu cm, View v, ContextMenuInfo i) {
		AdapterView.AdapterContextMenuInfo info = 
						(AdapterView.AdapterContextMenuInfo)i;
		selectedClip = ((TextView) info.targetView).getText().toString();
		long selectedID = v.getId();
		Log.i(TAG, "itemID: " + selectedID);
		cm.setHeaderTitle(selectedClip);
		cm.add(0, CNTX_INFO, 0, R.string.context_info);
	}
	
	@Override
	public boolean onContextItemSelected(MenuItem item) {
		AdapterView.AdapterContextMenuInfo info = 
			(AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
		int itemID = info.position;
		Log.i(TAG, "itemID: " + itemID);
		switch (item.getItemId()) {
		case CNTX_INFO:
			displayClipInfo(itemID);
			return true;
		}
		return true;
	}
	
	public void displayClipInfo(int itemID) {
		Cursor clipCursor = mDbHelper.queryClip(itemID);
		/*String type = clipCursor.getString(1);
		String data = clipCursor.getString(2);
		String time = clipCursor.getString(3);
		String clipboard = clipCursor.getString(4);
		Log.i(TAG, "type: "+type+"time:"+time+"clipboard:"+clipboard);*/
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
	    super.onCreateOptionsMenu(menu);
	    menu.add(0, OPT_NEW_CLIPBOARD, 0, R.string.create_new_clipboard);
	    menu.add(0, OPT_EMAIL_CLIPBOARD, 0, R.string.email_clipboard);
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
				SharedPreferences.Editor editor = mPrefs.edit();
				editor.putInt(AppPrefs.KEY_OPERATING_CLIPBOARD, cursor.getInt(0));
				editor.commit();
			}
		};
		
		DialogInterface.OnClickListener CancelListener = new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int whichButton) {  
				// Do nothing; canceled...  
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
				
				Cursor clipsCursor = mDbHelper.queryAllClips(
			            new String[] { Clip._ID, Clip.COL_DATA }, 
			            AppPrefs.operatingClipboardId);
				//Log.i(TAG, "clip count: " + clipsCursor.getCount());
				while (clipsCursor.moveToNext()) {
					String clipData = clipsCursor.getString(1);
					//Log.e(TAG, "clipData:: " + clipData);
					allData += clipData + "\n";
				}
				//Log.e(TAG, "allData:: " + allData);
				
				String emailAddr = in.getText().toString();
				//Log.e(TAG, "emailAddr:: " + emailAddr);
				
				final Intent emailIntent = new Intent(android.content.Intent.ACTION_SEND);
				emailIntent.setType("plain/text");
				emailIntent.putExtra(android.content.Intent.EXTRA_EMAIL, 
												new String[]{ emailAddr });
				emailIntent.putExtra(android.content.Intent.EXTRA_SUBJECT, 
						"I want to share my clips with you");
				emailIntent.putExtra(android.content.Intent.EXTRA_TEXT, 
						allData);
				startActivity(Intent.createChooser(emailIntent, "Sending.."));
				
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
    public boolean onTouch(View v, MotionEvent me) {
        // upXValue: X coordinate when the user releases the finger
        float upXValue = 0;
        float upYValue = 0;
        float ratio;
        double angle;
        int numOfClipboards = 0;

        Cursor clipboardsCursor = mDbHelper.queryAllClipboards();
        numOfClipboards = clipboardsCursor.getCount();

        switch (me.getAction()) {
            case MotionEvent.ACTION_DOWN: {
                downXValue = me.getX();
                downYValue = me.getY();
                Log.i(TAG, "downX: " + downXValue);
                break;
            }
            case MotionEvent.ACTION_UP: {
                upXValue = me.getX();
                // finger moving toward left
                ratio = Math.abs(upXValue - downXValue)
                        / Math.abs(upYValue - downYValue);
                angle = Math.atan(ratio);
                Log.i(TAG, "alpha: " + angle);
                if ((downXValue < upXValue) && (angle >= 0.6)) {
                    vf.setAnimation(AnimationUtils.loadAnimation(this,
                            R.anim.slide_right));
                    uiHandler.sendEmptyMessage(UiHandler.LOAD_LEFT_CLIPBOARD);
                    vf.showPrevious();
                    /*
                    if (operatingClipboardID == 1)
                        operatingClipboardID = numOfClipboards;
                    else if (operatingClipboardID > 1)
                        operatingClipboardID--;
                    Log.e(TAG, "operatingClipboardID:: " + operatingClipboardID);
                    */
                }
                // finger moving toward right
                if ((downXValue > upXValue) && (angle >= 0.6)) {
                    vf.setAnimation(AnimationUtils.loadAnimation(this,
                            R.anim.slide_left));
                    uiHandler.sendEmptyMessage(UiHandler.LOAD_RIGHT_CLIPBOARD);
                    vf.showNext();
                    /*
                    if (operatingClipboardID == numOfClipboards)
                        operatingClipboardID = 1;
                    else if (operatingClipboardID < numOfClipboards)
                        operatingClipboardID++;
                    Log.e(TAG, "operatingClipboardID:: " + operatingClipboardID);
                    */
                }
                break;
            }
        }

        // if return false, these actions won't be recorded
        return true;
    }
	
    private class UiHandler implements Handler.Callback {
        static final int LOAD_LEFT_CLIPBOARD = 1;
        static final int LOAD_RIGHT_CLIPBOARD = 2;

        @Override
        public boolean handleMessage(Message msg) {
            switch (msg.what) {
                case LOAD_LEFT_CLIPBOARD:
                    loadLeftClipboard();
                    break;
                case LOAD_RIGHT_CLIPBOARD:
                    loadRightClipboard();
                    break;
                default:
                    Log.i(TAG, "unknown message");
            }
            return true;
        }
    }
}
