package com.example.lamelameo.picturepuzzle.data

data class PuzzleData(var puzzleState: ArrayList<Int>,
                      var emptyCell: Int,
                      var gameTime: Int = 0,
                      var tickRemainder: Long = 0L,
                      var numMoves: Int = 0,
                      var gameState: Int = 0) {
}
