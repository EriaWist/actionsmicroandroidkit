package com.actionsmicro.analytics.googleanalytics.httpclient;

import android.content.Context;
import android.content.res.Resources;
import android.util.DisplayMetrics;

import com.actionsmicro.utils.PackageUtils;
import com.brsanthu.googleanalytics.GoogleAnalytics;
import com.brsanthu.googleanalytics.request.DefaultRequest;

public class AMGaTracker {

    private static DefaultRequest newDefaultRequest(Context context){
        DisplayMetrics displayMetrics = Resources.getSystem().getDisplayMetrics();
        return new DefaultRequest().applicationId(context.getPackageName())
                .applicationVersion(PackageUtils.getAppVersion(context))
                .screenResolution(displayMetrics.widthPixels+"x"+displayMetrics.heightPixels)
                .protocolVersion("1");
    }

    public static GoogleAnalytics newInstance(Context context, String trackingid) {
        return GoogleAnalytics.builder().withDefaultRequest(newDefaultRequest(context).trackingId(trackingid)
        ).build();
    }
}
