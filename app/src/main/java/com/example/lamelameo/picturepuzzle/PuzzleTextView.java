package com.example.lamelameo.picturepuzzle;

import android.content.Context;
import android.support.v7.widget.AppCompatTextView;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.VelocityTracker;

public class PuzzleTextView extends AppCompatTextView {

    private VelocityTracker mVelocityTracker = null;
    private String TAG = "PuzzleTextView";
    private int cellPostion;
    private int cellImageTag;
    private float xDown, yDown;

    public PuzzleTextView(Context context) {
        // initialise attributes

        super(context);
    }

    public PuzzleTextView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    // convert density independent pixels to pixels using the devices pixel density
    private int dpToPx(float dp) {
        float density = getResources().getDisplayMetrics().density;
        // rounds up/down around 0.5
        long pixels = Math.round(dp * density);
        return (int) pixels;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        //TODO: use gestureDetector here and swap this view for default textVIew in puzzle

        int action = event.getActionMasked();
        int index = event.getActionIndex();
        int pointerId = event.getPointerId(index);

        final int DISTANCE_THRESHOLD = dpToPx(11);  // ~1/3 the cell size
        final int VELOCITY_THRESHOLD = 200;  // TODO: BALANCE VALUE

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
            case MotionEvent.ACTION_OUTSIDE:
                Log.i(TAG, "onOutside: ");
                break;
            case MotionEvent.ACTION_UP:
                Log.i(TAG, "onUp");
            case MotionEvent.ACTION_CANCEL:
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

//                if (Math.abs(xDiff) > Math.abs(yDiff)) {  // potential horizontal swipe - check distance and velocity
//                    if (Math.abs(xDiff) > DISTANCE_THRESHOLD && Math.abs(xVelocity) > VELOCITY_THRESHOLD) {
//                        if (xDiff > 0) {  // right swipe
//                            //TODO: right swipe
//                            Log.i(TAG, "rightSwipe ");
//
//                            //TODO: need to add gridIndex to tags of cells or use custom cell
//                            SwipeCell(v, 13, (int)Math.sqrt(gridCells.size()), 1);
//                        } else {  // left swipe
//                            //TODO: left swipe
//                            Log.i(TAG, "leftSwipe");
//                        }
//                    } else {
//                        v.performClick();
//                    }
//                } else {
//                    if (Math.abs(xDiff) < Math.abs(yDiff)) {  // potential vertical swipe
//                        if (Math.abs(yDiff) > DISTANCE_THRESHOLD && Math.abs(yVelocity) > VELOCITY_THRESHOLD) {
//                            if (yDiff > 0) {  // down swipe
//                                //TODO: down swipe
//                                Log.i(TAG, "downSwipe");
//                            } else {  // up swipe
//                                //TODO: up swipe
//                                Log.i(TAG, "upSwipe");
//                            }
//                        } else {
//                            v.performClick();
//                        }
//                    } else {
//                        //TODO: get click to work..
//                        v.performClick();  //TODO: this is if swipe isnt found to have happened by set criteria
//                    }
//                }

                mVelocityTracker.recycle();
                mVelocityTracker = null;
                break;
        }
        return super.onTouchEvent(event);
    }

    @Override
    public boolean performClick() {
        // calls relevent click related tasks
        return super.performClick();
    }
}
