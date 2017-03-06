package tcss450.uw.edu.gvtest;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.util.Log;
import android.view.View;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

import static android.content.ContentValues.TAG;

/**
 * Created by Edward on 3/5/2017.
 */

public class PhotoDB {
    private final String TAG = "PhotoDB";
    private PictureObject picData;
    private SharedPreferences prefs;
    private Context context;
    private Activity myActivity;
    private int userid;
    private static final String PARTIAL_URL
            = "http://cssgate.insttech.washington.edu/" +
            "~ekoval/";
    private JSONArray photoData;
    private List<PictureObject> photoResult;


    public PhotoDB(Activity activity) {
        this.picData = null;
        this.myActivity = activity;
        this.context = activity.getApplicationContext();
        this.prefs = context.getSharedPreferences(context.getString(R.string.prefKey), Context.MODE_PRIVATE);
        userid = prefs.getInt(context.getString(R.string.UID) , -1);
        if (userid == -1){
            throw new IllegalArgumentException("User id not set in Prefs");
        }
    }

    public List<PictureObject> addPhoto(PictureObject pictureObject) {
        picData = pictureObject;
        final String SERVICE = "addPhoto.php";
        // build GET Statement
        final StringBuilder sb = new StringBuilder();
        sb.append("?userid=" + userid);
        sb.append("&photoid=" + picData.getMyPhotoId());
        sb.append("&location=" + picData.getMyLocation());
        sb.append("&total=" + picData.getMyPrice());
        sb.append("&payment_type=" + picData.getMyPaymentType());
        sb.append("&date=" + picData.getMyDate());
        sb.append("&category=" + picData.getMyCategory());

        Log.d("addphoto", "addPhoto: " + PARTIAL_URL + SERVICE + sb.toString());
        new AsyncTask<PictureObject, Void, String>() {

            @Override
            protected String doInBackground(PictureObject... params) {
                String response = "";
                HttpURLConnection urlConnection = null;
                try {
                    URL urlObject = new URL(PARTIAL_URL + SERVICE + sb.toString());
                    urlConnection = (HttpURLConnection) urlObject.openConnection();
                    InputStream content = urlConnection.getInputStream();
                    BufferedReader buffer = new BufferedReader(new InputStreamReader(content));
                    String s = "";
                    while ((s = buffer.readLine()) != null) {
                        response += s;
                    }
                } catch (Exception e) {
                    response = "Unable to connect, Reason: "
                            + e.getMessage();
                } finally {
                    if (urlConnection != null)
                        urlConnection.disconnect();
                }
                return response;
            }

            @Override
            protected void onPostExecute(String result) {
                if (result.startsWith("Unable to") || result.startsWith("There was an error")) {
                    Toast.makeText(context.getApplicationContext(), result, Toast.LENGTH_LONG)
                            .show();
                    return;
                } else if (result.startsWith("{\"Success")) {
//                    try {
//                        JSONObject root = new JSONObject(result);
//                        JSONObject success = root.getJSONObject("Success");
//                        photoResult = getJsonPhotos(success.getJSONArray("Status"));
//
//                        photoData = success.getJSONArray("photoData");
//                    } catch (JSONException e) {
//                        e.printStackTrace();
//                    }
                } else if (result.startsWith("{\"Error")) {
                    try {
                        JSONObject root = new JSONObject(result);
                        JSONObject error = root.getJSONObject("Error");
                        photoResult = getJsonPhotos(error.getJSONArray("photoData"));
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
            }
        }.execute(pictureObject);
        return photoResult;
    }

    private List<PictureObject> getJsonPhotos(JSONArray array) throws JSONException {
        List<PictureObject> result = null;
        if (array != null) {
            result = new ArrayList<>();
            for (int i = 0; i < array.length(); i++) {
                JSONObject photo = array.getJSONObject(i);
                PictureObject pic = new PictureObject(photo.getInt("userid"),
                        photo.getString("photoid"), photo.getString("location"),
                        BigDecimal.valueOf(photo.getDouble("total")),photo.getString("payment_type"),
                        photo.getString("date"), photo.getString("category"));
                result.add(pic);
            }
        } else {
            result = null;
        }
        return result;
    }

    public List<PictureObject> getAllPhotos() {
        final String SERVICE = "getPhotos.php";
        // build GET Statement
        final StringBuilder sb = new StringBuilder();
        sb.append("?userid=" + userid);

        Log.d("Starting debug photodb", "Starting debug");

        new AsyncTask<PictureObject, Void, String>() {

            @Override
            protected String doInBackground(PictureObject... params) {
                Log.d("IN On background", "doInBackground: "+PARTIAL_URL + SERVICE + sb.toString());
//                JSONParser
//                JSONObject json
                String response = "";
                HttpURLConnection urlConnection = null;
                try {
                    Log.d("IN On background", "doInBackground: "+PARTIAL_URL + SERVICE + sb.toString());
                    URL urlObject = new URL(PARTIAL_URL + SERVICE + sb.toString());
                    Log.d("In on background", "" + 1);
                    urlConnection = (HttpURLConnection) urlObject.openConnection();
                    Log.d("In on background", "" + 2);
                    InputStream content = urlConnection.getInputStream();
                    Log.d("In on background", "" + 3);
                    BufferedReader buffer = new BufferedReader(new InputStreamReader(content));
                    Log.d("In on background", "" + 4);
                    String s = "";
                    Log.d("In on background", "" + 5);

                    while ((s = buffer.readLine()) != null) {
                        response += s;
                    }
                } catch (Exception e) {
                    response = "Unable to connect, Reason: "
                            + e.getMessage();
                } finally {
                    if (urlConnection != null)
                        urlConnection.disconnect();
                }
//                Log.d("In On Background", response);
                return response;
            }

            @Override
            protected void onPostExecute(String result) {
                Log.d("getphotos", "onPostExecute: " + result);
                if (result.startsWith("Unable to") || result.startsWith("There was an error")) {
                    Toast.makeText(context.getApplicationContext(), result, Toast.LENGTH_LONG)
                            .show();
                    return;
                } else if (result.contains("Success")) {
                    try {
                        JSONObject root = new JSONObject(result);
                        JSONObject success = root.getJSONObject("Success");
                        Log.d("Success", "Parsing json pic array now");
                        photoResult = getJsonPhotos(success.getJSONArray("photoData"));
                        Log.d(TAG, "onPostExecute: " + result);
                        photoData = success.getJSONArray("photoData");
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                } else if (result.contains("Error")) {
                    try {
                        JSONObject root = new JSONObject(result);
                        JSONObject error = root.getJSONObject("Error");
                        photoResult = getJsonPhotos(error.getJSONArray("photoData"));
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }

                if (myActivity instanceof  OverviewActivity) {
                    ((OverviewActivity) myActivity).updateTable(photoResult);
                }

                Log.d(TAG, "onPostExecute photoResult: " + photoResult);
                Log.d(TAG, "onPostExecute: photodata : " + photoData);
            }
        }.execute();
        //Log.d("After async task", "getAllPhotos: " + photoResult.toString());
        return photoResult;
    }


    public List<PictureObject> getCategoryAll(String category) {
        final String SERVICE = "getPhotoCat.php";
        // build GET Statement
        final StringBuilder sb = new StringBuilder();
        sb.append("?userid=" + userid);
        sb.append("&category=" + category);

        new AsyncTask<PictureObject, Void, String>() {

            @Override
            protected String doInBackground(PictureObject... params) {
                String response = "";
                HttpURLConnection urlConnection = null;
                try {
                    URL urlObject = new URL(PARTIAL_URL + SERVICE + sb.toString());
                    urlConnection = (HttpURLConnection) urlObject.openConnection();
                    InputStream content = urlConnection.getInputStream();
                    BufferedReader buffer = new BufferedReader(new InputStreamReader(content));
                    String s = "";
                    while ((s = buffer.readLine()) != null) {
                        response += s;
                    }
                } catch (Exception e) {
                    response = "Unable to connect, Reason: "
                            + e.getMessage();
                } finally {
                    if (urlConnection != null)
                        urlConnection.disconnect();
                }
                return response;
            }

            @Override
            protected void onPostExecute(String result) {
                if (result.startsWith("Unable to") || result.startsWith("There was an error")) {
                    Toast.makeText(context.getApplicationContext(), result, Toast.LENGTH_LONG)
                            .show();
                    return;
                } else if (result.startsWith("{\"Success")) {
//                    try {
//                        JSONObject success = new JSONObject(result);
//                        photoResult = getJsonPhotos(success.getJSONArray("photoData"));
//
//                        photoData = success.getJSONArray("photoData");
//                    } catch (JSONException e) {
//                        e.printStackTrace();
//                    }
                } else if (result.startsWith("{\"Error")) {
                    try {
                        JSONObject root = new JSONObject(result);
                        JSONObject error = root.getJSONObject("Error");
                        photoResult = getJsonPhotos(error.getJSONArray("photoData"));
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
            }
        }.execute();
        return photoResult;
    }
}
