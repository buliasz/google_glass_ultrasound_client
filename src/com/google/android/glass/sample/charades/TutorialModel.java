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

package com.google.android.glass.sample.charades;

import java.io.Serializable;
import java.util.List;

/**
 * Represents the state of the tutorial: the phrases that describe this application, which of those
 * phrases have been read, and which phrase the user is currently on. This class is serializable so
 * that it can be stored in an {@code Intent} and passed from the main activity to the results
 * activity.
 */
public class TutorialModel implements Serializable {

    private static final long serialVersionUID = 1L;
    private final List<String> tutorialPhrases;

    /** The index of the phrase that the user is currently on in the tutorial. */
    private int currentPhrase;

    /** Constructs a new model with the specified list of phrases.
     * @param tutorialPhrases*/
    public TutorialModel(List<String> tutorialPhrases) {
        this.tutorialPhrases = tutorialPhrases;
        currentPhrase = 0;
    }

    /** Returns the index of the phrase that the user is currently on in the tutorial. */
    public int getCurrentPhraseIndex() {
        return currentPhrase;
    }

    /**
     * Returns number of tutorial phrases.
     * @return Number of tutorial phrases.
     */
    public int getPhraseCount() {
        return tutorialPhrases.size();
    }
}
