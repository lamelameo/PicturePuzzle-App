package com.example.lamelameo.picturepuzzle;

import android.content.Context;
import androidx.appcompat.widget.AppCompatImageView;
import android.util.AttributeSet;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.VelocityTracker;

public class PuzzleCellView extends AppCompatImageView {

    private VelocityTracker mVelocityTracker = null;
    private final String TAG = "PuzzleCellView";
    private float xDown, yDown;
    private PuzzleActivity mContext;
    private int numRows;
    private int DISTANCE_THRESHOLD;
    private int VELOCITY_THRESHOLD;

    public PuzzleCellView(Context context) {
        super(context);
        // initialise attributes
        if (context instanceof PuzzleActivity) {
            mContext = (PuzzleActivity) context;
            numRows = mContext.getNumRows();
            DISTANCE_THRESHOLD = dpToPx(11);  // ~1/3 the cell size
            VELOCITY_THRESHOLD = 200;
        }
    }

    public PuzzleCellView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    /**
     * convert density independent pixels to pixels using the device's pixel density
     *
     * @param dp amount to be converted from dp to px
     * @return the value converted to units of pixels as an integer (rounds up or down)
     */
    int dpToPx(float dp) {
        return Math.round(dp * getResources().getDisplayMetrics().density);
    }

    /**
     * Handle simple touch events as either a swipe (up/down/left/right) or a click - multi touch is not supported.
     * Swipe direction is determined by distance travelled in the x/y planes between touch/release and its release velocity.
     * Distance and velocity must be greater than DISTANCE_THRESHOLD and VELOCITY_THRESHOLD, respectively, to be valid.
     * An event that doesn't fit any of the set criteria is considered a click and handed to the onClick listener.
     */
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        //TODO: could use gestureDetector here instead, is simpler
        int[] tag = (int[]) this.getTag();
        // consume touch with no action taken if empty cell is touched
        if (tag[0] == mContext.getEmptyCellIndex()) {
            return true;
        }

        // TODO: multi finger touch bugs... think ive handled it but keeping jsut in case..
        // get pointer id which identifies the touch...can handle multi touch events
        int action = event.getActionMasked(), index = event.getActionIndex(), pointerId = event.getPointerId(index);
        // if the pointer id isnt 0, a touch is currently being processed - ignore this new one to avoid crashes
        if (pointerId != 0) {
            return true;
        }

        analyseEventForGesture(event, action, pointerId);
        return true;
    }

    private void analyseEventForGesture(MotionEvent event, int action, int pointerId) {
        float xVelocity, xCancel, xDiff, yVelocity, yCancel, yDiff;
        switch (action) {
            case MotionEvent.ACTION_DOWN:
//                Log.i(TAG, "onDown");
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
//                Log.i(TAG, "onMove: ");
                mVelocityTracker.addMovement(event);
                mVelocityTracker.computeCurrentVelocity(1000);
                break;
            case MotionEvent.ACTION_UP:
//                Log.i(TAG, "onTouch: ");
            case MotionEvent.ACTION_OUTSIDE:
//                Log.i(TAG, "onOutside: ");
            case MotionEvent.ACTION_CANCEL:
//                Log.i(TAG, "onCancel");
                mVelocityTracker.addMovement(event);
                mVelocityTracker.computeCurrentVelocity(1000);
                xCancel = event.getRawX();
                yCancel = event.getRawY();
                xDiff = xCancel - xDown;
                yDiff = yCancel - yDown;
                xVelocity = mVelocityTracker.getXVelocity(pointerId);
                yVelocity = mVelocityTracker.getYVelocity(pointerId);
//                Log.i(TAG, "xDown: "+xDown+" xCancel: "+xCancel+" yDown: "+yDown+" yCancel: "+yCancel);
//                Log.i(TAG, "diffX: "+xDiff+" diffY: "+yDiff+" velX: "+xVelocity+" velY: "+yVelocity);
                determineGesture(xVelocity, xDiff, yVelocity, yDiff);
                // reset velocity tracker
                mVelocityTracker.recycle();
                mVelocityTracker = null;
                break;
        }
    }

    /**
     * Determine the gesture given the relevant velocity and distances. If satisfied with distance and velocity we
     * call SwipeCell() from the PuzzleActivity context, else we call {@link #performClick()} instead.
     *
     * @param xVelocity velocity in x direction
     * @param xDiff     distance in x direction
     * @param yVelocity velocity in y direction
     * @param yDiff     distance in y direction
     */
    private void determineGesture(float xVelocity, float xDiff, float yVelocity, float yDiff) {
        if (Math.abs(xDiff) > Math.abs(yDiff)) {  // potential horizontal swipe - check distance and velocity
            if (Math.abs(xDiff) > DISTANCE_THRESHOLD && Math.abs(xVelocity) > VELOCITY_THRESHOLD) {
                if (xDiff > 0) {  // right swipe
                    mContext.SwipeCell(this, numRows, 1);
                } else {  // left swipe
                    mContext.SwipeCell(this, numRows, 2);
                }
            } else {
                performClick();
            }
        } else {
            if (Math.abs(xDiff) < Math.abs(yDiff)) {  // potential vertical swipe
                if (Math.abs(yDiff) > DISTANCE_THRESHOLD && Math.abs(yVelocity) > VELOCITY_THRESHOLD) {
                    if (yDiff > 0) {  // down swipe
                        mContext.SwipeCell(this, numRows, 3);
                    } else {  // up swipe
                        mContext.SwipeCell(this, numRows, 4);
                    }
                } else {
                    performClick();
                }
            } else {  // if swipe isnt found to have happened by any of the set criteria
                performClick();
            }
        }
    }

    /**
     * Handle single clicks on any cell other than the empty cell - these are filtered out in the touchListener.
     * Checks if the clicked cell is a direct neighbour of the empty cell.
     * If so then calls PuzzleActivity.MoveCells to handle movement of images/tags and checking if grid is solved.
     */
    @Override
    public boolean performClick() {
        // calls relevant click related tasks
        int emptyCellIndex = mContext.getEmptyCellIndex();
        int[] cellTag = (int[]) this.getTag();
        int cellIndex = cellTag[0];
        // check if cell is in same row or column as empty cell
        int emptyCellRow = emptyCellIndex / numRows;
        int emptyCellCol = emptyCellIndex - emptyCellRow * numRows;
        int cellRow = cellIndex / numRows;
        int cellCol = cellIndex - cellRow * numRows;
        // determine distance and direction from the empty cell, opposite to the swipe (move) direction
        int cellsRowDiff = cellCol - emptyCellCol;  // left = -1, right = 1
        int cellsColDiff = cellRow - emptyCellRow;  // up = -1, down = 1
        // if cell is in same group then call movecells to make only one swap in the appropriate direction
        if (cellRow == emptyCellRow && cellsRowDiff * cellsRowDiff == 1) {  // if cell is left/right of empty
            mContext.MoveCells(1, emptyCellCol, cellIndex, cellsRowDiff, mContext.getCellRow(cellRow));
        }
        if (cellCol == emptyCellCol && cellsColDiff * cellsColDiff == 1) {  // if cell is up/down of empty
            mContext.MoveCells(1, emptyCellRow, cellIndex, cellsColDiff, mContext.getCellCol(cellCol));
        }
        return super.performClick();
    }

    //TODO: can implement this to simplify gesture detection
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
            //TODO: cant get this method to work i dont think...
            //TODO: check 1st event X,Y to see if its in a view then proceed to process else...return false
//            if (e1.getX() != ) {
//                return result;
//            } else {
//                // proceed to process swipe on the view
//            }

            float diffX = e2.getX() - e1.getX();
            float diffY = e2.getY() - e1.getY();
//            Log.i(TAG, "onFling:" + "diffX: " + diffX + "diffY: " + diffY + "velX: " + velocityX + "velY: " + velocityY);
            if (Math.abs(diffX) > Math.abs(diffY)) {  // potential horizontal swipe - check distance and velocity
                if (Math.abs(diffX) > DISTANCE_THRESHOLD && Math.abs(velocityX) > VELOCITY_THRESHOLD) {
                    if (diffX > 0) {  // right swipe
                        Log.i(TAG, "rightSwipe ");
                    } else {  // left swipe
                        Log.i(TAG, "leftSwipe");
                    }
                    result = true;  // confirmed swipe
                }
            } else {
                if (Math.abs(diffX) < Math.abs(diffY)) {  // potential vertical swipe
                    if (Math.abs(diffY) > DISTANCE_THRESHOLD && Math.abs(velocityY) > VELOCITY_THRESHOLD) {
                        if (diffY > 0) {  // down swipe
                            Log.i(TAG, "downSwipe");
                        } else {  // up swipe
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
}
