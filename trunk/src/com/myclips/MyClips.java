package com.myclips;

import android.app.Activity;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.CompoundButton.OnCheckedChangeListener;

public class MyClips extends Activity {
	
	public static final String TAG = "ContactManager";
	private ClipboardDbManager mDbHelper;
	
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
    	super.onCreate(savedInstanceState);
    	setContentView(R.layout.clipboard_list);
    	mDbHelper = new ClipboardDbAdapter(this);
    	mDbHelper.open();
    	fillData();
    }
    
    public void fillData() {
    	Cursor c = mDbHelper.fetchAllCliboards();
    	startManagingCursor(c);
    	
    	// TODO: Here we have to learn how to output all clipboards as we can
    	// slide left/right on touch screen to change the clipboard
    	// Now, this just returns a list view
    	String[] from = new String[] { ClipboardDbAdapter.KEY_TITLE };
    	int[] to = new int[] { /* TODO: R.id.text1 */};
    	
    	// Now create an array adapter and set it to display using our row
        SimpleCursorAdapter clipboards =
            new SimpleCursorAdapter(this, R.layout.clipboards_row, c, from, to);
        setListAdapter(clipboards);
    }

    
}