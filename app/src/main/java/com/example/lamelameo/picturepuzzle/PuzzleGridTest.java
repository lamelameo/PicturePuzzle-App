package com.example.lamelameo.picturepuzzle;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.*;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.GridLayout;
import android.util.Log;
import android.view.*;
import android.widget.*;

import java.io.*;
import java.util.*;
import java.util.concurrent.*;

public class PuzzleGridTest extends AppCompatActivity implements PauseMenu.OnFragmentInteractionListener {

    private static final String TAG = "PuzzleGridTest";
    private ArrayList<Drawable> bitmaps = new ArrayList<>();
    private ArrayList<ArrayList<ImageView>> cellRows, cellCols;
    private ArrayList<ImageView> gridCells;
    private int emptyCellIndex;
    private VelocityTracker mVelocityTracker = null;
    private float xDown, yDown;
    private int numRows;
    private int timerCount;
    private int puzzleNum;
    private int numMoves;
    private ArrayList<String> savedDataList = new ArrayList<>();
    private TextView moveCounter;
    private boolean gamePaused = false;
    private Runnable timerRunnable;
    private PauseMenu pauseMenu;
    private int[] bestData;
    private boolean newBestData;

    /**
     * Retrieve data from main activity, and create the images for the grid cells using the given image and grid size.
     * Create a randomised list of indexes to randomise the image grid, and load the cell images to the grid according to
     * the randomised order. Add cell tags for tracking of the images as they are moved by user. Set cell onClickListener
     * to allow user to click or swipe cells to move the images, and add pause button listener, to open pause UI.
     * @param savedInstanceState previously saved instance of the activity
     */
    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_puzzle_grid_test);

        // ~~~ set up the puzzle grid ~~~

        // first get relevant chosen settings from main activity
        final GridLayout puzzleGrid = findViewById(R.id.gridLayout);
        final int numCols = getIntent().getIntExtra("numColumns", 4);
        numRows = numCols;
        int gridSize = puzzleGrid.getLayoutParams().width;
        puzzleNum = getIntent().getIntExtra("puzzleNum", 0);
        String photoPath = getIntent().getStringExtra("photoPath");

        // create puzzle piece bitmaps using the given image and add to bitmaps list
        Bitmap bmp;
        if (photoPath == null) {  // no photo taken, so use the selected app photo, or appropriate default image
            String appPhotoPath = getIntent().getStringExtra("appPhotoPath");
            if (appPhotoPath != null) {  // selected an app photo
                bmp = scalePhoto(gridSize, appPhotoPath);
            } else {  // selected a default image, or no selection
                int gridBitmap = getIntent().getIntExtra("drawableId", R.drawable.dfdfdefaultgrid);
                bmp = BitmapFactory.decodeResource(getResources(), gridBitmap);
            }
        } else {  // have taken a photo - so use it for the image
            bmp = scalePhoto(gridSize, photoPath);
        }
        int imageSize = bmp.getWidth();
        createBitmapGrid(bmp, numCols, numCols, imageSize);

        // initialise grid settings
        puzzleGrid.setColumnCount(numCols);
        puzzleGrid.setRowCount(numCols);
        emptyCellIndex = numCols*numCols-1;
        int gridWidth = puzzleGrid.getLayoutParams().width;  //TODO: could change this based on screen?

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

        // add cells to grid and set their now randomised images
        //TODO: allow for m x n sized grids?
        for (int x=0; x<numCols; x++) {
            for (int y=0; y<numCols; y++) {
                final int index = x*numCols + y;
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
                } else {  // set all other cells with a randomised image excluding the last cells image as it must be empty
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

        // show the saved best time and move data for the given puzzle
        TextView bestTimeView = findViewById(R.id.bestTimeView);
        bestData = puzzleBestData();
        if (bestData[0] != -1) {
            int secs = bestData[0] % 60;
            int mins = bestData[0] / 60;
            int bestMoves = bestData[1];
            bestTimeView.setText(String.format(Locale.getDefault(), "Best Time: %02d:%02d\nBest Moves: %d",
                                 mins, secs, bestMoves));
        }

        // initialise game timer and its runnable
        moveCounter = findViewById(R.id.moveCounter);
        final TextView timer = findViewById(R.id.gameTimer);
        timerCount = 0;
        // Create runnable task (calls code in new thread) which increments a counter used as the timers text
        timerRunnable = new Runnable() {
            @Override
            public void run() {
                // update timer every second, keeping track of current system time
                // this way we can resume the timer after a pause mid tick, keeping accurate time
                prevTickTime = SystemClock.uptimeMillis();
                timerCount += 1;
                int seconds = timerCount % 60;
                int minutes = timerCount / 60;  // rounds down the decimal if we use int
                timer.setText(String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds));
            }
        };

        // initialise pause button and pause menu fragment
        pauseMenu = PauseMenu.newInstance();
        ImageButton pauseButton = findViewById(R.id.pauseButton);
        pauseButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // pause the timer and open pause menu if game has started
                if (timerCount != 0) {
                    pauseTimer();
                    pauseFragment();
                } else {
                    String toastText = "You have not started the puzzle!";
                    Toast toast = Toast.makeText(getApplicationContext(), toastText, Toast.LENGTH_SHORT);
                    toast.show();
                }

            }
        });

    }

    /**
     * Called if user clicks New Puzzle button in pause UI. Finish activity to move back to the main activity,
     * to let the user choose a new puzzle.
     */
    @Override
    public void onClickNewPuzzle() {
        finish();
    }

    /**
     * Called if user clicks Resume button in pause UI. Removes the pause fragment UI and resumes the game timer.
     */
    @Override
    public void onClickResume() {
        pauseFragment();
        if (timerCount != 0) {  // resume timer only if it has been started already
            startTimer();
        }
    }

    /**
     * Handles device back button presses considering two cases: pause fragment open or closed. If open, closes the
     * the fragment and resumes the game timer. If closed, the game UI is open, so finish this activity, to navigate
     * back to the main activity.
     */
    @Override
    public void onBackPressed() {
        super.onBackPressed();
        if (gamePaused) {  // fragment is present - remove it, start timer if game started
            pauseFragment();
            if (timerCount != 0) {
                startTimer();
            }
        } else {  // fragment is not open - go back to main activity
            finish();
        }
    }

    /**
     * Handles adding or removing the pause menu fragment from he game activity. If the game is paused when called,
     * remove fragment and make its container invisible and unclickable. If game is unpaused do the opposite actions.
     */
    private void pauseFragment() {

        LinearLayout pauseContainer = findViewById(R.id.pauseContainer);
        FragmentTransaction fragmentTrans = getSupportFragmentManager().beginTransaction();
        if (gamePaused) {
            fragmentTrans.remove(pauseMenu);
            pauseContainer.setVisibility(View.INVISIBLE);
            pauseContainer.setClickable(false);
        } else {
            fragmentTrans.add(R.id.pauseContainer, pauseMenu);
            pauseContainer.setVisibility(View.VISIBLE);
            pauseContainer.setClickable(true);
        }
        gamePaused = !gamePaused;
        fragmentTrans.addToBackStack(null);  //TODO: needed?
        fragmentTrans.commit();
    }

//    private long startTime;
    private long tickRemainder = 0;
    private long prevTickTime = 0;

    private ScheduledThreadPoolExecutor timerExecutor = new ScheduledThreadPoolExecutor(1);
    private ScheduledFuture<?> timerFuture;

    /**
     * A method to post a delayed runnable task every 1000 ms which updates the timer TextView by 1 second.
     * ScheduledThreadPoolExecutor is used to schedule the runnables in an indefinite sequence which is cancelled
     * upon game pause or finish. The first runnable starts after a delay value given by {@link #tickRemainder} which
     * is 0 upon first starting the game, or the remaining time until the next tick, calculated after a game pause.
     */
    private void startTimer() {
        // save the system time for clock start to determine the delay for resumes
//        startTime = SystemClock.uptimeMillis() + tickRemainder;
        // start runnable with a delay of 1 second, with initial delay as remaining milliseconds to complete the tick before pause
        timerFuture = timerExecutor.scheduleAtFixedRate(timerRunnable, tickRemainder, 1000, TimeUnit.MILLISECONDS);
    }

    /**
     * Method to stop the game timer by cancelling the sequence of delayed runnable tasks created using a
     * ScheduledThreadPoolExecutor which are responsible for incrementing the timer and updating the TextView.
     * The time remaining until the next timer tick is calculated and stored as {@link #tickRemainder} to be used
     * as the first delay for the timer if it is resumed, to keep the timer accurate. This is calculated from the system
     * time at the previous tick stored in {@link #prevTickTime} each clock tick and the current system time upon call.
     */
    private void pauseTimer() {
        // determine how close to next timer tick (next second) we are (in milliseconds)
        long pauseTime = SystemClock.uptimeMillis();
        long elapsedSincePrevTick = pauseTime - prevTickTime;
        tickRemainder = 1000 - elapsedSincePrevTick;

        //TODO: alternate method:
        //convert to string to take only last 3 integer values (take sub second values)
//        long elapsedTime = pauseTime - startTime;
//        String remainderStr = String.valueOf(elapsedTime);
//        String remainderHundreds = remainderStr.substring(remainderStr.length() - 3);
//        long remainder = 1000 - Integer.valueOf(remainderHundreds);

        // stop scheduled tasks
        if (timerFuture != null) {
            timerFuture.cancel(false);
        }
    }

    /**
     * Pauses the game timer if the game has been started but the pause menu is not open. If either of these conditions
     * are not met, do not call {@link #pauseTimer()} as instance variables are updated in this method which affect the timer.
     * Used for instances where the app loses focus on the device but the user did not manually pause the game.
     */
    @Override
    protected void onPause() {
        super.onPause();
        Log.i(TAG, "onPause: ");
        // pause the game timer if it is currently running
        if (!gamePaused && timerCount != 0) {
            pauseTimer();
        }
    }

    /**
     * Open the pause UI if onPause was called while the game was running. Used for instances such as app moving to background
     * without the user manually pausing. Take no action if the timer had not started, or game was paused already.
     */
    @Override
    protected void onResume() {
        super.onResume();
        Log.i(TAG, "onResume: ");
        // automatically open pause fragment if the game was running when onPause was called
        if (timerCount != 0 && !gamePaused) {
            pauseFragment();
        }
    }

    int getEmptyCellIndex() {
        return emptyCellIndex;
    }

    int getNumRows()  {
        return numRows;
    }

    ArrayList<ImageView> getCellRow(int index) {
        return cellRows.get(index);
    }

    ArrayList<ImageView> getCellCol(int index) {
        return cellCols.get(index);
    }

    /**
     * Called anytime the game activity is created - on the first occasion it creates a file in the apps directory
     * to save the following game data: lowest amount of time and moves taken to complete each puzzle
     * If the file has already been created, then search the file for the saved data for the current puzzle and return it
     * (if any) else return placeholder values to signify no data is available
     * @return an array[2] containing an int for both the saved time and moves data
     */
    private int[] puzzleBestData() {

        FileInputStream saveTimesFile = null;
        int[] savedData = {-1, -1};
        StringBuilder stringBuilder = new StringBuilder();
        //TODO: support for all sized default grids...
        String[] puzzleStrings = {"defaultgrid: ", "carpet: ", "cat: ", "clock: ", "crab: ",
                "darklights: ", "nendou: ", "razer: ", "saiki: ", "mms: "};

        Log.i(TAG, "puzzleNum: "+puzzleNum);
        try {  // if file already created, read file and get the relevant time, append lines to an array for easy access
            saveTimesFile = openFileInput("gametimes");
            BufferedReader reader = new BufferedReader(new InputStreamReader(saveTimesFile));
            String line;
            int currentLine = 0;
            if (puzzleNum == -1) {
                //TODO: add support for photos taken by the app...change puzzleNum for these, and add lines in file as needed
                return savedData;
            }
            // loop through lines till we get the puzzle we are looking for and get the saved data from that line
            while ((line = reader.readLine()) != null) {
                Log.i(TAG, "savefile line: " + line);
                // NOTE: only call method once or this will make the savedDataList have twice the entries, causing bugs
                savedDataList.add(line);
                if (currentLine == puzzleNum) {
                    int timeStartIndex = line.indexOf(":") + 2;
                    int timeEndIndex = line.indexOf(",");  // indexOf will give -1 if not found ie no data for the puzzle
                    if (timeEndIndex != -1) {  // if there is saved data then get it
                        savedData[0] = Integer.valueOf(line.substring(timeStartIndex, timeEndIndex));
                        savedData[1] = Integer.valueOf(line.substring(timeEndIndex+1));
                    }
                }
                currentLine += 1;
            }

        } catch (Exception readException) {
            readException.printStackTrace();
            // create the file if there is none already
            if (readException instanceof FileNotFoundException) {
                for (String element : puzzleStrings) {
                    stringBuilder.append(element).append("\n");
                    savedDataList.add(element);
                }
                // string builder contains a single string separated by newlines with blank save data
                String fileContents = stringBuilder.toString();
                // create file containing the string builder string
                FileOutputStream outputStream = null;
                try {
                    outputStream = openFileOutput("gametimes", Context.MODE_PRIVATE);
                    outputStream.write(fileContents.getBytes());
                } catch (Exception writeError) {
                    Log.i(TAG, "write exception: "+writeError);
                } finally {  //TODO: need finally block to close outputstream?
                    if (outputStream != null) {
                        try {
                            outputStream.close();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }
            }

        } finally {  // close the file
            try {
                if (saveTimesFile != null) {
                    saveTimesFile.close();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        return savedData;
    }

    /**
     * Called when a puzzle is successfully solved, compares the time and moves taken to complete the puzzle
     *  to the saved data for the lowest value of each of these taken to complete the same puzzle (if any)
     *  updates the saved data file if either (or both) value(s) for the completed puzzle is lower than the saved data
     * @param gameData array[2] containing an int for the amount of time (seconds) and moves to complete the puzzle
     */
    private void saveGameData(int[] gameData) {
        //TODO: support for different sized grid times, also different saves for default grids not just bundled
        String gameTime = Integer.toString(gameData[0]);
        String gameMoves = Integer.toString(gameData[1]);
        StringBuilder stringBuilder = new StringBuilder();
        //TODO: add support for photos taken by the app...change puzzleNum for these, and add lines in file as needed
        if (puzzleNum == -1) {
            return;
        }
        // set or initialise variables
        newBestData = false;
        String savedData = savedDataList.get(puzzleNum);
        String newData = "";
        int timeStartIndex = savedData.indexOf(":") + 2;
        int timeEndIndex = savedData.indexOf(",");

        String puzzleIdentifier = savedData.substring(0, timeStartIndex);
        if (timeEndIndex == -1) {  // if there is no saved data then add the game data
            newData = puzzleIdentifier + gameTime + "," + gameMoves;
        } else {  // there is saved data, so check if game time/moves and update if either are lower
            String timeString = savedData.substring(timeStartIndex, timeEndIndex);
            String moveString = savedData.substring(timeEndIndex+1);
            // time and moves are lower than saved values, so update
            if (gameData[0] < Integer.valueOf(timeString) && gameData[1] < Integer.valueOf(moveString)) {
                newData = puzzleIdentifier + gameTime + "," + gameMoves;
            }  // update time only
            if (gameData[0] < Integer.valueOf(timeString) && gameData[1] > Integer.valueOf(moveString)) {
                newData = puzzleIdentifier + gameTime + "," + moveString;
            }  // update moves only
            if (gameData[0] > Integer.valueOf(timeString) && gameData[1] < Integer.valueOf(moveString)) {
                newData = puzzleIdentifier + timeString + "," + gameMoves;
            }
        }
        // if newdata is changed, then we have to update the data file
        if (!newData.equals("")) {
            newBestData = true;  // signal that a new best has been achieved
            // update the relevant item in the data list, with the new string
            savedDataList.set(puzzleNum, newData);
            // use string builder to concatenate all strings
            for (String element : savedDataList) {
                Log.i(TAG, "saveGameData listLine: "+element);
                stringBuilder.append(element).append("\n");
            }
            String fileContents = stringBuilder.toString();
            Log.i(TAG, "saveGameData fileContents: "+fileContents);
            // overwrite the old file to contain the updated data
            FileOutputStream outputStream = null;
            try {
                outputStream = openFileOutput("gametimes", Context.MODE_PRIVATE);
                outputStream.write(fileContents.getBytes());
            } catch (Exception writeError) {
                Log.i(TAG, "write exception: "+writeError);
            } finally {
                if (outputStream != null) {
                    try {
                        outputStream.close();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    /**
     * Create a list containing cell position indexes (0-14) in a random order which can be used to arrange bitmaps
     * in the grid in this randomised order. This order of cells is checked for its solvability and returns the list
     * if it is, or repeats the process if not (ie. repeats until it is a solvable grid order).
     * @param numCols number of columns in the puzzle grid
     */
    private ArrayList<Integer> randomiseGrid(int numCols) {
        // initialise objects and set variables
        Random random = new Random();
        ArrayList<Integer> randomisedGrid = new ArrayList<>();
        ArrayList<Integer> posPool = new ArrayList<>();
        int gridSize = numCols*numCols;
        // list of ascending values from 0 - size of grid used for tracking values tested for inversions
//        ArrayList<Integer> unTestedValues = new ArrayList<>();

        while (true) {  // create randomised grid, check if solvable, then break if it is
            // initialise variables for start of each loop
            int bound = gridSize-1;  // bounds for random generator...between 0 (inclusive) and number (exclusive)
            randomisedGrid.clear();
            for (int x=0; x<gridSize-1; x++) {  // pool for random indexes to be drawn from - exclude last cell index
                posPool.add(x);
            }

            // randomise grid and create list with outcome
            for (int x=0; x<gridSize; x++) {
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

            Log.i(TAG, "randomiseGrid: inversions "+inversions);
            // if randomised grid is solvable then break the while loop and return that grid - else next loop creates new grid
            if (inversions%2 == 0) {  // empty cell always on bottom right so both odd and even size grids need even inversions
                break;
            }
        }
        return randomisedGrid;
    }

    /**
     * convert density independent pixels to pixels using the device's pixel density
     * @param dp amount to be converted from dp to px
     * @return the value converted to units of pixels as an integer (rounds down)
     */
    int dpToPx(float dp) {
        float density = getResources().getDisplayMetrics().density;
        long pixels = Math.round(dp * density);  // rounds up/down around 0.5
        return (int) pixels;
    }

    /**
     * Scale an image from a file path to a specific views size, and return as a Bitmap
     * Intended to be used for photos taken with a camera intent so images are by default in landscape
     * Therefore images are also rotated 90 degrees
     * @param viewSize size of the (pixels) view that the image is intended to be placed into
     * @param photopath file path of the image to be scaled
     * @return the scaled and rotated image as a Bitmap object
     */
    private Bitmap scalePhoto(int viewSize, String photopath) {
        // scale puzzle bitmap to fit the game grid view to save app memory/ prevent errors
        BitmapFactory.Options bmOptions = new BitmapFactory.Options();
        bmOptions.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(photopath, bmOptions);
        int photoW = bmOptions.outWidth;
        int photoH = bmOptions.outHeight;
        int scaleFactor = Math.min(photoW/viewSize, photoH/viewSize);
        bmOptions.inJustDecodeBounds = false;
        bmOptions.inSampleSize = scaleFactor;
        return BitmapFactory.decodeFile(photopath, bmOptions);
    }


    /**
     * create the grid of smaller cell bitmaps using the chosen image and grid size and add them to the bitmaps list
     * @param bmp bitmap image to be used to create grid of images for the puzzle
     * @param rows number of rows to split the grid into
     * @param columns number of columns to split the grid into
     * @param imageSize size of the bitmap image (in pixels)
     */
    private void createBitmapGrid(Bitmap bmp, int rows, int columns, int imageSize) {
        // determine cell size in pixels from the image size and set amount of rows/cols
        int cellSize = imageSize/rows;
        // for each row loop 4 times creating a new cropped image from original bitmap and add to adapter dataset
        for(int x=0; x<columns; x++) {
            // for each row, increment y value to start the bitmap at
            float ypos = x*cellSize;
            for(int y=0; y<rows; y++) {
                // loop through 4 positions in row incrementing the x value to start bitmap at
                float xpos = y*cellSize;
                Bitmap gridImage = Bitmap.createBitmap(bmp, (int)xpos, (int)ypos, cellSize, cellSize);
                // converted to drawable for use of setImageDrawable to easily swap cell images
                Drawable drawable = new BitmapDrawable(getResources(), gridImage);
                bitmaps.add(drawable);
            }
        }
    }

    /**
     * Determine if grid is solved by iterating through all cells to check if the cell position matches the set image
     * cell tag[0] gives position, tag[1] is the image index, if they are the same then image is in correct cell
     * @return a boolean which indicates whether the grid is solved or not
     */
    private boolean gridSolved() {
        for (ImageView cell: gridCells) {
            // get the tags of each cell in the grid
            int[] cellTag = (int[])cell.getTag();
            if (cellTag[0] != cellTag[1]) {
                return false;
            }
        }
        return true;
    }

    /**
     * Handle single clicks on any cell other than the empty cell - these are filtered out in the touchListener.
     * Checks if the clicked cell is a direct neighbour of the empty cell.
     * If so then calls {@link #MoveCells} to handle movement of images/tags and checking if grid is solved.
     */
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

    /**
     * Called when puzzle is solved, it makes a screen wide layout clickable and visible to render the game UI frozen
     * and inflates a new layout UI into this. This overlay UI shows the game and saved data for the puzzle and contains
     * buttons to allow the user to navigate back to the main activity or restart the current puzzle.
     * @param gameData an array containing the time and moves data for the completed puzzle.
     */
    private void solvedPuzzleUI(int[] gameData) {
        // inflates a layout into the pause fragment container and makes the game activity unclickable
        final LinearLayout pauseContainer = findViewById(R.id.pauseContainer);
        LayoutInflater inflater = getLayoutInflater();
        inflater.inflate(R.layout.puzzle_solved_ui, pauseContainer, true);
        pauseContainer.setClickable(true);
        pauseContainer.setVisibility(View.VISIBLE);

        // set variables for the UI widgets
        TextView bestsView = findViewById(R.id.puzzleBests);
        TextView puzzleDataView = findViewById(R.id.puzzleDataView);
        Button retryButton = findViewById(R.id.retryButton);
        Button newButton = findViewById(R.id.newButton);

        //set onclick listeners for the UI buttons
        retryButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //TODO: randomise grid or reset to starting state?
                //  if reset puzzle without creating new activity instance, then must reset all data and variables
                //  eg. class vars: gamePaused, gameData, etc. also local vars in onCreate OR save the randomised grid
                //  and call oncreate
                recreate();

            }
        });
        newButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        // set text to be displayed - message, game data, and saved data
        String bestSecs;
        String bestMins;
        String bestMoves;
        // there is no saved data for the current puzzle
        if (bestData[0] == -1) {
            bestSecs = "";
            bestMins = "";
            bestMoves = "";
        } else {  // there is saved data
            bestSecs = String.valueOf(bestData[0] % 60);
            bestMins = String.valueOf(bestData[0] / 60);
            bestMoves = String.valueOf(bestData[1]);
        }
        int puzzleSecs = gameData[0] % 60;
        int puzzleMins = gameData[0] / 60;
        // emoticons: 😃 😁 😄 😎 😊 ☻ 👍 🖒 ☜ ☞
        puzzleDataView.setText(String.format(Locale.getDefault(), "Puzzle Solved \uD83D\uDE0E \n" +
                        " %02d m : %02d s\n %d moves",
                puzzleMins, puzzleSecs, gameData[1]));
        bestsView.setText(String.format(Locale.getDefault(), " Best Time: %s m : %s s \n Best Moves: %s",
                bestMins, bestSecs, bestMoves));

        // display a message if user achieved a new best
        if (newBestData) {
            Toast newBestToast = Toast.makeText(getApplicationContext(), "New Best \uD83D\uDC4D", Toast.LENGTH_LONG);
//            int toastPosition = (int)pauseContainer.getY();
            newBestToast.setGravity(Gravity.TOP, 0, 150);  // TODO: want it just above UI?
            newBestToast.show();
        }

    }

    /**
     * Called if a swipe in the valid direction occurs on a cell in the same group (row/column) of the empty cell or
     if a click occurs on a direct neighbour of the empty cell. Consecutively swaps the cell image and image tag for the
     empty cell with each cell in the group up to, and including, the touched cell. In effect, this shifts all cells from
     that touched until the empty cell by one grid position, leaving the empty cell in the touched cells original position.
     After this, the amount of moves for the puzzle is updated and {@link #gridSolved} is called to check if grid is solved.
     * @param groupMoves the amount of cell moves to handle with this call; minimum = 1, maximum = 5
     * @param emptyGroupIndex the index of the empty cell within the group that the touched cell is also an element
     * @param lastCell the index in the puzzle grid of the last cell
     * @param gridIndex the index of the touched cell within the puzzle grid
     * @param iterateSign value of +/- 1, used to iterate through the group in the correct direction depending on the swipe
     * @param group an array corresponding to a row or column of the puzzle grid, containing the cell ImageViews in that group
     */
    void MoveCells(int groupMoves, int emptyGroupIndex, int lastCell, int gridIndex, int iterateSign,
                   ArrayList<ImageView> group) {
        // start game timer on first move
        if (timerCount == 0) {
            startTimer();
        }
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
        }
        // update empty cell tracker to correspond to the new empty cell
        emptyCellIndex = gridIndex;
        // track amount of moves taken and update move counter to display this
        numMoves += groupMoves;
        moveCounter.setText(String.valueOf(numMoves));
        // check if grid is solved, if so then check if the game data was lower than the saved data (if any)
        int[] gameData = {timerCount, numMoves};
        if (gridSolved()) {
            pauseTimer();
            gamePaused = true;  // change this so onResume does not open pause fragment after a finished game
            //TODO: animation or wait between UI popup?
            saveGameData(gameData);
            solvedPuzzleUI(gameData);
        }
    }

    /**
     * Determines if a swiped cell is within the same row or column as the empty cell and if the swipe was
     * in the direction of the empty cell - if so then calls {@link #MoveCells} to process the valid swipe
     * @param view the swiped cell's view object
     * @param gridCols the amount of columns in the puzzle grid
     * @param direction the direction of the swipe: right = 1, left = 2, down = 3, up = 4
     */
    void SwipeCell(View view, int gridCols, int direction) {
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

    /**
     * Handle simple touch events as either a swipe (up/down/left/right) or a click - multi touch is not supported.
     * Swipe direction is determined by distance travelled in the x/y planes between touch/release and its release velocity.
     * Distance and velocity must be greater than DISTANCE_THRESHOLD and VELOCITY_THRESHOLD, respectively, to be valid.
     * An event that doesn't fit any of the set criteria is considered a click and handed to the onClick listener. */
    private View.OnTouchListener swipeListener = new View.OnTouchListener() {
        @Override
        public boolean onTouch(View v, MotionEvent event) {
            int[] tag = (int[])v.getTag();
            // consume touch with no action taken if empty cell is touched
            if (tag[0] == emptyCellIndex) {
                return true;
            }

            // get pointer id which identifies the touch...can handle multi touch events
            int action = event.getActionMasked();
            int index = event.getActionIndex();
            int pointerId = event.getPointerId(index);

            // if the pointer id isnt 0, a touch is currently being processed - ignore this new one to avoid crashes
            if (pointerId != 0) {
                Log.i(TAG, "pointer ID: "+pointerId);
                Log.i(TAG, "multi touch detected");
                return true;
            }

            // TODO: code from stackoverflow, can change, just is simple method to register anything resembling a swipe
            final int DISTANCE_THRESHOLD = dpToPx(11);  // ~1/3 the cell size
            final int VELOCITY_THRESHOLD = 200;  // TODO: change value if unhappy with sensitivity
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
                    // process swipes
                    if (Math.abs(xDiff) > Math.abs(yDiff)) {  // potential horizontal swipe - check distance and velocity
                        if (Math.abs(xDiff) > DISTANCE_THRESHOLD && Math.abs(xVelocity) > VELOCITY_THRESHOLD) {
                            if (xDiff > 0) {  // right swipe
                                SwipeCell(v, gridSize, 1);
                            } else {  // left swipe
                                SwipeCell(v, gridSize, 2);
                            }
                        } else {
                            v.performClick();
                        }
                    } else {
                        if (Math.abs(xDiff) < Math.abs(yDiff)) {  // potential vertical swipe
                            if (Math.abs(yDiff) > DISTANCE_THRESHOLD && Math.abs(yVelocity) > VELOCITY_THRESHOLD) {
                                if (yDiff > 0) {  // down swipe
                                    SwipeCell(v, gridSize, 3);
                                } else {  // up swipe
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
