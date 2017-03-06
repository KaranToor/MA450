package tcss450.uw.edu.gvtest;

import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;

import java.io.File;


public class newEntryActivity extends AppCompatActivity implements AdapterView.OnItemSelectedListener {
    String category = "null";
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

        Spinner spinner = (Spinner) findViewById(R.id.category_assigner);
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
                R.array.category_assignment, android.R.layout.simple_spinner_item);
// Specify the layout to use when the list of choices appears
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
// Apply the adapter to the spinner
        spinner.setAdapter(adapter);
        spinner.setOnItemSelectedListener(this);

        Uri image = Uri.parse(intent.getStringExtra(OverviewActivity.CAMERA_OR_GALLERY));
        setImage(image);

//        if (cameraOrGallery.equals(OverviewActivity.GALLERY_IMAGE_REQUEST)) {
////            Bitmap bitmap = (Bitmap) intent.getStringExtra(OverviewActivity.BITMAP_IMG);
//        } else if (cameraOrGallery.equals(OverviewActivity.CAMERA_IMAGE_REQUEST)) {
//            findLastPicture();
//        }
    }

    private void setImage(Uri theUri) {
        final ImageView imageView = (ImageView) findViewById(R.id.imageView);
        imageView.setImageURI(theUri);
    }

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        Spinner spinner = (Spinner) findViewById(R.id.category_assigner);
        String selectedCategory = spinner.getSelectedItem().toString();
        if (position>0){
            category = selectedCategory;
        }
        Log.d("selectedCat", category);
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {

    }


//    private void findLastPicture() {
//        // Find the last picture
//        String[] projection = new String[]{
//                MediaStore.Images.ImageColumns._ID,
//                MediaStore.Images.ImageColumns.DATA,
//                MediaStore.Images.ImageColumns.BUCKET_DISPLAY_NAME,
//                MediaStore.Images.ImageColumns.DATE_TAKEN,
//                MediaStore.Images.ImageColumns.MIME_TYPE
//        };
//        final Cursor cursor = getApplicationContext().getContentResolver()
//                .query(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, projection, null,
//                        null, MediaStore.Images.ImageColumns.DATE_TAKEN + " DESC");
//
//// Put it in the image view
//        if (cursor.moveToFirst()) {
//            final ImageView imageView = (ImageView) findViewById(R.id.imageView);
//            String imageLocation = cursor.getString(1);
//            File imageFile = new File(imageLocation);
//            if (imageFile.exists()) {   //
//                Bitmap bm = BitmapFactory.decodeFile(imageLocation);
//                imageView.setImageBitmap(bm);
//            }
//        }
//    }
}
