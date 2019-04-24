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
import android.support.v4.content.FileProvider;
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
        Bitmap savedPhoto = BitmapFactory.decodeFile(mCurrentPhotoPath);
        cropView.setImageBitmap(savedPhoto);
        ImageView rotateRight = findViewById(R.id.rotateRight);
        ImageView rotateLeft = findViewById(R.id.rotateLeft);
        rotateRight.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
               rotatePhoto(90);
            }
        });
        rotateLeft.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                rotatePhoto(270);
            }
        });

        // start game button onclicklistener
        Button startGame = findViewById(R.id.startGame);
        final Intent gameIntent = new Intent(this, PuzzleActivity.class);
        startGame.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mCurrentPhotoPath != null) {
                    //TODO: 1.save photo only on game start as this means user wants photo?
                    //  2.SEND newly saved photos to puzzle in case add function to move from Puzzle to Main directly
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
        Button galleryButton = findViewById(R.id.galleryButton);

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
    private View.OnClickListener arrowClickListener = new View.OnClickListener() {
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
     * @param requestCode request code is 1 for our camera intent
     * @param resultCode RESULT_OK means we got a photo, RESULT_CANCELLED means no photo
     * @param data returns result data from the camera Intent we used, can use getExtras to obtain this
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        // TODO: toast for each time photo is saved to app??
        // for cancelled results, must remove created file if empty and clear the photo path to avoid errors
        if (resultCode == RESULT_CANCELED) {
            try {
                File photoFile = new File(mCurrentPhotoPath);
                if (photoFile.length() == 0) {
                    boolean emptyFileDeleted = photoFile.delete();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            // If a previously loaded photo is still present in the preview, retrieve its path and set mCurrentPhotoPath
            // else, clear mCurrentPhotoPath or will get errors trying to access null image in game activity
            if (cropView.getTag() != null) {
                mCurrentPhotoPath = (String)cropView.getTag();
            } else {
                mCurrentPhotoPath = null;
            }
        }
        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK) {  // got a photo from camera intent
            // we already have the photo file path, which we will use to crop the photo and save in same file path
            File croppedPhotoFile = new File(mCurrentPhotoPath);
            // this is the URI to save the photo into after cropping - for camera photos it will overwrite
            // must use provider as we are sending photo saved in app folder to cropper activity outside app
            Uri croppedPhotoURI = FileProvider.getUriForFile(this, "com.example.android.fileprovider",
                    croppedPhotoFile);
            dispatchCropIntent(croppedPhotoURI, croppedPhotoURI);
        } else {
            // process gallery image selection using image URI from gallery sending to gallery cropper, then saving
            // to app using data given back by intent in getData(), no permission problems
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
                    dispatchCropIntent(selectedPhotoURI, croppedPhotoURI);
                }
            }
            // get the cropped photo and send ImageView in app for a preview
            if (requestCode == REQUEST_PHOTO_CROP && resultCode == RESULT_OK && data != null) {
                // update num saved photos to send to main activity for updating the recycler view
//                savedPhotos.add(mCurrentPhotoPath);
                String photoPath = mCurrentPhotoPath;
                savedPhotos.add(photoPath);
                // output URI is saved in the intent data, see link below
                // https://android.googlesource.com/platform/packages/apps/Gallery2/+/c9f743a/src/com/android/gallery3d/app/CropImage.java
                Uri croppedPhotoUri = data.getData();
                //TODO: is it appropriate to use photopath to save paths - if we add to arraylist then value changes ???
                // BUG: by selecting gallery photo then cancel a new selection (clears photopath) took photo then start
                // game pressing back twice, got a blank image in recycler + the new photos, if select blank choice
                // and start, app crashes - note: gallery selected photo is deleted in directory while photo is present
                try {
                    Bitmap bitmap = BitmapFactory.decodeStream(getContentResolver().openInputStream(croppedPhotoUri));
                    cropView.setImageBitmap(bitmap);
                    cropView.setTag(mCurrentPhotoPath);
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                }
            }
        }

    }

    private void dispatchCropIntent(Uri data, Uri saveUri) {
        //TODO: need a backup if device cant run this crop intent
        try {  // catch exception for devices without this crop activity
            Intent intent = new Intent("com.android.camera.action.CROP");
            intent.setDataAndType(data, "image/*");
            // must flag both read and write permissions or will get security error
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            intent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
            intent.putExtra(MediaStore.EXTRA_OUTPUT, saveUri);  // output file uri
            // output smaller photo
            //TODO: could be too large if user has poor quality camera or it will just scale it and look blurry?
            intent.putExtra("outputX", 1000);
            intent.putExtra("outputY", 1000);
            // set aspect ratio to 1:1 for a square image
            intent.putExtra("aspectX", 1);
            intent.putExtra("aspectY", 1);
//        intent.putExtra("return-data", true);
            intent.putExtra("scale", true);
            intent.putExtra("outputFormat", Bitmap.CompressFormat.JPEG.toString());
            intent.putExtra("noFaceDetection", true);
            startActivityForResult(intent, REQUEST_PHOTO_CROP);
        } catch (ActivityNotFoundException e) {
            e.printStackTrace();
            createErrorToast(e);
        }
    }

    /**
     * Rotates the given photo by 90 degrees given a direction of rotation and saves the new image over the old one
     * @param direction has value of either 90 or 270, determines if the photo rotates right or left, respectively
     */
    private void rotatePhoto(float direction) {
        if (mCurrentPhotoPath != null) {
            // rotate image to correct orientation - default is landscape
            Matrix matrix = new Matrix();
            matrix.postRotate(direction);
            Bitmap bitmap = BitmapFactory.decodeFile(mCurrentPhotoPath);
            Bitmap rotatedBmp = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
            // update preview and over write the old photo
            cropView.setImageBitmap(rotatedBmp);
            File rotatedPhoto = new File(mCurrentPhotoPath);
            try {
                FileOutputStream fileOutputStream = new FileOutputStream(rotatedPhoto);
                try {
                    rotatedBmp.compress(Bitmap.CompressFormat.JPEG, 100, fileOutputStream);
                } finally {
                    fileOutputStream.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

}
