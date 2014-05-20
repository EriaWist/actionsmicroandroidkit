package com.actionsmicro.airplay.airtunes.daap;

public class DaapDataParser {
	public static Item parse(byte[] raw) {
		return new Item(raw);
	}
}
