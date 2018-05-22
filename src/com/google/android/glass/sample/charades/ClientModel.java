/*
 * Copyright (C) 2017-2018 Bartlomiej Uliasz
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
 * Represents the state of the USG client. This class is serializable so that it can be stored in an
 * {@code Intent} and passed from the USG activity to the Settings activity.
 */
public class ClientModel implements Serializable {
    private static final long serialVersionUID = 1L;
}
