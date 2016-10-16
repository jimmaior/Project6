package com.example.android.sunshine.app;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;


import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.wearable.DataItem;
import com.google.android.gms.wearable.DataItemBuffer;
import com.google.android.gms.wearable.DataMap;
import com.google.android.gms.wearable.DataMapItem;
import com.google.android.gms.wearable.Wearable;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.TimeZone;

/**
 * Created by jim on 9/24/2016.
 */

public class SunshineWatchFace implements
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        SharedPreferences.OnSharedPreferenceChangeListener {

    private static final String TAG = SunshineWatchFace.class.getSimpleName();

    private GoogleApiClient mGoogleApiClient;

    private final Paint mTimeObject;
    private final Paint mDateObject;
    private final Paint mLowTempObject;
    private final Paint mHighTempObject;
    private  Bitmap mWeatherIcon;

    private static String mDateText = "0";
    private static String mTimeText = "0";
    private static String mLowTempText = "0";
    private static String mHighTempText = "0";
    private static int mWeatherId = 0;

    private Resources mAppResources;
    private SharedPreferences mSharedPreferences;

    private static final String TIME_FORMAT = "kk:mm";
    private static final String DATE_FORMAT = "MMMM dd";

 //   public static SunshineWatchFace newInstance(Context context) {
    public static SunshineWatchFace newInstance(Context context) {
        Log.d(TAG, "new Instance of sunshine watch");
        Paint mTimeObject = new Paint();
        mTimeObject.setColor(Color.GREEN);
        mTimeObject.setTextSize(55);

        Paint mDateObject = new Paint();
        mDateObject.setColor(Color.WHITE);
        mDateObject.setTextSize(30);

        Paint mLowTempObject = new Paint();
        mLowTempObject.setColor(Color.WHITE);
        mLowTempObject.setTextSize(25);

        Paint mHighTempObject = new Paint();
        mHighTempObject.setColor(Color.WHITE);
        mHighTempObject.setTextSize(25);

        return new SunshineWatchFace(context, mTimeObject, mDateObject, mLowTempObject, mHighTempObject);
    }

    private SunshineWatchFace(Context context, Paint objTime, Paint objDate, Paint objLowTemp, Paint objHighTemp) {
        this.mTimeObject = objTime;
        this.mDateObject = objDate;
        this.mLowTempObject = objLowTemp;
        this.mHighTempObject = objHighTemp;

        mAppResources = context.getResources();
        mSharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);

        mSharedPreferences.registerOnSharedPreferenceChangeListener(this);

       /* client for retrieving synch data from the Wearable Data Layer */
        mGoogleApiClient = new GoogleApiClient.Builder(context)
                .addApi(Wearable.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();
        mGoogleApiClient.connect();
    }

    // Method to update the watch face each time an update has occurred
    public void draw(Canvas canvas, Rect bounds) {
        canvas.drawColor(Color.BLACK);
        mTimeText = new SimpleDateFormat(TIME_FORMAT).format(Calendar.getInstance().getTime());
        mDateText = new SimpleDateFormat(DATE_FORMAT).format(Calendar.getInstance().getTime());

        mHighTempText = mSharedPreferences.getString("high_temp", "");
        mLowTempText = mSharedPreferences.getString("low_temp", "");
        mWeatherId = mSharedPreferences.getInt("weather_id", 0);

        // Date
        float dateXOffset = calcXOffset(mDateText, mDateObject, bounds);
        float dateYOffset = bounds.exactCenterY()/2.0f;
        canvas.drawText(mDateText, dateXOffset, dateYOffset, mDateObject);

        // Time
        float timeXOffset = calcXOffset(mTimeText, mTimeObject, bounds);
        float timeYOffset = calcTimeYOffset(bounds);
        canvas.drawText(mTimeText, timeXOffset, timeYOffset, mTimeObject);

        // Weather Icon
        mWeatherIcon = BitmapFactory
                .decodeResource(mAppResources,
                        getIconResourceForWeatherCondition(mWeatherId) );

        if (mWeatherIcon != null) {
            // icon
            float iconXOffset = calcIconXOffset(mWeatherIcon, bounds);
            float iconYOffset = calcIconYOffset(mWeatherIcon, bounds);
            canvas.drawBitmap(mWeatherIcon, iconXOffset, iconYOffset, null);

            // TODO High Temp
            float highTempXOffset = calcHighTempXOffset(mWeatherIcon, mHighTempText, mHighTempObject, bounds);
            float highTempYOffset = calcHighTempYOffset(mWeatherIcon, bounds);
            canvas.drawText(mHighTempText,
                    highTempXOffset,
                    highTempYOffset,
                    mHighTempObject);

            // TODO Low Temp
            float lowTempXOffset = calcLowTempXOffset(mWeatherIcon, mLowTempText, mLowTempObject, bounds);
            float lowTempYOffset = calcLowTempYOffset(mWeatherIcon, bounds);
            canvas.drawText(mLowTempText,
                    lowTempXOffset,
                    lowTempYOffset,
                    mLowTempObject);
        }
    }

    private float calcHighTempXOffset(Bitmap weatherIcon, String temp, Paint paint, Rect watchBounds ) {
        float centerX = watchBounds.exactCenterX();
        Rect textBounds = new Rect();
        paint.getTextBounds(temp, 0, temp.length(), textBounds);
        return centerX + weatherIcon.getHeight()/2;
    }

    private float calcHighTempYOffset(Bitmap weatherIcon, Rect watchBounds) {
        float centerY = watchBounds.exactCenterY();
        return centerY + weatherIcon.getHeight()/1.5f;
    }

    private float calcLowTempXOffset(Bitmap weatherIcon, String temp, Paint paint, Rect watchBounds) {
        float centerX = watchBounds.exactCenterX();
        Rect textBounds = new Rect();
        paint.getTextBounds(temp, 0, temp.length(), textBounds);
        return centerX + weatherIcon.getHeight()/2;
    }

    private float calcLowTempYOffset(Bitmap weatherIcon, Rect watchBounds) {
        float centerY = watchBounds.exactCenterY();
        return centerY + (1.2f * weatherIcon.getHeight());
    }

    private float calcIconXOffset(Bitmap icon, Rect watchBounds) {
        float centerX = watchBounds.exactCenterX();
        float width = icon.getWidth();
        return centerX - width/1.5f;
    }

    private float calcIconYOffset(Bitmap icon, Rect watchBounds) {
        float centerY = watchBounds.exactCenterY();
        float height = icon.getHeight();
        return centerY + height/3f;
    }

    // calc Time Y-Offset
    private float calcTimeYOffset(Rect watchBounds) {
        return watchBounds.exactCenterY();
    }

    // calc the X-Offset using our Time Label as the offset
    private float calcXOffset(String text, Paint paint, Rect watchBounds) {
        float centerX = watchBounds.exactCenterX();
        float timeLength = paint.measureText(text);
        return centerX - (timeLength / 2.0f);
    }

    public void setAntiAlias(boolean antiAlias) {
        mTimeObject.setAntiAlias(antiAlias);
        mDateObject.setAntiAlias(antiAlias);
    }

    // Set each of our objects colors
    public void setColor(int green, int white) {
        mTimeObject.setColor(green);
        mDateObject.setColor(white);
    }

    // method to get our current timezone and update the time field
    public void updateTimeZoneWith(String timeZone) {
        // Set our default time zone
        TimeZone.setDefault(TimeZone.getTimeZone(timeZone));
        // Get the current time for our current timezone
        mTimeText = new SimpleDateFormat(TIME_FORMAT).format(Calendar.getInstance().getTime());
    }

    public  void updateWeatherConditions() {
        Log.d(TAG, "updateWeatherConditions");
    }

//    private static int getIconResourceForWeatherCondition(int weatherId) {
//        // Based on weather code data found at:
//        // http://bugs.openweathermap.org/projects/api/wiki/Weather_Condition_Codes
//        if (weatherId >= 200 && weatherId <= 232) {
//            return R.drawable.ic_storm;
//        } else if (weatherId >= 300 && weatherId <= 321) {
//            return R.drawable.ic_light_rain;
//        } else if (weatherId >= 500 && weatherId <= 504) {
//            return R.drawable.ic_rain;
//        } else if (weatherId == 511) {
//            return R.drawable.ic_snow;
//        } else if (weatherId >= 520 && weatherId <= 531) {
//            return R.drawable.ic_rain;
//        } else if (weatherId >= 600 && weatherId <= 622) {
//            return R.drawable.ic_snow;
//        } else if (weatherId >= 701 && weatherId <= 761) {
//            return R.drawable.ic_fog;
//        } else if (weatherId == 761 || weatherId == 781) {
//            return R.drawable.ic_storm;
//        } else if (weatherId == 800) {
//            return R.drawable.ic_clear;
//        } else if (weatherId == 801) {
//            return R.drawable.ic_light_clouds;
//        } else if (weatherId >= 802 && weatherId <= 804) {
//            return R.drawable.ic_cloudy;
//        }
//        return -1;
//    }
    
    private static int getIconResourceForWeatherCondition(int weatherId) {
        // Based on weather code data found at:
        // http://bugs.openweathermap.org/projects/api/wiki/Weather_Condition_Codes
//        Log.d(TAG, "getIconResourceForWeatherCondition: " + weatherId );
        if (weatherId >= 0 && weatherId <= 299) {
            return R.drawable.ic_storm;
        } else if (weatherId >= 300 && weatherId <= 399) {
            return R.drawable.ic_light_rain;
        } else if (weatherId >= 400 && weatherId <= 499) {
            return R.drawable.ic_rain;
        } else if (weatherId == 511) {
            return R.drawable.ic_snow;
        } else if (weatherId >= 500 && weatherId <= 599) {
            return R.drawable.ic_rain;
        } else if (weatherId >= 600 && weatherId <= 699) {
            return R.drawable.ic_snow;
        } else if (weatherId >= 700 && weatherId <= 799) {
            return R.drawable.ic_fog;
        } else if (weatherId == 800 || weatherId == 899) {
            return R.drawable.ic_storm;
        } else if (weatherId >= 900 && weatherId <= 999) {
            return R.drawable.ic_cloudy;
        }
        return -1;
    }

    @Override
    public void onConnected(Bundle bundle) {
        Log.d(TAG, "onConnected");
        Wearable.DataApi
                .getDataItems(mGoogleApiClient)
                .setResultCallback(onConnectedResultCallback);
    }

    private final ResultCallback<DataItemBuffer>
            onConnectedResultCallback = new ResultCallback<DataItemBuffer>() {
        @Override
        public void onResult(DataItemBuffer dataItems) {
            Log.d(TAG, "onResult");
            for (DataItem item : dataItems) {
                setWeatherData(item);
            }
            dataItems.release();
        }
    };

    @Override
    public void onConnectionSuspended(int i) { }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) { }

    private void setWeatherData(DataItem item) {
        Log.d(TAG, "setWeatherData()");
        String highTemp = "";
        String lowTemp = "";
        int weatherId = 0;

        if ((item.getUri().getPath()).equals("/weather_data")) {
            DataMap dataMap = DataMapItem.fromDataItem(item).getDataMap();

            if (dataMap.containsKey("high_temp")) {
                highTemp = dataMap.get("high_temp");
                Log.d(TAG, "highTemp:" + mHighTempText);
            }
            if (dataMap.containsKey("low_temp")) {
                lowTemp = dataMap.get("low_temp");
                Log.d(TAG, "lowTemp:" + mLowTempText);
            }
            if (dataMap.containsKey("weather_id")) {
                weatherId = dataMap.getInt("weather_id");
                Log.d(TAG, "weather_id:" + mWeatherId);
            }
        }
        mSharedPreferences.edit()
                .putString("high_temp", highTemp)
                .putString("low_temp", lowTemp)
                .putInt("weather_id", weatherId)
                .apply();
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        Log.d(TAG, "onSharedPreferenceChanged");
    }
}