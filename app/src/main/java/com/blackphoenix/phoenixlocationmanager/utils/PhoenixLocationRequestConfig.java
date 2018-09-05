package com.blackphoenix.phoenixlocationmanager.utils;

import com.google.android.gms.location.LocationRequest;

/**
 * Created by Praba on 2/6/2018.
 */

public class PhoenixLocationRequestConfig {

    public final static int MODE_SLOW = 0;
    public final static int MODE_FAST = 1;
    public final static int MODE_GENERAL = 2;
    public final static int MODE_SLOW_ZERO_DISTANCE = 3;

    public final static int PRIORITY_HIGH = LocationRequest.PRIORITY_HIGH_ACCURACY;
    public final static int PRIORITY_POWER = LocationRequest.PRIORITY_LOW_POWER;
    public final static int PRIORITY_BALANCED = LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY;

    public int INTERVAL;
    public int FASTEST_INTERVAL;
    public float DISPLACEMENT;
    public int PRIORITY;

    public PhoenixLocationRequestConfig(){
        this(MODE_GENERAL);
    }

    public PhoenixLocationRequestConfig(int mode){
        this.loadConfig(mode);
    }

    public PhoenixLocationRequestConfig setLocationRequestInterval(int timeMilliSeconds){
        this.INTERVAL = timeMilliSeconds;
        return this;
    }

    public PhoenixLocationRequestConfig setLocationRequestFastestInterval(int timeMilliSeconds){
        this.FASTEST_INTERVAL = timeMilliSeconds;
        return this;
    }

    public PhoenixLocationRequestConfig setLocationRequestPriority(int priority){
        this.PRIORITY = priority;
        return this;
    }


    public PhoenixLocationRequestConfig setLocationRequestDisplacement(float displacement){
        this.DISPLACEMENT = displacement;
        return this;
    }


    public PhoenixLocationRequestConfig loadConfig(int mode){

        switch (mode){
            case MODE_FAST:
                setLocationRequestInterval(0); // 0 second
                setLocationRequestFastestInterval(0); // 0 second
                setLocationRequestDisplacement(0); // 0 meter
                setLocationRequestPriority(PRIORITY_HIGH);
                return this;
            case MODE_SLOW:
                setLocationRequestInterval(30000); // 1 Second
                setLocationRequestFastestInterval(15000); // 500 millisecond
                setLocationRequestDisplacement(5); // 5 meters
                setLocationRequestPriority(PRIORITY_HIGH);
                return this;
            case MODE_SLOW_ZERO_DISTANCE:
                setLocationRequestInterval(30000); // 0 second
                setLocationRequestFastestInterval(10000); // 0 second
                setLocationRequestDisplacement(0); // 0 meter
                setLocationRequestPriority(PRIORITY_HIGH);
            case MODE_GENERAL:
            default:
                setLocationRequestInterval(1000); // 1 Second
                setLocationRequestFastestInterval(500); // 500 millisecond
                setLocationRequestDisplacement(1); // 1 meter
                setLocationRequestPriority(PRIORITY_HIGH);
                return this;
        }
    }
}
