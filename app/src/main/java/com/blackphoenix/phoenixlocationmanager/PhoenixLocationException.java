package com.blackphoenix.phoenixlocationmanager;

/**
 * Created by praba on 2/6/2018.
 */
public class PhoenixLocationException extends Exception {

    public PhoenixLocationException(String message) {
        super("Location Exception: "+message);
    }
}
