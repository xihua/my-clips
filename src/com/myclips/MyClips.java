package com.myclips;

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
import android.util.Log;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnTouchListener;
import android.view.animation.AnimationUtils;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;
import android.widget.ViewFlipper;


public class MyClips extends Activity implements OnTouchListener, LogTag {

	ViewFlipper vf = null;
	SharedPreferences mPrefs;
	private static final int OPT_NEW_CLIPBOARD = 0;
	private ClipboardDbAdapter mDbHelper;
	private float downXValue;	
	

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		setContentView(R.layout.main);
		//getListView().setOnCreateContextMenuListener(this);
		
		mDbHelper = new ClipboardDbAdapter(this);
		mPrefs = getSharedPreferences(AppPrefs.NAME, 0);
		
		getClipboards();
		
		LinearLayout ll = (LinearLayout) findViewById(R.id.layout_main);
		ll.setOnTouchListener((OnTouchListener) this);
		
		startClipboardMonitor();
	}
	
	@Override
	protected void onDestroy() {
		super.onDestroy();
		mDbHelper.close();
	}
	
	private void getClipboards() {
		//Log.i(TAG, "clip count = " + clipsCursor.getCount());
		
		String[] from = new String[] { Clip.COL_DATA };
		int[] to = new int[] { R.id.clipEntryText };
		
		vf = (ViewFlipper) findViewById(R.id.details);
		
		Cursor clipboardsCursor = mDbHelper.queryAllClipboards();
		
		while (clipboardsCursor.moveToNext()) {
		    int clipboardId = clipboardsCursor.getInt(0);
		    Log.i(TAG, "clipboard name: " + clipboardsCursor.getString(1));
		    Cursor clipsCursor = mDbHelper.queryAllClips(
		            new String[] { Clip._ID, Clip.COL_DATA }, clipboardId);
	        startManagingCursor(clipsCursor);
	        
		    ListView lv = new ListView(this);
		    TextView tv = new TextView(this);
		    tv.setGravity(Gravity.CENTER_VERTICAL | Gravity.CENTER_HORIZONTAL);
		    tv.setText(clipboardsCursor.getString(1));
		    lv.addHeaderView(tv, null, false);
		    
		    SimpleCursorAdapter adapter = new SimpleCursorAdapter(
		            this, R.layout.clip_entry,
		            mDbHelper.queryAllClips(clipboardId), from, to);
		    lv.setAdapter(adapter);
		    
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
	public boolean onCreateOptionsMenu(Menu menu) {
	    super.onCreateOptionsMenu(menu);
	    menu.add(0, OPT_NEW_CLIPBOARD, 0, R.string.create_new_clipboard);
	    return true;
	}
	
	@Override
	public boolean onMenuItemSelected(int featureId, MenuItem item) {
		switch (item.getItemId()) {
		case OPT_NEW_CLIPBOARD:
			createNewClipboard();
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
	

	@Override
	public boolean onTouch(View v, MotionEvent me) {
		// upXValue: X coordinate when the user releases the finger
		float upXValue = 0;
		
		switch (me.getAction()) {
			case MotionEvent.ACTION_DOWN: {
				downXValue = me.getX();
				break;
			}
			case MotionEvent.ACTION_UP: {
				upXValue = me.getX();
				// finger moving toward left
				if (downXValue < upXValue) {
					vf.setAnimation(AnimationUtils.loadAnimation(this, R.anim.slide_right));				
					vf.showPrevious();
				}
				// finger moving toward right
				if (downXValue > upXValue) {
					vf.setAnimation(AnimationUtils.loadAnimation(this, R.anim.slide_left));
					vf.showNext();
				}
				break;
			}
		}

		// if return false, these actions won't be recorded
		return true;
	}
	
}