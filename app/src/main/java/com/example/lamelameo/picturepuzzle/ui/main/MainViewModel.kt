package com.example.lamelameo.picturepuzzle.ui.main

import android.graphics.Bitmap
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.util.Log
import androidx.lifecycle.*
import com.example.lamelameo.picturepuzzle.data.BestData
import com.example.lamelameo.picturepuzzle.data.BestDataDao
import com.example.lamelameo.picturepuzzle.data.PuzzleData
import com.example.lamelameo.picturepuzzle.data.PuzzleDataRepository

class MainViewModel(private val imagePath: String?,
                    private val imageBitmap: Bitmap?,
                    private val numRows: Int,
                    private val gridViewSize: Int) : ViewModel() {

    private val handler: Handler
    private val mTicker: Ticker
    private val controller: PuzzleController
    private val imageController: ImageController
    private val puzzleData: PuzzleData
//    private val mPuzzleData: MutableLiveData<PuzzleData>
    private val mMoves: MutableLiveData<Int>
    private val mTime: MutableLiveData<Int>
    private val mSolved: Boolean
    private val isSolved: MutableLiveData<Boolean>
    private val gameState: MutableLiveData<Int>
    private val TAG: String = "MainViewModel"
    private val bestData: BestData
    //    private var mBestDataRepository: PuzzleDataRepository = PuzzleDataRepository.getInstance()

    init {
        // TODO: get best data from repository
        // TODO: handle messages from handler 1 = tick timer, 2 = increment moves
        handler = object: Handler(Looper.getMainLooper()) {
            override fun handleMessage(msg: Message) {
                when(msg.what) {
                    1 -> tick()
                    2 -> incrementMoves()
                }
            }
        }
        mTicker = Ticker(handler)
        val gridSize = numRows * numRows
        // TODO: if we saved instance, then we can get saved data and give to puzzle data on creation
        puzzleData = PuzzleData(puzzleState = ArrayList(), emptyCell = gridSize - 1)
        controller = PuzzleController(handler, puzzleData, numRows)
        puzzleData.puzzleState = controller.generatePuzzle(gridSize)
//        mPuzzleData = MutableLiveData(puzzleData)
        mMoves = MutableLiveData(puzzleData.numMoves)
        mTime = MutableLiveData(puzzleData.gameTime)
        mSolved = false  // set based on puzzleData.gameState?
        isSolved = MutableLiveData(mSolved)
        gameState = MutableLiveData(puzzleData.gameState)
        imageController = ImageController(imagePath, imageBitmap, gridViewSize, numRows)
        // TODO: get best data from repository
        bestData = BestData("",0,0)
    }

    override fun onCleared() {
        super.onCleared()
        Log.i("PuzzleViewModel", "puzzle view model destroyed")
    }

//    fun getPuzzleData(): LiveData<PuzzleData> {
//        return mPuzzleData
//    }

    fun getMovesLiveData(): LiveData<Int> {
        return mMoves
    }

    fun getTimeLiveData(): LiveData<Int> {
        return mTime
    }

    fun getSolvedLiveData(): LiveData<Boolean> {
        return isSolved
    }

    fun getGameStateLiveData(): LiveData<Int> {
        return gameState
    }

    fun getPuzzleBest(name: String): LiveData<List<BestData>> {
        // TODO: get data from repository
        val bestRepo = PuzzleDataRepository.getInstance(BestDataDao())
        return bestRepo.getBests()
    }

    /**
     * Given a cells image, ret
     */
    fun getPuzzleImage(num: Int): Bitmap? {
        return if (num == -1) {
            imageController.getImageBitmap()
        } else {
            imageController.getCellBitmap(puzzleData.puzzleState[num])
        }
    }

    /**
     * Given the grid index of a cell that has been clicked, determine if it should be moved. Return a list of integers
     * which represents the image tags for the clicked cell and empty cell to be changed to (if valid click)
     */
    fun cellClicked(cellIndex: Int): List<Int> {
        val outcome = controller.cellClick(cellIndex, puzzleData.emptyCell)
        if (puzzleData.gameState == 0 && outcome.isNotEmpty()) {
            mTicker.startTimer()
            puzzleData.gameState = 1
        }
        return outcome
    }

    /**
     * Given the grid index of a cell that has been swiped, determine if moves should be made
     */
    fun cellSwiped(cellIndex: Int, direction: Int): List<List<Int>> {
        val outcome = controller.cellSwipe(cellIndex, puzzleData.emptyCell, direction)
        if (puzzleData.gameState == 0 && outcome.isNotEmpty()) {
            mTicker.startTimer()
            puzzleData.gameState = 1
        }
        return outcome
    }

    fun emptyCellIndex(): Int {
        return puzzleData.emptyCell
    }

//    private fun PuzzleData.set(data: Int) = when(data) {
//        1 ->
//    }

    // TODO: have ViewModel observe lifecycle instead of ticker?
    fun pauseGame(): Boolean {
        return if (puzzleData.gameState == 1) {
            mTicker.pauseTimer()
//            puzzleData.gameState = 2
            gameState.value = 2
            Log.i(TAG, "livedata: ${gameState.value}, puzzledata: ${puzzleData.gameState}")
            true
        } else {
            false
        }
    }

    fun resumeGame() {
        mTicker.startTimer()
    }

    fun gameState(): Int {
        return when(puzzleData.gameState) {
            0 -> 0  // not started
            1 -> { mTicker.startTimer(); 1 }  // running
            2 -> 2  // paused
            3 -> 3  // solved
            else -> -1  // should not be possible
        }
    }

    fun incrementMoves() {
        //TODO: update live data or PuzzleData value?
        mMoves.value = mMoves.value?.inc()
    }

    fun tick() {
        mTime.value = mTime.value?.inc()
        Log.i(TAG, "time: " + mTime.value + ", "+ puzzleData.gameTime)
    }

    fun newBest(time: Int, moves: Int) {

    }

    fun newImage() {
        // TODO: make new best data entry in database
    }


}