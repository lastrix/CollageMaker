package org.lastrix.collagemaker.app.content;

import android.content.ContentUris;
import android.net.Uri;
import org.lastrix.collagemaker.app.api.User;

/**
 * Created by lastrix on 8/24/14.
 */
public class ContentHelper {

    final static Uri URI_USER = Uri.parse(CollageContentProvider.AUTHORITY + "user");

    public static Uri getUserUri(User user) {
        return ContentUris.withAppendedId(URI_USER, user.getId());
    }

}
