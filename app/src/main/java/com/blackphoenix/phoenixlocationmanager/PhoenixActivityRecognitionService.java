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
                return "0";
            case DetectedActivity.ON_BICYCLE:
                return "1";
            case DetectedActivity.STILL:
                return "2";
            case DetectedActivity.TILTING:
                return "3";
            case DetectedActivity.RUNNING:
                return "4";
            case DetectedActivity.ON_FOOT:
                return "5";
            case DetectedActivity.WALKING:
                return "6";
            case DetectedActivity.UNKNOWN:
                return "7";
        }
        return "-1";
    }


}
