package tcss450.uw.edu.gvtest;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

import entry.OverviewActivity;

/**
 * @author MA450 Team 11
 * @version Winter 2017
 * Adds a new user to the app's database and allows for future logins from this new user.
 */
public class SignUpActivity extends AppCompatActivity {
    /**
     * The partial URL location used to access the server.
     */
    private static final String PARTIAL_URL = "http://cssgate.insttech.washington.edu/~ekoval/";

    /**
     * The field which contains the desired username.
     */
    EditText myUser;

    /**
     * The field which contains the desired password.
     */
    EditText myPassword;

    /**
     * The shared preferences for the application.
     */
    SharedPreferences mPrefs;

    /**
     * The ID number of the current user.
     */
    private int myUserId;

    /**
     * Initializes activity.
     *
     * @param theSavedInstanceState the instance that is saved.
     */
    @Override
    protected void onCreate(Bundle theSavedInstanceState) {
        super.onCreate(theSavedInstanceState);
        mPrefs = getApplicationContext().getSharedPreferences(getString(R.string.prefKey), Context.MODE_PRIVATE);
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
        CheckBox checkBox = (CheckBox) findViewById(R.id.checkBox);
        EditText pinBox = (EditText) findViewById(R.id.newPIN);
        EditText nameTextField = (EditText) findViewById(R.id.editText);

        if (nameTextField.getText().toString().length() < 1) {
            nameTextField.requestFocus();
            nameTextField.setError("Please enter a Name");
        }
        else if (myUser.getText().toString().length() < 1) {
            myUser.requestFocus();
            myUser.setError("Please enter an email address");
        }

         else if (myPassword.getText().length() < 8) {
            myPassword.requestFocus();
            myPassword.setError(getString(R.string.passwordErrorMessage));
        } else if (myPass2.getText().length() < 8) {
            myPass2.requestFocus();
            myPass2.setError(getString(R.string.passwordErrorMessage));
        } else if (checkBox.isChecked() && pinBox.getText().toString().length() < 4) {
            pinBox.requestFocus();
            pinBox.setError("PIN must contain at least 4 digits");
        } else if(!isValidEmail(myUser.getText())){
            myUser.requestFocus();
            myUser.setError("Please enter an email with address@example.com format");
        } else if (isValidEmail(myUser.getText()) && myPassword.getText().length() >= 8
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
     * Allows creating a pin or not, depending on the user's choice
     * @param view The view that was pressed
     */
    public void setPINFieldVisibility(View view) {
        CheckBox checkBox = (CheckBox) view;
        EditText pinBox = (EditText) findViewById(R.id.newPIN);
        LinearLayout linearLayout = (LinearLayout) findViewById(R.id.activity_sign_up);
        if (checkBox.isChecked()) {
            pinBox.setVisibility(View.VISIBLE);
        } else {
            pinBox.setVisibility(View.GONE);
        }
        pinBox.invalidate();
        linearLayout.requestLayout();
    }

    /**
     * Returns to the previous activity when the cancel button is pressed.
     * @param view the view that was pressed
     */
    public void cancelRegistration(View view) {
        this.onBackPressed();
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
            CheckBox checkBox = (CheckBox) findViewById(R.id.checkBox);
            EditText pinBox = (EditText) findViewById(R.id.newPIN);
            //            SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
            SharedPreferences.Editor editor = mPrefs.edit();
            if (checkBox.isChecked()) {
                editor.putBoolean(getString(R.string.hasPIN), true);
                editor.putString(getString(R.string.PINNum), pinBox.getText().toString());
                editor.commit();
            } else {
                editor.putBoolean(getString(R.string.hasPIN), false);
                editor.commit();
            }
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
            if (theResult.contains("Success")) {
                try {
                    myUserId = getUserId(theResult);
                    SharedPreferences.Editor editor = mPrefs.edit();
                    editor.putInt(getString(R.string.UID), myUserId);
                    editor.commit();
                    Intent intent = new Intent(getApplicationContext(), OverviewActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    getApplicationContext().startActivity(intent);
                } catch (JSONException e) {
                    e.printStackTrace();
                }

            } else if (theResult.contains("Error")) {
                if (theResult.contains("Already Registered")) {
                    EditText editText = (EditText) findViewById(R.id.editText4);
                    editText.requestFocus();
                    editText.setError("Error: Email already registered");
                }
            }
        }
    }

    /**
     * Returns the user Id
     * @param theResult the String of user data
     * @return uId the user's id to be retrieved
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
