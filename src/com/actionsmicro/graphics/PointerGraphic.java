package com.actionsmicro.graphics;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RadialGradient;
import android.graphics.Shader;
import android.os.Parcel;
import android.os.Parcelable;


public class PointerGraphic implements Graphic {
	private float x;
	public float getX() {
		return x;
	}

	public void setX(float x) {
		this.x = x;
	}
	private float y;
	public float getY() {
		return y;
	}

	public void setY(float y) {
		this.y = y;
	}
	private final float radius;
	public float getRadius() {
		return radius;
	}
	private final Paint paint;
	public PointerGraphic(Paint paint, float radius) {
		this.paint = new Paint(paint);
		this.radius = radius;
	}

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
	public int describeContents() {
		return 0;
	}
	private PointerGraphic(Parcel in) {
		// TODO
		this.paint = null;
		this.radius = 0;
	}
	public void writeToParcel(Parcel out, int flags) {
	}
	@Override
	public Paint getPaint() {
		return paint;
	}
}