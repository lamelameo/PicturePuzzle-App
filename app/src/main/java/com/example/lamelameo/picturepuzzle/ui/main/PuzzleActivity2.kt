package com.example.lamelameo.picturepuzzle.ui.main

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.size
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.example.lamelameo.picturepuzzle.R
import com.example.lamelameo.picturepuzzle.data.BestsDatabase
import com.example.lamelameo.picturepuzzle.data.MainViewModel
import com.example.lamelameo.picturepuzzle.data.PuzzleDataRepository
import com.example.lamelameo.picturepuzzle.data.ViewModelFactory
import com.example.lamelameo.picturepuzzle.databinding.Puzzle2ActivityBinding
import com.example.lamelameo.picturepuzzle.databinding.PuzzleSolvedUiBinding
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
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
        val photoPath = intent.getStringExtra("photoPath")
        val drawableId = intent.getIntExtra("drawableId", R.drawable.dfdfdefaultgrid)
        val imageBitmap: Bitmap? = if (photoPath == null) BitmapFactory.decodeResource(resources, drawableId) else null

        // initialise layout views and their attributes with given info from main activity
        mBinding.gridLayout.columnCount = numRows
        mBinding.gridLayout.rowCount = numRows
        mBinding.hintButton.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                mBinding.hintImage.visibility = View.VISIBLE; mBinding.hintImage.isClickable = true
            } else {
                mBinding.hintImage.visibility = View.INVISIBLE; mBinding.hintImage.isClickable = false
            }
        }
        mBinding.pauseButton.setOnClickListener {
            if (!mViewModel.pauseGame())
                Toast.makeText(applicationContext, "You have not started the puzzle!", Toast.LENGTH_SHORT).show()
        }

        // create or obtain viewmodel and repository instances and observe livedata for convenient updating of UI state
        val db = BestsDatabase.getInstance(this)
        val dataRepo = PuzzleDataRepository(db.bestDao)
        val viewModelFactory = ViewModelFactory(photoPath, "default_$drawableId", imageBitmap,
            dataRepo, numRows, mBinding.gridLayout.layoutParams.width
        )
        mViewModel = ViewModelProvider(this, viewModelFactory).get(MainViewModel::class.java)
        //TODO: coroutines?
        GlobalScope.launch { loadBestsView(mViewModel.getPuzzleBests()) }
        mHandler = Handler(Looper.getMainLooper())
        // TODO: one livedata of PuzzleData instance?
        mViewModel.getMovesLiveData().observe(this, { moves -> updateMovesView(moves) })
        mViewModel.getTimeLiveData().observe(this, { time -> updateTimerView(time) })
        mViewModel.getSolvedLiveData().observe(this, { solved -> if(solved) { openSolvedUI()} })
        mViewModel.getNewBestLiveData().observe(this, { best -> if (best) newToast() })
        mViewModel.getGameStateLiveData().observe(this,{ state ->
            when(state) { 1 -> closePauseUI(); 2 -> openPauseUI(); 3 -> openSolvedUI(); -1 -> finish()}})
        createPuzzleCells(mBinding.gridLayout, numRows)

        mPauseFragment = PauseFragment.newInstance(mViewModel)
        handleResume()
    }

    private fun openSolvedUI() {
        Log.i(TAG, "open solved UI")
//        mBinding.hintButton.visibility = View.INVISIBLE; mBinding.hintButton.isClickable = false
        mBinding.solvedUIContainer!!.visibility = View.VISIBLE; mBinding.solvedUIContainer!!.isClickable = true
        val solvedUIbinding: PuzzleSolvedUiBinding = PuzzleSolvedUiBinding.inflate(
            layoutInflater,
            mBinding.solvedUIContainer, true
        )
        solvedUIbinding.newButton.setOnClickListener { finish() }
        solvedUIbinding.retryButton.setOnClickListener { startActivity(intent); finish() }
        val time = mViewModel.getTimeLiveData().value
        solvedUIbinding.puzzleDataView.text = String.format(
            Locale.getDefault(), "Puzzle Solved 😎 \n %02d m : %02d s \n %d moves",
            time?.div(60), time?.rem(60), mViewModel.getMovesLiveData().value
        )
    }

    private fun newToast() {
        val newBestToast = Toast.makeText(applicationContext, "New Best \uD83D\uDC4D", Toast.LENGTH_LONG)
        newBestToast.setGravity(Gravity.TOP, 0, 150)
        newBestToast.show()
    }

    private fun openPauseUI() {
        val fragmentTrans = supportFragmentManager.beginTransaction()
        fragmentTrans.replace(mBinding.pauseContainer.id, mPauseFragment)
        mBinding.pauseContainer.visibility = View.VISIBLE
//        mBinding.pauseContainer.isClickable = true
        // TODO: pause UI stuck on backstack causing lifecycle problems
        fragmentTrans.addToBackStack(null)
        fragmentTrans.commit()
    }

    private fun closePauseUI() {
        val fragmentTrans = supportFragmentManager.beginTransaction()
        fragmentTrans.remove(mPauseFragment)
        supportFragmentManager.popBackStack()
        mBinding.pauseContainer.visibility = View.INVISIBLE
        mBinding.pauseContainer.isClickable = false
        fragmentTrans.commit()
    }

    private fun createPuzzleCells(gridLayout: androidx.gridlayout.widget.GridLayout, numRows: Int) {
        for (i in 0 until numRows * numRows) {
            val cellView = PuzzleCellView2(this, mViewModel)
            val cellSize: Int = gridLayout.layoutParams.width / numRows
            gridLayout.addView(cellView, i, ViewGroup.LayoutParams(cellSize, cellSize))
            cellView.tag = i
            if (i < numRows * numRows - 1) {
                cellView.setImageDrawable(BitmapDrawable(resources, mViewModel.getPuzzleImage(i)))
            }
        }
    }

    private fun handleResume() {
        // load model data into views
        updateMovesView(mViewModel.getMovesLiveData().value)
        updateTimerView(mViewModel.getTimeLiveData().value)
        mBinding.hintImage.setImageDrawable(BitmapDrawable(resources, mViewModel.getPuzzleImage(-1)))
        GlobalScope.launch { loadBestsView(mViewModel.getPuzzleBests()) }
        // 0 = not started, 1 = running, 2 = paused, 3 = solved
        //todo: if app coming back into foreground should auto pause
        when (mViewModel.gameState()) {
            1 -> mViewModel.resumeGame()  // TODO: have to remove fragments?
            2 -> openPauseUI()
            3 -> openSolvedUI()
        }
    }

    private fun loadBestsView(puzzleBests: List<Int>?) {
        var secs = ""; var mins = ""; var bestMoves = ""
        puzzleBests?.let {
            secs = puzzleBests[0].rem(60).toString()
            mins = puzzleBests[0].div(60).toString()
            bestMoves = puzzleBests[1].toString()
        }
        mBinding.bestsView.text = String.format(
            Locale.getDefault(), "Best Time: %s m:%s s \n Best Moves: %s", mins, secs, bestMoves
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

    override fun onBackPressed() {
        super.onBackPressed()
        when (mViewModel.gameState()) {
            2 -> { closePauseUI(); mViewModel.resumeGame() }
            else -> finish()
        }
    }

    override fun onResume() {
        super.onResume()
        Log.i(TAG, "onResume")
    }


}