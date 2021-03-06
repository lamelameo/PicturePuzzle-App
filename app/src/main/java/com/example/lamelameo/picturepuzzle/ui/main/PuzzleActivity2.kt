package com.example.lamelameo.picturepuzzle.ui.main

import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.ImageView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProviders
import com.example.lamelameo.picturepuzzle.PuzzleCellView
import com.example.lamelameo.picturepuzzle.R
import com.example.lamelameo.picturepuzzle.databinding.Puzzle2ActivityBinding
import java.util.*

class PuzzleActivity2 : AppCompatActivity() {

    private lateinit var mViewModel: MainViewModel
    private lateinit var mHandler: Handler
    private lateinit var mBinding: Puzzle2ActivityBinding
    private lateinit var mCells: ArrayList<ImageView>
    private lateinit var mPauseFragment: Fragment

    private class MyViewModelFactory(private val imagePath: String, private val imageDrawable: Int,
                             private val numRows: Int, private val gridViewSize: Int) : ViewModelProvider.Factory {
        override fun <T : ViewModel?> create(modelClass: Class<T>): T {
            return modelClass.getConstructor(Int::class.java).newInstance(imagePath, imageDrawable, numRows, gridViewSize)
        }
    }

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
        mCells = ArrayList(numRows * numRows)
        mBinding.gridLayout.columnCount = numRows
        mBinding.gridLayout.rowCount = numRows

        // TODO: how do we handle onCreate calls where we have a viewmodel still (onstop not called)
        val viewModelFactory = MyViewModelFactory(photoPath, imageDrawable, numRows, mBinding.gridLayout.width)
        mViewModel = ViewModelProviders.of(this, viewModelFactory).get(MainViewModel::class.java)
        mHandler = Handler(Looper.getMainLooper())
        // TODO: one livedata of PuzzleData instance?
        mViewModel.getMovesLiveData().observe(this, { moves -> updateMovesView(moves) })
        mViewModel.getTimeLiveData().observe(this, { time -> updateTimerView(time) })
        mViewModel.getSolvedLiveData().observe(this, { solved -> if(solved) { openSolvedUI()} })
        mBinding.hintButton.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                mBinding.hintImage.visibility = View.VISIBLE
            } else {
                mBinding.hintImage.visibility = View.INVISIBLE
            }
        }
        mBinding.pauseButton.setOnClickListener { mViewModel.pauseGame() }
        createPuzzleCells(mBinding.gridLayout, numRows*numRows, numRows)

        // TODO: pause fragment class + interface needed?
        mPauseFragment = Fragment()
        handleResume()

    }

    private fun openSolvedUI() {
        TODO("Not yet implemented")
    }

    private fun createPuzzleCells(gridLayout: androidx.gridlayout.widget.GridLayout, gridSize: Int, numRows: Int) {
        for (i in 0 until gridSize) {
            val cellView = PuzzleCellView2(this, mViewModel)
            gridLayout.addView(cellView, i, gridLayout.layoutParams.width / numRows)
            cellView.tag = i
            cellView.setImageDrawable(BitmapDrawable(resources, mViewModel.getPuzzleImage(i)))
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
        val result: Int = mViewModel.startGame()
        if (result == 2) {
            val fragTrans = supportFragmentManager.beginTransaction()
            //TODO: load pause UI fragment
//            fragTrans.add(R.id.pauseContainer, PauseMenu)
        }
        if (result == 3) {
            // TODO: load solved UI fragment
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

    private fun handleDataChange() {

    }

    fun swapCellImages(index1: Int, index2: Int) {
        val image: Drawable = mCells[index1].drawable
        mCells[index1].setImageDrawable(mCells[index2].drawable)
        mCells[index2].setImageDrawable(image)
    }

    override fun onPause() {
        super.onPause()
        mViewModel.pauseGame()
    }


}