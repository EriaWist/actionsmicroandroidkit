package com.actionsmicro.graphics;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.os.Parcelable;
/**
 * This interface defines methods for primitive graphic in a canvas system. In order to persist graphic object, this interface inherits Parcelable.
 * @author jamchen
 *
 */
public abstract interface Graphic extends Parcelable {
	/**
	 * Draw to Canvas.
	 * @param canvas The Canvas this object should draw to.
	 */
	public void draw(Canvas canvas);
	/**
	 * Return the Paint object along with this graphic object.
	 * @return The Paint object along with this graphic object.
	 */
	public Paint getPaint();
	
	public RectF getBounds();
}