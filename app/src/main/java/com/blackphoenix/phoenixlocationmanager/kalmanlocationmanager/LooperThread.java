/*
 * LooperThread
 *
 * Copyright (c) 2014 Renato Villone
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.blackphoenix.phoenixlocationmanager.kalmanlocationmanager;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
//import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.SystemClock;
import android.support.v4.app.ActivityCompat;
// Praba
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationListener;


import static com.blackphoenix.phoenixlocationmanager.kalmanlocationmanager.KalmanLocationManager.KALMAN_PROVIDER;
import static com.blackphoenix.phoenixlocationmanager.kalmanlocationmanager.KalmanLocationManager.UseProvider;

/**
 * Created by Rena on 28/09/2014.
 */

/*
    * Handle the If/Else statement on Permission checks
 */

class LooperThread extends Thread {

    // Static constant
    private static final int THREAD_PRIORITY = 5;

    private static final double DEG_TO_METER = 111225.0;
    private static final double METER_TO_DEG = 1.0 / DEG_TO_METER;

    private static final double TIME_STEP = 1.0;
    private static final double COORDINATE_NOISE = 4.0 * METER_TO_DEG;
    private static final double ALTITUDE_NOISE = 10.0;

    // Context
    private final Context mContext;
    private final Handler mClientHandler;
  //  private final LocationManager mLocationManager;

    // Praba
    private final GoogleApiClient mGoogleApiClient;
    private final LocationRequest mLocationRequest;

    // Settings
    private final UseProvider mUseProvider;
    private final long mMinTimeFilter;
    private final long mMinTimeGpsProvider;
    private final long mMinTimeNetProvider;
    private final LocationListener mClientLocationListener;
    private final boolean mForwardProviderUpdates;
    private final boolean mEnableFilter;

    // Thread
    private Looper mLooper;
    private Handler mOwnHandler;
    private Location mLastLocation;
    private boolean mPredicted;

    /**
     * Three 1-dimension trackers, since the dimensions are independent and can avoid using matrices.
     */
    private Tracker1D mLatitudeTracker, mLongitudeTracker, mAltitudeTracker;

    /**
     *
     * @param context
     * @param useProvider
     * @param minTimeFilter
     * @param minTimeGpsProvider
     * @param minTimeNetProvider
     * @param locationListener
     * @param forwardProviderUpdates
     */
    LooperThread(
            Context context,
            UseProvider useProvider,
            long minTimeFilter,
            long minTimeGpsProvider,
            long minTimeNetProvider,
            GoogleApiClient googleApiClient,
            LocationRequest locationRequest,
            LocationListener locationListener,
            boolean forwardProviderUpdates,
            boolean enableFilter) {
        mContext = context;
        mClientHandler = new Handler();
      //  mLocationManager = (LocationManager) mContext.getSystemService(Context.LOCATION_SERVICE);

        mUseProvider = useProvider;

        mMinTimeFilter = minTimeFilter;
        mMinTimeGpsProvider = minTimeGpsProvider;
        mMinTimeNetProvider = minTimeNetProvider;

        mGoogleApiClient = googleApiClient;
        mLocationRequest = locationRequest;

        mClientLocationListener = locationListener;
        mForwardProviderUpdates = forwardProviderUpdates;
        mEnableFilter = enableFilter;

        start();
    }

    @Override
    public void run() {

        setPriority(THREAD_PRIORITY);

        Looper.prepare();
        mLooper = Looper.myLooper();

        if (mUseProvider == UseProvider.GPS || mUseProvider == UseProvider.GPS_AND_NET) {

            if (ActivityCompat.checkSelfPermission(mContext, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(mContext, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                // TODO: Consider calling
                //    ActivityCompat#requestPermissions
                // here to request the missing permissions, and then overriding
                //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                //                                          int[] grantResults)
                // to handle the case where the user grants the permission. See the documentation
                // for ActivityCompat#requestPermissions for more details.
                return;
            }

            /*mLocationManager.requestLocationUpdates(
                    LocationManager.GPS_PROVIDER, mMinTimeGpsProvider, 0.0f, mOwnLocationListener, mLooper);*/

            LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, mOwnLocationListener);
        }

        if (mUseProvider == UseProvider.NET || mUseProvider == UseProvider.GPS_AND_NET) {

            LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, mOwnLocationListener);
        }

        Looper.loop();
    }

    public void close() {
        if (ActivityCompat.checkSelfPermission(mContext, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(mContext, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }

        //mLocationManager.removeUpdates(mOwnLocationListener);
        LocationServices.FusedLocationApi.removeLocationUpdates(
                mGoogleApiClient, mOwnLocationListener);

        mLooper.quit();
    }

    private LocationListener mOwnLocationListener = new LocationListener() {

        @Override
        public void onLocationChanged(final Location location) {

            // Reusable
            final double accuracy = location.getAccuracy();
            double position, noise;

            // Latitude
            position = location.getLatitude();
            noise = accuracy * METER_TO_DEG;

            if (mLatitudeTracker == null) {

                mLatitudeTracker = new Tracker1D(TIME_STEP, COORDINATE_NOISE);
                mLatitudeTracker.setState(position, 0.0, noise);
            }

            if (!mPredicted)
                mLatitudeTracker.predict(0.0);

            mLatitudeTracker.update(position, noise);

            // Longitude
            position = location.getLongitude();
            noise = accuracy * Math.cos(Math.toRadians(location.getLatitude())) * METER_TO_DEG ;

            if (mLongitudeTracker == null) {

                mLongitudeTracker = new Tracker1D(TIME_STEP, COORDINATE_NOISE);
                mLongitudeTracker.setState(position, 0.0, noise);
            }

            if (!mPredicted)
                mLongitudeTracker.predict(0.0);

            mLongitudeTracker.update(position, noise);

            // Altitude
            if (location.hasAltitude()) {

                position = location.getAltitude();
                noise = accuracy;

                if (mAltitudeTracker == null) {

                    mAltitudeTracker = new Tracker1D(TIME_STEP, ALTITUDE_NOISE);
                    mAltitudeTracker.setState(position, 0.0, noise);
                }

                if (!mPredicted)
                    mAltitudeTracker.predict(0.0);

                mAltitudeTracker.update(position, noise);
            }

            // Reset predicted flag
            mPredicted = false;

            // Forward update if requested
            if (mForwardProviderUpdates) {

                mClientHandler.post(new Runnable() {

                    @Override
                    public void run() {

                        mClientLocationListener.onLocationChanged(new Location(location));
                    }
                });
            }

            // Update last location
            if (location.getProvider().equals(LocationManager.GPS_PROVIDER)
                    || mLastLocation == null || mLastLocation.getProvider().equals(LocationManager.NETWORK_PROVIDER) || location.getProvider().equals("fused")) {

                mLastLocation = new Location(location);
            }

            // Enable filter timer if this is our first measurement
            if (mOwnHandler == null && mEnableFilter) {

                mOwnHandler = new Handler(mLooper, mOwnHandlerCallback);
                mOwnHandler.sendEmptyMessageDelayed(0, mMinTimeFilter);
            }
        }
/*
        @Override
        public void onStatusChanged(String provider, final int status, final Bundle extras) {

            final String finalProvider = provider;

            mClientHandler.post(new Runnable() {

                @Override
                public void run() {

                    mClientLocationListener.onStatusChanged(finalProvider, status, extras);
                }
            });
        }

        @Override
        public void onProviderEnabled(String provider) {

            final String finalProvider = provider;

            mClientHandler.post(new Runnable() {

                @Override
                public void run() {

                    mClientLocationListener.onProviderEnabled(finalProvider);
                }
            });
        }

        @Override
        public void onProviderDisabled(String provider) {

            final String finalProvider = provider;

            mClientHandler.post(new Runnable() {

                @Override
                public void run() {

                    mClientLocationListener.onProviderDisabled(finalProvider);
                }
            });
        }*/
    };



    private Handler.Callback mOwnHandlerCallback = new Handler.Callback() {

        @Override
        public boolean handleMessage(Message msg) {

            // Prepare location
            final Location location = new Location(KALMAN_PROVIDER);

            // Latitude
            mLatitudeTracker.predict(0.0);
            location.setLatitude(mLatitudeTracker.getPosition());

            // Longitude
            mLongitudeTracker.predict(0.0);
            location.setLongitude(mLongitudeTracker.getPosition());

            // Altitude
            if (mLastLocation.hasAltitude()) {

                mAltitudeTracker.predict(0.0);
                location.setAltitude(mAltitudeTracker.getPosition());
            }

            // Speed
            if (mLastLocation.hasSpeed())
                location.setSpeed(mLastLocation.getSpeed());

            // Bearing
            if (mLastLocation.hasBearing())
                location.setBearing(mLastLocation.getBearing());

            // Accuracy (always has)
            location.setAccuracy((float) (mLatitudeTracker.getAccuracy() * DEG_TO_METER));

            // Set times
            location.setTime(System.currentTimeMillis());

            if (Build.VERSION.SDK_INT >= 17)
                location.setElapsedRealtimeNanos(SystemClock.elapsedRealtimeNanos());

            // Post the update in the client (UI) thread
            mClientHandler.post(new Runnable() {

                @Override
                public void run() {

                    mClientLocationListener.onLocationChanged(location);
                }
            });

            // Enqueue next prediction
            mOwnHandler.removeMessages(0);
            mOwnHandler.sendEmptyMessageDelayed(0, mMinTimeFilter);
            mPredicted = true;

            return true;
        }
    };
}
