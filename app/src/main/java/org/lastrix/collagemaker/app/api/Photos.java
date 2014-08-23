package org.lastrix.collagemaker.app.api;

import java.util.LinkedList;
import java.util.List;

/**
 * This container is used as index database.
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
     * @param standardResolutionUrl -- image url
     */
    public void addPhoto(String thumbnailUrl, String standardResolutionUrl) {
        mPhotos.add(new Photo(thumbnailUrl, standardResolutionUrl));
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

}
