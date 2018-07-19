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

import com.google.android.glass.touchpad.Gesture;
import com.google.android.glass.touchpad.GestureDetector;

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
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.TextView;
import android.widget.ViewFlipper;

/**
 * An abstract implementation of the USG-client's user interface. This handles functionality shared
 * between the main application and the tutorial, such as displaying the battery level at the bottom
 * of the screen and animations between pages. It is up to subclasses to provide the data model and
 * map gestures to the appropriate logic.
 */
public abstract class BaseActivity extends Activity {

    /**
     * The amount of time to leave the previous view on screen before advancing.
     */
    private static final long PAGE_CHANGE_DELAY_MILLIS = 500;

    /**
     * The Unicode character for the filled parallelogram, representing charged battery part.
     * Not working on Glass
     */
    private static final char BATTERY_FILLED_PART_CHARACTER = '\u25a0';

    /**
     * Full circle character used for network indicator.
     */
    private static final char FULL_CIRCLE_CHARACTER = '\u25cf';

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
            return areGesturesEnabled() && handleGesture(gesture);
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
     * Value that can be updated to enable/disable gesture handling in the application. For example,
     * gestures are disabled briefly when a view is changed, so that the user cannot use gesture
     * again until the animation has completed.
     */
    private boolean gesturesEnabled;

    /**
     * View flipper with two views used to provide the flinging animations between views.
     */
    private ViewFlipper viewFlipper;

    /**
     * TextView containing the bars that represent the battery state.
     */
    private TextView batteryState;

//    /**
//     * TextView containing network transfer indicator.
//     */
//    protected TextView networkIndicatorTextView;

    /**
     * Animation used to briefly tug a view when the user swipes left.
     */
    private Animation tugRightAnimation;
    private boolean isConnected = false;
    int batteryLevel = 0;

    private BroadcastReceiver batteryInfoReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            batteryLevel = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, 0);
//            Log.d(LOG_TAG, "BATTERY LEVEL: " + batteryLevel);
            batteryState.setText(buildBatteryBar());
        }
    };

    /**
     * Current TextView.
     */
    private TextView textView;
    protected TextView gainTextView;
    protected TextView areaTextView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Log.d(LOG_TAG, "Application startup.");

        setContentView(R.layout.usg_session_layout);
        setGesturesEnabled(true);

        audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        gestureDetector = new GestureDetector(this).setBaseListener(mBaseListener);

        viewFlipper = (ViewFlipper) findViewById(R.id.view_flipper);

//        networkIndicatorTextView = (TextView) findViewById(R.id.network_indicator);
//        networkIndicatorTextView.setText(Character.toString(FULL_CIRCLE_CHARACTER));
//        networkIndicatorTextView.setTextColor(Color.DKGRAY);
        gainTextView = (TextView) findViewById(R.id.gain_value);
        areaTextView = (TextView) findViewById(R.id.area_value);
        batteryState = (TextView) findViewById(R.id.battery_state);

        tugRightAnimation = AnimationUtils.loadAnimation(this, R.anim.tug_right);

        this.registerReceiver(
                this.batteryInfoReceiver,
                new IntentFilter(Intent.ACTION_BATTERY_CHANGED));

        updateDisplay();
//        Log.d(LOG_TAG, "Base client activity created");
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

    /**
     * This method flings the parameters page into view.
     */
    protected void goToParameters() {
        // Disable gesture handling so that the user can't tap or swipe during the animation.
        setGesturesEnabled(false);

        mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                viewFlipper.showNext();
                updateDisplay();

                // Re-enable gesture handling after the delay has passed.
                setGesturesEnabled(true);
            }
        }, PAGE_CHANGE_DELAY_MILLIS);
    }

    /**
     * Change main TextView text.
     * @param text The new text to display.
     * @param color Text color.
     * @param size Text size.
     */
    protected void changeMainText(String text, int color, float size, long delayMilis) {
        textView = getCurrentTextView();
        textView.setText(text);
        textView.setTextSize(size);
        textView.setTextColor(color);
        if (delayMilis > 0) {
            mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                textView.setText("");
            }
        }, delayMilis);
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        this.unregisterReceiver(this.batteryInfoReceiver);
    }

    /**
     * Updates the display state.
     */
    private void updateDisplay() {
    }


    /**
     * Builds and returns a spanned string containing colorized battery level status.
     */
    private CharSequence buildBatteryBar() {
        int batteryColor = batteryLevel < 25 ? Color.RED
                : batteryLevel < 50 ? Color.MAGENTA : Color.GREEN;

        SpannableStringBuilder builder = new SpannableStringBuilder();
        for (int i = 0; i < 5; i++) {
            if (i < (batteryLevel + 10) / 20) {
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

    /** Returns the {@code TextView} inside the flipper that is currently on-screen. */
    protected TextView getCurrentTextView() {
        return (TextView) viewFlipper.getCurrentView();
    }

    /** Returns true if gestures should be processed or false if they should be ignored. */
    private boolean areGesturesEnabled() {
        return gesturesEnabled;
    }

    /**
     * Enables gesture handling if {@code enabled} is true, otherwise disables gesture handling.
     * Gestures are temporarily disabled when a freeze is enabled so that extraneous taps and
     * swipes are ignored during freeze.
     */
    private void setGesturesEnabled(boolean enabled) {
        gesturesEnabled = enabled;
    }

    /** Plays a tugging animation that provides feedback when the user tries to swipe backward. */
    private void tugSwipe() {
        viewFlipper.startAnimation(tugRightAnimation);
    }
}
