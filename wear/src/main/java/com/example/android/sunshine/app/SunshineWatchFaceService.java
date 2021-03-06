/*
 * Copyright (C) 2014 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.android.sunshine.app;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Rect;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.v4.content.LocalBroadcastManager;
import android.support.wearable.watchface.CanvasWatchFaceService;
import android.support.wearable.watchface.WatchFaceStyle;
import android.util.Log;
import android.view.SurfaceHolder;


import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.wearable.DataItem;
import com.google.android.gms.wearable.DataItemBuffer;
import com.google.android.gms.wearable.DataMap;
import com.google.android.gms.wearable.DataMapItem;
import com.google.android.gms.wearable.Wearable;

import java.util.Calendar;

public class SunshineWatchFaceService extends CanvasWatchFaceService {

    private static final String TAG = SunshineWatchFaceService.class.getSimpleName();

    @Override
    public Engine onCreateEngine() {
        Log.d(TAG, "onCreateEngine");
        return new SunshineWatchFaceServiceEngine();
    }

    private class SunshineWatchFaceServiceEngine extends CanvasWatchFaceService.Engine {

        private final String TAG = CanvasWatchFaceService.Engine.class.getSimpleName();

        private SunshineWatchFace mWatchFace;
        private Handler mClockTick;


        @Override
        public void onCreate(SurfaceHolder holder) {
            Log.d(TAG, "onCreate");
            super.onCreate(holder);

            /* initialize your watch face */
            setWatchFaceStyle(new WatchFaceStyle.Builder(SunshineWatchFaceService.this)
                    .setCardPeekMode(WatchFaceStyle.PEEK_MODE_SHORT)
                    .setAmbientPeekMode(WatchFaceStyle.AMBIENT_PEEK_MODE_HIDDEN)
                    .setBackgroundVisibility(WatchFaceStyle.BACKGROUND_VISIBILITY_INTERRUPTIVE)
                    .setShowSystemUiTime(false)
                    .build());

            mClockTick = new Handler(Looper.myLooper());
            mWatchFace = SunshineWatchFace.newInstance(SunshineWatchFaceService.this);
            startTimerIfNecessary();
        }

        private void startTimerIfNecessary() {
            mClockTick.removeCallbacks(timeRunnable);
            if (isVisible() && !isInAmbientMode()) {
                mClockTick.post(timeRunnable);
            }
        }

        private final Runnable timeRunnable = new Runnable() {
            @Override
            public void run() {
                onSecondTick();
                if (isVisible() && !isInAmbientMode()) {
                    long TICK_PERIOD_MILLIS = Calendar.getInstance().get(Calendar.MILLISECOND);
                    mClockTick.postDelayed(this, TICK_PERIOD_MILLIS);
                }
            }
        };

        // Method to handle when the seconds update
        private void onSecondTick() {
            invalidateIfNecessary();
        }

        private void invalidateIfNecessary() {
            if (isVisible() && !isInAmbientMode()) {
                invalidate();
            }
        }

        @Override
        public void onVisibilityChanged(boolean visible) {
            Log.d(TAG, "onVisibilityChanged()");
            super.onVisibilityChanged(visible);
            if (visible) {
                registerTimeZoneReceiver();
                registerWeatherConditionsReceiver();
            }
            else {
                unregisterTimeZoneReceiver();
                unregisterWeatherConditionsReceiver();
            }
            startTimerIfNecessary();
        }

        private void registerTimeZoneReceiver() {
            IntentFilter timeZoneFilter = new IntentFilter(Intent.ACTION_TIMEZONE_CHANGED);
            registerReceiver(timeZoneChangedReceiver, timeZoneFilter);
        }

        // method to unregister our detected timezone receiver
        private void unregisterTimeZoneReceiver() {
            unregisterReceiver(timeZoneChangedReceiver);
        }

        private void registerWeatherConditionsReceiver() {
            Log.d(TAG, "registerWeatherConditionsReceiver");
            IntentFilter filter = new IntentFilter("com.example.android.sunshine.app.INTENT");
            LocalBroadcastManager.getInstance(getApplicationContext())
                    .registerReceiver(weatherConditionsChangedReceiver,
                            filter);
        }

        private void unregisterWeatherConditionsReceiver() {
            Log.d(TAG, "unregisterWeatherConditionsReceiver");
            LocalBroadcastManager.getInstance(getApplicationContext())
                    .unregisterReceiver(weatherConditionsChangedReceiver);
        }

        private  BroadcastReceiver weatherConditionsChangedReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                Log.d(TAG, "onReceive()- weather condition data: " + intent.getAction());
                if ("com.example.android.sunshine.app.INTENT".equals(intent.getAction())) {
                    invalidate();
                }
            }
        };

        private BroadcastReceiver timeZoneChangedReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                Log.d(TAG, "On Time Zone Change Received");
                // Timezone Change
                if (Intent.ACTION_TIMEZONE_CHANGED.equals(intent.getAction())) {
                    mWatchFace.updateTimeZoneWith(intent.getStringExtra("time-zone"));
                }
            }
        };

        @Override
        public void onDraw(Canvas canvas, Rect bounds) {
            super.onDraw(canvas, bounds);
            mWatchFace.draw(canvas, bounds);
        }

        @Override
        public void onTimeTick() {
            super.onTimeTick();
            invalidate();
        }

        @Override
        public void onAmbientModeChanged(boolean inAmbientMode) {
            super.onAmbientModeChanged(inAmbientMode);
            mWatchFace.setAntiAlias(!inAmbientMode);

            if (!inAmbientMode) {
                mWatchFace.setColor(Color.GREEN, Color.WHITE);
            }
            else mWatchFace.setColor(Color.GRAY, Color.GRAY);
            invalidate();
            startTimerIfNecessary();

        }

        @Override
        public void onDestroy() {
            Log.d(TAG, "onDestroy");
            mClockTick.removeCallbacks(timeRunnable);
            super.onDestroy();
        }
    }
}
