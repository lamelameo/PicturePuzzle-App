package com.example.lamelameo.picturepuzzle;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.*;
import androidx.fragment.app.FragmentTransaction;
import androidx.appcompat.app.AppCompatActivity;
import androidx.gridlayout.widget.GridLayout;
import android.util.Log;
import android.view.*;
import android.widget.*;
import com.example.lamelameo.picturepuzzle.databinding.ActivityPuzzleBinding;
import com.example.lamelameo.picturepuzzle.databinding.PuzzleSolvedUiBinding;

import java.io.*;
import java.util.ArrayList;
import java.util.Locale;
import java.util.Random;
import java.util.concurrent.*;

public class PuzzleActivity extends AppCompatActivity implements PauseMenu.OnFragmentInteractionListener {

    private static final String TAG = "PuzzleActivity";
    private final ArrayList<Drawable> bitmaps = new ArrayList<>();
    private ArrayList<ArrayList<ImageView>> cellRows, cellCols;
    private ArrayList<ImageView> gridCells;
    private int emptyCellIndex, numRows, timerCount, puzzleNum, numMoves;
    private final ArrayList<String> savedDataList = new ArrayList<>();
    private boolean gamePaused = true, newBestData, hintShowing = false, wasGamePaused = true, gameSolved = false,
            returnMain = false;
    private Runnable timerRunnable;
    private PauseMenu pauseMenu;
    private int[] bestData;
    private ActivityPuzzleBinding mBinding;

    /**
     * Retrieve data from main activity, and create the images for the grid cells using the given image and grid size.
     * Create a randomised list of indexes to randomise the image grid, and load the cell images to the grid according to
     * the randomised order. Add cell tags for tracking of the images as they are moved by user. Set cell onClickListener
     * to allow user to click or swipe cells to move the images, and add pause button listener, to open pause UI.
     *
     * @param savedInstanceState previously saved instance of the activity
     */
    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mBinding = ActivityPuzzleBinding.inflate(getLayoutInflater());
        setContentView(mBinding.getRoot());
        // first get relevant chosen settings from main activity
        numRows = getIntent().getIntExtra("numColumns", 4);
        puzzleNum = getIntent().getIntExtra("puzzleNum", 0);
        // get binding and setup grid
        //TODO separate grid into custom layout with methods to initialise it
        initialiseGrid(mBinding.gridLayout);
        // create cell bitmaps using the given image then create cell objects and set the images to relevant cell
        Bitmap bmp;
        String photoPath = getIntent().getStringExtra("photoPath");
        if (photoPath == null) {  // default image
            bmp = BitmapFactory.decodeResource(getResources(),
                    getIntent().getIntExtra("drawableId", R.drawable.dfdfdefaultgrid));
        } else {  // given an image path
            bmp = scalePhoto(mBinding.gridLayout.getLayoutParams().width, photoPath);
        }
        createBitmapGrid(bmp, numRows, numRows);
        createPuzzleCells(mBinding.gridLayout);

        // show the saved best time and move data for the given puzzle
        //TODO: move saved data to database
        bestData = puzzleBestData();
        if (bestData[0] != -1) {
            int secs = bestData[0] % 60, mins = bestData[0] / 60, bestMoves = bestData[1];
            mBinding.bestsView.setText(String.format(Locale.getDefault(), "Best Time: %02d:%02d\nBest Moves: %d",
                    mins, secs, bestMoves));
        }

        // initialise game timer and its runnable
        timerCount = 0;

        // Create runnable task (calls code in new thread) which increments a counter used as the timers text
        timerRunnable = new Runnable() {
            @Override
            public void run() {
                // update timer every second, keeping track of current system time
                // this way we can resume the timer after a pause mid tick, keeping accurate time
                tickStartTime = SystemClock.uptimeMillis();
                tickElapsed = 0;
                timerCount += 1;
                int seconds = timerCount % 60;
                int minutes = timerCount / 60;  // rounds down the decimal if we use int
                mBinding.gameTimer.setText(String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds));
            }
        };

        // initialise pause button and pause menu fragment
        pauseMenu = PauseMenu.newInstance();
        mBinding.pauseButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // pause the timer and open pause menu if game has started
                //TODO: you cannot pause between 0-1s
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

        // Hint button makes original selected image visible over top of the puzzle grid to show cells solved order
        //TODO: one handler for hint and ticker which receive messages when tick happens or hint is to be removed
        mBinding.hintImage.setImageBitmap(bmp);
        // use handler to call a runnable to make the image invisible after 1 second (can change if needed)
        final Handler hintHandler = new Handler();
        final Runnable futureRunnable = new Runnable() {
            @Override
            public void run() {
                mBinding.hintImage.setVisibility(View.INVISIBLE);
                hintShowing = false;
                mBinding.hintImage.setClickable(false);
            }
        };
        mBinding.hintButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //TODO: extend timeout if hint is pressed whilst already shown?
                if (!hintShowing) {  // show hint only if isnt already showing to remove redundant calls
                    mBinding.hintImage.setVisibility(View.VISIBLE);
                    mBinding.hintImage.setClickable(true);
                    hintHandler.postDelayed(futureRunnable, 1000);  // set to remove image in 1 second
                }
            }
        });

        //TODO: can move to top of oncreate so no new objects are created?
        //TODO: could change fragment code to detect if rotate occurs in onPause, then dont open UI
        if (savedInstanceState != null) {
            // get saved data from saved instance
            timerCount = savedInstanceState.getInt("timer");
            tickElapsed = savedInstanceState.getLong("tickElapsed");
            numMoves = savedInstanceState.getInt("moves");
            gamePaused = savedInstanceState.getBoolean("paused");
            gameSolved = savedInstanceState.getBoolean("isSolved");
            setCellData(savedInstanceState.getIntegerArrayList("cellStates"));
            // Update move counter and timer if the game was started before rotation
            if (numMoves != 0) {
                mBinding.moveCounter.setText(String.valueOf(numMoves));
            }
            if (timerCount != 0) {
                mBinding.gameTimer.setText(String.format(Locale.getDefault(), "%02d:%02d",
                        timerCount / 60, timerCount % 60));
            }
            // handle previously set game state
            handlePause();
        }
    }

    private void initialiseGrid(GridLayout puzzleGrid) {
        // initialise grid settings
        puzzleGrid.setColumnCount(numRows);
        puzzleGrid.setRowCount(numRows);
        emptyCellIndex = numRows * numRows - 1;
        // initialise lists to hold grid objects
        gridCells = new ArrayList<>(numRows * numRows);
        cellRows = new ArrayList<>(numRows);
        cellCols = new ArrayList<>(numRows);
        for (int x = 0; x < numRows; x++) {
            cellRows.add(new ArrayList<ImageView>());
            cellCols.add(new ArrayList<ImageView>());
        }
    }

    private void updateTimer(TextView timer) {
        // update timer every second, keeping track of current system time
        // this way we can resume the timer after a pause mid tick, keeping accurate time
        timerCount += 1;
        int seconds = timerCount % 60;
        int minutes = timerCount / 60;  // rounds down the decimal if we use int
        timer.setText(String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds));

    }

    /**
     * Called if we have a saved instance state to handle the pause UI fragment opened before the activity is destroyed.
     * If the game was paused before activity was destroyed (such as by a screen orientation change), then the UI can be
     * made visible, else we want a resumed or completed game state with the corresponding UI.
     */
    private void handlePause() {
        // onPause called before onDestroy, so fragment will be present, must handle this depending if paused or not
        // replace pause UI fragment with newly created instance or it will cause issues...
        FragmentTransaction fragTrans = getSupportFragmentManager().beginTransaction();
        fragTrans.replace(R.id.pauseContainer, pauseMenu);
        // If pause UI was active, keep new instance and make it visible, else remove it and hide container
        // -- CAN change to hide/ show fragment? OR USE OLD FRAGMENT??
        if (!gamePaused || (timerCount == 0 && tickElapsed == 0) || gameSolved) {  // game had no pause UI active when rotated
            fragTrans.remove(pauseMenu);
            if ((timerCount + tickElapsed != 0) && !gameSolved) {  // game was running must resume timer
                startTimer();
            }
            if (gameSolved) {  // must inflate solved UI if solved, set appropriate text
                solvedPuzzleUI(bestData);
            }
        } else {  // app was in pause UI when rotated, must make it visible again
            mBinding.pauseContainer.setVisibility(View.VISIBLE);
            mBinding.pauseContainer.setClickable(true);
        }
        fragTrans.commit();
    }

    /**
     * Set images and tags for cell objects from a saved instance state
     * @param savedGrid ArrayList of integers corresponding to the saved cell states in ascending cell index order
     */
    private void setCellData(ArrayList<Integer> savedGrid) {
        // setting images and tags for cells
        int cellIndex = 0;
        for (ImageView cell : gridCells) {
            int cellState = savedGrid.get(cellIndex);
            int[] newTag = {cellIndex, cellState};
            cell.setTag(newTag);
            if (cellState != bitmaps.size() - 1) {
                cell.setImageDrawable(bitmaps.get(cellState));
            } else {
                emptyCellIndex = cellIndex;
                cell.setImageDrawable(null);
            }
            cellIndex += 1;
        }
    }

    /**
     * Create cell objects and set their images and tags according to a generated randomised state
     * @param puzzleGrid GridLayout which the cells are placed into
     */
    private void createPuzzleCells(GridLayout puzzleGrid) {
        // create randomised grid list - contains indexes in random order which can be used to assign bitmaps to cells
        ArrayList<Integer> randomisedGrid = randomiseGrid(numRows);
        // add cells to grid and set their now randomised images
        for (int index = 0; index < bitmaps.size(); index++) {
            // TODO: send settings to cell objects when created rather than doing manually?
            PuzzleCellView gridCell = new PuzzleCellView(this);
            int size = puzzleGrid.getLayoutParams().width / numRows;
            puzzleGrid.addView(gridCell, index, new ViewGroup.LayoutParams(size, size));
            //add cell to appropriate row/col lists
            gridCells.add(gridCell);
            cellRows.get(index / numRows).add(gridCell);
            cellCols.get(index % numRows).add(gridCell);
            // setting images and tags for cells
            if (index == bitmaps.size() - 1) {  // leave last cell with no image
                gridCell.setTag(new int[]{index, index});
            } else {  // set all other cells with a randomised image
                int rngBitmapIndex = randomisedGrid.get(index);
                gridCell.setImageDrawable(bitmaps.get(rngBitmapIndex));
                gridCell.setTag(new int[]{index, rngBitmapIndex});
            }
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        // Save game data...puzzle state, time/remainder, move count, best data, DONT redo onCreate??
        ArrayList<Integer> cellStates = new ArrayList<>(numRows * numRows);
        for (ImageView cell : gridCells) {
            int[] cellTag = (int[]) cell.getTag();
            cellStates.add(cellTag[1]);
        }
        outState.putIntegerArrayList("cellStates", cellStates);
        outState.putInt("timer", timerCount);
        outState.putLong("tickElapsed", tickElapsed);
        outState.putInt("moves", numMoves);
        outState.putBoolean("paused", wasGamePaused);
        outState.putBoolean("isSolved", gameSolved);

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        return super.onCreateOptionsMenu(menu);
        //TODO: customise menu bar
    }

    /**
     * Called if user clicks New Puzzle button in pause UI. Finish activity to move back to the previous activity,
     * to let the user choose a new puzzle.
     */
    @Override
    public void onClickNewPuzzle() {
        // TODO: have to close the pause fragment?
        finish();
    }

    /**
     * Called if user clicks Resume button in pause UI. Removes the pause fragment UI and resumes the game timer.
     */
    @Override
    public void onClickResume() {
        pauseFragment();
        if (timerCount + tickElapsed != 0) {  // resume timer only if it has been started already
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
        // BUG: back pressed when solved and rotated crashes app - pause rotate twice, press resume, then solved ->
        //  gives fragment + solved UI, then if rotate, fragment removed, then press back = BUG
        if (gamePaused && !gameSolved && (timerCount + tickElapsed != 0)) {  // pause fragment is open
            pauseFragment();
            startTimer();
        } else {  // fragment is not open - go back to main activity
            returnMain = true;
            finish();
        }
    }

    /**
     * Handles adding or removing the pause menu fragment from he game activity. If the game is paused when called,
     * remove fragment and make its container invisible and unclickable. If game is unpaused do the opposite actions.
     */
    private void pauseFragment() {
        FragmentTransaction fragmentTrans = getSupportFragmentManager().beginTransaction();
        if (gamePaused) {  // game was paused, unpause and remove UI fragment
            fragmentTrans.remove(pauseMenu);
            mBinding.pauseContainer.setVisibility(View.INVISIBLE);
            mBinding.pauseContainer.setClickable(false);
        } else {  // game was running, pause and open UI fragment
            fragmentTrans.replace(R.id.pauseContainer, pauseMenu);
            mBinding.pauseContainer.setVisibility(View.VISIBLE);
            mBinding.pauseContainer.setClickable(true);
        }
        gamePaused = !gamePaused;
        fragmentTrans.addToBackStack(null);  //TODO: needed?
        fragmentTrans.commit();
    }

    private long tickStartTime = 0, tickElapsed = 0;
    private final ScheduledThreadPoolExecutor timerExecutor = new ScheduledThreadPoolExecutor(1);
    private ScheduledFuture<?> timerFuture;

    /**
     * A method to post a delayed runnable task every 1000 ms which updates the timer TextView by 1 second.
     * ScheduledThreadPoolExecutor is used to schedule a runnable in an indefinite sequence which is cancelled upon
     * game pause or finish. Game pauses mid tick are handled by a delay which is calculated using {@link #tickElapsed} which
     * every call of {@link #pauseTimer()} which resets every tick completion.
     */
    private void startTimer() {
        // save the system time for clock start to determine the delay for repeated start/pauses
        tickStartTime = SystemClock.uptimeMillis();
        timerFuture = timerExecutor.scheduleAtFixedRate(timerRunnable, 1000 - tickElapsed, 1000, TimeUnit.MILLISECONDS);
    }

    /**
     * Method to pause the game timer by cancelling the ScheduledThreadPoolExecutor which handles scheduling runnables
     * responsible for incrementing the timer and updating the TextView. The time elapsed since the previous tick is
     * calculated and stored as {@link #tickElapsed} which can be used to determine the appropriate delay for the timer
     * once resumed, thus keeping the timer accurate. This is calculated from the system time at the previous tick
     * stored in {@link #tickStartTime} each clock tick and the current system time upon call.
     */
    private void pauseTimer() {
        tickElapsed += SystemClock.uptimeMillis() - tickStartTime;
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
        // if called after a back press, then device navigates back to main, dont want to open pause UI
        if (returnMain) {
            return;
        }
        // pause the game timer if it is currently running, and open the pause UI
        wasGamePaused = gamePaused;
        if (!gamePaused) {
            pauseTimer();
            pauseFragment();
        }
    }

    /**
     * Open the pause UI if onPause was called while the game was running. Used for instances such as app moving to background
     * without the user manually pausing. Take no action if the timer had not started, or game was paused already.
     */
    @Override
    protected void onResume() {
        super.onResume();
//        Log.i(TAG, "onResume: ");
    }

    int getEmptyCellIndex() {
        return emptyCellIndex;
    }

    int getNumRows() {
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
     *
     * @return an array[2] containing an int for both the saved time and moves data
     */
    private int[] puzzleBestData() {

        FileInputStream saveTimesFile = null;
        int[] savedData = {-1, -1};
        StringBuilder stringBuilder = new StringBuilder();
        //TODO: support for all sized default grids...
        String[] puzzleStrings = {"defaultgrid: ", "carpet: ", "cat: ", "clock: ", "crab: ",
                "darklights: ", "nendou: ", "razer: ", "saiki: ", "mms: "};

//        Log.i(TAG, "puzzleNum: " + puzzleNum);
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
//                Log.i(TAG, "savefile line: " + line);
                // NOTE: only call method once or this will make the savedDataList have twice the entries, causing bugs
                savedDataList.add(line);
                if (currentLine == puzzleNum) {
                    int timeStartIndex = line.indexOf(":") + 2;
                    int timeEndIndex = line.indexOf(",");  // indexOf will give -1 if not found ie no data for the puzzle
                    if (timeEndIndex != -1) {  // if there is saved data then get it
                        savedData[0] = Integer.parseInt(line.substring(timeStartIndex, timeEndIndex));
                        savedData[1] = Integer.parseInt(line.substring(timeEndIndex + 1));
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
                try (FileOutputStream outputStream = openFileOutput("gametimes", Context.MODE_PRIVATE)) {
                    outputStream.write(fileContents.getBytes());
                } catch (Exception writeError) {
                    Log.i(TAG, "write exception: " + writeError);
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
     * to the saved data for the lowest value of each of these taken to complete the same puzzle (if any)
     * updates the saved data file if either (or both) value(s) for the completed puzzle is lower than the saved data
     *
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
            String moveString = savedData.substring(timeEndIndex + 1);
            // time and moves are lower than saved values, so update
            if (gameData[0] < Integer.parseInt(timeString) && gameData[1] < Integer.parseInt(moveString)) {
                newData = puzzleIdentifier + gameTime + "," + gameMoves;
            }  // update time only
            if (gameData[0] < Integer.parseInt(timeString) && gameData[1] > Integer.parseInt(moveString)) {
                newData = puzzleIdentifier + gameTime + "," + moveString;
            }  // update moves only
            if (gameData[0] > Integer.parseInt(timeString) && gameData[1] < Integer.parseInt(moveString)) {
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
//                Log.i(TAG, "saveGameData listLine: " + element);
                stringBuilder.append(element).append("\n");
            }
            String fileContents = stringBuilder.toString();
//            Log.i(TAG, "saveGameData fileContents: " + fileContents);
            // overwrite the old file to contain the updated data
            try (FileOutputStream outputStream = openFileOutput("gametimes", Context.MODE_PRIVATE)) {
                outputStream.write(fileContents.getBytes());
            } catch (Exception writeError) {
                Log.i(TAG, "write exception: " + writeError);
            }
        }
    }

    /**
     * Create a list containing cell position indexes (0-14) in a random order which can be used to arrange bitmaps
     * in the grid in this randomised order. This order of cells is checked for a property which tells us if the puzzle
     * can be solved. If it cannot be solved, we simply have to swap two items and it is solvable. Once solvable, we
     * return the ArrayList.
     *
     * @param numCols number of columns in the puzzle grid
     */
    private ArrayList<Integer> randomiseGrid(int numCols) {
        // initialise objects and set variables
        Random random = new Random();
        int gridSize = numCols * numCols;
        ArrayList<Integer> randomisedGrid = new ArrayList<>(gridSize);
        ArrayList<Integer> posPool = new ArrayList<>(gridSize - 1);

        // initialise variables for start of each loop
        int bound = gridSize - 1;  // bounds for random generator...between 0 (inclusive) and number (exclusive)
        for (int x = 0; x < bound; x++) {  // pool for random indexes to be drawn from - exclude last cell index
            posPool.add(x);
        }

        // randomise grid and create list with outcome
        for (int x = 0; x < gridSize - 1; x++) {
            int rngIndex = random.nextInt(bound);  // gets a randomised number within the pools bounds
            int rngBmpIndex = posPool.get(rngIndex); // get the bitmap index from the pool using the randomised number
            posPool.remove((Integer) rngBmpIndex);  // remove used number from the pool - use Integer else it takes as Arrayindex
            bound -= 1;  // lower the bounds by 1 to match the new pool size so the next cycle can function properly
            randomisedGrid.add(rngBmpIndex);  // add the randomised bmp index to the gridList
        }
        randomisedGrid.add(gridSize - 1);

        // n=odd -> inversions: even = solvable
        // n=even -> empty cell on even row (from bottom: 1,2,3++ = 1 for bottom right) + inversions: odd = solvable
        //        -> empty cell on odd row + inversions: even = solvable
        // empty cell always on bottom right so both odd and even size grids need even inversions
        // inversion: position pairs (a,b) where (list) index a < index b and (value) a > b have to check all
        int inversions = 0;
        for (int index = 0; index < gridSize - 1; index++) {  // test all grid cells for pairs with higher index cells
            int currentNum = randomisedGrid.get(index);
            for (int x = index + 1; x < gridSize; x++) {  // find all pairs with higher index than current selected cell
                int pairNum = randomisedGrid.get(x);  // get the next highest index cell
                if (currentNum > pairNum) {  // add inversion if paired cell value is less than current cell value
                    inversions += 1;
                }
            }
        }

        // if randomised grid is not solvable then swap first two items to make it solvable then return that grid
        if (inversions % 2 != 0) {
            int swap = randomisedGrid.get(0);
            randomisedGrid.set(0, randomisedGrid.get(1));
            randomisedGrid.set(1, swap);
        }
        return randomisedGrid;
    }

    /**
     * Scale an image from a file path to a specific views size, and return as a Bitmap
     * Intended to be used for photos taken with a camera intent so images are by default in landscape
     * Therefore images are also rotated 90 degrees
     *
     * @param viewSize  size of the (pixels) view that the image is intended to be placed into
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
        int scaleFactor = Math.min(photoW / viewSize, photoH / viewSize);
        bmOptions.inJustDecodeBounds = false;
        bmOptions.inSampleSize = scaleFactor;
        return BitmapFactory.decodeFile(photopath, bmOptions);
    }

    /**
     * create the grid of smaller cell bitmaps using the chosen image and grid size and add them to the bitmaps list
     *
     * @param bmp     bitmap image to be used to create grid of images for the puzzle
     * @param rows    number of rows to split the grid into
     * @param columns number of columns to split the grid into
     */
    private void createBitmapGrid(Bitmap bmp, int rows, int columns) {
        // determine cell size in pixels from the image size and set amount of rows/cols
        int cellSize = bmp.getWidth() / rows;
        // for each row loop 4 times creating a new cropped image from original bitmap and add to adapter dataset
        for (int x = 0; x < columns; x++) {
            // for each row, increment y value to start the bitmap at
            int ypos = x * cellSize;
            for (int y = 0; y < rows; y++) {
                // loop through 4 positions in row incrementing the x value to start bitmap at
                int xpos = y * cellSize;
                Bitmap gridImage = Bitmap.createBitmap(bmp, xpos, ypos, cellSize, cellSize);
                // converted to drawable for use of setImageDrawable to easily swap cell images
                bitmaps.add(new BitmapDrawable(getResources(), gridImage));
            }
        }
    }

    /**
     * Determine if grid is solved by iterating through all cells to check if the cell position matches the set image
     * cell tag[0] gives position, tag[1] is the image index, if they are the same then image is in correct cell
     *
     * @return a boolean which indicates whether the grid is solved or not
     */
    private boolean gridSolved() {
        for (ImageView cell : gridCells) {
            // get the tags of each cell in the grid
            int[] cellTag = (int[]) cell.getTag();
            if (cellTag[0] != cellTag[1]) {
                return false;
            }
        }
        return true;
    }

    /**
     * Called when puzzle is solved, it makes a screen wide layout clickable and visible to render the game UI frozen
     * and inflates a new layout UI into this. This overlay UI shows the game and saved data for the puzzle and contains
     * buttons to allow the user to navigate back to the main activity or restart the current puzzle.
     *
     * @param gameData an array containing the time and moves data for the completed puzzle.
     */
    private void solvedPuzzleUI(int[] gameData) {
        // inflates a layout into the pause fragment container and makes the game activity unclickable
        mBinding.pauseContainer.setClickable(true);
        mBinding.pauseContainer.setVisibility(View.VISIBLE);
        PuzzleSolvedUiBinding solvedUiBinding = PuzzleSolvedUiBinding.inflate(getLayoutInflater(),
                mBinding.pauseContainer, true);

        //set onclick listeners for the UI buttons
        solvedUiBinding.retryButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // randomise grid or reset to starting state?
                startActivity(getIntent());
                finish();
            }
        });
        solvedUiBinding.newButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        // set text to be displayed - message, game data, and saved data
        String bestSecs = "", bestMins = "", bestMoves = "";
        // there is saved data
        if (bestData[0] != -1) {
            bestSecs = String.valueOf(bestData[0] % 60);
            bestMins = String.valueOf(bestData[0] / 60);
            bestMoves = String.valueOf(bestData[1]);
        }
        int puzzleSecs = gameData[0] % 60, puzzleMins = gameData[0] / 60;
        // emoticons: 😃 😁 😄 😎 😊 ☻ 👍 🖒 ☜ ☞
        solvedUiBinding.puzzleDataView.setText(String.format(Locale.getDefault(), "Puzzle Solved \uD83D\uDE0E \n" +
                        " %02d m : %02d s\n %d moves",
                puzzleMins, puzzleSecs, gameData[1]));
        solvedUiBinding.puzzleBests.setText(String.format(Locale.getDefault(), " Best Time: %s m : %s s \n Best Moves: %s",
                bestMins, bestSecs, bestMoves));

        // display a message if user achieved a new best
        if (newBestData) {
            Toast newBestToast = Toast.makeText(getApplicationContext(), "New Best \uD83D\uDC4D", Toast.LENGTH_LONG);
            newBestToast.setGravity(Gravity.TOP, 0, 150);
            newBestToast.show();
        }

    }

    /**
     * Called if a swipe in the valid direction occurs on a cell in the same group (row/column) of the empty cell or
     * if a click occurs on a direct neighbour of the empty cell. Consecutively swaps the cell image and image tag for the
     * empty cell with each cell in the group up to, and including, the touched cell. In effect, this shifts all cells from
     * that touched until the empty cell by one grid position, leaving the empty cell in the touched cells original position.
     * After this, the amount of moves for the puzzle is updated and {@link #gridSolved} is called to check if grid is solved.
     *
     * @param groupMoves      the amount of cell moves to handle with this call; minimum = 1, maximum = 5
     * @param emptyGroupIndex the index of the empty cell within the group that the touched cell is also an element
     * @param gridIndex       the index of the touched cell within the puzzle grid
     * @param iterateSign     value of +/- 1, used to iterate through the group in the correct direction depending on the swipe
     * @param group           an array corresponding to a row or column of the puzzle grid, containing the cell ImageViews in that group
     */
    void MoveCells(int groupMoves, int emptyGroupIndex, int gridIndex, int iterateSign,
                   ArrayList<ImageView> group) {
        // start game timer on first move
        if (timerCount == 0) {
            gamePaused = false;
            startTimer();
        }
        // get the empty cell and the adjacent cell in a given group (row/col) then get the tags and image to be swapped
        for (int x = 0; x < groupMoves; x++) {  // incrementally swap cells from empty -> touched, in this order
            // adjacent cell to be swapped with empty has an index either +/- 1 from empty cells index in the group
            // increment - up/left, decrement - down/right, swapIndex is always the emptyCell index as we swap then loop
            int swapIndex = emptyGroupIndex + (iterateSign * x);
            ImageView swapCell = group.get(swapIndex + iterateSign), emptyCell = group.get(swapIndex);
            Drawable image = swapCell.getDrawable();
            int[] swapTag = (int[]) swapCell.getTag(), emptyTag = (int[]) emptyCell.getTag();
            // set empty cells new image and tag
            emptyCell.setImageDrawable(image);
            emptyTag[1] = swapTag[1];
            // set touched cells new image and tag
            swapCell.setImageDrawable(null);
            swapTag[1] = numRows * numRows - 1;  // use gridsize - 1 rather than empty cell tag as it was just changed
        }
        // update empty cell tracker to correspond to the new empty cell
        emptyCellIndex = gridIndex;
        // track amount of moves taken and update move counter to display this
        numMoves += groupMoves;
        mBinding.moveCounter.setText(String.valueOf(numMoves));
        // check if grid is solved, if so then check if the game data was lower than the saved data (if any)
        int[] gameData = {timerCount, numMoves};
        if (gridSolved()) {
            pauseTimer();
            gamePaused = true;  // change this so onResume does not open pause fragment after a finished game
            gameSolved = true;
            //TODO: animation or wait between UI popup?
            saveGameData(gameData);
            solvedPuzzleUI(gameData);
        }
    }

    /**
     * Determines if a swiped cell is within the same row or column as the empty cell and if the swipe was
     * in the direction of the empty cell - if so then calls {@link #MoveCells} to process the valid swipe
     *
     * @param view      the swiped cell's view object
     * @param gridCols  the amount of columns in the puzzle grid
     * @param direction the direction of the swipe: right = 1, left = 2, down = 3, up = 4
     */
    void SwipeCell(View view, int gridCols, int direction) {
        // obtaining cell image, the row and column lists they are part of and their index in those lists
        int[] cellTag = (int[]) view.getTag();
        int gridIndex = cellTag[0];
        int cellRow = gridIndex / gridCols;
        int cellCol = gridIndex - cellRow * gridCols;
        int emptyCellRow = emptyCellIndex / gridCols;
        int emptyCellCol = emptyCellIndex - emptyCellRow * gridCols;
        int numRowMoves = Math.abs(emptyCellCol - cellCol);
        int numColMoves = Math.abs(emptyCellRow - cellRow);

        // check if empty and touched cells are in same row/col and if correct swipe direction
        switch (direction) {
            case (1):  // right swipe - empty row index > touched row index
                if (emptyCellRow == cellRow && emptyCellCol > cellCol) {  // cell columns give the index in row
                    MoveCells(numRowMoves, emptyCellCol, gridIndex, -1, cellRows.get(cellRow));
                }
                break;
            case (2):  // left swipe - empty row index < touched row index
                if (emptyCellRow == cellRow && emptyCellCol < cellCol) {
                    MoveCells(numRowMoves, emptyCellCol, gridIndex, 1, cellRows.get(cellRow));
                }
                break;
            case (3):  // down swipe - empty col index > touched col index
                if (emptyCellCol == cellCol && emptyCellRow > cellRow) {  // cell rows give the index in column
                    MoveCells(numColMoves, emptyCellRow, gridIndex, -1, cellCols.get(cellCol));
                }
                break;
            case (4):  // up swipe - empty col index < touched col index
                if (emptyCellCol == cellCol && emptyCellRow < cellRow) {
                    MoveCells(numColMoves, emptyCellRow, gridIndex, 1, cellCols.get(cellCol));
                }
                break;
        }
    }

}
