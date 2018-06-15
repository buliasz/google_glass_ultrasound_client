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
import com.google.android.glass.widget.CardScrollView;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.media.SoundPool;
import android.os.Bundle;
import android.os.Handler;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;

/**
 * This activity hosts a card scroller that shows the results of the session after the session has
 * ended.
 */
public class SessionResultsActivity extends Activity {

    /**
     * The intent extra that holds the instance of {@link ClientModel} to display in the
     * results.
     */
    public static final String EXTRA_MODEL = "model";

    /**
     * Handler used to post requests to start new activities so that the menu closing animation
     * works properly.
     */
    private final Handler mHandler = new Handler();

    /** Audio manager used to play system sound effects. */
    private AudioManager mAudioManager;

    /** Sound pool used to play the sound effects. */
    private SoundPool soundPool;

    /** Card scroller that shows the session results (summary card). */
    private CardScrollView cardScroller;

    /**
     * Stores the standard margin for a card, which is used when dynamically creating the table
     * rows for the result cards.
     */
    private int mCardMargin;

    /** Listener that displays the options menu when the card scroller is tapped. */
    private final AdapterView.OnItemClickListener mOnClickListener =
            new AdapterView.OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
            mAudioManager.playSoundEffect(Sounds.TAP);
            openOptionsMenu();
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mAudioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        ClientModel model = (ClientModel) getIntent().getSerializableExtra(EXTRA_MODEL);

        mCardMargin = (int) getResources().getDimension(R.dimen.card_margin);

        cardScroller = new CardScrollView(this);
        cardScroller.setHorizontalScrollBarEnabled(true);
        cardScroller.setAdapter(
                new SessionResultsAdapter(getLayoutInflater(), getResources(), model));
        cardScroller.setOnItemClickListener(mOnClickListener);
        cardScroller.activate();
        setContentView(cardScroller);

        // Initialize the sound pool and play the losing or winning sound immediately once it has
        // been loaded.
        soundPool = new SoundPool(1, AudioManager.STREAM_MUSIC, 0);
        soundPool.setOnLoadCompleteListener(new SoundPool.OnLoadCompleteListener() {
            @Override
            public void onLoadComplete(SoundPool soundPool, int sampleId, int status) {
                soundPool.play(sampleId, 1.0f, 1.0f, 0, 0, 1.0f);
            }
        });
        int soundResId = R.raw.triumph;
        soundPool.load(this, soundResId, 0);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.session_results, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // The startUsgTransfer() method starts a new activity, and if we call it directly here then
        // the new activity will start without giving the menu a chance to slide back down first.
        // By posting the call to a handler instead, it will be processed on an upcoming onGestureSwipeRight
        // through the message queue, after the animation has completed, which results in a
        // smoother transition between activities.
        if (item.getItemId() == R.id.new_session) {
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    startUsgTransfer();
                }
            });
            return true;
        } else {
            return false;
        }
    }

    /**
     * Starts a new video transfer activity and finishes this result activity.
     */
    private void startUsgTransfer() {
        startActivity(new Intent(this, UsgMainActivity.class));
        finish();
    }
}
