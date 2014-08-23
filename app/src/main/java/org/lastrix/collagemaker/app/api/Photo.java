package org.lastrix.collagemaker.app.api;

import android.graphics.Bitmap;

/**
 * Holds  image's urls.
 * See {@link #getStandardResolutionUrl()} for further details.
 */
public class Photo {
    private final String mThumbnailUrl;
    private final String mStandardResolutionUrl;

    public Photo(String mThumbnailUrl, String mStandardResolutionUrl) {
        this.mThumbnailUrl = mThumbnailUrl;
        this.mStandardResolutionUrl = mStandardResolutionUrl;
    }

    /**
     * Return smallest image url
     *
     * @return url
     */
    public String getThumbnailUrl() {
        return mThumbnailUrl;
    }

    /**
     * Return image url
     *
     * @return url
     */
    public String getStandardResolutionUrl() {
        return mStandardResolutionUrl;
    }

    @Override
    public boolean equals(Object o) {
        return this == o;
    }

    @Override
    public int hashCode() {
        int result = mThumbnailUrl.hashCode();
        result = 31 * result + mStandardResolutionUrl.hashCode();
        return result;
    }

    @Override
    public String toString() {
        return "Photo{" +
                "mThumbnailUrl='" + mThumbnailUrl + '\'' +
                ", mStandardResolutionUrl='" + mStandardResolutionUrl + '\'' +
                '}';
    }
}
