package com.actionsmicro.graphics;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.os.Parcelable;

public abstract interface Graphic extends Parcelable {
	public void draw(Canvas canvas);
	public Paint getPaint();
}