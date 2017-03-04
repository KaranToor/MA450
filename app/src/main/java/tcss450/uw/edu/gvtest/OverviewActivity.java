package tcss450.uw.edu.gvtest;

import android.app.ProgressDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TableLayout;
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
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * This Activity allows the user to view their receipts and add a new receipt via the camera button.
 *
 * @author Google Cloud Vision sample modified by MA450 Team 11
 * @version Winter 2017
 */
public class OverviewActivity extends AppCompatActivity implements View.OnLongClickListener {

    private static final String CLOUD_VISION_API_KEY = "AIzaSyAEmx8tOtRIn3KTxAgPcdqtcGD9CLcXGQQ";
    public static final String FILE_NAME = "temp.jpg";
    private static final String ANDROID_CERT_HEADER = "X-Android-Cert";
    private static final String ANDROID_PACKAGE_HEADER = "X-Android-Package";

    private static final String TAG = MainActivity.class.getSimpleName();
    private static final int GALLERY_PERMISSIONS_REQUEST = 0;
    private static final int GALLERY_IMAGE_REQUEST = 1;
    public static final int CAMERA_IMAGE_REQUEST = 3;

    private TextView myImageDetails;

    /**
     * Called at the creation of the Activity.
     *
     * @param theSavedInstanceState The instance state.
     */
    @Override
    protected void onCreate(Bundle theSavedInstanceState) {
        super.onCreate(theSavedInstanceState);
        setContentView(R.layout.activity_overview);
        setProgressLabel();
        init();

        TableLayout table = (TableLayout) findViewById(R.id.table);
        TableRow t = new TableRow(this);
        myImageDetails = new TextView(this);
        t.addView(myImageDetails);
        table.addView(t);
    }

    /**
     * Launches the dialog for the user to choose a picture from their gallery.
     *
     * @param theView The view used for event handling.
     */
    public void cameraButtonClicked(View theView) {
        AlertDialog.Builder builder = new AlertDialog.Builder(OverviewActivity.this);
        builder
                .setMessage(R.string.dialog_select_prompt)
                .setPositiveButton(R.string.dialog_select_gallery, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        startGalleryChooser();
                    }
                });
        builder.create().show();
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
     * Gets the file from external storage.
     *
     * @return The image file.
     */
    public File getCameraFile() {
        File dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
        return new File(dir, FILE_NAME);
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
            uploadImage(theData.getData());
        } else if (theRequestCode == CAMERA_IMAGE_REQUEST && theResultCode == RESULT_OK) {
            uploadImage(Uri.fromFile(getCameraFile()));
        }
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
            @Override
            protected void onPreExecute(){
                progressDialog = new ProgressDialog(OverviewActivity.this);
                progressDialog.setIndeterminate(true);
                progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
                progressDialog.setMessage("Processing image...");
                progressDialog.show();
            }
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
                progressDialog.dismiss();
                Pattern pattern = Pattern.compile("(\\d{1,10}\\.\\d{2})");
                Matcher matcher = pattern.matcher(theResult);
                myImageDetails.setText(theResult);
                Log.d(getString(R.string.testCaps), theResult);
                myImageDetails.append(getString(R.string.startParse));
                while (matcher.find()) {
                    myImageDetails.append(" " + matcher.group() + "\n");
                }
            }
        }.execute();
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
     * Initializes the rows of temporary receipts in the overview screen.
     */
    private void init() {
        TableLayout table = (TableLayout) findViewById(R.id.table);
        for (int i = 1; i < 5; i++) {
            TableRow t = new TableRow(this);
            TextView text = new TextView(this);
            text.setText(R.string.testString);
            t.addView(text);
            t.setClickable(true);
            t.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    viewEntry(v);
                }
            });
            table.addView(t);
        }
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
    public void viewEntry(View theView) {
        Log.d(getString(R.string.print), getString(R.string.clicked));
    }


}
