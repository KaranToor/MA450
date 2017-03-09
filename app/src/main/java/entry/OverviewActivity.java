package entry;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.StrictMode;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.GridLayout;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TableRow;
import android.widget.TextView;

import android.Manifest;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v7.app.AlertDialog;
import android.widget.Toast;

import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.vision.v1.Vision;
import com.google.api.services.vision.v1.VisionRequest;
import com.google.api.services.vision.v1.VisionRequestInitializer;
import com.google.api.services.vision.v1.model.AnnotateImageRequest;
import com.google.api.services.vision.v1.model.BatchAnnotateImagesRequest;
import com.google.api.services.vision.v1.model.BatchAnnotateImagesResponse;
import com.google.api.services.vision.v1.model.EntityAnnotation;
import com.google.api.services.vision.v1.model.Feature;
import com.google.api.services.vision.v1.model.Image;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import picture.PhotoDB;
import picture.PictureObject;
import tcss450.uw.edu.gvtest.MainActivity;
import tcss450.uw.edu.gvtest.R;
import utils.PackageManagerUtils;
import utils.PermissionUtils;


/**
 * This Activity allows the user to view their receipts and add a new receipt via the camera button.
 *
 * @author Google Cloud Vision sample modified by MA450 Team 11
 * @version Winter 2017
 */
public class OverviewActivity extends AppCompatActivity implements View.OnLongClickListener, AdapterView.OnItemSelectedListener {

    /**
     * Used in the NewEntryActivity to get the price via getIntent.
     */
    public static final String TOTAL_AMOUNT = "picture-total-text";

    /**
     * Used to get the location via getIntent.
     */
    public static final String LOCATION = "location-from-pic";

    /**
     * Used to get the payment type via getIntent.
     */
    public static final String PAYMENT_TYPE = "payment-from-pic";

    /**
     * Used to get the date via getIntent.
     */
    public static final String DATE = "date-from-pic";

    /**
     * Used to get the image file path via getIntent.
     */
    public static final String GET_FILE_NAME = "file-name";

    /**
     * Used to set the category position.
     */
    public static final String POSITION = "null";

    /**
     * The int representing gallery permissions request.
     */
    public static final int GALLERY_PERMISSIONS_REQUEST = 0;

    /**
     * The int representing gallery image request.
     */
    public static final int GALLERY_IMAGE_REQUEST = 1;

    /**
     * The int representing camera permissions request.
     */
    public static final int CAMERA_PERMISSIONS_REQUEST = 2;

    /**
     * The int representing camera image request.
     */
    public static final int CAMERA_IMAGE_REQUEST = 3;

    /**
     * Karan's Google Cloud Vision API key.
     */
    private static final String CLOUD_VISION_API_KEY = "AIzaSyAEmx8tOtRIn3KTxAgPcdqtcGD9CLcXGQQ";

    /**
     * A certificate header used to allow the use of Google Cloud Vision.
     */
    private static final String ANDROID_CERT_HEADER = "X-Android-Cert";

    /**
     * A header used with a package signature sent to Google Cloud Vision.
     */
    private static final String ANDROID_PACKAGE_HEADER = "X-Android-Package";

    /**
     * Used for debugging.
     */
    private static final String TAG = MainActivity.class.getSimpleName();

    /**
     * The photo path of the current photo.
     */
    public static String mCurrentPhotoPath;

    /**
     * The file path of this photo.
     */
    private static File mPhoto;

    /**
     * The photo database.
     */
    private PhotoDB mPhotoDB;

    /**
     * True if the picture is being retaken.
     */
    private Boolean mRetakePicture;

    /**
     * Called at the creation of the Activity.
     *
     * @param theSavedInstanceState The instance state.
     */
    @Override
    protected void onCreate(Bundle theSavedInstanceState) {
        super.onCreate(theSavedInstanceState);

        Intent intent = getIntent();
        mRetakePicture = intent.getBooleanExtra("Retake Picture", false);
        if (mRetakePicture) {
            mCurrentPhotoPath = intent.getStringExtra("OldPath");
            Log.d("ifRetakePic", "onCreate: oldPath =" + mCurrentPhotoPath);
            cameraButtonClicked();
        }

        setContentView(R.layout.activity_overview);
        setProgressLabel();

        Spinner spinner = (Spinner) findViewById(R.id.category_selector);
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
                R.array.categories, android.R.layout.simple_spinner_item);

        // Specify the layout to use when the list of choices appears
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        // Apply the adapter to the spinner
        spinner.setAdapter(adapter);
        spinner.setOnItemSelectedListener(this);
    }

    /**
     * The defined behavior for when the activity is resumed.
     */
    @Override
    public void onResume() {
        super.onResume();
        mPhotoDB = new PhotoDB(this);
    }

    /**
     * Updates the overview page with picture information.
     *
     * @param allPhotos the photos for this user as PictureObjects.
     */
    public void updateTable(final List<PictureObject> allPhotos) {
        GridLayout gridLayout = (GridLayout) findViewById(R.id.entries);
        gridLayout.removeViewsInLayout(0, gridLayout.getChildCount());
        if (allPhotos != null) {
            gridLayout.setRowCount(allPhotos.size() + 1);

            for (int i = 0; i < allPhotos.size(); i++) {
                TableRow t = new TableRow(this);
                String myCategory = allPhotos.get(i).getmCategory();
                if (myCategory.equals(getString(R.string.nullStr))) {
                    myCategory = "none";
                }
                final int finalI = i;
                View.OnClickListener onClickListener = new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        viewEntry(v, allPhotos.get(finalI));
                    }
                };

                TextView date = new TextView(this);
                date.setText(allPhotos.get(i).getmDate());
                date.setClickable(true);
                date.setOnClickListener(onClickListener);

                TextView location = new TextView(this);
                location.setText(allPhotos.get(i).getmLocation());
                location.setClickable(true);
                location.setOnClickListener(onClickListener);

                TextView price = new TextView(this);
                price.setText("$" + allPhotos.get(i).getmPrice());
                price.setClickable(true);
                price.setOnClickListener(onClickListener);

                TextView category = new TextView(this);
                category.setText(myCategory);
                category.setClickable(true);
                category.setOnClickListener(onClickListener);


                gridLayout.addView(date, i);
                GridLayout.LayoutParams dateparam = new GridLayout.LayoutParams(GridLayout.spec(GridLayout.UNDEFINED, .3f), GridLayout.spec(GridLayout.UNDEFINED, .3f));
                dateparam.height = GridLayout.LayoutParams.WRAP_CONTENT;
                dateparam.width = GridLayout.LayoutParams.WRAP_CONTENT;
                dateparam.rightMargin = 5;
                dateparam.topMargin = 5;
                dateparam.setGravity(Gravity.CENTER);
                dateparam.columnSpec = GridLayout.spec(0, 0.3f);
                dateparam.rowSpec = GridLayout.spec(i);
                date.setLayoutParams(dateparam);
                gridLayout.addView(location, i);

                GridLayout.LayoutParams locationParam = new GridLayout.LayoutParams();
                locationParam.height = GridLayout.LayoutParams.WRAP_CONTENT;
                locationParam.width = GridLayout.LayoutParams.WRAP_CONTENT;
                locationParam.leftMargin = 0;
                locationParam.rightMargin = 0;
                locationParam.topMargin = 5;
                locationParam.setGravity(Gravity.START);
                locationParam.columnSpec = GridLayout.spec(1, .3f);
                locationParam.rowSpec = GridLayout.spec(i);
                location.setLayoutParams(locationParam);

                GridLayout.LayoutParams priceParam = new GridLayout.LayoutParams();
                priceParam.height = GridLayout.LayoutParams.WRAP_CONTENT;
                priceParam.width = GridLayout.LayoutParams.WRAP_CONTENT;
                priceParam.rightMargin = 0;
                priceParam.topMargin = 5;
                priceParam.setGravity(Gravity.RIGHT);
                priceParam.columnSpec = GridLayout.spec(2, .3f);
                priceParam.rowSpec = GridLayout.spec(i);
                price.setLayoutParams(priceParam);
                price.setTextAlignment(View.TEXT_ALIGNMENT_VIEW_END);
                gridLayout.addView(price);

                GridLayout.LayoutParams categoryParam = new GridLayout.LayoutParams();
                categoryParam.height = GridLayout.LayoutParams.WRAP_CONTENT;
                categoryParam.width = GridLayout.LayoutParams.WRAP_CONTENT;
                categoryParam.leftMargin = 0;
                categoryParam.topMargin = 5;
                categoryParam.setGravity(Gravity.CENTER);
                categoryParam.columnSpec = GridLayout.spec(3, 0.5f);
                categoryParam.rowSpec = GridLayout.spec(i);
                category.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
                category.setLayoutParams(categoryParam);
                gridLayout.addView(category);
            }
        } else {
            System.out.println("NO PHOTOS FOUND");
            TextView date = new TextView(this);
            date.setText("NO ENTRIES");
            gridLayout.addView(date, 0);

        }
    }

    /**
     * Starts the camera when the camera button is clicked.
     */
    public void cameraButtonClicked() {
        AlertDialog.Builder builder = new AlertDialog.Builder(OverviewActivity.this);
        try {
            startCamera();
        } catch (IOException e) {
            e.printStackTrace();
        }
        builder.create().show();
    }

    /**
     * Launches the dialog for the user to choose a picture from their gallery.
     *
     * @param theView The view used for event handling.
     */
    public void cameraButtonClicked(View theView) {
        cameraButtonClicked();
    }

    /**
     * Starts the gallery image chooser Activity.
     */
    public void startGalleryChooser() {
        if (PermissionUtils.requestPermission(this, GALLERY_PERMISSIONS_REQUEST,
                Manifest.permission.READ_EXTERNAL_STORAGE)) {
            Intent intent = new Intent();
            intent.setType(getString(R.string.intenttype));
            intent.setAction(Intent.ACTION_GET_CONTENT);
            startActivityForResult(Intent.createChooser(intent, getString(R.string.choosePhotoPrompt)),
                    GALLERY_IMAGE_REQUEST);
        }
    }

    /**
     * Presents the user with a confirmation prompt if the logout button is selected.
     *
     * @param theView the button that was pressed.
     */
    public void onLogoutPressed(View theView) {
        new AlertDialog.Builder(OverviewActivity.this)
                .setTitle("LOGOUT")
                .setMessage("Are you sure you want to logout?")
                .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        SharedPreferences prefs = getApplicationContext().getSharedPreferences(
                                getString(R.string.prefKey), Context.MODE_PRIVATE);

                        SharedPreferences.Editor editor = prefs.edit();
                        editor.putBoolean(getString(R.string.isloggedin), false);
                        editor.commit();
                        Intent intent = new Intent(OverviewActivity.this, MainActivity.class);
                        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                        startActivity(intent);
                        OverviewActivity.this.finish();
                    }
                })
                .setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        // do nothing
                    }
                })
                .setIcon(android.R.drawable.ic_dialog_alert)
                .show();
    }

    /**
     * Launches the camera.
     *
     * @throws IOException
     */
    public void startCamera() throws IOException {
        if (PermissionUtils.requestPermission(
                this,
                CAMERA_PERMISSIONS_REQUEST,
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.CAMERA)) {
            Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            if (mRetakePicture) {
                Log.d(TAG, "startCamera: " + mCurrentPhotoPath);

                File overridePhoto = new File(Uri.parse(mCurrentPhotoPath).getPath());
                if (overridePhoto.exists() && overridePhoto.delete()) {
                    Log.d(TAG, "startCamera: oldphoto deleted");
                    mPhoto = overridePhoto;
                    intent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(overridePhoto));

                } else {
                    mPhoto = overridePhoto;
                    intent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(overridePhoto));
                    Log.d("ERROR DELETING", "startCamera: " + "FILE LINE ~399");
                }

            } else {
                intent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(getCameraFile()));
            }


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
    }

    /**
     * Gets the file from external storage.
     *
     * @return The image file.
     */
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
        File image = new File(
                storageDir,
                imageFileName +  /* prefix */
                        ".jpg"        /* suffix */
                      /* directory */
        );

        // Save a file: path for use with ACTION_VIEW intents
        mCurrentPhotoPath = image.getAbsolutePath();

        Log.d(TAG, "New image File absolute path: " + mCurrentPhotoPath);
        mPhoto = image;
        return image;
    }

    /**
     * Checks if the permissions were authorized.
     *
     * @param theRequestCode The camera or gallery permission code.
     * @param theResultCode  The result of the attempt.
     * @param theData        which item was selected.
     */
    @Override
    protected void onActivityResult(int theRequestCode, int theResultCode, Intent theData) {
        super.onActivityResult(theRequestCode, theResultCode, theData);

        if (theRequestCode == GALLERY_IMAGE_REQUEST && theResultCode == RESULT_OK && theData != null) {
            mCurrentPhotoPath = theData.getData().toString();

            Log.d("ONACTICITYRESULT", "onActivityResult: " + mCurrentPhotoPath);
            uploadImage(Uri.parse(mCurrentPhotoPath));    //theData.getData());

        } else if (theRequestCode == CAMERA_IMAGE_REQUEST && theResultCode == RESULT_OK) {
            galleryAddPic();
            if (mRetakePicture) {
                Log.d(TAG, "onActivityResult OldImage: " + Uri.fromFile(mPhoto));
                uploadImage(Uri.fromFile(mPhoto));
            } else {
                Log.d(TAG, "onActivityResult newImage: " + Uri.fromFile(mPhoto));
                uploadImage(Uri.fromFile(mPhoto));

            }
        }
    }

    private void galleryAddPic() {
        Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
        File f = new File(mCurrentPhotoPath);
        Uri contentUri = Uri.fromFile(f);
        mediaScanIntent.setData(contentUri);
        this.sendBroadcast(mediaScanIntent);
    }

    /**
     * Uploads the bitmap version of the image to Google Cloud Vision API.
     *
     * @param theUri the image.
     */
    public void uploadImage(Uri theUri) {
        if (theUri != null) {
            try {
                // scale the image to save on bandwidth
//                myImageUri = theUri;

                // Needed for some reason even though it is not used.
                Bitmap galleryBitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), theUri);

                Bitmap bitmap =
                        scaleBitmapDown(
                                MediaStore.Images.Media.getBitmap(getContentResolver(), theUri),
                                1200);

                callCloudVision(bitmap);
//                mMainImage.setImageBitmap(bitmap);

            } catch (IOException e) {
                Log.d(TAG, getString(R.string.imagePickFail) + e.getMessage());
                Toast.makeText(this, R.string.image_picker_error, Toast.LENGTH_LONG).show();
            }
        } else {
            Log.d(TAG, getString(R.string.nullImage));
            Toast.makeText(this, R.string.image_picker_error, Toast.LENGTH_LONG).show();
        }
    }

    /**
     * Makes the call to Google Cloud Vision and gets the result.
     *
     * @param theBitmap The image to send to Google Cloud Vision.
     * @throws IOException Throw exception when file is invalid.
     */
    private void callCloudVision(final Bitmap theBitmap) throws IOException {
        // Do the real work in an async task, because we need to use the network anyway
        new AsyncTask<Object, Void, String>() {
            ProgressDialog progressDialog;

            /**
             * Makes temporary changes to the UI to prevent user-created error.
             */
            @Override
            protected void onPreExecute() {
                progressDialog = new ProgressDialog(OverviewActivity.this);
                progressDialog.setIndeterminate(true);
                progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
                progressDialog.setMessage("Processing image...");
                progressDialog.setCancelable(false);
                progressDialog.setCanceledOnTouchOutside(false);
                progressDialog.show();
            }

            /**
             * Sends the image to be processed by Google Vision.
             * @param theParams is required to correctly override doInBackground but is not used.
             * @return
             */
            @Override
            protected String doInBackground(Object... theParams) {
                try {
                    HttpTransport httpTransport = AndroidHttp.newCompatibleTransport();
                    JsonFactory jsonFactory = GsonFactory.getDefaultInstance();

                    VisionRequestInitializer requestInitializer =
                            new VisionRequestInitializer(CLOUD_VISION_API_KEY) {
                                /**
                                 * We override this so we can inject important identifying fields into the HTTP
                                 * headers. This enables use of a restricted cloud platform API key.
                                 */
                                @Override
                                protected void initializeVisionRequest(VisionRequest<?> visionRequest)
                                        throws IOException {
                                    super.initializeVisionRequest(visionRequest);

                                    String packageName = getPackageName();
                                    visionRequest.getRequestHeaders().set(ANDROID_PACKAGE_HEADER,
                                            packageName);

                                    String sig = PackageManagerUtils.getSignature(getPackageManager(),
                                            packageName);

                                    visionRequest.getRequestHeaders().set(ANDROID_CERT_HEADER, sig);
                                }
                            };

                    Vision.Builder builder = new Vision.Builder(httpTransport, jsonFactory, null);
                    builder.setVisionRequestInitializer(requestInitializer);

                    Vision vision = builder.build();

                    BatchAnnotateImagesRequest batchAnnotateImagesRequest =
                            new BatchAnnotateImagesRequest();
                    batchAnnotateImagesRequest.setRequests(new ArrayList<AnnotateImageRequest>() {{
                        AnnotateImageRequest annotateImageRequest = new AnnotateImageRequest();

                        // Add the image
                        Image base64EncodedImage = new Image();

                        // Convert the bitmap to a JPEG
                        // Just in case it's a format that Android understands but Cloud Vision
                        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
                        theBitmap.compress(Bitmap.CompressFormat.JPEG, 90, byteArrayOutputStream);
                        byte[] imageBytes = byteArrayOutputStream.toByteArray();

                        // Base64 encode the JPEG
                        base64EncodedImage.encodeContent(imageBytes);
                        annotateImageRequest.setImage(base64EncodedImage);

                        // add the features we want
                        annotateImageRequest.setFeatures(new ArrayList<Feature>() {{
                            Feature textDetection = new Feature();
                            // We're interested in text detection so we use this type flag. @toork
                            textDetection.setType(getString(R.string.detectionType));
                            textDetection.setMaxResults(10);
                            add(textDetection);
                        }});

                        // Add the list of one thing to the request
                        add(annotateImageRequest);
                    }});

                    Vision.Images.Annotate annotateRequest =
                            vision.images().annotate(batchAnnotateImagesRequest);
                    // Due to a bug: requests to Vision API containing large images fail when GZipped.
                    annotateRequest.setDisableGZipContent(true);
                    Log.d(TAG, getString(R.string.sendingRequest));

                    BatchAnnotateImagesResponse response = annotateRequest.execute();
                    return convertResponseToString(response);

                } catch (GoogleJsonResponseException e) {
                    Log.d(TAG, getString(R.string.JSONRespExceptionFail) + e.getContent());
                } catch (IOException e) {
                    Log.d(TAG, getString(R.string.IOERequestFail) +
                            e.getMessage());
                }
                return getString(R.string.visionRequestFailed);
            }

            /**
             * After retrieving the data, post it to the specified TextView.
             * @param theResult the String result of text found in the image.
             */
            protected void onPostExecute(String theResult) {
                System.out.println("*****The Result " + theResult);
                startNewEntry(theResult);

                progressDialog.dismiss();
            }
        }.execute();
    }

    /**
     * Calls the methods which parse the REGEX response and sends their returns to a new NewEntryActivity.
     *
     * @param theMessage the message to be parsed
     */
    public void startNewEntry(String theMessage) {
        Pattern pattern = Pattern.compile("(([1-9][0-9]{0,2}(,[0-9]{3})*)|[0-9]+)+\\.[0-9]{1,2}");
        Matcher matcher = pattern.matcher(theMessage);
        StringBuilder sb = new StringBuilder();
        while (matcher.find()) {
            sb.append(" " + matcher.group() + "\n");
        }

        Intent intent = new Intent(this, NewEntryActivity.class);
        System.out.println("*****Printing result" + sb.toString());
        intent.putExtra(TOTAL_AMOUNT, parseREGEX(sb.toString()));
        intent.putExtra(LOCATION, parseLocation(theMessage));
        intent.putExtra(PAYMENT_TYPE, parsePaymentType(theMessage));
        intent.putExtra(DATE, parseDate(theMessage));
        Log.d("OverView", "startNewEntry: filepath = " + mPhoto.getAbsolutePath());
        intent.putExtra(GET_FILE_NAME, mPhoto.getAbsolutePath()); //myImageUri.toString());
        if (mRetakePicture) {
            intent.putExtra("fromTable", true);
            intent.putExtra(getString(R.string.category), "null");
            mRetakePicture = false;
        }
        intent.putExtra(POSITION, parseCategory(theMessage));
        startActivity(intent);
    }

    /**
     * Parses the response from Google Vision which has been passed through REGEX for a payment type.
     *
     * @param theInput the response from Google Vision which has been passed through REGEX
     * @return the discovered payment type
     */
    public String parsePaymentType(String theInput) {
        String toReturn = "Not Found";
        if ((theInput.contains("Master") && theInput.contains("Card")) ||
                theInput.contains("MASTERCARD") ||
                (theInput.contains("MASTER") && theInput.contains("CARD")) ||
                theInput.contains("Mastercard")) {
            toReturn = "Master_Card";
        } else if (theInput.contains("Visa") || theInput.contains("VISA")) {
            toReturn = "Visa";
        } else if (theInput.contains("Discover") || theInput.contains("DISCOVER")) {
            toReturn = "Discover";
        } else if (theInput.contains("Cash") || theInput.contains("cash") || theInput.contains("CASH")) {
            toReturn = "Cash";
        } else if (theInput.contains("Check") || theInput.contains("check") || theInput.contains("CHECK")) {
            toReturn = "Check";
        } else if (theInput.contains("American") && theInput.contains("Express")) {
            toReturn = "American_Express";
        }
        return toReturn;
    }

    /**
     * Parses the response from Google Vision which has been passed through REGEX for a date.
     *
     * @param theInput the response from Google Vision which has been passed through REGEX
     * @return the date that was found
     */
    public String parseDate(String theInput) {
        String toReturn = "Not Found";
        Pattern pattern = Pattern.compile("[0-9]{1,4}[\\.\\/-][0-9]{1,4}[\\.\\/-][0-9]{1,4}");
        Matcher matcher = pattern.matcher(theInput);

        StringBuilder sb = new StringBuilder();
        while (matcher.find()) {
            sb.append(" " + matcher.group() + "\n");
        }
        String temp = sb.toString();
        String[] tempArray = temp.split(" ");
        for (int i = 0; i < tempArray.length; i++) {
            if (!tempArray[i].isEmpty()) {
                toReturn = tempArray[i];
            }
        }
        return toReturn.trim();
    }

    /**
     * Parses the response from Google Vision which has been passed through REGEX for an abbreviated State location.
     *
     * @param theInput the response from Google Vision which has been passed through REGEX
     * @return the abbreviated State location
     */
    public String parseLocation(String theInput) {
        Pattern pattern = Pattern.compile("(([^A-Z])(AL|AK|AR|AZ|CA|CO|CT|DC|DE|FL|G" +
                "A|HI|IA|ID|IL|IN|KS|KY|LA|MA|MD|ME|MI|MN|MO|MS|MT|NC|ND|NE|NH|NJ|NM|NV|NY|" +
                "OH|OK|OR|PA|RI|SC|SD|TN|TX|UT|VA|VT|WA|WI|WV|WY)([^A-Z]))|(Alabama|Alaska|Arizona|" +
                "Arkansas|California|Colorado|Connecticut|Delaware|Florida|Georgia|Hawaii|Idaho|" +
                "Illinois|Indiana|Iowa|Kansas|Kentucky|Louisiana|Maine|Maryland|Massachusetts|" +
                "Michigan|Minnesota|Mississippi|Missouri|Montana|Nebraska|Nevada|New\\sHampshire|" +
                "New\\sJersey|New\\sMexico|New\\sYork|North\\sCarolina|North\\sDakota|Ohio|" +
                "Oklahoma|Oregon|Pennsylvania|Rhode\\sIsland|South\\sCarolina|South\\sDakota|" +
                "Tennessee|Texas|Utah|Vermont|Virginia|Washington|West\\sVirginia|Wisconsin|" +
                "Wyoming)");
        Matcher matcher = pattern.matcher(theInput);

        StringBuilder sb = new StringBuilder();
        String toReturn = "Not Found";
        while (matcher.find()) {
            sb.append(" " + matcher.group() + "\n");
        }
        String temp = sb.toString();
        String[] tempArray = temp.split(" ");
        for (int i = 0; i < tempArray.length; i++) {
            if (!tempArray[i].isEmpty()) {
                String state = tempArray[i];
                int count = 0;
                for (int j = 0; j < state.length(); j++) {
                    if (Character.isLetterOrDigit(state.charAt(j))) {
                        count++;
                    }
                }
                if (count == 2) {
                    toReturn = tempArray[i];
                }
            }
        }
        return toReturn.trim();
    }

    /**
     * Parses the response from Google Vision which has been passed through REGEX for a category.
     *
     * @param theInput the response from Google Vision which has been passed through REGEX
     * @return the int to set the spinner on correlated with category
     */
    public int parseCategory(String theInput) {
        int pos = 0;
        if (theInput.contains("Chipotle") || theInput.contains("Burger King")) {
            pos = 8;
        }
        return pos;
    }

    /**
     * Parses the response from Google Vision which has been run through REGEX, looking for a dollar amount.
     *
     * @param theInput the response from Google Vision which has been run through REGEX
     * @return the dollar amount
     */
    public String parseREGEX(String theInput) {
        String[] temp = theInput.split(" ");
        String toReturn = "";
        float highest = -1;
        for (int i = 0; i < temp.length; i++) {
            String toParse = temp[i].trim();
            if (!toParse.isEmpty()) {
                float tempPrice = Float.parseFloat(toParse);
                if (tempPrice > highest) {
                    highest = tempPrice;
                }
            }
        }
        toReturn += highest;

        if (highest == -1) {
            toReturn = "Not Found";
        } else {
            toReturn = formatDecimal(highest);
            toReturn = toReturn.trim();
        }
        return toReturn;
    }

    /**
     * From http://stackoverflow.com/questions/2379221/java-currency-number-format
     * Used to format to currency.
     *
     * @param number the input float.
     * @return The number as currency.
     */
    public String formatDecimal(float number) {
        float epsilon = 0.004f; // 4 tenths of a cent
        if (Math.abs(Math.round(number) - number) < epsilon) {
            return String.format("%10.0f", number); // sdb
        } else {
            return String.format("%10.2f", number); // dj_segfault
        }
    }

    /**
     * Scales the image to a specified dimension.
     *
     * @param theBitmap       The bitmap to scale.
     * @param theMaxDimension The maximum dimension of width or height.
     * @return The scaled bitmap.
     */
    public Bitmap scaleBitmapDown(Bitmap theBitmap, int theMaxDimension) {
        int originalWidth = theBitmap.getWidth();
        int originalHeight = theBitmap.getHeight();
        int resizedWidth = theMaxDimension;
        int resizedHeight = theMaxDimension;

        if (originalHeight > originalWidth) {
            resizedHeight = theMaxDimension;
            resizedWidth = (int) (resizedHeight * (float) originalWidth / (float) originalHeight);
        } else if (originalWidth > originalHeight) {
            resizedWidth = theMaxDimension;
            resizedHeight = (int) (resizedWidth * (float) originalHeight / (float) originalWidth);
        } else if (originalHeight == originalWidth) {
            resizedHeight = theMaxDimension;
            resizedWidth = theMaxDimension;
        }
        return Bitmap.createScaledBitmap(theBitmap, resizedWidth, resizedHeight, false);
    }

    /**
     * Prints out the text Google Cloud Vision has found.
     *
     * @param theResponse the response from Google Cloud Vision.
     * @return The String of text found.
     */
    private String convertResponseToString(BatchAnnotateImagesResponse theResponse) {
        String message = getString(R.string.found);

        List<EntityAnnotation> words = theResponse.getResponses().get(0).getTextAnnotations();
        if (words != null) {
            for (EntityAnnotation label : words) {
                message += String.format("%.3f: %s", label.getScore(), label.getDescription());
                message += "\n";
            }
        } else {
            message += getString(R.string.nothing);
        }

        return message;
    }

    /**
     * Sets the progress bar.
     */
    private void setProgressLabel() {
        //This *should* make the TextView over the progress bar be "current progress/maximum value"
        ProgressBar p = (ProgressBar) this.findViewById(R.id.progressBar3);
        TextView t = (TextView) this.findViewById(R.id.progressLabel);
        t.setText(p.getProgress() + "\\" + p.getMax());
    }

    /**
     * Cancel on long click.
     *
     * @param theView The view used for event handling.
     * @return The boolean representing a long click.
     */
    @Override
    public boolean onLongClick(View theView) { //This is the start of trying to make a fragment show up when an item in the overview is tapped and held
        return false;
    }

    /**
     * Log a click.
     *
     * @param theView The view used for event handling.
     */
    public void viewEntry(View theView, PictureObject photo) {
        Log.d(getString(R.string.print), getString(R.string.clicked));


        Intent intent = new Intent(this, NewEntryActivity.class);
        intent.putExtra(TOTAL_AMOUNT, photo.getmPrice().toPlainString());
        intent.putExtra(LOCATION, photo.getmLocation());
        intent.putExtra(PAYMENT_TYPE, photo.getmPaymentType());
        intent.putExtra(DATE, photo.getmDate());

        Log.d(TAG, "viewEntry: photoID " + photo.getmPhotoId());

        intent.putExtra(GET_FILE_NAME, photo.getmPhotoId());
        intent.putExtra("fromTable", true);
        intent.putExtra(getString(R.string.category), photo.getmCategory());
        startActivity(intent);
    }

    /**
     * A method to completely override the functionality of the Back button so that the user cannot return to the login/register activity without logging out.
     */
    @Override
    public void onBackPressed() {  //do not call super.onBackPressed() here
        Intent backToHome = new Intent(Intent.ACTION_MAIN);
        backToHome.addCategory(Intent.CATEGORY_HOME);
        backToHome.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(backToHome);
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
        Spinner spinner = (Spinner) findViewById(R.id.category_selector);
        String selectedCategory = spinner.getSelectedItem().toString();
        Log.d("selectedCat", selectedCategory);

        if (selectedCategory.equals("Show All")) {
            mPhotoDB.getAllPhotos();
        } else {
            //PhotoDB mPhotoDB = new PhotoDB(this);
            mPhotoDB.getCategoryAll(selectedCategory);
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
