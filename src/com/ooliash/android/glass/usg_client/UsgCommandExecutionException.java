package com.ooliash.android.glass.usg_client;

import java.io.IOException;

public class UsgCommandExecutionException extends IOException {
    /**
     * Constructs a new {@code IOException} with its stack trace and detail
     * message filled in.
     *
     * @param detailMessage the detail message for this exception.
     */
    public UsgCommandExecutionException(String detailMessage) {
        super(detailMessage);
    }
}
