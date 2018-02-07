package com.blackphoenix.phoenixlocationmanager.listeners;

import android.os.Bundle;

import com.google.android.gms.common.ConnectionResult;

/**
 * Created by Praba on 2/7/2018.
 *
 */
public interface PxConnectionCallbacks {
    void onAPIConnected(Bundle bundle);
    void onAPIConnectionSuspended(int i);
    void onAPIConnectionFailed(ConnectionResult connectionResult);
}
