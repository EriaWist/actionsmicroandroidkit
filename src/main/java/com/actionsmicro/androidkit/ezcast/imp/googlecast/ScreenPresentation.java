package com.actionsmicro.androidkit.ezcast.imp.googlecast;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Display;
import android.view.TextureView;
import android.view.View;
import android.widget.ImageView;

import com.actionsmicro.R;
import com.actionsmicro.utils.Log;
import com.google.android.gms.cast.CastPresentation;

import java.io.InputStream;

public class ScreenPresentation extends CastPresentation {

    private static final String TAG = ScreenPresentation.class.getSimpleName();
    private final Bitmap mAdvertiseImg;
    private ImageView mImageView;
    private TextureView mTextureView;
    private Handler mainHandler = new Handler(Looper.getMainLooper()) ;

    public ScreenPresentation(Context context, Display display, Bitmap advertiseImg) {
        super(context, display);
        mAdvertiseImg = advertiseImg;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.screen_presentation_layout);
        mTextureView = findViewById(R.id.texture_view);
        mImageView = findViewById(R.id.image_view);
        if (mAdvertiseImg != null) {
            mImageView.setImageBitmap(mAdvertiseImg);
        }
    }

    public TextureView getTextureView() {
        return mTextureView;
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
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mImageView.setVisibility(View.VISIBLE);
                mTextureView.setVisibility(View.GONE);
            }
        });

    }

    public void hideImage() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mImageView.setVisibility(View.GONE);
                mTextureView.setVisibility(View.VISIBLE);
            }
        });
    }

    private void runOnUiThread(Runnable runnable) {
        if (runnable != null) {
            if (Looper.myLooper() == Looper.getMainLooper()) {
                runnable.run();
            } else {
                if (!mainHandler.post(runnable)) {
                    Log.e(TAG, "Cannot post runnable:"+runnable);
                }
            }
        }
    }
}