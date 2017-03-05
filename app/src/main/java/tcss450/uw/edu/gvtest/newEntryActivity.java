package tcss450.uw.edu.gvtest;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.widget.EditText;


public class newEntryActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle theSavedInstanceState) {
        super.onCreate(theSavedInstanceState);
        setContentView(R.layout.activity_new_entry);

        Intent intent = getIntent();
        String message = intent.getStringExtra(OverviewActivity.TOTAL_AMOUNT);
        String loc = intent.getStringExtra(OverviewActivity.LOCATION);
        String payment = intent.getStringExtra(OverviewActivity.PAYMENT_TYPE);
        String date = intent.getStringExtra(OverviewActivity.DATE);

        EditText locationEditText = (EditText) findViewById(R.id.locationId);
        locationEditText.setText(loc);
        EditText dateEdit = (EditText) findViewById(R.id.dateId);
        dateEdit.setText(date);
        EditText editText = (EditText) findViewById(R.id.amountId);
        editText.setText(message);
        EditText paymentEdit = (EditText) findViewById(R.id.paymentId);
        paymentEdit.setText(payment);
    }
}
