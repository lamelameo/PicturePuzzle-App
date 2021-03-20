package com.example.lamelameo.picturepuzzle.unused;

import android.graphics.*;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import androidx.core.view.GestureDetectorCompat;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.*;
import android.widget.*;
import com.example.lamelameo.picturepuzzle.R;

import java.util.ArrayList;

public class game_test extends AppCompatActivity {

    private String TAG = "game_test";
    private GestureDetectorCompat mDetector;

    // convert density independent pixels to pixels using the devices pixel density
    private int dpToPx(float dp) {
        float density = getResources().getDisplayMetrics().density;
        // rounds up/down around 0.5
        long pixels = Math.round(dp * density);
        Log.i("tag", "dpToPx: "+pixels);
        //TODO: can change to this one line instead...?
//        int pix = Math.round(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,102.5f, getResources().getDisplayMetrics()));
//        Log.i("tagtag", "dp to Px: "+pix);
        return (int) pixels;
    }

    private ArrayList<Drawable> bitmaps = new ArrayList<>();

    private void createBitmapGrid(int drawableId, int rows, int columns) {
        // 4x4 grid 300dp x 300dp
        final Bitmap bmp = BitmapFactory.decodeResource(getResources(), drawableId);

        // size of each
        int size = dpToPx(53);
        int originx = dpToPx(51);
        // for each row loop 4 times creating a new cropped image from original bitmap and add to adapter dataset
        for(int x=0; x<columns; x++) {
            // for each row, increment y value to start the bitmap at
            int ypos = dpToPx(x*51);
            for(int y=0; y<rows; y++) {
                int index = x*columns + y;
                // loop through 4 positions in row incrementing the x value to start bitmap at
                int xpos = dpToPx(y*51);
                Bitmap gridImage = Bitmap.createBitmap(bmp, xpos, ypos, size, size);
                //TODO: converted to drawable for use of setImageDrawable to easily swap cell images
                Drawable drawable = new BitmapDrawable(getResources(), gridImage);
                bitmaps.add(drawable);
            }
        }
    }

    private ImageView emptyCell(GridView gridView, int gridSize) {
        // check all cells in the grid and return the cell with the empty image
        for (int x=0; x<gridSize; x++) {
            ImageView imageView = (ImageView)gridView.getItemAtPosition(x);
            int viewTag = (int)imageView.getTag();
            if (viewTag == gridSize) {
                return imageView;
            }
        }
        return null;
    }

    //TODO: can integrate into 1 method by just checking all columns and rows and do move stuff on relevant row/col
    private boolean gridCorrect(GridView gridView) {
        // iterate through all views in all rows to check if the position number matches the set image (using tag)
        // if true for all views this means the grid is solved so return true, else return false
        int numCells = gridView.getCount();
        for (int x=0; x<numCells; x++) {
            ImageView cell = (ImageView) gridView.getItemAtPosition(x);
            Log.i(TAG, "gridCorrectCell: "+x+" tag: "+cell.getTag());
            if (x != (int)cell.getTag()) {
                return false;
            }
        }
        return true;
    }

    private ImageView setVar(View defaultView, ArrayList<ImageView> group, int num, int index) {

        try {  // reassign variables to neighbouring cells (if border cell -> 1 or more assignment gives exception)
            defaultView = group.get(index+num);
        } catch (IndexOutOfBoundsException exception) {  // catch index error for border/edge cells
            Log.i(TAG, "moveCell: "+ exception);
        }
        return (ImageView)defaultView;
    }

    private void swipeCell(View view, int gridIndex, ImageAdapter imageAdapter) {

        // obtaining cell image, the row and column lists they are part of and their index in those lists
        ImageView cellImage = (ImageView)view;
        int gridSize = imageAdapter.getGridRows();
        int cellRow = (int)Math.floor(gridIndex/(float)gridSize);
        int cellCol = gridIndex - cellRow*gridSize;
        ArrayList<ImageView> row = imageAdapter.getRow(cellRow);
        ArrayList<ImageView> col = imageAdapter.getCol(cellCol);
        int cellRowIndex = row.indexOf(view);
        int cellColIndex = col.indexOf(view);
        // amount of moves in certain directions this cell could possibly move within a row/column
        int leftMoves = cellRowIndex;
        int rightMoves = gridSize - 1 - cellRowIndex;
        int upMoves = cellColIndex;
        int downMoves = gridSize - 1 - cellColIndex;


        //TODO: implement this for a swipe on a cell in same row/col as empty cell
    }

    private void moveCell(View view, int gridIndex, ImageAdapter imageAdapter) {
        // check other cells in same row and column as clicked view are the empty cell, which means it should move

        ImageView imageView = (ImageView)view;

        int gridSize = imageAdapter.getGridRows();
        //TODO: allow for different row/col sizes
        int cellRow = (int)Math.floor(gridIndex/(float)gridSize);
        int cellCol = gridIndex - cellRow*gridSize;
        Log.i(TAG, "cellCol: "+cellCol);

        ArrayList<ImageView> row = imageAdapter.getRow(cellRow);
        ArrayList<ImageView> col = imageAdapter.getCol(cellCol);
        for (View element : col) {
            Log.i(TAG, "col-before: "+element.getTag());
        }

        int cellRowIndex = row.indexOf(view);
        int cellColIndex = col.indexOf(view);

        //TODO: getTag will be null not an int if empty cell...
        //TODO: if cell is first or last in group then trying to get the next/prev index in list wont work

        // initialise vars as clicked view - assignment fails if the cell is first/last in group when we check the index
        // above/below (out of arrays bounds)...these vars pass through the following operations with no effect
        ImageView prevInRow = setVar(view, row, -1, cellRowIndex);
        ImageView nextInRow = setVar(view, row, 1, cellRowIndex);
        ImageView prevInCol = setVar(view, col, -1, cellColIndex);
        ImageView nextInCol = setVar(view, col, 1, cellColIndex);

        // check neighbouring cells for empty image as indicated by tag
        int lastCell = gridSize*gridSize - 1;
        ImageView[] cells = {prevInRow, prevInCol, nextInRow, nextInCol};
        for (ImageView element : cells) {
           if ((int)element.getTag() == lastCell) {
               //swap clicked cells image to this cell leaving it with nothing
               Drawable image = imageView.getDrawable();
               int imageTag = (int)imageView.getTag();
               // set neighbour image and tag
               element.setImageDrawable(image);
               element.setTag(imageTag);
               // set clicked image and tag
               imageView.setImageDrawable(null);
               imageView.setTag(lastCell);
               break;
           }
        }

        for (View element : col) {
            Log.i(TAG, "col-after: "+element.getTag());
        }

        //TODO: testing alternate method...need grid size and grid
        // get empty cell by checking all grid, then check if that cell is direct neighbour of clicked cell
//        int gridSize = 4;
//        for (int x=0; x<gridSize; x++) { // check all cells in the grid and return the cell with the empty image
//            ImageView imageView = (ImageView)gridView.getItemAtPosition(x);
//            Object viewTag = imageView.getTag();
//            if (viewTag == null) {
//                return imageView;
//            }
//        }

    }

//    @Override
//    public boolean onTouchEvent(MotionEvent event) {
//        // passes touch events to the gesture detector
//        this.mDetector.onTouchEvent(event);
//        return super.onTouchEvent(event);
//    }

    private VelocityTracker mVelocityTracker = null;

    private float xDown, yDown;

    private View.OnTouchListener swipeListener = new View.OnTouchListener() {
        @Override
        public boolean onTouch(View v, MotionEvent event) {
            Log.i(TAG, "onTouch-view: "+v.getTag());

            //TODO: attempt at getting velocity of event in view touch and determine a swipe before it has left the view
            //  in which case the event doesnt register as a fling, so i cant process it....becomes a scroll which is called
            //  more than once so also bad for processing as mutiple blocks get called...doesnt matter in my case as
            //  empty cell would move into the pressed cell on first call so subsequent doesnt do anything but is bad
            //  practice... and shouldnt have to resort to that...
            int action = event.getActionMasked();
            int index = event.getActionIndex();
            int pointerId = event.getPointerId(index);

            final int DISTANCE_THRESHOLD = dpToPx(11);  // ~1/3 the cell size
            final int VELOCITY_THRESHOLD = 150;

            float xVelocity, xCancel, xDiff, yVelocity, yCancel, yDiff;
            switch (action) {
                case MotionEvent.ACTION_DOWN:
                    Log.i(TAG, "onDown");
                    if (mVelocityTracker == null) {
                        mVelocityTracker = VelocityTracker.obtain();
                    } else {
                        mVelocityTracker.clear();
                    }
                    mVelocityTracker.addMovement(event);
                    xDown = event.getRawX();
                    yDown = event.getRawY();
                    Log.i(TAG, "xDown: "+xDown+" yDown: "+yDown);
                    break;
                case MotionEvent.ACTION_MOVE:
                    Log.i(TAG, "onMove");
                    mVelocityTracker.addMovement(event);
                    mVelocityTracker.computeCurrentVelocity(1000);
                    xVelocity = mVelocityTracker.getXVelocity(pointerId);
                    yVelocity = mVelocityTracker.getYVelocity(pointerId);
                    float xMove = event.getRawX();
                    float yMove = event.getRawY();
                    Log.i(TAG, "xMove: "+xMove+" yMove: "+yMove);
                    Log.i(TAG, "xVel: "+xVelocity);
                    Log.i(TAG, "yVel: "+yVelocity);
                    break;
                case MotionEvent.ACTION_UP:
                    Log.i(TAG, "onUp");
                case MotionEvent.ACTION_CANCEL:

                    //TODO: why is action cancel called so often and longer holds are not able to be completed if moving
                    //  consider ontouchevent for the activity so i can process a full swipe... or try another method..??
                    mVelocityTracker.addMovement(event);
                    mVelocityTracker.computeCurrentVelocity(1000);
                    Log.i(TAG, "onCancel");
                    xCancel = event.getRawX();
                    yCancel = event.getRawY();
                    xDiff = xCancel - xDown;
                    yDiff = yCancel - yDown;
                    xVelocity = mVelocityTracker.getXVelocity(pointerId);
                    yVelocity = mVelocityTracker.getYVelocity(pointerId);
                    Log.i(TAG, "xDown: "+xDown+" xCancel: "+xCancel+" yDown: "+yDown+" yCancel: "+yCancel);
                    Log.i(TAG, "diffX: "+xDiff+" diffY: "+yDiff+" velX: "+xVelocity+" velY: "+yVelocity);

                    if (Math.abs(xDiff) > Math.abs(yDiff)) {  // potential horizontal swipe - check distance and velocity
                        if (Math.abs(xDiff) > DISTANCE_THRESHOLD && Math.abs(xVelocity) > VELOCITY_THRESHOLD) {
                            if (xDiff > 0) {  // right swipe
                                //TODO: right swipe
                                Log.i(TAG, "rightSwipe ");
                            } else {  // left swipe
                                //TODO: left swipe
                                Log.i(TAG, "leftSwipe");
                            }
                        } else {
                            v.performClick();
                        }
                    } else {
                        if (Math.abs(xDiff) < Math.abs(yDiff)) {  // potential vertical swipe
                            if (Math.abs(yDiff) > DISTANCE_THRESHOLD && Math.abs(yVelocity) > VELOCITY_THRESHOLD) {
                                if (yDiff > 0) {  // down swipe
                                    //TODO: down swipe
                                    Log.i(TAG, "downSwipe");
                                } else {  // up swipe
                                    //TODO: up swipe
                                    Log.i(TAG, "upSwipe");
                                }
                            } else {
                                v.performClick();
                            }
                        } else {
                            //TODO: get click to work..
                            v.performClick();  //TODO: this is if swipe isnt found to have happened by set criteria
                        }
                    }

                    mVelocityTracker.recycle();
                    mVelocityTracker = null;
                    break;
                case MotionEvent.ACTION_OUTSIDE:
                    Log.i(TAG, "onOutside: ");
            }

            return true;
//            return mDetector.onTouchEvent(event);
        }
    };

    class MyGestureListener extends GestureDetector.SimpleOnGestureListener {
        // handle specific touch events only
        private static final String TAG = "gestures";
        private final int DISTANCE_THRESHOLD = dpToPx(40);
        private final int VELOCITY_THRESHOLD = 150;

        @Override
        public boolean onDown(MotionEvent e) {
            // return true for any down touch so we can check all gestures
            Log.i(TAG, "onDown:");
            return true;
        }

        @Override
        public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {

            boolean result = false;
            //TODO: cant get this method to work i dont think...@@@@@@@@@@@@@@@@@@@
            //TODO: check 1st event X,Y to see if its in a view then proceed to process else...return false
//            if (e1.getX() != ) {
//                return result;
//            } else {
//                //TODO: proceed to process swipe on the view
//            }

            float diffX = e2.getX() - e1.getX();
            float diffY = e2.getY() - e1.getY();
            Log.i(TAG, "onFling:"+"diffX: "+diffX+"diffY: "+diffY+"velX: "+velocityX+"velY: "+velocityY);
            if (Math.abs(diffX) > Math.abs(diffY)) {  // potential horizontal swipe - check distance and velocity
                if (Math.abs(diffX) > DISTANCE_THRESHOLD && Math.abs(velocityX) > VELOCITY_THRESHOLD) {
                    if (diffX > 0) {  // right swipe
                        //TODO: right swipe
                        Log.i(TAG, "rightSwipe ");
                    } else {  // left swipe
                        //TODO: left swipe
                        Log.i(TAG, "leftSwipe");
                    }
                    result = true;  // confirmed swipe
                }
            } else {
                if (Math.abs(diffX) < Math.abs(diffY)) {  // potential vertical swipe
                    if (Math.abs(diffY) > DISTANCE_THRESHOLD && Math.abs(velocityY) > VELOCITY_THRESHOLD) {
                        if (diffY > 0) {  // down swipe
                            //TODO: down swipe
                            Log.i(TAG, "downSwipe");
                        } else {  // up swipe
                            //TODO: up swipe
                            Log.i(TAG, "upSwipe");
                        }
                    }
                    result = true;  // confirmed swipe
                }
            }
            //TODO: handle slow or short swipes as clicks?? is the case if they fall through to here
            return result;  // no swipe
        }

        @Override
        public boolean onSingleTapUp(MotionEvent e) {
            //TODO: onclick here
            Log.i(TAG, "onSingleTapUp:");
            return super.onSingleTapUp(e);
        }

        @Override
        public boolean onDoubleTap(MotionEvent e) {
            Log.i(TAG, "onDoubleTap:");
            return super.onDoubleTap(e);
        }

        @Override
        public void onLongPress(MotionEvent e) {
            Log.i(TAG, "onLongPress:");
            super.onLongPress(e);
        }

        @Override
        public boolean onContextClick(MotionEvent e) {
            Log.i(TAG, "onContextClick:");
            return super.onContextClick(e);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_game_test);

        mDetector = new GestureDetectorCompat(this, new MyGestureListener());

//        // 4x4 grid 300dp x 300dp
//        final Bitmap bmp = BitmapFactory.decodeResource(getResources(), R.drawable.grid15);
//
//        // TODO: make the widths/heights changeable based on what type of grid (m x n) is selected
//        final Bitmap testBmp = BitmapFactory.decodeResource(getResources(), R.drawable.cropgrid);
//        final Bitmap croppedBmp = Bitmap.createBitmap(testBmp, 0, 0, 412, 412);
//        // size of each
//        int size = dpToPx(51, this);
//        // create x and y starting pixel values for cropping bitmap to create grid images
//        int xpos;
//        int ypos;

        final GridView gridView = findViewById(R.id.puzzlegrid);
        //TODO: set number of columns based on what type of grid chosen
        final int numCols = getIntent().getIntExtra("numColumns", 4);
        int gridBitmap = getIntent().getIntExtra("drawableId", R.drawable.grid15);

        Log.i(TAG, "columns: "+numCols);
        gridView.setNumColumns(numCols);
        // 300dp width but could get increased or decreased based on screen??
        int gridWidth = gridView.getLayoutParams().width;
        Log.i("gridWidth", "onCreate: "+gridWidth);
        createBitmapGrid(gridBitmap, numCols, numCols);

        final ImageAdapter imageAdapter = new ImageAdapter(this, bitmaps, gridWidth, swipeListener);
        gridView.setAdapter(imageAdapter);

//        gridView.onInterceptTouchEvent()
        for (int x=0; x<16; x++) {
            final int pos = x;
            ImageView cell = (ImageView)gridView.getItemAtPosition(x);
            cell.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if ((int)view.getTag() != numCols*numCols-1) {
                        //TODO: fix the code in this function...can be tidied up /changed
                        moveCell(view, pos, imageAdapter);
                        if (gridCorrect(gridView)) {
                            // emoticons: 😃 😁 😄 😎 😊 ☻ 👍 🖒 ☜ ☞
                            Toast gameWin = Toast.makeText(getApplicationContext(), "Correct \uD83D\uDE0E", Toast.LENGTH_LONG);
                            gameWin.show();
                        }
                        //TODO: toast for gamewin
                    }  // else is empty cell and does nothing on click
                }
            });
        }

//        gridView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
//            @Override
//            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
//
////                view.onTouchEvent()
//
////                ImageView imageView = (ImageView)view;
//                if ((int)view.getTag() != numCols*numCols-1) {
//                    //TODO: fix the code in this function...can be tidied up /changed
//                    moveCell(view, position, imageAdapter);
//                    if (gridCorrect(gridView)) {
//                        // emoticons: 😃 😁 😄 😎 😊 ☻ 👍 🖒 ☜ ☞
//                        Toast gameWin = Toast.makeText(getApplicationContext(), "Correct \uD83D\uDE0E", Toast.LENGTH_LONG);
//                        gameWin.show();
//                    }
//                    //TODO: toast for gamewin
//                }  // else is empty cell and does nothing on click
//            }
//
//        });

    }

}
