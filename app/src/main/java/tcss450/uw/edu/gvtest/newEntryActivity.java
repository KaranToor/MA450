package tcss450.uw.edu.gvtest;

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
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

public class newEntryActivity extends AppCompatActivity implements AdapterView.OnItemSelectedListener {
    public static final int GALLERY_PERMISSIONS_REQUEST = 0;
    public static final int GALLERY_IMAGE_REQUEST = 1;
    public static final int CAMERA_PERMISSIONS_REQUEST = 2;
    public static final int CAMERA_IMAGE_REQUEST = 3;

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

        /////////put in if statement? to bottom slashes ////////////////////////////////////////
        Button retakePhotoButton = (Button) findViewById(R.id.button7);
        retakePhotoButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                AlertDialog.Builder builder = new AlertDialog.Builder(newEntryActivity.this);
                builder
                        .setMessage(R.string.dialog_select_prompt)
                        .setPositiveButton(R.string.dialog_select_gallery, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                if (PermissionUtils.requestPermission(newEntryActivity.this, GALLERY_PERMISSIONS_REQUEST,
                                        Manifest.permission.READ_EXTERNAL_STORAGE)) {
                                    Intent intent = new Intent();
                                    intent.setType(getString(R.string.intenttype));
                                    intent.setAction(Intent.ACTION_GET_CONTENT);
                                    startActivityForResult(Intent.createChooser(intent, getString(R.string.choosePhotoPrompt)),
                                            GALLERY_IMAGE_REQUEST);
                                }
                            }
                        })
                        .setNegativeButton(R.string.dialog_select_camera, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                try {
                                    if (PermissionUtils.requestPermission(
                                            newEntryActivity.this,
                                            CAMERA_PERMISSIONS_REQUEST,
                                            Manifest.permission.READ_EXTERNAL_STORAGE,
                                            Manifest.permission.CAMERA)) {
                                        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                                        intent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(getCameraFile()));

                                        if (Build.VERSION.SDK_INT >= 24) {
                                            try {
                                                Method m = StrictMode.class.getMethod("disableDeathOnFileUriExposure");
                                                m.invoke(null);
                                            } catch (Exception e) {
                                                e.printStackTrace();
                                            }
                                        }
                                        startActivityForResult(intent, CAMERA_IMAGE_REQUEST);
                                    }
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }
                            }
                        });
                builder.create().show();
            }
        });
////////////////////////////////////////////////////////////////////////////////
        myPhotoId = Uri.parse(intent.getStringExtra(OverviewActivity.CAMERA_OR_GALLERY));
        if (new File(myPhotoId.getPath()).exists() || myPhotoId.toString().startsWith("content://")) {
            setImage(myPhotoId);
        } else {
            // File was not found
        }
//        setImage(myPhotoId);

        if (isEditEntry = intent.getBooleanExtra("fromTable", false)) {
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
            //Spinner mSpinner = (Spinner) findViewById(R.id.category_assigner);

            String category = intent.getStringExtra(getString(R.string.category));
            //spinner.setAdapter(adapter);
            if (!category.equals(null)) {
                int spinnerPosition = adapter.getPosition(category);
                spinner.setSelection(spinnerPosition);
            }
        }
    }



    //ADDED THIS METHOD FROM OVERVIEW
    private File getCameraFile() throws IOException {
        // Create an image file name
        SharedPreferences prefs = getApplicationContext().getSharedPreferences(getString(R.string.prefKey), Context.MODE_PRIVATE);
        int userid = prefs.getInt(getString(R.string.UID), -1);
        if (userid == -1) {
            throw new IllegalArgumentException("userid is not set in Prefs");
        }
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = userid + "_" + timeStamp + "_";
        File storageDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
        //File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
//        File image = File.createTempFile(
//                imageFileName,  /* prefix */
//                ".jpg",         /* suffix */
//                storageDir      /* directory */
//        );

        File image = new File(
                storageDir,
                imageFileName +  /* prefix */
                        ".jpg"        /* suffix */
                      /* directory */
        );

        // Save a file: path for use with ACTION_VIEW intents
        return image;
    }

    public void okButtonPress(View theView) {

        myLocation = ((EditText) findViewById(R.id.locationId)).getText().toString();
        myDate = ((EditText) findViewById(R.id.dateId)).getText().toString();
        myPrice = ((EditText) findViewById(R.id.amountId)).getText().toString();
        myPaymentType = ((EditText) findViewById(R.id.paymentId)).getText().toString();

        sendToDatabase(myPhotoId, myLocation, myPrice, myPaymentType, myDate, myCategory);

        Intent intent = new Intent(this, OverviewActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
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
        if (!myPrice.equals("Not Found")) {
            price = new BigDecimal(thePrice);
        } else {
            price = BigDecimal.ZERO;
        }

        // TODO Toork PhotoId.toString()
        PictureObject pictureObject = new PictureObject(userId, myPhotoId.toString(),
                theLocation, price, thePaymentType, theDate, theCategory);
        PhotoDB photoDB = new PhotoDB(this);
        photoDB.addPhoto(pictureObject);
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
//        Log.d("selectedCat", myCategory);
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {

    }

}
