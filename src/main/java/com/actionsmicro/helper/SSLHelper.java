package com.actionsmicro.helper;

import com.actionsmicro.utils.Log;
import com.koushikdutta.async.http.AsyncHttpClient;

import java.net.URL;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.X509Certificate;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

public class SSLHelper {
    public static void trustSSL(AsyncHttpClient asyncHttpClient, URL url) {
        SSLContext sslContext = null;
        TrustManager[] trustManagers = null;
        try {
            sslContext = SSLContext.getInstance("TLS");
            trustManagers = new TrustManager[] {
                    new X509TrustManager() {
                        public void checkClientTrusted(X509Certificate[] chain, String authType) {}
                        public void checkServerTrusted(X509Certificate[] chain, String authType) {}
                        public X509Certificate[] getAcceptedIssuers() { return new X509Certificate[]{}; }
                    }
            };
            try {
                sslContext.init(null, trustManagers, null);
            } catch (KeyManagementException e) {
                e.printStackTrace();
            }
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        asyncHttpClient.getSSLSocketMiddleware().setSSLContext(sslContext);
        asyncHttpClient.getSSLSocketMiddleware().setTrustManagers(trustManagers);
        asyncHttpClient.getSSLSocketMiddleware().setHostnameVerifier(new HostnameVerifier() {
            @Override
            public boolean verify(String s, SSLSession sslSession) {
                if(url.getHost().contains(s)){
                    return true;
                }
                return false;
            }
        });
    }
}
