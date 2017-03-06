package tcss450.uw.edu.gvtest;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.health.ServiceHealthStats;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;

/**
 * Provides a pin entry for users to sign in rather than with a myEmail and myPassword.
 *
 * @author MA450 Team 11
 * @version Winter 2017
 */
public class PINEntry extends AppCompatActivity {

    /**
     * The shared preferences for the application.
     */
    SharedPreferences mPrefs;

    /**
     * Creates the activity and sets the state if savedInstanceState is passed.
     *
     * @param theSavedInstanceState Saves the previous state of the activiy if previously
     *                           created.
     */
    @Override
    protected void onCreate(Bundle theSavedInstanceState) {
        super.onCreate(theSavedInstanceState);
        setContentView(R.layout.activity_pinentry);
        mPrefs = getApplicationContext().getSharedPreferences(getString(R.string.prefKey), Context.MODE_PRIVATE);
    }

    public void verifyPIN(View view) {
        EditText editText = (EditText)findViewById(R.id.PINInput);
//        SharedPreferences sharedPref = getPreferences(Context.MODE_PRIVATE);
        String PIN = mPrefs.getString(getString(R.string.PINNum), "null");
        if (PIN.equals("null")){
            editText.requestFocus();
            editText.setError("No PIN found");
        } else if (PIN.equals(editText.getText().toString())){
            startActivity(new Intent(getApplicationContext(), OverviewActivity.class));
        } else {
            editText.requestFocus();
            editText.setError("Incorrect PIN");
        }
    }
}
