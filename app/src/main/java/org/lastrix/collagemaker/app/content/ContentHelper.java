package org.lastrix.collagemaker.app.content;

import android.content.ContentUris;
import android.net.Uri;

/**
 * Created by lastrix on 8/24/14.
 */
public class ContentHelper {

    final static Uri URI_USER = Uri.parse("content://" + CollageContentProvider.AUTHORITY + "/" + User.TABLE_NAME);
    final static Uri URI_PHOTO = Uri.parse("content://" + CollageContentProvider.AUTHORITY + "/" + Photo.TABLE_NAME);

    public static Uri getUserUri(User user) {
        if (user == null) return URI_USER;
        return ContentUris.withAppendedId(URI_USER, user.getId());
    }

    public static Uri getPhotoUri(Photo photo) {
        if (photo == null) return URI_PHOTO;
        return ContentUris.withAppendedId(URI_USER, photo.getId());
    }

}
