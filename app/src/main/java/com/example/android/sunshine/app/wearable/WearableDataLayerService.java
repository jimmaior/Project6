package com.example.android.sunshine.app.wearable;

import android.app.Service;
import android.content.Intent;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;
import android.widget.Toast;


import com.example.android.sunshine.app.Utility;
import com.example.android.sunshine.app.data.WeatherContract;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.wearable.DataApi;
import com.google.android.gms.wearable.DataMap;
import com.google.android.gms.wearable.PutDataMapRequest;
import com.google.android.gms.wearable.PutDataRequest;
import com.google.android.gms.wearable.Wearable;

import java.util.Random;

public class WearableDataLayerService extends Service implements
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener {

    // class members
    private GoogleApiClient mGoogleApiClient;
    private static final String TAG = WearableDataLayerService.class.getSimpleName();
    private static final String WEATHER_DATA_PATH = "/weather_data";
    private static String mLowTemp;
    private static String mHighTemp;
    private static int mWeatherId;

    public WearableDataLayerService() {
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "onCreate");
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addApi(Wearable.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();
    }

    @Override
    public void onDestroy() {
        // Cancel the persistent notification.
        Log.d(TAG, "onDestroy");

    }


    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "onStartCommand");
        handleCommand(intent);
        return START_NOT_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public void onConnected(@Nullable Bundle connectionHint) {
        Log.d(TAG, "onConnected: " + connectionHint);
        // Now you can use the Data Layer API
        getDataFromProvider();
        sendWeatherDataToDataLayer();
        // get the min, max temp, icon from teh content provider
        // send the data to the data service

    }

    @Override
    public void onConnectionSuspended(int cause) {
        Log.d(TAG, "onConnectionSuspended: " + cause);
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        Log.d(TAG, "onConnectionFailed: " + connectionResult);
        if (connectionResult.getErrorCode() == ConnectionResult.API_UNAVAILABLE) {
            Toast.makeText(getApplicationContext(),
                    "The Wearable API is unavailable", Toast.LENGTH_SHORT).show();
        }
    }

    // private methods
    private void handleCommand(Intent intent) {
        Log.d(TAG, "handleCommand: " + intent.getDataString());
        mGoogleApiClient.connect();
    }

    private void getDataFromProvider() {
        String[] forecastCols = {
                WeatherContract.WeatherEntry.TABLE_NAME + "." + WeatherContract.WeatherEntry._ID,
                WeatherContract.WeatherEntry.COLUMN_DATE,
                WeatherContract.WeatherEntry.COLUMN_SHORT_DESC,
                WeatherContract.WeatherEntry.COLUMN_MAX_TEMP,
                WeatherContract.WeatherEntry.COLUMN_MIN_TEMP,
                WeatherContract.LocationEntry.COLUMN_LOCATION_SETTING,
                WeatherContract.WeatherEntry.COLUMN_WEATHER_ID,
                WeatherContract.LocationEntry.COLUMN_COORD_LAT,
                WeatherContract.LocationEntry.COLUMN_COORD_LONG
        };

        String location = Utility.getPreferredLocation(this);
        Uri weatherForLocationUri = WeatherContract.WeatherEntry.buildWeatherLocationWithStartDate(
                location, System.currentTimeMillis());
        Cursor cursor = getContentResolver().query(weatherForLocationUri, forecastCols, null,
                null, WeatherContract.WeatherEntry.COLUMN_DATE + " ASC");

        if (cursor != null) {
            cursor.moveToLast();

            double low = cursor.getDouble(4);
            String lowString = Utility.formatTemperature(getApplicationContext(), low);
            mLowTemp = lowString;

            double high = cursor.getDouble(3);
            String highString = Utility.formatTemperature(getApplicationContext(), high);
            mHighTemp = highString;

            int weatherId = cursor.getInt(6);
            mWeatherId = weatherId;

            cursor.close();
        }
    }

    private void sendWeatherDataToDataLayer() {
        Log.d(TAG, "sendWeatherDataToDataLayer()");
        DataMap dataMap = new DataMap();
        dataMap.putInt("weather_id", mWeatherId );
        dataMap.putString("low_temp", mLowTemp);
        dataMap.putString("high_temp", mHighTemp);
        new syncWeatherConditionChange(dataMap).start();
    }

    private class syncWeatherConditionChange extends Thread {
        DataMap mDataMap;

        syncWeatherConditionChange(DataMap dataMap) {
            mDataMap = dataMap;
        }

        public void run() {
            // Construct a DataRequest and send over the data layer
            PutDataMapRequest putDMR = PutDataMapRequest.create(WEATHER_DATA_PATH);
            putDMR.getDataMap().putAll(mDataMap);
            PutDataRequest request = putDMR.asPutDataRequest();
            DataApi.DataItemResult result =
                    Wearable.DataApi.putDataItem(mGoogleApiClient, request).await();
            if (result.getStatus().isSuccess()) {
                Log.d(TAG, "Weather update:" + mDataMap + " is successful");
            }
            else {
                // Log an error
                Log.d(TAG, "ERROR: failed to send weather update to Wearable data layer");
            }
            // terminate the service
            stopSelf();
        }
    }
}
