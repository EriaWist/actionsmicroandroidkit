package com.actionsmicro.graphics;

import java.io.Serializable;

/**
 * This class encapsulates 2D position in float. The android.graphics.Point store x-y values in integer.
 * @author jamchen
 *
 */
public class Point implements Serializable {
	/**
	 * A minimum value to indicate the unspecified value.
	 */
	public static final float UNSPECIFIED_FLOAT = -1000000;
	private static final long serialVersionUID = 0L;
	/**
	 * Create a Point
	 * @param currentX X-axis value.
	 * @param currentY Y-axis value.
	 */
	public Point(float currentX, float currentY) {
		x = currentX;
		y = currentY;
	}
	public float x = UNSPECIFIED_FLOAT;
	public float y = UNSPECIFIED_FLOAT;
}
