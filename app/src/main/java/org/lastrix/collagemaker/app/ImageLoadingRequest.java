package org.lastrix.collagemaker.app;

import android.graphics.Bitmap;
import org.lastrix.collagemaker.app.util.HttpHelper;
import rx.Observable;
import rx.Subscriber;

import java.io.IOException;
import java.util.List;

/**
 * Observable for loading images from url list.
 * {@link #LOADING_LIMIT} defines how many urls could be processed before forced stop.
 * <p/>
 * Created by lastrix on 8/21/14.
 */
class ImageLoadingRequest implements Observable.OnSubscribe<Bitmap> {
    /**
     * Limit image loading. It's not good idea to load everything.
     */
    private final static int LOADING_LIMIT = 8;
    private final List<String> mUrls;

    public ImageLoadingRequest(List<String> urls) {
        this.mUrls = urls;
    }

    @Override
    public void call(Subscriber<? super Bitmap> subscriber) {
        try {
            int idx = 0;
            Bitmap image;
            for (String url : mUrls) {
                image = HttpHelper.getImage(url);
                if (image == null) {
                    subscriber.onError(new NullPointerException("Failed to load image."));
                    return;
                }
                subscriber.onNext(image);
                idx++;
                if (idx >= LOADING_LIMIT) break;
            }
            subscriber.onCompleted();
        } catch (IOException e) {
            subscriber.onError(e);
        }
    }
}
