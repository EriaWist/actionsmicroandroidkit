package com.actionsmicro.airplay.auth;

/**
 * Created by Martin on 19.05.2017.
 */
public class PairSetupPin1Response {
    public final byte[] PK;
    public final byte[] SALT;

    public PairSetupPin1Response(byte[] pk, byte[] salt) {
        this.PK = pk;
        this.SALT = salt;
    }
}
