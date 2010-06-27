package com.myclips;

import com.myclips.db.Clipboard;
import com.myclips.db.ClipboardDbAdapter;
import com.myclips.prefs.AppPrefs;

import android.app.ListActivity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.AdapterView.OnItemClickListener;

public class ClipboardList extends ListActivity implements LogTag {
	
	private ClipboardDbAdapter mDbHelper;
	protected final int SUCCESS_RETURN_CODE = 1;
	private Cursor allClipboardsCursor;
	private SharedPreferences mPrefs;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //setContentView(R.layout.custom_list_activity_view);
        
        mDbHelper = new ClipboardDbAdapter(this);
        allClipboardsCursor = mDbHelper.queryAllClipboards();
        startManagingCursor(allClipboardsCursor);
        mPrefs = getSharedPreferences(AppPrefs.NAME, 0);
        
        ListAdapter adapter = new SimpleCursorAdapter(
                this,
                R.layout.clip_entry,
				allClipboardsCursor,
                new String[] { Clipboard.COL_NAME },
                new int[] { R.id.clipEntryText } );

        setListAdapter(adapter);
        
        ListView lv = getListView();
        
        lv.setOnItemClickListener(new OnItemClickListener() {
            public void onItemClick(AdapterView<?> parent, View view,
                int position, long id) {
            	Log.i(TAG, "ClipboardList: position = " + position);
            	allClipboardsCursor.moveToPosition(position);
            	AppPrefs.operatingClipboardId = allClipboardsCursor.getInt(0);
            	Log.i(TAG, "ClipboardList: allClipboardsCursor.getInt(0)=" + allClipboardsCursor.getInt(0));
            	setResult(RESULT_OK);
            	finish();
            }
        });
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		mDbHelper.close();
	} 
	
    @Override
    protected void onPause() {
        super.onPause();
        Log.i(TAG, "ClipboardList onPause(): write back operatingClipboardId = "
                + AppPrefs.operatingClipboardId);
        SharedPreferences.Editor editor = mPrefs.edit();
        editor.putInt(AppPrefs.KEY_OPERATING_CLIPBOARD,
                AppPrefs.operatingClipboardId);
        editor.commit();
    }
}
