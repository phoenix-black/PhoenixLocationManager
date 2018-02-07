package com.blackphoenix.phoenixlocationmanager.listeners;

import com.blackphoenix.phoenixlocationmanager.utils.FilterError;

/**
 * Created by Praba on 2/7/2018.
 *
 */

public interface PxFilterCallbacks {
    void onStatusUpdated(String statusMessage);
    void onInitialAccuracyAchieved();
    void onStabilityAchieved();
    void onFilterError(FilterError filterError);
    void onTimeOut();
}
