/*
 * Copyright (C) 2018 Bartlomiej Uliasz
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

package com.ooliash.android.glass.usg_client;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.media.AudioManager;
import android.os.BatteryManager;
import android.os.Bundle;
import android.os.Handler;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.util.Log;
import android.view.MotionEvent;
import android.widget.TextView;

import com.google.android.glass.touchpad.Gesture;
import com.google.android.glass.touchpad.GestureDetector;

/**
 * An abstract implementation of the USG-client's user interface. This handles functionality shared
 * between the main application and the tutorial, such as displaying the battery level at the bottom
 * of the screen and animations between pages. It is up to subclasses to provide the data model and
 * map gestures to the appropriate logic.
 */
public abstract class BaseActivity extends Activity {
    /**
     * The Unicode character for the filled parallelogram, representing charged battery part.
     * Not working on Glass
     */
    private static final char BATTERY_FILLED_PART_CHARACTER = '\u25a0';

    /**
     * The Unicode character for the empty parallelogram, representing empty battery part.
     */
    private static final char BATTERY_EMPTY_PART_CHARACTER = '\u25a1';

    /**
     * Logging tag.
     */
    private static final String LOG_TAG = "USG";

    /**
     * Handler used to post a delayed animation when a view is changed.
     */
    private final Handler mHandler = new Handler();

    /**
     * Listener for in-application tap and swipe gestures.
     */
    private final GestureDetector.BaseListener mBaseListener = new GestureDetector.BaseListener() {
        @Override
        public boolean onGesture(Gesture gesture) {
            return handleGesture(gesture);
        }
    };

    /**
     * Audio manager used to play system sound effects.
     */
    protected AudioManager audioManager;

    /**
     * Detects gestures during the application.
     */
    private GestureDetector gestureDetector;

    /**
     * TextView containing the bars that represent the battery state.
     */
    private TextView batteryState;

    int lastBatteryLevel = 0;

    private BroadcastReceiver batteryInfoReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            int batteryLevel = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, 0);
            if (batteryLevel != lastBatteryLevel) {
                lastBatteryLevel = batteryLevel;
                batteryState.setText(buildBatteryBar());
            }
        }
    };

    /**
     * Current TextView.
     */
    TextView textView;
    protected TextView gainTextView;
    protected TextView areaTextView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Log.d(LOG_TAG, "Application startup.");

        setContentView(R.layout.usg_session_layout);

        audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        gestureDetector = new GestureDetector(this).setBaseListener(mBaseListener);

        textView = (TextView) findViewById(R.id.session_main_view);

        gainTextView = (TextView) findViewById(R.id.gain_value);
        areaTextView = (TextView) findViewById(R.id.area_value);
        batteryState = (TextView) findViewById(R.id.battery_state);

        this.registerReceiver(
                this.batteryInfoReceiver,
                new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
    }

    @Override
    public boolean onGenericMotionEvent(MotionEvent event) {
        return gestureDetector.onMotionEvent(event);
    }

    /**
     * Subclasses must override this method to handle {@link Gesture#TAP} and
     * {@link Gesture#SWIPE_RIGHT} gestures that occur during application.
     */
    protected abstract boolean handleGesture(Gesture gesture);

    /** Plays the sound effect of the specified type. */
    protected void playSoundEffect(int effectType) {
        audioManager.playSoundEffect(effectType);
    }

    @Override
    protected void onStop() {
        super.onStop();
        this.unregisterReceiver(this.batteryInfoReceiver);
    }

    /**
     * Builds and returns a spanned string containing colorized battery level status.
     */
    private CharSequence buildBatteryBar() {
        int batteryColor = lastBatteryLevel < 25 ? Color.RED
                : lastBatteryLevel < 50 ? Color.MAGENTA : Color.GREEN;

        SpannableStringBuilder builder = new SpannableStringBuilder();
        for (int i = 0; i < 5; i++) {
            if (i < (lastBatteryLevel + 10) / 20) {
                builder.append(BATTERY_FILLED_PART_CHARACTER);
                builder.setSpan(
                        new ForegroundColorSpan(batteryColor),
                        builder.length() - 1,
                        builder.length(),
                        Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            } else {
                builder.append(BATTERY_EMPTY_PART_CHARACTER);
                builder.setSpan(
                        new ForegroundColorSpan(batteryColor),
                        builder.length() - 1,
                        builder.length(),
                        Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            }
        }
        return builder;
    }
}
