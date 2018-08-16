package com.blackphoenix.phoenixlocationmanager;

import android.Manifest;
import android.app.Activity;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AlertDialog;
import android.widget.Toast;


import com.blackphoenix.phoenixlocationmanager.kalmanlocationmanager.KalmanLocationManager;
import com.blackphoenix.phoenixlocationmanager.listeners.PxActivityRecognitionListener;
import com.blackphoenix.phoenixlocationmanager.listeners.PxConnectionCallbacks;
import com.blackphoenix.phoenixlocationmanager.listeners.PxLocationListener;
import com.blackphoenix.phoenixlocationmanager.utils.PhoenixAccuracyFilter;
import com.blackphoenix.phoenixlocationmanager.utils.PhoenixLocationRequestConfig;
import com.blackphoenix.phoenixlocationmanager.utils.PxActivityRecognition;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.ActivityRecognition;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by Praba on 2/6/2018.
 *
 */
public class PhoenixLocationManager  {

    // Activity Recognition

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



/*    public abstract void onPhoenixLocationChanged(Location location);
    public abstract void onAPIConnected(Bundle bundle);
    public abstract void onAPIConnectionSuspended(int i);
    public abstract void onAPIConnectionFailed(ConnectionResult connectionResult);*/

    private PxConnectionCallbacks mPxConnectionCallbacks;
    private PxLocationListener mPxLocationListener;
   // private PxFilterCallbacks mPxFilterCallbacks;



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

    // ActivityRecognition
    Intent activityRecognitionIntentService;
    PendingIntent activityRecognitionPendingIntent;
    BroadcastReceiver activityRecognitionBroadcastReceiver;
    PxActivityRecognitionListener pxActivityRecognitionListener;

    //AccuracyFilter
    // ToDo Move the FilerLocation and all related logic List to the PhoenixAccuracyFilter
    // ToDo Add a mthod : addLocationData(Location location) in PhoenixAccuracyFilter
    // ToDo Handle all filter related logic inside that method

   // ArrayList<Location> filterLocationList;

    public static boolean isSatelliteGPSEnabled(Context context) {
        LocationManager locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
    }

    public static boolean isNetworkGPSEnabled(Context context) {
        LocationManager locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);

        return locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
    }

    public static void openGPSSettings(final Context context, final int resultCode){
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(context);

        alertDialogBuilder.setMessage("GPS is disabled in your device. Would you like to enable it?")
                .setCancelable(false)
                .setPositiveButton("Goto Settings Page To Enable GPS",
                        new DialogInterface.OnClickListener(){
                            public void onClick(DialogInterface dialog, int id){
                                Intent callGPSSettingIntent = new Intent(
                                        android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                                ((Activity)context).startActivityForResult(callGPSSettingIntent,resultCode);
                            }
                        });

        alertDialogBuilder.setNegativeButton("Cancel",
                new DialogInterface.OnClickListener(){
                    public void onClick(DialogInterface dialog, int id){
                        dialog.cancel();
                    }
                });
        AlertDialog alert = alertDialogBuilder.create();
        alert.show();

    }


    public PhoenixLocationManager(Context context) throws PhoenixLocationException {
        mContext = context;

        if(!isGooglePlayServicesAvailable()){
            // ToDo Handle it in a better Way
            throw new PhoenixLocationException("Google Play Services Not Available");
        }

        mKalmanLocationManager = new KalmanLocationManager(context);
        mLocationRequest = new LocationRequest();
        locationRequestConfig = new PhoenixLocationRequestConfig();

        // Activity Recognition
        activityRecognitionIntentService = new Intent(mContext,PhoenixActivityRecognitionService.class);
        activityRecognitionPendingIntent = PendingIntent.getService( mContext, 0, activityRecognitionIntentService, PendingIntent.FLAG_UPDATE_CURRENT );
        activityRecognitionBroadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String Action = intent.getAction();
                if(Action==null){
                    return;
                }
                if (Action.equals(PxActivityRecognition.BROADCAST_DETECTED_ACTIVITY)) {
                    String allProbableActivity = intent.getStringExtra(PxActivityRecognition.ALL_PROBABLE_ACTIVITY);
                    String mostProbableActivity = intent.getStringExtra(PxActivityRecognition.MOST_PROBABLE_ACTIVITY);

                    if(pxActivityRecognitionListener!=null){
                        try {
                            pxActivityRecognitionListener.onActivityRecognized(new JSONArray(allProbableActivity),
                                    new JSONObject(mostProbableActivity));
                        } catch (JSONException e) {
                            e.printStackTrace();
                            // ToDo : Handle Here
                        }
                    }

                   // handleUserActivity(type, confidence);
                }
            }
        };
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


    public PhoenixLocationManager addConnectionCallbacks(PxConnectionCallbacks connectionCallbacks){
        this.mPxConnectionCallbacks = connectionCallbacks;
        return this;
    }

   /* public PhoenixLocationManager addFilterCallbacks(PxFilterCallbacks filterCallbacks){
        this.mPxFilterCallbacks = filterCallbacks;
        return this;
    }*/

    public PhoenixLocationManager setLocationListener(PxLocationListener locationListener){
        this.mPxLocationListener = locationListener;
        return this;
    }

    public PhoenixLocationManager removeLocationListener(){
        this.mPxLocationListener = null;
        return this;
    }

    public PhoenixLocationManager build(){

        mLocationRequest.setInterval(locationRequestConfig.INTERVAL); // INTERVAL in milliseconds
        mLocationRequest.setFastestInterval(locationRequestConfig.FASTEST_INTERVAL); // INTERVAL in milliseconds
        mLocationRequest.setSmallestDisplacement(locationRequestConfig.DISPLACEMENT);
        mLocationRequest.setPriority(locationRequestConfig.PRIORITY);

        mGoogleApiClient = new GoogleApiClient.Builder(mContext)
                .addApi(LocationServices.API)
                .addApi(ActivityRecognition.API)
                .addConnectionCallbacks(mConnectionCallbacks)
                .addOnConnectionFailedListener(mConnectionFailedListener)
                .build();

        return this;
    }


    public boolean loadOnStart(){
        return this.connect();
    }


    public void loadOnPause(){
        this.stopLocationUpdates();
        this.stopActivityRecognition();
    }

    public void loadOnStop(){
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

      //  filterLocationList = new ArrayList<>();
        if(accuracyFilter!=null && !accuracyFilter.isRunning()) {
            accuracyFilter.start();
        }

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

      //  filterLocationList = null;
        if(accuracyFilter!=null && accuracyFilter.isRunning()) {
            accuracyFilter.stop();
        }

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

            if(mPxLocationListener!=null){
                mPxLocationListener.onRawLocationChanged(location);
            }

            if(accuracyFilter!=null && accuracyFilter.isRunning()){
                accuracyFilter.processLocationData(location);
            }

        }
    };

    public void setActivityRecognitionListener(PxActivityRecognitionListener listener){
        this.pxActivityRecognitionListener = listener;
    }

    private void startActivityRecognition(){

        try {
            if (pxActivityRecognitionListener != null) {
                ActivityRecognition.ActivityRecognitionApi.requestActivityUpdates(mGoogleApiClient, 0, activityRecognitionPendingIntent);

                LocalBroadcastManager.getInstance(mContext).registerReceiver(activityRecognitionBroadcastReceiver,
                        new IntentFilter(PxActivityRecognition.BROADCAST_DETECTED_ACTIVITY));
            } else {
                // Log it
            }
        }catch (Exception e){

        }
    }


    private void stopActivityRecognition(){
        try {

            if(pxActivityRecognitionListener!=null) {
                ActivityRecognition.ActivityRecognitionApi.removeActivityUpdates(mGoogleApiClient, activityRecognitionPendingIntent);

                LocalBroadcastManager.getInstance(mContext).unregisterReceiver(activityRecognitionBroadcastReceiver);
            }
        } catch (Exception e){

        }

    }


    GoogleApiClient.ConnectionCallbacks mConnectionCallbacks = new GoogleApiClient.ConnectionCallbacks() {
        @Override
        public void onConnected(@Nullable Bundle bundle) {
            startLocationUpdates();
            startActivityRecognition();
            if(mPxConnectionCallbacks!=null){
                mPxConnectionCallbacks.onAPIConnected(bundle);
            }
        }

        @Override
        public void onConnectionSuspended(int i) {
            if(mPxConnectionCallbacks!=null){
                mPxConnectionCallbacks.onAPIConnectionSuspended(i);
            }
        }
    };


    GoogleApiClient.OnConnectionFailedListener mConnectionFailedListener = new GoogleApiClient.OnConnectionFailedListener() {
        @Override
        public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
            if(mPxConnectionCallbacks!=null){
                mPxConnectionCallbacks.onAPIConnectionFailed(connectionResult);
            }

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
