package com.actionsmicro.analytics.googleanalytics.httpclient;

import android.os.AsyncTask;

import com.actionsmicro.ezcom.http.AndroidHttpClient;
import com.actionsmicro.utils.Log;
import com.brsanthu.googleanalytics.GoogleAnalyticsConfig;
import com.brsanthu.googleanalytics.httpclient.HttpBatchRequest;
import com.brsanthu.googleanalytics.httpclient.HttpBatchResponse;
import com.brsanthu.googleanalytics.httpclient.HttpClient;
import com.brsanthu.googleanalytics.httpclient.HttpRequest;
import com.brsanthu.googleanalytics.httpclient.HttpResponse;

import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.message.BasicNameValuePair;

import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class AMHttplClientImpl implements HttpClient {

    private static final String TAG = "AMHttplClientImpl";
    AndroidHttpClient client;

    public AMHttplClientImpl(GoogleAnalyticsConfig config) {
        client = createHttpClient(config);
    }

    private AndroidHttpClient createHttpClient(GoogleAnalyticsConfig config) {
        return AndroidHttpClient.newInstance(config.getUserAgent());
    }


    @Override
    public HttpResponse post(HttpRequest req) {
        HttpResponse resp = new HttpResponse();
        AsyncTask task = new AsyncTask<Void, Void, HttpResponse>() {

            @Override
            protected HttpResponse doInBackground(Void... voids) {
                try {
                    HttpPost post = new HttpPost(req.getUrl());
                    post.setEntity(new UrlEncodedFormEntity(createNameValuePairs(req), StandardCharsets.UTF_8.toString()));
                    org.apache.http.HttpResponse httpResponse = client.execute(post);
                    resp.setStatusCode(httpResponse.getStatusLine().getStatusCode());
                } catch (Exception e) {
                    if (e instanceof UnknownHostException) {
                        Log.d(TAG, "Couldn't connect to Google Analytics. Internet may not be available. " + e.toString());
                    } else {
                        Log.e(TAG, "Exception while sending the Google Analytics tracker request " + req, e);
                    }
                } finally {
                    client.close();
                }
                return resp;
            }
        }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);

        try {
            task.get(3000, TimeUnit.MILLISECONDS);
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            Log.e(TAG, "Exception:" + e.getCause(), e);
        }
        return resp;
    }

    @Override
    public boolean isBatchSupported() {
        return false;
    }

    @Override
    public HttpBatchResponse postBatch(HttpBatchRequest req) {
        return null;
    }

    @Override
    public void close() throws Exception {
        client.close();
    }

    protected List<org.apache.http.NameValuePair> createNameValuePairs(HttpRequest req) {
        List<NameValuePair> parmas = new ArrayList<>();

        for (Map.Entry<String, String> param : req.getBodyParams().entrySet()) {
            parmas.add(new BasicNameValuePair(param.getKey(), param.getValue()));
        }
        return parmas;
    }

}
