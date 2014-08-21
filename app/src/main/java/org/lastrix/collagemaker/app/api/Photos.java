package org.lastrix.collagemaker.app.api;

import java.util.LinkedList;
import java.util.List;

/**
 * This container is used as database.
 * Holds information about user pictures (including different resolution).
 * No real image is stored here - only urls.
 * Created by lastrix on 8/21/14.
 */
public class Photos {
    private String mNextUrl;
    private List<Photo> mPhotos;

    public Photos() {
        mPhotos = new LinkedList<Photo>();
    }

    /**
     * Return link to next page
     *
     * @return url
     */
    public String getNextUrl() {
        return mNextUrl;
    }

    /**
     * Set next page url
     *
     * @param mNextUrl -- the page url
     */
    public void setNextUrl(String mNextUrl) {
        this.mNextUrl = mNextUrl;
    }

    /**
     * Get photos
     *
     * @return list of Photo
     */
    public List<Photo> getPhotos() {
        return mPhotos;
    }

    /**
     * Add photo urls
     *
     * @param thumbnailUrl          -- smallest image url
     * @param lowResolutionUrl      -- medium image url
     * @param standardResolutionUrl -- image url
     */
    public void addPhoto(String thumbnailUrl, String lowResolutionUrl, String standardResolutionUrl) {
        mPhotos.add(new Photo(thumbnailUrl, lowResolutionUrl, standardResolutionUrl));
    }

    @Override
    public boolean equals(Object o) {
        return this == o;
    }

    @Override
    public String toString() {
        return "Photos{" +
                "mNextUrl='" + mNextUrl + '\'' +
                ", mPhotos=" + mPhotos +
                '}';
    }

    /**
     * Holds  image's urls.
     * See {@link #getStandardResolutionUrl()} for further details.
     */
    public static class Photo {
        private final String mThumbnailUrl;
        private final String mLowResolutionUrl;
        private final String mStandardResolutionUrl;

        public Photo(String mThumbnailUrl, String mLowResolutionUrl, String mStandardResolutionUrl) {
            this.mThumbnailUrl = mThumbnailUrl;
            this.mLowResolutionUrl = mLowResolutionUrl;
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
         * Return medium image url
         *
         * @return url
         */
        public String getLowResolutionUrl() {
            return mLowResolutionUrl;
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
            result = 31 * result + mLowResolutionUrl.hashCode();
            result = 31 * result + mStandardResolutionUrl.hashCode();
            return result;
        }

        @Override
        public String toString() {
            return "Photo{" +
                    "mThumbnailUrl='" + mThumbnailUrl + '\'' +
                    ", mLowResolutionUrl='" + mLowResolutionUrl + '\'' +
                    ", mStandardResolutionUrl='" + mStandardResolutionUrl + '\'' +
                    '}';
        }
    }
}
