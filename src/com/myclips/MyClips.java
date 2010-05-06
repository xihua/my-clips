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
	private ListView mClipList;
	private boolean mShowInvisible;
	private CheckBox mShowInvisibleControl;
	
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
    	super.onCreate(savedInstanceState);
    	setContentView(R.layout.clipboard);
    	
    	// Obtain handles to UI objects
    	mClipList = (ListView) findViewById(R.id.clipList);
    	mShowInvisibleControl = (CheckBox) findViewById(R.id.showInvisible);
    	
    	mShowInvisible = false;
        mShowInvisibleControl.setChecked(mShowInvisible);
        
        // Register handler for UI elements
        mShowInvisibleControl.setOnCheckedChangeListener(new OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                Log.d(TAG, "mShowInvisibleControl changed: " + isChecked);
                mShowInvisible = isChecked;
                populateClipList();
            }
        });
    	
        // Populate clip list
        populateClipList();
        
    }
    
    private void populateClipList() {
        // Build adapter with clip entries
        Cursor cursor = getClips();
        String[] fields = new String[] { ClipsContract.Data.DISPLAY_NAME };
        SimpleCursorAdapter adapter = new SimpleCursorAdapter(this, R.layout.clip_entry, cursor,
                fields, new int[] {R.id.clipEntryText});
        mClipList.setAdapter(adapter);
    }
    
    private Cursor getClips() {
        // Run query
        Uri uri = ClipsContract.Clips.CONTENT_URI;
        String[] projection = new String[] {
        	ContactsContract.Clips._ID,
        	ContactsContract.Clips.DISPLAY_NAME
        };
        String selection = ContactsContract.Clips.IN_VISIBLE_GROUP + " = '" +
                (mShowInvisible ? "0" : "1") + "'";
        String[] selectionArgs = null;
        String sortOrder = ClipsContract.Clips.DISPLAY_NAME + " COLLATE LOCALIZED ASC";

        return managedQuery(uri, projection, selection, selectionArgs, sortOrder);
    }
    
}