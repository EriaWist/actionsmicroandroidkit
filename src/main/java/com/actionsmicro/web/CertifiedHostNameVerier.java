package com.actionsmicro.web;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLSession;

public class CertifiedHostNameVerier implements HostnameVerifier {

    private final String mServer;

    public CertifiedHostNameVerier(String server){
        mServer = server;
    }
    @Override
    public boolean verify(String s, SSLSession sslSession) {
        if(mServer.contains(s)){
            return true;
        }
        return false;
    }
}
