package com.myclips;

import com.myclips.service.ClipboardMonitor;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnTouchListener;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ViewFlipper;

public class MyClips extends Activity implements OnTouchListener, LogTag {
    //private final static String TAG = "MyClips"; 

	// TODO: Clean this Test Case.
	static String[] items = { "lorem", "ipsum", "dolor", "sit", "amet", "consectetuer",
			"adipiscing", "elit", "morbi", "vel", "ligula", "vitae", "arcu", "aliquet", "mollis",
			"etiam", "vel", "erat", "placerat", "ante", "porttitor", "sodales", "pellentesque",
			"augue", "purus" };

	// private ClipboardDbManager mDbHelper;
	private float downXValue;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);
		// mDbHelper = new ClipboardDbAdapter(this);
		// mDbHelper.open();
		// fillData();
		LinearLayout ll = (LinearLayout) findViewById(R.id.layout_main);
		ll.setOnTouchListener((OnTouchListener) this);
		startClipboardMonitor();
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

}