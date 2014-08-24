package org.lastrix.collagemaker.app.api;

/**
 * Exception wrapper.
 * Created by lastrix on 8/21/14.
 */
public class ApiException extends Exception {
    public ApiException(String detailMessage, Throwable throwable) {
        super(detailMessage, throwable);
    }

    public ApiException(String detailMessage) {
        super(detailMessage);
    }
}
