package com.example.lamelameo.picturepuzzle.data

data class PuzzleData(val gameTime: Int,
                      val tickRemainder: Long,
                      val numMoves: Int,
                      val puzzleState: ArrayList<Int>,
                      val emptyCell: Int,
                      var isPaused: Boolean,
                      val isSolved: Boolean,

                      ) {

}
