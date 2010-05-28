package com.myclips.prefs;

import com.myclips.LogTag;

/**
 * Define constants like name or keys for application preferences 
 */
public class AppPrefs implements LogTag {
    
    private AppPrefs() {}
    
    /** Name of shared preference */
    public static String NAME = "AppPrefs";
    
    /**
     * Time interval of monitor checking in milliseconds
     * <p>TYPE: int</p>
     */
    public static String KEY_MONITOR_INTERVAL = "monitor.interval";
    
    /**
     * Id of current operating clipboard
     * <p>TYPE: int</p>
     */
    public static String KEY_OPERATING_CLIPBOARD = "clipboard";

    public static int DEF_MONITOR_INTERVAL = 3000;

    public static int DEF_OPERATING_CLIPBOARD = 1; // 1 = default clipboard
}
