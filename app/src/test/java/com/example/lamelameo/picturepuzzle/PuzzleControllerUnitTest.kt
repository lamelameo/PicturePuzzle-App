package com.example.lamelameo.picturepuzzle

import android.os.Handler
import android.os.Looper
import com.example.lamelameo.picturepuzzle.data.PuzzleData
import com.example.lamelameo.picturepuzzle.data.PuzzleController
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import kotlin.math.abs


@RunWith(MockitoJUnitRunner::class)
class PuzzleControllerUnitTest {

//    @Mock
//    private val looper: Looper = Looper.getMainLooper()
//    @Mock
//    private val handler: Handler = Handler(looper)
//    private val puzzleController: PuzzleController = PuzzleController(handler,
//        PuzzleData(ArrayList(),0), 0)

    // TODO: Can I write a function to test all possible empty and clicked cell positions?
    // it would ultimately just be a rewrite of the function I already have??

    // TODO: Didn't implement Mockito yet - just testing with copy pasted functions...

    /**
     * Given the index of a cell that has been clicked, determine if it should be moved by
     */
    private fun cellClick(numCols: Int, cellIndex: Int, emptyIndex: Int): List<Int> {
        // cell should be in same row/col as empty but only 1 position away
        val cellRow: Int = cellIndex / numCols; val cellCol: Int = cellIndex % numCols
        val emptyCellRow: Int = emptyIndex / numCols; val emptyCellCol: Int = emptyIndex % numCols
        return if ((cellCol == emptyCellCol && abs(cellRow - emptyCellRow) == 1) ||
            (cellRow == emptyCellRow && abs(cellCol - emptyCellCol) == 1)) {
            listOf(cellIndex, emptyIndex)
        } else {
            listOf()
        }
    }

    private val maps: List<List<Int>> = listOf(listOf(0,1,2,3,4,5), listOf(0,1,3,2,4,7),
        listOf(2,3,0,1,6,8), listOf(2,3,1,0,6,9))
    /**
     *
     */
    private fun cellSwipe(numCols: Int, cellIndex: Int, emptyIndex: Int, direction: Int): List<List<Int>> {
        val updates: MutableList<List<Int>> = mutableListOf()
        maps[direction].map { listOf(emptyIndex / numCols, cellIndex / numCols, emptyIndex % numCols, cellIndex % numCols,
            emptyIndex % numCols - cellIndex % numCols, -1, emptyIndex / numCols - cellIndex / numCols, 1, -numCols, numCols)[it] }.let {
            if (it[0] == it[1] && it[2] > it[3]) {
                for (i in 0 until abs(it[4])) {
                    updates.add(listOf(emptyIndex + (i + 1) * it[5], emptyIndex + i * it[5]))
                }
            }
        }
        return updates
    }

    @Test
    fun testCellClick1() {
        // test 3x3 grid, empty cell in top right, all possible clicks 0->8
        val expected: Array<List<Int>> = Array(9) { i: Int -> if (i in listOf(1,5)) listOf(i,2) else listOf() }
        val results: Array<List<Int>?> = arrayOfNulls(9)
        for (i in 0 until 9) {
            results[i] = cellClick(3, i, 2)
        }
        Assert.assertArrayEquals("wrong", expected, results)
    }

    @Test
    fun testCellClick2() {
        // test 3x3 grid, empty cell in middle, all possible clicks 0 -> 8
        val expected: Array<List<Int>> = Array(9) { i: Int -> if (i in listOf(1,3,5,7)) listOf(i,4) else listOf() }
        val results: Array<List<Int>?> = arrayOfNulls(9)
        for (i in 0 until 9) {
            results[i] = cellClick(3, i, 4)
        }
        Assert.assertArrayEquals("wrong", expected, results)
    }

    // TODO: test doesnt tell us if swapCell was called correctly...need to check grid state after?
    // Testing cell swipes on 4x4 grid with the empty cell in top right corner (index 3) with all possible cell swipes.
    // Generate the expected results by passing the valid cells and calculate the corresponding number of moves returned.

    @Test
    fun testCellSwipeRight1() {
        val expected: List<List<Int>> = List(16) { cell: Int ->
            if (cell in listOf(0,1,2)) {
                List(3 - cell) { i: Int ->  listOf(2 - i, 3 - i) }.flatten()
            } else { listOf() }
        }
        val results: MutableList<List<Int>?> = List(16) { null }.toMutableList()
        for (i in 0 until 16) {
            results[i] = cellSwipe(4, i, 3, 0).flatten()
        }
        Assert.assertEquals("wrong", expected, results)
    }

    @Test
    fun testCellSwipeLeft1() {
        val expected: Array<List<Int>> = Array(16) { listOf() }
        val results: Array<List<Int>> = Array(16) { listOf() }
        for (i in 0 until 16) {
            results[i] = cellSwipe(4, i, 3, 1).flatten()
        }
        Assert.assertArrayEquals("wrong", expected, results)
    }

    @Test
    fun testCellSwipeDown1() {
        val expected: Array<List<Int>> = Array(16) { listOf() }
        val results: Array<List<Int>> = Array(16) { listOf() }
        for (i in 0 until 16) {
            results[i] = cellSwipe(4, i, 3, 2).flatten()
        }
        Assert.assertArrayEquals("wrong", expected, results)
    }

    @Test
    fun testCellSwipeUp1() {
        val expected: List<List<Int>> = List(16) { cell: Int ->
            if (cell in listOf(7,11,15)) {
                List(cell/4) { i: Int ->  listOf(4*(i+2) - 1, 4*(i+1) - 1) }.flatten()
            } else { listOf() }
        }
        val results: MutableList<List<Int>?> = List(16) { null }.toMutableList()
        for (i in 0 until 16) {
            results[i] = cellSwipe(4, i, 3, 3).flatten()
        }
        Assert.assertEquals("wrong", expected, results)
    }

    // Testing with empty cell in middle of grid (index 6)

    @Test
    fun testCellSwipeRight2() {
//        val expected: Array<Int> = Array(16) { i: Int -> if (i in listOf(4,5)) {2 - (i%4)} else {0} }
        val expected: List<List<Int>> = List(16) { cell: Int ->
            if (cell in listOf(4,5)) {
                List( 2 - cell%4) { i: Int ->  listOf(6 - (i+1), 6 - i) }.flatten()
            } else { listOf() }
        }
        val results: MutableList<List<Int>?> = MutableList(16) { null }
        for (i in 0 until 16) {
            results[i] = cellSwipe(4, i, 6, 0).flatten()
        }
        Assert.assertEquals("wrong", expected, results)
    }

    @Test
    fun testCellSwipeLeft2() {
        val expected: List<List<Int>> = List(16) { cell: Int -> if (cell == 7) listOf(7,6) else listOf() }
        val results: MutableList<List<Int>?> = MutableList(16) { null }
        for (i in 0 until 16) {
            results[i] = cellSwipe(4, i, 6, 1).flatten()
        }
        Assert.assertEquals("wrong", expected, results)
    }

    @Test
    fun testCellSwipeDown2() {
        val expected: List<List<Int>> = List(16) { cell: Int -> if (cell == 2) listOf(2,6) else listOf() }
        val results: MutableList<List<Int>?> = MutableList(16) { null }
        for (i in 0 until 16) {
            results[i] = cellSwipe(4, i, 6, 2).flatten()
        }
        Assert.assertEquals("wrong", expected, results)
    }

    @Test
    fun testCellSwipeUp2() {
//        val expected: Array<Int> = Array(16) { i: Int -> if (i in listOf(10,14)) {(i/4) - 1} else {0} }
        val expected: List<List<Int>> = List(16) { cell: Int ->
            if (cell in listOf(10,14)) {
                List(cell/4 - 1) { i: Int ->  listOf(4*(i+3) - 2, 4*(i+2) - 2) }.flatten()
            } else { listOf() }
        }
        val results: MutableList<List<Int>?> = MutableList(16) { null }
        for (i in 0 until 16) {
            results[i] = cellSwipe(4, i, 6, 3).flatten()
        }
        Assert.assertEquals("wrong", expected, results)
    }
}