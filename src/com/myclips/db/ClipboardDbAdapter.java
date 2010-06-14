package com.myclips.db;

import com.myclips.LogTag;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

/**
 * Adapter class used to manipulate clipboard database.
 * <p>
 * The database contains two tables:
 * <pre>
 * Clipboards TABLE:
 * +-----+------+
 * | _ID | NAME |
 * +-----+------+
 * Clips TABLE:
 * +-----+------+------+------+-----------+
 * | _ID | TYPE | DATA | TIME | CLIPBOARD |
 * +-----+------+------+------+-----------+
 * </pre>
 */
public class ClipboardDbAdapter implements LogTag {
    // private static final String TAG = "ClipboardDbAdapter";

    private static final String DATABASE_NAME = "clipboard.db";
    private static final int DATABASE_VERSION = 2;
    private static final String CLIPBOARDS_TABLE_NAME = "clipboards";
    private static final String CLIPS_TABLE_NAME = "clips";
    private static final String[] CLIPBOARDS_PROJECTION = new String[] {
        Clipboard._ID, Clipboard.COL_NAME
    };
    private static final String[] CLIPS_PROJECTION = new String[] {
        Clip._ID, Clip.COL_TYPE, Clip.COL_DATA, Clip.COL_TIME,
        Clip.COL_CLIPBOARD
    };


    /** Convenient class for handling database creation and upgrade */
    private static class DatabaseHelper extends SQLiteOpenHelper {

        public DatabaseHelper(Context context) {
            super(context, DATABASE_NAME, null, DATABASE_VERSION);
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            db.execSQL("CREATE TABLE " + CLIPBOARDS_TABLE_NAME + " ("
                    + Clipboard._ID + " INTEGER PRIMARY KEY,"
                    + Clipboard.COL_NAME + " TEXT"
                    + ");");
            Log.i(TAG, "create sqlite database table: " + CLIPBOARDS_TABLE_NAME);
            db.execSQL("CREATE TABLE " + CLIPS_TABLE_NAME + " ("
                    + Clip._ID + " INTEGER PRIMARY KEY,"
                    + Clip.COL_TYPE + " INTEGER,"
                    + Clip.COL_DATA + " TEXT,"
                    + Clip.COL_TIME + " INTEGER,"
                    + Clip.COL_CLIPBOARD + " INTEGER"
                    + ");");
            Log.i(TAG, "create sqlite database table: " + CLIPS_TABLE_NAME);
            db.execSQL("INSERT INTO " + CLIPBOARDS_TABLE_NAME + " ("
                    + Clipboard.COL_NAME + ") VALUES ('default');");
            Log.i(TAG, "insert default clipboard");
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            Log.w(TAG, "Upgrading database from version " + oldVersion + " to "
                    + newVersion + ", which will destroy all old data");
            db.execSQL("DROP TABLE IF EXISTS " + CLIPBOARDS_TABLE_NAME);
            db.execSQL("DROP TABLE IF EXISTS " + CLIPS_TABLE_NAME);
            onCreate(db);
        }
    }

    private DatabaseHelper mDbHelper;

    /**
     * Get an adapter to manipulate the clipboard database. The clipboard will
     * be created if it doesn't exist yet.
     *
     * @param context  Used to open or create database
     * @throws SQLException  Thrown when opening or creating database fails
     */
    public ClipboardDbAdapter(Context context) throws SQLException {
        this.mDbHelper = new DatabaseHelper(context);
    }

    /**
     * Insert a new clipboard
     *
     * @param clipboardName  Name of clipboard
     */
    public void insertClipboard(String clipboardName) {
        ContentValues values = new ContentValues();
        values.put(Clipboard.COL_NAME, clipboardName);
        long rowId = mDbHelper.getWritableDatabase().insert(
                CLIPBOARDS_TABLE_NAME, Clipboard.COL_NAME, values);
        if (rowId < 0) { // insert failed
            Log.e(TAG, "inserting new clipboard failed");
            return ;
        }
    }

    /**
     * Update information of clipboard by clipboard id
     *
     * @param clipboardId  Id of clipboard
     * @param clipboardName  New name of clipboard; if null, unmodified
     */
    public void updateClipboard(int clipboardId, String clipboardName) {
        ContentValues values = new ContentValues();
        if (clipboardName != null) {
            values.put(Clipboard.COL_NAME, clipboardName);
        }
        if (values.size() <= 0) {
            return ;
        }
        mDbHelper.getWritableDatabase().update(CLIPBOARDS_TABLE_NAME, values,
                Clipboard._ID + "=" + clipboardId, null);
    }

    /**
     * Delete a clipboard by clipboard id
     * <p>
     * Clips in the clipboard are also deleted.
     *
     * @param clipboardId  Id of clipboard to be deleted
     */
    public void deleteClipboard(int clipboardId) {
        SQLiteDatabase db = mDbHelper.getWritableDatabase();
        db.delete(CLIPS_TABLE_NAME, Clip.COL_CLIPBOARD + "=" + clipboardId, null);
        db.delete(CLIPBOARDS_TABLE_NAME, Clipboard._ID + "=" + clipboardId, null);
    }

    /**
     * Query all columns of all clipboards
     *
     * @return {@link Cursor} object, which is positioned before the first entry
     */
    public Cursor queryAllClipboards() {
        return mDbHelper.getReadableDatabase().query(CLIPBOARDS_TABLE_NAME,
                CLIPBOARDS_PROJECTION, null, null, null, null, null);
    }
    
    /**
     * Query the clipboard with given id
     *
     * @return {@link Cursor} object, which is positioned to the entry
     */
    public Cursor queryClipboard(String clipboardName) {
        return mDbHelper.getReadableDatabase().query(CLIPBOARDS_TABLE_NAME,
                CLIPBOARDS_PROJECTION, Clipboard.COL_NAME + "='" + clipboardName + "'",
                null, null, null, null);
    }
    

    /**
     * Insert a new clip into specified clipboard. The TIME feild of this clip
     * is current time of calling this method.
     *
     * @param clipType  Type of clip
     * @param clipData  Data of clip
     * @param clipboardId  Id of clipboard containing this clip
     */
    public void insertClip(int clipType, String clipData, int clipboardId) {
        ContentValues values = new ContentValues();
        values.put(Clip.COL_TYPE, clipType);
        values.put(Clip.COL_DATA, clipData);
        values.put(Clip.COL_CLIPBOARD, clipboardId);
        values.put(Clip.COL_TIME, System.currentTimeMillis());
        long rowId = mDbHelper.getWritableDatabase().insert(CLIPS_TABLE_NAME,
                Clip.COL_DATA, values);
        if (rowId < 0) {
            Log.e(TAG, "add clip failed");
            return ;
        }
    }

    /**
     * Update information of a clip by clip id
     *
     * @param clipId  Id of clip to be updated
     * @param clipType  New type of clip; if negative, unmodified
     * @param clipData  New data of clip; if null, unmodified
     * @param clipboardId  New clipboard id; if negative, unmodified
     */
    public void updateClip(int clipId, int clipType, String clipData,
            int clipboardId) {
        SQLiteDatabase db = mDbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        if (clipType >= 0) {
            values.put(Clip.COL_TYPE, clipType);
        }
        if (clipData != null) {
            values.put(Clip.COL_DATA, clipData);
        }
        if (clipboardId >= 0) {
            values.put(Clip.COL_CLIPBOARD, clipboardId);
        }
        if (values.size() <= 0) {
            return ;
        }
        db.update(CLIPS_TABLE_NAME, values, Clip._ID + "=" + clipId, null);
    }

    /**
     * Delete a clip by clip id
     *
     * @param clipId  Id of clip
     */
    public void deleteClip(int clipId) {
        mDbHelper.getWritableDatabase().delete(CLIPS_TABLE_NAME,
                Clip._ID + "=" + clipId, null);
    }
    
    /**
     * Query a clip by clip id
     *
     * @param clipId  Id of clip
     */
    public Cursor queryClip(int clipId) {
    	return queryClips(CLIPS_PROJECTION, Clip._ID + "=" + clipId, null, null);
    }

    /**
     * Query given columns of specified clips in specified order
     *
     * @param columns  Target columns
     * @param selection  Clip filter
     * @param selectionArgs  Clip filter arguments, if any
     * @param orderBy  Output order
     * @return {@link Cursor} object, which is positioned before first entry
     */
    public Cursor queryClips(String[] columns, String selection,
            String[] selectionArgs, String orderBy) {
        return mDbHelper.getReadableDatabase().query(CLIPS_TABLE_NAME, columns,
                selection, selectionArgs, null, null, orderBy);
    }

    /**
     * Query given columns of clips in the given clipboard in specified order
     *
     * @param columns  Target columns
     * @param clipboardId  Id of clipboard
     * @param orderBy  Order
     * @return {@link Cursor} object, which is positioned before first entry
     */
    public Cursor queryClips(String[] columns, int clipboardId,
            String orderBy) {
        return queryClips(columns, Clip.COL_CLIPBOARD + "=" + clipboardId,
                null, orderBy);
    }

    /**
     * Query given columns of clips in the given clipboard
     *
     * @param columns  Target columns
     * @param clipboardId  Id of clipboard
     * @return {@link Cursor} object, which is positioned before first entry
     */
    public Cursor queryAllClips(String[] columns, int clipboardId) {
        return queryClips(columns, clipboardId, Clip.DEFAULT_SORT_ORDER);
    }

    /**
     * Query all columns of clips in the given clipboard
     *
     * @param clipboardId  Id of clipboard
     * @return {@link Cursor} object, which is positioned before first entry
     */
    public Cursor queryAllClips(int clipboardId) {
        return queryAllClips(CLIPS_PROJECTION, clipboardId);
    }

    /**
     * Query given columns of all clips in all clipboards
     *
     * @param columns  Target columns
     * @return {@link Cursor} object, which is positioned before first entry
     */
    public Cursor queryAllClips(String[] columns) {
        return queryClips(columns, null, null, Clip.DEFAULT_SORT_ORDER);
    }

    /**
     * Query all columns of all clips in all clipboards
     *
     * @return {@link Cursor} object, which is positioned before first entry
     */
    public Cursor queryAllClips() {
        return queryAllClips(CLIPS_PROJECTION);
    }

    /**
     * Close open database
     */
    public void close() {
        mDbHelper.close();
    }
}
