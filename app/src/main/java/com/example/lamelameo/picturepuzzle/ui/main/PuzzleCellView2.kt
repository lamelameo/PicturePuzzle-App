package com.example.lamelameo.picturepuzzle.ui.main

import android.content.Context
import android.util.AttributeSet
import android.util.Log
import android.view.MotionEvent
import android.view.VelocityTracker
import com.example.lamelameo.picturepuzzle.R
import com.example.lamelameo.picturepuzzle.data.MainViewModel
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * TODO: document your custom view class.
 */
class PuzzleCellView2 : androidx.appcompat.widget.AppCompatImageView {

    private var mVelocityTracker: VelocityTracker? = null
    private val TAG = "PuzzleCellView"
    private var xDown: Float = 0f
    private var yDown: Float = 0f
    private lateinit var mContext: PuzzleActivity2
    private lateinit var mainViewModel: MainViewModel
    private var DISTANCE_THRESHOLD = 0
    private var VELOCITY_THRESHOLD = 0

    constructor(context: Context, viewModel: MainViewModel) : super(context) {
        init(null, 0, context, viewModel)
    }

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {
//        init(attrs, 0, context)
    }

    constructor(context: Context, attrs: AttributeSet, defStyle: Int) : super(context, attrs, defStyle) {
//        init(attrs, defStyle, context)
    }

    private fun init(attrs: AttributeSet?, defStyle: Int, context: Context, viewModel: MainViewModel) {
        // Load attributes
//        val a = context.obtainStyledAttributes(attrs, R.styleable.PuzzleCellView2, defStyle, 0)
        // initialise attributes
        if (context is PuzzleActivity2) {
            mContext = context
            mainViewModel = viewModel
            DISTANCE_THRESHOLD = dpToPx(11f) // ~1/3 the cell size
            VELOCITY_THRESHOLD = 150
        }
    }

    /**
     * convert density independent pixels to pixels using the device's pixel density
     *
     * @param dp amount to be converted from dp to px
     * @return the value converted to units of pixels as an integer (rounds up or down)
     */
    private fun dpToPx(dp: Float): Int {
        return (dp * resources.displayMetrics.density).roundToInt()
    }

    /**
     * Handle simple touch events as either a swipe (up/down/left/right) or a click - multi touch is not supported.
     * Swipe direction is determined by distance travelled in the x/y planes between touch/release and its release velocity.
     * Distance and velocity must be greater than DISTANCE_THRESHOLD and VELOCITY_THRESHOLD, respectively, to be valid.
     * An event that doesn't fit any of the set criteria is considered a click and handed to the onClick listener.
     */
    override fun onTouchEvent(event: MotionEvent): Boolean {
        val tag = this.tag as Int
        Log.i(TAG, "cell:$tag")
        // consume touch with no action taken if empty cell is touched
        if (tag == mainViewModel.emptyCellIndex()) {
            return true
        }

        // TODO: multi finger touch bugs... think ive handled it but keeping just in case..
        // get pointer id which identifies the touch...can handle multi touch events
        val pointerId = event.getPointerId(event.actionIndex)
        // if the pointer id isnt 0, a touch is currently being processed - ignore this new one to avoid crashes
        var cellUpdates: List<List<Int>> = listOf()
        if (pointerId == 0) {
            when (analyseEventForGesture(event, event.actionMasked, pointerId)) {
                0 -> { performClick() }  // click
                1 -> { cellUpdates = mainViewModel.cellSwiped(tag, 0) }  // right
                2 -> { cellUpdates = mainViewModel.cellSwiped(tag, 1) }  // left
                3 -> { cellUpdates = mainViewModel.cellSwiped(tag, 2) }  // down
                4 -> { cellUpdates = mainViewModel.cellSwiped(tag, 3) }  // up
                -1 -> { Log.i(TAG, "action in progress") }
            }
            for (cells in cellUpdates) {
                mContext.swapCellImages(cells[0], cells[1])
            }
        }
        return true
    }

    private fun analyseEventForGesture(event: MotionEvent, action: Int, pointerId: Int): Int {
        when (action) {
            MotionEvent.ACTION_DOWN -> {
                if (mVelocityTracker == null) {
                    mVelocityTracker = VelocityTracker.obtain()
                } else {
                    mVelocityTracker!!.clear()
                }
                mVelocityTracker!!.addMovement(event)
                xDown = event.rawX
                yDown = event.rawY
            }
            MotionEvent.ACTION_MOVE -> {
                mVelocityTracker!!.addMovement(event)
                mVelocityTracker!!.computeCurrentVelocity(1000)
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_OUTSIDE, MotionEvent.ACTION_CANCEL -> {
                mVelocityTracker!!.addMovement(event)
                mVelocityTracker!!.computeCurrentVelocity(1000)
                val gesture = determineGesture(
                    mVelocityTracker!!.getXVelocity(pointerId),
                    event.rawX - xDown,
                    mVelocityTracker!!.getYVelocity(pointerId),
                    event.rawY - yDown
                )
                // reset velocity tracker
                mVelocityTracker!!.recycle()
                mVelocityTracker = null
                return gesture
            }
        }
        return -1
    }

    /**
     * Determine the gesture given the relevant velocity and distances. If satisfied with distance and velocity we
     * call SwipeCell() from the PuzzleActivity context, else we call [.performClick] instead.
     *
     * @param xVelocity velocity in x direction
     * @param xDiff     distance in x direction
     * @param yVelocity velocity in y direction
     * @param yDiff     distance in y direction
     */
    private fun determineGesture(xVelocity: Float, xDiff: Float, yVelocity: Float, yDiff: Float): Int {
        if (abs(xDiff) > abs(yDiff)) {  // potential horizontal swipe - check distance and velocity
            return if (abs(xDiff) > DISTANCE_THRESHOLD && abs(xVelocity) > VELOCITY_THRESHOLD) {
                if (xDiff > 0) {  // right swipe
                    1
                } else {  // left swipe
                    2
                }
            } else {
                0
            }
        } else {
            return if (abs(xDiff) < abs(yDiff)) {  // potential vertical swipe
                if (abs(yDiff) > DISTANCE_THRESHOLD && abs(yVelocity) > VELOCITY_THRESHOLD) {
                    if (yDiff > 0) {  // down swipe
                        3
                    } else {  // up swipe
                        4
                    }
                } else {
                    0
                }
            } else {  // if swipe isnt found to have happened by any of the set criteria
                0
            }
        }
    }

    /**
     * Handle single clicks on any cell other than the empty cell - these are filtered out in the touchListener.
     * Checks if the clicked cell is a direct neighbour of the empty cell.
     * If so then calls PuzzleActivity.MoveCells to handle movement of images/tags and checking if grid is solved.
     */
    override fun performClick(): Boolean {
        val cellUpdates = mainViewModel.cellClicked(this.tag as Int)
        if (cellUpdates.isNotEmpty()) {
            mContext.swapCellImages(cellUpdates[0], cellUpdates[1])
        }
        return super.performClick()
    }

}
