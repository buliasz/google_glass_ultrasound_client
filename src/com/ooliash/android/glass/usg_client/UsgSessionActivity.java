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
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

/**
 * Implementation of the main activity: transfers video and allows additional gestures.
 */
public class UsgSessionActivity extends BaseActivity {

    private static final String LOG_TAG = "USG";
    UsgCommunicationTask usgCommunicationTask;

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
            sessionDurationTime += 1;
            updateTimer();
            nextTick();
        }
    };

    /**
     * Keeps track of number of seconds of diagnosis.
     */
    private int sessionDurationTime;
    Date sessionStartTime;

    /**
     * TextView that displays the current time.
     */
    private TextView mTimer;
    private UsgSessionMenuHandler _menuHandler;
    private Calendar _calendar;
    private SimpleDateFormat _timeFormat;

    public UsgSessionActivity() {
        _menuHandler = new UsgSessionMenuHandler(this);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        getWindow().requestFeature(WindowUtils.FEATURE_VOICE_COMMANDS);

        super.onCreate(savedInstanceState);

        usgCommunicationTask = new UsgCommunicationTask(this);
        mTimer = (TextView) findViewById(R.id.timer);
    }

    @Override
    protected void onStart() {
        super.onStart();
        _calendar = Calendar.getInstance();
        _timeFormat = new SimpleDateFormat("HH:mm", Locale.US);
        sessionStartTime = new Date();
        sessionDurationTime = 0;
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
        if (featureId == Window.FEATURE_OPTIONS_PANEL) {
            getMenuInflater().inflate(R.menu.session_menu_touch, menu);
            return true;
        }
        if (featureId == WindowUtils.FEATURE_VOICE_COMMANDS) {
            getMenuInflater().inflate(R.menu.session_menu_voice, menu);
            return true;
        }
        return super.onCreatePanelMenu(featureId, menu);
    }

    /**
     * Implementation of
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
            _menuHandler.HandleItem(item.getItemId());
            return true;
        }
        return super.onMenuItemSelected(featureId, item);
    }

    @Override
    protected boolean handleGesture(Gesture gesture) {
        switch (gesture) {
            case TAP:
                openOptionsMenu();
                break;
            case SWIPE_LEFT:
                sendCommand(Command.AREA_UP);
                break;
            case SWIPE_RIGHT:
                sendCommand(Command.AREA_DOWN);
                break;
            case SWIPE_UP:
                sendCommand(Command.GAIN_UP);
                break;
            case SWIPE_DOWN:
                sendCommand(Command.GAIN_DOWN);
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

    /** Updates the timer display with the current time. */
    private void updateTimer() {
        // The code point U+EE01 in Roboto is the vertically centered colon used in the clock on
        // the Glass home screen.
//        String timeString = String.format(Locale.ENGLISH,
//            "%d\uee01%02d", sessionDurationTime / 60, sessionDurationTime % 60);
//        mTimer.setText(timeString);

        String strTime = _timeFormat.format(_calendar.getTime());
        mTimer.setText(strTime);
    }

    /**
     * Change main TextView text.
     * @param text The new text to display.
     * @param color Text color.
     * @param size Text size.
     */
    private void changeMainText(String text, int color, float size, long delayMilis) {
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

    /**
     * Permanent message display.
     * @param text Message to display.
     */
    private void permText(String text) {
        changeMainText(text, Color.WHITE, 16.5f, 0);
    }

    void normalMessage(String text) {
        changeMainText(text, Color.WHITE, 16.5f, 2000);
    }

    /**
     * Error message to display (red with timeout).
     * @param text Message to display.
     */
    void errorMessage(String text) {
        changeMainText(text, Color.RED, 16.5f, 2000);
        audioManager.playSoundEffect(Sounds.ERROR);
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
    void sendCommand(String command) {
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

    public byte[] getLastUsgPictureBytes() {
        return usgCommunicationTask.getLastUsgPictureBytes();
    }
}
