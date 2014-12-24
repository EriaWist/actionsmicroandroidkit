package com.actionsmicro.mp4.box;

public class DataEntryUrlBox extends FullBox {

	public DataEntryUrlBox() {
		super(FourCharCode("url "), (char)0x00, 0x000001);
	}

}
