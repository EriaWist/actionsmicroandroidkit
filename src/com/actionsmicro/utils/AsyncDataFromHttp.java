package com.actionsmicro.utils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.StatusLine;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;

import android.os.AsyncTask;

public class AsyncDataFromHttp extends AsyncTask<String, String, String>{

	public interface DataDownloadListener {
        void dataDownloadedSuccessfully(String data);
        void dataDownloadFailed();
    }
	DataDownloadListener dataDownloadListener;
	public void setDataDownloadListener(DataDownloadListener dataDownloadListener){
		this.dataDownloadListener = dataDownloadListener;
	}
	
	@Override
    protected String doInBackground(String... uri) {
		final HttpParams httpParams = new BasicHttpParams();
	    HttpConnectionParams.setConnectionTimeout(httpParams, 1000);
	    HttpConnectionParams.setSoTimeout(httpParams, 1000);
		HttpClient httpclient = new DefaultHttpClient(httpParams);
        HttpResponse response;
        String responseString = null;
        try {
        	URI theUri = new URI(uri[0]);
        	if (theUri.getHost() != null) {
        		Reachability.resolveAddressByName(theUri.getHost(), 1000);
        	}
            response = httpclient.execute(new HttpGet(uri[0]));
            StatusLine statusLine = response.getStatusLine();
            if(statusLine.getStatusCode() == HttpStatus.SC_OK){
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                response.getEntity().writeTo(out);
                responseString = out.toString();
                out.close();
            } else{
                response.getEntity().getContent().close();
                throw new IOException(statusLine.getReasonPhrase());
            }
        } catch (ClientProtocolException e) {
        	e.printStackTrace();
        } catch (IOException e) {
        	e.printStackTrace();
        } catch (URISyntaxException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
        //Log.d("HttpRespondResult", responseString);
        return responseString;
    }

	@Override
	protected void onPostExecute(String result) {
		if (result != null){
			dataDownloadListener.dataDownloadedSuccessfully(result);
		}else{
			dataDownloadListener.dataDownloadFailed();
		}
	}
}
