package org.lastrix.collagemaker.app;

import org.lastrix.collagemaker.app.api.InstagramApi;
import org.lastrix.collagemaker.app.api.InstagramApiException;
import org.lastrix.collagemaker.app.api.Photos;
import org.lastrix.collagemaker.app.api.User;
import rx.Observable;
import rx.Subscriber;

/**
 * Observable for fetching user popular image urls.
 * Created by lastrix on 8/21/14.
 */
class PopularPhotoRequest implements Observable.OnSubscribe<Photos.Photo> {
    private final User mUser;

    public PopularPhotoRequest(User user) {
        mUser = user;
    }

    @Override
    public void call(Subscriber<? super Photos.Photo> subscriber) {
        Photos photos;
        String nextUrl = null;
        try {
            do {
                photos = InstagramApi.popularPhotos(mUser, nextUrl);
                if (photos == null) {
                    //no images
                    subscriber.onCompleted();
                    return;
                }
                nextUrl = photos.getNextUrl();

                for (Photos.Photo photo : photos.getPhotos()) {
                    subscriber.onNext(photo);
                }
            } while (nextUrl != null);
            subscriber.onCompleted();
        } catch (InstagramApiException e) {
            subscriber.onError(e);
        }
    }
}
