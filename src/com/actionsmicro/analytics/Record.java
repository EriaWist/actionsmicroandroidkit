package com.actionsmicro.analytics;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.TimeZone;

@SuppressWarnings("unused")
public class Record {
	private final String type;
	private final String schema_version;
	Record(String type, String schema_version) {
		this.type = type;
		this.schema_version = schema_version;
	}
	private final static TimeZone UTC_TIME_ZONE = TimeZone.getTimeZone("UTC");
	public final static DateFormat ISO_8601_DATE_TIME_FORMAT = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm'Z'");
    static {
    	ISO_8601_DATE_TIME_FORMAT.setTimeZone(UTC_TIME_ZONE);
    }
}
