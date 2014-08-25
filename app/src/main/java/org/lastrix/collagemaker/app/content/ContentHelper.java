package org.lastrix.collagemaker.app.content;

import android.content.ContentUris;
import android.net.Uri;

/**
 * Content helper to construct data uris.
 * Created by lastrix on 8/24/14.
 */
public class ContentHelper {

    private final static Uri URI_USER = Uri.parse("content://" + ContentProvider.AUTHORITY + "/" + User.TABLE_NAME);
    private final static Uri URI_PHOTO = Uri.parse("content://" + ContentProvider.AUTHORITY + "/" + Photo.TABLE_NAME);

    /**
     * Return uri to access users
     *
     * @param user -- user object or null
     * @return uri
     */
    public static Uri getUserUri(User user) {
        if (user == null) return URI_USER;
        return ContentUris.withAppendedId(URI_USER, user.getId());
    }

    /**
     * Return uri to access photos
     *
     * @param photo -- photo object or null
     * @return uri
     */
    public static Uri getPhotoUri(Photo photo) {
        if (photo == null) return URI_PHOTO;
        return ContentUris.withAppendedId(URI_PHOTO, photo.getId());
    }

}
