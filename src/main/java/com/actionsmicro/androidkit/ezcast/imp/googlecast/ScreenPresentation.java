package com.actionsmicro.androidkit.ezcast.imp.googlecast;

import android.content.Context;
import android.os.Bundle;
import android.view.Display;
import android.view.SurfaceView;
import android.widget.ImageView;

import com.actionsmicro.R;
import com.google.android.gms.cast.CastPresentation;

public class ScreenPresentation extends CastPresentation {

    private ImageView mImageView;
    private SurfaceView mSurfaceView;

    public ScreenPresentation(Context context, Display display) {
        super(context, display);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.screen_presentation_layout);
        mSurfaceView = findViewById(R.id.surface_view);
        mImageView = findViewById(R.id.background);
    }

    public SurfaceView getSurfaceView() {
        return mSurfaceView;
    }

    public ImageView getImageView() {
        return mImageView;
    }

    public void showBackground() {
//        mImageView.setVisibility(View.VISIBLE);
    }

    public void hideBackkGround() {
//        mImageView.setVisibility(View.GONE);
    }

}