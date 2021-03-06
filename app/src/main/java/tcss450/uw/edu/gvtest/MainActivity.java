package tcss450.uw.edu.gvtest;

import android.content.Context;
import android.content.SharedPreferences;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;

import java.net.HttpURLConnection;
import java.net.URL;

import android.content.Intent;
import android.os.AsyncTask;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;

import entry.OverviewActivity;


/**
 * Initial screen upon app startup. Provides user capability to login
 * or navigate to another screen to register.
 *
 * @author MA450 Team 11
 * @version Winter 2017
 */
public class MainActivity extends AppCompatActivity {
    /**
     * Used to log into cssgate.
     */
    private static final String PARTIAL_URL = "http://cssgate.insttech.washington.edu/~ekoval/";

    /**
     * The EditText which holds the inputted Email.
     */
    private EditText mEmail;

    /**
     * The EditText which holds the inputted password.
     */
    private EditText mPassword;

    /**
     * The application's SharedPreferences
     */
    private SharedPreferences mPrefs;

    /**
     * A field to hold the unique ID number of the user.
     */
    private int mUserId;

    /**
     * Initializes activity
     *
     * @param theSavedInstanceState the instance state.
     */
    @Override
    protected void onCreate(Bundle theSavedInstanceState) {
        super.onCreate(theSavedInstanceState);
        mPrefs = getApplicationContext().getSharedPreferences(
                getString(R.string.prefKey), Context.MODE_PRIVATE);
        setContentView(R.layout.activity_main);
    }

    /**
     * Starts the activity.
     */
    @Override
    protected void onStart() {
        super.onStart();
//        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        boolean isLoggedIn = mPrefs.getBoolean(getString(R.string.isloggedin), false);
        System.out.print(isLoggedIn);
        if (isLoggedIn) {
            boolean hasPIN = mPrefs.getBoolean(getString(R.string.hasPIN), false);
            if (!hasPIN) {
                startActivity(new Intent(getApplicationContext(), OverviewActivity.class));
                System.out.print(isLoggedIn);
            } else {
                startActivity(new Intent(getApplicationContext(), PINEntry.class));
            }
        }
    }

    /**
     * Navigates to the "sign up" activity
     *
     * @param theView The View passed in when this method is called.
     */
    public void registerUser(View theView) {
        Intent intent = new Intent(this, SignUpActivity.class);
        startActivity(intent);
    }

    /**
     * Directs the mEmail to the overview activity after logging in.
     *
     * @param theView The View passed in when this method is called.
     */
    public void logInUser(View theView) {
        mEmail = (EditText) findViewById(R.id.editText2);
        mPassword = (EditText) findViewById(R.id.editText3);

        if (SignUpActivity.isValidEmail(mEmail.getText()) && mPassword.getText().length() > 7) {

            if (mPassword.getText().toString().length() >= 1) {
                AsyncTask<String, Void, String> task = null;
                String message = ((EditText) findViewById(R.id.editText2)).getText().toString();
                String message2 = ((EditText) findViewById(R.id.editText3)).getText().toString();

                task = new GetWebServiceTask();
                task.execute(PARTIAL_URL, message, message2);

            } else {
                Toast.makeText(this, getString(R.string.incompleteFormMsg), Toast.LENGTH_LONG).show();

            }
        } else if (!SignUpActivity.isValidEmail(mEmail.getText())) {
            mEmail.requestFocus();
            mEmail.setError(getString(R.string.invalidEmailMsg));
        } else {
            mPassword.requestFocus();
            mPassword.setError(getString(R.string.passwordErrorMessage));
        }

    }

    /**
     * Asynchronously verifies login credentials
     */
    private class GetWebServiceTask extends AsyncTask<String, Void, String> {
        private final String SERVICE = getString(R.string.loginExtension);
        Button submit = (Button) findViewById(R.id.button2);

        /**
         * Things to be done before executing the AsyncTask.
         */
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            submit.setClickable(false);
            SharedPreferences.Editor editor = mPrefs.edit();
            editor.putBoolean(getString(R.string.isloggedin), true);
            editor.commit();
        }

        /**
         * Executes async tasks on a separate thread.
         *
         * @param theStrings The parameters to be passed in.
         * @return The result.
         */
        @Override
        protected String doInBackground(String... theStrings) {
            String response = "";
            HttpURLConnection urlConnection = null;
            String url = theStrings[0];
            String args = getString(R.string.queryUN) + theStrings[1] + getString(R.string.queryPass) + theStrings[2];
            try {
                URL urlObject = new URL(url + SERVICE + args);
                urlConnection = (HttpURLConnection) urlObject.openConnection();
                InputStream content = urlConnection.getInputStream();
                BufferedReader buffer = new BufferedReader(new InputStreamReader(content));
                String s = "";
                while ((s = buffer.readLine()) != null) {
                    response += s;
                }
            } catch (Exception e) {
                response = getString(R.string.unableToConnect)
                        + e.getMessage();
            } finally {
                if (urlConnection != null)
                    urlConnection.disconnect();
            }
            return response;
        }

        /**
         * Begins the "overview" activity
         *
         * @param theResult the result.
         */
        @Override
        protected void onPostExecute(String theResult) {

            // Something wrong with the network or the URL.
            submit.setClickable(true);
            if (theResult.contains("Success")) {
                try {
                    mUserId = getUserId(theResult);
                    SharedPreferences.Editor editor = mPrefs.edit();
                    editor.putInt(getString(R.string.UID), mUserId);
                    editor.commit();
                    Log.d("AFTER LOGIN :", "onPostExecute: result " + theResult);
                    Log.d("UID:", mPrefs.getInt(getString(R.string.UID), -1) + "");
                    startActivity(new Intent(getApplicationContext(), OverviewActivity.class));
                } catch (JSONException e) {
                    e.printStackTrace();
                }

            } else if (theResult.contains("Error")) {
                if (theResult.contains("Username Does not Exist")) {
                    mEmail.requestFocus();
                    mEmail.setError("Username Does Not Exist");
                } else if (theResult.contains("Incorrect Password")) {
                    mPassword.requestFocus();
                    mPassword.setError("Incorrect Password");
                }
            }
        }
    }

    /**
     * Parses the result for the ID of the current user.
     *
     * @param theResult is the result
     * @return the ID number of the logged-in user
     * @throws JSONException
     */
    public int getUserId(String theResult) throws JSONException {
        int uId = -1;
        try {
            JSONObject json = new JSONObject(theResult);
            JSONObject obj = json.getJSONObject("Success");
            uId = obj.getInt("ID#");
            System.out.println("ID is " + uId);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        if (uId == -1) {
            throw new JSONException("ID was unable to be parsed");
        }
        return uId;
    }
}