package com.corgrimm.imgy.api;


import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import com.google.inject.Inject;
import com.loopj.android.http.AsyncHttpResponseHandler;
import com.loopj.android.http.JsonHttpResponseHandler;
import com.loopj.android.http.RequestParams;
import org.apache.http.entity.FileEntity;
import org.apache.http.entity.StringEntity;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileNotFoundException;

import static com.corgrimm.imgy.core.Constants.Oauth.*;
import static com.corgrimm.imgy.core.Constants.Prefs.*;

/**
 * Created with IntelliJ IDEA.
 * User: Anderson
 * Date: 6/11/12
 * Time: 10:04 AM
 */
public class ImgyApi {

    @Inject protected static SharedPreferences sharedPrefs;
    static StringEntity entity;

    public static ConnectivityManager connectivityManager;

    private static RequestParams params;

    public static void getMainGallery(Context context, JsonHttpResponseHandler getGalleryResponseHandler) {

        sharedPrefs = context.getSharedPreferences(PREFS_NAME, Activity.MODE_PRIVATE);

        String collection = sharedPrefs.getString(COLLECTION, VIRAL);
        String filter = sharedPrefs.getString(FILTER, POPULAR);

        ImgyRestClient.get(context, String.format("gallery/%s/%s/0.json", collection, filter), null, getGalleryResponseHandler);
    }

    public static void getSubredditGallery(Context context, String subreddit, JsonHttpResponseHandler getGalleryResponseHandler) {

        sharedPrefs = context.getSharedPreferences(PREFS_NAME, Activity.MODE_PRIVATE);

        String filter = sharedPrefs.getString(FILTER, POPULAR);
        if (filter.equals(POPULAR)) {
            filter = TOP;
        }

        ImgyRestClient.get(context, String.format("gallery/r/%s/%s/0.json", subreddit, filter), null, getGalleryResponseHandler);
    }

    public static void getMyImages(Context context, JsonHttpResponseHandler getGalleryResponseHandler) {
        ImgyRestClient.rawUrlGet(context, "https://api.imgur.com/3/account/me/images", null, getGalleryResponseHandler);
    }

    public static void getMyAlbums(Context context, JsonHttpResponseHandler getGalleryResponseHandler) {
        ImgyRestClient.rawUrlGet(context, "https://api.imgur.com/3/account/me/albums", null, getGalleryResponseHandler);
    }

    public static void getImageInfo(Context context, String imageId, JsonHttpResponseHandler imageInfoResponseHandler) {
        ImgyRestClient.get(context, String.format("gallery/image/%s", imageId), null, imageInfoResponseHandler);
    }

    public static void getAlbumImageInfo(Context context, String albumId, String imageId, JsonHttpResponseHandler imageInfoResponseHandler) {
        ImgyRestClient.get(context, String.format("gallery/album/%s/image/%s", albumId, imageId), null, imageInfoResponseHandler);
    }

    public static void getImageComments(Context context, String imageId, JsonHttpResponseHandler imageInfoResponseHandler) {
        ImgyRestClient.get(context, String.format("gallery/image/%s/comments", imageId), null, imageInfoResponseHandler);
    }

    public static void getAlbumComments(Context context, String albumId, JsonHttpResponseHandler imageInfoResponseHandler) {
        ImgyRestClient.get(context, String.format("gallery/album/%s/comments", albumId), null, imageInfoResponseHandler);
    }

    public static void voteForImage(Context context, String imageId, String vote, JsonHttpResponseHandler imageVoteResponseHandler) {
        ImgyRestClient.post(context, String.format("gallery/image/%s/vote/%s", imageId, vote), null, imageVoteResponseHandler);
    }

    public static void voteForAlbum(Context context, String albumId, String vote, JsonHttpResponseHandler albumVoteResponseHandler) {
        ImgyRestClient.post(context, String.format("gallery/album/%s/vote/%s", albumId, vote), null, albumVoteResponseHandler);
    }

    public static void voteForComment(Context context, Number commentId, String vote, JsonHttpResponseHandler commentVoteResponseHandler) {
        ImgyRestClient.post(context, String.format("comment/%s/vote/%s", commentId.toString(), vote), null, commentVoteResponseHandler);
    }

    public static void postComment(Context context, String imageId, String commentId, String comment, JsonHttpResponseHandler commentVoteResponseHandler) {
        params = new RequestParams();
        params.put("image_id", imageId);
        params.put("comment", comment);
        if (commentId != null)
            params.put("parent_id", commentId);
        ImgyRestClient.post(context, "comment", params, commentVoteResponseHandler);
    }

    public static void getAccessTokenFromCode(Context context, String code, AsyncHttpResponseHandler tokenResponseHandler) {
        params = new RequestParams();

        params.put("client_id", IMGUR_CLIENT_ID);
        params.put("client_secret", IMGUR_CLIENT_SECRET);
        params.put("grant_type", "authorization_code");
        params.put("code", code);
//        params.put("lon", Float.toString(latestLocation.lastLong));
//        params.put("cep", Integer.toString(latestLocation.lastAccuracy) + ".0");

        ImgyRestClient.postRawUrl(context, "https://api.imgur.com/oauth2/token", params, tokenResponseHandler);
    }

    public static void getAccessTokenFromRefresh(Context context, AsyncHttpResponseHandler tokenResponseHandler) {
        sharedPrefs = context.getSharedPreferences(PREFS_NAME, Activity.MODE_PRIVATE);
        String refresh = sharedPrefs.getString(REFRESH_TOKEN, "");

        params = new RequestParams();

        params.put("client_id", IMGUR_CLIENT_ID);
        params.put("client_secret", IMGUR_CLIENT_SECRET);
        params.put("grant_type", "refresh_token");
        params.put("refresh_token", refresh);
//        params.put("lon", Float.toString(latestLocation.lastLong));
//        params.put("cep", Integer.toString(latestLocation.lastAccuracy) + ".0");

        ImgyRestClient.postRawUrl(context, "https://api.imgur.com/oauth2/token", params, tokenResponseHandler);
    }

    public static boolean CheckInternet(Context context) throws Error {
        boolean isNetAvailable = false;

        if (context != null) {
            ConnectivityManager mgr = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
            if (mgr != null) {
                boolean mobileNetwork;
                boolean wifiNetwork;
                boolean wiMaxNetwork;

                boolean mobileNetworkConnecetd = false;
                boolean wifiNetworkConnecetd = false;
                boolean wiMaxNetworkConnected = false;

                NetworkInfo mobileInfo = mgr.getNetworkInfo(ConnectivityManager.TYPE_MOBILE);
                NetworkInfo wifiInfo = mgr.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
                NetworkInfo wiMaxInfo = mgr.getNetworkInfo(ConnectivityManager.TYPE_WIMAX);

                if (mobileInfo != null) {
                    mobileNetwork = mobileInfo.isAvailable();
                    if (mobileNetwork)
                        mobileNetworkConnecetd = mobileInfo.isConnectedOrConnecting();
                }

                if (wifiInfo != null) {
                    wifiNetwork = wifiInfo.isAvailable();
                    if (wifiNetwork)
                        wifiNetworkConnecetd = wifiInfo.isConnectedOrConnecting();
                }

                if (wiMaxInfo != null) {
                    wiMaxNetwork = wiMaxInfo.isAvailable();
                    if (wiMaxNetwork)
                        wiMaxNetworkConnected = wiMaxInfo.isConnectedOrConnecting();
                }

                isNetAvailable = (mobileNetworkConnecetd || wifiNetworkConnecetd || wiMaxNetworkConnected);
            }
        }
        return isNetAvailable;
    }

    public static int checkForValidAuthToken(Context context) {
        sharedPrefs = context.getSharedPreferences(PREFS_NAME, Activity.MODE_PRIVATE);
        String authToken = sharedPrefs.getString(AUTH_TOKEN, null);
        Long tokenExpires = sharedPrefs.getLong(TOKEN_EXPIRE, 0);

        if (authToken != null && tokenExpires != 0) {
            if (tokenExpires > System.currentTimeMillis()) {
                return TOKEN_VALID;
            }
            return EXPIRED_TOKEN;
        }
        return NO_TOKEN;
    }

    public static void saveAuthToken(Context context, JSONObject object) {
        sharedPrefs = context.getSharedPreferences(PREFS_NAME, Activity.MODE_PRIVATE);

        try {
            sharedPrefs.edit()
                    .putString(AUTH_TOKEN, object.getString("access_token"))
                    .putString(REFRESH_TOKEN, object.getString("refresh_token"))
                    .putLong(TOKEN_EXPIRE, System.currentTimeMillis() + (object.getLong("expires_in")*1000))
                    .commit();
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public static void postImage(final Context context, final File image, AsyncHttpResponseHandler postEventResponseHandler) {
        FileEntity fileEntity = new FileEntity(image,"image/jpeg");
        params = new RequestParams();

        try {
            params.put("image", image);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
//        params.put("album", IMGUR_CLIENT_SECRET);
//        params.put("type", "refresh_token");
//        params.put("name", refresh);
//        params.put("title", refresh);
//        params.put("description", refresh);

        ImgyRestClient.post(context, "image", params, postEventResponseHandler);
    }
}
