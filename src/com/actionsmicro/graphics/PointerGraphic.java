package com.actionsmicro.graphics;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RadialGradient;
import android.graphics.RectF;
import android.graphics.Shader;
import android.os.Parcel;
import android.os.Parcelable;

/**
 * A graphic object which draws a pointer with given Paint and radius.
 * @author jamchen
 *
 */
public class PointerGraphic implements Graphic {
	private float x;
	/**
	 * Return the x position of the point.
	 * @return The x position of the point.
	 */
	public float getX() {
		return x;
	}
	/**
	 * Update the x position.
	 * @param x The new value of x position.
	 */
	public void setX(float x) {
		this.x = x;
	}
	private float y;
	/**
	 * Return the y position of the point.
	 * @return The y position of the point.
	 */
	public float getY() {
		return y;
	}
	/**
	 * Update the y position.
	 * @param y The new value of y position.
	 */
	public void setY(float y) {
		this.y = y;
	}
	private final float radius;
	/**
	 * Return the radius of the point.
	 * @return The radius of the porint.
	 */
	public float getRadius() {
		return radius;
	}
	private final Paint paint;
	/**
	 * Create a PointerGraphic with given Paint and radius.
	 * @param paint The Paint to be used while drawing.
	 * @param radius The radius of the pointer.
	 */
	public PointerGraphic(Paint paint, float radius) {
		this.paint = new Paint(paint);
		this.radius = radius;
	}
	@Override
	public void draw(Canvas canvas) {
		paint.setShader(new RadialGradient(x, y, radius, 0xffff0000, 0x00ff0000, Shader.TileMode.CLAMP));
		canvas.drawCircle(x, y, radius, paint);
	}
	public static final Parcelable.Creator<PointerGraphic> CREATOR
	= new Parcelable.Creator<PointerGraphic>() {
		public PointerGraphic createFromParcel(Parcel in) {
			return new PointerGraphic(in);
		}

		public PointerGraphic[] newArray(int size) {
			return new PointerGraphic[size];
		}
	};
	@Override
	public int describeContents() {
		return 0;
	}
	private PointerGraphic(Parcel in) {
		// TODO
		this.paint = null;
		this.radius = 0;
	}
	@Override
	public void writeToParcel(Parcel out, int flags) {
	}
	@Override
	public Paint getPaint() {
		return paint;
	}
	@Override
	public RectF getBounds() {		
		return new RectF(x-radius, y-radius, x+radius, y+radius);
	}
}