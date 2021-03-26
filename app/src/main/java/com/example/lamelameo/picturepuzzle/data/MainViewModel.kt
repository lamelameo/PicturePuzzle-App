package com.example.lamelameo.picturepuzzle.data

import android.graphics.Bitmap
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.util.Log
import androidx.lifecycle.*
import kotlinx.coroutines.launch

class MainViewModel(
    imagePath: String?,
    drawableName: String?,
    private val dataRepo: PuzzleDataRepository,
    imageBitmap: Bitmap?,
    numRows: Int,
    gridViewSize: Int
) : ViewModel() {

    private val handler: Handler
    private val mTicker: Ticker
    private val controller: PuzzleController
    private val imageController: ImageController
    private val puzzleData: PuzzleData
    private val mMoves: MutableLiveData<Int>
    private val mTime: MutableLiveData<Int>
    private val mSolved: Boolean
    private val isSolved: MutableLiveData<Boolean>
    private val gameState: MutableLiveData<Int>
    private val TAG: String = "MainViewModel"
    private val puzzleName: String
    private var bestData: List<Int>? = null
    private val newBest: MutableLiveData<Boolean> = MutableLiveData(false)

    init {
        handler = object: Handler(Looper.getMainLooper()) {
            override fun handleMessage(msg: Message) {
                when(msg.what) {
                    1 -> tick()
                    2 -> incrementMoves()
                    3 -> gameSolved()
                }
            }
        }
        mTicker = Ticker(handler)
        val gridSize = numRows * numRows
        // TODO: if we saved instance, then we can get saved data and give to puzzle data on creation
        puzzleData = PuzzleData(puzzleState = ArrayList(), emptyCell = gridSize - 1)
        controller = PuzzleController(handler, puzzleData, numRows)
        puzzleData.puzzleState = controller.generatePuzzle(gridSize)
        mMoves = MutableLiveData(puzzleData.numMoves)
        mTime = MutableLiveData(puzzleData.gameTime)
        mSolved = false  // set based on puzzleData.gameState?
        isSolved = MutableLiveData(mSolved)
        gameState = MutableLiveData(puzzleData.gameState)
        imageController = ImageController(imagePath, imageBitmap, gridViewSize, numRows)
        puzzleName = imagePath ?: "default_$drawableName"

    }

    override fun onCleared() {
        super.onCleared()
        Log.i("PuzzleViewModel", "puzzle view model destroyed")
    }

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

    fun getNewBestLiveData(): LiveData<Boolean> {
        return newBest
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
        if (gameState.value == 0 && outcome.isNotEmpty()) {
            mTicker.startTimer()
            gameState.value = 1
        }
        return outcome
    }

    /**
     * Given the grid index of a cell that has been swiped, determine if moves should be made
     */
    fun cellSwiped(cellIndex: Int, direction: Int): List<List<Int>> {
        val outcome = controller.cellSwipe(cellIndex, puzzleData.emptyCell, direction)
        if (gameState.value == 0 && outcome.isNotEmpty()) {
            mTicker.startTimer()
            gameState.value = 1
        }
        return outcome
    }

    fun emptyCellIndex(): Int {
        return puzzleData.emptyCell
    }

    // TODO: have ViewModel observe lifecycle instead of ticker?
    fun pauseGame(): Boolean {
        return if (gameState.value == 1) {
            mTicker.pauseTimer()
//            puzzleData.gameState = 2
            gameState.value = 2
//            Log.i(TAG, "livedata: ${gameState.value}, puzzledata: ${puzzleData.gameState}")
            true
        } else {
            false
        }
    }

    fun resumeGame() {
        mTicker.startTimer()
        if (gameState.value != 1) { gameState.value = 1 }
    }

    private fun gameSolved() {
        gameState.value = 3
        var updateBest = false
        viewModelScope.launch { updateBest = compareBest(puzzleName) }.invokeOnCompletion {
            if (updateBest) newBest.value = true }
    }

    private suspend fun compareBest(name: String): Boolean {
        var result = true
        dataRepo.getBest(name)?.let {
            if (it.time >= mTime.value!! && it.moves > mMoves.value!!) {
                dataRepo.update(BestData(name, mTime.value!!, mMoves.value!!))
            } else { result = false }
        } ?: dataRepo.addBest(BestData(name, mMoves.value!!, mTime.value!!))
        return result
    }

    fun finishGame() {
        mTicker.pauseTimer()
        gameState.value = -1
    }

    fun gameState(): Int {
        return when(gameState.value) {
            0 -> 0  // not started
            1 -> 1  // running
            2 -> 2  // paused
            3 -> 3  // solved
            else -> -1  // -1 if pause fragment new puzzle is clicked, but this should finish the activity
        }
    }

    private fun incrementMoves() {
        mMoves.value = mMoves.value?.inc()
    }

    private fun tick() {
        mTime.value = mTime.value?.inc()
    }

    suspend fun getPuzzleBests(): List<Int>? {
        return bestData ?: dataRepo.getBest(puzzleName)?.let { listOf(it.moves, it.time) }
    }


}