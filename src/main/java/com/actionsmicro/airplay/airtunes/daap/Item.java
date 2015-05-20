package com.actionsmicro.airplay.airtunes.daap;

import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;

public class Item {
	private int type;
	private int contentSize;
	private ByteBuffer data;
	private List<Item> children;
	public Item(byte[] raw) {
		ByteBuffer rawBuffer = ByteBuffer.wrap(raw);
		rawBuffer.order(ByteOrder.BIG_ENDIAN);
		type = rawBuffer.getInt();
		contentSize = rawBuffer.getInt();
		data = ByteBuffer.wrap(raw, rawBuffer.position(), contentSize);
		parseChildIfNeeded();		
	}
	private void parseChildIfNeeded() {
		if (type == 0x6D6C6974) { // "mlit"
			children = new ArrayList<Item>();
			while (data.hasRemaining()) {
				int childType = data.getInt();
				int childSize = data.getInt();
				children.add(new Item(childType, childSize, data.array(), data.arrayOffset()+data.position()));
				data.position(data.position()+childSize);
			}
			data.rewind();
		}
	}
	public Item(int type, int size, byte[] raw, int offset) {
		this.type = type;
		this.contentSize = size;
		data = ByteBuffer.wrap(raw, offset, contentSize);
		parseChildIfNeeded();
	}
	public int getType() {
		return type;
	}
	public ByteBuffer getData() {
		return ByteBuffer.wrap(data.array(), data.arrayOffset() + data.position(), contentSize);
	}
	public Item findChildForType(int type) {
		for (Item child:children) {
			if (child.getType() == type) {
				return child;
			}
		}
		return null;
	}
	public String getChildDataAsString(int type) {
		Item child = findChildForType(type);
		if (child != null) {
			return child.getDataAsString();
		}
		return null;
	}
	public String getDataAsString() {
		try {
			return new String(data.array(), data.arrayOffset()+data.position(), contentSize, "UTF-8");
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
		return null;
	}
}
