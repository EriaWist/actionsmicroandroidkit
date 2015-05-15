package com.actionsmicro.mp4.box;

import java.nio.ByteBuffer;
import java.util.List;

public class FileTypeBox extends Box {

	private int majorBrand;
	private int minorVersion;
	private List<Integer> compatibleBrands;

	public FileTypeBox(int majorBrand, int minorVersion, List<Integer> compatibleBrands) {
		super(FourCharCode("ftyp"));
		this.majorBrand = majorBrand;
		this.minorVersion = minorVersion;
		this.compatibleBrands = compatibleBrands;
	}
	@Override
	protected void writeBody(ByteBuffer byteBuffer) {
		super.writeBody(byteBuffer);
		byteBuffer.putInt(majorBrand);
		byteBuffer.putInt(minorVersion);
		if (compatibleBrands != null) {
			for (Integer compatibleBrand : compatibleBrands) {
				byteBuffer.putInt(compatibleBrand);
			}
		}
	}
	private int getCompatibleBrandsSize() {
		if (compatibleBrands == null) return 0;
		return compatibleBrands.size() * 4;
	}
	@Override
	protected int getBodySize() {
		return 4+4+getCompatibleBrandsSize();
	}
}
