package picture;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.util.Log;
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
import java.util.ArrayList;
import java.util.List;

import tcss450.uw.edu.gvtest.MainActivity;
import entry.OverviewActivity;
import tcss450.uw.edu.gvtest.R;

/**
 * PhotoDB is used to communicate with our PHP server which communicates to our MariaDB database.
 * This is used to store user information (username, password) and picture data (location,
 * amount, category, payment type and date of receipt).
 *
 * @author Edward Koval, Karanbir Toor (just a tiny bug fix)
 * @version 1.0
 */
public class PhotoDB {
    /**
     * Used for logging and debugging.
     */
    private final String TAG = "PhotoDB";

    /**
     * The picture details being uploaded to our server.
     */
    private PictureObject picData;

    /**
     * Locally stored information like userid.
     */
    private SharedPreferences prefs;

    /**
     * The context used to gain access to SharedPreferences.
     */
    private Context context;

    /**
     * The Activity used to return the user to the home screen in case of
     * an error.
     */
    private Activity myActivity;

    /**
     * The userid for this session.
     */
    private int userid;

    /**
     * The URL to our server.
     */
    private static final String PARTIAL_URL
            = "http://cssgate.insttech.washington.edu/" +
            "~ekoval/";

    /**
     * The returned photo data from our server.
     */
    private JSONArray photoData;

    /**
     * The JSON parsed to PictureObjects, returned by our server.
     */
    private List<PictureObject> photoResult;

    /**
     * Constructs this PhotoDB and loads the user preferences from SharedPreferences.
     * If no userid is present, log the user out.
     *
     * @param activity The Activity this class was called from.
     */
    public PhotoDB(Activity activity) {
        this.picData = null;
        this.myActivity = activity;
        this.context = activity.getApplicationContext();
        this.prefs = context.getSharedPreferences(context.getString(R.string.prefKey), Context.MODE_PRIVATE);
        userid = prefs.getInt(context.getString(R.string.UID), -1);
        if (userid == -1) {
            Toast.makeText(context, "There was an error, Please sign in again", Toast.LENGTH_LONG)
                    .show();
            forceLogout();
            //throw new IllegalArgumentException("User id not set in Prefs");
        }
    }

    /**
     * Force the user to logout because an error has occurred.
     */
    private void forceLogout() {
        SharedPreferences prefs = context.getSharedPreferences(
                context.getString(R.string.prefKey), Context.MODE_PRIVATE);

        SharedPreferences.Editor editor = prefs.edit();
        editor.putBoolean(context.getString(R.string.isloggedin), false);
        editor.commit();
        Intent intent = new Intent(context, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        myActivity.startActivity(intent);
        myActivity.finish();
    }

    /**
     * Adds a photo to the database using an AsyncTask and returns a newly
     * updated list of PictureObjects.
     *
     * @param pictureObject The photo info to add to the database.
     * @return The complete list of picture information for this user.
     */
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

        // Submit the picture object information to our database.
        new AsyncTask<PictureObject, Void, String>() {

            /**
             * Executes the connection and fetches information in a thread separate from
             * the UI thread.
             * @param params The PictureObject to upload.
             * @return The String JSON result.
             */
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

            /**
             * Processing the JSON String to PictureObjects.
             * @param result the JSON response from our server.
             */
            @Override
            protected void onPostExecute(String result) {
                if (result.startsWith("Unable to") || result.startsWith("There was an error") ||
                        result.startsWith("Incorrect Number of Variables")) {
                    Toast.makeText(context.getApplicationContext(), result, Toast.LENGTH_LONG)
                            .show();
                    return;
                } else if (result.startsWith("{\"Success")) {
                    Toast.makeText(context, "Photo Added", Toast.LENGTH_LONG).show();

                } else if (result.startsWith("{\"Error\"")) {
                    //Toast.makeText(context, "Photo Already Exists", Toast.LENGTH_LONG).show();
                    try {
                        JSONObject root = new JSONObject(result);
                        JSONObject error = root.getJSONObject("Error");

                        Toast.makeText(context, error.getString("Status"), Toast.LENGTH_LONG).show();

                        //photoResult = getJsonPhotos(error.getJSONArray("photoData"));
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
                Log.d("AddPhoto", "onPostExecute: "+ result);

            }
        }.execute(pictureObject);
        return photoResult;
    }

    /**
     * Builds PictureObjects using a JSON array.
     *
     * @param array The JSONArray of PictureObjects.
     * @return A list of PictureObjects.
     * @throws JSONException If the JSONArray is not able to be processed.
     */
    private List<PictureObject> getJsonPhotos(JSONArray array) throws JSONException {
        List<PictureObject> result = null;
        if (array != null) {
            result = new ArrayList<>();
            for (int i = 0; i < array.length(); i++) {
                JSONObject photo = array.getJSONObject(i);
                PictureObject pic = new PictureObject(photo.getInt("userid"),
                        photo.getString("photoid"), photo.getString("location"),
                        BigDecimal.valueOf(photo.getDouble("total")), photo.getString("payment_type"),
                        photo.getString("date"), photo.getString("category"));
                result.add(pic);
            }
        } else {
            result = null;
        }
        return result;
    }

    /**
     * Gets all photos for this user.
     *
     * @return A list of PictureObjects representing all photos for this user.
     */
    public List<PictureObject> getAllPhotos() {
        final String SERVICE = "getPhotos.php";
        // build GET Statement
        final StringBuilder sb = new StringBuilder();
        sb.append("?userid=" + userid);

        //Log.d("Starting debug photodb", "Starting debug");

        // Fetches all pictures for this user on a separate thread.
        new AsyncTask<PictureObject, Void, String>() {

            ProgressDialog progressDialog;
            @Override
            protected void onPreExecute() {
                super.onPreExecute();
                progressDialog = new ProgressDialog(myActivity);
                progressDialog.setIndeterminate(true);
                progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
                progressDialog.setMessage("Loading...");
                progressDialog.setCancelable(false);
                progressDialog.setCanceledOnTouchOutside(false);
                progressDialog.show();
            }

            /**
             * Executes the connection and fetches information in a thread separate from
             * the UI thread.
             * @param params The PictureObject to upload (if we need to send one).
             * @return The String JSON result.
             */
            @Override
            protected String doInBackground(PictureObject... params) {
                Log.d("IN On background", "doInBackground: " + PARTIAL_URL + SERVICE + sb.toString());
//                JSONParser
//                JSONObject json
                String response = "";
                HttpURLConnection urlConnection = null;
                try {
//                    Log.d("IN On background", "doInBackground: " + PARTIAL_URL + SERVICE + sb.toString());
                    URL urlObject = new URL(PARTIAL_URL + SERVICE + sb.toString());
//                    Log.d("In on background", "" + 1);
                    urlConnection = (HttpURLConnection) urlObject.openConnection();
//                    Log.d("In on background", "" + 2);
                    InputStream content = urlConnection.getInputStream();
//                    Log.d("In on background", "" + 3);
                    BufferedReader buffer = new BufferedReader(new InputStreamReader(content));
//                    Log.d("In on background", "" + 4);
                    String s = "";
//                    Log.d("In on background", "" + 5);

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

            /**
             /**
             * Processing the JSON String to PictureObjects.
             * @param result the JSON response from our server.
             */
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

                if (myActivity instanceof OverviewActivity) {
                    ((OverviewActivity) myActivity).updateTable(photoResult);
                }

                Log.d(TAG, "onPostExecute photoResult: " + photoResult);
                Log.d(TAG, "onPostExecute: photodata : " + photoData);
                progressDialog.dismiss();
            }
        }.execute();
        //Log.d("After async task", "getAllPhotos: " + photoResult.toString());
        return photoResult;
    }

    /**
     * Gets PictureObjects of only a specified category.
     *
     * @param category The String category selected by the user.
     * @return The list of PictureObjects in this category for this user.
     */
    public List<PictureObject> getCategoryAll(String category) {
        final String SERVICE = "getPhotoCat.php";
        // build GET Statement
        final StringBuilder sb = new StringBuilder();
        sb.append("?userid=" + userid);
        sb.append("&category=" + category);

        // Fetches all pictures for this user on a separate thread.
        new AsyncTask<PictureObject, Void, String>() {

            ProgressDialog progressDialog2;
            @Override
            protected void onPreExecute() {
                super.onPreExecute();
                progressDialog2 = new ProgressDialog(myActivity);
                progressDialog2.setIndeterminate(true);
                progressDialog2.setProgressStyle(ProgressDialog.STYLE_SPINNER);
                progressDialog2.setMessage("Loading...");
                progressDialog2.setCancelable(false);
                progressDialog2.setCanceledOnTouchOutside(false);
                progressDialog2.show();
            }

            /**
             * Executes the connection and fetches information in a thread separate from
             * the UI thread.
             * @param params The PictureObject to upload (if we need to send one).
             * @return The String JSON result.
             */
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

            /**
             * Processing the JSON String to PictureObjects.
             * @param result the JSON response from our server.
             */
            @Override
            protected void onPostExecute(String result) {
                if (result.startsWith("Unable to") || result.startsWith("There was an error")) {
                    Toast.makeText(context.getApplicationContext(), result, Toast.LENGTH_LONG)
                            .show();
                    return;
                } else if (result.startsWith("{\"Success")) {
                    try {
                        JSONObject root = new JSONObject(result);
                        JSONObject success = root.getJSONObject("Success");
                        photoResult = getJsonPhotos(success.getJSONArray("photoData"));


                    } catch (JSONException e) {
                        e.printStackTrace();
                    }


                } else if (result.startsWith("{\"Error")) {
                    try {
                        JSONObject root = new JSONObject(result);
                        JSONObject error = root.getJSONObject("Error");
                        photoResult = null;
                        //photoResult = getJsonPhotos(error.getJSONArray("photoData"));
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }

                if (myActivity instanceof OverviewActivity) {
                    ((OverviewActivity) myActivity).updateTable(photoResult);
                }
                progressDialog2.dismiss();
            }
        }.execute();
        return photoResult;
    }

    public List<PictureObject> updatePhoto(PictureObject pictureObject) {
        picData = pictureObject;
        final String SERVICE = "updatePhoto.php";
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

        // Submit the picture object information to our database.
        new AsyncTask<PictureObject, Void, String>() {

            /**
             * Executes the connection and fetches information in a thread separate from
             * the UI thread.
             * @param params The PictureObject to upload.
             * @return The String JSON result.
             */
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

            /**
             * Processing the JSON String to PictureObjects.
             * @param result the JSON response from our server.
             */
            @Override
            protected void onPostExecute(String result) {
                if (result.startsWith("Unable to") || result.startsWith("There was an error")) {
                    Toast.makeText(context.getApplicationContext(), result, Toast.LENGTH_LONG)
                            .show();
                    return;
                } else if (result.startsWith("{\"Success")) {
                    Toast.makeText(context, "Photo Data Updated", Toast.LENGTH_LONG).show();

//                    try {
//                        JSONObject root = new JSONObject(result);
//                        JSONObject success = root.getJSONObject("Success");
//                        photoResult = getJsonPhotos(success.getJSONArray("Status"));
//
////                        photoData = success.getJSONArray("photoData");
//                    } catch (JSONException e) {
//                        e.printStackTrace();
//                    }
                } else if (result.startsWith("{\"Error")) {
                    Toast.makeText(context, "No Update Occurred", Toast.LENGTH_LONG).show();
//                    try {
//                        JSONObject root = new JSONObject(result);
//                        JSONObject error = root.getJSONObject("Error");
//                        photoResult = getJsonPhotos(error.getJSONArray("photoData"));
//                    } catch (JSONException e) {
//                        e.printStackTrace();
//                    }
                }

//                if (myActivity instanceof OverviewActivity) {
//                    ((OverviewActivity) myActivity).updateTable(photoResult);
//                }
            }
        }.execute(pictureObject);
        return photoResult;
    }
}
