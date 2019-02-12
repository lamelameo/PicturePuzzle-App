package com.example.lamelameo.picturepuzzle;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.SystemClock;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.GridLayout;
import android.util.Log;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;

import java.util.*;

public class PuzzleGridTest extends AppCompatActivity {

    private ArrayList<Drawable> bitmaps = new ArrayList<>();
    private String TAG = "PuzzleGridTest";
    private ArrayList<ArrayList<ImageView>> cellRows, cellCols;
    private ArrayList<ImageView> gridCells;
    private int emptyCellIndex;
    private VelocityTracker mVelocityTracker = null;
    private float xDown, yDown;
    private int numRows;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_puzzle_grid_test);

        final GridLayout puzzleGrid = findViewById(R.id.gridLayout);

        //TODO: create game timer
//        TimePicker timePicker = new TimePicker(this);
//        TextClock textClock = new TextClock(this);
//        // get uptime when timer starts then again every x amount of time?
//        long time = SystemClock.uptimeMillis();
//        Timer timer = new Timer();
////        timer.schedule();

        // get number of columns set
        final int numCols = getIntent().getIntExtra("numColumns", 4);
        numRows = numCols;
        int gridSize = puzzleGrid.getLayoutParams().width;
        // get photopath if taken
        String photoPath = getIntent().getStringExtra("photoPath");

        // create puzzle pieces using the given image and add to bitmaps list
        if (photoPath == null) {
            // should give a grid of scaled cells based on the given default square image
            int gridBitmap = getIntent().getIntExtra("drawableId", R.drawable.dfdfdefaultgrid);
            Bitmap bmp = BitmapFactory.decodeResource(getResources(), gridBitmap);
            float imageSize = bmp.getWidth();
            createBitmapGrid(bmp, numCols, numCols, imageSize);
        } else {
            //TODO: gives a final grid corresponding to the preview shown on main activity
            final Bitmap bmp2 = scalePhoto(gridSize, photoPath);
            createBitmapGrid(bmp2, numCols, numCols, gridSize);
        }

        puzzleGrid.setColumnCount(numCols);
        puzzleGrid.setRowCount(numCols);
        // initialise emptycell index tracker
        emptyCellIndex = numCols*numCols-1;
        // 300dp width but could get increased or decreased based on screen??
        int gridWidth = puzzleGrid.getLayoutParams().width;

        // initialise lists to hold grid objects
        gridCells = new ArrayList<>();
        cellRows = new ArrayList<>();
        cellCols =  new ArrayList<>();
        for (int x=0; x<numCols; x++) {
            cellRows.add(new ArrayList<ImageView>());
            cellCols.add(new ArrayList<ImageView>());
        }

        // create randomised grid list - contains indexes in random order which can be used to assign bitmaps to cells
        ArrayList<Integer> randomisedGrid = randomiseGrid(numCols);

        //TODO: allow for m x n sized grids?
        for (int x=0; x<numCols; x++) {
            for (int y=0; y<numCols; y++) {
                final int index = x*numCols + y;
                //TODO: add views to grid - change to custom view: puzzle_view ??
                ImageView gridCell = new ImageView(this);
                // set cell size based on size of grid
                int size = gridWidth/numCols;
                gridCell.setLayoutParams(new ViewGroup.LayoutParams(size, size));
                // add cell to grid
                puzzleGrid.addView(gridCell, index);
                //add cell to appropriate row/col lists
                gridCells.add(gridCell);
                cellRows.get(x).add(gridCell);
                cellCols.get(y).add(gridCell);

                // setting images and tags for cells
                if (index == bitmaps.size() - 1) {  // leave last cell with no image
                    int[] cellTag = {index, index};
                    gridCell.setTag(cellTag);
                    // set all other cells with a randomised image excluding the last cells image as it must be empty
                } else {  //
                    int rngBitmapIndex = randomisedGrid.get(index);
                    // set the cells starting image
                    gridCell.setImageDrawable(bitmaps.get(rngBitmapIndex));
                    //set cell tags corresponding to the cell position and set image for tracking/identification purposes
                    int[] cellTag = {index, rngBitmapIndex};
                    gridCell.setTag(cellTag);
                }

                // set click/touch listeners for cells
                //TODO: maybe use custom imageviews for cells as this warning appears about not overriding performclick..
                gridCell.setOnTouchListener(swipeListener);
                gridCell.setOnClickListener(cellClickListener);

            }
        }

    }

    /** create a list containing cell position indexes (0-14) in a random order which can be used to arrange bitmaps
     in the grid in this randomised order.
     * This order of cells checked for its solvability and returns the list if it is, or repeats the process if not */
    private ArrayList<Integer> randomiseGrid(int numCols) {
        // creates a randomised grid that is guaranteed to be solvable - empty cell is always bottom right
        Random random = new Random();
        ArrayList<Integer> randomisedGrid = new ArrayList<>();
        ArrayList<Integer> posPool = new ArrayList<>();
        int gridSize = numCols*numCols;
        // list of ascending values from 0 - size of grid used for tracking values tested for inversions
        ArrayList<Integer> unTestedValues = new ArrayList<>();

        // TODO: slow algorithm? test how fast it runs and change it if needed - takes 3ms for 1 while loop
        //  50% of states are solvable but we fix last cell? => x(1 + 1 + (n-1)(3) + n(2 + 6) + 1 + (n-1)(3 + n(6)/2) + 1 + 1)
        //     what % chance solvable? determines x                                       [n for sum nat nums = gridsize-1]

        while (true) {  // create randomised grid in while loop and only break if
            // initialise variables for start of each loop
            int bound = gridSize-1;  // bounds for random generator...between 0 (inclusive) and number (exclusive)
            randomisedGrid.clear();
            for (int x=0; x<gridSize-1; x++) {  // pool for random indexes to be drawn from - exclude last cell index
                posPool.add(x);
            }

            // randomise grid and create list with outcome
            for (int x=0; x<gridSize; x++) {
                unTestedValues.add(x);
                if (x == gridSize-1) {  // add last index to last in list to ensure it is empty
                    randomisedGrid.add(gridSize-1);
                } else {
                    int rngIndex = random.nextInt(bound);  // gets a randomised number within the pools bounds
                    int rngBmpIndex = posPool.get(rngIndex); // get the bitmap index from the pool using the randomised number
                    posPool.remove((Integer) rngBmpIndex);  // remove used number from the pool - use Integer else it takes as Arrayindex
                    bound -= 1;  // lower the bounds by 1 to match the new pool size so the next cycle can function properly
                    randomisedGrid.add(rngBmpIndex);  // add the randomised bmp index to the gridList
                }
            }

            // n=odd -> inversions: even = solvable
            // n=even -> empty cell on even row (from bottom: 1,2,3++ = 1 for bottom right) + inversions: odd = solvable
            //        -> empty cell on odd row + inversions: even = solvable
            // inversion: position pairs (a,b) where (list) index a < index b and (value) a > b have to check all

            int inversions = 0;
            for (int index=0; index<gridSize-1; index++) {  // test all grid cells for pairs with higher index cells
                int currentNum = randomisedGrid.get(index);
                for (int x=index+1; x<gridSize; x++) {  // find all pairs with higher index than current selected cell
                    int pairNum = randomisedGrid.get(x);  // get the next highest index cell
                    if (currentNum > pairNum) {  // add inversion if paired cell value is less than current cell value
                        inversions += 1;
                    }

                }
            }

            //TODO: alternate method to count inversions, faster or slower?
            // logn for search, n for remove, but n decreases each loop

//            // index of the current value being tested in the ordered values = how many lesser values have a greater
//            // index than it and therefore how many inversions there are for this value - if we remove tested values
//            // as we go, the order remains but possible inversions are removed
//            int inversions = 0;
//            for (int index=0; index<gridSize-1; index++) {  // test all grid cells for pairs with higher index cells
//                int currentNum = randomisedGrid.get(index);  // O(1) to access in array
//                int numInvs = unTestedValues.indexOf(currentNum);  // O(logn) to find value in sorted array
//                inversions += numInvs;
//                // remove current num before next loop, use its index so no search is needed
//                unTestedValues.remove(numInvs);  // O(n) to remove item at current index in sorted array
//            }
//            unTestedValues.remove(0);  // remove the last remaining value as we dont loop anymore

            Log.i(TAG, "randomiseGrid: inversions "+inversions);
            // if randomised grid is sovlable then break the while loop and return that grid - else next loop creates new grid
            if (inversions%2 == 0) {  // empty cell always on bottom right so both odd and even size grids need even inversions
                break;
            }
        }
        return randomisedGrid;
    }

    /** convert density independent pixels to pixels using the devices pixel density */
    private int dpToPx(float dp) {
        float density = getResources().getDisplayMetrics().density;
        // rounds up/down around 0.5
        long pixels = Math.round(dp * density);
        return (int) pixels;
    }

    /** Scale an image given a @photopath to a specific views size, @viewSize and return as a Bitmap
     * Intended to be used for photos taken with a camera intent so images are by default in landscape
     * Therefore images are also rotated 90 degrees */
    private Bitmap scalePhoto(int viewSize, String photopath) {
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
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
    }

    /** create the grid of smaller cell bitmaps using the chosen image and grid size and add them to the bitmaps list */
    private void createBitmapGrid(Bitmap bmp, int rows, int columns, float imageSize) {
        // determine cell size in pixels from the image size and set amount of rows/cols
        float cellSize = imageSize/rows;
        // for each row loop 4 times creating a new cropped image from original bitmap and add to adapter dataset
        for(int x=0; x<columns; x++) {
            // for each row, increment y value to start the bitmap at
            float ypos = x*cellSize;
            for(int y=0; y<rows; y++) {
                // loop through 4 positions in row incrementing the x value to start bitmap at
                float xpos = y*cellSize;
                Bitmap gridImage = Bitmap.createBitmap(bmp, (int)xpos, (int)ypos, (int)cellSize, (int)cellSize);
                // converted to drawable for use of setImageDrawable to easily swap cell images
                Drawable drawable = new BitmapDrawable(getResources(), gridImage);
                bitmaps.add(drawable);
            }
        }
    }

    /** determine if grid is solved by iterating through all cells to check if the cell position matches the set image
     cell tag[0] gives position, tag[1] is the image index, if they are the same then image is in correct cell
     displays a Toast message if the grid is solved */
    private boolean gridCorrect() {
        // TODO: left as boolean as can use the value for stopping the game and adding the last cell as animation
        // get grid size and iterate through all cells
        int numCells = gridCells.size();
        for (int x=0; x<numCells; x++) {
            // get the tags of each cell in the grid
            int[] cellTag = (int[])gridCells.get(x).getTag();
            Log.i(TAG, "gridCorrectCell: "+x+" tag: "+cellTag[1]);
            if (cellTag[0] != cellTag[1]) {
                return false;
            }
        }
        // emoticons: 😃 😁 😄 😎 😊 ☻ 👍 🖒 ☜ ☞
        Toast gameWin = Toast.makeText(getApplicationContext(), "Correct \uD83D\uDE0E", Toast.LENGTH_LONG);
        gameWin.show();
        return true;
    }

    /** Handle single clicks on any cell other than the empty cell - these are filtered out in the touchListener
     * checks if the clicked cell is a direct neighbour of the empty cell
     * if so then calls MoveCells to handle movement of images/tags and checking if grid is solved */
    private View.OnClickListener cellClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            ImageView cellImage = (ImageView)v;
            int[] cellTag = (int[])cellImage.getTag();
            int cellIndex = cellTag[0];
            int lastCell = numRows*numRows-1;
            // check if cell is in same row or column as empty cell
            int emptyCellRow = (int)Math.floor(emptyCellIndex/(float)numRows);
            int emptyCellCol = emptyCellIndex - emptyCellRow*numRows;
            int cellRow = (int)Math.floor(cellIndex/(float)numRows);
            int cellCol = cellIndex - cellRow*numRows;
            // determine distance and direction from the empty cell, opposite to the swipe (move) direction
            int cellsRowDiff = cellCol - emptyCellCol;  // left = -1, right = 1
            int cellsColDiff = cellRow - emptyCellRow;  // up = -1, down = 1
            // if cell is in same group then call movecells to make only one swap in the appropriate direction
            if (cellRow == emptyCellRow && cellsRowDiff*cellsRowDiff == 1) {  // if cell is left/right of empty
                MoveCells(1, emptyCellCol, lastCell, cellIndex, cellsRowDiff, cellRows.get(cellRow));
            }
            if (cellCol == emptyCellCol && cellsColDiff*cellsColDiff == 1) {  // if cell is up/down of empty
                MoveCells(1, emptyCellRow, lastCell, cellIndex, cellsColDiff, cellCols.get(cellCol));
            }
        }
    };

    /** called if a swipe in the valid direction occurs on a cell in same row/column of the empty cell or if a click
     registers on any cell other than the empty one
     * swaps cell images and image tags for all cells between that clicked (included) and empty cell
     * lastly calls gridcorrect to check if grid is solved */
    private void MoveCells(int groupMoves, int emptyGroupIndex, int lastCell, int gridIndex, int iterateSign,
                           ArrayList<ImageView> group) {
        // get the empty cell and the adjacent cell in a given group (row/col) then get the tags and image to be swapped
        for (int x = 0; x < groupMoves; x++) {  // incrementally swap cells from empty -> touched, in this order
            // adjacent cell to be swapped with empty has an index either +/- 1 from empty cells index in the group
            // increment - up/left, decrement - down/right, swapIndex is always the emptyCell index as we swap then loop
            int swapIndex = emptyGroupIndex + (iterateSign*x);
            ImageView swapCell = group.get(swapIndex + iterateSign);
            ImageView emptyCell = group.get(swapIndex);
            Drawable image = swapCell.getDrawable();
            int[] swapTag = (int[])swapCell.getTag();
            int[] emptyTag = (int[])emptyCell.getTag();
            // set empty cells new image and tag
            emptyCell.setImageDrawable(image);
            emptyTag[1] = swapTag[1];
            // set touched cells new image and tag
            swapCell.setImageDrawable(null);
            swapTag[1] = lastCell;  // use gridsize - 1 rather than empty cell tag as it was just changed
            // update empty cell tracker
            emptyCellIndex = gridIndex;
        }
        // check if grid is solved
        gridCorrect();
    }

    /** Determines if a swiped cell is within the same row or column as the empty cell and if the swipe was
        in the direction of the empty cell - if so then calls MoveCells to process the valid swipe */
    private void SwipeCell(View view, int gridCols, int direction) {
        // obtaining cell image, the row and column lists they are part of and their index in those lists
        int[] cellTag = (int[])view.getTag();
        int gridIndex = cellTag[0];
        int cellRow = (int)Math.floor(gridIndex/(float)gridCols);
        int cellCol = gridIndex - cellRow*gridCols;
        ArrayList<ImageView> row = cellRows.get(cellRow);
        ArrayList<ImageView> col = cellCols.get(cellCol);

        int lastCell = gridCols*gridCols - 1;
        int emptyCellRow = (int)Math.floor(emptyCellIndex/(float)gridCols);
        int emptyCellCol = emptyCellIndex - emptyCellRow*gridCols;
        int numRowMoves = Math.abs(emptyCellCol - cellCol);
        int numColMoves = Math.abs(emptyCellRow - cellRow);

        // check if empty and touched cells are in same row/col and if correct swipe direction
        switch (direction) {
            case(1):  // right swipe - empty row index > touched row index
                if (emptyCellRow == cellRow && emptyCellCol > cellCol) {  // cell columns give the index in row
                    MoveCells(numRowMoves, emptyCellCol, lastCell, gridIndex,-1, row);
                }
                break;
            case(2):  // left swipe - empty row index < touched row index
                if (emptyCellRow == cellRow && emptyCellCol < cellCol) {
                    MoveCells(numRowMoves, emptyCellCol, lastCell, gridIndex,1, row);
                }
                break;
            case(3):  // down swipe - empty col index > touched col index
                if (emptyCellCol == cellCol && emptyCellRow > cellRow) {  // cell rows give the index in column
                    MoveCells(numColMoves, emptyCellRow, lastCell, gridIndex,-1, col);
                }
                break;
            case(4):  // up swipe - empty col index < touched col index
                if (emptyCellCol == cellCol && emptyCellRow < cellRow) {
                    MoveCells(numColMoves, emptyCellRow, lastCell, gridIndex,1, col);
                }
                break;
        }
    }

    /** Handle simple touch events as either a swipe (up/down/left/right) or a click - multi touch is not supported.
     * Swipe direction is determined by distance travelled in the x/y planes between touch/release and its release velocity.
     * Distance and velocity must be greater than @DISTANCE_THRESHOLD and @VELOCITY_THRESHOLD, respectively, to be valid.
     * An event that doesn't fit any of the set criteria is considered a click and handed to the onClick listener. */
    private View.OnTouchListener swipeListener = new View.OnTouchListener() {
        @Override
        public boolean onTouch(View v, MotionEvent event) {
            int[] tag = (int[])v.getTag();
            // consume touch with no action taken if empty cell is touched
            if (tag[0] == emptyCellIndex) {
                Log.i(TAG, "emptyCellTouched");
                return true;
            }

            // TODO: multi finger touch.. think ive handled it but keeping jsut in case..
            // get pointer id which identifies the touch...can handle multi touch events
            int action = event.getActionMasked();
            int index = event.getActionIndex();
            int pointerId = event.getPointerId(index);
            Log.i(TAG, "pointer ID: "+pointerId);

            // if the pointer id isnt 0, a touch is currently being processed - ignore this new one to avoid crashes
            if (pointerId != 0) {
                Log.i(TAG, "multi touch detected");
                return true;
            }

            final int DISTANCE_THRESHOLD = dpToPx(11);  // ~1/3 the cell size
            final int VELOCITY_THRESHOLD = 200;  // TODO: BALANCE VALUE
            int gridSize = (int)Math.sqrt(gridCells.size());

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
                    break;
                case MotionEvent.ACTION_MOVE:
                    Log.i(TAG, "onMove: ");
                    mVelocityTracker.addMovement(event);
                    mVelocityTracker.computeCurrentVelocity(1000);
//                    xVelocity = mVelocityTracker.getXVelocity(pointerId);
//                    yVelocity = mVelocityTracker.getYVelocity(pointerId);
//                    float xMove = event.getRawX();
//                    float yMove = event.getRawY();
                    break;
                case MotionEvent.ACTION_UP:
                    Log.i(TAG, "onTouch: ");
                case MotionEvent.ACTION_OUTSIDE:
                    Log.i(TAG, "onOutside: ");
                case MotionEvent.ACTION_CANCEL:
                    Log.i(TAG, "onCancel");
                    mVelocityTracker.addMovement(event);
                    mVelocityTracker.computeCurrentVelocity(1000);
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
                                Log.i(TAG, "rightSwipe ");
                                SwipeCell(v, gridSize, 1);
                            } else {  // left swipe
                                Log.i(TAG, "leftSwipe");
                                SwipeCell(v, gridSize, 2);
                            }
                        } else {
                            v.performClick();
                        }
                    } else {
                        if (Math.abs(xDiff) < Math.abs(yDiff)) {  // potential vertical swipe
                            if (Math.abs(yDiff) > DISTANCE_THRESHOLD && Math.abs(yVelocity) > VELOCITY_THRESHOLD) {
                                if (yDiff > 0) {  // down swipe
                                    Log.i(TAG, "downSwipe");
                                    SwipeCell(v, gridSize, 3);
                                } else {  // up swipe
                                    Log.i(TAG, "upSwipe");
                                    SwipeCell(v, gridSize, 4);
                                }
                            } else {
                                v.performClick();
                            }
                        } else {  // if swipe isnt found to have happened by any of the set criteria
                            v.performClick();
                        }
                    }
                    // reset velocity tracker
                    mVelocityTracker.recycle();
                    mVelocityTracker = null;
                    break;
            }
            return true;
        }
    };
}
