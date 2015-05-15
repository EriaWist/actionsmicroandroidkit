package com.actionsmicro.mp4.box;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

public class DataReferenceBox extends FullBox {

	private List<FullBox> dataEntries = new ArrayList<FullBox>();
	public DataReferenceBox() {
		super(FourCharCode("dref"), (char)0x00, 0);
	}
	public void addDataEntry(FullBox dataEntry) {
		dataEntries.add(dataEntry);
	}
	@Override
	protected void writeBody(ByteBuffer byteBuffer) {
		super.writeBody(byteBuffer);
		byteBuffer.putInt(dataEntries.size());
		for (FullBox d : dataEntries) {
			d.write(byteBuffer);		
		}
	}
	private int getDataEntrySize() {
		int size = 0;
		for (FullBox d : dataEntries) {
			size += d.getBoxSize(); 
		}
		return size;
	}
	@Override
	protected int getBodySize() {
		return super.getBodySize() + 4 + getDataEntrySize();
	}
}
