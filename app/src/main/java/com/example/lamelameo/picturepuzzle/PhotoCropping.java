package com.example.lamelameo.picturepuzzle;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.media.Image;
import android.net.Uri;
import android.os.Bundle;
import android.app.Activity;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.annotation.Nullable;
import android.support.v4.content.FileProvider;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import java.io.EOFException;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class PhotoCropping extends Activity {

    private static final String TAG = "PhotoCropping";
    private ImageView mCameraView, cropView;
    private String mCurrentPhotoPath;
    private static final int REQUEST_IMAGE_CAPTURE = 1;
    private static final int REQUEST_GALLERY_SELECT = 0;
    private static final int CAMERA_CROP_RESULT = 2;
    private float cropYBounds, cropXBounds, photoYBounds, photoXBounds;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_photo_cropping);
        Log.i(TAG, "onCreate: ");

        // Buttons
        ImageView photoCropView = findViewById(R.id.cropView);
        ImageView photoView = findViewById(R.id.photoView);
        mCameraView = photoView;
        ImageView zoomIn = findViewById(R.id.zoomIn);
        ImageView zoomOut = findViewById(R.id.zoomOut);
        ImageView cameraButton = findViewById(R.id.takePhoto);
        Button galleryButton = findViewById(R.id.galleryButton);
        ImageView[] moveArrows = {findViewById(R.id.upArrow), findViewById(R.id.downArrow),
                                  findViewById(R.id.leftArrow), findViewById(R.id.rightArrow)};
        cropView = photoCropView;
        Log.i(TAG, "onCreate photoY: "+photoView.getY());
        Log.i(TAG, "onCreate photoH: "+photoView.getHeight());
        photoXBounds = photoView.getX() + photoView.getWidth();
        photoYBounds = photoView.getY() + photoView.getHeight();

        // send intent to take photo using camera on button click
        cameraButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dispatchTakePictureIntent();
            }
        });

        // open gallery picker on gallery button click
        galleryButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent galleryIntent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                startActivityForResult(galleryIntent, REQUEST_GALLERY_SELECT);
            }
        });

        moveArrows[1].setOnClickListener(arrowClickListener);

    }

    private void setArrowListener(final int direction, ImageView arrowView) {
        // up = 0, down = 1, left = 2, right = 3
        View.OnClickListener onClickListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                switch (direction) {
                    case 0:  // up

                    case 1:  // down

                    case 2:  // left

                    case 3:  // right

                }
            }
        };
        arrowView.setOnClickListener(onClickListener);
    }

    private View.OnClickListener arrowClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
//            float xCoord = cropView.getX();
//            cropView.setX(xCoord + 100);
            // have to determine bounds or crop view can move outside of its constraints
            float yCoord = cropView.getY();
            Log.i(TAG, "onClick cropY: "+yCoord);
            Log.i(TAG, "onClick photoY: "+mCameraView.getY());
            float height = cropView.getHeight();
            cropYBounds = yCoord + height;
            //TODO: why is photo view y values giving 0
            if ((cropYBounds + 10) < photoYBounds) {  // only move crop outline if less than bounds of photo view
                cropView.setY(yCoord + 10);
            } else {
                Toast toast = Toast.makeText(getApplicationContext(), "Reached Bounds.", Toast.LENGTH_SHORT);
                toast.show();
            }
        }
    };

    private void cropImage() {
        //TODO: get the xy coords of crop and photo, determine difference, scale/crop photo and save cropped photo
        //  overwriting the prev file, must also reload recycler view upon moving back to main activity, and allow
        //  user to go into game with set photo
        Intent intent = new Intent();
    }

    /**
     * Creates and invokes an Intent to take a photo using the camera
     */
    private void dispatchTakePictureIntent() {
        Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        // Check there is a camera activity
        if (cameraIntent.resolveActivity(getPackageManager()) != null) {
            // Create a File to save the photo into
            File photoFile = null;
            try {
                photoFile = createImageFile();
            } catch (IOException ex) {  // error when making file
                ex.printStackTrace();
            }
            // if successful in creating File, save the photo into it
            if (photoFile != null) {
                Uri photoURI = FileProvider.getUriForFile(this, "com.example.android.fileprovider", photoFile);
                cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
                startActivityForResult(cameraIntent, REQUEST_IMAGE_CAPTURE);
            }
        }
    }

    /**
     * Save the full sized image taken from camera to an app private directory that is deleted if app is removed
     * @return a File to store a photo taken with the camera intent
     * @throws IOException error when making File
     */
    private File createImageFile() throws IOException {
//        Locale locale = ConfigurationCompat.getLocales(Resources.getSystem().getConfiguration());
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File image = File.createTempFile(imageFileName, ".jpg", storageDir);
        mCurrentPhotoPath = image.getAbsolutePath();
        return image;
    }

    /**
     * Gets the image taken with the camera intent as a Bitmap to display in an ImageView {@link #mCameraView} as a preview
     * @param requestCode request code is 1 for our camera intent
     * @param resultCode RESULT_OK means we got a photo, RESULT_CANCELLED means no photo
     * @param data returns result data from the camera Intent we used, can use getExtras to obtain this
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_IMAGE_CAPTURE) {  // result from camera intent
            if (resultCode == RESULT_OK) {  // got a photo back from camera
                // we already have the photo file path, which we will use to crop the photo and save in same file path
                File croppedPhotoFile = new File(mCurrentPhotoPath);
                // TODO: take camera file, save to gallery, get URI, send to cropper, save into new file in app?
                // this is the URI to save the photo into after cropping - for camera photos it will overwrite
//        Uri croppedPhotoURI = Uri.fromFile(croppedPhotoFile);
                //TODO: cant send photo, which is saved in app, to gallery cropper as get permission problem even with provider
                Uri croppedPhotoURI = FileProvider.getUriForFile(this, "com.example.android.fileprovider",
                        croppedPhotoFile);
                Log.i(TAG, "onActivityResult: photoURI "+croppedPhotoURI);
                Intent editPhotoIntent = new Intent("com.android.camera.action.CROP");
                editPhotoIntent.setDataAndType(croppedPhotoURI, "image/*");
                editPhotoIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                editPhotoIntent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
                dispatchCropIntent(editPhotoIntent, croppedPhotoURI);
            } else {  // didnt get a return value (no photo)
                // TODO: this is not needed anymore after changes?
                // clear file at photopath or will get errors reopening app as there are empty files with no pic stored
                try {
                    boolean deleteTempFile = new File(mCurrentPhotoPath).delete();
                    Log.i(TAG, "deleted temp photo file: "+deleteTempFile);
                } catch (Exception e) {
                    e.printStackTrace();
                    Log.i(TAG, "tried to delete temp file on no result camera exit, but got error");
                }
                // clear photopath or will get errors trying to access null image in game activity
                mCurrentPhotoPath = null;
            }
        } else {
            Log.i(TAG, "onActivityResult requestCode: " + requestCode);
            // process gallery image selection
            //TODO: using image URI from gallery sending to gallery cropper, then saving to app, no permission problems
            if (requestCode == REQUEST_GALLERY_SELECT && resultCode == RESULT_OK && data != null) {
                // get the URI for the photo selected from gallery and send it to the android photo editor to crop
                Uri selectedPhotoURI = data.getData();
                // Create a File in the app pictures directory to save the edited photo into
                File croppedPhotoFile = null;
                try {
                    croppedPhotoFile = createImageFile();
                } catch (IOException ex) {  // error when making file
                    ex.printStackTrace();
                }
                // if successful in creating File, save a copy of the photo into it to then be edited
                if (croppedPhotoFile != null) {
                    Uri croppedPhotoURI = Uri.fromFile(croppedPhotoFile);
                    Intent editPhotoIntent = new Intent("com.android.camera.action.CROP");
                    editPhotoIntent.setDataAndType(selectedPhotoURI, "image/*");
                    dispatchCropIntent(editPhotoIntent, croppedPhotoURI);
                }
            }
            // get the cropped photo and send ImageView in app for a preview
            if (requestCode == CAMERA_CROP_RESULT && resultCode == RESULT_OK && data != null) {
                Uri croppedPhotoUri = data.getData();
                try {
                    Bitmap bitmap = BitmapFactory.decodeStream(getContentResolver().openInputStream(croppedPhotoUri));
                    mCameraView.setImageBitmap(bitmap);
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                }
            }
        }

    }

    private void dispatchCropIntent(Intent intent, Uri saveUri) {
        intent.putExtra(MediaStore.EXTRA_OUTPUT, saveUri);  // output file uri
        // output smaller photo
        intent.putExtra("outputX", 300);
        intent.putExtra("outputY", 300);
        // set aspect ratio to 1:1 for a square image
        intent.putExtra("aspectX", 1);
        intent.putExtra("aspectY", 1);
        intent.putExtra("scale", true);
        intent.putExtra("output", saveUri);
        intent.putExtra("outputFormat", Bitmap.CompressFormat.JPEG.toString());
        // TODO: HOW TO ROTATE?
        intent.putExtra("noFaceDetection", true);
        startActivityForResult(intent, CAMERA_CROP_RESULT);
    }

    /**
     * Method to save a picture taken from this app into the device's gallery
     */
    private void addPicGallery() {
        Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
        File f = new File(mCurrentPhotoPath);
        Uri contentUri = Uri.fromFile(f);
        mediaScanIntent.setData(contentUri);
        this.sendBroadcast(mediaScanIntent);
        // directory for gallery images
//        Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
        //TODO: this method doesnt work??
    }

    /**
     * Scale an image to the size of a view, and rotate 90 degrees to obtain the image in portrait orientation
     * @param viewSize the size of the view for the image to be scaled to
     * @param photopath the file path of the image to be scaled
     * @return a Drawable of the given image scaled to a size suitable to fit into the target view
     */
    private Drawable scalePhoto(int viewSize, String photopath) {
        // scale image previews to fit the allocated View to save app memory
        BitmapFactory.Options bmOptions = new BitmapFactory.Options();
        bmOptions.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(photopath, bmOptions);
        int photoW = bmOptions.outWidth;
        int photoH = bmOptions.outHeight;
        int scaleFactor = Math.min(photoW/viewSize, photoH/viewSize);
        bmOptions.inJustDecodeBounds = false;
        bmOptions.inSampleSize = scaleFactor;

        // rotate image to correct orientation - default is landscape
        Matrix matrix = new Matrix();
        matrix.postRotate(90);
        Bitmap bitmap = BitmapFactory.decodeFile(photopath, bmOptions);
        Bitmap scaledBmp = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
        return new BitmapDrawable(getResources(), scaledBmp);
    }

}
