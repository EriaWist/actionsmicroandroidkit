package vavi.apps.shairport;

public class RTSPResponse {
	
	private StringBuilder response = new StringBuilder();
	private StringBuilder body = new StringBuilder();

	public RTSPResponse(String header) {
    	response.append(header + "\r\n");
	}
	
	public void append(String key, String value) {
    	response.append(key + ": " + value + "\r\n");
	}
	public void appendBody(String key, String value) {
		body.append(key + ": " + value + "\r\n");
	}
	
	
	/**
	 * close the response
	 */
	public void finalize() {
		if (body.length()>0) {
			append("Content-Type", "text/parameters");
			append("Content-Length", String.valueOf(body.length()));
			response.append("\r\n");
			response.append(body.toString());
		} else {
			response.append("\r\n");
		}
	}
	
	
	public String getRawPacket() {
		return response.toString();
	}
	
	@Override
	public String toString() {
		return " > " + response.toString().replaceAll("\r\n", "\r\n > ");
	}
	
}
