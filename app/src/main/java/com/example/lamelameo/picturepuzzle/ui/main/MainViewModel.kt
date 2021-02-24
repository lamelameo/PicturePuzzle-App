package com.example.lamelameo.picturepuzzle.ui.main

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.util.Log
import java.util.*
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.ScheduledThreadPoolExecutor
import java.util.concurrent.TimeUnit

class MainViewModel : ViewModel() {

    // TODO: main activity should send a URI to chosen image, then we can access it from here
    // save relevant game data
    private val _cellStates: MutableLiveData<List<Int>> = TODO()
    private val _gameTime = MutableLiveData<Int>()
    private val _tickRemainder = MutableLiveData<Int>()
    private val _gameMoves = MutableLiveData<Int>()
    private val _bestMoves = MutableLiveData<Int>()
    private val _bestTime = MutableLiveData<Int>()
    private val _imagePath = MutableLiveData<String>()
    private val _isPaused = MutableLiveData<Boolean>()
    private val _isSolved = MutableLiveData<Boolean>()

    // all game data variables
    var gameTime = 0
    var gameMoves = 0
    var bestTime = 0
    var bestMoves = 0
    private var imagePath: String

    private val handler: Handler = Handler(Looper.getMainLooper())
    private val mTicker: Ticker = Ticker(handler)

    override fun onCleared() {
        super.onCleared()
        Log.i("PuzzleViewModel", "puzzle view model destroyed")
    }

    fun setMoves(num: Int) {
        gameMoves = num
    }

    fun setImagePath(path: String) {
        imagePath = path
    }

    fun getCellBitmap(id: Int) {

    }

    fun tick() {
        gameTime += 1
        // TODO call UI fragment to udpdate Timer View
    }

    fun startTimer() {
        mTicker.startTimer()
    }

    fun pauseTimer() {
        mTicker.pauseTimer()
    }


}