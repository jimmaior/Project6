package com.example.android.sunshine.app.wearable;

import android.app.Service;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;
import android.widget.Toast;


import com.example.android.sunshine.app.Utility;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.wearable.Asset;
import com.google.android.gms.wearable.DataApi;
import com.google.android.gms.wearable.DataMap;
import com.google.android.gms.wearable.PutDataMapRequest;
import com.google.android.gms.wearable.PutDataRequest;
import com.google.android.gms.wearable.Wearable;

import java.io.ByteArrayOutputStream;

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
    private static Bitmap mWeatherConditionBitmap;

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
        // We want this service to continue running until it is explicitly
        // stopped, so return sticky.
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
        syncWeatherData();
        // get the min, max temp, icon from teh content provider
        // send the data to the data service

        // terminate the service
        stopSelf();
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
        // TODO: get data from provider
        mHighTemp = "85";
        mLowTemp = "70";
        mWeatherId = 800;
        int resource = Utility.getIconResourceForWeatherCondition(800);
        mWeatherConditionBitmap = BitmapFactory.decodeResource(getResources(), resource);
    }

    private void syncWeatherData() {
        DataMap dataMap = new DataMap();
        // todo: defensive programming
        Asset bmpAsset = createAssetFromBitmap(mWeatherConditionBitmap);
        dataMap.putAsset("weather_icon", bmpAsset);
        dataMap.putString("low_temp", mLowTemp);
        dataMap.putString("high_temp", mHighTemp);
        new SendToDataLayerThread(dataMap).start();
    }

    private static Asset createAssetFromBitmap(Bitmap bitmap) {
        final ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, byteStream);
        return Asset.createFromBytes(byteStream.toByteArray());
    }

    // TODO: Rename
    private class SendToDataLayerThread extends Thread {
        DataMap mDataMap;

        SendToDataLayerThread(DataMap dataMap) {
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
                Log.d(TAG, "DataMap: " + mDataMap + " sent successfully to data layer ");
            }
            else {
                // Log an error
                Log.d(TAG, "ERROR: failed to send DataMap to data layer");
            }
        }
    }
}
