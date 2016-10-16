package com.example.android.sunshine.app;

import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.content.SharedPreferencesCompat;
import android.util.Log;

import com.google.android.gms.wearable.Asset;
import com.google.android.gms.wearable.DataEvent;
import com.google.android.gms.wearable.DataEventBuffer;
import com.google.android.gms.wearable.DataMap;
import com.google.android.gms.wearable.DataMapItem;
import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.WearableListenerService;

import java.util.Random;

public class SunshineDataListenerService extends WearableListenerService {
    public SunshineDataListenerService() {
    }

    private static final String WEATHER_DATA_PATH = "/weather_data";
    private static final String TAG = SunshineDataListenerService.class.getSimpleName();


    @Override
    public void onMessageReceived(MessageEvent messageEvent) {
        Log.d(TAG, "onMessageReceived: " + messageEvent.getPath());
    }

    @Override
    public void onDataChanged(DataEventBuffer dataEvents) {
        DataMap dataMap;
        for (DataEvent event : dataEvents) {
            Log.d(TAG, "onDataChanged: " + DataMapItem.fromDataItem(event.getDataItem()).getDataMap());
            // Check the data type
            if (event.getType() == DataEvent.TYPE_CHANGED) {
                // Check the data path
                String path = event.getDataItem().getUri().getPath();
                if (path.equals(WEATHER_DATA_PATH)) {}
                dataMap = DataMapItem.fromDataItem(event.getDataItem()).getDataMap();

                SharedPreferences sharedPrefs = PreferenceManager
                        .getDefaultSharedPreferences(this);
                sharedPrefs.edit()
                        .putString("high_temp", dataMap.getString("high_temp"))
                        .putString("low_temp", dataMap.getString("low_temp"))
                        .putInt("weather_id", dataMap.getInt("weather_id"))
                        .apply();

                Intent messageIntent = new Intent();
                messageIntent.setAction("com.example.android.sunshine.app.INTENT");
                messageIntent.putExtra("datamap", dataMap.toBundle());
                LocalBroadcastManager.getInstance(this).sendBroadcast(messageIntent);
            }
        }
    }

//    private void setWeatherConditions() {
//        Log.d(TAG, "setWeatherConditions");
//        SharedPreferences sharedPrefs = PreferenceManager
//                .getDefaultSharedPreferences(this);
//        sharedPrefs.edit()
//                .putString("high_temp", randomTemp())
//                .putString("low_temp", randomTemp())
//                .putInt("weather_id", randomId())
//                .apply();
//    }
}
