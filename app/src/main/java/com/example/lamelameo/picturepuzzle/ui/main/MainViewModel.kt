package com.example.lamelameo.picturepuzzle.ui.main

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.util.Log
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LiveData
import com.example.lamelameo.picturepuzzle.PuzzleController
import com.example.lamelameo.picturepuzzle.data.PuzzleData
import com.example.lamelameo.picturepuzzle.data.PuzzleDataRepository

class MainViewModel(private val imagePath: String,
                    private val gridSize: Int,
                    private val mLifecycle: Lifecycle) : ViewModel() {

    private val handler: Handler = Handler(Looper.getMainLooper())
    private val mTicker: Ticker = Ticker(handler, mLifecycle)
    private val controller: PuzzleController = PuzzleController(handler)
    private val puzzleData: PuzzleData = PuzzleData(0,0,0,
        controller.generatePuzzle(gridSize), gridSize - 1, isPaused = true, isSolved = false
    )
    private val mPuzzleData: MutableLiveData<PuzzleData> = MutableLiveData(puzzleData)

//    private var mBestDataRepository: PuzzleDataRepository = PuzzleDataRepository.getInstance()

    // TODO: main activity should send a URI to chosen image, then we can access it from here
    // save relevant game data


    fun getPuzzleData(): LiveData<PuzzleData> {
        return mPuzzleData
    }

    init {
        // TODO: get best data from repository
        handler.handleMessage(Message.obtain(handler))
        // TODO: handle messages from handler 1=tick timer 2=increment moves
    }

    override fun onCleared() {
        super.onCleared()
        Log.i("PuzzleViewModel", "puzzle view model destroyed")
    }

    //TODO: how to update PuzzleData data


    fun cellClicked(cellIndex: Int, emptyIndex: Int) {
        controller.cellClick(cellIndex, emptyIndex)
    }

    fun cellSwiped(cellIndex: Int, emptyIndex: Int, direction: Int) {
        controller.cellSwipe(cellIndex, emptyIndex, direction)
    }

    // TODO: have ViewModel observe lifecycle instead of ticker?
    fun pauseGame() {
        mTicker.pauseTimer()
        mPuzzleData.value!!.isPaused = true
    }

    fun startGame() {
        mTicker.startTimer()
        mPuzzleData.value!!.isPaused = false
    }

    fun setMoves(num: Int) {
//        gameMoves = num
    }


    fun getCellBitmap(id: Int) {

    }

    fun tick() {
//        gameTime += 1
    }

    fun startTimer() {
        mTicker.startTimer()
    }

    fun pauseTimer() {
        mTicker.pauseTimer()
    }

    fun newBest(time: Int, moves: Int) {

    }

    fun newImage() {
        // TODO: make new best data entry in database
    }


}