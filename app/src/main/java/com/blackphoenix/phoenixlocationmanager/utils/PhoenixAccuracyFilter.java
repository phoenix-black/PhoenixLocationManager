package com.blackphoenix.phoenixlocationmanager.utils;

import android.location.Location;
import android.os.CountDownTimer;

import com.blackphoenix.phoenixlocationmanager.listeners.PxFilterCallbacks;
import com.blackphoenix.phoenixlocationmanager.listeners.PxFilteredLocationListener;

import java.util.ArrayList;

/**
 * Created by Praba on 2/6/2018.
 */

public class PhoenixAccuracyFilter {

    private final int MIN_TIMEOUT_THRESHOLD = 120000; // 2 Mins
    private final int RESET_TIMEOUT_THRESHOLD = 30000; // 30 seconds

    private boolean initialAccuracyEnabled;
    private boolean trackingAccuracyEnabled;
    private boolean stabilityFilterEnabled;
    private boolean timerEnabled;

    private float initialAccuracyThreshold = 10.0f;
    private float trackingAccuracyThreshold = 10.0f;
    private float meanStability = 1f;
    private long filterTimeout = MIN_TIMEOUT_THRESHOLD;

    private boolean isInitialGPSAccurate;
    private boolean isInitialGPSAccuracyOverridden;
    private boolean isGPSStable;
    private boolean isGPSStabilityOverridden;
    private boolean isFilterRunning;
    private boolean isTimerRunning;

    ArrayList<Location> filterLocationList;

    PxFilteredLocationListener pxFilteredLocationListener;
    PxFilterCallbacks pxFilterCallbacks;


    public PhoenixAccuracyFilter(){

    }

    public PhoenixAccuracyFilter(float initialThreshold, float stabilityMean, float trackingThreshold, long timeout){

        if(initialThreshold > 0) {
            this.initialAccuracyEnabled = true;
            setInitialAccuracyThreshold(initialThreshold);

        } else if(initialThreshold == 0){
            this.initialAccuracyEnabled = true;
            this.initialAccuracyThreshold = 10f;

        } else if(initialThreshold == -1) {
            this.initialAccuracyEnabled = false;
        }


        if(stabilityMean > 0) {
            this.stabilityFilterEnabled = true;
            setMeanStability(stabilityMean);

        } else if(stabilityMean == 0) {
            this.stabilityFilterEnabled = true;
            this.meanStability = 1f;

        } else if(stabilityMean == -1){
            this.stabilityFilterEnabled = false;
        }


        if(trackingThreshold > 0) {
            this.trackingAccuracyEnabled = true;
            setTrackingAccuracyThreshold(trackingThreshold);

        } else if(trackingThreshold == 0) {
            this.trackingAccuracyEnabled = true;
            this.trackingAccuracyThreshold = 10f;

        } else if(trackingThreshold == -1){
            this.trackingAccuracyEnabled = true;
        }


        if(timeout > 0) {
            this.timerEnabled = true;
            setFilterTimeout(timeout);

        } else if(timeout == 0){
            this.timerEnabled = true;
            setFilterTimeout(MIN_TIMEOUT_THRESHOLD);

        } else if(timeout == -1){
            this.timerEnabled = false;
        }
    }

    public PhoenixAccuracyFilter loadDefaults(){

        this.initialAccuracyEnabled = true;
        this.initialAccuracyThreshold = 10f;

        this.stabilityFilterEnabled = true;
        this.meanStability = 1f;

        this.trackingAccuracyEnabled = true;
        this.trackingAccuracyThreshold = 10f;

        this.timerEnabled = true;
        this.filterTimeout = MIN_TIMEOUT_THRESHOLD;

        return this;
    }


    /*
        Initial Accuracy Status

     */


    public boolean isInitialAccuracyEnabled() {
        return initialAccuracyEnabled;
    }

    public void setInitialAccuracyEnabled(boolean initialAccuracyEnabled) {
        this.initialAccuracyEnabled = initialAccuracyEnabled;
    }


    /*
        Stability Filter Status

     */



    public void setStabilityFilterEnabled(boolean stabilityFilterEnabled) {
        this.stabilityFilterEnabled = stabilityFilterEnabled;
    }

    public boolean isStabilityFilterEnabled() {
        return stabilityFilterEnabled;
    }

    /*
        Tracking Accuracy Status

     */

    public boolean isTrackingAccuracyEnabled() {
        return trackingAccuracyEnabled;
    }

    public void setTrackingAccuracyEnabled(boolean trackingAccuracyEnabled) {
        this.trackingAccuracyEnabled = trackingAccuracyEnabled;
    }

    /*
        Initial Accuracy Threshold

     */

    public float getInitialAccuracyThreshold() {
        return initialAccuracyThreshold;
    }

    public void setInitialAccuracyThreshold(float initialAccuracyThreshold) {
        this.initialAccuracyThreshold = initialAccuracyThreshold;
    }

    /*
       Mean Stability Value

     */

    public float getMeanStability() {
        return meanStability;
    }

    public void setMeanStability(float meanStability) {
        this.meanStability = meanStability;
    }


    /*
        Initial GPS Accuracy Status

     */

    public boolean isInitialGPSAccurate() {
        return isInitialGPSAccurate;
    }

    public void setInitialGPSAccurate(boolean initialGPSAccurate) {
        isInitialGPSAccurate = initialGPSAccurate;
    }

    public void overrideInitialGPSAccuracy(){
        isGPSStabilityOverridden = true;
    }


    /*
        GPS Stability Status

     */


    public boolean isGPSStable() {
        return isGPSStable;
    }

    public void setGPSStable(boolean GPSStable) {
        isGPSStable = GPSStable;
    }

    public void overrideGPSStability(){
        isGPSStabilityOverridden = true;
    }

    /*
        Tracking Threshold
     */


    public float getTrackingAccuracyThreshold() {
        return trackingAccuracyThreshold;
    }

    public void setTrackingAccuracyThreshold(float trackingAccuracyThreshold) {
        this.trackingAccuracyThreshold = trackingAccuracyThreshold;
    }

    /*
        Filter Timeout

     */



    public long getFilterTimeout() {
        return filterTimeout;
    }

    public void setFilterTimeout(long timeout) {
        if(timeout>=MIN_TIMEOUT_THRESHOLD) {
            this.filterTimeout = timeout;
        } else {
            this.filterTimeout = MIN_TIMEOUT_THRESHOLD;
        }
    }

    /*

        Filter Timeout Status

     */


    public boolean isTimerEnabled(){
        return this.timerEnabled;
    }

    public void setTimerEnabled(boolean status){
        this.timerEnabled = status;
    }


    /*

        Filter Location Listener

     */

    public void setFilteredLocationListener(PxFilteredLocationListener locationListener){
        this.pxFilteredLocationListener = locationListener;
    }


    public void addFilterCallbacks(PxFilterCallbacks callbacks){
        this.pxFilterCallbacks = callbacks;
    }


    /*
        Filter Operations

     */


    public void resetFilter(){
        this.isGPSStable = false;
        this.isInitialGPSAccurate = false;
        this.isGPSStabilityOverridden = false;
        this.isInitialGPSAccuracyOverridden = false;
        this.filterLocationList = null;
        if(filterCountDownTimer!=null){
            filterCountDownTimer.cancel();
        }
    }

    public void start(){
        filterLocationList = new ArrayList<>();
        this.isFilterRunning = true;

        if(isTimerEnabled()) {
            this.filterCountDownTimer.start();
        }
    }

    public void stop(){
        this.isFilterRunning = false;
        resetFilter();
    }

    public boolean isRunning(){
        return this.isFilterRunning;
    }

    public void restartTimer(long newTime){
        if(isTimerEnabled()) {
            if (newTime != 0 && newTime >= RESET_TIMEOUT_THRESHOLD) {
                filterTimeout = newTime;
            } else {
                filterTimeout = RESET_TIMEOUT_THRESHOLD;
            }

            filterCountDownTimer.start();
        }
    }


    /*

        Filter Processes

     */


    public boolean processGPSDataAccuracy(ArrayList<Location> accuracyList) {

        float meanAccuracy;
        float sumAccuracy = 0f;
        // ToDo Why the mean value of 6 is used
        // ToDo Check by increasing/decreasing the mean value

        if(isInitialGPSAccuracyOverridden){
            return true;
        }


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

        if(isGPSStabilityOverridden){
            return true;
        }

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




    public void processLocationData(Location location) {

        if(!isRunning()){
            return;
        }

        if (isInitialAccuracyEnabled() && !isInitialGPSAccurate()) {

            filterLocationList.add(location);

            if (!processGPSDataAccuracy(filterLocationList)) {
                return;
            }

            if (pxFilterCallbacks != null) {
                pxFilterCallbacks.onInitialAccuracyAchieved();
            }
        }

        if (isStabilityFilterEnabled() && !isGPSStable()) {


            if (filterLocationList == null) {
                filterLocationList = new ArrayList<>();
            }

            if (isTrackingAccuracyEnabled()) {
                if (location.getAccuracy() <= getTrackingAccuracyThreshold()) {
                    filterLocationList.add(location);
                }
            } else {
                filterLocationList.add(location);
            }

            if (!processGPSDataStability(filterLocationList)) {
                return;
            }

            if (pxFilterCallbacks != null) {
                pxFilterCallbacks.onStabilityAchieved();
            }
        }

        if (isTrackingAccuracyEnabled()) {
            if (location.getAccuracy() > getTrackingAccuracyThreshold()) {
                return;
            }
        }

        if(pxFilteredLocationListener !=null){
            pxFilteredLocationListener.onLocationChanged(location);
        }
    }


    CountDownTimer filterCountDownTimer = new CountDownTimer(filterTimeout,60000) {
        @Override
        public void onTick(long l) {

        }

        @Override
        public void onFinish() {
            //stop();

            if (pxFilterCallbacks != null) {
                pxFilterCallbacks.onTimeOut();
            }

            if(isInitialAccuracyEnabled()){
                if(!isInitialGPSAccurate() && pxFilterCallbacks != null){
                    pxFilterCallbacks.onFilterError(new FilterError(FilterError.INITIAL_ACCURACY_TIMEOUT));
                }
            }

            if(isStabilityFilterEnabled()){
                if(!isGPSStable() && pxFilterCallbacks != null){
                    pxFilterCallbacks.onFilterError(new FilterError(FilterError.GPS_STABILITY_TIMEOUT));
                }
            }

        }
    };


}
