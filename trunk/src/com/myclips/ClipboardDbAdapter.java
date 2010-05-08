package com.myclips;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class ClipboardDbAdapter {
	
	public static final String KEY_ROWID = "_id";
	public static final String KEY_TITLE = "title";
	public static final String KEY_CLIP_LIST = "clip_list";
	
	private static final String TAG = "ClipboardDbAdapter";
    private DatabaseHelper mDbHelper;
    private SQLiteDatabase mDb;
    
    /** Database creation sql statement */
    // TODO: Define the right db structure for our clipboards table + 
    // clips table approach ;)
    // CLipboards table must at least have these: id + title + clip_list
    // Clips table: id + title(?) + content + ...
    
    private final Context mCtx;
    
    // TODO: DatabaseHelper class goes here...
    public static class DatabaseHelper extends SQLiteOpenHelper {
    	// TODO: ...
    }
    
    /**
     * Constructor - takes the context to allow the database to be
     * opened/created
     * 
     * @param ctx the Context within which to work
     */
    public ClipboardDbAdapter(Context ctx) {
    	this.mCtx = ctx;
    }
    
    /**
     * Open the clipboards database. If it cannot be opened, try to create a new
     * instance of the database. If it cannot be created, throw an exception to
     * signal the failure
     * 
     * @return this (self reference, allowing this to be chained in an
     *         initialization call)
     * @throws SQLException if the database could be neither opened or created
     */
    public ClipboardDbADapter() throws SQLException {
    	mDbHelper = new DatabaseHelper(mCtx);
    	mDb = mDbHelper.getWriteableDatabase();
    	return this;
    }
    
    public void close() {
    	mDbHelper.close();
    }
    
    /**
     * Return a Cursor over the list of all clipboards in the database
     * 
     * @return Cursor over all clipboards
     */
    public Cursor fetchAllClipboards() {
    	
    	// TODO: Update later according to the db structure
    	return mDb.query(DATABASE_TABLE, new String[] {KEY_ROWID, KEY_TITLE,
                KEY_CLIP_LIST}, null, null, null, null, null);
    }
    
    /**
     * Return a Cursor over the list of all clips in the database
     * 
     * @return Cursor over all clips
     */
    public Cursor fetchAllNotes() {
    	
    	// TODO: Update later according to the db structure
    	return mDb.query(DATABASE_TABLE, new String[] {KEY_ROWID, KEY_TITLE,
                KEY_CLIP_LIST}, null, null, null, null, null);
    }
    
}
