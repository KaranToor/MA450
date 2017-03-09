package entry;

import android.Manifest;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.StrictMode;
import android.provider.MediaStore;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.sql.SQLOutput;
import java.text.SimpleDateFormat;
import java.util.Date;

import entry.OverviewActivity;
import picture.PhotoDB;
import picture.PictureObject;
import tcss450.uw.edu.gvtest.R;
import utils.PermissionUtils;

public class newEntryActivity extends AppCompatActivity implements AdapterView.OnItemSelectedListener {

    /**
     * The file location of the photo to be used.
     */
    private Uri myPhotoId;

    /**
     * The location which was found via REGEX.
     */
    private String myLocation;

    /**
     * The price which was found via REGEX.
     */
    private String myPrice;

    /**
     * The payment type which was found via REGEX.
     */
    private String myPaymentType;

    /**
     * The date which was found via REGEX.
     */
    private String myDate;

    /**
     * The category set by the spinner.
     */
    private String myCategory;

    private boolean isEditEntry;


    @Override
    protected void onCreate(Bundle theSavedInstanceState) {
        super.onCreate(theSavedInstanceState);
        setContentView(R.layout.activity_new_entry);

        Spinner spinner = (Spinner) findViewById(R.id.category_assigner);
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
                R.array.category_assignment, android.R.layout.simple_spinner_item);
// Specify the layout to use when the list of choices appears
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
// Apply the adapter to the spinner
        spinner.setAdapter(adapter);
        spinner.setOnItemSelectedListener(this);

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

        if (Build.VERSION.SDK_INT >= 24) {
            try {
                Method m = StrictMode.class.getMethod("disableDeathOnFileUriExposure");
                m.invoke(null);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        final String imagePath = intent.getStringExtra(OverviewActivity.CAMERA_OR_GALLERY);
        myPhotoId = Uri.parse(imagePath);  ///intent.getStringExtra(OverviewActivity.CAMERA_OR_GALLERY));

        /////////put in if statement? to bottom slashes ////////////////////////////////////////
        Button retakePhotoButton = (Button) findViewById(R.id.button7);
        retakePhotoButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                Intent intent = new Intent(newEntryActivity.this, OverviewActivity.class);
                intent.putExtra("Retake Picture", true);
                intent.putExtra("OldPath", imagePath);
                intent.putExtra(getString(R.string.category), "null");

                startActivity(intent);


            }
        });

        Log.d("myPhotoId", "onCreate: " + myPhotoId.getPath());
        File image = new File(myPhotoId.getPath());
        if (myPhotoId.toString().startsWith("content://")) {
            setImage(myPhotoId);
        } else if (image.exists()) {
            Log.d("image Exists", "onCreate: ");
            setImage(Uri.fromFile(image));
            // File was not found
        }
        isEditEntry = intent.getBooleanExtra("fromTable", false);
        if (isEditEntry) {
            Button b = (Button) findViewById(R.id.ok_button);
            b.setText(getString(R.string.updateStr));
            b = (Button) findViewById(R.id.new_entry_back_button);
            b.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    newEntryActivity.this.onBackPressed();
                }
            });
            b.setVisibility(View.VISIBLE);

            String category = intent.getStringExtra(getString(R.string.category));
            if (!category.equals("null")) {
                int spinnerPosition = adapter.getPosition(category);
                spinner.setSelection(spinnerPosition);
            }
        }
    }


    public void okButtonPress(View theView) {
        myLocation = ((EditText) findViewById(R.id.locationId)).getText().toString();
        myDate = ((EditText) findViewById(R.id.dateId)).getText().toString();
        myPrice = ((EditText) findViewById(R.id.amountId)).getText().toString();
        myPaymentType = ((EditText) findViewById(R.id.paymentId)).getText().toString();

        sendToDatabase(myPhotoId, myLocation, myPrice, myPaymentType, myDate, myCategory);


        Intent intent = new Intent(this, OverviewActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        intent.putExtra("Retake Picture", false);
        startActivity(intent);
        this.finish();
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

        BigDecimal price;
        Log.d("BIGDEC ZERO", "sendToDatabase: " + BigDecimal.ZERO);
        if (!myPrice.equals("Not Found") && Double.parseDouble(thePrice) > 0.0){
            price = new BigDecimal(thePrice);
        } else {
            price = new BigDecimal(0.00001); //BigDecimal.ZERO;
        }

        // TODO Toork PhotoId.toString()
        PictureObject pictureObject = new PictureObject(userId, myPhotoId.getPath(),
                theLocation, price, thePaymentType, theDate, theCategory);
        PhotoDB photoDB = new PhotoDB(this);
        if (isEditEntry){
            photoDB.updatePhoto(pictureObject);
        }else {
            photoDB.addPhoto(pictureObject);
        }
    }

    public void retakeClicked(View theView) throws IOException {
//        getApplicationContext().
//        OverviewActivity oa = new OverviewActivity();
//        //oa.onCreate(null);
//        oa.cameraButtonClicked(theView.findViewById(R.id.cameraButton));
    }

    private void setImage(final Uri theUri) {
        final ImageView imageView = (ImageView) findViewById(R.id.imageView);

        imageView.setImageURI(theUri);
        imageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent();
                intent.setAction(Intent.ACTION_VIEW);
                intent.setDataAndType(theUri, "image/*");
                startActivity(intent);
            }
        });
    }

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        Spinner spinner = (Spinner) findViewById(R.id.category_assigner);
        String selectedCategory = spinner.getSelectedItem().toString();
        if (position > 0) {
            myCategory = selectedCategory;
        }
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {

    }

}
