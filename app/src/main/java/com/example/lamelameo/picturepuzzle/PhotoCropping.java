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
import androidx.annotation.Nullable;
import androidx.core.content.FileProvider;
import androidx.appcompat.app.AppCompatActivity;
import android.view.View;
import android.widget.*;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

public class PhotoCropping extends AppCompatActivity {

    private static final String TAG = "PhotoCropping_debug";
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


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_photo_cropping);

        if (savedInstanceState != null) {
            mCurrentPhotoPath = (String) savedInstanceState.getCharSequence("photoPath");
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
                    if (!mSaved) {
                        savePhoto(mImage);
                    }
                    gameIntent.putExtra("photoPath", mCurrentPhotoPath);
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
                Toast noImage = Toast.makeText(getApplicationContext(), "No image to save.", Toast.LENGTH_SHORT);
                noImage.show();
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
                for (int x = 0; x < 4; x++) {
                    RadioButton radioButton = (RadioButton) group.getChildAt(x);
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
    protected void onDestroy() {
        super.onDestroy();
    }

    private void deleteEmptyFile() {
        if (mCurrentPhotoPath != null) {
            File photo = new File(mCurrentPhotoPath);
            if (photo.length() == 0) {
                boolean deleted = photo.delete();
            }
        }
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

    /**
     * Create and show a Toast to display an exception message encountered by the app
     *
     * @param exception the Exception the app has produced
     */
    private void createErrorToast(Exception exception) {
        // gives the exceptions name and message (if any)
        String errorMessage = exception.toString();
        Toast errorToast = Toast.makeText(getApplicationContext(), errorMessage, Toast.LENGTH_SHORT);
        errorToast.show();
    }

    /**
     * Creates and invokes an Intent to take a photo using the camera
     */
    private void dispatchTakePictureIntent() {
        Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (cameraIntent.resolveActivity(getPackageManager()) != null) {
            File photoFile = null;
            try {
                photoFile = createImageFile();
            } catch (IOException e) {
                createErrorToast(e);
            }
            if (photoFile != null) {
                mUri = FileProvider.getUriForFile(this,
                        "com.example.android.fileprovider",
                        photoFile);
                cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, mUri);
                startActivityForResult(cameraIntent, REQUEST_IMAGE_CAPTURE);
            }
        }
    }

    private void dispatchGallerySelectIntent() {
        Intent galleryIntent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.INTERNAL_CONTENT_URI);
        startActivityForResult(galleryIntent, REQUEST_GALLERY_SELECT);
    }

    /**
     * Save the full sized image taken from camera to an app private directory that is deleted if app is removed
     *
     * @return a File to store a photo taken with the camera intent
     * throws IOException error when making File
     */
    private File createImageFile() throws IOException {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        String imageFileName = timeStamp + "_";
        File image = File.createTempFile(imageFileName, ".jpg", getExternalFilesDir(Environment.DIRECTORY_PICTURES));
        mCurrentPhotoPath = image.getAbsolutePath();
        return image;
    }

    /**
     * Gets the image selected/taken and converts to a Bitmap to display in an ImageView {@link #cropView} as a preview
     *
     * @param requestCode 0: Gallery select 1: Camera, 2: Crop image
     * @param resultCode  RESULT_OK means we got a photo, RESULT_CANCELLED means no photo
     * @param data        returns result data from the intent we used, can use getExtras or getData to obtain this
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        // for cancelled results, must remove created file if empty and clear the photo path to avoid errors
        if (resultCode == RESULT_CANCELED) {
            deleteEmptyFile();
            // retrieve any image from camera or gallery if crop fails
            mImage = retrievePhoto();
            cropView.setImageBitmap(mImage);

        } else {  // process RESULT_OK for different request codes
            if (requestCode == REQUEST_IMAGE_CAPTURE) {  // got a photo from camera intent
                savePhoto(rotatePhoto(90, retrievePhoto()));
                dispatchCropIntent(mUri, mUri);
                return;
            }
            // process gallery image selection using image URI from gallery sending to gallery cropper, then saving
            // to app using data given back by intent in getData(), no permission problems
            if (requestCode == REQUEST_GALLERY_SELECT && data != null) {
                // get the URI for the photo selected from gallery and send it to the android photo editor to crop
                mSaved = false;
                mUri = data.getData();
                dispatchCropIntent(mUri, null);
                return;
            }
            // get the cropped photo and send ImageView in app for a preview
            if (requestCode == REQUEST_PHOTO_CROP && data != null) {
                // output URI is saved in the intent data, see link below
                // https://android.googlesource.com/platform/packages/apps/Gallery2/+/c9f743a/src/com/android/gallery3d/app/CropImage.java
                mUri = data.getData();
                try {
                    Bitmap bitmap = BitmapFactory.decodeStream(getContentResolver().openInputStream(mUri));
                    cropView.setImageBitmap(bitmap);
                    mImage = bitmap;
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }

    }

    /**
     * Try to send a crop intent using the given input image Uri and output Uri and display a toast if it fails.
     * @param input Uri of image to be cropped
     * @param output Uri for cropped image to be saved into
     */
    private void dispatchCropIntent(Uri input, Uri output) {
//        Log.i(TAG, "dispatchCropIntent: ");
        Intent intent = new Intent("com.android.camera.action.CROP");
        if (output == null) {
            File file = null;
            try {
                file = createImageFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
            output = Uri.fromFile(file);
        }

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
            Toast cropToast = Toast.makeText(getApplicationContext(),
                    "Device unable to run crop intent", Toast.LENGTH_LONG);
            cropToast.show();
            e.printStackTrace();
        }
    }

    /**
     * Rotates the given photo by @direction degrees of clockwise rotation and saves the new image over the old one
     *
     * @param degrees intended value of either 90 or 270, determines if the photo rotates right or left, respectively
     */
    private Bitmap rotatePhoto(float degrees, Bitmap img) {
        // rotate image to correct orientation
        mSaved = false;
        Matrix matrix = new Matrix();
        matrix.postRotate(degrees);
        return Bitmap.createBitmap(img, 0, 0, img.getWidth(), img.getHeight(), matrix, true);
    }

    /**
     * Retrieve camera or gallery image data from mImage after a failed crop intent and auto crop to size.
     */
    private Bitmap retrievePhoto() {
        Bitmap bitmap = null;
        Bitmap scaled = null;
        try {
            bitmap = BitmapFactory.decodeStream(getContentResolver().openInputStream(mUri));
        } catch (NullPointerException | IOException e) {
            e.printStackTrace();
        }
        if (bitmap != null) {
            int bmpH = bitmap.getHeight();
            int bmpW = bitmap.getWidth();
            Matrix matrix = new Matrix();
            float scale = 1000f / bmpH;
            matrix.postScale(scale, scale);
            scaled = Bitmap.createBitmap(bitmap, 0, 0, bmpW, bmpH, matrix, true);
        }
        return scaled;
    }

    /**
     * Given a Bitmap, this function will save the image into an app specific image folder for future use
     *
     * @param image An input Bitmap image
     */
    private void savePhoto(Bitmap image) {
        //TODO: check device storage before saving
        File photoFile = null;
        if (mCurrentPhotoPath == null) {
            try {
                photoFile = createImageFile();
            } catch (IOException ex) {  // error when making file
                ex.printStackTrace();
            }
        } else {
            photoFile = new File(mCurrentPhotoPath);
        }

        // if successful in creating File, save the photo into it
        if (photoFile != null) {
            try (FileOutputStream fileOutputStream = new FileOutputStream(photoFile)) {
                image.compress(Bitmap.CompressFormat.JPEG, 100, fileOutputStream);
            } catch (IOException e) {
                e.printStackTrace();
            }
            // Variable will update Recycler View in MainActivity with new image if user navigates back there
            savedPhotos.add(mCurrentPhotoPath);
            mSaved = true;
        }
    }

    //TODO: media scanner cannot access images saved in app specific picture library
    private void addPicGallery() {
        Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
        File f = new File(mCurrentPhotoPath);
        Uri contentUri = Uri.fromFile(f);
        mediaScanIntent.setData(contentUri);
        this.sendBroadcast(mediaScanIntent);
    }
}
