package com.blackphoenix.phoenixlocationmanager;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.widget.Toast;


import com.blackphoenix.phoenixlocationmanager.kalmanlocationmanager.KalmanLocationManager;
import com.blackphoenix.phoenixlocationmanager.utils.PhoenixAccuracyFilter;
import com.blackphoenix.phoenixlocationmanager.utils.PhoenixLocationRequestConfig;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;

import java.util.ArrayList;

/**
 * Created by Praba on 2/6/2018.
 *
 */
public abstract class PhoenixLocationManager  {

    // Static Declarations
    private final static int PLAY_SERVICES_RESOLUTION_REQUEST = 1000;
    private final static int API_CONNECTION_RESOLUTION_REQUEST = 1001;
    /**
     * Request location updates with the highest possible frequency on gps.
     * Typically, this means one update per second for gps.
     */
    private static final long GPS_TIME = 1000;

    /**
     * For the network provider, which gives locations with less accuracy (less reliable),
     * request updates every 5 seconds.
     */
    private static final long NET_TIME = 5000;

    /**
     * For the filter-time argument we use a "real" value: the predictions are triggered by a timer.
     * Lets say we want 5 updates (estimates) per second = update each 200 millis.
     */
    private static final long FILTER_TIME = 200;



    public abstract void onPhoenixLocationChanged(Location location);
    public abstract void onAPIConnected(Bundle bundle);
    public abstract void onAPIConnectionSuspended(int i);
    public abstract void onAPIConnectionFailed(ConnectionResult connectionResult);

    //
    Context mContext;


    // Kalman Filter

    KalmanLocationManager mKalmanLocationManager;
    boolean isKalmanFilterEnabled = false;

    // GoogleApi
    GoogleApiClient mGoogleApiClient;
    LocationRequest mLocationRequest;
    PhoenixLocationRequestConfig locationRequestConfig;
    PhoenixAccuracyFilter accuracyFilter;
    Activity mResultCallBackActivity = null;

    //AccuracyFilter
    // ToDo Move the FilerLocation and all related logic List to the PhoenixAccuracyFilter
    // ToDo Add a mthod : addLocationData(Location location) in PhoenixAccuracyFilter
    // ToDo Handle all filter related logic inside that method

    ArrayList<Location> filterLocationList;

    public PhoenixLocationManager(Context context) throws PhoenixLocationException {
        mContext = context;

        if(isGooglePlayServicesAvailable()){
            // ToDo Handle it in a better Way
            throw new PhoenixLocationException("Google Play Services Not Available");
        }

        mKalmanLocationManager = new KalmanLocationManager(context);
        mLocationRequest = new LocationRequest();
        locationRequestConfig = new PhoenixLocationRequestConfig();
        accuracyFilter = new PhoenixAccuracyFilter();
    }

    public PhoenixLocationManager setLocationRequestConfig(PhoenixLocationRequestConfig config){
        this.locationRequestConfig = config;
        return this;
    }

    public PhoenixLocationRequestConfig getLocationRequestConfig(){
        return this.locationRequestConfig;
    }


    public PhoenixLocationManager setAccuracyFilter(PhoenixAccuracyFilter filter){
        this.accuracyFilter = filter;
        return this;
    }

    public PhoenixAccuracyFilter getAccuracyFilter(){
        return this.accuracyFilter;
    }

    public PhoenixLocationManager setResultCallbackActivity(Activity activity){

        // Handle this request from onActivityResult()

        this.mResultCallBackActivity = activity;
        return this;
    }

    public PhoenixLocationManager setKalmanFilterEnabled(boolean enabled){
        this.isKalmanFilterEnabled = enabled;
        return this;
    }

    public PhoenixLocationManager build(){

        mLocationRequest.setInterval(locationRequestConfig.INTERVAL); // INTERVAL in milliseconds
        mLocationRequest.setFastestInterval(locationRequestConfig.FASTEST_INTERVAL); // INTERVAL in milliseconds
        mLocationRequest.setSmallestDisplacement(locationRequestConfig.DISPLACEMENT);
        mLocationRequest.setPriority(locationRequestConfig.PRIORITY);

        mGoogleApiClient = new GoogleApiClient.Builder(mContext)
                .addApi(LocationServices.API)
                .addConnectionCallbacks(mConnectionCallbacks)
                .addOnConnectionFailedListener(mConnectionFailedListener)
                .build();

        return this;
    }


    private boolean loadOnStart(){
        return this.connect();
    }


    private void loadOnPause(){
        this.stopLocationUpdates();
    }

    private void loadOnStop(){
        this.disconnect();
    }


    public boolean connect(){
        // ToDo Handle Each Condition separately
        if((mGoogleApiClient != null && !mGoogleApiClient.isConnected() && !mGoogleApiClient.isConnecting()))
        {
            mGoogleApiClient.connect();
            return true;
        }

        return false;
    }

    public void disconnect(){
        if(mGoogleApiClient.isConnected()){
            mGoogleApiClient.disconnect();
        }
    }

    public boolean isConnected(){
        return mGoogleApiClient.isConnected();
    }


    public void startLocationUpdates() {
        Toast.makeText(mContext, "Starting Location Updates", Toast.LENGTH_SHORT).show();
        // Start Location Updates
        if (ActivityCompat.checkSelfPermission(mContext, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(mContext, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }

        filterLocationList = new ArrayList<>();

        mKalmanLocationManager.requestLocationUpdates(
                KalmanLocationManager.UseProvider.GPS_AND_NET,
                FILTER_TIME,
                GPS_TIME,
                NET_TIME,
                mGoogleApiClient,
                mLocationRequest,
                mLocationListener, true, isKalmanFilterEnabled);
    }


    public void stopLocationUpdates() {

        filterLocationList = null;
        accuracyFilter.resetFilter();

        if(mGoogleApiClient.isConnected()) {
            mKalmanLocationManager.removeUpdates(mLocationListener);
        }
    }

    private boolean isGooglePlayServicesAvailable() {
        GoogleApiAvailability googleApiAvailability = GoogleApiAvailability.getInstance();

        int result = googleApiAvailability.isGooglePlayServicesAvailable(mContext);

        if(result!= ConnectionResult.SUCCESS){

            if(googleApiAvailability.isUserResolvableError(result)) {

                //ToDo : Chanage the getErrorDialog to getErrorResolutionPendingIntent/getErrorResolutionPendingIntent To Improve the user Experience

                googleApiAvailability.getErrorDialog((Activity) mContext, result,
                        PLAY_SERVICES_RESOLUTION_REQUEST, new DialogInterface.OnCancelListener() {
                            @Override
                            public void onCancel(DialogInterface dialogInterface) {
                                // ToDo Show Toast message showing user cannot proceed before completing this process
                                dialogInterface.dismiss();
                            }
                        }).show();
            } else {
                // ToDo Show message dialog - Some Serious Error Occurred. Contact/Report Support Team
                // ToDo Show message dialog - Try Again after some time
            }

            return false;
        }

        return true;

    }


    LocationListener mLocationListener = new LocationListener() {
        @Override
        public void onLocationChanged(Location location) {

            if(accuracyFilter.isInitialAccuracyEnabled() && !accuracyFilter.isInitialGPSAccurate()){


                if(filterLocationList == null) {
                    filterLocationList = new ArrayList<>();
                }

                filterLocationList.add(location);

                if (!accuracyFilter.processGPSDataAccuracy(filterLocationList)){
                    return;
                }
            }

            if(accuracyFilter.isStabilityFilterEnabled() && !accuracyFilter.isGPSStable()){



                if(filterLocationList == null) {
                    filterLocationList = new ArrayList<>();
                }

                if(accuracyFilter.isTrackingAccuracyEnabled() &&
                        location.getAccuracy() <= accuracyFilter.getTrackingAccuracyThreshold()) {
                    filterLocationList.add(location);
                }

                if (!accuracyFilter.processGPSDataStability(filterLocationList)){
                    return;
                }
            }

            if(accuracyFilter.isTrackingAccuracyEnabled()) {
                if(location.getAccuracy() > accuracyFilter.getTrackingAccuracyThreshold()) {
                    return;
                }
            }

            onPhoenixLocationChanged(location);
        }
    };

    GoogleApiClient.ConnectionCallbacks mConnectionCallbacks = new GoogleApiClient.ConnectionCallbacks() {
        @Override
        public void onConnected(@Nullable Bundle bundle) {
            startLocationUpdates();
            onAPIConnected(bundle);
        }

        @Override
        public void onConnectionSuspended(int i) {
            onAPIConnectionSuspended(i);
        }
    };


    GoogleApiClient.OnConnectionFailedListener mConnectionFailedListener = new GoogleApiClient.OnConnectionFailedListener() {
        @Override
        public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
            onAPIConnectionFailed(connectionResult);

            if(mResultCallBackActivity != null){
                if(connectionResult.hasResolution()) {
                    try {
                        connectionResult.startResolutionForResult(mResultCallBackActivity, API_CONNECTION_RESOLUTION_REQUEST);
                    } catch (IntentSender.SendIntentException e) {
                        // ToDo Think on What to do here -> Error occured during previous error handler.
                        e.printStackTrace();
                    }
                } else {
                    // ToDo Handle here - At-least a toast message
                }
            }


        }
    };




}
