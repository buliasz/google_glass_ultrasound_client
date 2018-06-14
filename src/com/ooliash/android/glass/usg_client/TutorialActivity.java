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

import android.app.Activity;
import android.os.Bundle;
import android.view.View;

import java.util.Arrays;
import java.util.List;

/**
 * An implementation of the application tutorial, restricting certain gestures to match
 * the instruction phrases on the screen.
 */
public class TutorialActivity extends Activity {

    /** The index of the "swipe to onGestureSwipeRight" card in the tutorial model. */
    private static final int SWIPE_TO_PASS_CARD = 1;

    // The index of the current tutorial phrase.
    private int phraseIndex;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Hide the status bar in tutorial mode.
        findViewById(R.id.status_bar).setVisibility(View.GONE);
    }

    /** Load the fixed tutorial phrases from the application's resources. */
    protected TutorialModel createTutorialModel() {
        List<String> tutorialPhrases = Arrays.asList(getResources().getStringArray(
                R.array.tutorial_phrases));
        return new TutorialModel(tutorialPhrases);
    }

    /**
     * Only allow the tap gesture on the "???" screen and to only allow the
     * swipe gesture on the "???" screen. Also automatically ended when the
     * final card is either tapped or swiped.
     */
    protected void handleGestures(Gesture gesture) {
        switch (gesture) {
            case TAP:
                break;
            case SWIPE_RIGHT:
                break;
        }

        // Finish the tutorial if we transitioned away from the final card.
        if (phraseIndex == getTutorialModel().getPhraseCount() - 1) {
            finish();
        }
    }

    private TutorialModel getTutorialModel() {
        return null;
    }
}
