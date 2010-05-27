package com.myclips.db;

import android.provider.BaseColumns;

/** Define constants like columns for clipboards table */
public class Clipboard implements BaseColumns {
    
    // This class shouldn't be instantiated
    private Clipboard() {}
    
    /**
     * The name of clipboard
     * <p>TYPE: TEXT</p>
     */
    public static final String COL_NAME = "name";
}
