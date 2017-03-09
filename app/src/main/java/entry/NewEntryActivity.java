package entry;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.StrictMode;
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
import java.lang.reflect.Method;
import java.math.BigDecimal;

import picture.PhotoDB;
import picture.PictureObject;
import tcss450.uw.edu.gvtest.R;

public class NewEntryActivity extends AppCompatActivity implements AdapterView.OnItemSelectedListener {

    /**
     * The file location of the photo to be used.
     */
    private Uri mPhotoId;

    /**
     * The location which was found via REGEX.
     */
    private String mLocation;

    /**
     * The price which was found via REGEX.
     */
    private String mPrice;

    /**
     * The payment type which was found via REGEX.
     */
    private String mPaymentType;

    /**
     * The position of the spinner category found via REGEX.
     */
    private String mPosition;

    /**
     * The date which was found via REGEX.
     */
    private String mDate;

    /**
     * The category set by the spinner.
     */
    private String mCategory;

    /**
     * If the text was altered, edit the entry.
     */
    private boolean mEditEntry;

    /**
     * Used to create the UI and initialize instance fields.
     *
     * @param theSavedInstanceState The bundle containing information.
     */
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
        mPrice = intent.getStringExtra(OverviewActivity.TOTAL_AMOUNT);
        mLocation = intent.getStringExtra(OverviewActivity.LOCATION);
        mPaymentType = intent.getStringExtra(OverviewActivity.PAYMENT_TYPE);
        mDate = intent.getStringExtra(OverviewActivity.DATE);
        mPosition = intent.getStringExtra(OverviewActivity.POSITION);



        EditText locationEditText = (EditText) findViewById(R.id.locationId);
        locationEditText.setText(mLocation);
        EditText dateEdit = (EditText) findViewById(R.id.dateId);
        dateEdit.setText(mDate);
        EditText editText = (EditText) findViewById(R.id.amountId);
        editText.setText(mPrice);
        EditText paymentEdit = (EditText) findViewById(R.id.paymentId);
        paymentEdit.setText(mPaymentType);

        if (Build.VERSION.SDK_INT >= 24) {
            try {
                Method m = StrictMode.class.getMethod("disableDeathOnFileUriExposure");
                m.invoke(null);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        final String imagePath = intent.getStringExtra(OverviewActivity.GET_FILE_NAME);
        mPhotoId = Uri.parse(imagePath);

        Button retakePhotoButton = (Button) findViewById(R.id.button7);
        retakePhotoButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                Intent intent = new Intent(NewEntryActivity.this, OverviewActivity.class);
                intent.putExtra("Retake Picture", true);
                intent.putExtra("OldPath", imagePath);
                intent.putExtra(getString(R.string.category), "null");

                startActivity(intent);


            }
        });

        Log.d("mPhotoId", "onCreate: " + mPhotoId.getPath());
        File image = new File(mPhotoId.getPath());
        if (mPhotoId.toString().startsWith("content://")) {
            setImage(mPhotoId);
        } else if (image.exists()) {
            Log.d("image Exists", "onCreate: ");
            setImage(Uri.fromFile(image));
            // File was not found
        }
        mEditEntry = intent.getBooleanExtra("fromTable", false);
        if (mEditEntry) {
            Button b = (Button) findViewById(R.id.ok_button);
            b.setText(getString(R.string.updateStr));
            b = (Button) findViewById(R.id.new_entry_back_button);
            b.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    NewEntryActivity.this.onBackPressed();
                }
            });
            b.setVisibility(View.VISIBLE);

            String category = intent.getStringExtra(getString(R.string.category));
            if (!category.equals("null")) {
                int spinnerPosition = adapter.getPosition(category);
                spinner.setSelection(spinnerPosition);
            }
        } else if (!mEditEntry) {
            Button retakeB = (Button) findViewById(R.id.button7);
            retakeB.setVisibility(View.INVISIBLE);
        }
    }

    /**
     * Submits the entry information.
     *
     * @param theView The view used for event handling.
     */
    public void okButtonPress(View theView) {
        mLocation = ((EditText) findViewById(R.id.locationId)).getText().toString();
        mDate = ((EditText) findViewById(R.id.dateId)).getText().toString();
        mPrice = ((EditText) findViewById(R.id.amountId)).getText().toString();
        mPaymentType = ((EditText) findViewById(R.id.paymentId)).getText().toString();

        sendToDatabase(mPhotoId, mLocation, mPrice, mPaymentType, mDate, mCategory);

        Intent intent = new Intent(this, OverviewActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        intent.putExtra("Retake Picture", false);
        startActivity(intent);
        this.finish();
    }

    /**
     * Constructs a PictureObject and sends it to the database.
     *
     * @param thePhotoId     The unique photoid.
     * @param theLocation    The location the picture was taken.
     * @param thePrice       The price of the purchase.
     * @param thePaymentType The payment type.
     * @param theDate        The date of the purchase.
     * @param theCategory    The purchase category.
     */
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
        if (!mPrice.equals("Not Found") && Double.parseDouble(thePrice) > 0.0) {
            price = new BigDecimal(thePrice);
        } else {
            price = new BigDecimal(0.00001); //BigDecimal.ZERO;
        }

        PictureObject pictureObject = new PictureObject(userId, mPhotoId.getPath(),
                theLocation, price, thePaymentType, theDate, theCategory);
        PhotoDB photoDB = new PhotoDB(this);
        if (mEditEntry) {
            photoDB.updatePhoto(pictureObject);
        } else {
            photoDB.addPhoto(pictureObject);
        }
    }

    /**
     * Sets the image so it is clickable and the image becomes full screen.
     *
     * @param theUri The image information.
     */
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

    /**
     * A method which is automatically called every time a spinner item is selected.
     *
     * @param parent   the AdapterView that triggered the event
     * @param view     The view within the AdapterView that triggered the event
     * @param position The position of the view that was selected
     * @param id       The row id of the item that was selected
     */
    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        Spinner spinner = (Spinner) findViewById(R.id.category_assigner);
        String selectedCategory = spinner.getSelectedItem().toString();
        if (position > 0) {
            mCategory = selectedCategory;
        }
    }

    /**
     * An obligatory empty method to implement OnItemSelectedListener.
     *
     * @param parent the parent adapter.
     */
    @Override
    public void onNothingSelected(AdapterView<?> parent) {

    }

}
