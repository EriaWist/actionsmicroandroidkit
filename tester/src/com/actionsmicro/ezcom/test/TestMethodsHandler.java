package com.actionsmicro.ezcom.test;

import java.text.DateFormat;
import java.util.Date;
import java.util.List;

import com.thetransactioncompany.jsonrpc2.JSONRPC2Error;
import com.thetransactioncompany.jsonrpc2.JSONRPC2Request;
import com.thetransactioncompany.jsonrpc2.JSONRPC2Response;
import com.thetransactioncompany.jsonrpc2.server.MessageContext;
import com.thetransactioncompany.jsonrpc2.server.RequestHandler;

public class TestMethodsHandler implements RequestHandler {


	// Reports the method names of the handled requests
	public String[] handledRequests() {

		return new String[]{"getDate", "getTime", "echo", "add"};
	}


	// Processes the requests
	public JSONRPC2Response process(JSONRPC2Request req, MessageContext ctx) {

		if (req.getMethod().equals("getDate")) {

			DateFormat df = DateFormat.getDateInstance();

			String date = df.format(new Date());

			return new JSONRPC2Response(date, req.getID());

		}
		else if (req.getMethod().equals("getTime")) {

			DateFormat df = DateFormat.getTimeInstance();

			String time = df.format(new Date());

			return new JSONRPC2Response(time, req.getID());
		} else if (req.getMethod().equals("add")) {
			
			List<Object> params = req.getPositionalParams();
			int result = 0;
			for (Object object : params) {
				result += ((Long)object).longValue();
			}
			return new JSONRPC2Response(Long.valueOf(result), req.getID());
			
		} else if (req.getMethod().equals("echo")) {

			// Echo first parameter
			List<Object> params = req.getPositionalParams();

			Object input = params.get(0);

			return new JSONRPC2Response(input, req.getID());
		}
		else {

			// Method name not supported

			return new JSONRPC2Response(JSONRPC2Error.METHOD_NOT_FOUND, req.getID());
		}
	}
}