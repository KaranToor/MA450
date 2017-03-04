package tcss450.uw.edu.gvtest;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;

import java.net.HttpURLConnection;
import java.net.URL;

import android.content.Intent;
import android.os.AsyncTask;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;


/**
 * Initial screen upon app startup. Provides user capability to login
 * or navigate to another screen to register.
 *
 * @author MA450 Team 11
 * @version Winter 2017
 */
public class MainActivity extends AppCompatActivity {
    private static final String PARTIAL_URL = "http://cssgate.insttech.washington.edu/~ekoval/";

    EditText myEmail;
    EditText myPassword;

    /**
     * Initializes activity
     *
     * @param theSavedInstanceState the instance state.
     */
    @Override
    protected void onCreate(Bundle theSavedInstanceState) {
        super.onCreate(theSavedInstanceState);
        setContentView(R.layout.activity_main);
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
     * Directs the myEmail to the overview activity after logging in.
     *
     * @param theView The View passed in when this method is called.
     */
    public void logInUser(View theView) {
        myEmail = (EditText) findViewById(R.id.editText2);
        myPassword = (EditText) findViewById(R.id.editText3);
        if (SignUpActivity.isValidEmail(myEmail.getText())) {

            if (myPassword.getText().toString().length() >= 1) {
                AsyncTask<String, Void, String> task = null;
                String message = ((EditText) findViewById(R.id.editText2)).getText().toString();
                String message2 = ((EditText) findViewById(R.id.editText3)).getText().toString();

                task = new GetWebServiceTask();
                task.execute(PARTIAL_URL, message, message2);

            } else {
                Toast.makeText(this, getString(R.string.incompleteFormMsg), Toast.LENGTH_LONG).show();

            }
        } else {
            myEmail.requestFocus();
            myEmail.setError(getString(R.string.invalidEmailMsg));
        }

    }

    /**
     * Asynchronously verifies login credentials
     */
    private class GetWebServiceTask extends AsyncTask<String, Void, String> {
        private final String SERVICE = getString(R.string.loginExtension);
        Button submit = (Button) findViewById(R.id.button2);


        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            submit.setClickable(false);
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
            if (theResult.startsWith(getString(R.string.unable))) {
                Toast.makeText(getApplicationContext(), theResult, Toast.LENGTH_LONG)
                        .show();
                return;
            } else if (theResult.contains(getString(R.string.err))) {
                Toast.makeText(getApplicationContext(), theResult, Toast.LENGTH_LONG)
                        .show();
                return;
            } else {
                startActivity(new Intent(getApplicationContext(), OverviewActivity.class));
            }

        }
    }

}