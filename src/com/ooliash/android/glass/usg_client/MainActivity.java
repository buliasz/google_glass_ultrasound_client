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
import com.google.android.glass.touchpad.GestureDetector;
import com.google.android.glass.view.WindowUtils;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.Window;
import android.view.WindowManager;

/**
 * The initial splash screen activity in the application that displays a "Start transfer" prompt and
 * allows the user to tap to access the instructions.
 */
public class MainActivity extends Activity {

    private static final String LOG_TAG = "USG";

    /**
     * Handler used to post requests to start new activities so that the menu closing animation
     * works properly.
     */
    private final Handler mHandler = new Handler();

    /** Listener that displays the options menu when the touchpad is tapped. */
    private final GestureDetector.BaseListener mBaseListener = new GestureDetector.BaseListener() {
        @Override
        public boolean onGesture(Gesture gesture) {
            if (gesture == Gesture.TAP) {
                mAudioManager.playSoundEffect(Sounds.TAP);
                openOptionsMenu();
                return true;
            }
            return false;
        }
    };

    /** Audio manager used to play system sound effects. */
    private AudioManager mAudioManager;

    /** Gesture detector used to present the options menu. */
    private GestureDetector mGestureDetector;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getWindow().requestFeature(WindowUtils.FEATURE_VOICE_COMMANDS);

        mAudioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        mGestureDetector = new GestureDetector(this).setBaseListener(mBaseListener);

        setContentView(R.layout.start_usg_app_layout);
    }

    @Override
    public boolean onGenericMotionEvent(MotionEvent event) {
        return mGestureDetector.onMotionEvent(event);
    }

    @Override
    public boolean onCreatePanelMenu(int featureId, Menu menu) {
        if (featureId == WindowUtils.FEATURE_VOICE_COMMANDS || featureId == Window.FEATURE_OPTIONS_PANEL) {
            getMenuInflater().inflate(R.menu.main_menu, menu);
            return true;
        }
        return super.onCreatePanelMenu(featureId, menu);
    }

    /**
     * Implementation of {@link Window.Callback#onMenuItemSelected} for activities.  This calls
     * through to the new {@link #onOptionsItemSelected} method for the
     * {@link Window#FEATURE_OPTIONS_PANEL} panel, so that subclasses of Activity don't need to deal
     * with feature codes.
     *
     * @param featureId ID of the feature calling this method.
     * @param item  Item selected.
     */
    @Override
    public boolean onMenuItemSelected(int featureId, MenuItem item) {
        if (featureId == WindowUtils.FEATURE_VOICE_COMMANDS || featureId == Window.FEATURE_OPTIONS_PANEL) {
            switch (item.getItemId()) {
                /*
                 * The act of starting an activity here is wrapped inside a posted {@code Runnable} to avoid
                 * animation problems between the closing menu and the new activity. The post ensures that the
                 * menu gets the chance to slide down off the screen before the activity is started.
                 * The startXXX() methods start a new activity, and if we call them directly here then
                 * the new activity will start without giving the menu a chance to slide back down first.
                 * By posting the calls to a handler instead, they will be processed on an upcoming onGestureSwipeRight
                 * through the message queue, after the animation has completed, which results in a
                 * smoother transition between activities.
                 */
                case R.id.new_session:
                    Log.d(LOG_TAG, "Voice: New session");
                    mHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            startNewSession();
                        }
                    });
                    return true;
                case R.id.options:
                    Log.d(LOG_TAG, "Voice: Options");
                    mHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            startOptions();
                        }
                    });
                    return true;
                case R.id.instructions:
                    Log.d(LOG_TAG, "Voice: Instructions");
                    mHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            startInstructions();
                        }
                    });
                    return true;
                case R.id.exit:
                    Log.d(LOG_TAG, "Voice: Exit");
                    finish();
                    return true;
            }
            Log.e(LOG_TAG, "Voice: NO CASE");
        }
        Log.d(LOG_TAG, "No voice");
        return super.onMenuItemSelected(featureId, item);
    }

    /**
     * Starts the main video transfer activity, and finishes this activity so that the user is not returned
     * to the splash screen when they exit.
     */
    private void startNewSession() {
        startActivity(new Intent(this, UsgSessionActivity.class));
    }

    /**
     * Starts the additional options configuration.
     */
    private void startOptions() {
        startActivity(new Intent(this, OptionsActivity.class));
    }

    /**
     * Starts the tutorial activity, but does not finish this activity so that the splash screen
     * reappears when the tutorial is over.
     */
    private void startInstructions() {
        startActivity(new Intent(this, InstructionsActivity.class));
    }
}
