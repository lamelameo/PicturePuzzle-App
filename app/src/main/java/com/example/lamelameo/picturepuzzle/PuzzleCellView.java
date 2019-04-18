package com.example.lamelameo.picturepuzzle;

import android.content.Context;
import android.support.v7.widget.AppCompatImageView;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.VelocityTracker;

public class PuzzleCellView extends AppCompatImageView {

    private VelocityTracker mVelocityTracker = null;
    private String TAG = "PuzzleCellView";
    private int cellPostion;
    private int cellImageTag;
    private float xDown, yDown;
    private PuzzleActivity mContext;
    private int numRows;
    private int DISTANCE_THRESHOLD;
    private int VELOCITY_THRESHOLD;

    public PuzzleCellView(Context context) {
        super(context);
        // initialise attributes
        if (context instanceof PuzzleActivity) {
            mContext = (PuzzleActivity)context;
            numRows = mContext.getNumRows();
            DISTANCE_THRESHOLD = mContext.dpToPx(11);  // ~1/3 the cell size
            VELOCITY_THRESHOLD = 200;
        }
    }

    public PuzzleCellView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        //TODO: could use gestureDetector here instead, is simpler

        int[] tag = (int[])this.getTag();
        // consume touch with no action taken if empty cell is touched
        if (tag[0] == mContext.getEmptyCellIndex()) {
            Log.i(TAG, "emptyCellTouched");
            return true;
        }

        // TODO: multi finger touch bugs... think ive handled it but keeping jsut in case..
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
                            mContext.SwipeCell(this, numRows, 1);
                        } else {  // left swipe
                            Log.i(TAG, "leftSwipe");
                            mContext.SwipeCell(this, numRows, 2);
                        }
                    } else {
                        performClick();
                    }
                } else {
                    if (Math.abs(xDiff) < Math.abs(yDiff)) {  // potential vertical swipe
                        if (Math.abs(yDiff) > DISTANCE_THRESHOLD && Math.abs(yVelocity) > VELOCITY_THRESHOLD) {
                            if (yDiff > 0) {  // down swipe
                                Log.i(TAG, "downSwipe");
                                mContext.SwipeCell(this, numRows, 3);
                            } else {  // up swipe
                                Log.i(TAG, "upSwipe");
                                mContext.SwipeCell(this, numRows, 4);
                            }
                        } else {
                            performClick();
                        }
                    } else {  // if swipe isnt found to have happened by any of the set criteria
                        performClick();
                    }
                }
                // reset velocity tracker
                mVelocityTracker.recycle();
                mVelocityTracker = null;
                break;
        }
//        return true;
        return super.onTouchEvent(event);
    }

    @Override
    public boolean performClick() {
        // calls relevant click related tasks
        int emptyCellIndex = mContext.getEmptyCellIndex();
        int[] cellTag = (int[])this.getTag();
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
            mContext.MoveCells(1, emptyCellCol, lastCell, cellIndex, cellsRowDiff, mContext.getCellRow(cellRow));
        }
        if (cellCol == emptyCellCol && cellsColDiff*cellsColDiff == 1) {  // if cell is up/down of empty
            mContext.MoveCells(1, emptyCellRow, lastCell, cellIndex, cellsColDiff, mContext.getCellCol(cellCol));
        }
        return super.performClick();

    }
}
