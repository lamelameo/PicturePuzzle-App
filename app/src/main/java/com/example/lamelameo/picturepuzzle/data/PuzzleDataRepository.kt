package com.example.lamelameo.picturepuzzle.data

class PuzzleDataRepository private constructor(private val bestDao: BestDataDao) {

    fun addBest(data: BestData) {
        bestDao.addBest(data)
    }

    fun getBests() = bestDao.getBest()

    companion object {
        @Volatile private var instance: PuzzleDataRepository? = null
        fun getInstance(bestDao: BestDataDao) =
            instance?: synchronized(this) {
                instance ?: PuzzleDataRepository(bestDao).also { instance= it}
        }
    }
}