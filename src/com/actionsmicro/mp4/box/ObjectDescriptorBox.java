package com.actionsmicro.mp4.box;

import java.nio.ByteBuffer;

public class ObjectDescriptorBox extends FullBox {

//-> 1 byte file IOD type tag = 8-bit hex value 0x10
// -> 3 bytes extended descriptor type tag string = 3 * 8-bit hex value
//   - types are Start = 0x80 ; End = 0xFE
//   - NOTE: the extended start tags may be left out
// -> 1 byte descriptor type length = 8-bit unsigned length
//
//   -> 2 bytes OD ID = 16-bit unsigned value
//   -> 1 byte OD profile level = 8-bit unsigned value
//   -> 1 byte scene profile level = 8-bit unsigned value
//   -> 1 byte audio profile level = 8-bit unsigned value
//   -> 1 byte video profile level = 8-bit unsigned value
//   -> 1 byte graphics profile level = 8-bit unsigned value
//     - NOTE: if level unused then set to 0xFF
	
	public ObjectDescriptorBox() {
		super(FourCharCode("iods"), (char)0, 0);
		
	}
	@Override
	protected void writeBody(ByteBuffer byteBuffer) {
		super.writeBody(byteBuffer);
		byteBuffer.put((byte)0x10);
		byteBuffer.put((byte)0x07);
		byteBuffer.putShort((short)0x004f);
		byteBuffer.put((byte)0xff);
		byteBuffer.put((byte)0xff);
		byteBuffer.put((byte)0xff);
		byteBuffer.put((byte)0x15);
		byteBuffer.put((byte)0xff);
	}
	@Override
	protected int getBodySize() {
		return super.getBodySize() + 9;
	}
}
