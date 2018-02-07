package com.blackphoenix.phoenixlocationmanager.listeners;

import android.location.Location;

/**
 * Created by Praba on 2/7/2018.
 *
 */
public interface PxLocationListener {
  /*  void onFilteredLocationChanged(Location location);*/
    void onRawLocationChanged(Location location);
}
