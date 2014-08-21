package org.lastrix.collagemaker.app;

import org.lastrix.collagemaker.app.api.InstagramApi;
import org.lastrix.collagemaker.app.api.InstagramApiException;
import org.lastrix.collagemaker.app.api.User;
import rx.Observable;
import rx.Subscriber;

/**
 * Observable for user searching
 * <p/>
 * Created by lastrix on 8/21/14.
 */
class UserSearchRequest implements Observable.OnSubscribe<User> {

    private final String mUsername;

    public UserSearchRequest(String username) {
        mUsername = username;
    }

    @Override
    public void call(Subscriber<? super User> subscriber) {
        try {
            for (User u : InstagramApi.search(mUsername)) {
                subscriber.onNext(u);
            }
            subscriber.onCompleted();
        } catch (InstagramApiException e) {
            subscriber.onError(e);
        }
    }
}
