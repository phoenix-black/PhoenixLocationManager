package com.blackphoenix.phoenixlocationmanager.listeners;

import org.json.JSONArray;
import org.json.JSONObject;

/**
 * Created by Praba on 4/27/2018.
 */

public interface PxActivityRecognitionListener {
    void onActivityRecognized(JSONArray probableActivityArray, JSONObject mostProbableActivity);
}
