package com.example.lamelameo.picturepuzzle.ui.main

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.example.lamelameo.picturepuzzle.R
import com.example.lamelameo.picturepuzzle.databinding.FragmentPauseBinding
import com.example.lamelameo.picturepuzzle.databinding.Puzzle2ActivityBinding
import java.util.*

class PuzzleActivity2 : AppCompatActivity() {

    private lateinit var mViewModel: MainViewModel
    private lateinit var mHandler: Handler
    private lateinit var mBinding: Puzzle2ActivityBinding
    private lateinit var mPauseFragment: Fragment
    private val TAG: String = "PuzzleActivity2"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (savedInstanceState == null) {
        }
        mBinding = Puzzle2ActivityBinding.inflate(layoutInflater)
        setContentView(mBinding.root)

        // first get relevant chosen settings from main activity
        val numRows = intent.getIntExtra("numColumns", 4)
        val puzzleNum = intent.getIntExtra("puzzleNum", 0)
        val photoPath = intent.getStringExtra("photoPath")
        val imageDrawable = intent.getIntExtra("drawableId", R.drawable.dfdfdefaultgrid)
        var imageBitmap: Bitmap? = null
        if (photoPath == null) {
            imageBitmap = BitmapFactory.decodeResource(resources, imageDrawable)
        }

        // initialise layout views and their attributes with given info from main activity
        mBinding.gridLayout.columnCount = numRows
        mBinding.gridLayout.rowCount = numRows
        mBinding.hintButton.setOnCheckedChangeListener { _, isChecked ->
            mBinding.hintImage.visibility = if (isChecked) { View.VISIBLE } else { View.INVISIBLE }
        }
        mBinding.pauseButton.setOnClickListener { if (!mViewModel.pauseGame())
            Toast.makeText(applicationContext, "You have not started the puzzle!", Toast.LENGTH_SHORT).show() }

        // create or obtain viewmodel and observe livedata for convenient updating of UI state
        val viewModelFactory = ViewModelFactory(photoPath, imageBitmap, numRows, mBinding.gridLayout.width)
        mViewModel = ViewModelProvider(this, viewModelFactory).get(MainViewModel::class.java)
        mHandler = Handler(Looper.getMainLooper())
        // TODO: one livedata of PuzzleData instance?
        mViewModel.getMovesLiveData().observe(this, { moves -> updateMovesView(moves) })
        mViewModel.getTimeLiveData().observe(this, { time -> updateTimerView(time) })
        mViewModel.getSolvedLiveData().observe(this, { solved -> if(solved) { openSolvedUI()} })
        mViewModel.getGameStateLiveData().observe(this,{ state ->
            when(state) { 1 -> closePauseUI(); 2 -> openPauseUI(); 3 -> openSolvedUI() }})
        createPuzzleCells(mBinding.gridLayout, numRows)

        // TODO: pause fragment class + interface needed?
        mPauseFragment = PauseFragment.newInstance(mViewModel)
        handleResume()

    }

    private fun openSolvedUI() {
        TODO("Not yet implemented")
    }

    private fun openPauseUI() {
        val fragmentTrans = supportFragmentManager.beginTransaction()
        fragmentTrans.replace(mBinding.pauseContainer.id, mPauseFragment)
        mBinding.pauseContainer.visibility = View.VISIBLE
//        mBinding.pauseContainer.isClickable = true
        fragmentTrans.addToBackStack(null) //TODO: needed?
        fragmentTrans.commit()
    }

    private fun closePauseUI() {
        val fragmentTrans = supportFragmentManager.beginTransaction()
        fragmentTrans.remove(mPauseFragment)
        mBinding.pauseContainer.visibility = View.INVISIBLE
        mBinding.pauseContainer.isClickable = false
        fragmentTrans.addToBackStack(null)
        fragmentTrans.commit()
    }

    private fun createPuzzleCells(gridLayout: androidx.gridlayout.widget.GridLayout, numRows: Int) {
        for (i in 0 until numRows*numRows) {
            val cellView = PuzzleCellView2(this, mViewModel)
            val cellSize: Int = gridLayout.layoutParams.width / numRows
            gridLayout.addView(cellView, i, ViewGroup.LayoutParams(cellSize, cellSize))
            cellView.tag = i
            if (i < numRows*numRows - 1) {
                cellView.setImageDrawable(BitmapDrawable(resources, mViewModel.getPuzzleImage(i)))
            }
        }
    }

    private fun handleResume() {
        // load model data into views
        updateMovesView(mViewModel.getMovesLiveData().value)
        updateTimerView(mViewModel.getTimeLiveData().value)
        mBinding.hintImage.setImageDrawable(BitmapDrawable(resources, mViewModel.getPuzzleImage(-1)))
        // TODO: load best view
//        loadBestsView(mViewModel.getPuzzleBests("test"))
        // 0 = not started, 1 = running, 2 = paused, 3 = solved
        //todo: if app coming back into foreground should auto pause
        when (mViewModel.gameState()) {
            1 -> return  // TODO: have to remove fragments?
            2 -> openPauseUI()
            3 -> openSolvedUI()
        }
    }

    private fun loadBestsView(puzzleBests: List<Int>) {
        val secs: Int = puzzleBests[0] % 60
        val mins: Int = puzzleBests[0] / 60
        val bestMoves: Int = puzzleBests[1]
        mBinding.bestsView.text = String.format(
            Locale.getDefault(), "Best Time: %02d:%02d\nBest Moves: %d", mins, secs, bestMoves
        )
    }

    private fun updateMovesView(moves: Int?) {
        mBinding.moveCounter.text = moves.toString()
    }

    private fun updateTimerView(time: Int?) {
        mBinding.gameTimer.text = time.toString()
    }

    fun swapCellImages(index1: Int, index2: Int) {
        val cell1: ImageView = mBinding.gridLayout.getChildAt(index1) as ImageView
        val cell2: ImageView = mBinding.gridLayout.getChildAt(index2) as ImageView
        val image: Drawable? = cell1.drawable
        cell1.setImageDrawable(cell2.drawable)
        cell2.setImageDrawable(image)
    }

    override fun onPause() {
        super.onPause()
        mViewModel.pauseGame()
    }

    override fun onResume() {
        super.onResume()
        when (mViewModel.gameState()) {
            1 -> {  }
            2 -> { Log.i(TAG, "resumed - remove pause") }
            3 -> {  }
        }
    }


}