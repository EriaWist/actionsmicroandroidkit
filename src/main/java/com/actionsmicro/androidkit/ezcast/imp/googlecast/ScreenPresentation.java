package com.actionsmicro.androidkit.ezcast.imp.googlecast;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.view.Display;
import android.view.SurfaceView;
import android.view.View;
import android.widget.ImageView;

import com.actionsmicro.R;
import com.google.android.gms.cast.CastPresentation;

import java.io.InputStream;

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
        mImageView = findViewById(R.id.image_view);
    }

    public SurfaceView getSurfaceView() {
        return mSurfaceView;
    }

    public ImageView getImageView() {
        return mImageView;
    }

    public void setImageView(InputStream stream) {
        showImage();
        Bitmap bmp = BitmapFactory.decodeStream(stream);
        mImageView.setImageBitmap(bmp);
    }

    public void showImage() {
        mImageView.setVisibility(View.VISIBLE);
        mSurfaceView.setVisibility(View.GONE);
    }

    public void hideImage() {
        mImageView.setVisibility(View.GONE);
        mSurfaceView.setVisibility(View.VISIBLE);
    }

}