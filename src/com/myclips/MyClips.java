package com.myclips;

import com.myclips.db.Clip;
import com.myclips.db.ClipboardDbAdapter;
import com.myclips.prefs.AppPrefs;
import com.myclips.service.ClipboardMonitor;

import android.app.Activity;
import android.app.ListActivity;
import android.content.ComponentName;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnTouchListener;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.ResourceCursorAdapter;
import android.widget.SimpleCursorAdapter;
import android.widget.ViewFlipper;

//public class MyClips extends Activity implements OnTouchListener, LogTag {
public class MyClips extends ListActivity implements LogTag {
	
	private ClipboardDbAdapter mDbHelper;
	private float downXValue;	
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		getListView().setOnCreateContextMenuListener(this);
		mDbHelper = new ClipboardDbAdapter(this);
		
		getClips();
		
		//LinearLayout ll = (LinearLayout) findViewById(R.id.clipList);
		//ll.setOnTouchListener((OnTouchListener) this);
		startClipboardMonitor();
		
	}
	
	private void getClips() {
		Cursor clipsCursor = mDbHelper.queryAllClips(new String[] { Clip._ID, Clip.COL_DATA }, 1);
		startManagingCursor(clipsCursor);
		
		Log.i(TAG, "clip count = " + clipsCursor.getCount());
		
		String[] from = new String[] { Clip.COL_DATA };
		int[] to = new int[] { R.id.clipEntryText };
		
		SimpleCursorAdapter mAdapter = 
			new SimpleCursorAdapter(this, R.layout.clip_entry, clipsCursor, from, to);
		

		Log.i(TAG, "mAdapter = " + mAdapter);
		
		setListAdapter(mAdapter);
		
		//ViewFlipper vf = (ViewFlipper) findViewById(R.id.details);		
		//vf.addView(lv, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,
		//		ViewGroup.LayoutParams.WRAP_CONTENT));
		
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
	    menu.add(0, INSERT_ID, 0, R.string.create_new_clipboard);
	    inflater.inflate(R.menu.menu, menu);
	    return true;
	}

	/*
	@Override
	public boolean onTouch(View v, MotionEvent me) {
		// upXValue: X coordinate when the user releases the finger
		float upXValue = 0;
		
		// reference to the ViewFlipper
		ViewFlipper vf = (ViewFlipper) findViewById(R.id.details);
		
		// TODO: Here we have to replace this loop elements with the clipboards
		for (String item : items) {
			Button btn = new Button(this);
			btn.setText(item);
			vf.addView(btn, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,
					ViewGroup.LayoutParams.WRAP_CONTENT));
		}
		
		switch (me.getAction()) {
			case MotionEvent.ACTION_DOWN: {
				downXValue = me.getX();
				break;
			}
			case MotionEvent.ACTION_UP: {
				upXValue = me.getX();
				// finger moving toward left
				if (downXValue < upXValue) {
					// set animation
					vf.setAnimation(AnimationUtils.loadAnimation(this, R.anim.push_left_out));				
					// flip
					vf.showPrevious();
				}
				// finger moving toward right
				if (downXValue > upXValue) {
					//ViewFlipper vf = (ViewFlipper) findViewById(R.id.details);
					vf.setAnimation(AnimationUtils.loadAnimation(this, R.anim.push_left_in));
					vf.showNext();
				}
				break;
			}
		}

		// if return false, these actions won't be recorded
		return true;
	}
	*/
	
}