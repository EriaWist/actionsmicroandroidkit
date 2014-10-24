package com.actionsmicro.analytics;

@SuppressWarnings("unused")
public class Record {
	private final String type;
	private final String schema_version;
	Record(String type, String schema_version) {
		this.type = type;
		this.schema_version = schema_version;
	}
}
