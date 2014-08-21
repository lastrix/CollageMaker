package org.lastrix.collagemaker.app.api;

/**
 * Exception wrapper.
 * Created by lastrix on 8/21/14.
 */
public class InstagramApiException extends Exception {
    public InstagramApiException(String detailMessage, Throwable throwable) {
        super(detailMessage, throwable);
    }

    public InstagramApiException(String detailMessage) {
        super(detailMessage);
    }
}
