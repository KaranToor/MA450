package tcss450.uw.edu.gvtest;

import android.content.Intent;
import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * Adds a new user to the app's database and allows for future logins from this new user.
 */
public class SignUpActivity extends AppCompatActivity {
    private static final String PARTIAL_URL = "http://cssgate.insttech.washington.edu/~ekoval/";
    EditText myUser;
    EditText myPassword;

    /**
     * Initializes activity.
     *
     * @param theSavedInstanceState the instance that is saved.
     */
    @Override
    protected void onCreate(Bundle theSavedInstanceState) {
        super.onCreate(theSavedInstanceState);
        setContentView(R.layout.activity_sign_up);
    }

    /**
     * Creates a new user.
     *
     * @param theView The view passed in when this method is called
     */
    public void createUser(View theView) {
        myUser = (EditText) findViewById(R.id.editText4);
        myPassword = (EditText) findViewById(R.id.editText6);
        EditText myPass2 = (EditText) findViewById(R.id.editText7);

        if (isValidEmail(myUser.getText()) && myPassword.getText().length() >= 8
                && myPass2.getText().length() >= 8) {

            if (myPass2.getText().toString().equals(myPassword.getText().toString())) {

                if (myUser.getText().toString().length() >= 1 && myPassword.getText().toString().length() >= 1) {
                    AsyncTask<String, Void, String> task = null;
                    String message = ((EditText) findViewById(R.id.editText)).getText().toString();
                    String message2 = ((EditText) findViewById(R.id.editText4)).getText().toString();
                    String message3 = ((EditText) findViewById(R.id.editText6)).getText().toString();
                    task = new CreatingUserWebServiceTask();
                    task.execute(PARTIAL_URL, message2, message3, message);
                } else {
                    Toast.makeText(this, R.string.incompleteFormMsg, Toast.LENGTH_LONG).show();
                }
            } else {
                myPass2.requestFocus();
                myPass2.setError(getString(R.string.confirmPasswordErr));
            }
        } else if (myPassword.getText().length() < 8){
            myPassword.requestFocus();
            myPassword.setError(getString(R.string.passwordErrorMessage));
        } else if (myPass2.getText().length() < 8) {
            myPass2.requestFocus();
            myPass2.setError(getString(R.string.passwordErrorMessage));
        }
    }

    /**
     * From http://stackoverflow.com/questions/24969894/android-email-validation-on-edittext
     * to validate email imput.
     *
     * @param target the user input.
     * @return The result of the validation.
     */
    public final static boolean isValidEmail(CharSequence target) {
        return !TextUtils.isEmpty(target) && android.util.Patterns.EMAIL_ADDRESS.matcher(target).matches();
    }

    /**
     * Asynchronously reaches out to the app's database and adds the credentials of the new user
     */
    private class CreatingUserWebServiceTask extends AsyncTask<String, Void, String> {
        private final String SERVICE = "register.php";
        Button submit = (Button) findViewById(R.id.button3);

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            submit.setClickable(false);
        }

        /**
         * Performs operations in separate thread.
         *
         * @param theStrings The string parameters.
         * @return the result.
         */
        @Override
        protected String doInBackground(String... theStrings) {
            String response = "";
            HttpURLConnection urlConnection = null;
            String url = theStrings[0];
            String args = "?username=" + theStrings[1] + "&password=" + theStrings[2] + "&name=" + theStrings[3];
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
                response = "Unable to connect, Reason: "
                        + e.getMessage();
            } finally {
                if (urlConnection != null)
                    urlConnection.disconnect();
            }
            return response;
        }

        /**
         * After async task is complete, show result.
         *
         * @param theResult The completed result.
         */
        @Override
        protected void onPostExecute(String theResult) {
            // Something wrong with the network or the URL.
            submit.setClickable(true);
            if (theResult.startsWith(getString(R.string.unable))) {
                Toast.makeText(getApplicationContext(), theResult, Toast.LENGTH_LONG)
                        .show();
            } else if (theResult.contains(getString(R.string.err))) {
                Toast.makeText(getApplicationContext(), theResult, Toast.LENGTH_LONG)
                        .show();
            } else {
                Intent intent = new Intent(getApplicationContext(), OverviewActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                getApplicationContext().startActivity(intent);
            }
        }
    }
}
