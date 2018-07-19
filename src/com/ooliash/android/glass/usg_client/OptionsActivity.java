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
 * Implementation of the options activity: allows additional options setting.
 */
public class OptionsActivity extends BaseActivity {

    private static final String LOG_TAG = "USG";

    /**
     * Handler used to keep the timer ticking once per second.
     */
    private final Handler mHandler = new Handler();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    protected void onStart() {
        super.onStart();
    }

    @Override
    protected void onStop() {
        super.onStop();
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

    /**
     * Gesture callbacks.
     */
    protected void onGestureTap() {
        permText("FREEZE");
    }

    protected void onGestureSwipeLeft() {
        playSoundEffect(Sounds.TAP);
    }

    protected void onGestureSwipeRight() {
        playSoundEffect(Sounds.TAP);
    }

    protected void onGestureSwipeUp() {
        playSoundEffect(Sounds.TAP);
    }

    protected void onGestureSwipeDown() {
        playSoundEffect(Sounds.TAP);
    }

    private void permText(String text) {
        changeMainText(text, Color.WHITE, 16.5f, 0);
    }

    void errorMessage(String text) {
        changeMainText(text, Color.RED, 16.5f, 2000);
    }
}
