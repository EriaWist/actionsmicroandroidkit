package com.actionsmicro.graphics;

import java.io.Serializable;

public class Point implements Serializable {
	public static final float UNSPECIFIED_FLOAT = -1000000;
	private static final long serialVersionUID = 0L;
	public Point(float currentX, float currentY) {
		x = currentX;
		y = currentY;
	}
	public float x = UNSPECIFIED_FLOAT;
	public float y = UNSPECIFIED_FLOAT;
}
