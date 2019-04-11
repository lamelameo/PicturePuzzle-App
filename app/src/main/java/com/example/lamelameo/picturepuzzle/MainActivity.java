package com.example.lamelameo.picturepuzzle;

import android.content.Intent;
import android.graphics.*;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;
import android.widget.*;

import java.io.File;
import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";
    private RecyclerView mRecyclerView;
    private ImageView mCameraView;
    private String mCurrentPhotoPath;
//    private Drawable[] puzzleImages;
    private int mGridRows;
    private boolean defaultAdapter;

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
        Bitmap bitmap = BitmapFactory.decodeFile(photopath, bmOptions);
        return new BitmapDrawable(getResources(), bitmap);
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

        final int[] drawableInts = {
                R.drawable.dfdfdefaultgrid, R.drawable.dfdfcarpet, R.drawable.dfdfcat, R.drawable.dfdfclock,
                R.drawable.dfdfcrab, R.drawable.dfdfdarklights, R.drawable.dfdfnendou,
                R.drawable.dfdfrazer, R.drawable.dfdfsaiki, R.drawable.dfdfmms
        };

        File imageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File[] storedImages = imageDir.listFiles();  //TODO: why warning - even when no files no exception
        final ArrayList<Drawable> savedPhotos = new ArrayList<>();
        final ArrayList<String> photoPaths = new ArrayList<>();
        float density = getResources().getDisplayMetrics().density;
        long recyclerViewPx = Math.round(150 * density);
        for (File file : storedImages) {
            if (file.length() == 0) {  // checks for empty files and deletes them
                boolean deletedFile = file.delete();
                Log.i(TAG, "onCreate:fileDeleted? "+deletedFile);
            } else {  // TODO: could have wrong file types?
                Log.i(TAG, "onCreate:file "+file.getName());
                String imagePath = file.getAbsolutePath();
                photoPaths.add(imagePath);
                Drawable imageBitmap = scalePhoto((int)recyclerViewPx, imagePath);
//            Drawable drawable = BitmapDrawable.createFromPath(imagePath);
                savedPhotos.add(imageBitmap);
            }
        }

        mRecyclerView = findViewById(R.id.pictureRecyclerView);
        // improves performance given that recycler does not change size based on its contents (the images)
        mRecyclerView.setHasFixedSize(true);
        // use layout manager -  horizontal orientation = 1, vertical = 0
        RecyclerView.LayoutManager layoutManager = new LinearLayoutManager(this, 1, false);
        mRecyclerView.setLayoutManager(layoutManager);
        // set adapter to default use default image dataset
        final ImageRecyclerAdapter testAdapter = new ImageRecyclerAdapter(drawableInts,this);
        mRecyclerView.setAdapter(testAdapter);
        defaultAdapter = true;
        // toggle recycler view between default images and photos taken and saved using this app
        ToggleButton adapterButton = findViewById(R.id.adapterButton);
        final ImageRecyclerAdapter photoAdapter = new ImageRecyclerAdapter(savedPhotos, this);
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

        // move to cropper activity to crop gallery images or take photo with camera
        Button cameraGalleryButton = findViewById(R.id.photoCropButton);
        final Intent cropperIntent = new Intent(this, PhotoCropping.class);
        cameraGalleryButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(cropperIntent);
            }
        });

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
                //TODO: should change all this with switch case instead, same as in game activity, is much simpler?
                gameIntent.putExtra("numColumns", mGridRows);  // set extra for grid size
                // remove any previous extra so the game activity does not use it instead of the intended image/photo
                gameIntent.removeExtra("photoPath");  // app taken photo
                gameIntent.removeExtra("appPhotoPath"); // saved photos
                gameIntent.removeExtra("drawableId");  // default images
                // set extra for image to use - check if photo has been taken or use the default numbered image
                if (mCurrentPhotoPath != null) {  // if taken pic use that
                    gameIntent.putExtra("photoPath", mCurrentPhotoPath);
                    gameIntent.putExtra("puzzleNum", -1);
                } else {  // get drawable id or photo path for selected image from recycler view adapter
                    int selectedImage = testAdapter.getSelection();
                    // if no selection, check for selected grid size to send the appropriate default image
                    if (selectedImage == -1) {
                        gameIntent.putExtra("drawableId", defaultPuzzles[mGridRows - 3]);  // 3x3 grid is index 0 in array
                    } else {  // there is a selected item from whichever dataset is displayed
                        if (defaultAdapter) {  // selection is from default images
                            Log.i(TAG, "default image chosen");
                            // if there is a selection send the id and puzzle number to the game activity
                            gameIntent.putExtra("drawableId", drawableInts[selectedImage]);
                            gameIntent.putExtra("puzzleNum", selectedImage);
                        } else {  // selection is from app photos
                            Log.i(TAG, "app photo chosen");
                            //TODO: send photo path, with no puzzlenum extra as savefile does not support it
                            gameIntent.putExtra("appPhotoPath", photoPaths.get(selectedImage));
                            gameIntent.putExtra("puzzleNum", -1);
                        }
                    }
                }
                startActivity(gameIntent);  // start game activity
            }
        });

    }

    @Override
    protected void onResume() {
        super.onResume();
        //TODO: must update recycler view adapter for photos, as new photos may have been loaded

    }
}
