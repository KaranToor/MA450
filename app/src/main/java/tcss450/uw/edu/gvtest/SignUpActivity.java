package tcss450.uw.edu.gvtest;

import android.content.Intent;
import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;


public class SignUpActivity extends AppCompatActivity {
    private static final String PARTIAL_URL = "http://cssgate.insttech.washington.edu/~ekoval/";
    EditText u;
    EditText p;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_up);
    }

    public void createUser(View view) {
        u = (EditText) findViewById(R.id.editText4);
        p = (EditText) findViewById(R.id.editText6);
        if (u.getText().toString().length() >= 1 && p.getText().toString().length() >= 1) {
            AsyncTask<String, Void, String> task = null;
            String message = ((EditText) findViewById(R.id.editText)).getText().toString();
            String message2 = ((EditText) findViewById(R.id.editText4)).getText().toString();
            String message3 = ((EditText) findViewById(R.id.editText6)).getText().toString();
            task = new CreatingUserWebServiceTask();
            task.execute(PARTIAL_URL, message2, message3, message);

        } else {
            Toast.makeText(this, "All fields must be filled", Toast.LENGTH_LONG).show();
        }
    }

    private class CreatingUserWebServiceTask extends AsyncTask<String, Void, String> {
        private final String SERVICE = "register.php";

        @Override
        protected String doInBackground(String... strings) {
            String response = "";
            HttpURLConnection urlConnection = null;
            String url = strings[0];
            String args = "?username=" + strings[1] + "&password=" + strings[2] + "&name=" + strings[3];
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

        @Override
        protected void onPostExecute(String result) {
            // Something wrong with the network or the URL.
            if (result.startsWith("Unable to")) {
                Toast.makeText(getApplicationContext(), result, Toast.LENGTH_LONG)
                        .show();
                return;
            } else if (result.contains("Error")){
                Toast.makeText(getApplicationContext(), result, Toast.LENGTH_LONG)
                        .show();
                return;
            }  else {
                getApplicationContext().startActivity(new Intent(getApplicationContext(), OverviewActivity.class));
            }

        }
    }

}
