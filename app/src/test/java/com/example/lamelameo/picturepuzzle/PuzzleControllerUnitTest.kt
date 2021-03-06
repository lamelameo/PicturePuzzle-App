package com.example.lamelameo.picturepuzzle

import android.os.Handler
import android.os.Looper
import com.example.lamelameo.picturepuzzle.ui.main.PuzzleController
import org.junit.Assert
import org.junit.Test

class PuzzleControllerUnitTest {

    private val puzzleController: PuzzleController = PuzzleController(Handler(Looper.getMainLooper()))

    // TODO: Can I write a function to test all possible empty and clicked cell positions?
    // it would ultimately just be a rewrite of the function I already have??

    @Test
    fun testCellClick1() {
        // test 3x3 grid, empty cell in top right, all possible clicks 0->8
        val expected: Array<Int> = arrayOf(0,1,0,0,0,1,0,0,0)
        val results: Array<Int?> = arrayOfNulls(9)
        for (i in 0 until 9) {
            results[i] = puzzleController.cellClick(i, 2)
        }
        Assert.assertArrayEquals("wrong", expected, results)
    }

    @Test
    fun testCellClick2() {
        // test 3x3 grid, empty cell in middle, all possible clicks 0->8
        val expected: Array<Int> = arrayOf(0,1,0,1,0,1,0,1,0)
        val results: Array<Int?> = arrayOfNulls(9)
        for (i in 0 until 9) {
            results[i] = puzzleController.cellClick(i, 4)
        }
        Assert.assertArrayEquals("wrong", expected, results)
    }

    // TODO: test doesnt tell us if swapCell was called correctly...need to check grid state after?
    // Testing cell swipes on 4x4 grid with the empty cell in top right corner (index 3) with all possible cell swipes.
    // Generate the expected results by passing the valid cells and calculate the corresponding number of moves returned.

    @Test
    fun testCellSwipeRight1() {
        val expected: Array<Int> = Array(16) { i: Int -> if (i in listOf(0,1,2)) {3 - i} else {0} }
        val results: Array<Int> = Array(16) { 0 }
        val startState: Array<Int> = Array(16) { i: Int -> puzzleController.puzzleState[i] }
        for (i in 0 until 16) {
            results[i] = puzzleController.cellSwipe(i, 3, 1)
        }
        Assert.assertArrayEquals("wrong", expected, results)
    }

    @Test
    fun testCellSwipeLeft1() {
        val expected: Array<Int> = Array(16) { 0 }
        val results: Array<Int> = Array(16) { 0 }
        for (i in 0 until 16) {
            results[i] = puzzleController.cellSwipe(i, 3, 2)
        }
        Assert.assertArrayEquals("wrong", expected, results)
    }

    @Test
    fun testCellSwipeDown1() {
        val expected: Array<Int> = Array(16) { 0 }
        val results: Array<Int> = Array(16) { 0 }
        for (i in 0 until 16) {
            results[i] = puzzleController.cellSwipe(i, 3, 3)
        }
        Assert.assertArrayEquals("wrong", expected, results)
    }

    @Test
    fun testCellSwipeUp1() {
        val expected: Array<Int> = Array(16) { i: Int -> if (i in listOf(7,11,15)) {i/4} else {0} }
        val results: Array<Int> = Array(16) { 0 }
        for (i in 0 until 16) {
            results[i] = puzzleController.cellSwipe(i, 3, 4)
        }
        Assert.assertArrayEquals("wrong", expected, results)
    }

    @Test
    fun testCellSwipeRight2() {
        val expected: Array<Int> = Array(16) { i: Int -> if (i in listOf(4,5)) {2 - (i%4)} else {0} }
        val results: Array<Int> = Array(16) { 0 }
        for (i in 0 until 16) {
            results[i] = puzzleController.cellSwipe(i, 6, 1)
        }
        Assert.assertArrayEquals("wrong", expected, results)
    }

    // Testing with empty cell in middle of grid (index 6)

    @Test
    fun testCellSwipeLeft2() {
        val expected: Array<Int> = Array(16) { 0 }
        expected[7] = 1
        val results: Array<Int> = Array(16) { 0 }
        for (i in 0 until 16) {
            results[i] = puzzleController.cellSwipe(i, 6, 2)
        }
        Assert.assertArrayEquals("wrong", expected, results)
    }

    @Test
    fun testCellSwipeDown2() {
        val expected: Array<Int> = Array(16) { 0 }
        expected[2] = 1
        val results: Array<Int> = Array(16) { 0 }
        for (i in 0 until 16) {
            results[i] = puzzleController.cellSwipe(i, 6, 3)
        }
        Assert.assertArrayEquals("wrong", expected, results)
    }

    @Test
    fun testCellSwipeUp2() {
        val expected: Array<Int> = Array(16) { i: Int -> if (i in listOf(10,14)) {(i/4) - 1} else {0} }
        val results: Array<Int> = Array(16) { 0 }
        for (i in 0 until 16) {
            results[i] = puzzleController.cellSwipe(i, 6, 4)
        }
        Assert.assertArrayEquals("wrong", expected, results)
    }
}