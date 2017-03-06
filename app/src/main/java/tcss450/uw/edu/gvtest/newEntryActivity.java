package tcss450.uw.edu.gvtest;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;

import java.io.File;
import java.math.BigDecimal;


public class newEntryActivity extends AppCompatActivity {
    private Uri myPhotoId;
    private String myLocation;
    private String myPrice;
    private String myPaymentType;
    private String myDate;
    private String myCategory;

    @Override
    protected void onCreate(Bundle theSavedInstanceState) {
        super.onCreate(theSavedInstanceState);
        setContentView(R.layout.activity_new_entry);

        Intent intent = getIntent();
        myPrice = intent.getStringExtra(OverviewActivity.TOTAL_AMOUNT);
        myLocation = intent.getStringExtra(OverviewActivity.LOCATION);
        myPaymentType = intent.getStringExtra(OverviewActivity.PAYMENT_TYPE);
        myDate = intent.getStringExtra(OverviewActivity.DATE);

        EditText locationEditText = (EditText) findViewById(R.id.locationId);
        locationEditText.setText(myLocation);
        EditText dateEdit = (EditText) findViewById(R.id.dateId);
        dateEdit.setText(myDate);
        EditText editText = (EditText) findViewById(R.id.amountId);
        editText.setText(myPrice);
        EditText paymentEdit = (EditText) findViewById(R.id.paymentId);
        paymentEdit.setText(myPaymentType);

        myPhotoId = Uri.parse(intent.getStringExtra(OverviewActivity.CAMERA_OR_GALLERY));
        setImage(myPhotoId);
    }

    private void okButtonPressed(View theView) {

        sendToDatabase(myPhotoId, myLocation, myPrice, myPaymentType, myDate, myCategory);
    }

    private void sendToDatabase(Uri thePhotoId, String theLocation, String thePrice,
                                String thePaymentType, String theDate,
                                String theCategory) {
        SharedPreferences prefs = getApplicationContext().getSharedPreferences(
                getString(R.string.prefKey), Context.MODE_PRIVATE);
        int userId = prefs.getInt(getString(R.string.UID), -1);
        if (userId == -1) {
            throw new IllegalArgumentException("UserId was not set in SharedPreferences");
        }
        BigDecimal price = new BigDecimal(thePrice);

        PictureObject pictureObject = new PictureObject(userId, thePhotoId.toString(),
                theLocation, price, thePaymentType, theDate, theCategory);
        PhotoDB photoDB = new PhotoDB(getApplicationContext());
        photoDB.addPhoto(pictureObject);

    }

    public void retakeClicked(View theView) {

    }

    private void setImage(Uri theUri) {
        final ImageView imageView = (ImageView) findViewById(R.id.imageView);
        imageView.setImageURI(theUri);
    }
}
