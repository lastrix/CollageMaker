package org.lastrix.collagemaker.app.api;

import android.util.Log;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONException;
import org.json.JSONObject;
import org.lastrix.collagemaker.app.BuildConfig;
import org.lastrix.collagemaker.app.content.User;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

/**
 * Performs instagram api calls and processes it's results.<br/>
 * {@link #apiCall(String)}<br/>
 * {@link #getApiPopularPhotosUrl(org.lastrix.collagemaker.app.content.User)}<br/>
 * {@link #getApiUserSearchUrl(String)}<br/>
 * Created by lastrix on 8/21/14.
 */
final class API {

    public static final String CLIENT_ID = BuildConfig.INSTAGRAM_CLIENT_ID;
    public static final String API_USER_SEARCH = "https://api.instagram.com/v1/users/search?q=%s&client_id=%s";
    public static final String API_USER_S_POPULAR_PHOTOS = "https://api.instagram.com/v1/users/%d/media/recent/?client_id=%s";

    public static final HttpClient CLIENT = new DefaultHttpClient();
    public static final int HTTP_OK = 200;

    public static final String JSON_META = "meta";
    public static final String JSON_META_ATTR_CODE = "code";
    public static final String JSON_META_ATTR_ERROR_TYPE = "error_type";
    public static final String JSON_META_ATTR_ERROR_MESSAGE = "error_message";
    public static final String JSON_PAGINATION_NEXT_URL_ATTR = "next_url";
    public static final String JSON_PAGINATION = "pagination";
    public static final String JSON_DATA = "data";
    public static final String JSON_DATA_TYPE = "type";
    public static final String DATA_TYPE_IMAGE = "image";


    public static final String LOG_MESSAGE_CONNECTION_PROBLEM = "Connection problem";
    public static final String LOG_MESSAGE_JSON_PARSING_PROBLEM = "JSON parsing problem";
    public static final String LOG_MESSAGE_RESPONSE_ERROR = "Code: %d;\ntype: %s;\nerror_message: %s";
    public static final String LOG_MESSAGE_NO_RESPONSE_FROM_SERVER = "No response from server.";

    public static final String LOG_TAG = API.class.getSimpleName();
    private static final boolean LOG_ALL = BuildConfig.LOG_ALL;

    /**
     * Calls api and returns response if all is good, throws ApiException otherwise.<br/>
     * The returned document should look like (you should ignore meta tag):
     * <pre>
     * {@code     {
     *          "meta": {"code": 200 },
     *          "data": {  },
     *          "pagination": { "next_url": "...",
     *                          "next_max_id": "..."
     *                        }
     *       }}
     * </pre>
     * Pagination notice:<br/>
     * Since this method accepts only URL you should pass next_url to fetch more pages.<br/>
     * {@link #JSON_PAGINATION}<br/>
     * {@link #JSON_PAGINATION_NEXT_URL_ATTR}<br/>
     * {@link #JSON_DATA}<br/>
     *
     * @param url -- the api call url
     * @throws ApiException
     */
    public static JSONObject apiCall(String url) throws ApiException {
        try {
            if (LOG_ALL) {
                Log.v(LOG_TAG, "apiCall for " + url);
            }
            //download json document
            final String responseString = get(url);
            if (responseString == null) {
                Log.e(LOG_TAG, LOG_MESSAGE_NO_RESPONSE_FROM_SERVER);
                throw new ApiException(LOG_MESSAGE_NO_RESPONSE_FROM_SERVER);
            }
            //parse document
            JSONObject response = new JSONObject(responseString);

            //check errors
            JSONObject meta = response.getJSONObject(JSON_META);
            final int code = meta.getInt(JSON_META_ATTR_CODE);
            if (code != HTTP_OK) {
                String message = String.format(
                        LOG_MESSAGE_RESPONSE_ERROR,
                        code,
                        meta.getString(JSON_META_ATTR_ERROR_TYPE),
                        meta.getString(JSON_META_ATTR_ERROR_MESSAGE)
                );
                Log.e(LOG_TAG, message);
                throw new ApiException(message);
            }

            //pass to caller
            return response;

        } catch (IOException e) {
            //probably connection problem
            Log.e(LOG_TAG, LOG_MESSAGE_CONNECTION_PROBLEM, e);
            throw new ApiException(LOG_MESSAGE_CONNECTION_PROBLEM, e);
        } catch (JSONException e) {
            //json was not parsed correctly?
            Log.e(LOG_TAG, LOG_MESSAGE_JSON_PARSING_PROBLEM, e);
            throw new ApiException(LOG_MESSAGE_JSON_PARSING_PROBLEM, e);
        }
    }

    /**
     * Get http response body for url.
     *
     * @param url -- request url
     * @return response body or null
     * @throws java.io.IOException in case of connection problems
     */
    private static String get(String url) throws IOException {
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
            Log.w(LOG_TAG, String.format("Server returned: %d", statusCode));
            //in case error code return null.
            return null;
        }
        return builder.toString();
    }

    /**
     * Get url for searching user
     *
     * @param username -- the username to be searched
     * @return api call url
     */
    public static String getApiUserSearchUrl(String username) {
        return String.format(API_USER_SEARCH, username, CLIENT_ID);
    }

    /**
     * Return url for fetching popular user photos
     *
     * @param user -- the user
     * @return api call url
     */
    public static String getApiPopularPhotosUrl(User user) {
        return String.format(API_USER_S_POPULAR_PHOTOS, user.getId(), CLIENT_ID);
    }

    /**
     * Safely get next_url attr.
     *
     * @param root -- response JSONObject
     * @return null or string
     * @throws org.json.JSONException
     */
    public static String nextUrl(JSONObject root) throws JSONException {
        if (root.isNull(JSON_PAGINATION)) {
            return null;
        }
        JSONObject pagination = root.getJSONObject(JSON_PAGINATION);
        //the check is necessary: we could get JSONException if no such attr found.
        if (!pagination.isNull(JSON_PAGINATION_NEXT_URL_ATTR)) {
            return pagination.getString(JSON_PAGINATION_NEXT_URL_ATTR);
        }
        return null;
    }

    /**
     * Check whether data element is image entry
     *
     * @param entry -- json document
     * @return true if image, false otherwise
     * @throws JSONException
     */
    public static boolean isImage(JSONObject entry) throws JSONException {
        return DATA_TYPE_IMAGE.equals(entry.getString(JSON_DATA_TYPE));
    }

}
