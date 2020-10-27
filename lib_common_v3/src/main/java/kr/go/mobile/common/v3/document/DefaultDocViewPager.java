package kr.go.mobile.common.v3.document;

import android.content.Context;
import android.util.AttributeSet;

import androidx.annotation.NonNull;
import androidx.viewpager.widget.ViewPager;

public class DefaultDocViewPager extends ViewPager {
    public DefaultDocViewPager(@NonNull Context context) {
        super(context);
    }

    public DefaultDocViewPager(@NonNull Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public void setSwipeable(boolean b) {
    }
}
