package com.actionsmicro.mp4.box;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

public class Box {
	private int type;
	private List<Box> children = new ArrayList<Box>();
	public static int FourCharCode(String fourcc) {
		if (fourcc == null) {
			throw new NullPointerException("fourCC cannot be null");
		}
		if (fourcc.length() != 4) {
			throw new IllegalArgumentException("FourCC must be four characters long");
		}
		//TODO ENDIAN issue
		int val = 0;
		for (int i = 0; i < 4; i++) {
			val <<= 8;
			val |= fourcc.charAt(i);
		}
		return val;
	}
	public Box(int type) {
		this.type = type;
	}
	public final void write(ByteBuffer byteBuffer) {
		byteBuffer.putInt(getBoxSize());
		byteBuffer.putInt(type);
		writeBody(byteBuffer);
		for (Box child : children) {
			child.write(byteBuffer);
		}
	}
	protected void writeBody(ByteBuffer byteBuffer) {
		
	}
	private int getChildrenSize() {
		int size = 0;
		for (Box child : children) {
			size += child.getBoxSize();
		}
		return size;
	}
	public final int getBoxSize() {
		return 8 + getBodySize() + getChildrenSize();
	}
	protected int getBodySize() {
		return 0;
	}
	public void addChild(Box child) {
		if (child != null) {
			children.add(child);
		}
	}
}
