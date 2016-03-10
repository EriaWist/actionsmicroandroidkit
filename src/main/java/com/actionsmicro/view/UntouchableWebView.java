package com.actionsmicro.view;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.webkit.WebView;

public class UntouchableWebView extends WebView {
    public UntouchableWebView(Context context) {
        super(context);
    }
    public UntouchableWebView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public UntouchableWebView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        return false;
    }

    @Override
    protected void finalize () throws Throwable {
        super.finalize();
    }
}
