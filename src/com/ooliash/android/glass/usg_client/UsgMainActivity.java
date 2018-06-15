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

import com.google.android.glass.media.Sounds;
import com.google.android.glass.touchpad.Gesture;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.widget.TextView;

/**
 * Implementation of the main activity: transfers video and allows additional gestures.
 */
public class UsgMainActivity extends BaseClientActivity {

    private int numberOfParams = 2;
    private int highlighedParam = 0;

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
        mHandler.removeCallbacks(mTick);
        stopConnectionToUsg();
    }

    /**
     * Overridden to select ten random phrases from the application's resources.
     */
    @Override
    protected ClientModel createClientModel() {
        return new ClientModel();
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
            case TWO_TAP:
                tempText("AREA DOWN");
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
        String timeString = String.format(
            "%d\uee01%02d", sessionTime / 60, sessionTime % 60);
        mTimer.setText(timeString);
    }

    /**
     * Called to finish the USG maintenance activity and display the results screen.
     */
    private void endUsgMaintenance() {
        Intent intent = new Intent(this, SessionResultsActivity.class);
        intent.putExtra(SessionResultsActivity.EXTRA_MODEL, getClientModel());
        startActivity(intent);
        finish();
    }

    /**
     * Gesture callbacks.
     */
    protected void onGestureTap() {
        receiver.SendCommand("FREEZE");
        tempText("FREEZE");
    }

    protected void onGestureSwipeLeft() {
        playSoundEffect(Sounds.TAP);
        receiver.SendCommand("AREA_UP");
//        updateDisplay();
        tempText("AREA UP");
    }

    private void highlightPrevoiusParam() {
        highlighedParam = highlighedParam > 0 ? highlighedParam - 1 : numberOfParams - 1;
    }

    protected void onGestureSwipeRight() {
        playSoundEffect(Sounds.TAP);
//        highlightNextParam();
//        updateDisplay();
        receiver.SendCommand("AREA_DOWN");
        tempText("AREA DOWN");
    }

    private void highlightNextParam() {
        highlighedParam = (highlighedParam + 1) % numberOfParams;
    }

    protected void onGestureSwipeUp() {
        playSoundEffect(Sounds.TAP);
        tempText("GAIN UP");
        receiver.SendCommand("GAIN_UP");
    }

    protected void onGestureSwipeDown() {
        playSoundEffect(Sounds.TAP);
        tempText("GAIN_DOWN");
    }

    private void tempText(String text) {
        changeMainText(text, Color.WHITE, 16.5f);
    }

}