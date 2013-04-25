package com.actionsmicro.graphics;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Xfermode;
import android.os.Parcel;
import android.os.Parcelable;

/**
 * A graphic object to draw given path with given Paint.
 * 
 * @author jamchen
 *
 */

public class PathGraphic implements Graphic {
	private final Path path = new Path();
	private final Paint paint;
	private final ArrayList<Point> points = new ArrayList<Point>();

	/**
	 * Create a PathGraphic with initial position and Paint.
	 * @param x Initial x position.
	 * @param y Initial y position.
	 * @param paint The Paint to be used while drawing the path.
	 */
	public PathGraphic(float x, float y, final Paint paint) {
		this.paint = new Paint(paint);
		path.moveTo(x, y);
		points.add(new Point(x, y));
	}
	/**
	 * Draw this graphic object to given Canvas.
	 * @param canvas The canvas this graphic object draws to.
	 */
	@Override
	public void draw(Canvas canvas) {
		canvas.drawPath(path, paint);
	}
	public static final Parcelable.Creator<PathGraphic> CREATOR
	= new Parcelable.Creator<PathGraphic>() {
		public PathGraphic createFromParcel(Parcel in) {
			return new PathGraphic(in);
		}

		public PathGraphic[] newArray(int size) {
			return new PathGraphic[size];
		}
	};
	@Override
	public int describeContents() {
		return 0;
	}
	private PathGraphic(Parcel in) {
		points.addAll(Arrays.asList((Point[])in.readArray(null)));
		final Iterator<Point> it = points.iterator();
		if (it.hasNext()) {
			Point point = it.next();
			path.moveTo(point.x, point.y);
			while (it.hasNext()) {
				point = it.next();
				path.lineTo(point.x, point.y);
			}
		}
		paint = createPaintFromParcel(in);
	}
	@Override
	public void writeToParcel(Parcel out, int flags) {
		out.writeArray(points.toArray());
		writePaitToParcel(paint, out);
	}
	/**
	 * Create a Paint from Parcel.
	 * @param in The Parcel which contains the data stored via writePaitToParcel.
	 * @return A Paint which creates with parameters stored in the Parcel.
	 * @see Falcon#writePaitToParcel
	 */
	private static Paint createPaintFromParcel(Parcel in) {
		final Paint paint = new Paint();
		paint.setColor(in.readInt());
		paint.setStrokeWidth(in.readFloat());
		paint.setAlpha(in.readInt());
		paint.setAntiAlias(in.readByte() == 1);
		paint.setStrokeCap(Paint.Cap.valueOf(in.readString()));
		paint.setStyle(Paint.Style.valueOf(in.readString()));
		if (in.readByte() == 1) {
			paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.CLEAR));
		}
		return paint;
	}
	/**
	 * Store parameters of a paint to Parcel for recreating a Paint which has same parameters later.
	 * @param paint The Paint to be store.
	 * @param out The Parcel the Paint's parameters will be stored to.
	 */
	private static void writePaitToParcel(Paint paint, Parcel out) {
		out.writeInt(paint.getColor());
		out.writeFloat(paint.getStrokeWidth());
		out.writeInt(paint.getAlpha());
		out.writeByte((byte) (paint.isAntiAlias() ? 1 : 0));
		out.writeString(paint.getStrokeCap().toString());
		out.writeString(paint.getStyle().toString());
		final Xfermode xfermode = paint.getXfermode();
		if (xfermode != null && xfermode instanceof PorterDuffXfermode) {
			out.writeByte((byte) 1);			
		} else {
			out.writeByte((byte) 0);
		}
	}
	@Override
	public Paint getPaint() {
		return paint;
	}
	/**
	 * Add a line segment to the graphic.
	 * @param x The destination x position.
	 * @param y The destination y position.
	 */
	public void lineTo(float x, float y) {
		this.path.lineTo(x, y);
		points.add(new Point(x, y));
	}
}