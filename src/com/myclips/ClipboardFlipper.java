package com.myclips;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ViewFlipper;

/**
 * This class modifies original {@link ViewFlipper} to support performing
 * OnTouchListener when intercepting MotionEvent.  
 */
public class ClipboardFlipper extends ViewFlipper {

    private View.OnTouchListener mInterceptTouchListenter = null;
    
    public ClipboardFlipper(Context context) {
        super(context);
    }

    public ClipboardFlipper(Context context, AttributeSet attrs) {
        super(context, attrs);

    }

    public void setInterceptTouchListener(View.OnTouchListener l) {
        mInterceptTouchListenter = l;
    }
    
    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        if (mInterceptTouchListenter != null) {
            mInterceptTouchListenter.onTouch(null, ev);
        }
        return super.onInterceptTouchEvent(ev);
    }
}
