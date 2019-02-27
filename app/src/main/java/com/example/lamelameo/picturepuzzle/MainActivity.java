package com.example.lamelameo.picturepuzzle;

import android.content.Intent;
import android.content.res.Resources;
import android.graphics.*;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.constraint.ConstraintLayout;
import android.support.v4.content.FileProvider;
import android.support.v4.widget.ImageViewCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.MotionEvent;
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

    @Nullable
    private Integer checkRBs(RadioButton[] radioButtons) {
        // iterate through array to check if any button is checked, then return that items tag, else return null
        for(RadioButton element : radioButtons) {
            if(element.isChecked()) {
                return (int)element.getTag();
            }
        }
        return null;
    }

    //TODO: testing camera properties
//    public static Camera getCameraInstance(Context context) {
//        CameraManager cameraManager = (CameraManager)context.getSystemService(CAMERA_SERVICE);
//        try {
//            for (String cameraId : cameraManager.getCameraIdList()) {
//                CameraCharacteristics chars = cameraManager.getCameraCharacteristics(cameraId);
//                chars.get(CameraCharacteristics.SENSOR_INFO_ACTIVE_ARRAY_SIZE);
//                CameraCharacteristics.SENSOR_INFO_ACTIVE_ARRAY_SIZE = new Rect(0,0,0,0);
//                StreamConfigurationMap;
//                CaptureRequest.SCALER_CROP_REGION;
//
//            }
//        } catch (CameraAccessException e) {
//            e.printStackTrace();
//        }
//    }

    private void dispatchTakePictureIntent() {
        // invokes intent to take a photo using the camera
        Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (cameraIntent.resolveActivity(getPackageManager()) != null) {
            File photoFile = null;
            try {
                photoFile = createImageFile();
            } catch (IOException ex) {
                // error when making file
                ex.printStackTrace();
            }

            if (photoFile != null) {
                Uri photoURI = FileProvider.getUriForFile(this, "com.example.android.fileprovider", photoFile);
                cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
                startActivityForResult(cameraIntent, 1);
                //TODO: SET CAMERA SIZE to 1:1 instead of 4:3 default..??? can do this?
            }
        }
    }

    private File createImageFile() throws IOException {
        // save the full sized image taken from camera to an app private directory that is deleted if app is removed
//        Locale locale = ConfigurationCompat.getLocales(Resources.getSystem().getConfiguration());
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File image = File.createTempFile(imageFileName, ".jpg", storageDir);
        mCurrentPhotoPath = image.getAbsolutePath();
        return image;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        // gets the image taken with camera as a bitmap displayed in an ImageView for a preview
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

    private void addPicGallery() {
        // save a picture taken from this app to the gallery
        Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
        File f = new File(mCurrentPhotoPath);
        Uri contentUri = Uri.fromFile(f);
        mediaScanIntent.setData(contentUri);
        this.sendBroadcast(mediaScanIntent);
        // directory for gallery images
//        Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
        //TODO: get pics from storage to display?
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

        final Intent intent1 = new Intent(this, game_test.class);
        final Intent intent2 = new Intent(this, PuzzleGridTest.class);
        mGridRows = 4; // default amount of grid rows is 4

        final int[] drawableInts = {
                R.drawable.dfdfdefaultgrid, R.drawable.dfdfcarpet, R.drawable.dfdfcat, R.drawable.dfdfclock,
                R.drawable.dfdfcrab, R.drawable.dfdfdarklights, R.drawable.dfdfnendou,
                R.drawable.dfdfrazer, R.drawable.dfdfsaiki, R.drawable.dfdfmms
        };

        File imageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File[] storedImages = imageDir.listFiles();
        final ArrayList<Drawable> savedPhotos = new ArrayList<>();
        float density = getResources().getDisplayMetrics().density;
        long recyclerViewPx = Math.round(150 * density);
        for (File file : storedImages) {
            Log.i(TAG, "onCreate:file "+file.getName());
            String imagePath = file.getAbsolutePath();
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
        final ImageRecyclerAdapter testAdapter = new ImageRecyclerAdapter(drawableInts, intent2,this);
        mAdapter = testAdapter;
        mRecyclerView.setAdapter(testAdapter);

        // toggle recycler view between default images and photos taken and saved using this app
        ToggleButton adapterButton = findViewById(R.id.adapterButton);
        final ImageRecyclerAdapter photoAdapter = new ImageRecyclerAdapter(savedPhotos, intent2, this);
        mPhotoAdapter = photoAdapter;
        adapterButton.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                //TODO: have to update selection upon changing adapters
                if (isChecked) {
                    Log.i(TAG, "onCheckedChanged: checked");
                    mRecyclerView.swapAdapter(photoAdapter, true);
                    photoAdapter.resetSelection();

                } else {
                    Log.i(TAG, "onCheckedChanged: unchecked");
                    mRecyclerView.swapAdapter(testAdapter, true);
                    testAdapter.resetSelection();
                }
            }
        });

        final RadioGroup setGrid = findViewById(R.id.setGrid);
        RadioButton set3 = findViewById(R.id.set3);
        set3.setTag(3);
        RadioButton set4 = findViewById(R.id.set4);
        set4.setTag(4);
        RadioButton set5 = findViewById(R.id.set5);
        set5.setTag(5);
        RadioButton set6 = findViewById(R.id.set6);
        set6.setTag(6);
        final RadioButton[] radioButtons = {
                set3, set4, set5, set6
        };

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

        // set the gridsize based on the checked button, this value will be used as an intent extra when starting the game
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

        // on click listener for the load button creates an intent to start the game activity and sets extras to give
        // that activity the information of grid size and image to use
        Button loadButton = findViewById(R.id.loadButton);
        loadButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                intent2.putExtra("numColumns", mGridRows);  // set extra for grid size
                // set extra for image to use - check if photo has been taken or use the default numbered image
                if (mCurrentPhotoPath != null) {  // if taken pic use that
                    intent2.putExtra("photoPath", mCurrentPhotoPath);
                    intent2.putExtra("puzzleNum", -1);
                } else {  // get drawable for selected image from recycler view
                    int selectedImage = testAdapter.getmSelectedImage();
                    // if there is a selection send the id and puzzle number to the game activity - defaults are 15grid
                    if (selectedImage != -1) {
                        intent2.putExtra("drawableId", drawableInts[selectedImage]);
                        intent2.putExtra("puzzleNum", selectedImage);
                    }
                }
                startActivity(intent2);  // start game activity
            }
        });

    }
}
