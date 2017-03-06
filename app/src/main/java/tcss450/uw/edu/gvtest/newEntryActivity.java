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
import android.widget.EditText;
import android.widget.ImageView;

import java.io.File;
import java.math.BigDecimal;


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
        String category = intent.getStringExtra(OverviewActivity.CATEGORY);

        EditText locationEditText = (EditText) findViewById(R.id.locationId);
        locationEditText.setText(loc);
        EditText dateEdit = (EditText) findViewById(R.id.dateId);
        dateEdit.setText(date);
        EditText editText = (EditText) findViewById(R.id.amountId);
        editText.setText(message);
        EditText paymentEdit = (EditText) findViewById(R.id.paymentId);
        paymentEdit.setText(payment);


        Uri image = Uri.parse(intent.getStringExtra(OverviewActivity.CAMERA_OR_GALLERY));
        setImage(image);

//        if (cameraOrGallery.equals(OverviewActivity.GALLERY_IMAGE_REQUEST)) {
////            Bitmap bitmap = (Bitmap) intent.getStringExtra(OverviewActivity.BITMAP_IMG);
//        } else if (cameraOrGallery.equals(OverviewActivity.CAMERA_IMAGE_REQUEST)) {
//            findLastPicture();
//        }
    }

    private void sendToDatabase(Uri thePhotoId, String theLocation, String thePrice, String thePaymentType,
                                String theDate, String theCategory) {
        SharedPreferences prefs = getApplicationContext().getSharedPreferences(
                getString(R.string.prefKey), Context.MODE_PRIVATE);
        int userId = prefs.getInt(getString(R.string.UID), -1);
        if (userId == -1) {
            throw new IllegalArgumentException("UserId was not set in SharedPreferences");
        }
        BigDecimal price = new BigDecimal(thePrice);

        PictureObject pictureObject = new PictureObject(userId, thePhotoId.toString(), theLocation, price,
                thePaymentType, theDate, theCategory);
        PhotoDB photoDB = new PhotoDB(getApplicationContext());
        photoDB.addPhoto(pictureObject);

    }

    private void setImage(Uri theUri) {
        final ImageView imageView = (ImageView) findViewById(R.id.imageView);
        imageView.setImageURI(theUri);
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
//            if (imageFile.exists()) {   // TODO: is there a better way to do this?
//                Bitmap bm = BitmapFactory.decodeFile(imageLocation);
//                imageView.setImageBitmap(bm);
//            }
//        }
//    }
}
