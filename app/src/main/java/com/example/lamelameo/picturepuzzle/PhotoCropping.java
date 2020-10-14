package com.example.lamelameo.picturepuzzle;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.*;

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

public class PhotoCropping extends AppCompatActivity {

    private static final String TAG = "PhotoCropping";
    private ImageView cropView;
    private String mCurrentPhotoPath = null;
    private Uri mUri = null;
    private Bitmap mImage = null;
    private Boolean mSaved = false;
    private ArrayList<String> savedPhotos = new ArrayList<>();
    private int mGridRows = 4;
    private static final int REQUEST_IMAGE_CAPTURE = 1;
    private static final int REQUEST_GALLERY_SELECT = 0;
    private static final int REQUEST_PHOTO_CROP = 2;
    private float cropYBounds, cropXBounds, photoYBounds, photoXBounds;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_photo_cropping);

        if (savedInstanceState != null) {
            mCurrentPhotoPath = (String)savedInstanceState.getCharSequence("photoPath");
            savedPhotos = savedInstanceState.getStringArrayList("savedPhotos");
        }

        // Buttons
        cropView = findViewById(R.id.cropView);
        mImage = BitmapFactory.decodeFile(mCurrentPhotoPath);
        cropView.setImageBitmap(mImage);
        ImageView rotateRight = findViewById(R.id.rotateRight);
        ImageView rotateLeft = findViewById(R.id.rotateLeft);
        rotateRight.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mImage != null) {
                    mImage = rotatePhoto(90, mImage);
                    cropView.setImageBitmap(mImage);
                }
            }
        });
        rotateLeft.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mImage != null) {
                    mImage = rotatePhoto(270, mImage);
                    cropView.setImageBitmap(mImage);
                }
            }
        });

        // start game button onclicklistener
        Button startGame = findViewById(R.id.startGame);
        final Intent gameIntent = new Intent(this, PuzzleActivity.class);
        startGame.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mImage != null) {
                    //TODO: 1.save photo only on game start as this means user wants photo?
                    //  2.SEND newly saved photos to puzzle in case add function to move from Puzzle to Main directly
                    if (!mSaved) {
                        savePhoto(mImage);
                    }

                    gameIntent.putExtra("photoPath", mCurrentPhotoPath);
                    gameIntent.putExtra("puzzleNum", -1);
                    gameIntent.putExtra("numColumns", mGridRows);
                    startActivity(gameIntent);
                } else {
                    Toast photoToast = Toast.makeText(getApplicationContext(),
                            "Take a photo with Camera or choose one from Gallery.", Toast.LENGTH_LONG);
                    photoToast.show();
                }
            }
        });

        ImageView cameraButton = findViewById(R.id.takePhoto);
        Button saveButton = findViewById(R.id.saveButton);
        Button galleryButton = findViewById(R.id.galleryButton);
        saveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mImage != null) {
                    if (mSaved) {
                        Toast error = Toast.makeText(getApplicationContext(),
                                "Image Saved Already.", Toast.LENGTH_SHORT);
                        error.show();
                        return;
                    }
                    savePhoto(mImage);
                    Toast saved = Toast.makeText(getApplicationContext(), "Image Saved.", Toast.LENGTH_SHORT);
                    saved.show();
                }
            }
        });
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
                dispatchGallerySelectIntent();
            }
        });

        //TODO: use to change overlay of photo previews... setForeground requires higher min SDK
//        final int[] gridOverlays = {R.drawable.gridoverlay3, R.drawable.gridoverlay4,
//                R.drawable.gridoverlay5, R.drawable.gridoverlay6};

        // set gridsize based on the checked radio button, this value will be used as an intent extra when starting the game
        final RadioGroup setGrid = findViewById(R.id.radioGroup);
        setGrid.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                for (int x = 0; x<4; x++) {
                    RadioButton radioButton = (RadioButton)group.getChildAt(x);
                    if (radioButton.getId() == checkedId) {
                        mGridRows = x + 3;  // update grid size for use in load button listener in this context
//                        Drawable gridOverlay = getResources().getDrawable(gridOverlays[x], null);
//                        cropView.setForeground(gridOverlay);
                        break;
                    }
                }
            }
        });

    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        //TODO: save mImage
        outState.putCharSequence("photoPath", mCurrentPhotoPath);
        outState.putStringArrayList("savedPhotos", savedPhotos);
    }

    @Override
    public void onBackPressed() {
//        super.onBackPressed();
        // send list of paths of newly saved photos to main activity so the recycler can be updated.
        Intent intent = new Intent();
        intent.putStringArrayListExtra("savedPhotos", savedPhotos);
        if (savedPhotos.size() != 0) {
            setResult(RESULT_OK, intent);
        } else {
            setResult(RESULT_CANCELED);
        }
        finish();
    }

    //TODO: for use in self made cropping function
    private final View.OnClickListener arrowClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
//            float xCoord = cropView.getX();
//            cropView.setX(xCoord + 100);
            // have to determine bounds or crop view can move outside of its constraints
            float yCoord = cropView.getY();
            Log.i(TAG, "onClick cropY: "+yCoord);
            Log.i(TAG, "onClick photoY: "+ cropView.getY());
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


    /**
     * Create and show a Toast to display an exception message encountered by the app
     * @param exception the Exception the app has produced
     */
    private void createErrorToast(Exception exception) {
        // gives the exceptions name and message (if any)
        String errorMessage = exception.toString();
        String exceptionName = exception.getClass().toString();
        Toast errorToast = Toast.makeText(getApplicationContext(), errorMessage, Toast.LENGTH_SHORT);
        errorToast.show();
    }

    /**
     * Creates and invokes an Intent to take a photo using the camera
     */
    private void dispatchTakePictureIntent() {
        Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE, MediaStore.Images.Media.INTERNAL_CONTENT_URI);
        if (cameraIntent.resolveActivity(getPackageManager()) != null) {
            startActivityForResult(cameraIntent, REQUEST_IMAGE_CAPTURE);
        }
    }

    private void dispatchGallerySelectIntent() {
        Intent galleryIntent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.INTERNAL_CONTENT_URI);
        startActivityForResult(galleryIntent, REQUEST_GALLERY_SELECT);
    }

    /**
     * Save the full sized image taken from camera to an app private directory that is deleted if app is removed
     * @return a File to store a photo taken with the camera intent
     * throws IOException error when making File
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
     * Gets the image taken with the camera intent as a Bitmap to display in an ImageView {@link #cropView} as a preview
     * @param requestCode 0: Gallery select 1: Camera, 2: Crop image
     * @param resultCode RESULT_OK means we got a photo, RESULT_CANCELLED means no photo
     * @param data returns result data from the camera Intent we used, can use getExtras to obtain this
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        // TODO: toast for each time photo is saved to app??
        // for cancelled results, must remove created file if empty and clear the photo path to avoid errors
        if (resultCode == RESULT_CANCELED) {
            Log.i(TAG, "cancelled");
            // retrieve any image from camera or gallery if crop fails
            retrievePhoto();
        } else {  // process RESULT_OK for different request codes
            if (requestCode == REQUEST_IMAGE_CAPTURE && data != null) {  // got a photo from camera intent
                mUri = data.getData();
                dispatchCropIntent(mUri);
                return;
            }
            // process gallery image selection using image URI from gallery sending to gallery cropper, then saving
            // to app using data given back by intent in getData(), no permission problems
            if (requestCode == REQUEST_GALLERY_SELECT && data != null) {
                // get the URI for the photo selected from gallery and send it to the android photo editor to crop
                mUri = data.getData();
                dispatchCropIntent(mUri);
                return;
            }
            // get the cropped photo and send ImageView in app for a preview
            if (requestCode == REQUEST_PHOTO_CROP && data != null) {
                // output URI is saved in the intent data, see link below
                // https://android.googlesource.com/platform/packages/apps/Gallery2/+/c9f743a/src/com/android/gallery3d/app/CropImage.java
                mUri = data.getData();
//                ImageDecoder.Source source = ImageDecoder.createSource(getContentResolver(), mUri);
                //TODO: is it appropriate to use photopath to save paths - if we add to arraylist then value changes ???
                // BUG: by selecting gallery photo then cancel a new selection (clears photopath) took photo then start
                // game pressing back twice, got a blank image in recycler + the new photos, if select blank choice
                // and start, app crashes - note: gallery selected photo is deleted in directory while photo is present
                try {
                    Bitmap bitmap = BitmapFactory.decodeStream(getContentResolver().openInputStream(mUri));
//                    Bitmap bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), mUri);
//                    mImage = bitmap;
                    cropView.setImageBitmap(bitmap);
                    mImage = bitmap;
//                    cropView.setImageURI(mUri);
//                    cropView.setTag(mCurrentPhotoPath);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }

    }

    private void dispatchCropIntent(Uri input) {
        //TODO: need a backup if device cant run this crop intent
        Intent intent = new Intent("com.android.camera.action.CROP", MediaStore.Images.Media.EXTERNAL_CONTENT_URI);

        File file = null;
        try {
            file = createImageFile();
        } catch (IOException e) {
            e.printStackTrace();
        }
        Uri output = Uri.fromFile(file);

        //        if (intent.resolveActivity(getPackageManager()) != null) {
        try {
            intent.setDataAndType(input, "image/*");
            // must flag both read and write permissions or will get security error
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            intent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
            intent.putExtra(MediaStore.EXTRA_OUTPUT, output);  // output file uri
            // output smaller photo
            //TODO: could be too large if user has poor quality camera or it will just scale it and look blurry?
            intent.putExtra("outputX", 1000);
            intent.putExtra("outputY", 1000);
            // set aspect ratio to 1:1 for a square image
            intent.putExtra("aspectX", 1);
            intent.putExtra("aspectY", 1);
//            intent.putExtra("return-data", true);
            intent.putExtra("scale", true);
            intent.putExtra("outputFormat", Bitmap.CompressFormat.JPEG.toString());
            intent.putExtra("noFaceDetection", true);
            startActivityForResult(intent, REQUEST_PHOTO_CROP);
        } catch (ActivityNotFoundException e) {
//        } else {
            Toast cropToast = Toast.makeText(getApplicationContext(), "Device unable to run crop intent", Toast.LENGTH_LONG);
            cropToast.show();
            e.printStackTrace();
        }
        // Variable will update Recycler View in MainActivity with new image if user navigates back there
        savedPhotos.add(mCurrentPhotoPath);
        mSaved = true;
    }

    /**
     * Rotates the given photo by @direction degrees of clockwise rotation and saves the new image over the old one
     * @param direction intended value of either 90 or 270, determines if the photo rotates right or left, respectively
     */
    private Bitmap rotatePhoto(float direction, Bitmap img) {
        // rotate image to correct orientation
        mSaved = false;
        Matrix matrix = new Matrix();
        matrix.postRotate(direction);
        return Bitmap.createBitmap(img, 0, 0, img.getWidth(), img.getHeight(), matrix, true);
    }

    /**
     * Retrieve camera or gallery image data from mImage after a failed crop intent and auto crop to size.
     */
    private void retrievePhoto() {
        if (mUri == null) {
            return;
        }
//        Log.i(TAG, "retrieve");
        Bitmap bitmap = null;
        try {
            try (InputStream inputStream = getContentResolver().openInputStream(mUri)) {
                bitmap = BitmapFactory.decodeStream(inputStream);
            }
        } catch (NullPointerException | IOException e) {
            e.printStackTrace();
        }
        if (bitmap != null) {
            int bmpSize = bitmap.getHeight();
            Matrix matrix = new Matrix();
            // image is in landscape by default, so rotate it
            matrix.postRotate(90);
            float scaled = 1000f/bmpSize;
            matrix.postScale(scaled, scaled);
            Bitmap cropped = Bitmap.createBitmap(bitmap, 0, 0, bmpSize, bmpSize, matrix, true);
            mImage = cropped;
            cropView.setImageBitmap(cropped);
        }
        mSaved = false;
    }

    /**
     * Given a Bitmap, this function will save the image into an app specific image folder for future use
     * @param image An input Bitmap image
     */
    private void savePhoto(Bitmap image) {
        //TODO: check device storage before saving
        File photoFile = null;
        try {
            photoFile = createImageFile();
        } catch (IOException ex) {  // error when making file
            ex.printStackTrace();
        }
        // if successful in creating File, save the photo into it
        if (photoFile != null) {
            try {
                try (FileOutputStream fileOutputStream = new FileOutputStream(mCurrentPhotoPath)) {
                    image.compress(Bitmap.CompressFormat.JPEG, 100, fileOutputStream);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
            // Variable will update Recycler View in MainActivity with new image if user navigates back there
            savedPhotos.add(mCurrentPhotoPath);
            mSaved = true;
        }
    }

    //TODO: implement this ?
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
}
