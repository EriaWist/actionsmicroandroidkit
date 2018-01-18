package com.actionsmicro.airplay.auth;

/**
 * Created by Martin on 19.05.2017.
 */
public class PairSetupPin2Response {
    public final byte[] PROOF;

    public PairSetupPin2Response(byte[] proof) {
        this.PROOF = proof;
    }
}
