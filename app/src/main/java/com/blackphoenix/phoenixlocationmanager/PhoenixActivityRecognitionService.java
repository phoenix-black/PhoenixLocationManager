package com.blackphoenix.phoenixlocationmanager;

import android.app.IntentService;
import android.content.Intent;
import android.support.annotation.Nullable;
import android.support.v4.content.LocalBroadcastManager;

import com.blackphoenix.phoenixlocationmanager.utils.PxActivityRecognition;
import com.google.android.gms.location.ActivityRecognitionResult;
import com.google.android.gms.location.DetectedActivity;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.List;

/**
 *
 * Created by Praba on 4/27/2018.
 */

public class PhoenixActivityRecognitionService extends IntentService {


    public PhoenixActivityRecognitionService(){
        this("PhoenixActivityRecognitionService");
    }

    public PhoenixActivityRecognitionService(String name) {
        super(name);
    }

    @Override
    protected void onHandleIntent(@Nullable Intent intent) {
        ActivityRecognitionResult activityRecognitionResult = ActivityRecognitionResult.extractResult(intent);
        handleResult(activityRecognitionResult.getProbableActivities(),
                activityRecognitionResult.getMostProbableActivity());

    }

    private void handleResult(List<DetectedActivity> allProbableActivities,
                              DetectedActivity mostProbableActivity){

        JSONArray probableActivityArray = new JSONArray();

        for(DetectedActivity detectedActivity : allProbableActivities){
            JSONObject activityObject = new JSONObject();
            int confidence = detectedActivity.getConfidence();
            String recognizedActivity = getActivityName(detectedActivity.getType());
            try {
                activityObject.put(PxActivityRecognition.ACTIVITY_CONFIDENCE,confidence);
                activityObject.put(PxActivityRecognition.ACTIVITY_TYPE,recognizedActivity);
            } catch (JSONException e) {
                e.printStackTrace();
                // ToDo : Handle Here
            }
            probableActivityArray.put(activityObject);
        }

        JSONObject mostProbableActivityObject = new JSONObject();
        int confidence = mostProbableActivity.getConfidence();
        String recognizedActivity = getActivityName(mostProbableActivity.getType());
        try {
            mostProbableActivityObject.put(PxActivityRecognition.ACTIVITY_CONFIDENCE,confidence);
            mostProbableActivityObject.put(PxActivityRecognition.ACTIVITY_TYPE,recognizedActivity);
        } catch (JSONException e) {
            e.printStackTrace();
            // ToDo : Handle Here
        }

        Intent intent = new Intent(PxActivityRecognition.BROADCAST_DETECTED_ACTIVITY);
        intent.putExtra(PxActivityRecognition.ALL_PROBABLE_ACTIVITY, ""+probableActivityArray.toString());
        intent.putExtra(PxActivityRecognition.MOST_PROBABLE_ACTIVITY, ""+mostProbableActivityObject.toString());
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);

    }

    private String getActivityName(int activityType){
        switch (activityType){
            case DetectedActivity.IN_VEHICLE:
                return "IN_VEHICLE";
            case DetectedActivity.ON_BICYCLE:
                return "ON_BICYCLE";
            case DetectedActivity.STILL:
                return "STILL";
            case DetectedActivity.TILTING:
                return "TILTING";
            case DetectedActivity.RUNNING:
                return "RUNNING";
            case DetectedActivity.ON_FOOT:
                return "ON_FOOT";
            case DetectedActivity.WALKING:
                return "WALKING";
            case DetectedActivity.UNKNOWN:
                return "UNKNOWN";
        }
        return "";
    }


}
