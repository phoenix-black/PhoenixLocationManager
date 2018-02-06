package com.blackphoenix.phoenixlocationmanager.utils;

import android.location.Location;

import java.util.ArrayList;

/**
 * Created by Praba on 2/6/2018.
 */

public class PhoenixAccuracyFilter {

    private boolean initialAccuracyEnabled;
    private boolean trackingAccuracyEnabled;
    private boolean stabilityFilterEnabled;
    private float initialAccuracyThreshold = 10.0f;
    private float trackingAccuracyThreshold = 10.0f;
    private float meanStability = 1f;

    private boolean isInitialGPSAccurate;

    private boolean isGPSStable;


    public PhoenixAccuracyFilter(){

    }

    public PhoenixAccuracyFilter(float accuracyThreshold, float startPointMean){
        this.initialAccuracyEnabled = true;
        this.initialAccuracyThreshold = accuracyThreshold;
        this.stabilityFilterEnabled = true;
        this.meanStability = startPointMean;
    }


    public boolean isInitialAccuracyEnabled() {
        return initialAccuracyEnabled;
    }

    public void setInitialAccuracyEnabled(boolean initialAccuracyEnabled) {
        this.initialAccuracyEnabled = initialAccuracyEnabled;
    }


    public void setStabilityFilterEnabled(boolean stabilityFilterEnabled) {
        this.stabilityFilterEnabled = stabilityFilterEnabled;
    }

    public boolean isTrackingAccuracyEnabled() {
        return trackingAccuracyEnabled;
    }

    public boolean isStabilityFilterEnabled() {
        return stabilityFilterEnabled;
    }

    public void setTrackingAccuracyEnabled(boolean trackingAccuracyEnabled) {
        this.trackingAccuracyEnabled = trackingAccuracyEnabled;
    }

    public float getInitialAccuracyThreshold() {
        return initialAccuracyThreshold;
    }

    public void setInitialAccuracyThreshold(float initialAccuracyThreshold) {
        this.initialAccuracyThreshold = initialAccuracyThreshold;
    }

    public float getMeanStability() {
        return meanStability;
    }

    public void setMeanStability(float meanStability) {
        this.meanStability = meanStability;
    }

    public boolean isInitialGPSAccurate() {
        return isInitialGPSAccurate;
    }

    public void setInitialGPSAccurate(boolean initialGPSAccurate) {
        isInitialGPSAccurate = initialGPSAccurate;
    }

    public boolean isGPSStable() {
        return isGPSStable;
    }

    public void setGPSStable(boolean GPSStable) {
        isGPSStable = GPSStable;
    }

    public float getTrackingAccuracyThreshold() {
        return trackingAccuracyThreshold;
    }

    public void setTrackingAccuracyThreshold(float trackingAccuracyThreshold) {
        this.trackingAccuracyThreshold = trackingAccuracyThreshold;
    }

    public void resetFilter(){
        this.isGPSStable = false;
        this.isInitialGPSAccurate = false;
    }

    public boolean processGPSDataAccuracy(ArrayList<Location> accuracyList) {

        float meanAccuracy;
        float sumAccuracy = 0f;
        // ToDo Why the mean value of 6 is used
        // ToDo Check by increasing/decreasing the mean value
        if(accuracyList != null && accuracyList.size()> 6) {

            for(int i = 1; i< 6; i++) {
                Location location = accuracyList.get(accuracyList.size()-i);
                if(location!=null) {
                    sumAccuracy += location.getAccuracy();
                } else {
                    return false;
                }
            }

            meanAccuracy = sumAccuracy/5f;

            isInitialGPSAccurate = (meanAccuracy <= initialAccuracyThreshold);
            return isInitialGPSAccurate;
        }

        return false;
    }


    public boolean processGPSDataStability(ArrayList<Location> locationList) {

        float maxDistance;
        float totalDistance = 0f;
        float meanDistance;


        if(locationList != null && locationList.size()>6) {

            // ToDo - Why the count is choosen as 6
            // ToDo Test by increasing or decreasing the value
            for(int i=1;i<6;i++){
                float[] result = new float[3];
                Location source = locationList.get(locationList.size()-i);
                Location destination = locationList.get(locationList.size()-(i+1));
                Location.distanceBetween(source.getLatitude(), source.getLongitude(),
                        destination.getLatitude(), destination.getLongitude(), result);
                totalDistance +=  result[0];
            }

            meanDistance = totalDistance / 5f;

            Location Data1 = locationList.get(locationList.size()-1);
            Location Data6 = locationList.get(locationList.size()-6);

            float[] result = new float[3];
            Location.distanceBetween(Data1.getLatitude(), Data1.getLongitude(),
                    Data6.getLatitude(), Data6.getLongitude(), result);
            maxDistance =  result[0];

            isGPSStable = (meanDistance <= meanStability) && (maxDistance <= meanStability);
            return isGPSStable;
        }

        return false;
    }
}
