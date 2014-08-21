package org.lastrix.collagemaker.app.util;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * Utility for easier http access.
 * Created by lastrix on 8/21/14.
 */
public class HttpHelper {

    public static final int HTTP_OK = 200;
    public static final String LOG_TAG = HttpHelper.class.getSimpleName();

    /**
     * Get http response body for url.
     *
     * @param url -- request url
     * @return response body or null
     * @throws IOException in case of connection problems
     */
    public static String get(String url) throws IOException {
        StringBuilder builder = new StringBuilder();
        HttpClient client = new DefaultHttpClient();
        HttpGet get = new HttpGet(url);

        //fetch response
        HttpResponse response = client.execute(get);
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
        } else {
            //in case error code return null.
            return null;
        }

        return builder.toString();
    }

    /**
     * Download image from http server
     *
     * @param urlString -- request url
     * @return bitmap or null
     * @throws IOException
     */
    public static Bitmap getImage(String urlString) throws IOException {
        InputStream in;
        Bitmap bmp;
        URL url = new URL(urlString);
        Log.d(LOG_TAG, "Url is " + url);
        HttpURLConnection con = (HttpURLConnection) url.openConnection();
        con.setDoInput(true);
        con.connect();
        int responseCode = con.getResponseCode();
        if (responseCode == HttpURLConnection.HTTP_OK) {
            //download
            in = con.getInputStream();
            bmp = BitmapFactory.decodeStream(in);
            in.close();
            return bmp;
        }
        return null;
    }
}
