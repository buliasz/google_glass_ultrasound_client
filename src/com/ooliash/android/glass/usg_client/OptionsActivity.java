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
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.widget.TextView;

import com.google.android.glass.media.Sounds;
import com.google.android.glass.touchpad.Gesture;

import java.util.Locale;

/**
 * Implementation of the main activity: transfers video and allows additional gestures.
 */
public class OptionsActivity extends BaseActivity {

    private static final String LOG_TAG = "USG";
    private int numberOfParams = 2;
    private int highlightedParam = 0;

    /**
     * Handler used to keep the timer ticking once per second.
     */
    private final Handler mHandler = new Handler();

    /**
     * Runner that is called once per second during the transfer to show diagnosis time.
     */
    private final Runnable mTick = new Runnable() {
        @Override
        public void run() {
            sessionTime += 1;
            updateTimer();
            nextTick();
        }
    };

    /**
     * Keeps track of number of seconds of diagnosis.
     */
    private int sessionTime;

    /**
     * TextView that displays the current diagnosis time.
     */
    private TextView mTimer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mTimer = (TextView) findViewById(R.id.timer);
    }

    @Override
    protected void onStart() {
        super.onStart();
        sessionTime = 0;
        updateTimer();
        nextTick();
        startConnectionToUsg();
    }

    @Override
    protected void onStop() {
        super.onStop();
        cancelUsgCommunicationTask();
        mHandler.removeCallbacks(mTick);
    }

    @Override
    protected boolean handleGesture(Gesture gesture) {
        switch (gesture) {
            case TAP:
                onGestureTap();
                break;
            case SWIPE_LEFT:
                onGestureSwipeLeft();
                break;
            case SWIPE_RIGHT:
                onGestureSwipeRight();
                break;
            case SWIPE_UP:
                onGestureSwipeUp();
                break;
            case SWIPE_DOWN:
                onGestureSwipeDown();
                break;
            case TWO_LONG_PRESS:
                permText("EXITING...");
                this.finish();
                break;
            default:
                return false;
        }
        return true;
    }

    /** Enqueues the next timer tick into the message queue after one second. */
    private void nextTick() {
        mHandler.postDelayed(mTick, 1000);
    }

    /** Updates the timer display with the current number of seconds remaining. */
    private void updateTimer() {
        // The code point U+EE01 in Roboto is the vertically centered colon used in the clock on
        // the Glass home screen.
        String timeString = String.format(Locale.ENGLISH,
            "%d\uee01%02d", sessionTime / 60, sessionTime % 60);
        mTimer.setText(timeString);
    }

    /**
     * Gesture callbacks.
     */
    protected void onGestureTap() {
        permText("FREEZE");
    }

    protected void onGestureSwipeLeft() {
        playSoundEffect(Sounds.TAP);
//        updateDisplay();
        permText("AREA UP");
    }

    private void highlightPrevoiusParam() {
        highlightedParam = highlightedParam > 0 ? highlightedParam - 1 : numberOfParams - 1;
    }

    protected void onGestureSwipeRight() {
        playSoundEffect(Sounds.TAP);
        permText("AREA DOWN");
    }

    private void highlightNextParam() {
        highlightedParam = (highlightedParam + 1) % numberOfParams;
    }

    protected void onGestureSwipeUp() {
        playSoundEffect(Sounds.TAP);
        permText("GAIN UP");
    }

    protected void onGestureSwipeDown() {
        playSoundEffect(Sounds.TAP);
        permText("GAIN_DOWN");
    }

    private void permText(String text) {
        changeMainText(text, Color.WHITE, 16.5f, 0);
    }

    void errorMessage(String text) {
        changeMainText(text, Color.RED, 16.5f, 2000);
    }


    protected void startConnectionToUsg() {
        Log.d(LOG_TAG, "Starting communication task...");
        changeMainText(
                "Connecting to PJA USG...",
                Color.RED,
                26.5f,
                0);
    }

    protected void cancelUsgCommunicationTask() {
            Log.e(LOG_TAG, "I'm not connected to any USG.");
    }
}
