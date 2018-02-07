package com.blackphoenix.phoenixlocationmanager.utils;

/**
 * Created by Praba on 2/7/2018.
 */
public class FilterError {
    public final static int INITIAL_ACCURACY_TIMEOUT = 0;
    public final static int GPS_STABILITY_TIMEOUT = 1;
    private int mErrorCode;
    private String mErrorMessage;

    public FilterError(int errorCode){
        this.mErrorCode = errorCode;
    }

    public int getErrorCode(){
        return this.mErrorCode;
    }

    public String getErrorMessage(){
        switch (mErrorCode){
            case INITIAL_ACCURACY_TIMEOUT:
                return "Timeout Error : Unable to Achieve Initial Accuracy";
            case GPS_STABILITY_TIMEOUT:
                return "Timeout Error : Unable to Achieve GPS Stability";
            default:
                return "Unknown Filter Error";
        }
    }

}
