package com.example.lamelameo.picturepuzzle.data

import android.os.Handler
import android.os.Message
import android.os.SystemClock
import android.util.Log
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.OnLifecycleEvent
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.ScheduledThreadPoolExecutor
import java.util.concurrent.TimeUnit

class Ticker(private var handler: Handler) {

    private var tickStartTime: Long = SystemClock.uptimeMillis()
    private var tickElapsed: Long = 0
    private val timerExecutor = ScheduledThreadPoolExecutor(1)
    private lateinit var timerFuture: ScheduledFuture<*>
    private var isRunning: Boolean = false

    // Create runnable task (calls code in new thread) which increments a counter used as the timers text
    private var timerRunnable =
        Runnable {
            tickStartTime = SystemClock.uptimeMillis()
            tickElapsed = 0
            handler.sendMessage(Message.obtain(handler, 1))
        }

    /**
     * A method to post a delayed runnable task every 1000 ms which handles timer ticks.
     * ScheduledThreadPoolExecutor is used to schedule a runnable in an indefinite sequence which is cancelled upon
     * game pause or finish. Game pauses mid tick are handled by a delay which is calculated using {@link #tickElapsed}
     * which every call of {@link #pauseTimer()} which resets every tick completion.
     */
    fun startTimer() {
//        Log.i("Ticker", "start")
        //TODO: can i replace with coroutines?
        tickStartTime = SystemClock.uptimeMillis()
        timerFuture = timerExecutor.scheduleAtFixedRate(timerRunnable, 1000 - tickElapsed, 1000, TimeUnit.MILLISECONDS)
    }

    /**
     * Method to pause the game timer by cancelling the ScheduledThreadPoolExecutor which handles scheduling runnables
     * responsible for incrementing the timer and updating the TextView. The time elapsed since the previous tick is
     * calculated and stored as {@link #tickElapsed} which can be used to determine the appropriate delay for the timer
     * once resumed, thus keeping the timer accurate. This is calculated from the system time at the previous tick
     * stored in {@link #tickStartTime} each clock tick and the current system time upon call.
     */
    fun pauseTimer() {
//        Log.i("ticker", "pause")
        tickElapsed += SystemClock.uptimeMillis() - tickStartTime
        timerFuture.cancel(false)
    }

    /**
     * Setter function for tickElapsed variable to be accessed from ViewModel after UI has undergone a state change.
     */
    fun setTickElapsed(num: Long) {
        tickElapsed = num
    }

    /**
     * Getter function for tickElapsed variable to be accessed from ViewModel after UI has undergone a state change.
     */
    fun getTickElapsed(): Long = tickElapsed

    fun isRunning(): Boolean = isRunning

}