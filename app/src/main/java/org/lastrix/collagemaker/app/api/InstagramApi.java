package org.lastrix.collagemaker.app.api;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.lastrix.collagemaker.app.BuildConfig;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.LinkedList;
import java.util.List;

/**
 * Performs instagram api calls and processes it's results.
 * <p/>
 * {@link #search(String)} calls server to find user by it's nickname.
 * Server may return list of users, so as function.
 * <p/>
 * {@link #popularPhotos(User, String)} collects list of popular photos
 * for specific user. Returned objects contain urls to different image sizes.
 * <p/>
 * Created by lastrix on 8/21/14.
 */
public class InstagramApi {

    public static final String CLIENT_ID = BuildConfig.INSTAGRAM_CLIENT_ID;
    public static final String API_USER_SEARCH = "https://api.instagram.com/v1/users/search?q=%s&client_id=%s";
    public static final String API_USER_S_POPULAR_PHOTOS = "https://api.instagram.com/v1/users/%d/media/recent/?client_id=%s";

    public static final String JSON_META = "meta";
    public static final String JSON_META_CODE_ATTR = "code";
    public static final String JSON_DATA = "data";
    public static final String JSON_URL_ATTR = "url";
    public static final String JSON_THUMBNAIL = "thumbnail";
    public static final String JSON_STANDARD_RESOLUTION = "standard_resolution";
    public static final String JSON_IMAGES = "images";
    public static final String JSON_NEXT_URL_ATTR = "next_url";
    public static final String JSON_PAGINATION = "pagination";
    public static final int HTTP_OK = 200;
    public static final HttpClient CLIENT = new DefaultHttpClient();

    /**
     * Search for user(s) by name
     *
     * @param username -- the username to search
     * @return list of users
     * @throws InstagramApiException in case of errors
     */
    public static List<User> search(String username) throws InstagramApiException {
        final String url = String.format(API_USER_SEARCH, username, CLIENT_ID);
        final List<User> result = new LinkedList<User>();
        try {
            JSONArray o = apiCallForArray(url);
            if (o == null) throw new InstagramApiException("Failed to fetch data.");
            final int size = o.length();
            for (int i = 0; i < size; i++) {
                result.add(User.fromJson(o.getJSONObject(i)));
            }
        } catch (IOException e) {
            throw new InstagramApiException("Failed.", e);
        } catch (JSONException e) {
            throw new InstagramApiException("Failed.", e);
        }
        return result;
    }

    /**
     * Return list of popular user photos
     *
     * @param user    -- photos owner
     * @param nextUrl -- used for pagination purposes
     * @return an {@link org.lastrix.collagemaker.app.api.Photos} container
     * @throws InstagramApiException because bad things may happen
     */
    public static Photos popularPhotos(User user, String nextUrl) throws InstagramApiException {
        String url = nextUrl != null ? nextUrl : String.format(API_USER_S_POPULAR_PHOTOS, user.getId(), CLIENT_ID);
        try {
            JSONObject root = apiCall(url);
            if (root == null) throw new InstagramApiException("Failed to fetch data.");
            JSONObject pagination = root.getJSONObject(JSON_PAGINATION);
            JSONArray data = root.getJSONArray(JSON_DATA);
            Photos photos = new Photos();
            if (!pagination.isNull(JSON_NEXT_URL_ATTR)) {
                photos.setNextUrl(pagination.getString(JSON_NEXT_URL_ATTR));
            }

            final int size = data.length();
            JSONObject entry;
            for (int i = 0; i < size; i++) {
                entry = data.getJSONObject(i).getJSONObject(JSON_IMAGES);
                photos.addPhoto(
                        getUrlFrom(entry, JSON_THUMBNAIL),
                        getUrlFrom(entry, JSON_STANDARD_RESOLUTION)
                );
            }
            return photos;
        } catch (IOException e) {
            throw new InstagramApiException("Failed.", e);
        } catch (JSONException e) {
            throw new InstagramApiException("Failed.", e);
        } catch (NullPointerException e) {
            throw new InstagramApiException("Failed.", e);
        }
    }


    /**
     * Performs api call and return data(JSONArray) from response if OK
     *
     * @param url -- the api call url
     * @return JSONArray
     * @throws IOException   in case of connection problems
     * @throws JSONException in case of json parsing problems
     * @see #apiCall(String)
     */
    private static JSONArray apiCallForArray(String url) throws IOException, JSONException {
        JSONObject object = apiCall(url);
        if (object != null) return object.getJSONArray(JSON_DATA);
        return null;
    }

    /**
     * Calls api and returns response if all is good
     *
     * @param url -- the api call url
     * @throws IOException   in case of connection problems
     * @throws JSONException in case of json parsing problems
     */
    private static JSONObject apiCall(String url) throws IOException, JSONException {
        String responseString = get(url);
        if (responseString == null) return null;
        JSONObject response = new JSONObject(responseString);
        JSONObject meta = response.getJSONObject(JSON_META);
        if (meta.getInt(JSON_META_CODE_ATTR) == HTTP_OK) {
            return response;
        }
        return null;
    }

    /**
     * Helper method
     *
     * @param entry -- json object
     * @param field -- where url is located
     * @return url as string
     * @throws JSONException in case of json parsing problems
     */
    private static String getUrlFrom(JSONObject entry, String field) throws JSONException {
        return entry.getJSONObject(field).getString(JSON_URL_ATTR);
    }

    /**
     * Get http response body for url.
     *
     * @param url -- request url
     * @return response body or null
     * @throws java.io.IOException in case of connection problems
     */
    public static String get(String url) throws IOException {
        StringBuilder builder = new StringBuilder();
        HttpGet get = new HttpGet(url);

        //fetch response
        HttpResponse response = CLIENT.execute(get);
        StatusLine statusLine = response.getStatusLine();
        int statusCode = statusLine.getStatusCode();
        if (statusCode == HTTP_OK) {
            HttpEntity entity = response.getEntity();
            InputStream content = entity.getContent();
            BufferedReader reader = new BufferedReader(new InputStreamReader(content));
            String line;
            while ((line = reader.readLine()) != null) {
                builder.append(line);
            }
            entity.consumeContent();
        } else {
            //in case error code return null.
            return null;
        }
        return builder.toString();
    }
}
