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
import com.google.android.glass.view.WindowUtils;

import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.Window;
import android.view.WindowManager;
import android.widget.TextView;

import java.util.Locale;

/**
 * Implementation of the main activity: transfers video and allows additional gestures.
 */
public class UsgSessionActivity extends BaseActivity {

    private static final String LOG_TAG = "USG";
    private UsgCommunicationTask usgCommunicationTask;

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

//        getWindow().addFlags(WindowUtils.FEATURE_VOICE_COMMANDS);

        usgCommunicationTask = new UsgCommunicationTask(this);
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

    /**
     * Default implementation of
     * {@link Window.Callback#onCreatePanelMenu}
     * for activities.  This calls through to the new
     * {@link #onCreateOptionsMenu} method for the
     * {@link Window#FEATURE_OPTIONS_PANEL} panel,
     * so that subclasses of Activity don't need to deal with feature codes.
     *
     * @param featureId Caller feature ID.
     * @param menu The menu.
     */
    @Override
    public boolean onCreatePanelMenu(int featureId, Menu menu) {
        if (featureId == WindowUtils.FEATURE_VOICE_COMMANDS
                || featureId == Window.FEATURE_OPTIONS_PANEL) {
            getMenuInflater().inflate(R.menu.session_menu, menu);
            return true;
        }
        return super.onCreatePanelMenu(featureId, menu);
    }

    /**
     * Default implementation of
     * {@link Window.Callback#onMenuItemSelected}
     * for activities.  This calls through to the new
     * {@link #onOptionsItemSelected} method for the
     * {@link Window#FEATURE_OPTIONS_PANEL}
     * panel, so that subclasses of
     * Activity don't need to deal with feature codes.
     *
     * @param featureId ID of the caller feature.
     * @param item Menu item selected.
     */
    @Override
    public boolean onMenuItemSelected(int featureId, MenuItem item) {
        if (featureId == WindowUtils.FEATURE_VOICE_COMMANDS
                || featureId == Window.FEATURE_OPTIONS_PANEL) {
            playSoundEffect(Sounds.TAP);
            switch (item.getItemId()) {
                case R.id.freeze:
                    commandFreeze();
                    break;
                case R.id.save_picture:
                    //TODO: Implement save picture.
                    errorMessage("Not implemented yet.");
                    break;
                case R.id.options:
                    //TODO: Implement options menu.
                    errorMessage("Not implemented yet.");
                    break;
                case R.id.close_session:
                    finish();
                    break;
                default:
                    Log.e(LOG_TAG, "Unknown session menu item ID."); // + item.getTitle()
            }
            return true;
        }
        return super.onMenuItemSelected(featureId, item);
    }

    @Override
    protected boolean handleGesture(Gesture gesture) {
        switch (gesture) {
            case TAP:
//                commandFreeze();
                openOptionsMenu();
                break;
            case SWIPE_LEFT:
                commandAreaUp();
                break;
            case SWIPE_RIGHT:
                commandAreaDown();
                break;
            case SWIPE_UP:
                commandGainUp();
                break;
            case SWIPE_DOWN:
                commandGainDown();
                break;
            case TWO_LONG_PRESS:
                permText("EXITING...");
                finish();
                break;
            default:
                return false;
        }
        playSoundEffect(Sounds.TAP);
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
     * Permanent message display.
     * @param text Message to display.
     */
    private void permText(String text) {
        changeMainText(text, Color.WHITE, 16.5f, 0);
    }

    /**
     * Error message to display (red with timeout).
     * @param text Message to display.
     */
    void errorMessage(String text) {
        changeMainText(text, Color.RED, 16.5f, 2000);
    }

    // USG communication methods

    /**
     * Starts new task/connection to USG (cancelling if there's any previously started).
     */
    protected void startConnectionToUsg() {
        Log.d(LOG_TAG, "Starting communication task...");
        permText("Connecting to PJA USG...");
        usgCommunicationTask.execute();
    }

    /**
     * Stops current USG communication task/connection.
     */
    protected void cancelUsgCommunicationTask() {
        if (!usgCommunicationTask.isCancelled()) {
            permText("Disconnecting from PJA USG...");
            usgCommunicationTask.cancel(true);
            changeMainText("Disconnected", Color.GREEN, 26.5f, 1000);
        } else {
            Log.e(LOG_TAG, "I'm not connected to any USG.");
        }
    }

    /**
     * This method is intended to be called from the main task.
     * @param command Command you want to send to the USG server.
     */
    void SendCommand(String command) {
        if (usgCommunicationTask.isConnected() && usgCommunicationTask.commandQueue.isEmpty()) {
            try {
                usgCommunicationTask.commandQueue.put(command);
                permText(command.replace('_', ' '));

            } catch (InterruptedException e) {
                Log.e(LOG_TAG, "Cannot put new command to the command queue: "
                        + e.getMessage());
            }
        }
    }

    /**
     * Gesture method calls.
     */
    protected void commandFreeze() {
        SendCommand(UsgCommunicationTask.COMMAND_FREEZE);
    }

    protected void commandAreaUp() {
        SendCommand(UsgCommunicationTask.COMMAND_AREA_UP);
    }

    protected void commandAreaDown() {
        SendCommand(UsgCommunicationTask.COMMAND_AREA_DOWN);
    }

    protected void commandGainUp() {
        SendCommand(UsgCommunicationTask.COMMAND_GAIN_UP);
    }

    protected void commandGainDown() {
        SendCommand(UsgCommunicationTask.COMMAND_GAIN_DOWN);
    }
}
