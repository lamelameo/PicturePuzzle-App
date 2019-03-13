package com.example.lamelameo.picturepuzzle;

import android.content.Intent;
import android.content.res.Resources;
import android.graphics.*;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.annotation.Nullable;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;
import android.widget.*;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private String TAG = "MainActivity";
    private RecyclerView mRecyclerView;
    private RecyclerView.Adapter mAdapter;
    private RecyclerView.Adapter mPhotoAdapter;
    private RecyclerView.LayoutManager mlayoutManager;
    private ImageView mCameraView;
    private String mCurrentPhotoPath;
//    private Drawable[] puzzleImages;
    private int mGridRows;
    private boolean defaultAdapter;

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
                startActivityForResult(cameraIntent, 1);
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
        if  (requestCode == 1) {  // result from camera intent
            if (resultCode == RESULT_OK) {  // got a photo back from camera
                // create the image preview and add to phones gallery
                mCameraView.setImageDrawable(scalePhoto(mCameraView.getWidth(), mCurrentPhotoPath));
                addPicGallery();
            } else {  // didnt get a return value (no photo)
                // clear file at photopath or will get errors reopening app as there are empty files with no pic stored
                try {
                    boolean deleteTempFile = new File(mCurrentPhotoPath).delete();
                    Log.i(TAG, "deleted temp photo file: "+deleteTempFile);
                } catch (Exception e) {
                    e.printStackTrace();
                    Log.i(TAG, "tried to delete temp file on no result camera exit, but got error");
                }
                // clear photopath or will get errors trying to access null image in game activity
                Log.i(TAG, "no photo-resultCode: " + resultCode);
                mCurrentPhotoPath = null;
            }
        }
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

    /**
     * Setup Buttons and their onClickListeners that allow the user to choose settings, take a photo, and start a puzzle.
     * Also setup the recycler view which displays image choices for the puzzle.
     * @param savedInstanceState get previously saved activity instance
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //TODO: picture button allows you to choose image from gallery or take photo
        //  can search gallery images with mediastore intent

//        Field[] drawableFields = com.example.lamelameo.picturepuzzle.R.drawable.class.getFields();
//        ArrayList<Drawable> defaultImages = new ArrayList<>();
//        Drawable[] puzzleImages = new Drawable[12];
//        int counter = 0;
//        for (Field field : drawableFields) {
//            try {
//                String fieldName = field.getName();
////                Log.i(TAG, "onCreate: "+fieldName);
//                if (fieldName.startsWith("dfdf")) {
//                    defaultImages.add(getResources().getDrawable(field.getInt(null)));
//                    puzzleImages[counter] = getResources().getDrawable(field.getInt(null));
//                    counter+=1;
//                }
//            } catch (Exception e) {
//                e.printStackTrace();
//            }
//        }
//        Log.i(TAG, "onCreate: "+defaultImages);

        final int[] drawableInts = {
                R.drawable.dfdfdefaultgrid, R.drawable.dfdfcarpet, R.drawable.dfdfcat, R.drawable.dfdfclock,
                R.drawable.dfdfcrab, R.drawable.dfdfdarklights, R.drawable.dfdfnendou,
                R.drawable.dfdfrazer, R.drawable.dfdfsaiki, R.drawable.dfdfmms
        };

        File imageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File[] storedImages = imageDir.listFiles();
        final ArrayList<Drawable> savedPhotos = new ArrayList<>();
        final ArrayList<String> photoPaths = new ArrayList<>();
        float density = getResources().getDisplayMetrics().density;
        long recyclerViewPx = Math.round(150 * density);
        for (File file : storedImages) {
            Log.i(TAG, "onCreate:file "+file.getName());
            String imagePath = file.getAbsolutePath();
            photoPaths.add(imagePath);
            Drawable imageBitmap = scalePhoto((int)recyclerViewPx, imagePath);
//            Drawable drawable = BitmapDrawable.createFromPath(imagePath);
            savedPhotos.add(imageBitmap);
            //TODO: have to rotate and scale images, recyclerview is very slow...
        }

        //TODO: create a fragment to scroll through images including the defaults (at top) and image gallery
        // add defaults to app pics on create,

        mRecyclerView = findViewById(R.id.pictureRecyclerView);
        // improves performance given that view doesnt change size
//        mRecyclerView.setHasFixedSize(true);
        // use layout manager -  horizontal orientation = 1, vertical = 0
        mlayoutManager = new LinearLayoutManager(this, 1, false);
        mRecyclerView.setLayoutManager(mlayoutManager);
        // set adapter to default use default image dataset
        final ImageRecyclerAdapter testAdapter = new ImageRecyclerAdapter(drawableInts,this);
        mAdapter = testAdapter;
        mRecyclerView.setAdapter(testAdapter);
        defaultAdapter = true;
        // toggle recycler view between default images and photos taken and saved using this app
        ToggleButton adapterButton = findViewById(R.id.adapterButton);
        final ImageRecyclerAdapter photoAdapter = new ImageRecyclerAdapter(savedPhotos, this);
        mPhotoAdapter = photoAdapter;
        //TODO: could use a grid layout manager to allow for a grid in recycler view rather than list (or a choice)

        // set button listener to change between datasets (defaults or photos) for the recycler view
        adapterButton.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                defaultAdapter = !defaultAdapter;  // update boolean to track which dataset is displayed
                if (isChecked) {  // recycler displaying default images -> change to app photo gallery
                    Log.i(TAG, "onCheckedChanged: checked");
                    mRecyclerView.swapAdapter(photoAdapter, true);
//                    testAdapter.resetSelection();
                    // inform adapter of dataset change to defaults
                    photoAdapter.notifyDataSetChanged();
                    photoAdapter.setIsDefaultImages();  // update boolean which tells adapter which dataset is shown
                } else {  // recycler displaying app gallery -> defaults
                    Log.i(TAG, "onCheckedChanged: unchecked");
                    mRecyclerView.swapAdapter(testAdapter, true);
//                    testAdapter.resetSelection();
                    testAdapter.notifyDataSetChanged();
                    testAdapter.setIsDefaultImages();
                }
            }
        });

        // take a photo from camera and get pic
        mCameraView = findViewById(R.id.photoView);
        ImageButton cameraButton = findViewById(R.id.cameraButton);
        cameraButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // open camera and take photo and send preview to framelayout
                dispatchTakePictureIntent();

            }
        });

        //TODO: use to change overlay of photo previews... setForeground requires higher min SDK
        final int[] gridOverlays = {R.drawable.gridoverlay3, R.drawable.gridoverlay4,
                R.drawable.gridoverlay5, R.drawable.gridoverlay6};

        // set gridsize based on the checked radio button, this value will be used as an intent extra when starting the game
        final RadioGroup setGrid = findViewById(R.id.setGrid);
        setGrid.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                Log.i(TAG, "onCheckedChanged: "+checkedId);
                for (int x = 0; x<4; x++) {
                    RadioButton radioButton = (RadioButton)group.getChildAt(x);
                    if (radioButton.getId() == checkedId) {
                        mGridRows = x + 3;  // update grid size for use in load button listener in this context
                        testAdapter.setmGridRows(x + 3);  // send the value to recycler adapter for use in button listener there
                        //TODO: set photo previews grid overlay based on checked radio button
//                        Drawable gridOverlay = getResources().getDrawable(gridOverlays[x], null);
//                        mCameraView.setForeground(gridOverlay);
                        break;
                    }
                }
            }
        });

        final Intent gameIntent = new Intent(this, PuzzleGridTest.class);
        mGridRows = 4; // default amount of grid rows is 4
        final int[] defaultPuzzles = {R.drawable.grid9, R.drawable.grid15, R.drawable.grid25, R.drawable.grid36};

        // on click listener for the load button creates an intent to start the game activity and sets extras to give
        // that activity the information of grid size and image to use
        Button loadButton = findViewById(R.id.loadButton);
        loadButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //TODO: should change all this with switch case instead, same as in game activity, is much simpler
                gameIntent.putExtra("numColumns", mGridRows);  // set extra for grid size
                // set extra for image to use - check if photo has been taken or use the default numbered image
                if (mCurrentPhotoPath != null) {  // if taken pic use that
                    gameIntent.putExtra("photoPath", mCurrentPhotoPath);
                    gameIntent.putExtra("puzzleNum", -1);
                } else {  // get drawable for selected image from recycler view adapter
                    int selectedImage = testAdapter.getSelection();
                    gameIntent.removeExtra("photoPath");  // must remove or will choose photo
                    if (defaultAdapter) {  // selection is from default images
                        Log.i(TAG, "default image chosen");
                        gameIntent.removeExtra("appPhotoPath");
                        // if we have no selection, check for selected grid size to send the appropriate default image
                        if (selectedImage == -1) {
                            gameIntent.putExtra("drawableId", defaultPuzzles[mGridRows - 3]);  // 3x3 grid is index 0 in array
                        } else {  // if there is a selection send the id and puzzle number to the game activity
                            gameIntent.putExtra("drawableId", drawableInts[selectedImage]);
                            gameIntent.putExtra("puzzleNum", selectedImage);
                        }
                    } else {  // selection is from app photos
                        Log.i(TAG, "app photo chosen");
                        if (selectedImage == -1) {
                            gameIntent.removeExtra("appPhotoPath");
                            gameIntent.putExtra("drawableId", defaultPuzzles[mGridRows - 3]);
                        } else {
                            //TODO: send photo path, with no puzzlenum extra as savefiel doesnt support it
                            gameIntent.putExtra("appPhotoPath", photoPaths.get(selectedImage));
                            gameIntent.putExtra("puzzleNum", -1);
                        }
                    }
                }
                startActivity(gameIntent);  // start game activity
            }
        });

    }
}
