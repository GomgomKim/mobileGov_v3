package com.infrawaretech.docviewer.ui;

import android.content.Context;
import androidx.viewpager.widget.ViewPager;
import android.util.AttributeSet;
import android.view.MotionEvent;

public class DocPager extends ViewPager {

	public DocPager(Context context) {
		super(context);
	}
	
	public DocPager(Context context, AttributeSet attrs) {
		super(context, attrs);
	}
	
	private boolean mSwipeable = true;
	
	public void setSwipeable(boolean swipeable) {
		this.mSwipeable = swipeable;
	}
	
	@Override
	public boolean onTouchEvent(MotionEvent event) {
		if (this.mSwipeable) {
			try {
				return super.onTouchEvent(event);
			} catch (Exception e) {
				return false;
			}

		}
		return false;

	}

	@Override
	public boolean onInterceptTouchEvent(MotionEvent event) {
		if (this.mSwipeable) {
			try {
				return super.onInterceptTouchEvent(event);
			} catch (Exception e) {
				return false;
			}
		}
		return false;
	}
}
