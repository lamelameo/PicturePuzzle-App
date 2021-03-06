package com.example.lamelameo.picturepuzzle.ui.main

import android.os.Handler
import android.os.Message
import com.example.lamelameo.picturepuzzle.data.PuzzleData
import java.util.*
import kotlin.collections.ArrayList
import kotlin.math.abs

class PuzzleController(private var handler: Handler, private var puzzleData: PuzzleData) {

    //TODO: generates, stores and controls manipulation of the puzzle data
    private val numCols = 4
    private var numCorrect = 0
    private var numMoves = 0

    /**
     * Populate a list with cell position indexes in a random order. This order of cells is checked for a property
     * which tells us if the puzzle can be solved. If it cannot be solved, we simply have to swap two items to make it
     * solvable. Once solvable, we return the ArrayList.
     *
     * @param gridSize number of cells in the puzzle grid
     */
    fun generatePuzzle(gridSize: Int): ArrayList<Int> {
        // initialise objects and set variables
        //TODO: gridSize is not numCols as in PuzzleActivity
        val list = ArrayList<Int>(gridSize)
        val random = Random()
        val cellPool = ArrayList<Int>(gridSize - 1)
        var bound = gridSize - 1
        // pool of cell indexes to be drawn from - excluding last cell index
        for (x in 0 until bound) {
            cellPool.add(x)
        }

        // randomise grid and create list with outcome
        // Randomly generate an index within the pool bounds to then pop that item and append to our output state list
        for (x in 0 until gridSize - 1) {
            val rngIndex = random.nextInt(bound)
            val cellValue = cellPool[rngIndex]
            list.add(cellValue)
            cellPool.removeAt(rngIndex)
            bound -= 1
        }
        list.add(gridSize - 1)

        // If we have even number of inversions then the puzzle is solvable, otherwise swap first two items
        // inversion: position pairs (a,b) where (list) index a < index b and (value) a > b have to check all
        if (getInversions(list) % 2 != 0) {
            val swap = list[0]
            list[0] = list[1]
            list[1] = swap
        }
        return list
    }

    /**
     * Given an ordered list of integers, determine the number of pairs (a,b) where a > b and index a < index b
     * @param list input list of integers
     */
    private fun getInversions(list: ArrayList<Int>): Int {
        var inversions = 0
        for (index in 0 until list.size - 1) {
            val currentNum = list[index]
            for (x in index + 1 until list.size) {
                val pairNum = list[x]
                if (currentNum > pairNum) {
                    inversions += 1
                }
            }
        }
        return inversions
    }

    /**
     *
     */
    private fun swapCells(cell1: Int, cell2: Int) {
        val swap = puzzleData.puzzleState[cell1]
        puzzleData.puzzleState[cell1] = puzzleData.puzzleState[cell2]
        puzzleData.puzzleState[cell2] = swap
        numMoves += 1
        handler.sendMessage(Message.obtain(handler, 2))
        if (gridSolved(puzzleData.puzzleState)) {
            handler.sendMessage(Message.obtain(handler, 3))
        }
    }

    /**
     * Determine if grid is solved by iterating through the puzzleState list and checking that the index matches the
     * cell index at that position
     *
     * @return a boolean which indicates whether the grid is solved or not
     */
    private fun gridSolved(list: ArrayList<Int>): Boolean {
        list.forEachIndexed { i, item ->
            if (i != item) {
                return false
            }
        }
        return true
    }

    /**
     * Given the index of a cell that has been clicked, determine if it should be moved by
     */
    fun cellClick(cellIndex: Int, emptyIndex: Int): Int {
        // cell should be in same row/col as empty but only 1 position away
        val cellRow: Int = cellIndex / numCols; val cellCol: Int = cellIndex % numCols
        val emptyCellRow: Int = emptyIndex / numCols; val emptyCellCol: Int = emptyIndex % numCols
        return if ((cellCol == emptyCellCol && abs(cellRow - emptyCellRow) == 1) ||
            (cellRow == emptyCellRow && abs(cellCol - emptyCellCol) == 1)) {
            swapCells(cellIndex, emptyIndex)
            1
        } else {
            0
        }
    }

    private val maps: List<List<Int>> = listOf(listOf(0,1,2,3,4,5,6,7), listOf(0,1,3,2,4,7,6,5),
        listOf(2,3,0,1,6,5,4,7), listOf(2,3,1,0,6,7,4,5))

    /**
     *
     */
    fun cellSwipe(cellIndex: Int, emptyIndex: Int, direction: Int): List<List<Int>> {
        val updates: MutableList<List<Int>> = mutableListOf()
        val cellRow: Int = cellIndex / numCols; val cellCol: Int = cellIndex % numCols
        val emptyCellRow: Int = emptyIndex / numCols; val emptyCellCol: Int = emptyIndex % numCols
        val colDiff: Int = emptyCellCol - cellCol; val rowDiff: Int = emptyCellRow - cellRow
        val list: List<Int> = listOf(emptyCellRow, cellRow, emptyCellCol, cellCol, colDiff, -1, rowDiff, 1)
        val vars: List<Int> = List(8) {i: Int -> list[maps[direction - 1][i]]}
        if (vars[0] == vars[1] && vars[2] > vars[3]) {
            for (i in 0 until abs(vars[4])) {
                swapCells(emptyIndex + i * vars[5], emptyIndex + (i + 1) * vars[5])
                updates.add(listOf(emptyIndex + i * vars[5], emptyIndex + (i + 1) * vars[5]))
            }
        }
        return updates
    }

    fun cellSwipeSimple(cellIndex: Int, emptyIndex: Int, direction: Int): Int {
        val cellRow: Int = cellIndex / numCols; val cellCol: Int = cellIndex % numCols
        val emptyCellRow: Int = emptyIndex / numCols; val emptyCellCol: Int = emptyIndex % numCols
        val colDiff: Int = emptyCellCol - cellCol; val rowDiff: Int = emptyCellRow - cellRow
        // 1, 2, 3, 4 = right, left, down, up, respectively
        when (direction) {
            1 -> if (emptyCellRow == cellRow && emptyCellCol > cellCol) {
                for (i in 0 until colDiff) {
                    swapCells(emptyIndex, emptyIndex)
                }
                return abs(colDiff)
            }
            2 -> if (emptyCellRow == cellRow && emptyCellCol < cellCol) {
                for (i in 0 until colDiff) {
                    swapCells(emptyIndex, emptyIndex)
                }
                return abs(colDiff)
            }
            3 -> if (emptyCellCol == cellCol && emptyCellRow > cellRow) {
                for (i in 0 until rowDiff) {
                    swapCells(emptyIndex,emptyIndex)
                }
                return abs(rowDiff)
            }
            4 -> if (emptyCellCol == cellCol && emptyCellRow < cellRow) {
                for (i in 0 until rowDiff) {
                    swapCells(emptyIndex,emptyIndex)
                }
                return abs(rowDiff)
            }
        }
        return 0
    }

}